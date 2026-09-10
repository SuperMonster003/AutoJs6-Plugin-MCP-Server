package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostBridgeClient
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotification
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotificationParams
import io.modelcontextprotocol.kotlin.sdk.types.RequestId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Executes catalog tools (roadmap P2.3): validates the arguments, records the `tool_call`
 * event, runs plugin-local tools in process, and sends the others through the [HostBridgeClient]
 * of the open session. While a host call is pending and the client asked for progress
 * (`_meta.progressToken`), a heartbeat notification goes out every [heartbeatMs] so long script
 * runs do not look stuck.
 */
class CatalogToolExecutor(
    private val bridge: () -> HostBridgeClient?,
    private val local: Map<String, suspend (JsonObject) -> JsonObject>,
    private val clientNameOf: (sessionId: String?) -> String?,
    private val onToolCall: (toolName: String, clientName: String?) -> Unit,
    private val heartbeatMs: Long = HEARTBEAT_MS,
) : ToolExecutor {

    private class BridgeCall(val args: JsonArray, val timeoutMs: Long, val shape: (JsonElement) -> JsonObject)

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
        val call = try {
            prepare(spec, arguments)
        } catch (e: ToolArgumentException) {
            return ToolResults.failure(e.failure)
        }
        val outcome = withHeartbeat(connection, request.meta?.progressToken, call.timeoutMs) {
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
            BridgeCall(prepared.args, prepared.bridgeTimeoutMs) { result -> ScriptRunTool.shape(result, prepared.maxConsoleLines) }
        }
        else -> BridgeCall(JsonArray(emptyList()), spec.timeoutMs) { result ->
            result as? JsonObject ?: buildJsonObject { put("result", result) }
        }
    }

    private suspend fun <T> withHeartbeat(connection: ClientConnection, token: RequestId?, totalMs: Long, block: suspend () -> T): T {
        if (token == null) return block()
        return coroutineScope {
            val startedAt = System.currentTimeMillis()
            val ticker = launch {
                while (isActive) {
                    delay(heartbeatMs)
                    val elapsed = System.currentTimeMillis() - startedAt
                    runCatching {
                        connection.notification(
                            ProgressNotification(
                                ProgressNotificationParams(token, elapsed.toDouble(), totalMs.toDouble(), "waiting for AutoJs6 (${elapsed / 1000} s)"),
                            ),
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

    companion object {

        /** Progress heartbeat interval while a host call is pending. */
        const val HEARTBEAT_MS = 5_000L
    }
}
