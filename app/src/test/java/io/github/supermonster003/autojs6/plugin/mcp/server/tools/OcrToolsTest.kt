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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OcrToolsTest {

    private class Call(val module: String, val method: String, val args: JsonArray, val timeoutMs: Long, val permissions: List<String>) {
        val key: String get() = "$module.$method"
    }

    /** Records every host call and answers from [answers] (`module.method` -> outcome), echoing by default. */
    private class FakeCaller(val answers: MutableMap<String, (Call) -> BridgeOutcome> = mutableMapOf()) : BridgeCaller {

        val calls = mutableListOf<Call>()

        override suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome {
            val call = Call(module, method, args, timeoutMs, permissions)
            calls += call
            return answers[call.key]?.invoke(call) ?: BridgeOutcome.Ok(JsonPrimitive(true), 1L)
        }
    }

    private val tools = OcrTools(clock = { 5_000L })

    private val screenItems = Json.parseToJsonElement("""[{"text":"Hello","confidence":0.91,"bounds":{"left":10,"top":20,"right":110,"bottom":60}},{"text":"","confidence":null,"bounds":null}]""")
    private val fileItems = Json.parseToJsonElement("""[{"text":"Decoded","confidence":0.8,"bounds":{"left":10,"top":20,"right":110,"bottom":60}}]""")
    private val handle = Json.parseToJsonElement("""{"id":"img-1","__autojs6ImageHandle":"img-1","width":1000,"height":500,"sourceWidth":2000,"sourceHeight":1000,"scale":0.5}""")

    private fun args(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun run(caller: FakeCaller, arguments: String): ToolFlowResult = runBlocking {
        tools.planFor(ToolCatalog.ocrRecognize, args(arguments))!!.run(caller)
    }

    @Test
    fun `the screen source maps onto readScreenText with the host region form and the engine id`() {
        val caller = FakeCaller(mutableMapOf("accessibility.readScreenText" to { BridgeOutcome.Ok(screenItems, 2L) }))
        val result = run(caller, """{"image":"screen","region":{"left":100,"top":200,"right":500,"bottom":400},"engine":"paddle","timeoutMs":90000}""")

        assertEquals(listOf("accessibility.readScreenText"), caller.calls.map { it.key })
        val call = caller.calls.single()
        assertEquals(listOf("accessibility", "screen_capture", "ocr"), call.permissions)
        assertEquals(90_000L, call.timeoutMs)
        val options = call.args[0].jsonObject
        assertEquals("""{"x":100,"y":200,"width":400,"height":200}""", options["region"].toString())
        assertEquals("paddle-ocr", options["engine"]?.jsonPrimitive?.content)

        assertEquals("Hello", result.text)
        val structured = result.structured
        assertEquals("screen", structured["source"]?.jsonPrimitive?.content)
        assertEquals("screen", structured["image"]?.jsonPrimitive?.content)
        assertEquals("paddle", structured["engine"]?.jsonPrimitive?.content)
        assertEquals(2, structured["count"]?.jsonPrimitive?.content?.toInt())
        val first = structured["items"]!!.jsonArray[0].jsonObject
        assertEquals("Hello", first["text"]?.jsonPrimitive?.content)
        assertEquals(0.91, first["confidence"]?.jsonPrimitive?.content?.toDouble())
        assertEquals("""{"left":10,"top":20,"right":110,"bottom":60}""", first["bounds"].toString())
        val second = structured["items"]!!.jsonArray[1].jsonObject
        assertEquals(setOf("text"), second.keys)
        assertEquals("""{"left":100,"top":200,"right":500,"bottom":400}""", structured["region"].toString())
        assertNull(structured["width"])
        assertEquals("Hello", structured["text"]?.jsonPrimitive?.content)
        assertNotNull(structured["durationMs"])
    }

    @Test
    fun `auto engine sends no engine and an empty answer still has text content`() {
        val caller = FakeCaller(mutableMapOf("accessibility.readScreenText" to { BridgeOutcome.Ok(JsonArray(emptyList()), 2L) }))
        val result = run(caller, """{"image":"screen"}""")
        assertEquals(JsonObject(emptyMap()), caller.calls.single().args[0].jsonObject)
        assertEquals(60_000L, caller.calls.single().timeoutMs)
        assertEquals("(no text recognized)", result.text)
        assertEquals("", result.structured["text"]?.jsonPrimitive?.content)
        assertEquals("auto", result.structured["engine"]?.jsonPrimitive?.content)
        assertEquals(0, result.structured["count"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `a file runs readImage recognize recycle and maps the region and the bounds through the host scale`() {
        val caller = FakeCaller(mutableMapOf(
            "image.readImage" to { BridgeOutcome.Ok(handle, 3L) },
            "ocr.recognize" to { BridgeOutcome.Ok(fileItems, 4L) },
        ))
        val result = run(caller, """{"image":"./pics/a.png","region":{"left":100,"top":201,"right":500,"bottom":401},"engine":"rapid"}""")

        assertEquals(listOf("image.readImage", "ocr.recognize", "image.recycle"), caller.calls.map { it.key })
        val read = caller.calls[0]
        assertEquals("pics/a.png", read.args[0].jsonPrimitive.content)
        assertEquals(listOf("image"), read.permissions)
        assertEquals(ToolCatalog.Ocr.READ_TIMEOUT_MS, read.timeoutMs)
        val recognize = caller.calls[1]
        assertEquals("img-1", recognize.args[0].jsonObject["__autojs6ImageHandle"]?.jsonPrimitive?.content)
        assertEquals("img-1", recognize.args[0].jsonObject["id"]?.jsonPrimitive?.content)
        // File pixels 100..500 x 201..401 at scale 0.5: x 50, y floor(100.5) = 100, right 250, bottom ceil(200.5) = 201.
        assertEquals("""{"x":50,"y":100,"width":200,"height":101}""", recognize.args[1].jsonObject["region"].toString())
        assertEquals("rapid-ocr", recognize.args[1].jsonObject["engine"]?.jsonPrimitive?.content)
        assertEquals(listOf("ocr", "image"), recognize.permissions)
        assertEquals(60_000L, recognize.timeoutMs)
        val recycle = caller.calls[2]
        assertEquals("img-1", recycle.args[0].jsonObject["__autojs6ImageHandle"]?.jsonPrimitive?.content)
        assertEquals(ToolCatalog.Ocr.RECYCLE_TIMEOUT_MS, recycle.timeoutMs)

        val structured = result.structured
        assertEquals("file", structured["source"]?.jsonPrimitive?.content)
        assertEquals("./pics/a.png", structured["image"]?.jsonPrimitive?.content)
        assertEquals(2000, structured["width"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1000, structured["height"]?.jsonPrimitive?.content?.toInt())
        assertEquals(0.5, structured["scale"]?.jsonPrimitive?.content?.toDouble())
        assertEquals(1000, structured["decodedWidth"]?.jsonPrimitive?.content?.toInt())
        assertEquals(500, structured["decodedHeight"]?.jsonPrimitive?.content?.toInt())
        // Decoded bounds 10..110 x 20..60 become file pixels 20..220 x 40..120.
        assertEquals("""{"left":20,"top":40,"right":220,"bottom":120}""", structured["items"]!!.jsonArray[0].jsonObject["bounds"].toString())
        assertEquals("Decoded", result.text)
    }

    @Test
    fun `a full-size decode reports scale 1 without decoded dimensions and recycles after a recognition failure`() {
        val plainHandle = Json.parseToJsonElement("""{"id":"img-2","width":640,"height":480}""")
        val caller = FakeCaller(mutableMapOf(
            "image.readImage" to { BridgeOutcome.Ok(plainHandle, 3L) },
            "ocr.recognize" to { BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "AutoJs6 ocr bridge requires an enabled compatible recognition plugin.", code = "x", category = BridgeError.CATEGORY_UNAVAILABLE, module = "ocr", method = "recognize"))) },
        ))
        try {
            run(caller, """{"image":"shot.png"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, e.failure.code)
            assertTrue(e.failure.message, e.failure.message.startsWith("tool ocr_recognize needs the ocr plugin family, which is not installed, disabled, or not usable: "))
        }
        assertEquals(listOf("image.readImage", "ocr.recognize", "image.recycle"), caller.calls.map { it.key })
        assertEquals("img-2", caller.calls[2].args[0].jsonObject["id"]?.jsonPrimitive?.content)

        caller.answers["ocr.recognize"] = { BridgeOutcome.Ok(fileItems, 4L) }
        caller.calls.clear()
        val result = run(caller, """{"image":"shot.png"}""")
        assertEquals(1.0, result.structured["scale"]?.jsonPrimitive?.content?.toDouble())
        assertNull(result.structured["decodedWidth"])
        assertEquals(640, result.structured["width"]?.jsonPrimitive?.content?.toInt())
        assertEquals("""{"left":10,"top":20,"right":110,"bottom":60}""", result.structured["items"]!!.jsonArray[0].jsonObject["bounds"].toString())
    }

    @Test
    fun `a missing OCR plugin behind readScreenText is PLUGIN_UNAVAILABLE and consent is requested once`() {
        val missing = BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "OCR_PLUGIN_REQUIRED", code = "x", category = BridgeError.CATEGORY_UNAVAILABLE, module = "accessibility", method = "readScreenText")))
        val caller = FakeCaller(mutableMapOf("accessibility.readScreenText" to { missing }))
        try {
            run(caller, """{"image":"screen"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, e.failure.code)
            assertEquals(ToolFailure.HINT_PLUGIN, e.failure.hint)
        }

        var attempts = 0
        val denied = BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "Screen capture permission is required.", code = "x", category = BridgeError.CATEGORY_PERMISSION_DENIED, module = "accessibility", method = "readScreenText")))
        val consenting = FakeCaller(mutableMapOf(
            "accessibility.readScreenText" to { if (attempts++ == 0) denied else BridgeOutcome.Ok(screenItems, 2L) },
        ))
        val result = run(consenting, """{"image":"screen"}""")
        assertEquals(listOf("accessibility.readScreenText", "media_projection.requestScreenCapture", "accessibility.readScreenText"), consenting.calls.map { it.key })
        assertEquals(listOf("screen_capture"), consenting.calls[1].permissions)
        assertEquals("Hello", result.text)

        val refusing = FakeCaller(mutableMapOf(
            "accessibility.readScreenText" to { denied },
            "media_projection.requestScreenCapture" to { denied },
        ))
        try {
            run(refusing, """{"image":"screen"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.CAPABILITY_DENIED, e.failure.code)
            assertEquals(ScreenTools.CONSENT_HINT, e.failure.hint)
        }
        assertEquals(2, refusing.calls.size)
    }

    @Test
    fun `arguments are checked before any host call`() {
        val caller = FakeCaller()
        listOf(
            """{}""" to "requires the argument image",
            """{"image":""}""" to "image must be",
            """{"image":"/sdcard/a.png"}""" to "inside the AutoJs6 working directory",
            """{"image":"../a.png"}""" to "inside the AutoJs6 working directory",
            """{"image":"."}""" to "inside the AutoJs6 working directory",
            """{"image":"screen","engine":"tesseract"}""" to "engine must be one of",
            """{"image":"screen","region":{"left":10,"top":10,"right":10,"bottom":20}}""" to "positive width and height",
            """{"image":"screen","region":{"left":10,"top":10,"right":20}}""" to "exactly left, top, right and bottom",
            """{"image":"screen","timeoutMs":10}""" to "timeoutMs must be at least 1000",
            """{"image":"screen","unknown":1}""" to "does not accept an argument named unknown",
        ).forEach { (arguments, expected) ->
            try {
                tools.planFor(ToolCatalog.ocrRecognize, args(arguments))
                fail("$arguments should be rejected")
            } catch (e: ToolArgumentException) {
                assertTrue("$arguments -> ${e.failure.message}", e.failure.message.contains(expected))
            }
        }
        assertTrue(caller.calls.isEmpty())

        // A region beyond the file is rejected after the handle is read, and the handle is still recycled.
        val reading = FakeCaller(mutableMapOf("image.readImage" to { BridgeOutcome.Ok(handle, 3L) }))
        try {
            run(reading, """{"image":"a.png","region":{"left":0,"top":0,"right":2001,"bottom":10}}""")
            fail("expected a failure")
        } catch (e: ToolArgumentException) {
            assertTrue(e.failure.message, e.failure.message.contains("outside the image (2000 x 1000)"))
        }
        assertEquals(listOf("image.readImage", "image.recycle"), reading.calls.map { it.key })
    }

    @Test
    fun `the catalog row declares every host method the flow uses`() {
        val spec = ToolCatalog.ocrRecognize
        assertEquals(ToolGroup.OCR, spec.group)
        assertEquals(BridgeMethod("ocr", "recognize"), spec.bridge)
        assertEquals(
            listOf("accessibility.readScreenText", "media_projection.requestScreenCapture", "image.readImage", "ocr.recognize", "image.recycle"),
            spec.hostMethods.map { it.key },
        )
        assertEquals(listOf("image", "region", "engine", "timeoutMs"), spec.properties.keys.toList())
        assertEquals(listOf("image"), spec.required)
        assertEquals(60_000L, spec.timeoutMs)
        assertEquals(300_000L, spec.maxTimeoutMs)
        assertTrue(spec.hints.readOnly)
        assertNull(tools.planFor(ToolCatalog.deviceInfo, JsonObject(emptyMap())))
    }
}
