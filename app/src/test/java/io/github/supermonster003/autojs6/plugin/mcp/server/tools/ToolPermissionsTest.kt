package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolPermissionsTest {

    @Test
    fun `defaults come from the group table`() {
        val permissions = ToolPermissions.DEFAULT
        ToolGroup.entries.forEach { group -> assertEquals(group.id, group.defaultEnabled, permissions.isEnabled(group)) }
        assertTrue(permissions.overrides.isEmpty())
    }

    @Test
    fun `overrides round-trip through the document`() {
        val permissions = ToolPermissions.DEFAULT.with(ToolGroup.SHELL, true).with(ToolGroup.SCRIPT, false)

        val encoded = permissions.encode()
        assertEquals(
            """{"format":1,"groups":{"script":false,"ui":true,"ui_gesture":false,"screen":true,"files":true,"files_delete":false,"device":true,"shell":true}}""",
            encoded,
        )
        val decoded = ToolPermissions.decode(encoded)
        assertEquals(permissions.effective, decoded.effective)
        assertTrue(decoded.isEnabled(ToolGroup.SHELL))
        assertFalse(decoded.isEnabled(ToolGroup.SCRIPT))
    }

    @Test
    fun `unknown groups and broken documents fall back to the defaults`() {
        assertEquals(ToolPermissions.DEFAULT.effective, ToolPermissions.decode(null).effective)
        assertEquals(ToolPermissions.DEFAULT.effective, ToolPermissions.decode("{{").effective)
        assertEquals(ToolPermissions.DEFAULT.effective, ToolPermissions.decode("""{"format":1}""").effective)
        val partial = ToolPermissions.decode("""{"format":1,"groups":{"future_group":true,"ui":"yes","files_delete":true}}""")
        assertEquals(mapOf(ToolGroup.FILES_DELETE to true), partial.overrides)
        assertTrue(partial.isEnabled(ToolGroup.UI))
        assertTrue(partial.isEnabled(ToolGroup.FILES_DELETE))
    }
}
