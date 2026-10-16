package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The switches of one plugin that describes its own tools (roadmap P12, decision D34), keyed by its short name: [enabled]
 * turns the whole group on or off, [writes] admits its tools whose risk is not `readonly`. Null keeps the default.
 */
data class PluginGroupSwitch(
    val enabled: Boolean? = null,
    val writes: Boolean? = null,
)

/**
 * The per-group switches (roadmap P2.3, decision D6). Groups the document does not mention keep
 * their default, so a build that adds a group never silently enables or disables it for the user.
 *
 * [pluginGroups] holds the switches of the self-described plugin tools (roadmap P12, decision D34): one group per short
 * name, on by default for official and trusted plugins and off for user-granted ones, plus a sub-switch for the tools
 * that write, destroy or reach the network, off by default. Only explicit choices are stored, because the set of
 * plugins changes and their defaults depend on the trust the host reports.
 */
data class ToolPermissions(
    val overrides: Map<ToolGroup, Boolean> = emptyMap(),
    val allowShellRoot: Boolean = false,
    /** Roadmap P11 D26: the read-only tools may read absolute paths in shared storage; off by default. */
    val allowSharedStorageRead: Boolean = false,
    val pluginGroups: Map<String, PluginGroupSwitch> = emptyMap(),
) {

    fun isEnabled(group: ToolGroup): Boolean = overrides[group] ?: group.defaultEnabled

    fun with(group: ToolGroup, enabled: Boolean): ToolPermissions = copy(overrides = overrides + (group to enabled))

    /** Whether the group of the plugin with [shortName] is on; [userGranted] plugins start off. */
    fun isPluginGroupEnabled(shortName: String, userGranted: Boolean): Boolean = pluginGroups[shortName]?.enabled ?: !userGranted

    /** Whether the plugin with [shortName] may offer its tools that are not read-only; off by default. */
    fun arePluginWritesEnabled(shortName: String): Boolean = pluginGroups[shortName]?.writes ?: false

    /** Whether [tool] may be listed and called under these switches. */
    fun allows(tool: PluginToolInfo): Boolean =
        isPluginGroupEnabled(tool.shortName, tool.userGranted) && (tool.readOnly || arePluginWritesEnabled(tool.shortName))

    fun withPluginGroup(shortName: String, enabled: Boolean): ToolPermissions =
        copy(pluginGroups = pluginGroups + (shortName to (pluginGroups[shortName] ?: PluginGroupSwitch()).copy(enabled = enabled)))

    fun withPluginWrites(shortName: String, enabled: Boolean): ToolPermissions =
        copy(pluginGroups = pluginGroups + (shortName to (pluginGroups[shortName] ?: PluginGroupSwitch()).copy(writes = enabled)))

    /** Every group with its effective value. */
    val effective: Map<ToolGroup, Boolean>
        get() = ToolGroup.entries.associateWith(::isEnabled)

    fun encode(): String = buildJsonObject {
        put("format", FORMAT)
        put("allowShellRoot", allowShellRoot)
        put("allowSharedStorageRead", allowSharedStorageRead)
        putJsonObject("groups") {
            ToolGroup.entries.forEach { group -> put(group.id, isEnabled(group)) }
        }
        if (pluginGroups.isNotEmpty()) {
            putJsonObject("pluginGroups") {
                pluginGroups.toSortedMap().forEach { (shortName, switch) ->
                    putJsonObject(shortName) {
                        switch.enabled?.let { put("enabled", it) }
                        switch.writes?.let { put("writes", it) }
                    }
                }
            }
        }
    }.toString()

    companion object {

        const val FORMAT = 1

        val DEFAULT = ToolPermissions()

        /** Tolerant: unknown groups and non-boolean values are ignored. */
        fun decode(text: String?): ToolPermissions {
            val root = text?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } as? JsonObject ?: return DEFAULT
            val groups = root["groups"] as? JsonObject ?: return DEFAULT
            val overrides = LinkedHashMap<ToolGroup, Boolean>()
            groups.forEach { (id, value) ->
                val group = ToolGroup.fromId(id) ?: return@forEach
                val enabled = (value as? JsonPrimitive)?.booleanOrNull ?: return@forEach
                overrides[group] = enabled
            }
            fun flag(name: String) = (root[name] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull == true
            fun JsonObject.choice(name: String) = (this[name] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
            val pluginGroups = LinkedHashMap<String, PluginGroupSwitch>()
            (root["pluginGroups"] as? JsonObject)?.forEach { (shortName, value) ->
                val record = value as? JsonObject ?: return@forEach
                val switch = PluginGroupSwitch(record.choice("enabled"), record.choice("writes"))
                if (switch != PluginGroupSwitch()) pluginGroups[shortName] = switch
            }
            return ToolPermissions(overrides, flag("allowShellRoot"), flag("allowSharedStorageRead"), pluginGroups)
        }
    }
}
