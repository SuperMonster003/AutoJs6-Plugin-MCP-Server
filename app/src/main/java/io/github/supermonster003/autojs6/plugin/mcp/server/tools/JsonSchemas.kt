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

    /** A free-form object whose values are primitives (strings, numbers, booleans, null). */
    fun primitiveMap(description: String): JsonObject = buildJsonObject {
        put("type", "object")
        put("description", description)
        put("additionalProperties", buildJsonObject {
            put("type", JsonArray(listOf("string", "number", "boolean", "null").map { JsonPrimitive(it) }))
        })
    }

    /** The `type` of a property schema, for validation; a list of types yields null. */
    fun typeOf(schema: JsonElement?): String? = ((schema as? JsonObject)?.get("type") as? JsonPrimitive)?.takeIf { it.isString }?.content
}
