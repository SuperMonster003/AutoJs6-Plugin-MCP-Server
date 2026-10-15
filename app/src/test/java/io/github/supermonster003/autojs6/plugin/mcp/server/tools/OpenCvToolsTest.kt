package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OpenCvToolsTest {

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

    private val tools = OpenCvTools(clock = { 5_000L })

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    private val screenHandle = json("""{"id":"img-s","__autojs6ImageHandle":"img-s","width":1080,"height":2400,"captureSource":"accessibility"}""")
    private val templateHandle = json("""{"id":"img-t","__autojs6ImageHandle":"img-t","width":100,"height":60}""")
    private val bigFileHandle = json("""{"id":"img-f","__autojs6ImageHandle":"img-f","width":3000,"height":2000,"sourceWidth":6000,"sourceHeight":4000,"scale":0.5}""")

    /** Answers `image.readImage` by path: the template for paths under `tpl`, the big file otherwise. */
    private fun readByPath(call: Call): BridgeOutcome =
        BridgeOutcome.Ok(if (call.args[0].jsonPrimitive.content.startsWith("tpl/")) templateHandle else bigFileHandle, 2L)

    private fun run(caller: FakeCaller, spec: ToolSpec, arguments: String): ToolFlowResult = runBlocking {
        tools.planFor(spec, json(arguments).jsonObject)!!.run(caller)
    }

    @Test
    fun `find image matches a template on the screen and reports bounds, center and score`() {
        val caller = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "image.readImage" to ::readByPath,
            "image.matchTemplate" to { BridgeOutcome.Ok(json("""[{"x":200.0,"y":500.0,"score":0.97},{"x":700,"y":1500,"score":0.91}]"""), 3L) },
        ))
        val result = run(caller, ToolCatalog.opencvFindImage, """{"image":"screen","template":"tpl/ok.png","threshold":0.85,"region":{"left":0,"top":400,"right":1080,"bottom":2000},"maxResults":2}""")

        assertEquals(
            listOf("accessibility.captureScreenImage", "image.readImage", "image.matchTemplate", "image.recycle", "image.recycle"),
            caller.calls.map { it.key },
        )
        val match = caller.calls[2]
        assertEquals(listOf("image"), match.permissions)
        assertEquals(30_000L, match.timeoutMs)
        assertEquals("img-s", match.args[0].jsonObject["id"]?.jsonPrimitive?.content)
        assertEquals("img-t", match.args[1].jsonObject["id"]?.jsonPrimitive?.content)
        val options = match.args[2].jsonObject
        assertEquals(0.85, options["threshold"]?.jsonPrimitive?.content?.toDouble())
        assertEquals(2, options["limit"]?.jsonPrimitive?.content?.toInt())
        assertEquals("[0,400,1080,1600]", options["region"].toString())
        // The template is released first, then the image.
        assertEquals("img-t", caller.calls[3].args[0].jsonObject["id"]?.jsonPrimitive?.content)
        assertEquals("img-s", caller.calls[4].args[0].jsonObject["id"]?.jsonPrimitive?.content)

        val structured = result.structured
        assertEquals("screen", structured["source"]?.jsonPrimitive?.content)
        assertEquals(2, structured["count"]?.jsonPrimitive?.content?.toInt())
        assertEquals(100, structured["templateWidth"]?.jsonPrimitive?.content?.toInt())
        val first = structured["matches"]!!.jsonArray[0].jsonObject
        assertEquals("""{"left":200,"top":500,"right":300,"bottom":560}""", first["bounds"].toString())
        assertEquals("""{"x":250,"y":530}""", first["center"].toString())
        assertEquals(0.97, first["score"]?.jsonPrimitive?.content?.toDouble())
        assertEquals("center (250, 530) score 0.97\ncenter (750, 1530) score 0.91", result.text)
    }

    @Test
    fun `find image refuses different scales and a template larger than the search area, recycling both handles`() {
        val caller = FakeCaller(mutableMapOf("image.readImage" to ::readByPath))
        try {
            run(caller, ToolCatalog.opencvFindImage, """{"image":"photos/big.jpg","template":"tpl/icon.png"}""")
            fail("expected a failure")
        } catch (e: ToolArgumentException) {
            assertTrue(e.failure.message, e.failure.message.contains("template matching needs both at one scale"))
            assertTrue(e.failure.message, e.failure.message.contains("(6000 x 4000) at scale 0.5"))
        }
        assertEquals(listOf("image.readImage", "image.readImage", "image.recycle", "image.recycle"), caller.calls.map { it.key })

        val screen = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "image.readImage" to ::readByPath,
        ))
        try {
            run(screen, ToolCatalog.opencvFindImage, """{"image":"screen","template":"tpl/icon.png","region":{"left":0,"top":0,"right":99,"bottom":2400}}""")
            fail("expected a failure")
        } catch (e: ToolArgumentException) {
            assertTrue(e.failure.message, e.failure.message.contains("the template (100 x 60) is larger than the search area (99 x 2400)"))
        }
        assertEquals(listOf("accessibility.captureScreenImage", "image.readImage", "image.recycle", "image.recycle"), screen.calls.map { it.key })

        // A template that is itself named "screen" is still read as a file.
        val named = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "image.readImage" to { BridgeOutcome.Ok(templateHandle, 2L) },
            "image.matchTemplate" to { BridgeOutcome.Ok(JsonArray(emptyList()), 3L) },
        ))
        val empty = run(named, ToolCatalog.opencvFindImage, """{"image":"screen","template":"screen"}""")
        assertEquals("screen", named.calls[1].args[0].jsonPrimitive.content)
        assertEquals("(no match with similarity >= 0.9)", empty.text)
        assertEquals(0, empty.structured["count"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `find color sends the normalized color and maps the point of a downscaled file back to file pixels`() {
        val caller = FakeCaller(mutableMapOf(
            "image.readImage" to ::readByPath,
            "image.findColor" to { BridgeOutcome.Ok(json("""{"x":1000.0,"y":250.0}"""), 3L) },
        ))
        val result = run(caller, ToolCatalog.opencvFindColor, """{"image":"photos/big.jpg","color":"#f0a","threshold":16,"region":{"left":10,"top":11,"right":4001,"bottom":3000}}""")

        assertEquals(listOf("image.readImage", "image.findColor", "image.recycle"), caller.calls.map { it.key })
        val find = caller.calls[1]
        assertEquals(listOf("image"), find.permissions)
        assertEquals("#FF00AA", find.args[1].jsonPrimitive.content)
        val options = find.args[2].jsonObject
        assertEquals(16, options["threshold"]?.jsonPrimitive?.content?.toInt())
        // File pixels 10..4001 x 11..3000 at scale 0.5: x 5, y floor(5.5) = 5, right ceil(2000.5) = 2001, bottom 1500.
        assertEquals("[5,5,1996,1495]", options["region"].toString())

        val structured = result.structured
        assertTrue(structured["found"]!!.jsonPrimitive.content.toBoolean())
        // Decoded pixel (1000, 250) covers file pixels 2000..2001 x 500..501; its center falls on (2001, 501).
        assertEquals("""{"x":2001,"y":501}""", structured["point"].toString())
        assertEquals("#FF00AA", structured["color"]?.jsonPrimitive?.content)
        assertEquals(0.5, structured["scale"]?.jsonPrimitive?.content?.toDouble())
        assertEquals("found at (2001, 501)", result.text)

        val missing = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "image.findColor" to { BridgeOutcome.Ok(JsonNull, 3L) },
        ))
        val none = run(missing, ToolCatalog.opencvFindColor, """{"image":"screen","color":"#123456"}""")
        assertEquals(4, missing.calls[1].args[2].jsonObject["threshold"]?.jsonPrimitive?.content?.toInt())
        assertNull(missing.calls[1].args[2].jsonObject["region"])
        assertFalse(none.structured["found"]!!.jsonPrimitive.content.toBoolean())
        assertNull(none.structured["point"])
        assertEquals("(color not found)", none.text)
    }

    @Test
    fun `find multi colors sends the neighbors as host triples scaled to decoded pixels`() {
        val caller = FakeCaller(mutableMapOf(
            "image.readImage" to ::readByPath,
            "image.findMultiColors" to { BridgeOutcome.Ok(json("""{"x":10,"y":20}"""), 3L) },
        ))
        val result = run(caller, ToolCatalog.opencvFindMultiColors, """{"image":"photos/big.jpg","color":"#FFFFFF","points":[{"x":10,"y":-5,"color":"#000"},{"x":-3,"y":0,"color":"#00ff00"}]}""")

        assertEquals(listOf("image.readImage", "image.findMultiColors", "image.recycle"), caller.calls.map { it.key })
        val find = caller.calls[1]
        assertEquals("#FFFFFF", find.args[1].jsonPrimitive.content)
        // Offsets in file pixels at scale 0.5: (10, -5) -> (5, -2.5 -> -2), (-3, 0) -> (-1.5 -> -1, 0).
        assertEquals("""[[5,-2,"#000000"],[-1,0,"#00FF00"]]""", find.args[2].toString())
        assertEquals("""{"threshold":4}""", find.args[3].toString())
        assertEquals("""{"x":21,"y":41}""", result.structured["point"].toString())
        assertEquals(2, result.structured["points"]!!.jsonArray.size)

        val screen = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "image.findMultiColors" to { BridgeOutcome.Ok(JsonNull, 3L) },
        ))
        val none = run(screen, ToolCatalog.opencvFindMultiColors, """{"image":"screen","color":"#FFFFFF","points":[{"x":1,"y":1,"color":"#000000"}]}""")
        assertEquals("""[[1,1,"#000000"]]""", screen.calls[1].args[2].toString())
        assertEquals("(color pattern not found)", none.text)
    }

    @Test
    fun `host failures keep their mapping and the handle is recycled`() {
        val caller = FakeCaller(mutableMapOf(
            "accessibility.captureScreenImage" to { BridgeOutcome.Ok(screenHandle, 2L) },
            "image.findColor" to { BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "OpenCV plugin is missing", code = "x", category = BridgeError.CATEGORY_PROVIDER_FAILED, module = "image", method = "findColor"))) },
        ))
        try {
            run(caller, ToolCatalog.opencvFindColor, """{"image":"screen","color":"#FFFFFF"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.HOST_ERROR, e.failure.code)
            assertEquals("findColor", e.failure.method)
        }
        assertEquals(listOf("accessibility.captureScreenImage", "image.findColor", "image.recycle"), caller.calls.map { it.key })
    }

    @Test
    fun `arguments are checked before any host call`() {
        listOf(
            ToolCatalog.opencvFindImage to """{"image":"screen"}""" to "requires the argument template",
            ToolCatalog.opencvFindImage to """{"image":"screen","template":"/sdcard/t.png"}""" to "reading shared storage is switched off",
            ToolCatalog.opencvFindImage to """{"image":"screen","template":"t.png","threshold":1.5}""" to "threshold must be at most 1.0",
            ToolCatalog.opencvFindImage to """{"image":"screen","template":"t.png","maxResults":101}""" to "maxResults must be at most 100",
            ToolCatalog.opencvFindColor to """{"image":"screen"}""" to "requires the argument color",
            ToolCatalog.opencvFindColor to """{"image":"screen","color":"red"}""" to "color must be a color like #RRGGBB or #RGB",
            ToolCatalog.opencvFindColor to """{"image":"screen","color":"#12345"}""" to "color must be a color like",
            ToolCatalog.opencvFindColor to """{"image":"screen","color":"#123456","threshold":256}""" to "threshold must be at most 255",
            ToolCatalog.opencvFindMultiColors to """{"image":"screen","color":"#123456"}""" to "requires the argument points",
            ToolCatalog.opencvFindMultiColors to """{"image":"screen","color":"#123456","points":[]}""" to "points must list 1 to 64 neighbors",
            ToolCatalog.opencvFindMultiColors to """{"image":"screen","color":"#123456","points":[{"x":1,"y":1}]}""" to "points[0] must contain exactly x, y and color",
            ToolCatalog.opencvFindMultiColors to """{"image":"screen","color":"#123456","points":[{"x":1.5,"y":1,"color":"#000"}]}""" to "points[0].x must be an integer",
            ToolCatalog.opencvFindMultiColors to """{"image":"screen","color":"#123456","points":[{"x":1,"y":1,"color":"black"}]}""" to "points[0].color must be a color like",
        ).forEach { (input, expected) ->
            val (spec, arguments) = input
            try {
                tools.planFor(spec, json(arguments).jsonObject)
                fail("${spec.name} $arguments should be rejected")
            } catch (e: ToolArgumentException) {
                assertTrue("${spec.name} $arguments -> ${e.failure.message}", e.failure.message.contains(expected))
            }
        }
    }

    @Test
    fun `the catalog rows declare every host method their flow uses`() {
        listOf(
            ToolCatalog.opencvFindImage to "image.matchTemplate",
            ToolCatalog.opencvFindColor to "image.findColor",
            ToolCatalog.opencvFindMultiColors to "image.findMultiColors",
        ).forEach { (spec, operation) ->
            assertEquals(ToolGroup.OPENCV, spec.group)
            assertEquals(operation, spec.bridge?.key)
            assertEquals(
                listOf("accessibility.captureScreenImage", "media_projection.requestScreenCapture", "image.readImage", operation, "image.recycle"),
                spec.hostMethods.map { it.key },
            )
            assertEquals(listOf("image", "accessibility", "screen_capture"), spec.permissions)
            assertTrue(spec.hints.readOnly)
        }
        assertEquals(listOf("image", "template", "threshold", "region", "maxResults", "timeoutMs"), ToolCatalog.opencvFindImage.properties.keys.toList())
        assertEquals(listOf("image", "template"), ToolCatalog.opencvFindImage.required)
        assertEquals(listOf("image", "color", "points"), ToolCatalog.opencvFindMultiColors.required)
        assertNull(tools.planFor(ToolCatalog.barcodeDetect, json("""{"image":"screen"}""").jsonObject))
    }
}
