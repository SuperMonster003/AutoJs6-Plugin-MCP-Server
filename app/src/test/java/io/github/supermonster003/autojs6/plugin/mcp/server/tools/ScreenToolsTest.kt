package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeBytes
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
import org.autojs.plugin.mcp.server.api.McpServerContract
import org.junit.Assert.*
import org.junit.Test
import kotlin.io.encoding.Base64

class ScreenToolsTest {
    private data class Call(val key: String, val args: JsonArray, val permissions: List<String>, val timeoutMs: Long)
    private class Host : BridgeCaller {
        val calls = mutableListOf<Call>()
        var landscape = false
        var capture: (Call) -> BridgeOutcome = { image(it) }
        var consent: BridgeOutcome = BridgeOutcome.Ok(json("""{"id":"projection"}"""), 1)
        override suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome {
            val call = Call("$module.$method", args, permissions, timeoutMs).also { calls += it }
            return when (call.key) {
                "device.info" -> BridgeOutcome.Ok(json("""{"screen":{"width":${if (landscape) 2400 else 1080},"height":${if (landscape) 1080 else 2400},"density":3,"densityDpi":480,"orientation":"portrait","rotation":${if (landscape) 1 else 0}}}"""), 1)
                "device.isScreenOn" -> BridgeOutcome.Ok(JsonPrimitive(true), 1)
                "media_projection.requestScreenCapture" -> consent
                else -> capture(call)
            }
        }
    }

    @Test fun `default capture bounds longest edge and returns image plus metadata`() = runBlocking {
        val host = Host()
        val result = run(host)
        val call = host.calls.last()
        assertEquals("accessibility.screenshot", call.key)
        assertEquals(json("""{"format":"jpeg","quality":70,"maxWidth":576}"""), call.args[0])
        assertEquals(listOf("accessibility", "screen_capture"), call.permissions)
        assertEquals(15_000L, call.timeoutMs)
        assertArrayEquals(byteArrayOf(1, 2, 3), Base64.decode(result.image!!.data))
        assertEquals("image/jpeg", result.image.mimeType)
        assertEquals(JsonPrimitive(4), result.structured["encodedBytes"])
        assertEquals(JsonPrimitive(false), result.structured["adjusted"])
        assertFalse(result.text.contains("payload"))
        val response = ToolResults.success(result.structured, result.text, result.image)
        assertEquals(2, response.content.size)
        assertFalse(response.isError!!)
    }

    @Test fun `region and explicit scale preserve the host encoding options`() = runBlocking {
        val host = Host()
        val args = """{"scale":0.5,"format":"webp","quality":90,"region":{"left":10,"top":20,"right":110,"bottom":220}}"""
        run(host, args)
        assertEquals(json(args), host.calls.last().args[0])
    }

    @Test fun `default crop scales according to cropped aspect ratio`() = runBlocking {
        val host = Host()
        run(host, """{"region":{"left":0,"top":0,"right":100,"bottom":200}}""")
        assertEquals(JsonPrimitive(100), host.calls.last().args[0].jsonObject["maxWidth"])
    }

    @Test fun `integer-valued decimals use the supplied width and quality`() = runBlocking {
        val host = Host()
        run(host, """{"maxWidth":1000.0,"quality":60.0}""")
        assertEquals(JsonPrimitive(1000), host.calls.last().args[0].jsonObject["maxWidth"])
        assertEquals(JsonPrimitive(60), host.calls.last().args[0].jsonObject["quality"])
    }

    @Test fun `unavailable accessibility falls back with explicit consent and encoded capture`() = runBlocking {
        val host = Host().apply { capture = { if (it.key == "accessibility.screenshot") unavailable() else image(it) } }
        val tools = ScreenTools()
        repeat(2) { assertEquals(JsonPrimitive("media_projection"), run(host, tools = tools).structured["source"]) }
        assertEquals(2, host.calls.count { it.key == "media_projection.requestScreenCapture" })
        val consent = host.calls.first { it.key == "media_projection.requestScreenCapture" }
        assertEquals(60_000L, consent.timeoutMs)
        assertEquals(listOf("screen_capture"), consent.permissions)
        assertEquals(listOf("image", "screen_capture"), host.calls.last().permissions)
    }

    @Test fun `structured fallback is supported and permission denial includes phone guidance`() = runBlocking {
        val host = Host().apply {
            capture = { BridgeOutcome.Ok(json("""{"fallback":"media_projection"}"""), 1) }
            consent = BridgeOutcome.Failed(ToolFailure(ToolErrorCodes.CAPABILITY_DENIED, "denied"))
        }
        val failure = failure { run(host) }
        assertEquals(ToolErrorCodes.CAPABILITY_DENIED, failure.code)
        assertTrue(failure.hint!!.contains("approve AutoJs6"))
        assertFalse(host.calls.any { it.key == "image.captureScreen" })
    }

