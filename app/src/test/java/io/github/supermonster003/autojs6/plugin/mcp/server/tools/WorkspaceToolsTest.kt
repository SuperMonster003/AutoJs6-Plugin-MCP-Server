package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class WorkspaceToolsTest {
    private fun args(text: String) = Json.parseToJsonElement(text).jsonObject
    private val calls = mutableListOf<BridgeRequest>()
    private var result: JsonElement = args("""{"schema":"host-v1","path":"test.js","bytes":3}""")
    private val caller = BridgeCaller { module, method, values, timeout, permissions ->
        calls += BridgeRequest("test", module, method, values, timeout, permissions)
        BridgeOutcome.Ok(result, 0)
    }
    private fun plan(flows: ToolFlows, name: String, values: JsonObject) =
        flows.planFor(ToolCatalog.byName.getValue(name), ToolArguments.validate(ToolCatalog.byName.getValue(name), values))!!

    @Test fun `paths normalize one relative prefix and trailing slash`() {
        assertEquals(".", WorkspacePath.normalize("./"))
        assertEquals("scripts/example.js", WorkspacePath.normalize("./scripts/example.js/"))
        assertEquals("目录/a.js", WorkspacePath.normalize("目录/a.js"))
    }
    @Test fun `absolute traversal control characters and malformed segments never reach the host`() {
        listOf("", " ", "/", "/sdcard/a", "C:a", "./C:/a", "C:/a", "\\\\server\\a", "a\\b", "../a", "a/../b", "a/./b", "a//b", "a\u0000b", "a\nb").forEach {
            assertThrows(it, ToolArgumentException::class.java) { WorkspacePath.normalize(it) }
        }
    }
    @Test fun `path budget counts UTF8 bytes and mutations refuse root`() {
        assertEquals(4096, WorkspacePath.normalize("a".repeat(4096)).length)
        assertThrows(ToolArgumentException::class.java) { WorkspacePath.normalize("中".repeat(1366)) }
        listOf(".", "./").forEach { assertThrows(ToolArgumentException::class.java) { WorkspacePath.normalize(it, false) } }
    }
    @Test fun `file list defaults and integral decimal options are encoded`() = runBlocking {
        plan(FileTools(), ToolCatalog.FILES_LIST, args("""{"maxEntries":2.0,"recursive":true}""")).run(caller)
        assertEquals(args("""{"recursive":true,"maxEntries":2}"""), calls.single().args[1])
        assertEquals(JsonPrimitive("."), calls.single().args[0])
    }
    @Test fun `file write has explicit overwrite and parent options without hidden append`() = runBlocking {
        val shaped = plan(FileTools(), ToolCatalog.FILES_WRITE, args("""{"path":"./test.js","content":"a\n中","overwrite":false}""")).run(caller).structured
        assertEquals("write", calls.single().method)
        assertEquals(listOf("files", "files.write"), calls.single().permissions)
        assertEquals(JsonPrimitive("a\n中"), calls.single().args[1])
        assertEquals(args("""{"createDirs":true,"overwrite":false}"""), calls.single().args[2])
        assertFalse(shaped.containsKey("schema"))
    }
    @Test fun `binary read preserves encoding and content metadata`() = runBlocking {
        result = args("""{"schema":"file-v1","encoding":"base64","content":"AP8=","bytes":2,"totalBytes":9,"truncated":true}""")
        val actual = plan(FileTools(), ToolCatalog.FILES_READ, args("""{"path":"a.bin","encoding":"base64","maxBytes":2}""")).run(caller).structured
        assertEquals("AP8=", actual["content"]!!.jsonPrimitive.content)
        assertTrue(actual["truncated"]!!.jsonPrimitive.boolean)
        assertEquals("base64", calls.single().args[1].jsonObject["encoding"]!!.jsonPrimitive.content)
    }
    @Test fun `rename checks both paths and delete uses its own token`() = runBlocking {
        assertThrows(ToolArgumentException::class.java) { plan(FileTools(), ToolCatalog.FILES_RENAME, args("""{"path":"a","to":"../b"}""")) }
        plan(FileTools(), ToolCatalog.FILES_DELETE, args("""{"path":"a","recursive":true}""")).run(caller)
        assertEquals(listOf("files", "files.delete"), calls.single().permissions)
        assertEquals(args("""{"recursive":true}"""), calls.single().args[1])
    }
    @Test fun `file content and read limits are checked before dispatch`() {
        assertThrows(ToolArgumentException::class.java) { plan(FileTools(), ToolCatalog.FILES_WRITE,
            buildJsonObject { put("path", "a"); put("content", "中".repeat(349526)) }) }
        listOf(0, 1048577).forEach { assertThrows(ToolArgumentException::class.java) {
            plan(FileTools(), ToolCatalog.FILES_READ, buildJsonObject { put("path", "a"); put("maxBytes", it) })
        } }
        assertTrue(calls.isEmpty())
    }
    @Test fun `editor sends one-based positions and normalized path`() = runBlocking {
        result = JsonPrimitive(true)
        val shaped = plan(DeviceTools(), ToolCatalog.EDITOR_OPEN, args("""{"path":"./test.js","line":3,"column":4}""")).run(caller).structured
        assertEquals("app", calls.single().module); assertEquals("editFile", calls.single().method)
        assertEquals(args("""{"line":3,"column":4}"""), calls.single().args[1])
        assertEquals(JsonPrimitive(3), shaped["line"])
    }
    @Test fun `app launch requires exactly one target and maps both methods`() = runBlocking {
        listOf("{}", """{"packageName":"a","appName":"b"}""", """{"appName":" "}""").forEach {
            assertThrows(ToolArgumentException::class.java) { plan(DeviceTools(), ToolCatalog.APP_LAUNCH, args(it)) }
        }
        result = JsonPrimitive(true)
        plan(DeviceTools(), ToolCatalog.APP_LAUNCH, args("""{"appName":"Settings"}""")).run(caller)
        plan(DeviceTools(), ToolCatalog.APP_LAUNCH, args("""{"packageName":"com.android.settings"}""")).run(caller)
        assertEquals(listOf("launchApp", "launchPackage"), calls.map { it.method })
    }
    @Test fun `accessibility failure gives manual guidance without asking to call itself again`() = runBlocking {
        result = JsonPrimitive(false)
        try { plan(DeviceTools(), ToolCatalog.DEVICE_ENSURE_ACCESSIBILITY, args("{}")).run(caller); fail() }
        catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING, e.failure.code)
            assertFalse(e.failure.hint!!.contains("device_ensure_accessibility"))
            assertTrue(e.failure.hint!!.contains("Android 13"))
        }
    }
    @Test fun `shell and root switches are separate and reloaded when the flow runs`() = runBlocking {
        var policy = ToolPermissions.DEFAULT
        val flows = ShellTools { policy }
        val flow = plan(flows, ToolCatalog.SHELL_EXEC, args("""{"cmd":"id","root":true}"""))
        for (next in listOf(policy, policy.with(ToolGroup.SHELL, true), policy.copy(allowShellRoot = true))) {
            policy = next
            try { flow.run(caller); fail() } catch (e: ToolFailureException) { assertEquals(ToolErrorCodes.TOOL_DISABLED, e.failure.code) }
        }
        assertTrue(calls.isEmpty())
        policy = ToolPermissions.DEFAULT.with(ToolGroup.SHELL, true).copy(allowShellRoot = true)
        result = args("""{"code":0,"stdout":"","stderr":"","truncated":false,"timedOut":false}""")
        flow.run(caller)
        assertEquals(listOf("shell", "shell.root"), calls.single().permissions)
        assertEquals(args("""{"root":true,"timeoutMs":15000,"maxOutputBytes":65536}"""), calls.single().args[1])
    }
    @Test fun `shell output timeout and command bounds reject invalid arguments`() {
        val flows = ShellTools { ToolPermissions.DEFAULT }
        listOf("""{"cmd":""}""", """{"cmd":"a\u0000b"}""", """{"cmd":"id","maxOutputBytes":262145}""",
            """{"cmd":"id","timeoutMs":0}""", """{"cmd":"id","root":"true"}""").forEach {
            assertThrows(ToolArgumentException::class.java) { plan(flows, ToolCatalog.SHELL_EXEC, args(it)) }
        }
    }
    @Test fun `root preference defaults false and survives unrelated group changes`() {
        assertFalse(ToolPermissions.decode("""{"groups":{"shell":true}}""").allowShellRoot)
        assertFalse(ToolPermissions.decode("""{"groups":{},"allowShellRoot":"true"}""").allowShellRoot)
        val policy = ToolPermissions.DEFAULT.copy(allowShellRoot = true).with(ToolGroup.SHELL, true)
        assertTrue(ToolPermissions.decode(policy.encode()).allowShellRoot)
        assertTrue(policy.with(ToolGroup.DEVICE, false).allowShellRoot)
    }

    @Test fun `shell output is bounded across streams even when an older host ignores the option`() {
        val output = ShellTools { ToolPermissions.DEFAULT }.shape(args("""{"code":0,"stdout":"中","stderr":"xy"}"""), 4)
        assertEquals(JsonPrimitive("中"), output["stdout"])
        assertEquals(JsonPrimitive("x"), output["stderr"])
        assertEquals(JsonPrimitive(4), output["outputBytes"])
        assertEquals(JsonPrimitive(true), output["truncated"])
    }
}
