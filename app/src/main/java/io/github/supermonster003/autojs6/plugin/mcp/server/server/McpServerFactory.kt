package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities

/**
 * Builds the SDK [Server] with the identity and capabilities every session announces (roadmap
 * P2.1): `autojs6-mcp-server` at the plugin's version, tools with `listChanged` (tool groups can
 * be toggled at runtime, P2.3), and the resources and prompts features that P3.5 fills.
 *
 * Tools keep their registration order in `tools/list` (the SDK registry is insertion ordered),
 * so callers register the catalog in catalog order for stable client caches.
 */
object McpServerFactory {

    const val SERVER_NAME = "autojs6-mcp-server"

    fun create(versionName: String): Server = Server(
        serverInfo = Implementation(name = SERVER_NAME, version = versionName),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = false, listChanged = false),
                prompts = ServerCapabilities.Prompts(listChanged = false),
            ),
        ),
    )
}
