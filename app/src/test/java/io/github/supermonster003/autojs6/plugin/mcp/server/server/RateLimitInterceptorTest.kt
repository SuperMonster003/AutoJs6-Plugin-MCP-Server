package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rate limiter behind the request gate on Ktor's test engine (roadmap P6): the per-second
 * request window answers `429` with `Retry-After` and a JSON-RPC `RATE_LIMITED` error, the
 * per-minute screenshot window answers a `tools/call` of `screen_capture` as an `isError` tool
 * result with `retryAfterMs`, and both recover once the window moved on.
 */
class RateLimitInterceptorTest {

    private var now = 5_000_000L

    private fun server(): Server = McpServerFactory.create("1.0.0-test").also { server ->
        listOf("screen_capture", "device_ping").forEach { name ->
            server.addTool(name = name, description = "Test tool $name") {
                CallToolResult(content = listOf(TextContent(text = "ok $name")))
            }
        }
    }

    private fun ApplicationTestBuilder.mount(limits: RateLimits) {
        val server = server()
        application {
            mcpServerModule(server, policy = { GatePolicy.loopback() }, rateLimiter = RateLimiter(limits) { now })
        }
    }

    @Test
    fun requestsAboveThePerSecondWindowAreRefusedWith429AndRecover() = testApplication {
        mount(RateLimits(requestsPerWindow = 3, requestWindowMs = 1_000L))
        val init = post(INITIALIZE, sessionId = null)
        assertEquals(HttpStatusCode.OK, init.status)
        val sessionId = init.headers[SESSION_HEADER]
        // Before initialize the client is known by its User-Agent, afterwards by its clientInfo name:
        // the session's own window starts with the initialized notification.
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)
        assertEquals(HttpStatusCode.OK, post(TOOLS_LIST, sessionId).status)
        assertEquals(HttpStatusCode.OK, post(TOOLS_LIST, sessionId).status)

        now += 250
        val refused = post(TOOLS_LIST, sessionId)
        assertEquals(HttpStatusCode.TooManyRequests, refused.status)
        assertEquals("1", refused.headers[HttpHeaders.RetryAfter])
        assertEquals(ContentType.Application.Json, refused.contentType()?.withoutParameters())
        val refusedDocument = refused.bodyJson()
        val error = refusedDocument["error"]!!.jsonObject
        assertEquals(McpErrors.RATE_LIMITED, error["code"]!!.jsonPrimitive.int)
        assertTrue(error["message"]!!.jsonPrimitive.content.startsWith("RATE_LIMITED: at most 3 requests per 1 s per client"))
        val data = error["data"]!!.jsonObject
        assertEquals("RATE_LIMITED", data["code"]!!.jsonPrimitive.content)
        assertEquals(750L, data["retryAfterMs"]!!.jsonPrimitive.long)
        assertEquals(3, data["limit"]!!.jsonPrimitive.int)
        assertEquals(1_000L, data["windowMs"]!!.jsonPrimitive.long)
        assertEquals("the request id is echoed", 2, refusedDocument["id"]!!.jsonPrimitive.int)

        val refusedDelete = client.delete(PATH) {
            header(HttpHeaders.Host, DEFAULT_HOST)
            header(SESSION_HEADER, sessionId!!)
        }
        assertEquals("a request without a body is refused with a null id", HttpStatusCode.TooManyRequests, refusedDelete.status)
        assertTrue(refusedDelete.bodyAsText().contains("\"id\":null"))

