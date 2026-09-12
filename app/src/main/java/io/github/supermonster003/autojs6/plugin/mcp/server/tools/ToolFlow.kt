package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Thrown inside a tool flow when the host answered a failure; the executor turns it into an `isError` result. */
class ToolFailureException(val failure: ToolFailure) : RuntimeException(failure.render())

/** The host bridge as a flow sees it: one call, the session's client name already attached. */
fun interface BridgeCaller {

    suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome
}

/** [BridgeCaller.call] that throws [ToolFailureException] on a failure and returns the result otherwise. */
suspend fun BridgeCaller.callOrThrow(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): JsonElement =
    when (val outcome = call(module, method, args, timeoutMs, permissions)) {
        is BridgeOutcome.Ok -> outcome.result
        is BridgeOutcome.Failed -> throw ToolFailureException(outcome.failure)
    }

/** A flow's structured metadata, display text, and optional MCP image content. */
class ToolFlowResult(val structured: JsonObject, val text: String = structured.toString(), val image: ImageContent? = null)

/**
 * A tool that needs more than one host call, or shapes its arguments and results beyond a
 * single method mapping (the `ui` group of roadmap P3.2): [progressTotalMs] sizes the progress
 * heartbeat, [run] performs the calls through the [BridgeCaller] and may throw
 * [ToolArgumentException] or [ToolFailureException].
 */
class ToolFlow(val progressTotalMs: Long, val run: suspend (BridgeCaller) -> ToolFlowResult)

/** Provides a [ToolFlow] for the tools it knows, null for the others (which map onto one host call). */
fun interface ToolFlows {

    /** May throw [ToolArgumentException] while reading [arguments]. */
    fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow?
}
