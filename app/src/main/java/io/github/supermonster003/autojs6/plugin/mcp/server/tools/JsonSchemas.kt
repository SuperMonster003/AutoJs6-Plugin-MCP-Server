package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Small builders for the JSON Schema 2020-12 documents of the tool catalog (appendix A.1). */
object JsonSchemas {

    const val DIALECT = "https://json-schema.org/draft/2020-12/schema"

    /** An object schema with `additionalProperties: false`, as every tool input must be. */
    fun objectSchema(properties: Map<String, JsonObject> = emptyMap(), required: List<String> = emptyList(), description: String? = null): JsonObject =
        buildJsonObject {
            put("type", "object")
            description?.let { put("description", it) }
            put("properties", JsonObject(properties))
            if (required.isNotEmpty()) put("required", JsonArray(required.map { JsonPrimitive(it) }))
            put("additionalProperties", false)
        }

    fun string(description: String, maxLength: Int? = null, default: String? = null, enum: List<String>? = null): JsonObject = buildJsonObject {
        put("type", "string")
        put("description", description)
        maxLength?.let { put("maxLength", it) }
        default?.let { put("default", it) }
        enum?.let { put("enum", JsonArray(it.map { value -> JsonPrimitive(value) })) }
    }

    fun integer(description: String, minimum: Long? = null, maximum: Long? = null, default: Long? = null): JsonObject = buildJsonObject {
        put("type", "integer")
        put("description", description)
        minimum?.let { put("minimum", it) }
        maximum?.let { put("maximum", it) }
        default?.let { put("default", it) }
    }

    fun boolean(description: String, default: Boolean? = null): JsonObject = buildJsonObject {
        put("type", "boolean")
        put("description", description)
        default?.let { put("default", it) }
    }

    /**
     * A free-form object whose values are primitives (strings, numbers, booleans, null). The value
     * schema is an `anyOf` of single-type branches rather than an array-valued `type`: several MCP
     * clients read `type` as one string and reject or weaken the tool otherwise (roadmap P5.2).
     */
    fun primitiveMap(description: String): JsonObject = buildJsonObject {
        put("type", "object")
        put("description", description)
        put("additionalProperties", buildJsonObject {
            put("anyOf", JsonArray(listOf("string", "number", "boolean", "null").map { type -> buildJsonObject { put("type", type) } }))
        })
    }

    /** An array schema; [items] is the schema of every element. */
    fun array(description: String, items: JsonObject, minItems: Int? = null, maxItems: Int? = null): JsonObject = buildJsonObject {
        put("type", "array")
        put("description", description)
        put("items", items)
        minItems?.let { put("minItems", it) }
        maxItems?.let { put("maxItems", it) }
    }

    /** A screen rectangle in device pixels: `{left, top, right, bottom}`. */
    fun bounds(description: String): JsonObject = objectSchema(
        properties = linkedMapOf(
            "left" to integer("Left edge in device pixels"),
            "top" to integer("Top edge in device pixels"),
            "right" to integer("Right edge in device pixels"),
            "bottom" to integer("Bottom edge in device pixels"),
        ),
        required = listOf("left", "top", "right", "bottom"),
        description = description,
    )

    /**
     * The node selector of the `ui` tools (decision D13): the keys of the host's `BridgeSelector`
     * dialect, every present condition must hold. `UiSelectors` validates the values.
     */
    fun selector(description: String): JsonObject = objectSchema(
        properties = linkedMapOf(
            "text" to string("The node text equals this"),
            "textContains" to string("The node text contains this"),
            "textMatches" to string("The whole node text matches this Java regular expression; (?i) makes it case-insensitive"),
            "desc" to string("The content description equals this"),
            "descContains" to string("The content description contains this"),
            "descMatches" to string("The whole content description matches this Java regular expression"),
            "id" to string("The view id: either the full resource name (package:id/name) or just the name part shown by ui_dump"),
            "idMatches" to string("The whole view id resource name matches this Java regular expression"),
            "className" to string("The class name: either fully qualified (android.widget.Button) or the short name shown by ui_dump (Button)"),
            "classNameMatches" to string("The whole class name matches this Java regular expression"),
            "clickable" to boolean("Whether the node is clickable"),
            "enabled" to boolean("Whether the node is enabled"),
            "scrollable" to boolean("Whether the node is scrollable"),
            "depth" to integer("Depth below the window root; the root is 0", minimum = 0),
            "boundsInside" to bounds("Only nodes whose bounds lie inside this rectangle"),
            "boundsContains" to bounds("Only nodes whose bounds contain this rectangle"),
        ),
        description = description,
    )

    /** The `type` of a property schema, for validation; a list of types yields null. */
    fun typeOf(schema: JsonElement?): String? = ((schema as? JsonObject)?.get("type") as? JsonPrimitive)?.takeIf { it.isString }?.content
}
