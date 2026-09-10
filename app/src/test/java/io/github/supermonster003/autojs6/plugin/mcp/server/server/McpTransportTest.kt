package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.shared.RequestHandlerExtra
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotification
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotificationParams
import io.modelcontextprotocol.kotlin.sdk.types.RequestId
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the real SDK transport behind the request gate on Ktor's test engine: the stateful
 * session flow, the deterministic tool order, the server-sent-event responses that carry a
 * request's own notifications, and every rejection the gate adds.
 */
class McpTransportTest {

    @Volatile
    private var policy: GatePolicy = GatePolicy.loopback()

    private fun mcpServer(vararg toolNames: String): Server = McpServerFactory.create("1.0.0-test").also { server ->
        toolNames.forEach { name ->
            server.addTool(name = name, description = "Test tool $name") {
                CallToolResult(content = listOf(TextContent(text = "ok $name")))
            }
        }
    }

    private fun ApplicationTestBuilder.mount(server: Server = mcpServer("zeta_tool", "alpha_tool")) {
        application { mcpServerModule(server, policy = { policy }) }
    }

    @Test
    fun statefulSessionNegotiatesIdentityAndListsToolsInRegistrationOrder() = testApplication {
        mount()
        val init = post(INITIALIZE, sessionId = null)
        assertEquals(HttpStatusCode.OK, init.status)
        val sessionId = init.headers[SESSION_HEADER]
        assertNotNull("initialize must return Mcp-Session-Id", sessionId)
        val result = init.bodyJson()["result"]!!.jsonObject
        assertEquals(PROTOCOL_VERSION, result["protocolVersion"]!!.jsonPrimitive.content)
        val serverInfo = result["serverInfo"]!!.jsonObject
        assertEquals(McpServerFactory.SERVER_NAME, serverInfo["name"]!!.jsonPrimitive.content)
        assertEquals("1.0.0-test", serverInfo["version"]!!.jsonPrimitive.content)
        val capabilities = result["capabilities"]!!.jsonObject
        assertTrue(capabilities["tools"]!!.jsonObject["listChanged"]!!.jsonPrimitive.boolean)
        assertTrue(capabilities.containsKey("resources"))
        assertTrue(capabilities.containsKey("prompts"))

        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)

        val list = post(TOOLS_LIST, sessionId)
        assertEquals(HttpStatusCode.OK, list.status)
        val names = list.bodyJson()["result"]!!.jsonObject["tools"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertEquals(listOf("zeta_tool", "alpha_tool"), names)

        val call = post(toolsCall("alpha_tool"), sessionId)
        assertEquals(HttpStatusCode.OK, call.status)
        val content = call.bodyJson()["result"]!!.jsonObject["content"]!!.jsonArray.first().jsonObject
        assertEquals("ok alpha_tool", content["text"]!!.jsonPrimitive.content)

        val close = client.delete(PATH) {
            header(HttpHeaders.Host, DEFAULT_HOST)
            header(SESSION_HEADER, sessionId)
            header(PROTOCOL_HEADER, PROTOCOL_VERSION)
        }
        assertEquals(HttpStatusCode.OK, close.status)
        assertEquals(HttpStatusCode.NotFound, post(TOOLS_LIST, sessionId).status)
    }

    @Test
    fun postResponsesStreamAsEventsWithTheirRelatedNotificationsFirst() = testApplication {
        val server = mcpServer("plain_tool").also { server ->
            server.addTool(name = "slow_tool", description = "sends a related notification before its result") { request ->
                // The transport returns from the POST before this handler runs; the delay makes that visible.
                delay(SLOW_TOOL_MS)
                val related = currentCoroutineContext()[RequestHandlerExtra.Key]?.requestId
                val token = request.meta?.progressToken ?: RequestId.StringId("none")
                notification(ProgressNotification(ProgressNotificationParams(token, 1.0, 2.0, "half way")), related)
                CallToolResult(content = listOf(TextContent(text = "ok slow_tool")))
            }
        }
        mount(server)
        val init = post(INITIALIZE, sessionId = null)
        assertEquals(HttpStatusCode.OK, init.status)
        assertTrue("initialize streams: ${init.contentType()}", init.contentType()?.match(ContentType.Text.EventStream) == true)
        val sessionId = init.headers[SESSION_HEADER]!!
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)

        val call = post(
            """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"slow_tool","arguments":{},"_meta":{"progressToken":"hb"}}}""",
            sessionId,
        )
        assertEquals(HttpStatusCode.OK, call.status)
        assertTrue("tools/call streams: ${call.contentType()}", call.contentType()?.match(ContentType.Text.EventStream) == true)
        val events = call.bodyEvents()
        assertEquals(listOf("notifications/progress", null), events.map { it["method"]?.jsonPrimitive?.contentOrNull })
        val progress = events[0]["params"]!!.jsonObject
        assertEquals("hb", progress["progressToken"]!!.jsonPrimitive.content)
        assertEquals("half way", progress["message"]!!.jsonPrimitive.content)
        assertEquals(7, events[1]["id"]!!.jsonPrimitive.int)
        val content = events[1]["result"]!!.jsonObject["content"]!!.jsonArray.first().jsonObject
        assertEquals("ok slow_tool", content["text"]!!.jsonPrimitive.content)

