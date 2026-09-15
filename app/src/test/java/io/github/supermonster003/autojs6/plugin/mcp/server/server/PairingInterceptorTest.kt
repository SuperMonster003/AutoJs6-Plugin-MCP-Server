package io.github.supermonster003.autojs6.plugin.mcp.server.server

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The full stack on Ktor's test engine: request gate, bearer check, pairing gate, and the real
 * SDK transport. Confirms that listing works before pairing, that a gated call is held back
 * with `PAIRING_REQUIRED`, and that confirmation, denial, and revocation behave as the state
 * machine promises when driven through HTTP.
 */
class PairingInterceptorTest {

    private class MemoryRepository : PairedClientRepository {
        val clients = LinkedHashMap<String, PairedClient>()
        override fun all(): List<PairedClient> = clients.values.toList()
        override fun put(client: PairedClient) { clients[client.fingerprint] = client }
        override fun remove(fingerprint: String): Boolean = clients.remove(fingerprint) != null
    }

    private val token = BearerTokens.generate()
    private val repository = MemoryRepository()
    private val gate = PairingGate(repository)

    private fun mcpServer(): Server = McpServerFactory.create("1.0.0-test").also { server ->
        server.addTool(name = "device_ping", description = "Test tool") {
            CallToolResult(content = listOf(TextContent(text = "pong")))
        }
    }

    private fun ApplicationTestBuilder.mount() {
        application { mcpServerModule(mcpServer(), policy = { GatePolicy.loopback() }, tokenProvider = { token }, pairingGate = gate) }
    }

