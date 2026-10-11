package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The plugin side of the host snapshots behind `#n` references (roadmap P10). */
class HostSnapshotsTest {

    private val dump = listOf(
        "window: com.android.settings/.Settings  bounds=[0,0][1080,2400]  nodes=4",
        "#n1 FrameLayout [0,0][1080,2400]",
        "#n2  RecyclerView scrollable id=com.android.settings:id/list [0,200][1080,2400]",
        "#n3   TextView clickable long_clickable \"Wi-Fi c=(1,2) hidden\" c=(540,250)",
        "#n4   Switch checkable hidden id=android:id/switch_widget c=(980,298)",
        "(1 listed nodes have children that are not shown (visibleOnly, maxDepth=5, or the budget); raise maxNodes (up to 400), lower maxDepth, or use ui_find)",
    ).joinToString("\n")

    @Test
    fun `rows keep the bounds of containers and a square around the centre of leaves`() {
        val rows = HostSnapshots.parseRows(dump)

        assertEquals(listOf("#n1", "#n2", "#n3", "#n4"), rows.map { it.ref })
        assertEquals(HostSnapshots.Row("#n2", Bounds(0, 200, 1080, 2400), leaf = false, hidden = false), rows[1])
        // The quoted text mentions c=(...) and hidden; only the line end and the markers count.
        assertEquals(HostSnapshots.Row("#n3", Bounds(524, 234, 556, 266), leaf = true, hidden = false), rows[2])
        assertEquals(true, rows[3].hidden)
        assertEquals(emptyList<HostSnapshots.Row>(), HostSnapshots.parseRows("window: ?  nodes=0"))
    }

    @Test
    fun `the newest dump is remembered until cleared`() {
        var now = 5L
        val snapshots = HostSnapshots(clock = { now })
        assertNull(snapshots.latest)
        assertEquals("none", snapshots.describe())

        snapshots.record("a:1", dump)
        now = 9L
        val second = snapshots.record("a:2", "#n1 View c=(5,5)")
        assertEquals("a:2", snapshots.latest?.id)
        assertEquals(9L, second.takenAt)
        assertEquals("a:2: 1 rows", snapshots.describe())

        snapshots.clear()
        assertNull(snapshots.latest)
    }

    @Test
    fun `references are normalized to the form the host accepts`() {
        assertEquals("#n12", HostSnapshots.normalizeRef("#n12"))
        assertEquals("#n12", HostSnapshots.normalizeRef(" n12 "))
        assertEquals("#n12", HostSnapshots.normalizeRef("12"))
        assertNull(HostSnapshots.normalizeRef("#n0"))
        assertNull(HostSnapshots.normalizeRef("#n12345"))
        assertNull(HostSnapshots.normalizeRef("#x1"))
        assertNull(HostSnapshots.normalizeRef(""))
        assertNull(HostSnapshots.normalizeRef(null))
    }
}
