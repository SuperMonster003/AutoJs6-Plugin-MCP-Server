package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BarcodeToolsTest {

    private class Call(val module: String, val method: String, val args: JsonArray, val timeoutMs: Long, val permissions: List<String>) {
        val key: String get() = "$module.$method"
    }

    /** Records every host call and answers from [answers] (`module.method` -> outcome), echoing `true` by default. */
    private class FakeCaller(val answers: MutableMap<String, (Call) -> BridgeOutcome> = mutableMapOf()) : BridgeCaller {

        val calls = mutableListOf<Call>()

        override suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome {
            val call = Call(module, method, args, timeoutMs, permissions)
            calls += call
            return answers[call.key]?.invoke(call) ?: BridgeOutcome.Ok(JsonPrimitive(true), 1L)
        }
    }

    private val tools = BarcodeTools(clock = { 5_000L })

    private val screenHandle = Json.parseToJsonElement("""{"id":"img-s","__autojs6ImageHandle":"img-s","width":1080,"height":2400,"captureSource":"accessibility"}""")
    private val fileHandle = Json.parseToJsonElement("""{"id":"img-f","__autojs6ImageHandle":"img-f","width":3000,"height":2000,"sourceWidth":6000,"sourceHeight":4000,"scale":0.5}""")
    private val snapshots = Json.parseToJsonElement(
        """[{"schema":"barcode-v1","rawValue":"https://example.org","displayValue":"example.org","format":256,"formatName":"QR_CODE","valueType":8,"valueTypeName":"URL",
           "boundingBox":{"left":100,"top":200,"right":300,"bottom":400,"width":200,"height":200},
           "cornerPoints":[{"x":100,"y":200},{"x":300,"y":200},{"x":300,"y":400},{"x":100,"y":400}],"structured":{"url":"https://example.org"}},
           {"schema":"barcode-v1","rawValue":null,"displayValue":null,"format":32,"formatName":"EAN_13","valueType":5,"valueTypeName":"PRODUCT"}]""",
    )

    private fun args(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun run(caller: FakeCaller, arguments: String): ToolFlowResult = runBlocking {
        tools.planFor(ToolCatalog.barcodeDetect, args(arguments))!!.run(caller)
    }

    @Test
    fun `the screen is captured as a handle, detected with the host options, and recycled`() {
        val caller = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "barcode.detectAll" to { BridgeOutcome.Ok(snapshots, 3L) },
        ))
        val result = run(caller, """{"image":"screen","formats":["QR_CODE","EAN_13","QR_CODE"],"region":{"left":0,"top":100,"right":1080,"bottom":1300},"maxResults":3,"timeoutMs":45000}""")

        assertEquals(listOf("accessibility.captureScreenImage", "barcode.detectAll", "image.recycle"), caller.calls.map { it.key })
        val capture = caller.calls[0]
        assertEquals(listOf("accessibility", "screen_capture", "image"), capture.permissions)
        assertEquals(ToolCatalog.Images.CAPTURE_TIMEOUT_MS, capture.timeoutMs)
        val detect = caller.calls[1]
        assertEquals(listOf("barcode", "image"), detect.permissions)
        assertEquals(45_000L, detect.timeoutMs)
        assertEquals("img-s", detect.args[0].jsonObject["__autojs6ImageHandle"]?.jsonPrimitive?.content)
        val options = detect.args[1].jsonObject
        assertEquals("""["QR_CODE","EAN_13"]""", options["formats"].toString())
        assertEquals(3, options["maxResults"]?.jsonPrimitive?.content?.toInt())
        assertEquals("""{"x":0,"y":100,"width":1080,"height":1200}""", options["region"].toString())
        assertEquals("img-s", caller.calls[2].args[0].jsonObject["id"]?.jsonPrimitive?.content)
        assertEquals(listOf("image"), caller.calls[2].permissions)

        assertEquals("QR_CODE: https://example.org\nEAN_13: (no raw value)", result.text)
        val structured = result.structured
        assertEquals("screen", structured["source"]?.jsonPrimitive?.content)
        assertEquals(2, structured["count"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1080, structured["width"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1.0, structured["scale"]?.jsonPrimitive?.content?.toDouble())
        assertEquals("accessibility", structured["captureSource"]?.jsonPrimitive?.content)
        assertNull(structured["decodedWidth"])
        val first = structured["items"]!!.jsonArray[0].jsonObject
        assertEquals("https://example.org", first["rawValue"]?.jsonPrimitive?.content)
        assertEquals("example.org", first["displayValue"]?.jsonPrimitive?.content)
        assertEquals("QR_CODE", first["format"]?.jsonPrimitive?.content)
        assertEquals("URL", first["valueType"]?.jsonPrimitive?.content)
        assertEquals("""{"left":100,"top":200,"right":300,"bottom":400}""", first["bounds"].toString())
        assertEquals(4, first["cornerPoints"]!!.jsonArray.size)
        assertEquals("""{"url":"https://example.org"}""", first["structured"].toString())
        val second = structured["items"]!!.jsonArray[1].jsonObject
        assertEquals(JsonNull, second["rawValue"])
        assertNull(second["bounds"])
    }

    @Test
    fun `a downscaled file maps the region to decoded pixels and the coordinates back to file pixels`() {
        val caller = FakeCaller(mutableMapOf(
            "image.readImage" to { BridgeOutcome.Ok(fileHandle, 2L) },
            "barcode.detectAll" to { BridgeOutcome.Ok(snapshots, 3L) },
        ))
        val result = run(caller, """{"image":"./photos/label.jpg","region":{"left":1001,"top":0,"right":5000,"bottom":3001}}""")

        assertEquals(listOf("image.readImage", "barcode.detectAll", "image.recycle"), caller.calls.map { it.key })
        assertEquals("photos/label.jpg", caller.calls[0].args[0].jsonPrimitive.content)
        assertEquals(ToolCatalog.Images.READ_TIMEOUT_MS, caller.calls[0].timeoutMs)
        val options = caller.calls[1].args[1].jsonObject
        // File pixels 1001..5000 x 0..3001 at scale 0.5: x floor(500.5) = 500, right 2500, bottom ceil(1500.5) = 1501.
        assertEquals("""{"x":500,"y":0,"width":2000,"height":1501}""", options["region"].toString())
        assertEquals(10, options["maxResults"]?.jsonPrimitive?.content?.toInt())
        assertNull(options["formats"])
        assertEquals(30_000L, caller.calls[1].timeoutMs)

        val structured = result.structured
        assertEquals("file", structured["source"]?.jsonPrimitive?.content)
        assertEquals(6000, structured["width"]?.jsonPrimitive?.content?.toInt())
        assertEquals(0.5, structured["scale"]?.jsonPrimitive?.content?.toDouble())
        assertEquals(3000, structured["decodedWidth"]?.jsonPrimitive?.content?.toInt())
        assertNull(structured["captureSource"])
        val first = structured["items"]!!.jsonArray[0].jsonObject
        assertEquals("""{"left":200,"top":400,"right":600,"bottom":800}""", first["bounds"].toString())
        assertEquals("""{"x":600,"y":400}""", first["cornerPoints"]!!.jsonArray[1].toString())
    }

    @Test
    fun `no code and a missing plugin are reported clearly and the handle is always recycled`() {
        val empty = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "barcode.detectAll" to { BridgeOutcome.Ok(JsonArray(emptyList()), 3L) },
        ))
        val result = run(empty, """{"image":"screen"}""")
        assertEquals("(no barcode found)", result.text)
        assertEquals(0, result.structured["count"]?.jsonPrimitive?.content?.toInt())

        val missing = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "barcode.detectAll" to { BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "requires an enabled compatible recognition plugin", code = "x", category = BridgeError.CATEGORY_UNAVAILABLE, module = "barcode", method = "detectAll"))) },
        ))
        try {
            run(missing, """{"image":"screen"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, e.failure.code)
            assertTrue(e.failure.message, e.failure.message.startsWith("tool barcode_detect needs the barcode plugin family, which is not installed, disabled, or not usable: "))
            assertEquals(ToolFailure.HINT_PLUGIN, e.failure.hint)
        }
        assertEquals(listOf("accessibility.captureScreenImage", "barcode.detectAll", "image.recycle"), missing.calls.map { it.key })
    }

    @Test
    fun `screen consent is requested once and a full handle table gets a hint`() {
        var attempts = 0
        val denied = BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "Screen capture permission is required.", code = "x", category = BridgeError.CATEGORY_PERMISSION_DENIED, module = "accessibility", method = "captureScreenImage")))
        val consenting = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { if (attempts++ == 0) denied else BridgeOutcome.Ok(screenHandle, 2L) },
            "barcode.detectAll" to { BridgeOutcome.Ok(snapshots, 3L) },
        ))
        run(consenting, """{"image":"screen"}""")
        assertEquals(
            listOf("accessibility.captureScreenImage", "media_projection.requestScreenCapture", "accessibility.captureScreenImage", "barcode.detectAll", "image.recycle"),
            consenting.calls.map { it.key },
        )
        assertEquals(listOf("screen_capture"), consenting.calls[1].permissions)

        val refusing = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { denied },
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

        val full = FakeCaller(mutableMapOf(
            "image.readImage" to { BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "Image bridge exceeded 8 active handles.", code = "x", category = BridgeError.CATEGORY_RESOURCE_LIMIT, module = "image", method = "readImage"))) },
        ))
        try {
            run(full, """{"image":"a.png"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.LIMIT_EXCEEDED, e.failure.code)
            assertEquals(ImageInputs.HINT_HANDLES, e.failure.hint)
        }
        // Nothing was acquired, so nothing is recycled.
        assertEquals(listOf("image.readImage"), full.calls.map { it.key })
    }

    @Test
    fun `arguments are checked before any host call`() {
        listOf(
            """{}""" to "requires the argument image",
            """{"image":"/sdcard/a.png"}""" to "inside the AutoJs6 working directory",
            """{"image":"screen","formats":[]}""" to "at least one symbology",
            """{"image":"screen","formats":["QR"]}""" to "formats entries must be one of",
            """{"image":"screen","maxResults":0}""" to "maxResults must be at least 1",
            """{"image":"screen","maxResults":129}""" to "maxResults must be at most 128",
            """{"image":"screen","region":{"left":5,"top":5,"right":5,"bottom":9}}""" to "positive width and height",
            """{"image":"screen","timeoutMs":500}""" to "timeoutMs must be at least 1000",
        ).forEach { (arguments, expected) ->
            try {
                tools.planFor(ToolCatalog.barcodeDetect, args(arguments))
                fail("$arguments should be rejected")
            } catch (e: ToolArgumentException) {
                assertTrue("$arguments -> ${e.failure.message}", e.failure.message.contains(expected))
            }
        }

        // A region beyond the screen is rejected once the handle's size is known, and the handle is still recycled.
        val capturing = FakeCaller(mutableMapOf("accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) }))
        try {
            run(capturing, """{"image":"screen","region":{"left":0,"top":0,"right":1081,"bottom":10}}""")
            fail("expected a failure")
        } catch (e: ToolArgumentException) {
            assertTrue(e.failure.message, e.failure.message.contains("outside the image (1080 x 2400)"))
        }
        assertEquals(listOf("accessibility.captureScreenImage", "image.recycle"), capturing.calls.map { it.key })
    }

    @Test
    fun `the catalog row declares every host method the flow uses`() {
        val spec = ToolCatalog.barcodeDetect
        assertEquals(ToolGroup.BARCODE, spec.group)
        assertEquals(BridgeMethod("barcode", "detectAll"), spec.bridge)
        assertEquals(
            listOf("accessibility.captureScreenImage", "media_projection.requestScreenCapture", "image.readImage", "barcode.detectAll", "image.recycle"),
            spec.hostMethods.map { it.key },
        )
        assertEquals(listOf("image", "formats", "region", "maxResults", "timeoutMs"), spec.properties.keys.toList())
        assertEquals(listOf("image"), spec.required)
        assertEquals(30_000L, spec.timeoutMs)
        assertTrue(spec.hints.readOnly)
        assertNull(tools.planFor(ToolCatalog.ocrRecognize, args("""{"image":"screen"}""")))
    }
}
