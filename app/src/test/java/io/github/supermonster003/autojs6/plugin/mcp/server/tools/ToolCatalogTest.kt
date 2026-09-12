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
        assertEquals(DEVICE_NAMES + SCRIPT_NAMES + UI_NAMES + GESTURE_NAMES + SCREEN_NAMES, names)
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
        assertEquals(DEVICE_NAMES + SCRIPT_NAMES + UI_NAMES + SCREEN_NAMES, ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name })
        assertEquals(DEVICE_NAMES + UI_NAMES + SCREEN_NAMES, ToolCatalog.enabled(ToolPermissions.DEFAULT.with(ToolGroup.SCRIPT, false)).map { it.name })
        assertEquals(SCRIPT_NAMES + UI_NAMES + SCREEN_NAMES, ToolCatalog.enabled(ToolPermissions.DEFAULT.with(ToolGroup.DEVICE, false)).map { it.name })
        assertEquals(DEVICE_NAMES + SCRIPT_NAMES + SCREEN_NAMES, ToolCatalog.enabled(ToolPermissions.DEFAULT.with(ToolGroup.UI, false)).map { it.name })
        assertEquals(ToolCatalog.all.map { it.name }, ToolCatalog.enabled(ToolPermissions.DEFAULT.with(ToolGroup.UI_GESTURE, true)).map { it.name })
        assertEquals(listOf(ToolGroup.SCRIPT, ToolGroup.UI, ToolGroup.UI_GESTURE, ToolGroup.SCREEN, ToolGroup.DEVICE), ToolCatalog.groups)
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
    fun `the script group rows of P3_1 map onto the host engines and console methods`() {
        val runFile = ToolCatalog.scriptRunFile
        assertEquals(BridgeMethod("engines", "execScriptFile"), runFile.bridge)
        assertEquals(listOf("path"), runFile.required)
        assertEquals(ToolCatalog.scriptRun.properties.keys - setOf("source", "name"), runFile.properties.keys - setOf("path"))
        assertEquals(listOf("engines", "engines.exec"), runFile.permissions)
        assertEquals(McpServerContract.MAX_TOOL_TIMEOUT_MS, runFile.maxTimeoutMs)
        assertTrue(runFile.hints.openWorld)

        assertEquals(BridgeMethod("engines", "stop"), ToolCatalog.scriptStop.bridge)
        assertEquals(listOf("executionId"), ToolCatalog.scriptStop.required)
        assertEquals("integer", ToolCatalog.scriptStop.propertyType("executionId"))
        assertEquals(BridgeMethod("engines", "stopAll"), ToolCatalog.scriptStopAll.bridge)
        assertTrue(ToolCatalog.scriptStopAll.hints.destructive)
        assertTrue(ToolCatalog.scriptStopAll.properties.isEmpty())
        assertEquals(BridgeMethod("engines", "list"), ToolCatalog.scriptList.bridge)
        assertTrue(ToolCatalog.scriptList.hints.readOnly && ToolCatalog.scriptList.hints.idempotent)
        assertEquals(BridgeMethod("console", "tail"), ToolCatalog.consoleTail.bridge)
        assertEquals(listOf("console"), ToolCatalog.consoleTail.permissions)
        assertTrue(ToolCatalog.consoleTail.hints.readOnly)
        val lines = ToolCatalog.consoleTail.properties["lines"] as JsonObject
        assertEquals(1L, lines["minimum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong(), lines["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(100L, lines["default"]?.jsonPrimitive?.content?.toLong())
        val levels = (ToolCatalog.consoleTail.properties["level"] as JsonObject)["enum"] as JsonArray
        assertEquals(listOf("verbose", "debug", "info", "warn", "error", "assert"), levels.map { it.jsonPrimitive.content })
        assertEquals("integer", ToolCatalog.consoleTail.propertyType("sinceId"))
        listOf(ToolCatalog.scriptStop, ToolCatalog.scriptStopAll, ToolCatalog.scriptList, ToolCatalog.consoleTail).forEach { spec ->
            assertEquals(spec.name, ToolGroup.SCRIPT, spec.group)
            assertEquals(spec.name, 10_000L, spec.timeoutMs)
            assertEquals(spec.name, listOf("engines"), spec.permissions.takeIf { spec.bridge?.module == "engines" } ?: listOf("engines"))
        }
    }

    @Test
    fun `the ui rows of P3_2 map onto the accessibility keys and app methods with the appendix A ceilings`() {
        listOf(ToolCatalog.uiDump, ToolCatalog.uiFind, ToolCatalog.uiCurrentWindow, ToolCatalog.uiExplainSelector, ToolCatalog.uiWaitFor).forEach { spec ->
            assertEquals(spec.name, ToolGroup.UI, spec.group)
            assertTrue(spec.name, spec.hints.readOnly)
        }
        listOf(ToolCatalog.uiClick, ToolCatalog.uiLongClick, ToolCatalog.uiSetText, ToolCatalog.uiScroll, ToolCatalog.uiPressKey).forEach { spec ->
            assertEquals(spec.name, ToolGroup.UI, spec.group)
            assertFalse(spec.name, spec.hints.readOnly)
            assertEquals(spec.name, listOf("accessibility"), spec.permissions)
        }
        listOf(ToolCatalog.uiSwipe, ToolCatalog.uiGesture).forEach { spec ->
            assertEquals(spec.name, ToolGroup.UI_GESTURE, spec.group)
            assertEquals(spec.name, listOf("accessibility", "accessibility.gesture"), spec.permissions)
            assertFalse(spec.name, spec.hints.readOnly)
        }
        assertEquals(UI_NAMES + GESTURE_NAMES, ToolCatalog.all.filter { it.group == ToolGroup.UI || it.group == ToolGroup.UI_GESTURE }.map { it.name })

        assertEquals(BridgeMethod("accessibility", "dump"), ToolCatalog.uiDump.bridge)
        assertEquals(listOf("accessibility"), ToolCatalog.uiDump.permissions)
        assertEquals(listOf("format", "maxDepth", "maxNodes", "visibleOnly", "window"), ToolCatalog.uiDump.properties.keys.toList())
        val maxNodes = ToolCatalog.uiDump.properties["maxNodes"] as JsonObject
        assertEquals(McpServerContract.MAX_DUMP_NODES.toLong(), maxNodes["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(ToolCatalog.Ui.DEFAULT_MAX_NODES, maxNodes["default"]?.jsonPrimitive?.content?.toLong())
        assertEquals(McpServerContract.MAX_DUMP_DEPTH.toLong(), (ToolCatalog.uiDump.properties["maxDepth"] as JsonObject)["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(listOf("text", "json", "xml"), ((ToolCatalog.uiDump.properties["format"] as JsonObject)["enum"] as JsonArray).map { it.jsonPrimitive.content })
        assertEquals(listOf("active", "all"), ((ToolCatalog.uiDump.properties["window"] as JsonObject)["enum"] as JsonArray).map { it.jsonPrimitive.content })

        assertEquals(BridgeMethod("accessibility", "findAll"), ToolCatalog.uiFind.bridge)
        assertEquals(listOf("selector"), ToolCatalog.uiFind.required)
        assertEquals("object", ToolCatalog.uiFind.propertyType("selector"))
        val selector = ToolCatalog.uiFind.properties["selector"] as JsonObject
        assertEquals(UiSelectors.KEYS, (selector["properties"] as JsonObject).keys.toList())
        assertEquals(false, selector["additionalProperties"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(ToolCatalog.Ui.MAX_FIND_LIMIT, (ToolCatalog.uiFind.properties["limit"] as JsonObject)["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(ToolCatalog.Ui.MAX_FIND_TIMEOUT_MS, (ToolCatalog.uiFind.properties["timeoutMs"] as JsonObject)["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(BridgeMethod("app", "currentWindow"), ToolCatalog.uiCurrentWindow.bridge)
        assertEquals(listOf("app.query", "accessibility"), ToolCatalog.uiCurrentWindow.permissions)
        assertTrue(ToolCatalog.uiCurrentWindow.properties.isEmpty())
        assertEquals(BridgeMethod("accessibility", "explain"), ToolCatalog.uiExplainSelector.bridge)
        assertEquals(BridgeMethod("accessibility", "findOne"), ToolCatalog.uiWaitFor.bridge)
        assertEquals(listOf("appear", "disappear"), ((ToolCatalog.uiWaitFor.properties["state"] as JsonObject)["enum"] as JsonArray).map { it.jsonPrimitive.content })
        val waitTimeout = ToolCatalog.uiWaitFor.properties["timeoutMs"] as JsonObject
        assertEquals(ToolCatalog.Ui.MAX_WAIT_TIMEOUT_MS, waitTimeout["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(ToolCatalog.Ui.DEFAULT_WAIT_TIMEOUT_MS, waitTimeout["default"]?.jsonPrimitive?.content?.toLong())
        assertTrue(ToolCatalog.uiWaitFor.maxTimeoutMs > ToolCatalog.Ui.MAX_WAIT_TIMEOUT_MS)

        listOf(ToolCatalog.uiClick, ToolCatalog.uiLongClick).forEach { spec ->
            assertEquals(spec.name, listOf("nodeRef", "selector", "x", "y"), spec.properties.keys.toList())
            assertTrue(spec.name, spec.required.isEmpty())
            assertEquals(spec.name, ToolCatalog.Ui.MAX_COORDINATE, (spec.properties["x"] as JsonObject)["maximum"]?.jsonPrimitive?.content?.toLong())
        }
        assertEquals(BridgeMethod("accessibility", "click"), ToolCatalog.uiClick.bridge)
        assertEquals(BridgeMethod("accessibility", "longClick"), ToolCatalog.uiLongClick.bridge)
        assertEquals(BridgeMethod("accessibility", "setText"), ToolCatalog.uiSetText.bridge)
        assertEquals(listOf("nodeRef", "selector", "text", "append"), ToolCatalog.uiSetText.properties.keys.toList())
        assertEquals(listOf("text"), ToolCatalog.uiSetText.required)
        assertEquals(ToolCatalog.Ui.MAX_TEXT_BYTES.toLong(), (ToolCatalog.uiSetText.properties["text"] as JsonObject)["maxLength"]?.jsonPrimitive?.content?.toLong())
        assertEquals(BridgeMethod("accessibility", "scrollForward"), ToolCatalog.uiScroll.bridge)
        assertEquals(listOf("nodeRef", "selector", "direction", "times"), ToolCatalog.uiScroll.properties.keys.toList())
        assertEquals(listOf("forward", "backward", "down", "up", "right", "left"), ((ToolCatalog.uiScroll.properties["direction"] as JsonObject)["enum"] as JsonArray).map { it.jsonPrimitive.content })
        assertEquals(ToolCatalog.Ui.MAX_TIMES, (ToolCatalog.uiScroll.properties["times"] as JsonObject)["maximum"]?.jsonPrimitive?.content?.toLong())
        assertEquals(BridgeMethod("accessibility", "back"), ToolCatalog.uiPressKey.bridge)
        assertEquals(listOf("back", "home", "recents", "notifications", "quick_settings", "power_dialog", "lock_screen"), ((ToolCatalog.uiPressKey.properties["key"] as JsonObject)["enum"] as JsonArray).map { it.jsonPrimitive.content })
        assertEquals(listOf("key"), ToolCatalog.uiPressKey.required)

        assertEquals(BridgeMethod("accessibility", "swipe"), ToolCatalog.uiSwipe.bridge)
        assertEquals(listOf("x1", "y1", "x2", "y2"), ToolCatalog.uiSwipe.required)
        assertEquals(ToolCatalog.Ui.DEFAULT_SWIPE_DURATION_MS, (ToolCatalog.uiSwipe.properties["durationMs"] as JsonObject)["default"]?.jsonPrimitive?.content?.toLong())
        assertEquals(BridgeMethod("accessibility", "gesture"), ToolCatalog.uiGesture.bridge)
        assertEquals(listOf("durationMs", "points"), ToolCatalog.uiGesture.required)
        assertEquals("array", ToolCatalog.uiGesture.propertyType("points"))
        val points = ToolCatalog.uiGesture.properties["points"] as JsonObject
        assertEquals(ToolCatalog.Ui.MAX_POINTS.toLong(), points["maxItems"]?.jsonPrimitive?.content?.toLong())
        assertEquals(2L, points["minItems"]?.jsonPrimitive?.content?.toLong())
        assertEquals("array", (points["items"] as JsonObject)["type"]?.jsonPrimitive?.content)
        assertEquals(ToolCatalog.Ui.MAX_GESTURE_DURATION_MS + 10_000L, ToolCatalog.uiGesture.maxTimeoutMs)
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
        assertEquals(JsonPrimitive("accessibility.dump"), (rows[8] as JsonObject)["bridge"])
        assertEquals(JsonPrimitive("ui_gesture"), (rows[19] as JsonObject)["group"])
    }

    private companion object {
        const val SNAPSHOT_RESOURCE = "tool-catalog.snapshot.json"
        val DEVICE_NAMES = listOf("device_ping", "device_info")
        val SCRIPT_NAMES = listOf("script_run", "script_run_file", "script_stop", "script_stop_all", "script_list", "console_tail")
        val UI_NAMES = listOf("ui_dump", "ui_find", "ui_current_window", "ui_explain_selector", "ui_wait_for", "ui_click", "ui_long_click", "ui_set_text", "ui_scroll", "ui_press_key")
        val GESTURE_NAMES = listOf("ui_swipe", "ui_gesture")
        val SCREEN_NAMES = listOf("screen_capture", "screen_state")
    }
}
