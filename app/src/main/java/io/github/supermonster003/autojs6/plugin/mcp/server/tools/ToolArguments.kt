package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Thrown by [ToolArguments] and the executors; the registry turns it into an `isError` result. */
class ToolArgumentException(val failure: ToolFailure) : RuntimeException(failure.render())

/**
 * Validates `tools/call` arguments against the catalog schema before anything reaches the host:
 * required names, unknown names (`additionalProperties: false`), primitive types, integer bounds,
 * string length, and enumerations. Deliberately small; nested schemas are validated by the host.
 */
object ToolArguments {

    fun validate(spec: ToolSpec, arguments: JsonObject?): JsonObject {
        val values = arguments ?: JsonObject(emptyMap())
        val properties = spec.properties
        values.keys.firstOrNull { it !in properties }?.let { unknown ->
            fail("${spec.name} does not accept an argument named $unknown")
        }
        spec.required.firstOrNull { name -> values[name].let { it == null || it is JsonNull } }?.let { missing ->
            fail("${spec.name} requires the argument $missing")
        }
        values.forEach { (name, value) ->
            if (value is JsonNull) return@forEach
            val schema = properties[name] as? JsonObject ?: return@forEach
            check(spec.name, name, value, schema)
        }
        return values
    }

    fun string(arguments: JsonObject, name: String, default: String? = null): String? =
        (arguments[name] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: default

    fun long(arguments: JsonObject, name: String, default: Long): Long =
        (arguments[name] as? JsonPrimitive)?.longOrNull ?: default

    fun boolean(arguments: JsonObject, name: String, default: Boolean): Boolean =
        (arguments[name] as? JsonPrimitive)?.booleanOrNull ?: default

    fun objectOrNull(arguments: JsonObject, name: String): JsonObject? = arguments[name] as? JsonObject

    private fun check(tool: String, name: String, value: JsonElement, schema: JsonObject) {
        when (JsonSchemas.typeOf(schema)) {
            "string" -> {
                val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("$tool: $name must be a string")
                (schema["maxLength"] as? JsonPrimitive)?.longOrNull?.let { max ->
                    if (text.toByteArray(Charsets.UTF_8).size > max) fail("$tool: $name is longer than $max bytes")
                }
                (schema["enum"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }?.let { allowed ->
                    if (text !in allowed) fail("$tool: $name must be one of ${allowed.joinToString(", ")}")
                }
            }
            "integer" -> {
                val primitive = value as? JsonPrimitive
                val number = primitive?.takeIf { !it.isString }?.longOrNull
                    ?: primitive?.takeIf { !it.isString }?.doubleOrNull?.takeIf { it == Math.floor(it) }?.toLong()
                    ?: fail("$tool: $name must be an integer")
                (schema["minimum"] as? JsonPrimitive)?.longOrNull?.let { min -> if (number < min) fail("$tool: $name must be at least $min") }
                (schema["maximum"] as? JsonPrimitive)?.longOrNull?.let { max -> if (number > max) fail("$tool: $name must be at most $max") }
            }
            "number" -> {
                val number = (value as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull ?: fail("$tool: $name must be a number")
                (schema["minimum"] as? JsonPrimitive)?.doubleOrNull?.let { min -> if (number < min) fail("$tool: $name must be at least $min") }
                (schema["maximum"] as? JsonPrimitive)?.doubleOrNull?.let { max -> if (number > max) fail("$tool: $name must be at most $max") }
            }
            "boolean" -> (value as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: fail("$tool: $name must be true or false")
            "object" -> {
                val obj = value as? JsonObject ?: fail("$tool: $name must be an object")
                if (schema["additionalProperties"] is JsonObject) {
                    obj.values.firstOrNull { it !is JsonPrimitive && it !is JsonNull }?.let { fail("$tool: $name values must be strings, numbers, booleans, or null") }
                }
            }
            "array" -> value as? JsonArray ?: fail("$tool: $name must be an array")
            else -> Unit
        }
    }

    private fun fail(message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments(message))
}
