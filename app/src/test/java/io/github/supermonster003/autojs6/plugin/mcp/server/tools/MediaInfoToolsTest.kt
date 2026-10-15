package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MediaInfoToolsTest {

    private class Call(val module: String, val method: String, val args: JsonArray, val timeoutMs: Long, val permissions: List<String>)

    private class FakeCaller(val answer: (Call) -> BridgeOutcome) : BridgeCaller {

        val calls = mutableListOf<Call>()

        override suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome {
            val call = Call(module, method, args, timeoutMs, permissions)
            calls += call
            return answer(call)
        }
    }

    private val tools = MediaInfoTools(clock = { 7_000L })

    private val snapshot = Json.parseToJsonElement(
        """{"schema":"autojs6-node-mediainfo-snapshot-v1","path":"music/a.mp4","fileName":"a.mp4","sizeBytes":4821,
           "inform":"General\nFormat : MPEG-4\n","sections":{
             "general":[{"completeName":"a.mp4","format":"MPEG-4","fileSize":"4.71 KiB","duration":"1 s 0 ms","overallBitRate":"38.6 kb/s","writingApplication":"Lavf"}],
             "video":[{"id":"1","format":"AVC","width":"320 pixels","height":"240 pixels","frameRate":"25.000 FPS","bitRate":"30 kb/s"}],
             "audio":[{"format":"AAC LC","channels":"2 channels","samplingRate":"44.1 kHz"},{"format":"AAC LC","channels":"1 channel","language":"English"}],
             "menu":[{"chapter1":"00:00:00.000 Intro"}]}}""",
    )

    private fun args(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun run(caller: FakeCaller, arguments: String): ToolFlowResult = runBlocking {
        tools.planFor(ToolCatalog.mediainfoRead, args(arguments))!!.run(caller)
    }

    @Test
    fun `the snapshot keeps every section and the text summarizes each track`() {
        val caller = FakeCaller { BridgeOutcome.Ok(snapshot, 1L) }
        val result = run(caller, """{"path":"./music/a.mp4"}""")

        val call = caller.calls.single()
        assertEquals("mediainfo.read", "${call.module}.${call.method}")
        assertEquals("music/a.mp4", call.args[0].jsonPrimitive.content)
        assertEquals("""{"includeInform":false,"includeSections":true}""", call.args[1].toString())
        assertEquals(listOf("media", "media.metadata"), call.permissions)
        assertEquals(60_000L, call.timeoutMs)

        assertEquals(
            listOf(
                "general: format=MPEG-4, duration=1 s 0 ms, fileSize=4.71 KiB, overallBitRate=38.6 kb/s",
                "video: format=AVC, width=320 pixels, height=240 pixels, frameRate=25.000 FPS, bitRate=30 kb/s",
                "audio #1: format=AAC LC, channels=2 channels, samplingRate=44.1 kHz",
                "audio #2: format=AAC LC, channels=1 channel, language=English",
                "menu: chapter1=00:00:00.000 Intro",
            ).joinToString("\n"),
            result.text,
        )
        val structured = result.structured
        assertEquals("./music/a.mp4", structured["path"]?.jsonPrimitive?.content)
        assertEquals("a.mp4", structured["fileName"]?.jsonPrimitive?.content)
        assertEquals(4821L, structured["sizeBytes"]?.jsonPrimitive?.content?.toLong())
        assertEquals("""{"general":1,"video":1,"audio":2,"menu":1}""", structured["trackCounts"].toString())
        assertEquals(snapshot.jsonObject["sections"], structured["sections"])
        assertNull(structured["report"])
    }

    @Test
    fun `report adds the text report and truncates it`() {
        val long = "x".repeat(ToolCatalog.MediaInfo.MAX_REPORT_CHARS + 10)
        val caller = FakeCaller { call ->
            assertEquals("""{"includeInform":true,"includeSections":true}""", call.args[1].toString())
            BridgeOutcome.Ok(Json.parseToJsonElement("""{"fileName":"b.mp3","inform":"$long","sections":{}}"""), 1L)
        }
        val result = run(caller, """{"path":"b.mp3","report":true,"timeoutMs":90000}""")
        assertEquals(90_000L, caller.calls.single().timeoutMs)
        assertEquals(ToolCatalog.MediaInfo.MAX_REPORT_CHARS, result.structured["report"]?.jsonPrimitive?.content?.length)
        assertEquals("true", result.structured["reportTruncated"]?.jsonPrimitive?.content)
        assertTrue(result.text.startsWith("(MediaInfo found no tracks)\n\nxxx"))
    }

    @Test
    fun `a missing plugin is PLUGIN_UNAVAILABLE and other failures keep their mapping`() {
        val missing = FakeCaller { BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "Install the MediaInfo plugin to inspect audio files.", code = "x", category = BridgeError.CATEGORY_UNAVAILABLE, module = "mediainfo", method = "read"))) }
        try {
            run(missing, """{"path":"a.mp4"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, e.failure.code)
            assertTrue(e.failure.message, e.failure.message.startsWith("tool mediainfo_read needs the mediainfo plugin family"))
        }
        val absent = FakeCaller { BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "AutoJs6 mediainfo target does not exist: a.mp4", code = "x", category = BridgeError.CATEGORY_INVALID_REQUEST, module = "mediainfo", method = "read"))) }
        try {
            run(absent, """{"path":"a.mp4"}""")
            fail("expected a failure")
        } catch (e: ToolFailureException) {
            assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, e.failure.code)
        }
    }

    @Test
    fun `a shared storage file declares files shared read`() {
        val shared = MediaInfoTools(clock = { 7_000L }, access = PathAccess(listOf("/storage/emulated/0", "/sdcard")) { true })
        val caller = FakeCaller { BridgeOutcome.Ok(snapshot, 1L) }
        runBlocking { shared.planFor(ToolCatalog.mediainfoRead, args("""{"path":"/sdcard/Music/a.mp4"}"""))!!.run(caller) }
        assertEquals("/sdcard/Music/a.mp4", caller.calls.single().args[0].jsonPrimitive.content)
        assertEquals(listOf("media", "media.metadata", "files.shared_read"), caller.calls.single().permissions)
        // A relative path stays in the working directory and declares nothing more, switch or not.
        runBlocking { shared.planFor(ToolCatalog.mediainfoRead, args("""{"path":"music/a.mp4"}"""))!!.run(caller) }
        assertEquals(listOf("media", "media.metadata"), caller.calls[1].permissions)
    }

    @Test
    fun `paths are checked before any host call`() {
        listOf(
            """{}""" to "requires the argument path",
            """{"path":"/sdcard/Music/a.mp3"}""" to "reading shared storage is switched off",
            """{"path":"../a.mp3"}""" to "inside the AutoJs6 working directory",
            """{"path":"."}""" to "inside the AutoJs6 working directory",
            """{"path":"a.mp3","timeoutMs":10}""" to "timeoutMs must be at least 1000",
        ).forEach { (arguments, expected) ->
            try {
                tools.planFor(ToolCatalog.mediainfoRead, args(arguments))
                fail("$arguments should be rejected")
            } catch (e: ToolArgumentException) {
                assertTrue("$arguments -> ${e.failure.message}", e.failure.message.contains(expected))
            }
        }
        val spec = ToolCatalog.mediainfoRead
        assertEquals(ToolGroup.MEDIAINFO, spec.group)
        assertEquals(listOf("mediainfo.read"), spec.hostMethods.map { it.key })
        assertEquals(listOf("path", "report", "timeoutMs"), spec.properties.keys.toList())
        assertNull(tools.planFor(ToolCatalog.pinyinConvert, args("""{"text":"中"}""")))
    }
}
