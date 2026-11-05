package io.github.supermonster003.autojs6.plugin.mcp.server.transfer

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.host.McpServerRuntime
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.store.PairedClientStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfigStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.TokenStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissionStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissions
import io.github.supermonster003.autojs6.plugin.mcp.server.transfer.McpSettingsTransfer.FollowUp
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.McpSettingsReceiver
import org.autojs.plugin.common.api.PluginSettingsTransferProvider
import org.json.JSONObject

/**
 * The settings transfer provider of AutoJs6 (contract v1, authority `<package>.autojs6.settings`, behind the PLUGIN
 * permission; roadmap P13, host settings roadmap P18.2). The document is [McpSettingsTransfer]'s and holds the bearer
 * token and the paired clients, so an export that has either is sensitive.
 *
 * The provider lives in the `:mcp_server` process, next to the listener. After an import that changes the port, the
 * bind scope, the developer mode or the extra host names while the listener runs, it answers `restartNeeded` and
 * [restartProcessAfterImport] ends the process about 500 ms later, unless an activity of that process (the pairing
 * confirmation) is in the foreground; AutoJs6 then binds again and reopens the session, which starts the listener with
 * the imported settings (T30). Other imports apply at once through [McpSettingsReceiver], as a save on the settings
 * page does: the bearer token and the paired clients are read on every request anyway.
 */
class SettingsTransferProvider : PluginSettingsTransferProvider() {

    override val restartProcessAfterImport: Boolean = true

    override fun exportSettings(request: Request): Exported = McpSettingsTransfer(StoreStorage(appContext())).exportSettings()

    override fun importSettings(settings: JSONObject, source: Source, request: Request): Imported {
        val context = appContext()
        val outcome = McpSettingsTransfer(StoreStorage(context)).importSettings(settings)
        val followUp = outcome.followUp(listening = McpServerRuntime.existing?.isListening == true)
        when (followUp) {
            FollowUp.APPLY -> McpSettingsReceiver.send(context, McpSettingsReceiver.APPLY)
            FollowUp.REFRESH -> McpSettingsReceiver.send(context, McpSettingsReceiver.REFRESH)
            FollowUp.RESTART -> Log.i(TAG, "Imported listener settings apply after the process restarts")
            FollowUp.NOTHING -> Unit
        }
        return Imported(outcome.written, outcome.skipped, restartNeeded = followUp == FollowUp.RESTART)
    }

    private fun appContext(): Context = checkNotNull(context) { "The provider is not attached" }.applicationContext

    /** The plugin's documents through their stores. */
    private class StoreStorage(context: Context) : McpSettingsStorage {

        private val config = ServerConfigStore(context)
        private val tools = ToolPermissionStore(context)
        private val tokens = TokenStore(context)
        private val pairings = PairedClientStore(context)

        override fun serverConfig(): ServerConfig? = config.stored()

        override fun writeServerConfig(config: ServerConfig) = this.config.replace(config)

        override fun toolPermissions(): ToolPermissions? = tools.stored()

        override fun writeToolPermissions(permissions: ToolPermissions) = tools.replace(permissions)

        override fun token(): String? = tokens.stored()

        override fun writeToken(token: String) = tokens.replace(token)

        override fun pairedClients(): List<PairedClient> = pairings.all()

        override fun writePairedClients(clients: List<PairedClient>) = pairings.replaceAll(clients)
    }

    private companion object {
        const val TAG = "McpSettingsTransfer"
    }
}