        // A tool without a notification still answers on a stream with the response alone.
        assertEquals(1, post(toolsCall("plain_tool"), sessionId).bodyEvents().size)
    }

    @Test
    fun hostOutsideThePolicyIsRefusedWithAJsonRpcError() = testApplication {
        mount()
        val response = post(INITIALIZE, sessionId = null, host = "evil.example:9637")
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.contentType()?.match(ContentType.Application.Json) == true)
        val error = response.bodyJson()["error"]!!.jsonObject
        assertEquals(RequestGate.JSON_RPC_INVALID_REQUEST, error["code"]!!.jsonPrimitive.int)
        assertTrue(error["message"]!!.jsonPrimitive.content.contains("Host"))
    }

    @Test
    fun lanAddressesAreHonouredAfterARefreshWithoutRestarting() = testApplication {
        mount()
        policy = GatePolicy.loopback()
        assertEquals(HttpStatusCode.Forbidden, post(INITIALIZE, sessionId = null, host = "192.168.1.20:9637").status)
        policy = GatePolicy.lan(listOf("192.168.1.20"))
        assertEquals(HttpStatusCode.OK, post(INITIALIZE, sessionId = null, host = "192.168.1.20:9637").status)
        assertEquals(HttpStatusCode.Forbidden, post(INITIALIZE, sessionId = null, host = "192.168.1.21:9637").status)
    }

    @Test
    fun browserOriginsAreRefusedUnlessDeveloperModeAllowsTheLoopbackOrigin() = testApplication {
        mount()
        val origin = "http://localhost:6274"
        policy = GatePolicy.loopback()
        assertEquals(HttpStatusCode.Forbidden, post(INITIALIZE, sessionId = null, origin = origin).status)
        assertEquals(
            HttpStatusCode.Forbidden,
            client.options(PATH) {
                header(HttpHeaders.Host, DEFAULT_HOST)
                header(HttpHeaders.Origin, origin)
            }.status,
        )

        policy = GatePolicy.loopback(developerMode = true)
        val init = post(INITIALIZE, sessionId = null, origin = origin)
        assertEquals(HttpStatusCode.OK, init.status)
        assertEquals(origin, init.headers[HttpHeaders.AccessControlAllowOrigin])
        assertTrue(init.headers[HttpHeaders.AccessControlExposeHeaders].orEmpty().contains(SESSION_HEADER))
        assertEquals(HttpStatusCode.Forbidden, post(INITIALIZE, sessionId = null, origin = "http://evil.example").status)

        val preflight = client.options(PATH) {
            header(HttpHeaders.Host, DEFAULT_HOST)
            header(HttpHeaders.Origin, origin)
            header(HttpHeaders.AccessControlRequestMethod, "POST")
        }
        assertEquals(HttpStatusCode.NoContent, preflight.status)
        assertEquals(origin, preflight.headers[HttpHeaders.AccessControlAllowOrigin])
        assertTrue(preflight.headers[HttpHeaders.AccessControlAllowMethods].orEmpty().contains("POST"))
        assertTrue(preflight.headers[HttpHeaders.AccessControlAllowHeaders].orEmpty().contains(SESSION_HEADER))
    }

    @Test
    fun requestBodiesAboveOneMebibyteAreRefused() = testApplication {
        mount()
        val padding = "x".repeat(RequestGate.MAX_REQUEST_BODY_BYTES.toInt())
        val body = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"pad":"$padding"}}"""
        assertEquals(HttpStatusCode.PayloadTooLarge, post(body, sessionId = null).status)

        val bytes = body.toByteArray()
        val streamed = client.post(PATH) {
            header(HttpHeaders.Host, DEFAULT_HOST)
            header(HttpHeaders.Accept, ACCEPT)
            header(PROTOCOL_HEADER, PROTOCOL_VERSION)
            setBody(object : OutgoingContent.WriteChannelContent() {
                override val contentType: ContentType = ContentType.Application.Json
                override suspend fun writeTo(channel: ByteWriteChannel) = channel.writeFully(bytes)
            })
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, streamed.status)

        assertEquals(HttpStatusCode.OK, post(INITIALIZE, sessionId = null).status)
    }

    private suspend fun ApplicationTestBuilder.post(
        body: String,
        sessionId: String?,
        host: String? = null,
        origin: String? = null,
    ): HttpResponse = client.post(PATH) {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Accept, ACCEPT)
        header(PROTOCOL_HEADER, PROTOCOL_VERSION)
        sessionId?.let { header(SESSION_HEADER, it) }
        header(HttpHeaders.Host, host ?: DEFAULT_HOST)
        origin?.let { header(HttpHeaders.Origin, it) }
        setBody(body)
    }

    /** Every JSON document of the response: the `data:` payloads of a stream, or the one JSON body. */
    private suspend fun HttpResponse.bodyEvents(): List<JsonObject> {
        val text = bodyAsText()
        val payloads = if (contentType()?.match(ContentType.Text.EventStream) == true) {
            text.lineSequence().filter { it.startsWith("data:") }.map { it.removePrefix("data:").trim() }.filter { it.isNotEmpty() }.toList()
        } else {
            listOf(text)
        }
        return payloads.map { Json.parseToJsonElement(it).jsonObject }
    }

    /** The JSON-RPC response: the last document of a stream, or the JSON body. */
    private suspend fun HttpResponse.bodyJson(): JsonObject = bodyEvents().last()

    private fun toolsCall(name: String): String =
        """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"$name","arguments":{}}}"""

    private companion object {
        const val PATH = "/mcp"

        /** Ktor's test client sends no Host header of its own; HTTP/1.1 clients always do. */
        const val DEFAULT_HOST = "localhost"
        const val ACCEPT = "application/json, text/event-stream"
        const val PROTOCOL_HEADER = "MCP-Protocol-Version"
        const val SESSION_HEADER = "Mcp-Session-Id"
        const val PROTOCOL_VERSION = "2025-06-18"
        const val INITIALIZE = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"McpTransportTest","version":"1"}}}"""
        const val INITIALIZED = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""
        const val TOOLS_LIST = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""
        const val SLOW_TOOL_MS = 300L
    }
}
