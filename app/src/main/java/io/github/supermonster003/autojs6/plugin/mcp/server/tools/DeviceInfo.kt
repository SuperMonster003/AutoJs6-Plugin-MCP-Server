package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.resources.ResourceUri
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * `device_info` and the resource `autojs6://device/info` (roadmap P3.4, P11 D27): the host's
 * `device.info` plus a `plugins` section built from `app.pluginCapabilities`, so a model learns which
 * plugin families are available, missing, disabled or incompatible, which tools they bring, and what
 * the user has to do in the AutoJs6 plugin center. A host without the status method (an older build,
 * or a grant without it) simply gets no `plugins` field; the device part is unchanged either way.
 */
object DeviceInfo {

    /** Waiting for `app.pluginCapabilities`, which answers from the package manager without binding a plugin. */
    const val PLUGINS_TIMEOUT_MS = 5_000L

    private val NO_ARGS = JsonArray(emptyList())
    private val DEVICE_PERMISSIONS = listOf("device")
    private val STATUS_PERMISSIONS = listOf("app.query")

    /** The plugins a family stands for, as the plugin center names them. */
    private val PLUGIN_NAMES = mapOf(
        "ocr" to "ML Kit OCR, Paddle OCR or Rapid OCR",
        "barcode" to "ML Kit Barcode",
        "opencv" to "OpenCV",
        "pinyin" to "Pinyin",
        "pinyin4j" to "Pinyin4j",
        "mediainfo" to "MediaInfo",
        "docs" to "Offline Docs",
    )

    const val HINT_PLUGIN_CENTER = "plugins are installed and enabled in AutoJs6 > Plugin center; the tool list follows within seconds"

    /** `device.info`, plus `plugins` when the host can report its plugin families. */
    suspend fun read(caller: BridgeCaller, permissions: ToolPermissions, timeoutMs: Long): JsonObject {
        val info = when (val result = caller.callOrThrow("device", "info", NO_ARGS, timeoutMs, DEVICE_PERMISSIONS)) {
            is JsonObject -> result
            else -> buildJsonObject { put("result", result) }
        }
        val outcome = caller.call("app", "pluginCapabilities", NO_ARGS, PLUGINS_TIMEOUT_MS, STATUS_PERMISSIONS)
        val report = (outcome as? BridgeOutcome.Ok)?.let { PluginFamilyReport.parse(it.result) } ?: return info
        return JsonObject(info + ("plugins" to section(report, permissions)))
    }

    /** The `plugins` section: one entry per family the host reports, in its order, with the tools, the group switch and a hint. */
    fun section(report: PluginFamilyReport, permissions: ToolPermissions): JsonObject = buildJsonObject {
        put("families", buildJsonArray {
            report.families.values.forEach { family -> add(familyJson(family, permissions)) }
        })
        put("hint", HINT_PLUGIN_CENTER)
    }

    private fun familyJson(family: PluginFamilyStatus, permissions: ToolPermissions): JsonObject = buildJsonObject {
        val group = ToolGroup.PLUGIN_FAMILIES.firstOrNull { it.family == family.id }
        val tools = ToolCatalog.pluginFamilyRows.filter { it.group.family == family.id }.map { it.name }
        put("id", family.id)
        put("name", PLUGIN_NAMES[family.id] ?: family.id)
        put("status", family.status)
        if (group != null) {
            put("toolGroup", group.id)
            put("toolGroupEnabled", permissions.isEnabled(group))
        }
        put("tools", JsonArray(tools.map { JsonPrimitive(it) }))
        if (family.id == "docs") put("resources", JsonArray(listOf(JsonPrimitive(ResourceUri.DOCS_TEMPLATE))))
        put("plugins", buildJsonArray {
            family.plugins.forEach { plugin ->
                add(buildJsonObject {
                    put("packageName", plugin.packageName)
                    plugin.versionName?.let { put("versionName", it) }
                    plugin.versionCode?.let { put("versionCode", it) }
                    put("enabled", plugin.enabled)
                    put("compatible", plugin.compatible)
                    plugin.requiresHostVersion?.let { put("requiresHostVersion", it) }
                })
            }
        })
        family.detail?.takeIf { it.isNotBlank() }?.let { put("detail", it) }
        hint(family, group, permissions)?.let { put("hint", it) }
    }

    /** What the user can do about a family that brings no tools right now, or null when nothing is needed. */
    private fun hint(family: PluginFamilyStatus, group: ToolGroup?, permissions: ToolPermissions): String? {
        val names = PLUGIN_NAMES[family.id] ?: family.id
        return when (family.status) {
            PluginFamilyStatus.STATUS_NOT_INSTALLED -> "install the $names plugin from the AutoJs6 plugin center"
            PluginFamilyStatus.STATUS_DISABLED -> "enable the $names plugin in the AutoJs6 plugin center"
            PluginFamilyStatus.STATUS_INCOMPATIBLE -> {
                val required = family.plugins.mapNotNull { it.requiresHostVersion }.maxOrNull()
                if (required != null) "the installed $names plugin needs AutoJs6 build $required or later; update AutoJs6, or install a plugin version for this build"
                else "update AutoJs6 or the $names plugin; the installed plugin does not fit this AutoJs6 build"
            }
            PluginFamilyStatus.STATUS_AVAILABLE ->
                if (group != null && !permissions.isEnabled(group)) "switch on the ${group.id} group in the MCP Server settings on the phone to list its tools" else null
            else -> null
        }
    }
}
