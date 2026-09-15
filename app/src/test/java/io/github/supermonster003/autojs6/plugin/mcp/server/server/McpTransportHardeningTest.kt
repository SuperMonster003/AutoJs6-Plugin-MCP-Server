package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.client.request.delete
import io.ktor.client.request.get
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hostile input against the real SDK transport behind the gates on Ktor's test engine (roadmap
 * P6): every case must end in a bounded 4xx or JSON-RPC error, and the listener must answer a
 * normal `initialize` afterwards.
 */
class McpTransportHardeningTest {

    private fun mcpServer(): Server = McpServerFactory.create("1.0.0-test").also { server ->
        server.addTool(name = "alpha_tool", description = "Test tool") {
            CallToolResult(content = listOf(TextContent(text = "ok")))
        }
        server.addTool(name = "slow_tool", description = "Answers after a pause") {
            delay(SLOW_TOOL_MS)
            CallToolResult(content = listOf(TextContent(text = "slow ok")))
        }
    }

    private fun ApplicationTestBuilder.mount(server: Server = mcpServer()) {
        application { mcpServerModule(server, policy = { GatePolicy.loopback() }, rateLimiter = RateLimiter(RateLimits(requestsPerWindow = 1_000))) }
    }

    @Test
    fun deeplyNestedBodiesAreRefusedBeforeAnyParserRecurses() = testApplication {
        mount()
        val nested = "[".repeat(100_000)
        val refused = post(nested, sessionId = null)
        assertEquals(HttpStatusCode.BadRequest, refused.status)
        val error = refused.bodyJson()["error"]!!.jsonObject
        assertEquals(McpErrors.INVALID_REQUEST, error["code"]!!.jsonPrimitive.int)
        assertEquals(RequestBodyChecks.MESSAGE_TOO_DEEP, error["message"]!!.jsonPrimitive.content)

        val deepButAllowed = "[".repeat(RequestBodyChecks.MAX_JSON_DEPTH) + "]".repeat(RequestBodyChecks.MAX_JSON_DEPTH)
        assertTrue("64 levels reach the parser and fail as JSON-RPC, not as structure", post(deepButAllowed, sessionId = null).status.value < 500)
        assertStillAlive()
    }

    @Test
    fun duplicateRequestIdsInOneBodyAreRefused() = testApplication {
        mount()
        val batch = """[{"jsonrpc":"2.0","id":1,"method":"ping"},{"jsonrpc":"2.0","id":1,"method":"ping"}]"""
        val refused = post(batch, sessionId = null)
        assertEquals(HttpStatusCode.BadRequest, refused.status)
        assertEquals(RequestBodyChecks.MESSAGE_DUPLICATE_ID, refused.bodyJson()["error"]!!.jsonObject["message"]!!.jsonPrimitive.content)
        assertStillAlive()
    }

    @Test
    fun aRequestIdStillInFlightOnTheSessionIsRefusedInsteadOfOrphaningTheFirstPost() = testApplication {
        mount()
        val init = post(INITIALIZE, sessionId = null)
        val sessionId = init.headers[SESSION_HEADER]
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)
        val slowCall = """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"slow_tool","arguments":{}}}"""
        val (first, second) = withTimeout(10_000) {
            coroutineScope {
                val first = async { post(slowCall, sessionId) }
                delay(SLOW_TOOL_MS / 3)
                val second = async { post(slowCall, sessionId) }
                first.await() to second.await()
            }
        }
        assertEquals("the first POST is answered", HttpStatusCode.OK, first.status)
        assertEquals("slow ok", first.bodyJson()["result"]!!.jsonObject["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals("the second POST with the same id is refused while the first is in flight", HttpStatusCode.BadRequest, second.status)
        assertEquals(MESSAGE_ID_IN_FLIGHT, second.bodyJson()["error"]!!.jsonObject["message"]!!.jsonPrimitive.content)
        val reused = withTimeout(10_000) { post(slowCall, sessionId) }
        assertEquals("the id is free again once the first POST completed", HttpStatusCode.OK, reused.status)
        assertStillAlive()
    }

    @Test
    fun invalidUtf8TruncatedJsonAndUnknownMethodsAreBoundedErrors() = testApplication {
        mount()
        val garbage = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), '{'.code.toByte(), 0xC3.toByte(), 0x28, '"'.code.toByte(), 0x00, '}'.code.toByte())
        val invalid = postBytes(garbage, sessionId = null)
        assertTrue("invalid UTF-8 ends in a client error: ${invalid.status}", invalid.status.value in 400..499)
        assertTrue(invalid.bodyAsText().contains("error"))

