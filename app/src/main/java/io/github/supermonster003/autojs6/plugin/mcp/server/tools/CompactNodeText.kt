package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The compact node tree of `ui_dump` (roadmap D12, appendix B): one line per node with its
 * `#n` reference, an indent per depth, the short class name, the state markers that are true,
 * the text, the description, the short id, and the position (bounds for a node with children,
 * the centre for a leaf). A header names the window and the node count; a footer says what
 * the depth or node budget left out. Pure Kotlin; `CompactNodeTextTest` covers it.
 */
object CompactNodeText {

    /** Longest text or description shown on a line; the rest becomes `...`. */
    const val MAX_TEXT_CHARS = 40

    /**
     * Renders [entries] (the registered nodes of one dump, in pre-order) below a header for
     * [window] (`packageName` / `activityName`); [truncated] is the host's flag that the node
     * budget cut the walk, [maxNodes] and [maxDepth] name the limits in the footer.
     */
    fun render(window: JsonObject, entries: List<NodeRefRegistry.Entry>, truncated: Boolean, maxNodes: Long, maxDepth: Long): String = buildString {
        val packageName = window.string("packageName")
        val activityName = window.string("activityName")
        append("window: ").append(packageName.ifEmpty { "?" })
        if (activityName.isNotEmpty()) append('/').append(shortActivity(packageName, activityName))
        rootBounds(entries)?.let { append("  bounds=").append(it.compact()) }
        append("  nodes=").append(entries.size)
        if (truncated) append("  truncated=true")
        entries.forEach { entry -> append('\n').append(line(entry)) }
        val unlisted = nodesWithUnlistedChildren(entries)
        if (truncated || unlisted > 0) {
            append("\n(")
            if (truncated) append("the maxNodes=$maxNodes budget cut the tree; ")
            if (unlisted > 0) append("$unlisted listed nodes have children that are not shown (visibleOnly, maxDepth=$maxDepth, or the budget); ")
            append("raise maxNodes (up to ${ToolCatalog.Ui.MAX_NODES_LIMIT}), lower maxDepth, or use ui_find)")
        }
    }

    /** One line: `#n5    Switch checked clickable id=switch_widget c=(980,298)`. */
    fun line(entry: NodeRefRegistry.Entry): String {
        val node = entry.node
        return buildString {
            append(entry.ref).append(' ')
            repeat(node.depth) { append(' ') }
            append(node.shortClassName.ifEmpty { "?" })
            markers(node).forEach { append(' ').append(it) }
            if (node.text.isNotEmpty()) append(" \"").append(clip(node.text)).append('"')
            if (node.desc.isNotEmpty()) append(" desc=\"").append(clip(node.desc)).append('"')
            if (node.id.isNotEmpty()) append(" id=").append(node.shortId)
            append(' ')
            if (node.childCount > 0) append(node.bounds.compact()) else append("c=(").append(node.bounds.centerX).append(',').append(node.bounds.centerY).append(')')
        }
    }

    /** The state markers of appendix B, only the true ones, plus `hidden` for an invisible node. */
    fun markers(node: UiNodeInfo): List<String> = buildList {
        if (node.clickable) add("clickable")
        if (node.longClickable) add("long_clickable")
        if (node.checkable) add("checkable")
        if (node.checked) add("checked")
        if (node.scrollable) add("scrollable")
        if (node.editable) add("editable")
        if (node.focused) add("focused")
        if (node.selected) add("selected")
        if (!node.enabled) add("!enabled")
        if (!node.visible) add("hidden")
    }

    /** Escapes quotes, backslashes, and line breaks, and cuts to [MAX_TEXT_CHARS]. */
    fun clip(text: String): String {
        val escaped = buildString {
            for (ch in text) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
        return if (escaped.length <= MAX_TEXT_CHARS) escaped else escaped.take(MAX_TEXT_CHARS) + "..."
    }

    /** `.Settings` for `com.android.settings.Settings` under `com.android.settings`. */
    fun shortActivity(packageName: String, activityName: String): String =
        if (packageName.isNotEmpty() && activityName.startsWith("$packageName.")) activityName.substring(packageName.length) else activityName

    /** The union of the root bounds (depth 0 entries), or null without roots. */
    fun rootBounds(entries: List<NodeRefRegistry.Entry>): Bounds? {
        val roots = entries.filter { it.node.depth == 0 }.map { it.node.bounds }
        if (roots.isEmpty()) return null
        return Bounds(roots.minOf { it.left }, roots.minOf { it.top }, roots.maxOf { it.right }, roots.maxOf { it.bottom })
    }

    /** How many entries report more children than the pre-order list shows below them. */
    fun nodesWithUnlistedChildren(entries: List<NodeRefRegistry.Entry>): Int {
        var count = 0
        for ((i, entry) in entries.withIndex()) {
            if (entry.node.childCount <= 0) continue
            var listed = 0
            var j = i + 1
            while (j < entries.size && entries[j].node.depth > entry.node.depth) {
                if (entries[j].node.depth == entry.node.depth + 1) listed++
                j++
            }
            if (listed < entry.node.childCount) count++
        }
        return count
    }

    private fun JsonObject.string(name: String): String = (this[name] as? JsonPrimitive)?.contentOrNull.orEmpty()
}
