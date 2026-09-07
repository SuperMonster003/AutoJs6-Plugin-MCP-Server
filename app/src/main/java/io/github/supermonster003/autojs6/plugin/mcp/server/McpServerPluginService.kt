package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Entry point the AutoJs6 host binds to (action `org.autojs.plugin.MCP_SERVER`, category
 * `mcp-server`) in the dedicated `:mcp_server` process that will also host the HTTP listener.
 *
 * Until roadmap P1.1 stages the `IMcpServerPlugin` AIDL from the host's `mcp-server-api`
 * module, the service returns a placeholder Binder that only carries the contract descriptor,
 * so discovery, permission enforcement, and cross-process binding are already verifiable.
 */
class McpServerPluginService : Service() {

    private val binder = object : Binder() {
        init {
            attachInterface(null, McpServerPlugin.SERVICE_DESCRIPTOR)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
