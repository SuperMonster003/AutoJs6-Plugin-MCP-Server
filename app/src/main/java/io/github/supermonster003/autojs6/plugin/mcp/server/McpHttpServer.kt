package io.github.supermonster003.autojs6.plugin.mcp.server

import android.content.Context
import android.util.Log
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStatelessStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities

/**
 * Owns one Ktor CIO listener bound to the loopback interface and the MCP [Server] mounted on it at
 * [McpServerPlugin.ENDPOINT_PATH] over Streamable HTTP (roadmap decision D9).
 *
 * Roadmap P0.2 registers the single `device_ping` tool; P2 adds the bearer token, pairing, and the
 * tool catalog, and P5.2 makes the bind address configurable for the opt-in LAN path.
 */
class McpHttpServer(private val context: Context) {

    private var engine: EmbeddedServer<*, *>? = null

    val isRunning: Boolean
        @Synchronized get() = engine != null

    /**
     * Starts the listener on `127.0.0.1:[port]`; a second call while running is a no-op.
     *
     * [stateless] mounts the SDK's session-less transport instead of the session-based one. It
     * exists to measure the SDK's behaviour for roadmap decision D9 and is not a product mode.
     */
    @Synchronized
    fun start(port: Int, stateless: Boolean = false) {
        if (engine != null) return
        val info = context.mcpServerPluginRuntimeInfo()
        val mcpServer = Server(
            serverInfo = Implementation(name = McpServerPlugin.ID, version = info.versionName),
            options = ServerOptions(
                capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false)),
            ),
        )
        DevicePingTool.register(mcpServer, context, info)
        val startedAt = System.currentTimeMillis()
        engine = embeddedServer(CIO, host = LOOPBACK_HOST, port = port) {
            if (stateless) {
                mcpStatelessStreamableHttp(path = McpServerPlugin.ENDPOINT_PATH) { mcpServer }
            } else {
                mcpStreamableHttp(path = McpServerPlugin.ENDPOINT_PATH) { mcpServer }
            }
        }.start(wait = false)
        val mode = if (stateless) "stateless" else "stateful"
        Log.i(TAG, "MCP endpoint listening on ${endpointUrl(port)} [$mode] (${System.currentTimeMillis() - startedAt} ms to bind)")
    }

    @Synchronized
    fun stop() {
        val running = engine ?: return
        engine = null
        running.stop(gracePeriodMillis = 250, timeoutMillis = 1_000)
        Log.i(TAG, "MCP endpoint stopped")
    }

    companion object {

        const val LOOPBACK_HOST = "127.0.0.1"

        private const val TAG = "McpHttpServer"

        fun endpointUrl(port: Int): String = "http://$LOOPBACK_HOST:$port${McpServerPlugin.ENDPOINT_PATH}"
    }
}
