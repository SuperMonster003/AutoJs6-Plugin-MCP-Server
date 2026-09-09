package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.annotation.SuppressLint
import android.content.Context

/**
 * Persists the [ServerConfig] in the plugin's private preferences (roadmap P2.1).
 *
 * Values are stored as strings and decoded with [ServerConfig.fromMap], so a value written by a
 * newer or older build degrades to the default instead of breaking the server. Writes use
 * `commit()` because the settings page (P4.2) and the `:mcp_server` process are different
 * processes and the server reads the file right after the user saved it.
 */
class ServerConfigStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ServerConfig = ServerConfig.fromMap(ServerConfig.KEYS.associateWith { key -> preferences.getString(key, null) })

    /** Synchronous on purpose: another process reads the file right after the user saved it. */
    @SuppressLint("ApplySharedPref")
    fun save(config: ServerConfig) {
        val editor = preferences.edit()
        config.toMap().forEach { (key, value) -> editor.putString(key, value) }
        editor.commit()
    }

    fun update(transform: (ServerConfig) -> ServerConfig): ServerConfig = transform(load()).also(::save)

    companion object {

        const val PREFERENCES_NAME = "mcp_server_config"
    }
}