        now += 1_000
        assertEquals("the window moved on", HttpStatusCode.OK, post(TOOLS_LIST, sessionId).status)
    }

    @Test
    fun screenshotsAboveThePerMinuteWindowBecomeAToolErrorWithRetryAfterMs() = testApplication {
        mount(RateLimits(requestsPerWindow = 100, requestWindowMs = 1_000L, screenshotsPerWindow = 2, screenshotWindowMs = 60_000L))
        val init = post(INITIALIZE, sessionId = null)
        val sessionId = init.headers[SESSION_HEADER]
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)

        repeat(2) {
            val ok = post(toolsCall("screen_capture", id = 10 + it), sessionId).bodyJson()["result"]!!.jsonObject
            assertNull(ok["isError"])
        }
        now += 15_000
        val refused = post(toolsCall("screen_capture", id = 12), sessionId)
        assertEquals("a tool result, not a transport error", HttpStatusCode.OK, refused.status)
        val document = refused.bodyJson()
        assertEquals(12, document["id"]!!.jsonPrimitive.int)
        val result = document["result"]!!.jsonObject
        assertTrue(result["isError"]!!.jsonPrimitive.boolean)
        val text = result["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        assertEquals("RATE_LIMITED: screen_capture is limited to 2 calls per 60 s per client (wait 45000 ms before calling it again)", text)
        val error = result["structuredContent"]!!.jsonObject["error"]!!.jsonObject
        assertEquals("RATE_LIMITED", error["code"]!!.jsonPrimitive.content)
        assertEquals(45_000L, error["retryAfterMs"]!!.jsonPrimitive.long)
        assertEquals("rate-limited", error["category"]!!.jsonPrimitive.content)

        val other = post(toolsCall("device_ping", id = 13), sessionId).bodyJson()["result"]!!.jsonObject
        assertNull("other tools are not counted", other["isError"])

        now += 45_000
        val again = post(toolsCall("screen_capture", id = 14), sessionId).bodyJson()["result"]!!.jsonObject
        assertNull("the oldest screenshot left the window", again["isError"])
    }

    @Test
    fun anInventedSessionIdDoesNotOpenAFreshWindow() = testApplication {
        mount(RateLimits(requestsPerWindow = 2, requestWindowMs = 1_000L))
        assertEquals(HttpStatusCode.OK, post(INITIALIZE, sessionId = null).status)
        assertEquals(HttpStatusCode.OK, post(INITIALIZE, sessionId = null).status)
        val forged = post(TOOLS_LIST, sessionId = "no-such-session")
        assertEquals(HttpStatusCode.TooManyRequests, forged.status)
        val listed = post(TOOLS_LIST, sessionId = null)
        assertEquals(HttpStatusCode.TooManyRequests, listed.status)
        assertFalse(listed.bodyAsText().contains("Session not found"))
    }

    private suspend fun ApplicationTestBuilder.post(body: String, sessionId: String?): HttpResponse = client.post(PATH) {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Accept, ACCEPT)
        header(PROTOCOL_HEADER, PROTOCOL_VERSION)
        header(HttpHeaders.UserAgent, "RateLimitInterceptorTest/1")
        sessionId?.let { header(SESSION_HEADER, it) }
        header(HttpHeaders.Host, DEFAULT_HOST)
        setBody(body)
    }

    private suspend fun HttpResponse.bodyJson(): JsonObject {
        val text = bodyAsText()
        val payload = if (contentType()?.match(ContentType.Text.EventStream) == true) {
            text.lineSequence().filter { it.startsWith("data:") }.map { it.removePrefix("data:").trim() }.last { it.isNotEmpty() }
        } else {
            text
        }
        val document = Json.parseToJsonElement(payload).jsonObject
        assertNotNull(document)
        return document
    }

    private fun toolsCall(name: String, id: Int): String =
        """{"jsonrpc":"2.0","id":$id,"method":"tools/call","params":{"name":"$name","arguments":{}}}"""

    private companion object {
        const val PATH = "/mcp"
        const val DEFAULT_HOST = "localhost"
        const val ACCEPT = "application/json, text/event-stream"
        const val PROTOCOL_HEADER = "MCP-Protocol-Version"
        const val SESSION_HEADER = "Mcp-Session-Id"
        const val PROTOCOL_VERSION = "2025-06-18"
        const val INITIALIZE = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"RateLimitInterceptorTest","version":"1"}}}"""
        const val INITIALIZED = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""
        const val TOOLS_LIST = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""
    }
}
