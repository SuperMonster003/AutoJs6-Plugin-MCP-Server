package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginInfo

/** Answers `org.autojs.plugin.INFO` (category `mcp-server`) for the AutoJs6 plugin center. */
class McpServerPluginInfoService : Service() {

    private val binder = object : IPluginInfoProvider.Stub() {
        override fun getInfo(): PluginInfo {
            return mcpServerPluginRuntimeInfo().toPluginInfo().apply {
                // Explicit and auditable: no ABI restriction (see AGENTS.md, PluginInfo rules).
                supportedAbis = emptyArray()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
