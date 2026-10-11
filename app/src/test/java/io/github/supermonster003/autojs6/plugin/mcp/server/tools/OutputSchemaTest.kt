package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The output schemas of roadmap P9: which tools declare one, that real result shapes conform, and
 * that errors of those tools carry no `structuredContent` (clients validate it against the schema).
 */
class OutputSchemaTest {

    @Test
    fun `exactly the tools with a stable structured result declare an output schema`() {
        val declared = ToolCatalog.all.filter { it.outputSchema != null }.map { it.name }
        assertEquals(listOf(ToolCatalog.DEVICE_INFO, ToolCatalog.SCRIPT_RUN, ToolCatalog.SCRIPT_RUN_FILE, ToolCatalog.SCREEN_STATE), declared)
        ToolCatalog.all.filter { it.outputSchema != null }.forEach { spec ->
            val sdk = spec.toSdkTool().outputSchema
            assertNotNull(spec.name, sdk)
            assertEquals(spec.outputSchema!!["properties"], sdk!!.properties)
            assertEquals(spec.outputSchema!!["required"]?.let { (it as JsonArray).map { name -> (name as JsonPrimitive).content } }, sdk.required)
        }
    }

    @Test
    fun `a device info result as AutoJs6 sends it conforms`() {
        val result = json(
            """{"schema":"autojs6-bridge-device-info-v1","build":{"brand":"Sony","sdkInt":28,"abis":["arm64-v8a"]},""" +
                    """"screen":{"width":1080,"height":2160,"density":2.75,"densityDpi":440,"rotation":0},"battery":{"available":true,"percent":80},""" +
                    """"memory":{"totalMem":3800000000,"availMem":1200000000},"host":{"packageName":"org.autojs.autojs6","versionCode":5328},""" +
                    """"accessibility":{"serviceRunning":true,"operational":true},"screenOn":true,"locale":"zh-CN","timeZone":"Asia/Shanghai",""" +
                    """"uptimeMs":123456,"now":1760000000000,"futureField":{"added":"later"}}""",
        )
        assertConforms(ToolCatalog.deviceInfo, result)
        assertTrue(violations(ToolCatalog.deviceInfo.outputSchema!!, json("""{"screenOn":"yes"}""")).isNotEmpty())
    }

    @Test
    fun `script results of every status conform`() {
        val prepared = ScriptRunTool.Prepared(JsonArray(emptyList()), 5_000L, true, 15_000L, 20)
        listOf(
            """{"id":3,"engineName":"rhino","sourceName":"demo","cwd":"/sdcard/Scripts","outcome":"success","finished":true,"waitedMs":812,""" +
                    """"console":{"count":1,"truncated":false,"entries":[{"id":1,"level":4,"levelName":"info","time":1,"text":"a"}]}}""",
            """{"id":4,"sourceName":"demo","outcome":"exception","finished":true,"waitedMs":20,"error":"ReferenceError: nope (x.js#3)"}""",
            """{"id":9,"sourceName":"loop","outcome":"running","finished":false,"waitedMs":5000}""",
            """{"id":10,"sourceName":"bg"}""",
            """"plain"""",
        ).forEach { host ->
            val shaped = ScriptRunTool.shape(Json.parseToJsonElement(host), prepared)
            assertConforms(ToolCatalog.scriptRun, shaped)
            assertConforms(ToolCatalog.scriptRunFile, shaped)
        }
        assertTrue(violations(ToolCatalog.scriptRun.outputSchema!!, json("""{"status":"done"}""")).isNotEmpty())
        assertTrue(violations(ToolCatalog.scriptRun.outputSchema!!, json("""{"waited":true}""")).isNotEmpty())
    }

    @Test
    fun `errors of tools with an output schema carry text only, other tools keep structured errors`() {
        val failure = ToolFailure.hostUnavailable()
        val error = ToolResults.failure(failure)
        assertNotNull(error.structuredContent)

        val stripped = ToolResults.forSpec(ToolCatalog.deviceInfo, error)
        assertNull(stripped.structuredContent)
        assertEquals(true, stripped.isError)
        assertEquals(failure.render(), (stripped.content.single() as TextContent).text)

        assertEquals(error, ToolResults.forSpec(ToolCatalog.uiDump, error))
        val success: CallToolResult = ToolResults.success(json("""{"status":"finished"}"""))
        assertEquals(success, ToolResults.forSpec(ToolCatalog.scriptRun, success))

        assertNull(ToolResults.failureJson(failure, structured = false)["structuredContent"])
        assertEquals(JsonPrimitive(true), ToolResults.failureJson(failure, structured = false)["isError"])
        assertNotNull(ToolResults.failureJson(failure)["structuredContent"]?.jsonObject?.get("error"))
    }

    companion object {

        fun assertConforms(spec: ToolSpec, value: JsonObject) {
            val found = violations(spec.outputSchema!!, value)
            assertTrue("${spec.name} result $value violates its output schema: $found", found.isEmpty())
        }

        /**
         * The JSON Schema keywords the catalog's output schemas use (`type`, `properties`,
         * `required`, `items`, `enum`); anything else in a schema is ignored.
         */
        fun violations(schema: JsonObject, value: JsonElement, path: String = "$"): List<String> {
            val found = mutableListOf<String>()
            val type = (schema["type"] as? JsonPrimitive)?.contentOrNull
            if (type != null && !hasType(value, type)) return listOf("$path is not $type: $value")
            (schema["enum"] as? JsonArray)?.let { allowed -> if (value !in allowed) found += "$path is not one of $allowed: $value" }
            if (value is JsonObject) {
                (schema["required"] as? JsonArray)?.forEach { name ->
                    val key = (name as JsonPrimitive).content
                    if (key !in value) found += "$path.$key is missing"
                }
                (schema["properties"] as? JsonObject)?.forEach { (key, child) ->
                    value[key]?.let { found += violations(child as JsonObject, it, "$path.$key") }
                }
            }
            if (value is JsonArray) {
                (schema["items"] as? JsonObject)?.let { items -> value.forEachIndexed { i, item -> found += violations(items, item, "$path[$i]") } }
            }
            return found
        }

        private fun hasType(value: JsonElement, type: String): Boolean = when (type) {
            "object" -> value is JsonObject
            "array" -> value is JsonArray
            "string" -> value is JsonPrimitive && value.isString
            "boolean" -> value is JsonPrimitive && !value.isString && value.booleanOrNull != null
            "integer" -> value is JsonPrimitive && !value.isString && value.longOrNull != null
            "number" -> value is JsonPrimitive && !value.isString && value.doubleOrNull != null && value.booleanOrNull == null
            "null" -> value is JsonNull
            else -> true
        }

        private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject
    }
}
