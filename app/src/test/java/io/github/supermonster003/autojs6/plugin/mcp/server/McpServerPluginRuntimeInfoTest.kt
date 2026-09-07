package io.github.supermonster003.autojs6.plugin.mcp.server

import org.autojs.plugin.common.api.PluginActions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class McpServerPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = McpServerPluginRuntimeInfo(
            name = "MCP Server",
            description = "Exposes device automation to AI agents over the Model Context Protocol",
            instruction = "# MCP Server",
            versionName = "1.0.0",
            versionCode = 1L,
            versionDate = "Sep 7, 2026",
        )

        assertEquals("MCP Server", info.name)
        assertEquals("Exposes device automation to AI agents over the Model Context Protocol", info.description)
        assertEquals("# MCP Server", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("mcp-server", info.id)
        assertEquals("mcp-server", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.0.0", info.versionName)
        assertEquals(1L, info.versionCode)
        assertEquals("Sep 7, 2026", info.versionDate)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5279L, info.requiresHostVersion)
        assertEquals(McpServerPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.mcp.server", McpServerPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", McpServerPlugin.HOST_PACKAGE_NAME)
        assertEquals("mcp-server", McpServerPlugin.ID)
        assertEquals(McpServerPlugin.ID, McpServerPlugin.ENGINE)
        assertEquals("default", McpServerPlugin.VARIANT)
        assertEquals("org.autojs.plugin.MCP_SERVER", McpServerPlugin.SERVICE_ACTION)
        assertEquals("mcp-server", McpServerPlugin.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.INFO", McpServerPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, McpServerPlugin.INFO_ACTION)
        assertEquals("org.autojs.plugin.mcp.server.api.IMcpServerPlugin", McpServerPlugin.SERVICE_DESCRIPTOR)
        assertEquals(9637, McpServerPlugin.DEFAULT_PORT)
        assertEquals("/mcp", McpServerPlugin.ENDPOINT_PATH)
    }
}
