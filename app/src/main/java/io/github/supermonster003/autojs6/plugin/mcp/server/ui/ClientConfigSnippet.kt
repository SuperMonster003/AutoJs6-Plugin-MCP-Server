package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI

enum class McpClient { CLAUDE_CODE, CURSOR, CODEX, GENERIC }

/** Never persist or log this value: all variants except the Codex file contain a credential. */
data class ClientConfigSnippet(val text: String, val environmentCommand: String? = null) {
    companion object {
        const val TOKEN_ENV = "AUTOJS6_MCP_TOKEN"
        private val json = Json { prettyPrint = true }

        fun create(client: McpClient, endpoint: String, token: String): ClientConfigSnippet {
            val uri = URI(endpoint)
            require(uri.scheme == "http" && uri.host != null && uri.port in 1024..65535 && uri.rawPath == "/mcp")
            require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
            require(endpoint.matches(Regex("http://[A-Za-z0-9.\\[\\]:-]+/mcp")))
            require(token.matches(Regex("[A-Za-z0-9_-]{32,256}")))
            return when (client) {
                McpClient.CLAUDE_CODE -> ClientConfigSnippet(
                    "claude mcp add --transport http autojs6 '$endpoint' --header 'Authorization: Bearer $token'",
                )
                McpClient.CODEX -> ClientConfigSnippet(
                    "[mcp_servers.autojs6]\nurl = \"$endpoint\"\nbearer_token_env_var = \"$TOKEN_ENV\"\n",
                    "\$env:$TOKEN_ENV = '$token'",
                )
                McpClient.CURSOR, McpClient.GENERIC -> ClientConfigSnippet(json.encodeToString(JsonObject.serializer(), buildJsonObject {
                    putJsonObject("mcpServers") {
                        putJsonObject("autojs6") {
                            if (client == McpClient.GENERIC) put("type", "http")
                            put("url", endpoint)
                            putJsonObject("headers") { put("Authorization", "Bearer $token") }
                        }
                    }
                }))
            }
        }

        fun adbForward(port: Int): String {
            require(port in 1024..65535)
            return "adb forward tcp:$port tcp:$port"
        }
    }
}
