package io.github.supermonster003.autojs6.plugin.mcp.server.tools

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
import org.junit.Test

class ScriptRunToolTest {

    private fun arguments(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `defaults wait for completion with the console captured`() {
        val prepared = ScriptRunTool.prepare(arguments("""{"source":"toast('hi')"}"""))

        assertEquals("mcp_script", prepared.name)
        assertEquals(3, prepared.args.size)
        assertEquals("mcp_script", prepared.args[0].jsonPrimitive.content)
        assertEquals("toast('hi')", prepared.args[1].jsonPrimitive.content)
        val options = prepared.args[2].jsonObject
        assertEquals(setOf("timeoutMs", "waitMs", "captureConsole"), options.keys)
        assertEquals(60_000L, options["timeoutMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(60_000L, options["waitMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(true, options["captureConsole"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(60_000L, prepared.waitMs)
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
    fun `names lose the js suffix and blanks fall back to the default`() {
        assertEquals("task", ScriptRunTool.normalizeName("task"))
        assertEquals("Task", ScriptRunTool.normalizeName(" Task.JS "))
        assertEquals("demo.min", ScriptRunTool.normalizeName("demo.min.js"))
        assertEquals("mcp_script", ScriptRunTool.normalizeName(".js"))
        assertEquals("mcp_script", ScriptRunTool.normalizeName("   "))
        assertEquals("mcp_script", ScriptRunTool.normalizeName(null))
    }

    @Test
    fun `console entries are trimmed to the newest lines`() {
        val entries = (1..5).joinToString(",") { """{"id":$it,"level":4,"levelName":"info","time":$it,"text":"line $it"}""" }
        val result = Json.parseToJsonElement("""{"outcome":"success","console":{"count":5,"truncated":false,"entries":[$entries]}}""")

        val shaped = ScriptRunTool.shape(result, 2)

        val console = shaped["console"]!!.jsonObject
        assertEquals(listOf("line 4", "line 5"), console["entries"]!!.jsonArray.map { it.jsonObject["text"]!!.jsonPrimitive.content })
        assertEquals(5, console["count"]?.jsonPrimitive?.content?.toInt())
        assertEquals(2, console["returned"]?.jsonPrimitive?.content?.toInt())
        assertEquals(true, console["truncated"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("success", shaped["outcome"]?.jsonPrimitive?.content)

        val untouched = ScriptRunTool.shape(result, 5)
        assertEquals(result, untouched)
        assertFalse(untouched["console"]!!.jsonObject["truncated"]!!.jsonPrimitive.booleanOrNull!!)
        assertTrue(ScriptRunTool.shape(JsonPrimitive("plain"), 5)["result"] is JsonPrimitive)
        assertTrue(ScriptRunTool.shape(JsonArray(emptyList()), 5)["result"] is JsonArray)
    }
}