    @Test fun `host failures other than unavailable never request capture consent`() = runBlocking {
        val host = Host().apply { capture = { BridgeOutcome.Failed(ToolFailure.hostUnavailable()) } }
        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, failure { run(host) }.code)
        assertEquals(2, host.calls.size)
    }

    @Test fun `base64 limit reduces jpeg quality before dimensions and reports it`() = runBlocking {
        val host = Host()
        var count = 0
        host.capture = { image(it, if (count++ == 0) 3 * 1024 * 1024 + 1 else 3) }
        val result = run(host)
        assertEquals(JsonPrimitive(55), result.structured["quality"])
        assertEquals(JsonPrimitive(true), result.structured["adjusted"])
        assertEquals(JsonPrimitive(2), result.structured["attempts"])
        assertEquals(JsonPrimitive(576), host.calls.last().args[0].jsonObject["maxWidth"])
        assertEquals(McpServerContract.MAX_SCREENSHOT_ENCODED_BYTES.toLong(), ScreenTools.base64Size(3 * 1024 * 1024))
    }

    @Test fun `host size rejection reduces png dimensions and preserves format`() = runBlocking {
        val host = Host()
        var count = 0
        host.capture = { if (count++ == 0) BridgeOutcome.Failed(ToolFailure.limitExceeded("encoded image too large")) else image(it) }
        val result = run(host, """{"format":"png","maxWidth":1000}""")
        val options = host.calls.last().args[0].jsonObject
        assertEquals(JsonPrimitive(700), options["maxWidth"])
        assertEquals(JsonPrimitive(70), options["quality"])
        assertEquals("image/png", result.image!!.mimeType)
    }

    @Test fun `persistent size rejection terminates within the attempt budget`() = runBlocking {
        val host = Host().apply { capture = { BridgeOutcome.Failed(ToolFailure.limitExceeded("large")) } }
        assertEquals(ToolErrorCodes.LIMIT_EXCEEDED, failure { run(host) }.code)
        assertEquals(ToolCatalog.Screen.MAX_ATTEMPTS, host.calls.count { it.key == "accessibility.screenshot" })
    }

    @Test fun `malformed metadata missing descriptors and wrong MIME fail without publishing an image`() = runBlocking {
        val variants = listOf(
            BridgeOutcome.Ok(json("{}"), 1),
            BridgeOutcome.Ok(json("""{"bytes":3,"width":1,"height":1,"mime":"image/png"}"""), 1, BridgeBytes(byteArrayOf(1, 2, 3), "image/png")),
            BridgeOutcome.Ok(json("""{"bytes":4,"width":1,"height":1,"mime":"image/jpeg"}"""), 1, BridgeBytes(byteArrayOf(1, 2, 3), "image/jpeg")),
        )
        variants.forEach { result -> assertEquals(ToolErrorCodes.HOST_ERROR, failure { run(Host().apply { capture = { result } }) }.code) }
    }

    @Test fun `invalid or unknown options and rectangles fail before capture`() = runBlocking {
        listOf(
            """{"scale":0.5,"maxWidth":100}""", """{"scale":0}""", """{"maxWidth":9000}""", """{"quality":0}""",
            """{"format":"gif"}""", """{"annotate":true}""", """{"region":{"left":0,"top":0,"right":1}}""",
            """{"region":{"left":1,"top":0,"right":1,"bottom":2}}""", """{"region":{"left":-1,"top":0,"right":1,"bottom":2}}""",
            """{"region":{"left":0,"top":0,"right":1,"bottom":2,"x":1}}""",
        ).forEach { args ->
            val host = Host()
            try { run(host, args); fail(args) } catch (e: ToolArgumentException) { assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, e.failure.code) }
            assertTrue(host.calls.isEmpty())
        }
    }

    @Test fun `screen state reports oriented dimensions and never captures`() = runBlocking {
        val host = Host().apply { landscape = true }
        val result = ScreenTools().planFor(ToolCatalog.screenState, json("{}"))!!.run(host)
        assertEquals(JsonPrimitive(2400), result.structured["width"])
        assertEquals(JsonPrimitive(1080), result.structured["height"])
        assertEquals(JsonPrimitive("landscape"), result.structured["orientation"])
        assertEquals(JsonPrimitive(480), result.structured["densityDpi"])
        assertEquals(JsonPrimitive(true), result.structured["screenOn"])
        assertEquals(listOf("device.isScreenOn", "device.info"), host.calls.map { it.key })
    }

    private suspend fun run(host: Host, args: String = "{}", tools: ScreenTools = ScreenTools()): ToolFlowResult =
        tools.planFor(ToolCatalog.screenCapture, json(args))!!.run(host)

    private suspend fun failure(block: suspend () -> Unit): ToolFailure {
        try { block() } catch (e: ToolFailureException) { return e.failure }
        throw AssertionError("expected a tool failure")
    }

    companion object {
        private fun json(value: String) = Json.parseToJsonElement(value).jsonObject
        private fun unavailable() = BridgeOutcome.Failed(ToolFailure(ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING, "API below 30", category = BridgeError.CATEGORY_UNAVAILABLE))
        private fun image(call: Call, bytes: Int = 3): BridgeOutcome {
            val options = call.args[0].jsonObject
            val format = options["format"]!!.jsonPrimitive.content
            return BridgeOutcome.Ok(json("""{"width":100,"height":200,"bytes":$bytes,"mime":"image/$format"}"""), 2,
                BridgeBytes(ByteArray(bytes) { (it + 1).toByte() }, "image/$format"))
        }
    }
}
