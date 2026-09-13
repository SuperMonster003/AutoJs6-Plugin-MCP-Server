package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.mcp.server.host.HostCallerVerifier
import io.github.supermonster003.autojs6.plugin.mcp.server.host.McpServerRuntime
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mcp.server.api.IMcpHostCapabilityBroker
import org.autojs.plugin.mcp.server.api.IMcpServerCallback
import org.autojs.plugin.mcp.server.api.IMcpServerPlugin
import org.autojs.plugin.mcp.server.api.IMcpServerSession
import org.autojs.plugin.mcp.server.api.McpServerContract
import org.autojs.plugin.mcp.server.api.McpServerCapabilityKeys
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerLifecycleStore

/**
 * Entry point the AutoJs6 host binds to (action `org.autojs.plugin.MCP_SERVER`, category
 * `mcp-server`) in the `:mcp_server` process that also hosts the HTTP listener (roadmap P2.3).
 *
 * `getInfo` and `getCapabilities` are metadata and answer any caller the manifest permission
 * admits, like the INFO service; `openServer` additionally requires the installed same-signer
 * host (decision D18) and hands the session to [McpServerRuntime].
 */
class McpServerPluginService : Service() {

    private lateinit var verifier: HostCallerVerifier
    private lateinit var runtime: McpServerRuntime

    private val binder = object : IMcpServerPlugin.Stub() {

        override fun getInfo(): PluginInfo = applicationContext.mcpServerPluginRuntimeInfo().toPluginInfo()

        override fun getCapabilities(): Bundle = applicationContext.mcpServerPluginRuntimeInfo().capabilitiesBundle().apply {
            putBoolean(McpServerCapabilityKeys.USER_STOPPED, ServerLifecycleStore(applicationContext).userStopped)
        }

        override fun openServer(config: Bundle?, broker: IMcpHostCapabilityBroker?, callback: IMcpServerCallback?): IMcpServerSession {
            val callerUid = verifier.enforceHost()
            if (broker == null) {
                throw IllegalArgumentException("${McpServerContract.ERROR_INVALID_CONFIG}: a capability broker is required")
            }
            return runtime.openSession(config, broker, callback, callerUid, verifier)
        }
    }

    override fun onCreate() {
        super.onCreate()
        verifier = HostCallerVerifier(this)
        runtime = McpServerRuntime.get(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
