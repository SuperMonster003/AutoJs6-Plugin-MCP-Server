package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respondText

/**
 * Bearer token check for every request that passed the [RequestGate] (roadmap P2.2).
 *
 * A missing, malformed, or wrong `Authorization` header answers `401` with
 * `WWW-Authenticate: Bearer realm="autojs6-mcp-server"` and a JSON-RPC error; the comparison is
 * constant-time and fails closed when [tokenProvider] has no token. Nothing about the presented
 * or the expected token is logged.
 */
fun Application.installBearerAuth(tokenProvider: () -> String?) {
    intercept(ApplicationCallPipeline.Plugins) {
        val presented = BearerTokens.parseAuthorization(call.request.header(HttpHeaders.Authorization))
        if (BearerTokens.constantTimeEquals(tokenProvider(), presented)) return@intercept
        call.response.header(HttpHeaders.WWWAuthenticate, "${BearerTokens.SCHEME} realm=\"${BearerTokens.REALM}\"")
        val message = if (presented == null) {
            "${McpErrors.CODE_UNAUTHORIZED}: send the server token as Authorization: Bearer <token>"
        } else {
            "${McpErrors.CODE_UNAUTHORIZED}: the bearer token is not accepted"
        }
        call.respondText(
            McpErrors.error(null, McpErrors.UNAUTHORIZED, message).toString(),
            ContentType.Application.Json,
            HttpStatusCode.Unauthorized,
        )
        finish()
    }
}
