package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeRefRegistryTest {

    private var now = 1_000_000L
    private val registry = NodeRefRegistry(clock = { now }, ttlMs = 60_000L)

    private fun node(text: String, left: Int = 0): UiNodeInfo =
        UiNodeInfo("android.widget.TextView", text, "", "", Bounds(left, 0, left + 100, 50), clickable = true, enabled = true)

    @Test
    fun `a dump numbers its nodes from one and names the snapshot`() {
        val first = registry.replace(listOf(node("a"), node("b")))
        assertEquals("s1", first.snapshotId)
        assertEquals(listOf("#n1", "#n2"), first.refs)
        assertEquals("s1", registry.currentSnapshotId)
        assertEquals(2, registry.size)

        val found = registry.resolve("#n2") as NodeRefRegistry.Resolution.Found
        assertEquals("b", found.entry.node.text)
        assertEquals("s1", found.snapshotId)
        assertEquals("#n2", found.entry.ref)
        assertTrue(registry.resolve("n1") is NodeRefRegistry.Resolution.Found)
        assertTrue(registry.resolve(" 1 ") is NodeRefRegistry.Resolution.Found)
    }

    @Test
    fun `the next dump replaces the snapshot and finds append to it`() {
        registry.replace(listOf(node("a"), node("b")))
        val appended = registry.append(listOf(node("c")))
        assertEquals("s1", appended.snapshotId)
        assertEquals(listOf("#n3"), appended.refs)
        assertEquals(3, registry.size)

        val second = registry.replace(listOf(node("x")))
        assertEquals("s2", second.snapshotId)
        assertEquals(listOf("#n1"), second.refs)
        val stale = registry.resolve("#n3") as NodeRefRegistry.Resolution.Stale
        assertTrue(stale.detail, stale.detail.contains("not in the current snapshot s2"))
        assertEquals("x", (registry.resolve("#n1") as NodeRefRegistry.Resolution.Found).entry.node.text)
    }

    @Test
    fun `a find without a dump starts a snapshot`() {
        assertNull(registry.currentSnapshotId)
        val stale = registry.resolve("#n1") as NodeRefRegistry.Resolution.Stale
        assertTrue(stale.detail, stale.detail.contains("no ui_dump"))
        val registration = registry.append(listOf(node("a")))
        assertEquals("s1", registration.snapshotId)
        assertEquals(listOf("#n1"), registration.refs)
    }

    @Test
    fun `references expire after the ttl and malformed ones are refused`() {
        registry.replace(listOf(node("a")))
        now += 59_000L
        assertTrue(registry.resolve("#n1") is NodeRefRegistry.Resolution.Found)
        now += 2_000L
        val expired = registry.resolve("#n1") as NodeRefRegistry.Resolution.Stale
        assertTrue(expired.detail, expired.detail.startsWith("expired"))
        assertEquals(0, registry.size)

        listOf("", "#n0", "#nx", "node", "#n-1").forEach { ref ->
            val stale = registry.resolve(ref) as NodeRefRegistry.Resolution.Stale
            assertTrue(ref, stale.detail.contains("not a node reference"))
        }
        assertNull(NodeRefRegistry.parse("#n"))
        assertEquals(12, NodeRefRegistry.parse("#n12"))
        assertEquals("#n7", NodeRefRegistry.refOf(7))
        assertEquals("s1: 0 refs", registry.describe())
        registry.clear()
        assertEquals("none: 0 refs", registry.describe())
    }
}
