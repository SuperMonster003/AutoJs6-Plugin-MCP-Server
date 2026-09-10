package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.math.min

/**
 * The selector dialect of the `ui` tools (roadmap D13): the keys of the host's `BridgeSelector`,
 * validated here so a wrong condition answers `INVALID_ARGUMENTS` with its name instead of a
 * host error, and normalized into the descriptor the bridge parses. Two conveniences on top of
 * the frozen dialect: an `id` without `:` is the name part `ui_dump` shows and becomes an
 * `idMatches` over any package, and a `className` without `.` is the short class name and
 * becomes a `classNameMatches` over any package.
 *
 * Also builds the selectors the node references need (D12): the relocation selector of a
 * fingerprint (class, id, text, description, bounds with a tolerance) and the exact selector
 * of a located node. Pure Kotlin; `UiSelectorsTest` covers it.
 */
object UiSelectors {

    val KEYS: List<String> = listOf(
        "text", "textContains", "textMatches",
        "desc", "descContains", "descMatches",
        "id", "idMatches",
        "className", "classNameMatches",
        "clickable", "enabled", "scrollable", "depth",
        "boundsInside", "boundsContains",
    )

    /** Longest string condition; the host caps requests anyway, this keeps messages readable. */
    const val MAX_STRING_LENGTH = 4096

    /** Text and description longer than this are left out of a fingerprint selector. */
    const val MAX_FINGERPRINT_TEXT = 256

    /** How far a node may have moved, per side, and still be the node a reference means. */
    const val BOUNDS_TOLERANCE_PX = 48

    private val STRING_KEYS = setOf("text", "textContains", "desc", "descContains", "id", "className")
    private val REGEX_KEYS = setOf("textMatches", "descMatches", "idMatches", "classNameMatches")
    private val BOOLEAN_KEYS = setOf("clickable", "enabled", "scrollable")
    private val BOUNDS_KEYS = setOf("boundsInside", "boundsContains")

    /** Validates the [value] of the argument [name] of [tool] and returns the bridge descriptor. */
    fun parse(tool: String, value: JsonElement?, name: String = ToolCatalog.Ui.SELECTOR): JsonObject {
        val json = value as? JsonObject ?: fail("$tool: $name must be an object with at least one condition")
        val out = LinkedHashMap<String, JsonElement>()
        fun set(key: String, element: JsonElement) {
            if (key in out) fail("$tool: $name gives $key twice (a short id or class name is turned into ${key})")
            out[key] = element
        }
        json.forEach { (key, element) ->
            if (element is JsonNull) return@forEach
            when (key) {
                in STRING_KEYS -> {
                    val text = string(tool, name, key, element)
                    when (key) {
                        "id" -> if (':' in text) set("id", JsonPrimitive(text)) else set("idMatches", JsonPrimitive(shortIdPattern(text)))
                        "className" -> if ('.' in text) set("className", JsonPrimitive(text)) else set("classNameMatches", JsonPrimitive(shortClassPattern(text)))
                        else -> set(key, JsonPrimitive(text))
                    }
                }
                in REGEX_KEYS -> {
                    val pattern = string(tool, name, key, element)
                    runCatching { Regex(pattern) }.exceptionOrNull()?.let { fail("$tool: $name.$key is not a valid regular expression (${it.message?.lineSequence()?.firstOrNull()})") }
                    set(key, JsonPrimitive(pattern))
                }
                in BOOLEAN_KEYS -> {
                    val flag = (element as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: fail("$tool: $name.$key must be true or false")
                    set(key, JsonPrimitive(flag))
                }
                "depth" -> {
                    val depth = integer(element) ?: fail("$tool: $name.depth must be an integer")
                    if (depth < 0) fail("$tool: $name.depth must be at least 0")
                    set(key, JsonPrimitive(depth))
                }
                in BOUNDS_KEYS -> {
                    val bounds = bounds(element) ?: fail("$tool: $name.$key must be an object with integer left, top, right, and bottom")
                    set(key, bounds.toJson())
                }
                else -> fail("$tool: $name does not accept a condition named $key; use ${KEYS.joinToString(", ")}")
            }
        }
        if (out.isEmpty()) fail("$tool: $name needs at least one condition (${KEYS.joinToString(", ")})")
        return JsonObject(out)
    }

    /**
     * The selector that finds the node a reference means again: class, id, text, and
     * description when the fingerprint has them, and the bounds with a tolerance that stays
     * below half the node size (so a neighbour of the same size never qualifies).
     */
    fun relocation(node: UiNodeInfo): JsonObject = buildJsonObject {
        identity(node)
        val bounds = node.bounds
        val dx = min(BOUNDS_TOLERANCE_PX, bounds.width / 2)
        val dy = min(BOUNDS_TOLERANCE_PX, bounds.height / 2)
        put("boundsInside", bounds.expanded(dx, dy).toJson())
    }

    /** The selector that pins one located node: its identity plus its exact bounds. */
    fun exact(node: UiNodeInfo): JsonObject = buildJsonObject {
        identity(node)
        put("boundsInside", node.bounds.toJson())
        put("boundsContains", node.bounds.toJson())
    }

    /** `text="Wi-Fi" className=android.widget.TextView` for messages. */
    fun describe(selector: JsonObject): String = selector.entries.joinToString(" ") { (key, value) ->
        val rendered = when (value) {
            is JsonPrimitive -> if (value.isString) "\"${value.content.take(60)}\"" else value.content
            is JsonObject -> Bounds.fromJson(value)?.compact() ?: value.toString()
            else -> value.toString()
        }
        "$key=$rendered"
    }

    /** `(.*:id/)?name` as a whole-value pattern: the entry name of any package. */
    fun shortIdPattern(name: String): String = "(?:.*:id/)?" + Regex.escape(name)

    /** `(.*\.)?Name` as a whole-value pattern: the short class name of any package. */
    fun shortClassPattern(name: String): String = "(?:.*\\.)?" + Regex.escape(name)

    private fun kotlinx.serialization.json.JsonObjectBuilder.identity(node: UiNodeInfo) {
        if (node.className.isNotEmpty()) put("className", node.className)
        if (node.id.isNotEmpty()) put("id", node.id)
        if (node.text.isNotEmpty() && node.text.length <= MAX_FINGERPRINT_TEXT) put("text", node.text)
        if (node.desc.isNotEmpty() && node.desc.length <= MAX_FINGERPRINT_TEXT) put("desc", node.desc)
    }

    private fun string(tool: String, name: String, key: String, element: JsonElement): String {
        val text = (element as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("$tool: $name.$key must be a string")
        if (text.isBlank()) fail("$tool: $name.$key must not be blank")
        if (text.length > MAX_STRING_LENGTH) fail("$tool: $name.$key is longer than $MAX_STRING_LENGTH characters")
        return text
    }

    private fun integer(element: JsonElement): Int? {
        val primitive = (element as? JsonPrimitive)?.takeIf { !it.isString } ?: return null
        return primitive.longOrNull?.toInt() ?: primitive.doubleOrNull?.takeIf { it == Math.floor(it) }?.toInt()
    }

    private fun bounds(element: JsonElement): Bounds? {
        val json = element as? JsonObject ?: return null
        if (json.keys.any { it !in EDGES }) return null
        val edges = EDGES.map { edge -> integer(json[edge] ?: return null) ?: return null }
        return Bounds(edges[0], edges[1], edges[2], edges[3])
    }

    private val EDGES = listOf("left", "top", "right", "bottom")

    private fun fail(message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments(message))
}
