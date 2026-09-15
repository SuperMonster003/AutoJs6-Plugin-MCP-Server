package io.github.supermonster003.autojs6.plugin.mcp.server.resources

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.prompts.PromptCatalog
import io.github.supermonster003.autojs6.plugin.mcp.server.server.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Exercises the public SDK session hooks through the real request, auth, pairing, and SSE stack. */
class ResourcePromptTransportTest {
    @Test fun discoveryPairingReadsPromptsAndLiveGroupChangesShareTheGuardedEndpoint() = testApplication {
        val clients = mutableMapOf<String, PairedClient>()
        val gate = PairingGate(object : PairedClientRepository {
            override fun all() = clients.values.toList()
            override fun put(client: PairedClient) { clients[client.fingerprint] = client }
            override fun remove(fingerprint: String) = clients.remove(fingerprint) != null
        })
        var policy = ToolPermissions.DEFAULT
        val methods = mutableListOf<String>()
        val resources = ResourceCatalog({ _ -> BridgeCaller { module, method, _, _, _ ->
            methods += "$module.$method"
            BridgeOutcome.Ok(when (method) {
                "listSamples" -> json("""{"available":true,"entries":[{"path":"Example/hello.js","type":"file"}],"truncated":false}""")
                "read", "readSample" -> json("""{"content":"console.log(1);","encoding":"utf-8","bytes":15,"truncated":false}""")
                else -> json("""{"api":24}""")
            }, 1)
        } }, { policy })
        val server = McpServerFactory.create("1-test")
        resources.install(server) { "resource-client" }
        PromptCatalog({ "Prompt asset: $it" }).install(server)
        val token = BearerTokens.generate()
        application { mcpServerModule(server, policy = { GatePolicy.loopback() }, tokenProvider = { token }, pairingGate = gate) }
        var sessionId: String? = null
        var id = 0
        suspend fun request(method: String, params: String = "{}", authorized: Boolean = true): HttpResponse = client.post("/mcp") {
            header(HttpHeaders.Host, "localhost")
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            if (authorized) header(HttpHeaders.Authorization, "Bearer $token")
            sessionId?.let { header("Mcp-Session-Id", it) }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("jsonrpc", "2.0"); put("method", method)
                if (!method.startsWith("notifications/")) put("id", ++id)
                put("params", json(params))
            }.toString())
        }
        suspend fun rpc(method: String, params: String = "{}"): JsonObject = request(method, params).document()
        val init = request("initialize", """{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"resource-client","version":"1"}}""")
        sessionId = init.headers["Mcp-Session-Id"]
        assertNotNull(sessionId)
        assertFalse(init.document()["result"]!!.jsonObject["capabilities"]!!.jsonObject["resources"]!!.jsonObject["subscribe"]!!.jsonPrimitive.boolean)
        request("notifications/initialized")
        assertEquals(HttpStatusCode.Unauthorized, request("resources/list", authorized = false).status)
        val listing = rpc("resources/list")["result"]!!.jsonObject
        assertEquals(4, listing["resources"]!!.jsonArray.size)
        assertEquals(0, listing["_meta"]!!.jsonObject["ttlMs"]!!.jsonPrimitive.int)
        assertEquals(2, rpc("resources/templates/list")["result"]!!.jsonObject["resourceTemplates"]!!.jsonArray.size)
        assertEquals(3, rpc("prompts/list")["result"]!!.jsonObject["prompts"]!!.jsonArray.size)
        val held = rpc("resources/read", """{"uri":"autojs6://workspace/nested/test.js"}""")["error"]!!.jsonObject
        assertEquals(McpErrors.PAIRING_REQUIRED, held["code"]!!.jsonPrimitive.int)
        assertEquals(McpErrors.PAIRING_REQUIRED, rpc("prompts/get", """{"name":"write_autojs6_script"}""")["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
        // resources/list scans samples and docs; resources/templates/list probes the optional docs provider once.
        assertEquals(listOf("app.listSamples", "app.listDocs", "app.listDocs"), methods)
        assertTrue(gate.approve(held["data"]!!.jsonObject["fingerprint"]!!.jsonPrimitive.content))
        val text = rpc("resources/read", """{"uri":"autojs6://workspace/nested/test.js"}""")["result"]!!.jsonObject["contents"]!!.jsonArray.single().jsonObject
        assertEquals("console.log(1);", text["text"]!!.jsonPrimitive.content)
        assertEquals("files.read", methods.last())
        val prompt = rpc("prompts/get", """{"name":"write_autojs6_script","arguments":{"goal":"Observe only","language":"zh"}}""")["result"]!!.jsonObject
        assertTrue(prompt["messages"]!!.jsonArray.first().jsonObject["content"]!!.jsonObject["text"]!!.jsonPrimitive.content.contains("prompts/zh/"))
        assertEquals(-32602, rpc("prompts/get", """{"name":"automate_task"}""")["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
        assertEquals(-32602, rpc("resources/read", """{"uri":"autojs6://workspace/%2e%2e/a"}""")["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
        assertEquals(-32002, rpc("resources/read", """{"uri":"autojs6://manual/a"}""")["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
        policy = policy.with(ToolGroup.FILES, false)
        assertTrue(rpc("resources/templates/list")["result"]!!.jsonObject["resourceTemplates"]!!.jsonArray.isEmpty())
        val disabled = rpc("resources/read", """{"uri":"autojs6://workspace/a.js"}""")["error"]!!.jsonObject
        assertEquals("TOOL_DISABLED", disabled["data"]!!.jsonObject["code"]!!.jsonPrimitive.content)
        assertEquals(2, rpc("resources/list")["result"]!!.jsonObject["resources"]!!.jsonArray.size)
        assertTrue(gate.revoke(held["data"]!!.jsonObject["fingerprint"]!!.jsonPrimitive.content))
        assertEquals(McpErrors.PAIRING_REQUIRED, rpc("prompts/get", """{"name":"write_autojs6_script"}""")["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
    }

    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject
    private suspend fun HttpResponse.document(): JsonObject {
        val body = bodyAsText()
        val data = body.lineSequence().filter { it.startsWith("data:") }.map { it.removePrefix("data:").trim() }.lastOrNull() ?: body
        return json(data)
    }
}
