package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
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

class ScriptToolsTest {

    private fun arguments(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `stop and stop_all encode the execution id and the host scope`() {
        assertEquals(JsonArray(listOf(JsonPrimitive(7L))), ScriptTools.stopArgs(arguments("""{"executionId":7}""")))
        assertEquals("host", ScriptTools.STOP_ALL_ARGS[0].jsonObject["scope"]?.jsonPrimitive?.content)
        assertEquals(1, ScriptTools.STOP_ALL_ARGS.size)
        assertTrue(ScriptTools.NO_ARGS.isEmpty())
    }

    @Test
    fun `console_tail forwards only the options the client gave`() {
        val bare = ScriptTools.tailArgs(arguments("{}"))[0].jsonObject
        assertEquals(setOf("lines"), bare.keys)
        assertEquals(100L, bare["lines"]?.jsonPrimitive?.content?.toLong())

        val full = ScriptTools.tailArgs(arguments("""{"lines":5,"sinceId":42,"level":"warn"}"""))[0].jsonObject
        assertEquals(5L, full["lines"]?.jsonPrimitive?.content?.toLong())
        assertEquals(42L, full["sinceId"]?.jsonPrimitive?.content?.toLong())
        assertEquals("warn", full["level"]?.jsonPrimitive?.content)

        val nulls = ScriptTools.tailArgs(arguments("""{"sinceId":null,"level":null}"""))[0].jsonObject
        assertEquals(setOf("lines"), nulls.keys)
    }

    @Test
    fun `stop results carry the execution id name state and flag`() {
        val shaped = ScriptTools.shapeStop(
            Json.parseToJsonElement("""{"schema":"autojs6-bridge-engines-stop-v1","scope":"host","id":7,"sourceName":"loop.js","state":"finished","stopped":true}"""),
        )
        assertEquals(setOf("executionId", "name", "state", "stopped"), shaped.keys)
        assertEquals(7, shaped["executionId"]?.jsonPrimitive?.content?.toInt())
        assertEquals("loop.js", shaped["name"]?.jsonPrimitive?.content)
        assertEquals("finished", shaped["state"]?.jsonPrimitive?.content)
        assertEquals(true, shaped["stopped"]?.jsonPrimitive?.booleanOrNull)

        assertEquals(false, ScriptTools.shapeStop(JsonPrimitive("x"))["stopped"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(2, ScriptTools.shapeStopAll(Json.parseToJsonElement("""{"schema":"s","scope":"host","stopped":2}"""))["stopped"]?.jsonPrimitive?.content?.toInt())
        assertEquals(0, ScriptTools.shapeStopAll(JsonNull)["stopped"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `list results rename the host records`() {
        val shaped = ScriptTools.shapeList(
            Json.parseToJsonElement(
                """{"schema":"autojs6-bridge-engines-list-v1","scope":"host","count":2,"executions":[""" +
                        """{"id":7,"engineName":"rhino","sourceName":"run.js","sourcePath":"demo/run.js","workingDirectory":"/sdcard/Scripts","state":"running","startedAt":1,"uptimeMs":5},""" +
                        """{"id":8,"engineName":"","sourceName":"bg","sourcePath":"","workingDirectory":"/sdcard/Scripts","state":"starting","startedAt":0,"uptimeMs":null}]}""",
            ),
        )
        assertEquals(2, shaped["count"]?.jsonPrimitive?.content?.toInt())
        val executions = shaped["executions"]!!.jsonArray
        assertEquals(2, executions.size)
        val first = executions[0].jsonObject
        assertEquals(setOf("executionId", "name", "engine", "path", "workingDirectory", "state", "startedAt", "uptimeMs"), first.keys)
        assertEquals(7, first["executionId"]?.jsonPrimitive?.content?.toInt())
        assertEquals("run.js", first["name"]?.jsonPrimitive?.content)
        assertEquals("demo/run.js", first["path"]?.jsonPrimitive?.content)
        assertEquals("running", first["state"]?.jsonPrimitive?.content)
        assertEquals(5L, first["uptimeMs"]?.jsonPrimitive?.content?.toLong())
        assertTrue(executions[1].jsonObject["uptimeMs"] is JsonNull)

        val empty = ScriptTools.shapeList(Json.parseToJsonElement("""{"count":0,"executions":[]}"""))
        assertEquals(0, empty["count"]?.jsonPrimitive?.content?.toInt())
        assertEquals(0, empty["executions"]!!.jsonArray.size)
        assertEquals(0, ScriptTools.shapeList(JsonPrimitive("x"))["executions"]!!.jsonArray.size)
    }

    @Test
    fun `tail results drop the schema tag and keep the cursor fields`() {
        val shaped = ScriptTools.shapeTail(
            Json.parseToJsonElement(
                """{"schema":"autojs6-bridge-console-tail-v1","lines":2,"sinceId":-1,"minLevel":2,"minLevelName":"verbose","count":1,"firstId":9,"lastId":9,""" +
                        """"nextSinceId":9,"latestId":9,"total":40,"capacity":500,"bytes":12,"truncated":false,"evicted":false,"entries":[{"id":9,"level":4,"levelName":"info","time":9,"text":"hi"}]}""",
            ),
        )
        assertFalse(shaped.containsKey("schema"))
        assertEquals(9, shaped["nextSinceId"]?.jsonPrimitive?.content?.toInt())
        assertEquals(9, shaped["latestId"]?.jsonPrimitive?.content?.toInt())
        assertEquals(40, shaped["total"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1, shaped["entries"]!!.jsonArray.size)
        assertEquals("hi", shaped["entries"]!!.jsonArray[0].jsonObject["text"]?.jsonPrimitive?.content)
        assertNull(ScriptTools.shapeTail(JsonPrimitive("x"))["entries"])
        assertTrue(ScriptTools.shapeTail(JsonPrimitive("x"))["result"] is JsonPrimitive)
    }
}
