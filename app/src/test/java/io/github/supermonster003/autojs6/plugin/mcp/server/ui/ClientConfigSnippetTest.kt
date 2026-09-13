package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class ClientConfigSnippetTest {
    private val token = "test_" + "0".repeat(43)

    @Test fun `all client outputs match the copyable snapshots`() {
        val expected = Json.parseToJsonElement(javaClass.getResource("/client-config.snapshot.json")!!.readText()).jsonObject
        McpClient.entries.forEach { client ->
            val actual = ClientConfigSnippet.create(client, "http://127.0.0.1:9637/mcp", token)
            val entry = expected.getValue(client.name).jsonObject
            assertEquals(client.name, entry.getValue("text").jsonPrimitive.content, actual.text)
            assertEquals(entry["environmentCommand"]?.jsonPrimitive?.content, actual.environmentCommand)
        }
    }

    @Test fun `Codex file uses an environment name and never embeds its secret`() {
        val snippet = ClientConfigSnippet.create(McpClient.CODEX, "http://192.168.1.5:10443/mcp", token)
        assertFalse(snippet.text.contains(token))
        assertTrue(snippet.text.contains("bearer_token_env_var = \"AUTOJS6_MCP_TOKEN\""))
        assertTrue(snippet.environmentCommand!!.endsWith("'$token'"))
    }

    @Test fun `JSON headers and IPv6 survive serialization unchanged`() {
        for (client in listOf(McpClient.CURSOR, McpClient.GENERIC)) {
            val text = ClientConfigSnippet.create(client, "http://[::1]:9637/mcp", token).text
            val server = Json.parseToJsonElement(text).jsonObject.getValue("mcpServers").jsonObject.getValue("autojs6").jsonObject
            assertEquals("http://[::1]:9637/mcp", server.getValue("url").jsonPrimitive.content)
            assertEquals("Bearer $token", server.getValue("headers").jsonObject.getValue("Authorization").jsonPrimitive.content)
        }
    }

    @Test fun `untrusted endpoint and token text cannot become shell instructions`() {
        for (endpoint in listOf("https://localhost:9637/mcp", "http://user@localhost:9637/mcp", "http://localhost:9637/mcp?x=1", "http://localhost:80/mcp", "http://localhost:9637/mcp\nwhoami", "http://localhost:9637/mcp#fragment")) {
            assertTrue(endpoint, runCatching { ClientConfigSnippet.create(McpClient.CLAUDE_CODE, endpoint, token) }.isFailure)
        }
        assertTrue(runCatching { ClientConfigSnippet.create(McpClient.CLAUDE_CODE, "http://localhost:9637/mcp", "'; whoami; '") }.isFailure)
        assertTrue(runCatching { ClientConfigSnippet.adbForward(65536) }.isFailure)
        assertEquals("adb forward tcp:11000 tcp:11000", ClientConfigSnippet.adbForward(11000))
    }
}
