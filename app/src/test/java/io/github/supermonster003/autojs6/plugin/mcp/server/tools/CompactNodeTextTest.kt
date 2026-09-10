package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactNodeTextTest {

    private val registry = NodeRefRegistry(clock = { 0L })

    private fun window(packageName: String = "com.android.settings", activity: String = "com.android.settings.Settings") =
        Json.parseToJsonElement("""{"packageName":"$packageName","activityName":"$activity"}""").jsonObject

    private fun node(
        className: String,
        depth: Int,
        bounds: Bounds,
        text: String = "",
        desc: String = "",
        id: String = "",
        childCount: Int = 0,
        clickable: Boolean = false,
        enabled: Boolean = true,
        visible: Boolean = true,
        scrollable: Boolean = false,
        checkable: Boolean = false,
        checked: Boolean = false,
        editable: Boolean = false,
        focused: Boolean = false,
        selected: Boolean = false,
        longClickable: Boolean = false,
    ) = UiNodeInfo(className, text, desc, id, bounds, clickable, enabled, depth, if (depth == 0) -1 else 0, childCount, visible, scrollable, checkable, checked, editable, focused, selected, longClickable)

    @Test
    fun `renders the appendix B layout with references markers and positions`() {
        val entries = registry.replace(
            listOf(
                node("android.widget.FrameLayout", 0, Bounds(0, 0, 1080, 2400), childCount = 1),
                node("androidx.recyclerview.widget.RecyclerView", 1, Bounds(0, 220, 1080, 2400), childCount = 1, scrollable = true, id = "com.android.settings:id/recycler_view"),
                node("android.widget.LinearLayout", 2, Bounds(0, 220, 1080, 376), childCount = 2, clickable = true, id = "com.android.settings:id/recycler_item"),
                node("android.widget.TextView", 3, Bounds(0, 220, 1080, 320), text = "Wi-Fi"),
                node("android.widget.Switch", 3, Bounds(900, 250, 1060, 346), checkable = true, checked = true, clickable = true, id = "android:id/switch_widget"),
            ),
        ).entries

        val text = CompactNodeText.render(window(), entries, truncated = false, maxNodes = 200, maxDepth = 32)

        assertEquals(
            """
            window: com.android.settings/.Settings  bounds=[0,0][1080,2400]  nodes=5
            #n1 FrameLayout [0,0][1080,2400]
            #n2  RecyclerView scrollable id=recycler_view [0,220][1080,2400]
            #n3   LinearLayout clickable id=recycler_item [0,220][1080,376]
            #n4    TextView "Wi-Fi" c=(540,270)
            #n5    Switch clickable checkable checked id=switch_widget c=(980,298)
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun `escapes and clips text and marks disabled hidden and unlisted children`() {
        val longText = "a".repeat(45)
        val entries = registry.replace(
            listOf(
                node("android.widget.FrameLayout", 0, Bounds(0, 0, 100, 100), childCount = 3),
                node("android.widget.TextView", 1, Bounds(0, 0, 100, 20), text = "line\nbreak \"quoted\" \\ back", enabled = false),
                node("android.widget.EditText", 1, Bounds(0, 20, 100, 40), text = longText, desc = "Search", editable = true, focused = true, longClickable = true, selected = true),
                node("android.view.View", 1, Bounds(0, 40, 100, 60), visible = false, childCount = 2),
            ),
        ).entries

        val text = CompactNodeText.render(window(packageName = "", activity = ""), entries, truncated = true, maxNodes = 4, maxDepth = 1)
        val lines = text.lines()

        assertEquals("window: ?  bounds=[0,0][100,100]  nodes=4  truncated=true", lines[0])
        assertEquals("#n2  TextView !enabled \"line\\nbreak \\\"quoted\\\" \\\\ back\" c=(50,10)", lines[2])
        assertEquals("#n3  EditText long_clickable editable focused selected \"${"a".repeat(40)}...\" desc=\"Search\" c=(50,30)", lines[3])
        assertEquals("#n4  View hidden [0,40][100,60]", lines[4])
        assertTrue(lines[5], lines[5].startsWith("(the maxNodes=4 budget cut the tree; 1 listed nodes have children that are not shown (visibleOnly, maxDepth=1, or the budget); raise maxNodes (up to 400)"))
        assertEquals(1, CompactNodeText.nodesWithUnlistedChildren(entries))
        assertEquals(listOf("clickable", "checked", "!enabled", "hidden"), CompactNodeText.markers(node("x", 0, Bounds.EMPTY, clickable = true, checked = true, enabled = false, visible = false)))
    }

    @Test
    fun `short forms of activity id and class follow the dump conventions`() {
        assertEquals(".Settings", CompactNodeText.shortActivity("com.android.settings", "com.android.settings.Settings"))
        assertEquals("com.other.Activity", CompactNodeText.shortActivity("com.android.settings", "com.other.Activity"))
        assertEquals("Full", CompactNodeText.shortActivity("", "Full"))
        val node = node("android.widget.Button", 0, Bounds(0, 0, 10, 10), id = "com.example:id/ok")
        assertEquals("Button", node.shortClassName)
        assertEquals("ok", node.shortId)
        assertEquals("Button id=ok", node.label())
        assertEquals("TextView \"hello\"", node("android.widget.TextView", 0, Bounds(0, 0, 10, 10), text = "hello").label())
        assertEquals("View [0,0][10,10]", node("android.view.View", 0, Bounds(0, 0, 10, 10)).label())
        assertEquals("abc", CompactNodeText.clip("abc"))
        assertEquals("tab\\there", CompactNodeText.clip("tab\there"))
    }
}
