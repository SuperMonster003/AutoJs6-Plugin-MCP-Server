package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScriptRunToolTest {

    private fun arguments(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun prepared(waitMs: Long = 60_000L, maxConsoleLines: Int = 200): ScriptRunTool.Prepared =
        ScriptRunTool.Prepared(JsonArray(emptyList()), waitMs, true, waitMs + 10_000L, maxConsoleLines)

    @Test
    fun `defaults wait for completion with the console captured`() {
        val prepared = ScriptRunTool.prepare(arguments("""{"source":"toast('hi')"}"""))

        assertEquals(3, prepared.args.size)
        assertEquals("mcp_script", prepared.args[0].jsonPrimitive.content)
        assertEquals("toast('hi')", prepared.args[1].jsonPrimitive.content)
        val options = prepared.args[2].jsonObject
        assertEquals(setOf("timeoutMs", "waitMs", "captureConsole"), options.keys)
        assertEquals(60_000L, options["timeoutMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(60_000L, options["waitMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(true, options["captureConsole"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(60_000L, prepared.waitMs)
        assertTrue(prepared.captureConsole)
        assertEquals(70_000L, prepared.bridgeTimeoutMs)
        assertEquals(200, prepared.maxConsoleLines)
    }

    @Test
    fun `long timeouts keep the host start timeout at its ceiling and cap the wait`() {
        val prepared = ScriptRunTool.prepare(arguments("""{"source":"sleep(1)","timeoutMs":300000}"""))

        val options = prepared.args[2].jsonObject
        assertEquals(60_000L, options["timeoutMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(290_000L, options["waitMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(300_000L, prepared.bridgeTimeoutMs)
    }

    @Test
    fun `fire and forget sends no wait but keeps the bridge timeout above the start timeout`() {
        val prepared = ScriptRunTool.prepare(arguments("""{"source":"x","waitForCompletion":false,"captureConsole":false,"timeoutMs":5000}"""))

        val options = prepared.args[2].jsonObject
        assertEquals(5_000L, options["timeoutMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(0L, options["waitMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(false, options["captureConsole"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(0L, prepared.waitMs)
        assertFalse(prepared.captureConsole)
        assertEquals(15_000L, prepared.bridgeTimeoutMs)
    }

    @Test
    fun `optional fields reach the host only when they carry something`() {
        val prepared = ScriptRunTool.prepare(
            arguments("""{"source":"x","name":"task","workingDirectory":"scripts/mcp","arguments":{"n":1,"s":"a","b":true},"maxConsoleLines":5}"""),
        )

        val options = prepared.args[2].jsonObject
        assertEquals("task", prepared.args[0].jsonPrimitive.content)
        assertEquals("scripts/mcp", options["cwd"]?.jsonPrimitive?.content)
        assertEquals(3, options["arguments"]?.jsonObject?.size)
        assertEquals(5, prepared.maxConsoleLines)

        val bare = ScriptRunTool.prepare(arguments("""{"source":"x","workingDirectory":".","arguments":{}}""")).args[2].jsonObject
        assertNull(bare["cwd"])
        assertNull(bare["arguments"])
    }

    @Test
    fun `script files travel as path plus options`() {
        val prepared = ScriptRunTool.prepareFile(
            arguments("""{"path":"tools/demo.js","timeoutMs":5000,"waitForCompletion":false,"arguments":{"n":1},"workingDirectory":"mcp"}"""),
        )

        assertEquals(2, prepared.args.size)
        assertEquals("tools/demo.js", prepared.args[0].jsonPrimitive.content)
        val options = prepared.args[1].jsonObject
        assertEquals(5_000L, options["timeoutMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(0L, options["waitMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(1, options["arguments"]?.jsonObject?.size)
        assertEquals("mcp", options["cwd"]?.jsonPrimitive?.content)
        assertEquals(0L, prepared.waitMs)
        assertEquals(15_000L, prepared.bridgeTimeoutMs)

        try {
            ScriptRunTool.prepareFile(arguments("""{"path":"   "}"""))
            fail("blank paths must be refused")
        } catch (expected: ToolArgumentException) {
            assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, expected.failure.code)
        }
    }

    @Test
    fun `names lose the js suffix and blanks fall back to the default`() {
        assertEquals("task", ScriptRunTool.normalizeName("task"))
        assertEquals("Task", ScriptRunTool.normalizeName(" Task.JS "))
        assertEquals("demo.min", ScriptRunTool.normalizeName("demo.min.js"))
        assertEquals("mcp_script", ScriptRunTool.normalizeName(".js"))
        assertEquals("mcp_script", ScriptRunTool.normalizeName("   "))
        assertEquals("mcp_script", ScriptRunTool.normalizeName(null))
    }

    @Test
    fun `a finished run reports its id name duration and trimmed console`() {
        val entries = (1..5).joinToString(",") { """{"id":$it,"level":4,"levelName":"info","time":$it,"text":"line $it"}""" }
        val result = Json.parseToJsonElement(
            """{"id":3,"engineName":"rhino","sourceName":"demo","cwd":"/sdcard/Scripts","outcome":"success","finished":true,""" +
                    """"waitMs":60000,"waitedMs":812,"console":{"count":5,"truncated":false,"entries":[$entries]}}""",
        )

        val shaped = ScriptRunTool.shape(result, prepared(maxConsoleLines = 2))

        assertEquals(3, shaped["executionId"]?.jsonPrimitive?.content?.toInt())
        assertEquals("demo", shaped["name"]?.jsonPrimitive?.content)
        assertEquals("rhino", shaped["engine"]?.jsonPrimitive?.content)
        assertEquals("/sdcard/Scripts", shaped["workingDirectory"]?.jsonPrimitive?.content)
        assertEquals(ScriptRunTool.STATUS_FINISHED, shaped["status"]?.jsonPrimitive?.content)
        assertEquals(true, shaped["waited"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(60_000L, shaped["waitMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(812L, shaped["durationMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(listOf("line 4", "line 5"), shaped["console"]!!.jsonArray.map { it.jsonObject["text"]!!.jsonPrimitive.content })
        assertEquals(5L, shaped["consoleCount"]?.jsonPrimitive?.content?.toLong())
        assertEquals(true, shaped["consoleTruncated"]?.jsonPrimitive?.booleanOrNull)
        assertNull(shaped["exception"])
        assertNull(shaped["hint"])

        val untrimmed = ScriptRunTool.shape(result, prepared(maxConsoleLines = 5))
        assertEquals(5, untrimmed["console"]!!.jsonArray.size)
        assertEquals(false, untrimmed["consoleTruncated"]?.jsonPrimitive?.booleanOrNull)
    }

    @Test
    fun `a thrown error becomes status error with the message and the Rhino line`() {
        val result = Json.parseToJsonElement(
            """{"id":4,"sourceName":"demo","outcome":"exception","finished":true,"waitedMs":20,"error":"ReferenceError: nope is not defined (${'$'}engine/demo.js#3)"}""",
        )

        val shaped = ScriptRunTool.shape(result, prepared())

        assertEquals(ScriptRunTool.STATUS_ERROR, shaped["status"]?.jsonPrimitive?.content)
        val exception = shaped["exception"]!!.jsonObject
        assertEquals("ReferenceError: nope is not defined (\$engine/demo.js#3)", exception["message"]?.jsonPrimitive?.content)
        assertEquals(3, exception["line"]?.jsonPrimitive?.content?.toInt())
        assertEquals(20L, shaped["durationMs"]?.jsonPrimitive?.content?.toLong())
        assertNull(shaped["console"])
        assertNull(shaped["hint"])

        assertEquals(JsonObject(mapOf("message" to JsonPrimitive("the script threw"))), ScriptRunTool.exceptionOf(null))
        assertNull(ScriptRunTool.exceptionOf("no location")["line"])
        assertEquals(12, ScriptRunTool.exceptionOf("SyntaxError: missing ; before statement (x.js#12)")["line"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `a script still running after the wait or started without waiting names script_stop`() {
        val waited = ScriptRunTool.shape(
            Json.parseToJsonElement("""{"id":9,"sourceName":"loop","outcome":"running","finished":false,"waitedMs":5000,"console":{"count":0,"truncated":false,"entries":[]}}"""),
            prepared(waitMs = 5_000L),
        )
        assertEquals(ScriptRunTool.STATUS_RUNNING, waited["status"]?.jsonPrimitive?.content)
        assertEquals(true, waited["waited"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(5_000L, waited["waitMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(5_000L, waited["durationMs"]?.jsonPrimitive?.content?.toLong())
        assertTrue(waited["hint"]!!.jsonPrimitive.content.startsWith("still running after 5000 ms; script_stop with executionId 9"))
        assertEquals(0, waited["console"]!!.jsonArray.size)
        assertEquals(false, waited["consoleTruncated"]?.jsonPrimitive?.booleanOrNull)

        val detached = ScriptRunTool.shape(Json.parseToJsonElement("""{"id":10,"sourceName":"bg"}"""), prepared(waitMs = 0L))
        assertEquals(ScriptRunTool.STATUS_RUNNING, detached["status"]?.jsonPrimitive?.content)
        assertEquals(false, detached["waited"]?.jsonPrimitive?.booleanOrNull)
        assertTrue(detached["hint"]!!.jsonPrimitive.content.startsWith("started without waiting; script_list shows it, script_stop with executionId 10"))
        assertNull(detached["durationMs"])
        assertNull(detached["console"])

        assertEquals(ScriptRunTool.STATUS_RUNNING, ScriptRunTool.shape(JsonPrimitive("plain"), prepared())["status"]?.jsonPrimitive?.content)
        assertTrue(ScriptRunTool.shape(JsonArray(emptyList()), prepared())["result"] is JsonArray)
    }
}
