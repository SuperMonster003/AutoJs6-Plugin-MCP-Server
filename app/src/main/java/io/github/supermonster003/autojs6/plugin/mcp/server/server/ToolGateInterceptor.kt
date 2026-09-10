package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolResults
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respondText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * What the tool gate asks the registry (roadmap P2.3, decision D6): whether the group switches
 * changed since the tools were registered, and whether a tool exists but is switched off.
 */
interface ToolGate {

    /** Re-reads the group switches and updates the SDK registration; true when it changed. Called before list and call requests. */
    suspend fun refresh(): Boolean

    /** The `TOOL_DISABLED` failure for a known but switched-off tool, or null when the tool may run. */
    fun disabledFailure(toolName: String): ToolFailure?
}

/**
 * Answers `tools/call` requests for switched-off tools with an `isError` result carrying
 * `TOOL_DISABLED` (appendix A.5) instead of the SDK's "tool not found" error, after the pairing
 * gate admitted the client. `tools/list` and `tools/call` first give the gate a chance to pick
 * up group switches changed by the settings page, so a toggle takes effect on the next request.
 */
fun Application.installToolGate(gate: ToolGate) {
    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.httpMethod != HttpMethod.Post) return@intercept
        val body = call.attributes.getOrNull(BUFFERED_BODY) ?: return@intercept
        val root = runCatching { Json.parseToJsonElement(String(body, Charsets.UTF_8)) }.getOrNull() as? JsonObject ?: return@intercept
        val method = runCatching { root["method"]?.jsonPrimitive?.contentOrNull }.getOrNull() ?: return@intercept
        if (method != METHOD_TOOLS_LIST && method != METHOD_TOOLS_CALL) return@intercept
        gate.refresh()
        if (method != METHOD_TOOLS_CALL) return@intercept
        val id = root["id"] ?: return@intercept
        val name = runCatching { (root["params"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull }.getOrNull() ?: return@intercept
        val failure = gate.disabledFailure(name) ?: return@intercept
        val document = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("result", ToolResults.failureJson(failure))
        }
        call.respondText(document.toString(), ContentType.Application.Json, HttpStatusCode.OK)
        finish()
    }
}

private const val METHOD_TOOLS_LIST = "tools/list"
private const val METHOD_TOOLS_CALL = "tools/call"
