package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpServerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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

    @Test
    fun `plugin-family rows follow the grant and the host report and answer with the reason while hidden`() = runBlocking {
        val ocrSpec = ToolSpec(
            name = "ocr_test_probe",
            title = "OCR probe",
            description = "test row of a plugin-family group",
            inputSchema = JsonSchemas.objectSchema(),
            group = ToolGroup.OCR,
            bridge = BridgeMethod("ocr", "recognize"),
            permissions = listOf("ocr"),
            hostMethods = listOf(BridgeMethod("ocr", "recognize"), BridgeMethod("image", "readImage")),
        )
        val report = PluginFamilyReport.parse(Json.parseToJsonElement(
            """{"schema":"autojs6-bridge-app-plugin-capabilities-v1","families":[{"id":"ocr","status":"available","methods":["ocr.recognize"],"plugins":[]}]}""",
        ))!!
        val granted = setOf("ocr.recognize", "image.readImage", "app.pluginCapabilities")
        var availability = PluginToolAvailability.NONE
        val registry = ToolRegistry(ToolCatalog.all + ocrSpec, { permissions }, { availability }) { _, _, _ -> ToolResults.success(JsonObject(emptyMap())) }
        val defaults = ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name }

        registry.install(McpServerFactory.create("1"))
        assertEquals("plugin-family rows wait for the first refresh", defaults, registry.enabledNames)
        assertFalse(registry.refresh())
        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, registry.disabledFailure(ocrSpec.name)?.code)

        availability = PluginToolAvailability(granted, report)
        assertTrue(registry.refresh())
        assertEquals(defaults + ocrSpec.name, registry.enabledNames)
        assertNull(registry.disabledFailure(ocrSpec.name))
        assertFalse(registry.refresh())
        assertEquals(availability, registry.pluginAvailability)

        availability = PluginToolAvailability(granted - "image.readImage", report)
        assertTrue(registry.refresh())
        assertEquals(defaults, registry.enabledNames)
        assertEquals(ToolErrorCodes.CAPABILITY_DENIED, registry.disabledFailure(ocrSpec.name)?.code)

        availability = PluginToolAvailability(granted, PluginFamilyReport(families = mapOf("ocr" to PluginFamilyStatus("ocr", PluginFamilyStatus.STATUS_NOT_INSTALLED))))
        assertFalse("hidden before and after: nothing to announce", registry.refresh())
        assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, registry.disabledFailure(ocrSpec.name)?.code)

        availability = PluginToolAvailability(granted, report)
        assertTrue(registry.refresh())
        permissions = ToolPermissions.DEFAULT.with(ToolGroup.OCR, false)
        assertTrue(registry.refresh())
        assertEquals(defaults, registry.enabledNames)
        assertEquals("the group switch wins over the host state", ToolErrorCodes.TOOL_DISABLED, registry.disabledFailure(ocrSpec.name)?.code)
        permissions = ToolPermissions.DEFAULT
        assertTrue(registry.refresh())
        assertEquals(defaults + ocrSpec.name, registry.enabledNames)
    }
}
