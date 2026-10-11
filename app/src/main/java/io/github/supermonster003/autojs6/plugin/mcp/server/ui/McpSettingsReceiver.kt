package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.github.supermonster003.autojs6.plugin.mcp.server.host.McpServerRuntime
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingCoordinator
import org.autojs.plugin.mcp.server.api.McpServerContract

/** Explicit, unexported control for this app's settings page; it never starts an idle listener. */
class McpSettingsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SHOW_PENDING_PAIRING) {
            PairingCoordinator.instance?.showPending()
            return
        }
        if (intent.action !in setOf(APPLY, STOP, REFRESH)) return
        val pending = goAsync()
        McpServerRuntime.get(context).applySettingsFromUi(stopRequested = intent.action == STOP, applyConfiguration = intent.action == APPLY) {
            pending.finish()
        }
    }
    companion object {
        const val APPLY = McpServerPlugin.PACKAGE_NAME + ".action.APPLY_SETTINGS"
        const val STOP = McpServerPlugin.PACKAGE_NAME + ".action.STOP_FROM_SETTINGS"
        const val REFRESH = McpServerPlugin.PACKAGE_NAME + ".action.REFRESH_SETTINGS"
        /** Re-shows a pending pairing confirmation while the settings page is in front. */
        const val SHOW_PENDING_PAIRING = McpServerPlugin.PACKAGE_NAME + ".action.SHOW_PENDING_PAIRING"
        fun send(context: Context, action: String) = context.sendBroadcast(Intent(context, McpSettingsReceiver::class.java).setAction(action))
    }
}
