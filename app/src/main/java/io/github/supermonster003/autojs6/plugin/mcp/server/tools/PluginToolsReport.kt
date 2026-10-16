package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** One binary output a self-described tool may produce (`outputs[]` of its catalog entry). */
data class PluginToolOutput(
    val name: String,
    val mimeType: String,
    val description: String? = null,
)

/**
 * One tool a plugin describes itself (roadmap P12, decisions D29 - D34), as the host's `plugin_tools.list` lists it:
 * [name] is the MCP-facing `<shortName>_<tool>`, [inputSchema] the plugin's JSON Schema (a subset the host checks),
 * and [imageParameters] / [fileParameters] the top-level string arguments the host resolves (`"screen"` or a path).
 */
data class PluginToolInfo(
    val name: String,
    val tool: String,
    val shortName: String,
    val provider: String,
    val trust: String,
    val title: String?,
    val description: String,
    val inputSchema: JsonObject,
    val outputSchema: JsonObject?,
    val risk: String,
    val timeoutMs: Long,
    val maxConcurrency: Int,
    val outputs: List<PluginToolOutput>,
    val imageParameters: List<String>,
    val fileParameters: List<String>,
    /** Directory arguments (roadmap D49): the host opens the files of the directory and hands them over. */
    val directoryParameters: List<String> = emptyList(),
) {

    val readOnly: Boolean
        get() = risk == RISK_READONLY

    val userGranted: Boolean
        get() = trust == TRUST_USER_GRANTED

    companion object {

        const val RISK_READONLY = "readonly"
        const val RISK_WRITE = "write"
        const val RISK_DESTRUCTIVE = "destructive"
        const val RISK_NETWORK = "network"

        const val TRUST_OFFICIAL = "official"
        const val TRUST_TRUSTED = "trusted"
        const val TRUST_USER_GRANTED = "user_granted"
    }
}

/** One plugin that declares tools, with the host's verdict on it (`providers[]` of `plugin_tools.list`). */
data class PluginToolProvider(
    val packageName: String,
    val versionCode: Long?,
    val trust: String?,
    val status: String,
    val shortName: String?,
    val tools: Int?,
    val diagnostics: List<String> = emptyList(),
    val detail: String? = null,
    val toolsHidden: Boolean = false,
) {

    val available: Boolean
        get() = status == STATUS_AVAILABLE

    companion object {

        const val STATUS_AVAILABLE = "available"
    }
}

/**
 * The host's `plugin_tools.list` answer (AutoJs6 6.8.0, schema `autojs6-bridge-plugin-tools-list-v1`): the tools of
 * every available provider and the state of every provider, read by the host from its catalog cache.
 */
data class PluginToolsReport(
    val tools: List<PluginToolInfo> = emptyList(),
    val providers: List<PluginToolProvider> = emptyList(),
) {

    /** `shortName=status` pairs for logs and the dump. */
    fun describe(): String = providers.joinToString(",", "{", "}") { "${it.shortName ?: it.packageName}=${it.status}" }

    companion object {

        const val SCHEMA = "autojs6-bridge-plugin-tools-list-v1"

        val EMPTY = PluginToolsReport()

        /** Tolerant: entries that miss a required field are skipped, a missing or foreign schema yields null. */
        fun parse(element: JsonElement?): PluginToolsReport? {
            val root = element as? JsonObject ?: return null
            if (root["schema"]?.string() != SCHEMA) return null
            val tools = (root["tools"] as? JsonArray)?.mapNotNull { parseTool(it as? JsonObject) }.orEmpty()
            val providers = (root["providers"] as? JsonArray)?.mapNotNull { item ->
                val record = item as? JsonObject ?: return@mapNotNull null
                PluginToolProvider(
                    packageName = record["packageName"]?.string() ?: return@mapNotNull null,
                    versionCode = (record["versionCode"] as? JsonPrimitive)?.longOrNull,
                    trust = record["trust"]?.string(),
                    status = record["status"]?.string() ?: return@mapNotNull null,
                    shortName = record["shortName"]?.string(),
                    tools = (record["tools"] as? JsonPrimitive)?.longOrNull?.toInt(),
                    diagnostics = (record["diagnostics"] as? JsonArray)?.mapNotNull { it.string() }.orEmpty(),
                    detail = record["detail"]?.string(),
                    toolsHidden = (record["toolsHidden"] as? JsonPrimitive)?.booleanOrNull == true,
                )
            }.orEmpty()
            return PluginToolsReport(tools.distinctBy { it.name }, providers)
        }

        private fun parseTool(record: JsonObject?): PluginToolInfo? {
            record ?: return null
            return PluginToolInfo(
                name = record["name"]?.string() ?: return null,
                tool = record["tool"]?.string() ?: return null,
                shortName = record["shortName"]?.string() ?: return null,
                provider = record["provider"]?.string() ?: return null,
                trust = record["trust"]?.string() ?: return null,
                title = record["title"]?.string(),
                description = record["description"]?.string() ?: return null,
                inputSchema = record["inputSchema"] as? JsonObject ?: return null,
                outputSchema = record["outputSchema"] as? JsonObject,
                risk = record["risk"]?.string() ?: return null,
                timeoutMs = (record["timeoutMs"] as? JsonPrimitive)?.longOrNull ?: return null,
                maxConcurrency = (record["maxConcurrency"] as? JsonPrimitive)?.longOrNull?.toInt() ?: 1,
                outputs = (record["outputs"] as? JsonArray)?.mapNotNull { item ->
                    val output = item as? JsonObject ?: return@mapNotNull null
                    PluginToolOutput(
                        name = output["name"]?.string() ?: return@mapNotNull null,
                        mimeType = output["mimeType"]?.string() ?: return@mapNotNull null,
                        description = output["description"]?.string(),
                    )
                }.orEmpty(),
                imageParameters = (record["imageParameters"] as? JsonArray)?.mapNotNull { it.string() }.orEmpty(),
                fileParameters = (record["fileParameters"] as? JsonArray)?.mapNotNull { it.string() }.orEmpty(),
                directoryParameters = (record["directoryParameters"] as? JsonArray)?.mapNotNull { it.string() }.orEmpty(),
            )
        }

        private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
}
