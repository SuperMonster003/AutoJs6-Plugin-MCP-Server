package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpServerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    private var permissions = ToolPermissions.DEFAULT
    private val registry = ToolRegistry(ToolCatalog.all, { permissions }) { _, _, _ -> ToolResults.success(JsonObject(emptyMap())) }

    @Test
    fun `the registration follows the switches and keeps the catalog order when a group comes back`() = runBlocking {
        registry.install(McpServerFactory.create("1"))
        assertEquals(ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name }, registry.enabledNames)
        assertFalse("nothing changed", registry.refresh())
        assertEquals(ToolErrorCodes.TOOL_DISABLED, registry.disabledFailure(ToolCatalog.UI_SWIPE)?.code)
        assertNull(registry.disabledFailure(ToolCatalog.UI_DUMP))
        assertNull(registry.disabledFailure("no_such_tool"))

        permissions = ToolPermissions.DEFAULT.with(ToolGroup.SCRIPT, false)
        assertTrue(registry.refresh())
        assertEquals(ToolCatalog.enabled(permissions).map { it.name }, registry.enabledNames)
        assertEquals(ToolErrorCodes.TOOL_DISABLED, registry.disabledFailure(ToolCatalog.SCRIPT_RUN)?.code)

        // The script group comes back: its rows return to their catalog position, not to the end.
        permissions = ToolPermissions.DEFAULT.with(ToolGroup.UI_GESTURE, true)
        assertTrue(registry.refresh())
        assertEquals(ToolCatalog.enabled(permissions).map { it.name }, registry.enabledNames)
        assertNull(registry.disabledFailure(ToolCatalog.UI_SWIPE))

        permissions = ToolPermissions.DEFAULT
        assertTrue(registry.refresh())
        assertEquals(ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name }, registry.enabledNames)
        assertFalse(registry.refresh())
    }
}