        val truncated = post("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":""", sessionId = null)
        assertTrue("truncated JSON ends in a client error: ${truncated.status}", truncated.status.value in 400..499)

        val init = post(INITIALIZE, sessionId = null)
        val sessionId = init.headers[SESSION_HEADER]
        assertNotNull(sessionId)
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)
        val unknown = post("""{"jsonrpc":"2.0","id":9,"method":"no/such/method","params":{}}""", sessionId)
        assertEquals(HttpStatusCode.OK, unknown.status)
        val error = unknown.bodyJson()["error"]!!.jsonObject
        assertEquals(-32601, error["code"]!!.jsonPrimitive.int)

        val wrongShape = post("""{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":["not","a","string"],"arguments":7}}""", sessionId)
        assertTrue("a malformed tools/call is answered, not dropped: ${wrongShape.status}", wrongShape.status.value < 500)
        assertStillAlive()
    }

    @Test
    fun headerCombinationsAreAnsweredWithoutFallingOver() = testApplication {
        mount()
        val noAccept = client.post(PATH) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Host, DEFAULT_HOST)
            setBody(INITIALIZE)
        }
        // Ktor's client sends Accept: */* when none is set; the transport takes that as acceptable.
        assertTrue("POST without an explicit Accept: ${noAccept.status}", noAccept.status.value < 500)

        val jsonOnlyAccept = client.post(PATH) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Host, DEFAULT_HOST)
            setBody(INITIALIZE)
        }
        assertTrue("POST accepting JSON only: ${jsonOnlyAccept.status}", jsonOnlyAccept.status.value in 400..499)

        val textBody = client.post(PATH) {
            contentType(ContentType.Text.Plain)
            header(HttpHeaders.Accept, ACCEPT)
            header(HttpHeaders.Host, DEFAULT_HOST)
            setBody(INITIALIZE)
        }
        assertTrue("POST with text/plain: ${textBody.status}", textBody.status.value in 400..499)

        assertEquals(HttpStatusCode.BadRequest, client.get(PATH) { header(HttpHeaders.Host, DEFAULT_HOST); header(HttpHeaders.Accept, "text/event-stream") }.status)
        assertEquals(HttpStatusCode.NotFound, client.get(PATH) { header(HttpHeaders.Host, DEFAULT_HOST); header(HttpHeaders.Accept, "text/event-stream"); header(SESSION_HEADER, "nope") }.status)
        assertEquals(HttpStatusCode.BadRequest, client.delete(PATH) { header(HttpHeaders.Host, DEFAULT_HOST) }.status)
        assertEquals(HttpStatusCode.NotFound, client.delete(PATH) { header(HttpHeaders.Host, DEFAULT_HOST); header(SESSION_HEADER, "nope") }.status)
        assertEquals(HttpStatusCode.NotFound, post(TOOLS_LIST, sessionId = "nope").status)
        assertEquals(HttpStatusCode.NotFound, post(TOOLS_LIST, sessionId = "x".repeat(4_096)).status)

        val badVersion = client.post(PATH) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, ACCEPT)
            header(HttpHeaders.Host, DEFAULT_HOST)
            header(PROTOCOL_HEADER, "not-a-version")
            setBody(INITIALIZE)
        }
        assertTrue("an unknown protocol header value is bounded: ${badVersion.status}", badVersion.status.value < 500)
        assertStillAlive()
    }

    @Test
    fun largeBase64ArgumentsAreBoundedByTheBodyCeiling() = testApplication {
        mount()
        val init = post(INITIALIZE, sessionId = null)
        val sessionId = init.headers[SESSION_HEADER]
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)

        val underCeiling = "QUJD".repeat(160_000) // 640 000 characters of base64
        val call = post("""{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"no_such_tool","arguments":{"content":"$underCeiling","encoding":"base64"}}}""", sessionId)
        assertTrue("a big argument to an unknown tool is answered: ${call.status}", call.status.value < 500)
        assertTrue(call.bodyAsText().isNotEmpty())

        val overCeiling = "QUJD".repeat(270_000) // 1 080 000 characters, above 1 MiB with the envelope
        val refused = post("""{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"alpha_tool","arguments":{"content":"$overCeiling"}}}""", sessionId)
        assertEquals(HttpStatusCode.PayloadTooLarge, refused.status)
        assertStillAlive()
    }

    @Test
    fun sixtyFourConcurrentSessionsAllComplete() = testApplication {
        val server = mcpServer()
        mount(server)
        val results = coroutineScope {
            (1..64).map { index ->
                async {
                    val init = post(initialize("Concurrent$index"), sessionId = null)
                    val sessionId = init.headers[SESSION_HEADER] ?: return@async "no session ($index): ${init.status}"
                    val initialized = post(INITIALIZED, sessionId).status
                    val list = post(TOOLS_LIST, sessionId)
                    val tools = list.bodyJson()["result"]?.jsonObject?.get("tools")?.jsonArray?.size
                    val call = post("""{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"alpha_tool","arguments":{}}}""", sessionId)
                    val text = call.bodyJson()["result"]?.jsonObject?.get("content")?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content
                    "${init.status.value} ${initialized.value} ${list.status.value} tools=$tools call=${call.status.value} $text"
                }
            }.awaitAll()
        }
        val expected = "200 202 200 tools=2 call=200 ok"
        val failures = results.filter { it != expected }
        assertTrue("every session completes: $failures", failures.isEmpty())
        assertEquals(64, server.sessions.size)
        assertStillAlive()
    }

    private suspend fun ApplicationTestBuilder.assertStillAlive() {
        val init = post(INITIALIZE, sessionId = null)
        assertEquals("the listener answers a normal initialize afterwards", HttpStatusCode.OK, init.status)
        assertNotNull(init.headers[SESSION_HEADER])
    }

    private suspend fun ApplicationTestBuilder.post(body: String, sessionId: String?): HttpResponse = postBytes(body.toByteArray(Charsets.UTF_8), sessionId)

    private suspend fun ApplicationTestBuilder.postBytes(body: ByteArray, sessionId: String?): HttpResponse = client.post(PATH) {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Accept, ACCEPT)
        header(PROTOCOL_HEADER, PROTOCOL_VERSION)
        header(HttpHeaders.UserAgent, "McpTransportHardeningTest/1")
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
        return Json.parseToJsonElement(payload).jsonObject
    }

    private fun initialize(clientName: String): String =
        """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"$clientName","version":"1"}}}"""

    private companion object {
        const val PATH = "/mcp"
        const val DEFAULT_HOST = "localhost"
        const val ACCEPT = "application/json, text/event-stream"
        const val PROTOCOL_HEADER = "MCP-Protocol-Version"
        const val SESSION_HEADER = "Mcp-Session-Id"
        const val PROTOCOL_VERSION = "2025-06-18"
        const val INITIALIZE = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"McpTransportHardeningTest","version":"1"}}}"""
        const val INITIALIZED = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""
        const val TOOLS_LIST = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""
        const val SLOW_TOOL_MS = 600L
    }
}
