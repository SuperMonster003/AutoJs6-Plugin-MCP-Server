package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.PluginToolsReport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.IOException

/** One plugin with self-described tools as the settings page shows it. */
data class PluginToolsGroup(
    val shortName: String,
    val packageName: String,
    val trust: String?,
    val status: String,
    val detail: String?,
    /** Tool name to risk (`readonly`, `write`, `destructive`, `network`). */
    val tools: Map<String, String>,
) {

    val userGranted: Boolean
        get() = trust == "user_granted"

    val hasWritingTools: Boolean
        get() = tools.values.any { it != "readonly" }
}

/**
 * The plugins with self-described tools (roadmap P12) as the listener last saw them, for the settings page: it runs in
 * the main process and never talks to the host, so the `:mcp_server` process writes what `plugin_tools.list` answered.
 * Only plugins with a short name appear (the others have no group to switch); an empty or missing document means the
 * listener has not met such a host yet. Never contains tool descriptions or arguments.
 */
class PluginToolsSnapshotStore(context: Context) {

    private val document = ProcessSharedFile(context, FILE_NAME)

    @Volatile
    private var lastWritten: String? = null

    fun load(): List<PluginToolsGroup> = decode(runCatching { document.read() }.getOrNull())

    /** Writes [report] when it differs from the last write; a null report (no session, old host) keeps the last one. */
    fun saveIfChanged(report: PluginToolsReport?) {
        report ?: return
        val text = encode(report)
        if (text == lastWritten) return
        try {
            document.write(text)
            lastWritten = text
        } catch (e: IOException) {
            Log.w(TAG, "Cannot store the plugin tools snapshot (${e.javaClass.simpleName})")
        }
    }

    companion object {

        const val FILE_NAME = "plugin_tools.json"

        private const val TAG = "PluginToolsSnapshot"

        fun encode(report: PluginToolsReport): String = buildJsonObject {
            put("format", 1)
            putJsonArray("groups") {
                report.providers.filter { it.shortName != null }.sortedBy { it.shortName }.forEach { provider ->
                    add(buildJsonObject {
                        put("shortName", provider.shortName)
                        put("packageName", provider.packageName)
                        provider.trust?.let { put("trust", it) }
                        put("status", provider.status)
                        provider.detail?.let { put("detail", it) }
                        put("tools", buildJsonObject {
                            report.tools.filter { it.provider == provider.packageName }.sortedBy { it.name }.forEach { put(it.name, it.risk) }
                        })
                    })
                }
            }
        }.toString()

        /** Tolerant: entries that miss a field are skipped. */
        fun decode(text: String?): List<PluginToolsGroup> {
            val root = text?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } as? JsonObject ?: return emptyList()
            return (root["groups"] as? JsonArray)?.mapNotNull { item ->
                val group = item as? JsonObject ?: return@mapNotNull null
                PluginToolsGroup(
                    shortName = group.text("shortName") ?: return@mapNotNull null,
                    packageName = group.text("packageName") ?: return@mapNotNull null,
                    trust = group.text("trust"),
                    status = group.text("status") ?: return@mapNotNull null,
                    detail = group.text("detail"),
                    tools = (group["tools"] as? JsonObject)?.mapNotNull { (name, risk) ->
                        (risk as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { name to it }
                    }?.toMap().orEmpty(),
                )
            }.orEmpty()
        }

        private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
}
