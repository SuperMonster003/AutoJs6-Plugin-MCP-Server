package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PinyinToolsTest {

    private class Call(val module: String, val method: String, val args: JsonArray, val timeoutMs: Long, val permissions: List<String>) {
        val key: String get() = "$module.$method"
    }

    private class FakeCaller(val answer: (Call) -> BridgeOutcome) : BridgeCaller {

        val calls = mutableListOf<Call>()

        override suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome {
            val call = Call(module, method, args, timeoutMs, permissions)
            calls += call
            return answer(call)
        }
    }

    private val tools = PinyinTools()

    private fun args(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun matrix(json: String) = BridgeOutcome.Ok(Json.parseToJsonElement(json), 1L)

    private fun run(spec: ToolSpec, caller: FakeCaller, arguments: String): ToolFlowResult = runBlocking {
        tools.planFor(spec, args(arguments))!!.run(caller)
    }

    @Test
    fun `pinyin_convert sends the host option names with segmentation on by default`() {
        val caller = FakeCaller { matrix("""[["yín"],["háng"],["AutoJs6"]]""") }
        val result = run(ToolCatalog.pinyinConvert, caller, """{"text":"银行AutoJs6"}""")

        val call = caller.calls.single()
        assertEquals("pinyin.convert", call.key)
        assertEquals(listOf("pinyin"), call.permissions)
        assertEquals(30_000L, call.timeoutMs)
        assertEquals("银行AutoJs6", call.args[0].jsonPrimitive.content)
        assertEquals("""{"style":"TONE","mode":"NORMAL","segment":true,"heteronym":false}""", call.args[1].toString())
        assertEquals("yín háng AutoJs6", result.text)
        assertEquals("yín háng AutoJs6", result.structured["pinyin"]?.jsonPrimitive?.content)
        assertEquals("""[["yín"],["háng"],["AutoJs6"]]""", result.structured["rows"].toString())
        assertEquals("tone", result.structured["style"]?.jsonPrimitive?.content)
    }

    @Test
    fun `pinyin_convert maps every option and joins heteronyms with a bar`() {
        val caller = FakeCaller { matrix("""[["zhong","zhong"],["guo"]]""") }
        val result = run(ToolCatalog.pinyinConvert, caller, """{"text":"中国","style":"first_letter","mode":"place_name","segment":false,"heteronym":true}""")
        assertEquals("""{"style":"FIRST_LETTER","mode":"PLACE_NAME","segment":false,"heteronym":true}""", caller.calls.single().args[1].toString())
        assertEquals("zhong|zhong guo", result.text)
    }

    @Test
    fun `pinyin4j_convert reads one row per character and joins the first readings`() {
        val caller = FakeCaller { matrix("""[["zhōng","zhòng"],["guó"],["!"]]""") }
        val result = run(ToolCatalog.pinyin4jConvert, caller, """{"text":"中国!"}""")

        val call = caller.calls.single()
        assertEquals("pinyin4j.readings", call.key)
        assertEquals(listOf("pinyin4j"), call.permissions)
        assertEquals("""{"romanization":"HANYU","tone":"WITH_TONE_MARK","case":"LOWERCASE"}""", call.args[1].toString())
        assertEquals("zhōng guó !", result.text)
        assertEquals("""[["zhōng"],["guó"],["!"]]""", result.structured["rows"].toString())
        assertEquals("mark", result.structured["tone"]?.jsonPrimitive?.content)

        val all = run(ToolCatalog.pinyin4jConvert, caller, """{"text":"中国!","tone":"number","case":"upper","separator":"-","allReadings":true}""")
        assertEquals("""{"romanization":"HANYU","tone":"WITH_TONE_NUMBER","case":"UPPERCASE"}""", caller.calls[1].args[1].toString())
        assertEquals("zhōng|zhòng-guó-!", all.text)
        assertEquals("""[["zhōng","zhòng"],["guó"],["!"]]""", all.structured["rows"].toString())
    }

    @Test
    fun `other romanizations send no tone or case and refuse them`() {
        val caller = FakeCaller { matrix("""[["chung1"],["kuo2"]]""") }
        val result = run(ToolCatalog.pinyin4jConvert, caller, """{"text":"中国","romanization":"wade_giles"}""")
        assertEquals("""{"romanization":"WADE_GILES"}""", caller.calls.single().args[1].toString())
        assertEquals("chung1 kuo2", result.text)
        assertNull(result.structured["tone"])
        assertEquals("wade_giles", result.structured["romanization"]?.jsonPrimitive?.content)

        listOf("tone", "case").forEach { name ->
            try {
                tools.planFor(ToolCatalog.pinyin4jConvert, args("""{"text":"中国","romanization":"yale","$name":"${if (name == "tone") "number" else "upper"}"}"""))
                fail("$name should be refused")
            } catch (e: ToolArgumentException) {
                assertTrue(e.failure.message, e.failure.message.contains("$name applies to hanyu only"))
            }
        }
    }

    @Test
    fun `arguments are checked before any host call and host failures pass through`() {
        listOf(
            ToolCatalog.pinyinConvert to """{}""" to "requires the argument text",
            ToolCatalog.pinyinConvert to """{"text":"  "}""" to "text must not be empty",
            ToolCatalog.pinyinConvert to """{"text":"中","style":"bopomofo"}""" to "style must be one of",
            ToolCatalog.pinyinConvert to """{"text":"${"中".repeat(2800)}"}""" to "text is longer than 8192 bytes",
            ToolCatalog.pinyin4jConvert to """{"text":"中","romanization":"ipa"}""" to "romanization must be one of",
            ToolCatalog.pinyin4jConvert to """{"text":"中","separator":"${"-".repeat(17)}"}""" to "separator is longer than 16 bytes",
        ).forEach { (input, expected) ->
            val (spec, arguments) = input
            try {
                tools.planFor(spec, args(arguments))
                fail("${spec.name} $arguments should be rejected")
            } catch (e: ToolArgumentException) {
                assertTrue("${spec.name} -> ${e.failure.message}", e.failure.message.contains(expected))
            }
        }

        val failing = FakeCaller { BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "pinyin plugin failed", code = "x", category = BridgeError.CATEGORY_PROVIDER_FAILED, module = "pinyin", method = "convert"))) }
        try {
            run(ToolCatalog.pinyinConvert, failing, """{"text":"中"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.HOST_ERROR, e.failure.code)
        }
        val malformed = FakeCaller { BridgeOutcome.Ok(JsonPrimitive("zhong"), 1L) }
        try {
            run(ToolCatalog.pinyin4jConvert, malformed, """{"text":"中"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertTrue(e.failure.message, e.failure.message.contains("pinyin4j.readings returned no list of rows"))
        }
    }

    @Test
    fun `the catalog rows map onto one host method each`() {
        assertEquals(ToolGroup.PINYIN, ToolCatalog.pinyinConvert.group)
        assertEquals(listOf("pinyin.convert"), ToolCatalog.pinyinConvert.hostMethods.map { it.key })
        assertEquals(listOf("text", "style", "mode", "segment", "heteronym"), ToolCatalog.pinyinConvert.properties.keys.toList())
        assertEquals(ToolGroup.PINYIN4J, ToolCatalog.pinyin4jConvert.group)
        assertEquals(listOf("pinyin4j.readings"), ToolCatalog.pinyin4jConvert.hostMethods.map { it.key })
        assertEquals(listOf("text", "romanization", "tone", "case", "separator", "allReadings"), ToolCatalog.pinyin4jConvert.properties.keys.toList())
        listOf(ToolCatalog.pinyinConvert, ToolCatalog.pinyin4jConvert).forEach { spec ->
            assertEquals(listOf("text"), spec.required)
            assertTrue(spec.hints.readOnly && spec.hints.idempotent)
        }
        assertNull(tools.planFor(ToolCatalog.ocrRecognize, args("""{"image":"screen"}""")))
    }
}
