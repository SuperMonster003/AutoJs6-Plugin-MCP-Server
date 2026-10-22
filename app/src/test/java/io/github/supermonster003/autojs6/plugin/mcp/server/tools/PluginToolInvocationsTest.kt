package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpServerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PluginToolInvocationsTest {

    private val report = PluginToolsReport.parse(Json.parseToJsonElement(PluginToolSpecsTest.LIST))!!
    private val opencc = report.tools[0]
    private val quant = report.tools[1]
    private val fake = report.tools[2]
    private val now = 1_760_000_000_123L
    private val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(Date(now))
    private val shared = PathAccess(listOf("/storage/emulated/0")) { true }

    @Test
    fun `a read-only tool forwards its arguments with the module token and a longer time limit`() {
        val call = PluginToolInvocations.prepare(opencc, args("""{"text":"汉字","config":"s2twi"}"""), PathAccess.WORKSPACE_ONLY, now)
        assertEquals("""["opencc_convert",{"text":"汉字","config":"s2twi"}]""", call.args.toString())
        assertEquals(listOf("plugin_tools"), call.permissions)
        assertEquals(12_000L, call.timeoutMs)
        assertTrue(call.outputs.isEmpty())
        assertEquals(listOf("plugin_tools", "plugin_tools.user_granted"), PluginToolInvocations.prepare(fake, JsonObject(emptyMap()), PathAccess.WORKSPACE_ONLY, now).permissions)
    }

    @Test
    fun `images, files and outputs add their tokens and paths`() {
        val screen = PluginToolInvocations.prepare(quant, args("""{"image":" screen ","colors":16}"""), PathAccess.WORKSPACE_ONLY, now)
        assertEquals(listOf("plugin_tools", "screen_capture", "files.write"), screen.permissions)
        assertEquals(mapOf("image" to "plugin-outputs/quant/quantize-$stamp.png"), screen.outputs)
        assertEquals(
            """["quant_quantize",{"image":"screen","colors":16},{"outputs":{"image":"plugin-outputs/quant/quantize-$stamp.png"}}]""",
            screen.args.toString(),
        )
        assertEquals(62_000L, screen.timeoutMs)

        val file = PluginToolInvocations.prepare(quant, args("""{"image":"pics/a.png","outputPaths":{"image":"out/q.png"}}"""), PathAccess.WORKSPACE_ONLY, now)
        assertEquals(listOf("plugin_tools", "files", "files.write"), file.permissions)
        assertEquals("""["quant_quantize",{"image":"pics/a.png"},{"outputs":{"image":"out/q.png"}}]""", file.args.toString())

        val absolute = PluginToolInvocations.prepare(quant, args("""{"image":"/storage/emulated/0/DCIM/a.png"}"""), shared, now)
        assertEquals(listOf("plugin_tools", "files", "files.shared_read", "files.write"), absolute.permissions)
        val closed = assertThrows(ToolArgumentException::class.java) {
            PluginToolInvocations.prepare(quant, args("""{"image":"/storage/emulated/0/DCIM/a.png"}"""), PathAccess.WORKSPACE_ONLY, now)
        }
        assertEquals(ToolErrorCodes.CAPABILITY_DENIED, closed.failure.code)
    }

    @Test
    fun `directory arguments are paths with the files token and a note`() {
        val yolo = opencc.copy(
            name = "yolo_detect", tool = "detect", shortName = "yolo",
            inputSchema = args("""{"type":"object","properties":{"image":{"type":"string"},"model":{"type":"string","description":"The model."}},"required":["image","model"],"additionalProperties":false}"""),
            outputSchema = null, imageParameters = listOf("image"), directoryParameters = listOf("model"),
        )
        val call = PluginToolInvocations.prepare(yolo, args("""{"image":"screen","model":"models/yolo11n/"}"""), PathAccess.WORKSPACE_ONLY, now)
        assertEquals(listOf("plugin_tools", "screen_capture", "files"), call.permissions)
        assertEquals("""["yolo_detect",{"image":"screen","model":"models/yolo11n"}]""", call.args.toString())
        val note = PluginToolSpecs.inputSchema(yolo)["properties"]!!.jsonObject["model"]!!.jsonObject["description"].toString()
        assertTrue(note, note.contains("The model. AutoJs6 accepts a directory path, whose files it hands to the plugin"))
        val parsed = PluginToolsReport.parse(Json.parseToJsonElement(PluginToolSpecsTest.LIST.replace(""""fileParameters":[]},""", """"fileParameters":[],"directoryParameters":["model"]},""")))!!
        assertEquals(listOf("model"), parsed.tools.first().directoryParameters)
    }

    @Test
    fun `directory outputs get a path without extension, a note and a summary of files and conflicts`() {
        val parsed = PluginToolsReport.parse(Json.parseToJsonElement(PluginToolSpecsTest.LIST.replace(
            """"outputs":[{"name":"image","mimeType":"image/png","description":"the quantized PNG"}]""",
            """"outputs":[{"name":"image","kind":"directory","mimeType":"inode/directory","description":"the entries"}]""",
        )))!!
        val extract = parsed.tools[1].copy(name = "archive_extract", tool = "extract", shortName = "archive", imageParameters = emptyList(), fileParameters = listOf("image"))
        assertTrue(extract.outputs.single().isDirectory)
        val call = PluginToolInvocations.prepare(extract, args("""{"image":"a.zip"}"""), PathAccess.WORKSPACE_ONLY, now)
        assertEquals(mapOf("image" to "plugin-outputs/archive/extract-$stamp"), call.outputs)
        val schema = PluginToolSpecs.inputSchema(extract)["properties"]!!.jsonObject[PluginToolSpecs.OUTPUT_PATHS]!!.jsonObject
        assertTrue(schema["description"].toString().contains("files already in it are kept and reported as conflicts"))
        assertTrue(PluginToolSpecs.description(extract).contains("Writes image (a directory) into the AutoJs6 working directory"))
        val (_, summary) = PluginToolInvocations.shape(extract, Json.parseToJsonElement(
            """{"result":{"entries":3},"outputs":[{"name":"image","kind":"directory","path":"out/x","mimeType":"inode/directory","files":2,"bytes":30,"conflicts":["a.txt"],"conflictCount":1}]}""",
        ))
        assertEquals("{\"entries\":3}\nWrote image to out/x (2 files, 30 bytes, 1 conflict kept as it was)", summary)
    }

    @Test
    fun `output paths must name declared outputs inside the working directory`() {
        val unknown = assertThrows(ToolArgumentException::class.java) {
            PluginToolInvocations.prepare(quant, args("""{"image":"screen","outputPaths":{"thumbnail":"t.png"}}"""), PathAccess.WORKSPACE_ONLY, now)
        }
        assertEquals("quant_quantize: outputPaths.thumbnail is not an output of this tool (image)", unknown.failure.message)
        assertThrows(ToolArgumentException::class.java) {
            PluginToolInvocations.prepare(quant, args("""{"image":"screen","outputPaths":{"image":"../q.png"}}"""), PathAccess.WORKSPACE_ONLY, now)
        }
        assertThrows(ToolArgumentException::class.java) {
            PluginToolInvocations.prepare(quant, args("""{"image":"screen","outputPaths":"q.png"}"""), PathAccess.WORKSPACE_ONLY, now)
        }
    }

    @Test
    fun `answers become the result, or the result with the written outputs and a preview candidate`() {
        val (structured, text) = PluginToolInvocations.shape(opencc, Json.parseToJsonElement(
            """{"tool":"opencc_convert","result":{"text":"漢字","config":"s2t"},"text":"漢字","outputs":[],"durationMs":3}""",
        ))
        assertEquals("""{"text":"漢字","config":"s2t"}""", structured.toString())
        assertEquals("漢字", text)

        val answer = Json.parseToJsonElement(
            """{"tool":"quant_quantize","result":{"colors":16},"outputs":[{"name":"image","path":"plugin-outputs/quant/q.png","bytes":2048,"mimeType":"image/png"}],"durationMs":40}""",
        )
        val (withOutputs, summary) = PluginToolInvocations.shape(quant, answer)
        assertEquals("""{"result":{"colors":16},"outputs":[{"name":"image","path":"plugin-outputs/quant/q.png","bytes":2048,"mimeType":"image/png"}]}""", withOutputs.toString())
        assertEquals("{\"colors\":16}\nWrote image to plugin-outputs/quant/q.png (2048 bytes)", summary)
        assertEquals("plugin-outputs/quant/q.png" to "image/png", PluginToolInvocations.previewCandidate(withOutputs))
        val large = JsonObject(withOutputs + ("outputs" to Json.parseToJsonElement("""[{"name":"image","path":"q.png","bytes":2097152,"mimeType":"image/png"}]""")))
        assertNull(PluginToolInvocations.previewCandidate(large))
        assertThrows(ToolFailureException::class.java) { PluginToolInvocations.shape(quant, JsonPrimitive("x")) }
    }

    @Test
    fun `default output files take the extension of the type the plugin wrote`() {
        val synthesize = quant.copy(
            name = "speech_synthesize", tool = "synthesize", shortName = "speech", imageParameters = emptyList(),
            outputs = listOf(PluginToolOutput("audio", "audio/wav")),
        )
        val call = PluginToolInvocations.prepare(synthesize, args("""{"text":"hi","format":"m4a"}"""), PathAccess.WORKSPACE_ONLY, now)
        val wav = "plugin-outputs/speech/synthesize-$stamp.wav"
        assertEquals(mapOf("audio" to wav), call.outputs)
        assertEquals(setOf("audio"), call.defaultOutputs)
        fun answer(path: String, mimeType: String) = Json.parseToJsonElement(
            """{"result":{"format":"m4a"},"outputs":[{"name":"audio","path":"$path","bytes":900,"mimeType":"$mimeType"}]}""",
        )
        val m4a = answer(wav, "audio/mp4")
        val renames = PluginToolInvocations.renames(synthesize, call, m4a)
        assertEquals(mapOf(wav to "plugin-outputs/speech/synthesize-$stamp.m4a"), renames)
        val (_, summary) = PluginToolInvocations.shape(synthesize, PluginToolInvocations.moved(m4a, renames))
        assertEquals("{\"format\":\"m4a\"}\nWrote audio to plugin-outputs/speech/synthesize-$stamp.m4a (900 bytes)", summary)
        assertEquals(m4a, PluginToolInvocations.moved(m4a, emptyMap()))

        assertTrue("the declared type", PluginToolInvocations.renames(synthesize, call, answer(wav, "audio/wav")).isEmpty())
        assertTrue("a type without an extension", PluginToolInvocations.renames(synthesize, call, answer(wav, "audio/amr")).isEmpty())
        val given = PluginToolInvocations.prepare(synthesize, args("""{"text":"hi","outputPaths":{"audio":"a.wav"}}"""), PathAccess.WORKSPACE_ONLY, now)
        assertTrue(given.defaultOutputs.isEmpty())
        assertTrue("a path the caller gave", PluginToolInvocations.renames(synthesize, given, answer("a.wav", "audio/mp4")).isEmpty())
    }

    @Test
    fun `the registry lists plugin tools by grant and switches and explains the hidden ones`() = runBlocking {
        var permissions = ToolPermissions.DEFAULT
        var availability = PluginToolAvailability.NONE
        val registry = ToolRegistry(ToolCatalog.all, { permissions }, { availability }) { _, _, _ -> ToolResults.success(JsonObject(emptyMap())) }
        val server = McpServerFactory.create("1")
        registry.install(server)
        val defaults = ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name }

        assertFalse(registry.refresh())
        assertNull("nobody reported it", registry.disabledFailure("opencc_convert"))

        availability = PluginToolAvailability(AGENT + "plugin_tools.list", null, tools = report)
        assertFalse("without plugin_tools.invoke nothing joins", registry.refresh())
        assertEquals(ToolErrorCodes.CAPABILITY_DENIED, registry.disabledFailure("opencc_convert")?.code)

        val granted = AGENT + setOf("plugin_tools.list", "plugin_tools.invoke")
        availability = PluginToolAvailability(granted, null, tools = report)
        assertTrue(registry.refresh())
        assertEquals(defaults + "opencc_convert", registry.enabledNames)
        assertNull(registry.disabledFailure("opencc_convert"))
        assertTrue(registry.disabledFailure("quant_quantize")!!.message.contains("does not allow tools that write, delete or reach the network"))
        assertEquals(ToolErrorCodes.TOOL_DISABLED, registry.disabledFailure("fake_echo")?.code)
        assertFalse(registry.refresh())
        assertEquals("plugin tools never touch the SDK registration", ToolCatalog.all.map { it.name }.toSet(), server.tools.keys)

        permissions = ToolPermissions.DEFAULT.withPluginWrites("quant", true).withPluginGroup("fake", true).withPluginWrites("fake", true)
        assertTrue(registry.refresh())
        assertEquals(defaults + listOf("fake_echo", "opencc_convert", "quant_quantize"), registry.enabledNames)
        assertEquals(listOf("fake_echo", "opencc_convert", "quant_quantize"), registry.pluginTools.map { it.name })

        val described = report.copy(tools = report.tools.map { if (it.name == "opencc_convert") it.copy(description = "Now different.") else it })
        availability = PluginToolAvailability(granted, null, tools = described)
        assertTrue("a changed description is a change for clients", registry.refresh())

        val disabled = described.copy(
            tools = described.tools.filter { it.shortName != "quant" },
            providers = described.providers.map { if (it.shortName == "quant") it.copy(status = "disabled", detail = "disabled in the plugin center") else it },
        )
        availability = PluginToolAvailability(granted, null, tools = disabled)
        assertTrue(registry.refresh())
        assertEquals(defaults + listOf("fake_echo", "opencc_convert"), registry.enabledNames)
        assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, registry.disabledFailure("quant_quantize")?.code)

        availability = PluginToolAvailability(granted, null, tools = report)
        registry.refresh()
        assertTrue(registry.hasOutputSchema("opencc_convert"))
        assertFalse("its result carries the written outputs", registry.hasOutputSchema("quant_quantize"))
        assertTrue(registry.hasOutputSchema(ToolCatalog.DEVICE_INFO))
        assertTrue(registry.countsAsScreenshot("quant_quantize", buildJsonObject { put("image", "screen") }))
        assertFalse(registry.countsAsScreenshot("quant_quantize", buildJsonObject { put("image", "a.png") }))
        assertFalse(registry.countsAsScreenshot("opencc_convert", buildJsonObject { put("text", "screen") }))
    }

    private fun args(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private companion object {
        /** The methods of the grant-gated catalog rows, so that they stay listed as without a session. */
        val AGENT = setOf("agent.listScripts", "agent.readManifest", "agent.execRegistered")
    }
}
