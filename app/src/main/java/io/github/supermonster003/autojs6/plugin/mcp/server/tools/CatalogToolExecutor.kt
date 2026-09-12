package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostBridgeClient
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.shared.RequestHandlerExtra
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotification
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotificationParams
import io.modelcontextprotocol.kotlin.sdk.types.RequestId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Executes catalog tools (roadmap P2.3 / P3.1 / P3.2): validates the arguments, records the
 * `tool_call` event, runs plugin-local tools in process, hands the tools that need several host
 * calls to their [ToolFlows] (the `ui` group), and sends the others through the
 * [HostBridgeClient] of the open session as one call. While a host call is pending and the
 * client asked for progress (`_meta.progressToken`), a heartbeat notification goes out every
 * [heartbeatMs]; the two script run tools use the faster [scriptHeartbeatMs] and add the newest
 * console line to it, so long script runs show what they are doing. The heartbeat is sent as a
 * notification related to the pending request, so the SDK writes it into that request's own
 * response stream and a client needs no standalone GET stream to see it.
 */
class CatalogToolExecutor(
    private val bridge: () -> HostBridgeClient?,
    private val local: Map<String, suspend (JsonObject) -> JsonObject>,
    private val clientNameOf: (sessionId: String?) -> String?,
    private val onToolCall: (toolName: String, clientName: String?) -> Unit,
    private val flows: ToolFlows? = null,
    private val heartbeatMs: Long = HEARTBEAT_MS,
    private val scriptHeartbeatMs: Long = SCRIPT_HEARTBEAT_MS,
) : ToolExecutor {

    private class BridgeCall(
        val args: JsonArray,
        val timeoutMs: Long,
        val progressMs: Long,
        val followConsole: Boolean,
        val shape: (JsonElement) -> JsonObject,
    )

    override suspend fun execute(spec: ToolSpec, connection: ClientConnection, request: CallToolRequest): CallToolResult {
        val arguments = try {
            ToolArguments.validate(spec, request.arguments)
        } catch (e: ToolArgumentException) {
            return ToolResults.failure(e.failure)
        }
        val clientName = runCatching { clientNameOf(connection.sessionId) }.getOrNull()
        onToolCall(spec.name, clientName)
        local[spec.name]?.let { handler ->
            return try {
                ToolResults.success(handler(arguments))
            } catch (e: ToolArgumentException) {
                ToolResults.failure(e.failure)
            } catch (e: Exception) {
                ToolResults.failure(ToolFailure.internal("${spec.name} failed: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
        val method = spec.bridge ?: return ToolResults.failure(ToolFailure.internal("tool ${spec.name} has no host method"))
        val client = bridge() ?: return ToolResults.failure(ToolFailure.hostUnavailable())
        val token = request.meta?.progressToken
        val relatedRequestId = currentCoroutineContext()[RequestHandlerExtra.Key]?.requestId
        Log.d(TAG, "${spec.name}: progress token ${if (token != null) "present" else "absent"}, related request id ${if (relatedRequestId != null) "present" else "absent"}")
        val flow = try {
            flows?.planFor(spec, arguments)
        } catch (e: ToolArgumentException) {
            return ToolResults.failure(e.failure)
        }
        if (flow != null) {
            val caller = BridgeCaller { module, name, args, timeoutMs, permissions -> client.call(module, name, args, timeoutMs, permissions, clientName) }
            return try {
                val result = withHeartbeat(connection, token, relatedRequestId, heartbeatMs, flow.progressTotalMs, followConsole = false, client, clientName) {
                    flow.run(caller)
                }
                ToolResults.success(result.structured, result.text, result.image)
            } catch (e: ToolArgumentException) {
                ToolResults.failure(e.failure)
            } catch (e: ToolFailureException) {
                ToolResults.failure(e.failure)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "${spec.name} failed", e)
                ToolResults.failure(ToolFailure.internal("${spec.name} failed: ${e.message ?: e.javaClass.simpleName}"))
            }
        }
        val call = try {
            prepare(spec, arguments)
        } catch (e: ToolArgumentException) {
            return ToolResults.failure(e.failure)
        }
        val outcome = withHeartbeat(connection, token, relatedRequestId, call.progressMs, call.timeoutMs, call.followConsole, client, clientName) {
            client.call(method.module, method.method, call.args, call.timeoutMs, spec.permissions, clientName)
        }
        return when (outcome) {
            is BridgeOutcome.Ok -> ToolResults.success(call.shape(outcome.result))
            is BridgeOutcome.Failed -> ToolResults.failure(outcome.failure)
        }
    }

    private fun prepare(spec: ToolSpec, arguments: JsonObject): BridgeCall = when (spec.name) {
        ToolCatalog.SCRIPT_RUN -> {
            val prepared = ScriptRunTool.prepare(arguments)
            BridgeCall(prepared.args, prepared.bridgeTimeoutMs, scriptHeartbeatMs, followConsole = true) { ScriptRunTool.shape(it, prepared) }
        }
        ToolCatalog.SCRIPT_RUN_FILE -> {
            val prepared = ScriptRunTool.prepareFile(arguments)
            BridgeCall(prepared.args, prepared.bridgeTimeoutMs, scriptHeartbeatMs, followConsole = true) { ScriptRunTool.shape(it, prepared) }
        }
        ToolCatalog.SCRIPT_STOP -> BridgeCall(ScriptTools.stopArgs(arguments), spec.timeoutMs, heartbeatMs, followConsole = false) { ScriptTools.shapeStop(it) }
        ToolCatalog.SCRIPT_STOP_ALL -> BridgeCall(ScriptTools.STOP_ALL_ARGS, spec.timeoutMs, heartbeatMs, followConsole = false) { ScriptTools.shapeStopAll(it) }
        ToolCatalog.SCRIPT_LIST -> BridgeCall(ScriptTools.NO_ARGS, spec.timeoutMs, heartbeatMs, followConsole = false) { ScriptTools.shapeList(it) }
        ToolCatalog.CONSOLE_TAIL -> BridgeCall(ScriptTools.tailArgs(arguments), spec.timeoutMs, heartbeatMs, followConsole = false) { ScriptTools.shapeTail(it) }
        else -> BridgeCall(ScriptTools.NO_ARGS, spec.timeoutMs, heartbeatMs, followConsole = false) { result ->
            result as? JsonObject ?: buildJsonObject { put("result", result) }
        }
    }

    private suspend fun <T> withHeartbeat(
        connection: ClientConnection,
        token: RequestId?,
        relatedRequestId: RequestId?,
        progressMs: Long,
        totalMs: Long,
        followConsole: Boolean,
        client: HostBridgeClient,
        clientName: String?,
        block: suspend () -> T,
    ): T {
        if (token == null) return block()
        return coroutineScope {
            val startedAt = System.currentTimeMillis()
            val ticker = launch {
                while (isActive) {
                    delay(progressMs)
                    val elapsed = System.currentTimeMillis() - startedAt
                    val message = buildString {
                        append("waiting for AutoJs6 (${elapsed / 1000} s)")
                        if (followConsole) latestConsoleLine(client, clientName)?.let { append("; last output: ").append(it) }
                    }
                    runCatching {
                        connection.notification(
                            ProgressNotification(ProgressNotificationParams(token, elapsed.toDouble(), totalMs.toDouble(), message)),
                            relatedRequestId,
                        )
                    }
                }
            }
            try {
                block()
            } finally {
                ticker.cancel()
            }
        }
    }

    /** The newest console line when the grant allows `console.tail`; null on any failure (the heartbeat still goes out). */
    private suspend fun latestConsoleLine(client: HostBridgeClient, clientName: String?): String? {
        if (!client.isGranted("console", "tail")) return null
        val outcome = client.call("console", "tail", CONSOLE_PEEK_ARGS, CONSOLE_PEEK_TIMEOUT_MS, listOf("console"), clientName)
        val result = (outcome as? BridgeOutcome.Ok)?.result as? JsonObject ?: return null
        val entry = (result["entries"] as? JsonArray)?.lastOrNull() as? JsonObject ?: return null
        return (entry["text"] as? JsonPrimitive)?.contentOrNull?.trimEnd()?.takeIf { it.isNotEmpty() }?.take(MAX_PROGRESS_LINE_CHARS)
    }

    companion object {

        private const val TAG = "CatalogToolExecutor"

        /** Progress heartbeat interval while a host call is pending. */
        const val HEARTBEAT_MS = 5_000L

        /** Progress heartbeat interval of `script_run` / `script_run_file` (roadmap P3.1: 2 s with the newest output line). */
        const val SCRIPT_HEARTBEAT_MS = 2_000L

        /** How long a heartbeat waits for `console.tail` before going out without a line. */
        const val CONSOLE_PEEK_TIMEOUT_MS = 3_000L

        /** The newest console line is cut to this many characters in a progress message. */
        const val MAX_PROGRESS_LINE_CHARS = 200

        private val CONSOLE_PEEK_ARGS = JsonArray(listOf(buildJsonObject { put("lines", 1) }))
    }
}
