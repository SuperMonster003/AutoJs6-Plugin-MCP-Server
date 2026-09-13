package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException

/**
 * Persists the [ServerConfig] as one JSON document in the plugin's private storage (roadmap P2.1).
 *
 * Values are stored as strings and decoded with [ServerConfig.fromMap], so a value written by a
 * newer or older build degrades to the default instead of breaking the server. The document is
 * read on every [load] and replaced atomically, because the settings page (P4.2) and the
 * `:mcp_server` process are different processes and the server must see what the user just
 * saved even when its process is still alive from an earlier run.
 */
class ServerConfigStore(context: Context) {

    private val document = ProcessSharedFile(context, FILE_NAME)

    fun load(): ServerConfig = ServerConfig.fromMap(decode(document.read()))

    fun save(config: ServerConfig) {
        try {
            document.write(encode(config))
        } catch (e: IOException) {
            Log.w(TAG, "Cannot store the server configuration (${e.javaClass.simpleName})")
        }
    }

    fun update(transform: (ServerConfig) -> ServerConfig): ServerConfig {
        val stored = document.update { encode(transform(ServerConfig.fromMap(decode(it)))) }
        return ServerConfig.fromMap(decode(stored))
    }

    private fun encode(config: ServerConfig): String = buildJsonObject {
        config.toMap().forEach { (key, value) -> put(key, value) }
    }.toString()

    private fun decode(text: String?): Map<String, String?> {
        val stored = text?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
        return ServerConfig.KEYS.associateWith { key -> stored?.get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() } }
    }

    companion object {

        const val FILE_NAME = "config.json"

        private const val TAG = "ServerConfigStore"
    }
}
