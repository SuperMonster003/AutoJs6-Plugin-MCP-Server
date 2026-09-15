package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.ApplicationReceivePipeline
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.io.readByteArray

/**
 * The Ktor module of the MCP endpoint (roadmap P2.1 / P2.2): the [RequestGate], the bearer
 * check, and the pairing gate run in that order in front of the SDK's stateful Streamable HTTP
 * mount at [path] (decision D9 as amended on 2026-09-10: the stateless model waits for SDK
 * support and has no route).
 *
 * `initialize` negotiation (`2025-06-18` / `2025-11-25`), the `Mcp-Session-Id` header, and the
 * `DELETE` session close are the SDK's behaviour; the SDK's own DNS rebinding validator is off
 * because [policy] changes at runtime (LAN addresses) and the gate covers every route. A null
 * [tokenProvider], [pairingGate], or [toolGate] leaves that layer out, which only the JVM tests
 * and the default `device_ping`-only listener use. The activity marker of the idle auto-stop
 * and the rate limiter (roadmap P6) run after the bearer check so that only authenticated
 * traffic is counted. The tool gate (roadmap P2.3)
 * runs last, after the client is admitted, so unpaired clients learn nothing about the catalog. The mount
 * itself is [mcpStreamableSse] (roadmap P3.1): the SDK's `mcpStreamableHttp` answers in JSON
 * and drops the notifications that belong to a request, so responses stream as server-sent
 * events instead.
 */
fun Application.mcpServerModule(
    server: Server,
    policy: () -> GatePolicy,
    tokenProvider: (() -> String?)? = null,
    pairingGate: PairingGate? = null,
    toolGate: ToolGate? = null,
    rateLimiter: RateLimiter? = null,
    activity: IdleStopMonitor? = null,
    path: String = McpServerPlugin.ENDPOINT_PATH,
) {
    installRequestGate(policy)
    if (tokenProvider != null) installBearerAuth(tokenProvider)
    if (activity != null) installActivityMarker(activity)
    if (rateLimiter != null) installRateLimit(server, rateLimiter)
    if (pairingGate != null) installPairingGate(server, pairingGate)
    if (toolGate != null) installToolGate(toolGate)
    mcpStreamableSse(path) { server }
}

/**
 * Installs the gate ahead of routing: `Host` / `Origin` validation, the declared-length check,
 * and a hard ceiling on the bytes of a `POST` body. The body is read here (at most the ceiling
 * plus one byte) and replayed to the transport through the receive pipeline, so an oversized
 * body is refused with `413` before the SDK parses anything, whether or not the client declared
 * its length. A body that nests deeper than [RequestBodyChecks.MAX_JSON_DEPTH] or repeats a
 * request id is refused with `400` before any parser recurses into it (roadmap P6); the
 * request list parsed here is kept in [JSON_RPC_CALLS] for the gates behind.
 */
fun Application.installRequestGate(policy: () -> GatePolicy) {
    intercept(ApplicationCallPipeline.Plugins) {
        val active = policy()
        val request = GateRequest(
            method = call.request.httpMethod.value,
            host = call.request.header(HttpHeaders.Host),
            origin = call.request.header(HttpHeaders.Origin),
            contentLength = call.request.header(HttpHeaders.ContentLength)?.trim()?.toLongOrNull(),
        )
        when (val decision = RequestGate.evaluate(request, active)) {
            is GateDecision.Allow -> {
                decision.corsOrigin?.let { call.response.appendCorsHeaders(it) }
                if (call.request.httpMethod == HttpMethod.Post) {
                    val bytes = call.request.receiveChannel().readRemaining(active.maxBodyBytes + 1).readByteArray()
                    if (bytes.size > active.maxBodyBytes) {
                        call.reject(RequestGate.rejectBodyTooLarge(active.maxBodyBytes))
                        finish()
                    } else if (RequestBodyChecks.isTooDeep(bytes)) {
                        call.reject(RequestGate.rejectBody(RequestBodyChecks.MESSAGE_TOO_DEEP))
                        finish()
                    } else {
                        val calls = JsonRpcCalls.parse(String(bytes, Charsets.UTF_8))
                        if (calls != null && RequestBodyChecks.hasDuplicateIds(calls)) {
                            call.reject(RequestGate.rejectBody(RequestBodyChecks.MESSAGE_DUPLICATE_ID))
                            finish()
                        } else {
                            call.attributes.put(BUFFERED_BODY, bytes)
                            calls?.let { call.attributes.put(JSON_RPC_CALLS, it) }
                        }
                    }
                }
            }
            is GateDecision.Preflight -> {
                call.response.appendCorsHeaders(decision.corsOrigin)
                call.response.header(HttpHeaders.AccessControlAllowMethods, "GET, POST, DELETE, OPTIONS")
                call.response.header(HttpHeaders.AccessControlAllowHeaders, CORS_ALLOWED_HEADERS)
                call.response.header(HttpHeaders.AccessControlMaxAge, CORS_MAX_AGE_SECONDS.toString())
                call.respond(HttpStatusCode.NoContent)
                finish()
            }
            is GateDecision.Reject -> {
                call.reject(decision)
                finish()
            }
        }
    }
    receivePipeline.intercept(ApplicationReceivePipeline.Before) {
        val bytes = call.attributes.getOrNull(BUFFERED_BODY) ?: return@intercept
        proceedWith(ByteReadChannel(bytes))
    }
}

/**
 * Counts the traffic the idle auto-stop option looks at (roadmap P6): a `POST` from its
 * arrival to the end of its response (a tool call that streams for minutes is never idle), any
 * other method when it arrives. Installed behind the bearer check, so refused requests never
 * count.
 */
fun Application.installActivityMarker(monitor: IdleStopMonitor) {
    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.httpMethod != HttpMethod.Post) {
            monitor.touch()
            return@intercept
        }
        monitor.begin()
        try {
            proceed()
        } finally {
            monitor.end()
        }
    }
}

/** The POST body the gate read, replayed to the transport and inspected by the pairing gate. */
internal val BUFFERED_BODY = AttributeKey<ByteArray>("McpRequestGate.bufferedBody")

/** The JSON-RPC requests of the buffered body, when it parsed as JSON-RPC at all. */
internal val JSON_RPC_CALLS = AttributeKey<JsonRpcCalls>("McpRequestGate.jsonRpcCalls")

private const val CORS_ALLOWED_HEADERS = "Content-Type, Accept, Authorization, Mcp-Session-Id, MCP-Protocol-Version, Last-Event-ID"
private const val CORS_EXPOSED_HEADERS = "Mcp-Session-Id, MCP-Protocol-Version"
private const val CORS_MAX_AGE_SECONDS = 600

private suspend fun ApplicationCall.reject(decision: GateDecision.Reject) {
    respondText(
        McpErrors.error(null, decision.code, decision.message).toString(),
        ContentType.Application.Json,
        HttpStatusCode.fromValue(decision.status),
    )
}

private fun ApplicationResponse.appendCorsHeaders(origin: String) {
    header(HttpHeaders.AccessControlAllowOrigin, origin)
    header(HttpHeaders.Vary, HttpHeaders.Origin)
    header(HttpHeaders.AccessControlExposeHeaders, CORS_EXPOSED_HEADERS)
}
