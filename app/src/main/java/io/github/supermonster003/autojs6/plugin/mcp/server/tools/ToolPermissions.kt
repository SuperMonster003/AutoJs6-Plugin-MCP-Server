package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The per-group switches (roadmap P2.3, decision D6). Groups the document does not mention keep
 * their default, so a build that adds a group never silently enables or disables it for the user.
 */
data class ToolPermissions(val overrides: Map<ToolGroup, Boolean> = emptyMap(), val allowShellRoot: Boolean = false) {

    fun isEnabled(group: ToolGroup): Boolean = overrides[group] ?: group.defaultEnabled

    fun with(group: ToolGroup, enabled: Boolean): ToolPermissions = copy(overrides = overrides + (group to enabled))

    /** Every group with its effective value. */
    val effective: Map<ToolGroup, Boolean>
        get() = ToolGroup.entries.associateWith(::isEnabled)

    fun encode(): String = buildJsonObject {
        put("format", FORMAT)
        put("allowShellRoot", allowShellRoot)
        putJsonObject("groups") {
            ToolGroup.entries.forEach { group -> put(group.id, isEnabled(group)) }
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
            return ToolPermissions(overrides, (root["allowShellRoot"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull == true)
        }
    }
}
