package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlin.math.abs

/** A screen rectangle in device pixels, as the host's `BridgeBounds` JSON (`{left, top, right, bottom}`) carries it. */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    val centerX: Int
        get() = (left + right) / 2

    val centerY: Int
        get() = (top + bottom) / 2

    /** Sum of the absolute edge differences; 0 for identical rectangles. */
    fun distanceTo(other: Bounds): Int =
        abs(left - other.left) + abs(top - other.top) + abs(right - other.right) + abs(bottom - other.bottom)

    /** The rectangle grown by [dx] / [dy] on every side, never below 0. */
    fun expanded(dx: Int, dy: Int): Bounds = Bounds((left - dx).coerceAtLeast(0), (top - dy).coerceAtLeast(0), right + dx, bottom + dy)

    /** `[l,t][r,b]`, the compact form of appendix B. */
    fun compact(): String = "[$left,$top][$right,$bottom]"

    fun toJson(): JsonObject = buildJsonObject {
        put("left", left)
        put("top", top)
        put("right", right)
        put("bottom", bottom)
    }

    companion object {

        val EMPTY = Bounds(0, 0, 0, 0)

        fun fromJson(element: JsonElement?): Bounds? {
            val json = element as? JsonObject ?: return null
            fun edge(name: String): Int? = (json[name] as? JsonPrimitive)?.intOrNull
            return Bounds(edge("left") ?: return null, edge("top") ?: return null, edge("right") ?: return null, edge("bottom") ?: return null)
        }
    }
}

/**
 * One accessibility node as the host describes it (roadmap P3.2): the seven frozen descriptor
 * fields of `findOne` / `findAll` / `explain`, and, from `accessibility.dump`, the position in
 * the tree and the state flags. Missing strings are empty; flags the host did not send are false.
 */
data class UiNodeInfo(
    val className: String,
    val text: String,
    val desc: String,
    val id: String,
    val bounds: Bounds,
    val clickable: Boolean,
    val enabled: Boolean,
    val depth: Int = 0,
    val index: Int = -1,
    val childCount: Int = 0,
    val visible: Boolean = true,
    val scrollable: Boolean = false,
    val checkable: Boolean = false,
    val checked: Boolean = false,
    val editable: Boolean = false,
    val focused: Boolean = false,
    val selected: Boolean = false,
    val longClickable: Boolean = false,
) {

    /** The class name without its package, as the compact tree shows it. */
    val shortClassName: String
        get() = className.substringAfterLast('.')

    /** The view id without `package:id/`, as the compact tree shows it. */
    val shortId: String
        get() = id.substringAfter(":id/", id)

    /** A short label for messages: `TextView "Wi-Fi"`, `Switch id=switch_widget`, `View [0,0][10,10]`. */
    fun label(): String = buildString {
        append(shortClassName.ifEmpty { "node" })
        when {
            text.isNotEmpty() -> append(' ').append('"').append(text.take(40)).append('"')
            desc.isNotEmpty() -> append(" desc=\"").append(desc.take(40)).append('"')
            id.isNotEmpty() -> append(" id=").append(shortId)
            else -> append(' ').append(bounds.compact())
        }
    }

    /**
     * The JSON a tool result carries for the node: the descriptor fields, `center`, and, when
     * [full], the tree position and every state flag (the `format = json` dump).
     */
    fun toJson(ref: String? = null, full: Boolean = false): JsonObject = buildJsonObject {
        ref?.let { put("ref", it) }
        if (full) {
            put("depth", depth)
            put("index", index)
            put("childCount", childCount)
            put("visible", visible)
        }
        put("className", className)
        put("text", text)
        put("desc", desc)
        put("id", id)
        put("bounds", bounds.toJson())
        put("center", JsonArray(listOf(JsonPrimitive(bounds.centerX), JsonPrimitive(bounds.centerY))))
        put("clickable", clickable)
        put("enabled", enabled)
        if (full) {
            put("longClickable", longClickable)
            put("scrollable", scrollable)
            put("checkable", checkable)
            put("checked", checked)
            put("editable", editable)
            put("focused", focused)
            put("selected", selected)
        }
    }

    companion object {

        /** Reads a host node object; null when it is not an object or has no bounds. */
        fun fromJson(element: JsonElement?): UiNodeInfo? {
            val json = element as? JsonObject ?: return null
            fun string(name: String): String = (json[name] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
            fun flag(name: String, default: Boolean = false): Boolean = (json[name] as? JsonPrimitive)?.booleanOrNull ?: default
            fun int(name: String, default: Int): Int = (json[name] as? JsonPrimitive)?.intOrNull ?: default
            return UiNodeInfo(
                className = string("className"),
                text = string("text"),
                desc = string("desc"),
                id = string("id"),
                bounds = Bounds.fromJson(json["bounds"]) ?: return null,
                clickable = flag("clickable"),
                enabled = flag("enabled", default = true),
                depth = int("depth", 0),
                index = int("index", -1),
                childCount = int("childCount", 0),
                visible = flag("visible", default = true),
                scrollable = flag("scrollable"),
                checkable = flag("checkable"),
                checked = flag("checked"),
                editable = flag("editable"),
                focused = flag("focused"),
                selected = flag("selected"),
                longClickable = flag("longClickable"),
            )
        }

        /** Every readable node of a host array, in order. */
        fun listFromJson(element: JsonElement?): List<UiNodeInfo> = (element as? JsonArray)?.mapNotNull(::fromJson).orEmpty()

        /** The `packageName` / `activityName` pair the host's dump and window results carry. */
        fun windowOf(json: JsonObject?): JsonObject = buildJsonObject {
            put("packageName", (json?.get("packageName") as? JsonPrimitive)?.contentOrNull.orEmpty())
            put("activityName", (json?.get("activityName") as? JsonPrimitive)?.contentOrNull.orEmpty())
        }
    }
}
