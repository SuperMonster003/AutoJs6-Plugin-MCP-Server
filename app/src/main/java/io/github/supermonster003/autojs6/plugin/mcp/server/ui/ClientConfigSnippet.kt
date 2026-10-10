package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI

enum class McpClient { CLAUDE_CODE, CURSOR, VS_CODE, CODEX, GENERIC }

/**
 * Never persist or log this value: [environmentCommand], [token] and every [text] except the Codex and
 * VS Code files contain a credential. Those two files reference the token instead of embedding it.
 */
data class ClientConfigSnippet(val text: String, val environmentCommand: String? = null, val token: String? = null) {
    companion object {
        const val TOKEN_ENV = "AUTOJS6_MCP_TOKEN"

        /** VS Code prompts for this input when the server first starts and keeps the value out of mcp.json. */
        const val VS_CODE_TOKEN_INPUT = "autojs6-token"
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
                // VS Code syncs mcp.json with Settings Sync, so the token goes through an input variable.
                McpClient.VS_CODE -> ClientConfigSnippet(json.encodeToString(JsonObject.serializer(), buildJsonObject {
                    putJsonArray("inputs") {
                        addJsonObject {
                            put("type", "promptString")
                            put("id", VS_CODE_TOKEN_INPUT)
                            put("description", "AutoJs6 MCP Server token")
                            put("password", true)
                        }
                    }
                    putJsonObject("servers") {
                        putJsonObject("autojs6") {
                            put("type", "http")
                            put("url", endpoint)
                            putJsonObject("headers") { put("Authorization", "Bearer \${input:$VS_CODE_TOKEN_INPUT}") }
                        }
                    }
                }), token = token)
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
