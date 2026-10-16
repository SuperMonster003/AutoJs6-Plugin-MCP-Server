package io.github.supermonster003.autojs6.plugin.mcp.server.store

import io.github.supermonster003.autojs6.plugin.mcp.server.tools.PluginToolSpecsTest
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.PluginToolsReport
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginToolsSnapshotStoreTest {

    @Test
    fun `the snapshot keeps groups, trust, status and tool risks but no descriptions`() {
        val report = PluginToolsReport.parse(Json.parseToJsonElement(PluginToolSpecsTest.LIST))!!
        val text = PluginToolsSnapshotStore.encode(report)
        assertFalse("descriptions stay out of the snapshot", text.contains("Converts Chinese text"))
        val groups = PluginToolsSnapshotStore.decode(text)
        assertEquals(listOf("fake", "opencc", "quant"), groups.map { it.shortName })
        val (fake, opencc, quant) = groups
        assertEquals(mapOf("opencc_convert" to "readonly"), opencc.tools)
        assertFalse(opencc.userGranted)
        assertFalse(opencc.hasWritingTools)
        assertTrue(quant.hasWritingTools)
        assertTrue(fake.userGranted)
        assertEquals("org.example.fake", fake.packageName)
        assertEquals(emptyList<PluginToolsGroup>(), PluginToolsSnapshotStore.decode(null))
        assertEquals(emptyList<PluginToolsGroup>(), PluginToolsSnapshotStore.decode("""{"groups":[{"shortName":"x"}]}"""))
    }
}