    @Test
    fun gatedCallsWaitForTheConfirmationOnThePhone() = testApplication {
        mount()
        val init = post(INITIALIZE, sessionId = null)
        assertEquals(HttpStatusCode.OK, init.status)
        val sessionId = init.headers[SESSION_HEADER]
        assertNotNull(sessionId)
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId).status)
        val list = post(TOOLS_LIST, sessionId)
        assertEquals(HttpStatusCode.OK, list.status)
        assertNull("listing needs no pairing", list.bodyJson()["error"])

        val first = post(TOOLS_CALL, sessionId)
        assertEquals(HttpStatusCode.OK, first.status)
        assertTrue(first.contentType()?.match(ContentType.Application.Json) == true)
        val error = first.bodyJson()["error"]!!.jsonObject
        assertEquals(McpErrors.PAIRING_REQUIRED, error["code"]!!.jsonPrimitive.int)
        val message = error["message"]!!.jsonPrimitive.content
        assertTrue(message, message.contains("PairingInterceptorTest"))
        val data = error["data"]!!.jsonObject
        assertEquals(McpErrors.CODE_PAIRING_REQUIRED, data["code"]!!.jsonPrimitive.content)
        assertEquals("PairingInterceptorTest", data["client"]!!.jsonPrimitive.content)
        assertEquals("loopback", data["addressClass"]!!.jsonPrimitive.content)
        assertTrue(data["firstRequest"]!!.jsonPrimitive.boolean)
        val fingerprint = data["fingerprint"]!!.jsonPrimitive.content
        assertEquals(3, first.bodyJson()["id"]!!.jsonPrimitive.int)
        assertEquals(1, gate.pendingRequests().size)
        assertEquals("PairingInterceptorTest", gate.pendingRequests().single().client.name)
        assertEquals("1", gate.pendingRequests().single().client.version)

        val second = post(TOOLS_CALL, sessionId)
        val secondData = second.bodyJson()["error"]!!.jsonObject["data"]!!.jsonObject
        assertEquals(false, secondData["firstRequest"]!!.jsonPrimitive.boolean)
        assertEquals(1, gate.pendingRequests().size)

        assertTrue(gate.approve(fingerprint))
        val call = post(TOOLS_CALL, sessionId)
        assertEquals(HttpStatusCode.OK, call.status)
        val content = call.bodyJson()["result"]!!.jsonObject["content"]!!.jsonArray.first().jsonObject
        assertEquals("pong", content["text"]!!.jsonPrimitive.content)
        assertEquals("PairingInterceptorTest", repository.clients.getValue(fingerprint).name)

        assertTrue(gate.revoke(fingerprint))
        val afterRevoke = post(TOOLS_CALL, sessionId).bodyJson()["error"]!!.jsonObject
        assertEquals(McpErrors.PAIRING_REQUIRED, afterRevoke["code"]!!.jsonPrimitive.int)

        assertTrue(gate.deny(fingerprint))
        val denied = post(TOOLS_CALL, sessionId).bodyJson()["error"]!!.jsonObject
        assertEquals(McpErrors.PAIRING_DENIED, denied["code"]!!.jsonPrimitive.int)
        assertEquals(PairingGate.REASON_DENIED, denied["data"]!!.jsonObject["reason"]!!.jsonPrimitive.content)
        assertEquals(HttpStatusCode.OK, post(TOOLS_LIST, sessionId).status)
    }

    @Test
    fun aSessionIdOfAPreviousListenerIsAnsweredWithNotFoundAndRaisesNoPairingPrompt() = testApplication {
        mount()
        val stale = post(TOOLS_CALL, sessionId = "session-of-a-previous-listener-process")
        assertEquals(HttpStatusCode.NotFound, stale.status)
        assertEquals("Session not found", stale.bodyJson()["error"]!!.jsonObject["message"]!!.jsonPrimitive.content)
        assertTrue("a stale session id must not open a pairing request", gate.pendingRequests().isEmpty())

        val init = post(INITIALIZE, sessionId = null)
        assertEquals(HttpStatusCode.OK, init.status)
        val required = post(TOOLS_CALL, init.headers[SESSION_HEADER]).bodyJson()["error"]!!.jsonObject
        assertEquals(McpErrors.PAIRING_REQUIRED, required["code"]!!.jsonPrimitive.int)
        assertEquals("PairingInterceptorTest", gate.pendingRequests().single().client.name)
    }

    @Test
    fun clientsWithoutASessionAreNamedByTheirUserAgent() = testApplication {
        mount()
        val response = client.post(PATH) {
            header(HttpHeaders.Host, "localhost")
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ACCEPT)
            header(HttpHeaders.UserAgent, "curl/8.0")
            contentType(ContentType.Application.Json)
            setBody(TOOLS_CALL)
        }
        val data = response.bodyJson()["error"]!!.jsonObject["data"]!!.jsonObject
        assertEquals("curl/8.0", data["client"]!!.jsonPrimitive.content)
        assertEquals("curl/8.0", gate.pendingRequests().single().client.name)
    }

    @Test
    fun batchesWithGatedCallsAreAnsweredPerRequest() = testApplication {
        mount()
        val batch = """[$TOOLS_LIST,$TOOLS_CALL,{"jsonrpc":"2.0","method":"notifications/initialized"}]"""
        val response = client.post(PATH) {
            header(HttpHeaders.Host, "localhost")
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ACCEPT)
            contentType(ContentType.Application.Json)
            setBody(batch)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val documents = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(listOf(2, 3), documents.map { it.jsonObject["id"]!!.jsonPrimitive.int })
        assertTrue(documents.all { it.jsonObject["error"]!!.jsonObject["code"]!!.jsonPrimitive.int == McpErrors.PAIRING_REQUIRED })
    }

    @Test
    fun jsonRpcCallsAreParsedTolerantly() {
        val single = JsonRpcCalls.parse(TOOLS_CALL)!!
        assertEquals(listOf("tools/call"), single.methods)
        assertEquals(false, single.batch)
        assertTrue(single.hasGated(gate))
        val notification = JsonRpcCalls.parse(INITIALIZED)!!
        assertTrue(notification.ids.isEmpty())
        val batch = JsonRpcCalls.parse("[$TOOLS_LIST,$INITIALIZED]")!!
        assertEquals(listOf("tools/list"), batch.methods)
        assertEquals(true, batch.batch)
        assertEquals(false, batch.hasGated(gate))
        assertNull(JsonRpcCalls.parse("not json"))
        assertNull(JsonRpcCalls.parse("42"))
        assertEquals(0, JsonRpcCalls.parse("{}")!!.ids.size)
    }

    private suspend fun ApplicationTestBuilder.post(body: String, sessionId: String?): HttpResponse = client.post(PATH) {
        header(HttpHeaders.Host, "localhost")
        header(HttpHeaders.Authorization, "Bearer $token")
        header(HttpHeaders.Accept, ACCEPT)
        header(PROTOCOL_HEADER, PROTOCOL_VERSION)
        contentType(ContentType.Application.Json)
        sessionId?.let { header(SESSION_HEADER, it) }
        setBody(body)
    }

    private suspend fun HttpResponse.bodyJson(): JsonObject {
        val text = bodyAsText()
        val payload = if (contentType()?.match(ContentType.Text.EventStream) == true) {
            text.lineSequence().filter { it.startsWith("data:") }.map { it.removePrefix("data:").trim() }.first { it.isNotEmpty() }
        } else {
            text
        }
        return Json.parseToJsonElement(payload).jsonObject
    }

    private companion object {
        const val PATH = "/mcp"
        const val ACCEPT = "application/json, text/event-stream"
        const val PROTOCOL_HEADER = "MCP-Protocol-Version"
        const val SESSION_HEADER = "Mcp-Session-Id"
        const val PROTOCOL_VERSION = "2025-06-18"
        const val INITIALIZE = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"PairingInterceptorTest","version":"1"}}}"""
        const val INITIALIZED = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""
        const val TOOLS_LIST = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""
        const val TOOLS_CALL = """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"device_ping","arguments":{}}}"""
    }
}
