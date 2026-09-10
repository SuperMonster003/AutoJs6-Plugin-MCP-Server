package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.autojs.plugin.mcp.server.api.McpServerContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ToolCatalogTest {

    @Test
    fun `tool names are unique snake_case group_action identifiers`() {
        val names = ToolCatalog.all.map { it.name }
        assertEquals(names, names.distinct())
        names.forEach { name ->
            assertTrue(name, Regex("^[a-z]+(_[a-z]+)+$").matches(name))
        }
        assertEquals(listOf("device_ping", "device_info", "script_run"), names)
        assertEquals(ToolCatalog.all, ToolCatalog.byName.values.toList())
    }

    @Test
    fun `every input schema is a closed object whose required names exist`() {
        ToolCatalog.all.forEach { spec ->
            val schema = spec.inputSchema
            assertEquals(spec.name, "object", schema["type"]?.jsonPrimitive?.contentOrNull)
            assertEquals(spec.name, false, schema["additionalProperties"]?.jsonPrimitive?.booleanOrNull)
            assertTrue(spec.name, schema["properties"] is JsonObject)
            spec.required.forEach { name -> assertTrue("${spec.name}.$name", name in spec.properties) }
            spec.properties.forEach { (name, property) ->
                assertNotNull("${spec.name}.$name needs a type", JsonSchemas.typeOf(property))
                assertNotNull("${spec.name}.$name needs a description", (property as JsonObject)["description"])
            }
            assertTrue(spec.name, spec.title.isNotBlank() && spec.description.isNotBlank())
            assertTrue(spec.name, spec.timeoutMs in 1L..McpServerContract.MAX_TOOL_TIMEOUT_MS)
            assertTrue(spec.name, spec.maxTimeoutMs >= spec.timeoutMs && spec.maxTimeoutMs <= McpServerContract.MAX_TOOL_TIMEOUT_MS)
            if (spec.bridge != null) assertTrue("${spec.name} needs permissions", spec.permissions.isNotEmpty())
        }
    }

    @Test
    fun `group defaults follow decision D6`() {
        val expected = mapOf(
            "script" to true,
            "ui" to true,
            "ui_gesture" to false,
            "screen" to true,
            "files" to true,
            "files_delete" to false,
            "device" to true,
            "shell" to false,
        )
        assertEquals(expected, ToolGroup.entries.associate { it.id to it.defaultEnabled })
        assertEquals(expected.keys.toList(), ToolGroup.IDS)
        assertEquals(ToolGroup.UI_GESTURE, ToolGroup.fromId("ui_gesture"))
        assertEquals(null, ToolGroup.fromId("nope"))
    }

    @Test
    fun `enabled rows follow the group switches and keep the catalog order`() {
        assertEquals(listOf("device_ping", "device_info", "script_run"), ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name })
        assertEquals(listOf("device_ping", "device_info"), ToolCatalog.enabled(ToolPermissions.DEFAULT.with(ToolGroup.SCRIPT, false)).map { it.name })
        assertEquals(listOf("script_run"), ToolCatalog.enabled(ToolPermissions.DEFAULT.with(ToolGroup.DEVICE, false)).map { it.name })
        assertEquals(listOf(ToolGroup.SCRIPT, ToolGroup.DEVICE), ToolCatalog.groups)
        assertEquals(
            """{"script":true,"ui":true,"ui_gesture":false,"screen":true,"files":true,"files_delete":false,"device":true,"shell":false}""",
            ToolCatalog.summary(ToolPermissions.DEFAULT),
        )
    }

    @Test
    fun `the script_run row maps onto engines execScript with the documented bounds`() {
        val spec = ToolCatalog.scriptRun
        assertEquals(BridgeMethod("engines", "execScript"), spec.bridge)
        assertEquals("engines.execScript", spec.bridge?.key)
        assertEquals(listOf("engines", "engines.exec"), spec.permissions)
        assertEquals(listOf("source"), spec.required)
        assertEquals(
            listOf("source", "name", "workingDirectory", "arguments", "timeoutMs", "waitForCompletion", "captureConsole", "maxConsoleLines"),
            spec.properties.keys.toList(),
        )
        assertEquals("integer", spec.propertyType("timeoutMs"))
        val timeout = spec.properties["timeoutMs"] as JsonObject
        assertEquals(1000L, timeout["minimum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(McpServerContract.MAX_TOOL_TIMEOUT_MS, timeout["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(60_000L, timeout["default"]?.jsonPrimitive?.content?.toLong())
        assertEquals(McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong(), (spec.properties["maxConsoleLines"] as JsonObject)["maximum"]?.jsonPrimitive?.content?.toLong())
        assertTrue(spec.hints.openWorld)
        assertFalse(spec.hints.readOnly)
        assertEquals(ToolGroup.SCRIPT, spec.group)
    }

    @Test
    fun `the device rows are read-only local and bridge tools`() {
        assertEquals(null, ToolCatalog.devicePing.bridge)
        assertTrue(ToolCatalog.devicePing.hints.readOnly && ToolCatalog.devicePing.hints.idempotent)
        assertEquals(BridgeMethod("device", "info"), ToolCatalog.deviceInfo.bridge)
        assertEquals(listOf("device"), ToolCatalog.deviceInfo.permissions)
        assertTrue(ToolCatalog.deviceInfo.hints.readOnly)
        assertTrue(ToolCatalog.deviceInfo.properties.isEmpty())
    }

    @Test
    fun `the SDK view carries name title schema and annotations`() {
        val tool = ToolCatalog.scriptRun.toSdkTool()
        assertEquals("script_run", tool.name)
        assertEquals("Run script", tool.title)
        assertEquals(ToolCatalog.scriptRun.description, tool.description)
        assertEquals(listOf("source"), tool.inputSchema.required)
        assertEquals(ToolCatalog.scriptRun.properties, tool.inputSchema.properties)
        assertEquals(true, tool.annotations?.openWorldHint)
        assertEquals(false, tool.annotations?.readOnlyHint)
        val ping = ToolCatalog.devicePing.toSdkTool()
        assertEquals(null, ping.inputSchema.required)
        assertEquals(true, ping.annotations?.readOnlyHint)
    }

    @Test
    fun `the catalog snapshot matches the committed file`() {
        val actual = ToolCatalog.snapshot()
        val expected = javaClass.classLoader?.getResourceAsStream(SNAPSHOT_RESOURCE)?.bufferedReader()?.use { it.readText() }
        if (expected == null || expected.trim() != actual.trim()) {
            val dump = File("build/$SNAPSHOT_RESOURCE.actual.json").apply { parentFile.mkdirs() }
            dump.writeText(actual + "\n")
            throw AssertionError(
                "The tool catalog changed. Review the diff, then copy ${dump.path} to src/test/resources/$SNAPSHOT_RESOURCE " +
                        "and mention the change in the changelog and the docs.",
            )
        }
        val rows = kotlinx.serialization.json.Json.parseToJsonElement(actual) as JsonArray
        assertEquals(ToolCatalog.all.size, rows.size)
        assertEquals("local", (rows[0] as JsonObject)["bridge"]?.jsonPrimitive?.content)
        assertEquals(JsonPrimitive("engines.execScript"), (rows[2] as JsonObject)["bridge"])
    }

    private companion object {
        const val SNAPSHOT_RESOURCE = "tool-catalog.snapshot.json"
    }
}
