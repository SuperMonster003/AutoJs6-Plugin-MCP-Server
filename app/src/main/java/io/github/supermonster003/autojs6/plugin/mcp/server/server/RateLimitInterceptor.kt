package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolResults
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Per-client rate limits in front of the SDK transport (roadmap P6), after the bearer check and
 * before the pairing gate, so that only authenticated traffic is counted and a flood from one
 * client never reaches the pairing prompts or the host.
 *
 * The client is the same identity the pairing gate uses (the `clientInfo.name` of its session,
 * or the `User-Agent` before `initialize`) together with the remote address; an invented
 * `Mcp-Session-Id` therefore does not open a fresh window. A request over the per-second limit
 * is answered with `429`, a `Retry-After` header and a JSON-RPC `RATE_LIMITED` error whose
 * `data.retryAfterMs` says when to retry. A `tools/call` of a screenshot tool over the per-minute
 * limit is answered as a tool result with `isError` and the `RATE_LIMITED` code, so the model
 * reads the wait time instead of losing the transport.
 */
fun Application.installRateLimit(server: Server, limiter: RateLimiter, screenshotTools: Set<String> = RateLimiter.SCREENSHOT_TOOLS) {
    intercept(ApplicationCallPipeline.Plugins) {
        val identity = resolveIdentity(server, call.request.header(SESSION_HEADER), call.request.header(HttpHeaders.UserAgent), call.request.origin.remoteHost)
        val key = "${identity.name}@${call.request.origin.remoteHost}"
        val body = if (call.request.httpMethod == HttpMethod.Post) call.attributes.getOrNull(BUFFERED_BODY) else null
        val calls = body?.let { bytes -> call.attributes.getOrNull(JSON_RPC_CALLS) ?: JsonRpcCalls.parse(String(bytes, Charsets.UTF_8)) }

        val verdict = limiter.acquireRequest(key)
        if (!verdict.allowed) {
            call.response.header(HttpHeaders.RetryAfter, verdict.retryAfterSeconds.toString())
            val message = "${McpErrors.CODE_RATE_LIMITED}: at most ${verdict.limit} requests per ${verdict.windowMs / 1_000L} s per client; retry after ${verdict.retryAfterMs} ms"
            val data = buildJsonObject {
                put("code", McpErrors.CODE_RATE_LIMITED)
                put("retryAfterMs", verdict.retryAfterMs)
                put("limit", verdict.limit)
                put("windowMs", verdict.windowMs)
            }
            val document = if (calls != null && calls.ids.isNotEmpty()) {
                McpErrors.errors(calls.ids, calls.batch, McpErrors.RATE_LIMITED, message, data)
            } else {
                McpErrors.error(null, McpErrors.RATE_LIMITED, message, data)
            }
            call.respondText(document.toString(), ContentType.Application.Json, HttpStatusCode.TooManyRequests)
            finish()
            return@intercept
        }

        if (body == null || calls == null || calls.ids.isEmpty()) return@intercept
        val screenshotCalls = screenshotCallsOf(body, screenshotTools)
        if (screenshotCalls.isEmpty()) return@intercept
        for (screenshot in screenshotCalls) {
            val shot = limiter.acquireScreenshot(key)
            if (shot.allowed) continue
            val failure = ToolFailure.rateLimited(screenshot.name, shot.limit, shot.windowMs, shot.retryAfterMs)
            val document = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", screenshot.id)
                put("result", ToolResults.failureJson(failure))
            }
            call.respondText(document.toString(), ContentType.Application.Json, HttpStatusCode.OK)
            finish()
            return@intercept
        }
    }
}

private class ScreenshotCall(val id: kotlinx.serialization.json.JsonElement, val name: String)

/** The `tools/call` requests of [body] whose tool is one of [tools], in body order. */
private fun screenshotCallsOf(body: ByteArray, tools: Set<String>): List<ScreenshotCall> {
    val root = runCatching { Json.parseToJsonElement(String(body, Charsets.UTF_8)) }.getOrNull() ?: return emptyList()
    val elements = when (root) {
        is JsonArray -> root
        is JsonObject -> listOf(root)
        else -> return emptyList()
    }
    return elements.filterIsInstance<JsonObject>().mapNotNull { request ->
        val id = request["id"] ?: return@mapNotNull null
        val method = runCatching { request["method"]?.jsonPrimitive?.contentOrNull }.getOrNull() ?: return@mapNotNull null
        if (method != METHOD_TOOLS_CALL) return@mapNotNull null
        val name = runCatching { (request["params"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull }.getOrNull() ?: return@mapNotNull null
        if (name in tools) ScreenshotCall(id, name) else null
    }
}

private const val SESSION_HEADER = "Mcp-Session-Id"
private const val METHOD_TOOLS_CALL = "tools/call"
