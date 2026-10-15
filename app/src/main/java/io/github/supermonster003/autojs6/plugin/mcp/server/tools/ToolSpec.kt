package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract

/** The host bridge method a tool maps onto (`module.method`), or null for a plugin-local tool. */
data class BridgeMethod(val module: String, val method: String) {

    val key: String
        get() = "$module.$method"
}

/** MCP tool annotations as the catalog declares them (appendix A.1). */
data class ToolHints(
    val readOnly: Boolean = false,
    val destructive: Boolean = false,
    val idempotent: Boolean = false,
    val openWorld: Boolean = false,
)

/**
 * One row of the tool catalog (roadmap P2.3): the MCP-facing description, the JSON Schema of the
 * arguments, the group switch it belongs to, and how it reaches the host.
 */
data class ToolSpec(
    val name: String,
    val title: String,
    val description: String,
    val inputSchema: JsonObject,
    val group: ToolGroup,
    val outputSchema: JsonObject? = null,
    val bridge: BridgeMethod? = null,
    val permissions: List<String> = emptyList(),
    val timeoutMs: Long = McpServerContract.DEFAULT_TOOL_TIMEOUT_MS,
    val maxTimeoutMs: Long = timeoutMs,
    val hints: ToolHints = ToolHints(),
    /**
     * Every host method the tool may call (a flow calls several); defaults to [bridge]. For a
     * plugin-family group the registry lists the tool only while the session grant covers all of them.
     */
    val hostMethods: List<BridgeMethod> = listOfNotNull(bridge),
    /**
     * A built-in row that needs host methods only newer AutoJs6 builds grant (roadmap P11 D42): listed
     * only while the open session's grant covers all [hostMethods], so an older host does not offer a
     * tool that can only fail; without a session it is listed like the other built-in rows.
     */
    val gatedByGrant: Boolean = false,
) {

    val defaultEnabled: Boolean
        get() = group.defaultEnabled

    /** The `properties` object of [inputSchema]. */
    val properties: JsonObject
        get() = inputSchema["properties"] as? JsonObject ?: JsonObject(emptyMap())

    /** The `required` names of [inputSchema]. */
    val required: List<String>
        get() = (inputSchema["required"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

    /** The SDK view of the tool for `tools/list`. */
    fun toSdkTool(): Tool = Tool(
        name = name,
        title = title,
        description = description,
        inputSchema = ToolSchema(properties = properties, required = required.takeIf { it.isNotEmpty() }),
        outputSchema = outputSchema?.let { schema ->
            ToolSchema(
                properties = schema["properties"] as? JsonObject,
                required = (schema["required"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
            )
        },
        annotations = ToolAnnotations(
            title = title,
            readOnlyHint = hints.readOnly,
            destructiveHint = hints.destructive,
            idempotentHint = hints.idempotent,
            openWorldHint = hints.openWorld,
        ),
    )

    /** Stable JSON of the row for the snapshot drift guard (`tool-catalog.snapshot.json`). */
    fun toSnapshotJson(): JsonObject = buildJsonObject {
        put("name", name)
        put("title", title)
        put("description", description)
        put("group", group.id)
        put("defaultEnabled", defaultEnabled)
        put("bridge", bridge?.key?.let { JsonPrimitive(it) } ?: JsonPrimitive("local"))
        put("permissions", JsonArray(permissions.map { JsonPrimitive(it) }))
        put("timeoutMs", timeoutMs)
        put("maxTimeoutMs", maxTimeoutMs)
        if (hostMethods != listOfNotNull(bridge)) put("hostMethods", JsonArray(hostMethods.map { JsonPrimitive(it.key) }))
        if (gatedByGrant) put("gatedByGrant", true)
        put("annotations", buildJsonObject {
            put("readOnlyHint", hints.readOnly)
            put("destructiveHint", hints.destructive)
            put("idempotentHint", hints.idempotent)
            put("openWorldHint", hints.openWorld)
        })
        put("inputSchema", inputSchema)
        outputSchema?.let { put("outputSchema", it) }
    }

    /** The declared JSON Schema `type` of a property, if it is a simple one. */
    fun propertyType(name: String): String? = (properties[name] as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull
}
