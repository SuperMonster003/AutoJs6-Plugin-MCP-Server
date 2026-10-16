package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginToolSpecsTest {

    @Test
    fun `the host list parses and skips incomplete entries`() {
        val report = PluginToolsReport.parse(Json.parseToJsonElement(LIST))!!
        assertEquals(listOf("opencc_convert", "quant_quantize", "fake_echo"), report.tools.map { it.name })
        val opencc = report.tools.first()
        assertEquals("opencc", opencc.shortName)
        assertEquals("official", opencc.trust)
        assertTrue(opencc.readOnly)
        assertEquals(10_000L, opencc.timeoutMs)
        assertEquals(4, opencc.maxConcurrency)
        assertEquals(listOf("image"), report.tools[1].imageParameters)
        assertEquals(listOf(PluginToolOutput("image", "image/png", "the quantized PNG")), report.tools[1].outputs)
        assertTrue(report.tools[2].userGranted)
        assertEquals(3, report.providers.size)
        assertEquals(listOf("deadbeef: inputSchema is not closed"), report.providers[2].diagnostics)
        assertTrue(report.providers[2].toolsHidden)
        assertEquals("{opencc=available,quant=available,fake=available}", report.describe())
        assertNull(PluginToolsReport.parse(Json.parseToJsonElement("""{"schema":"other","tools":[]}""")))
        assertNull(PluginToolsReport.parse(JsonPrimitive("x")))
    }

    @Test
    fun `tools keep their name and schema and gain notes, hints and the output paths`() {
        val report = PluginToolsReport.parse(Json.parseToJsonElement(LIST))!!
        val opencc = report.tools[0]
        val sdk = PluginToolSpecs.toSdkTool(opencc)
        assertEquals("opencc_convert", sdk.name)
        assertEquals("Convert Chinese text (OpenCC)", sdk.title)
        assertEquals("Converts Chinese text. (AutoJs6 plugin io.github.example.cc.)", sdk.description)
        assertEquals(listOf("text"), sdk.inputSchema.required)
        assertEquals(setOf("text", "config"), sdk.inputSchema.properties!!.keys)
        assertEquals(listOf("text", "config"), sdk.outputSchema!!.required)
        assertEquals(true, sdk.annotations!!.readOnlyHint)
        assertEquals(false, sdk.annotations!!.openWorldHint)
        assertFalse(PluginToolSpecs.takesOutputPaths(opencc))

        val quant = report.tools[1]
        assertTrue(PluginToolSpecs.takesOutputPaths(quant))
        val schema = PluginToolSpecs.inputSchema(quant)
        val properties = schema["properties"]!!.jsonObject
        assertEquals(
            "The image to quantize. AutoJs6 accepts \"screen\" (a screenshot taken now) or an image path; relative paths are in the AutoJs6 working directory.",
            properties["image"]!!.jsonObject["description"]!!.jsonPrimitive.content,
        )
        assertEquals("image", properties["image"]!!.jsonObject["x-autojs6-type"]!!.jsonPrimitive.content)
        val outputPaths = properties[PluginToolSpecs.OUTPUT_PATHS]!!.jsonObject
        assertEquals(setOf("image"), outputPaths["properties"]!!.jsonObject.keys)
        assertEquals(JsonPrimitive(false), outputPaths["additionalProperties"])
        assertTrue(PluginToolSpecs.description(quant).endsWith("Writes image (image/png) into the AutoJs6 working directory and returns the paths."))
        assertNull("a tool with outputs answers with the paths too", PluginToolSpecs.outputSchema(quant))
        val hints = PluginToolSpecs.hints(quant)
        assertFalse(hints.readOnly)
        assertFalse(hints.destructive)
        assertTrue("an image parameter can read the screen", hints.openWorld)

        val fake = report.tools[2]
        assertTrue(PluginToolSpecs.description(fake).contains("a third-party plugin the user allowed"))
        assertTrue(PluginToolSpecs.hints(fake).destructive)
        assertNotEquals(PluginToolSpecs.signature(opencc), PluginToolSpecs.signature(opencc.copy(description = "changed")))
    }

    @Test
    fun `plugin groups follow the trust by default and round-trip only explicit choices`() {
        val report = PluginToolsReport.parse(Json.parseToJsonElement(LIST))!!
        val (opencc, quant, fake) = report.tools
        val defaults = ToolPermissions.DEFAULT
        assertTrue(defaults.allows(opencc))
        assertFalse("a writing tool waits for its sub-switch", defaults.allows(quant))
        assertFalse("user-granted plugins start off", defaults.allows(fake))

        val chosen = defaults.withPluginWrites("quant", true).withPluginGroup("fake", true).withPluginGroup("opencc", false)
        assertFalse(chosen.allows(opencc))
        assertTrue(chosen.allows(quant))
        assertFalse("the destructive tool still needs the sub-switch", chosen.allows(fake))
        assertTrue(chosen.withPluginWrites("fake", true).allows(fake))

        val encoded = chosen.encode()
        assertTrue(encoded, encoded.endsWith(""","pluginGroups":{"fake":{"enabled":true},"opencc":{"enabled":false},"quant":{"writes":true}}}"""))
        assertEquals(chosen.pluginGroups, ToolPermissions.decode(encoded).pluginGroups)
        assertFalse("no plugin choices, no key", defaults.encode().contains("pluginGroups"))
        val tolerant = ToolPermissions.decode("""{"format":1,"groups":{},"pluginGroups":{"a":{"enabled":"yes"},"b":3,"c":{"writes":true}}}""")
        assertEquals(mapOf("c" to PluginGroupSwitch(writes = true)), tolerant.pluginGroups)
    }

    private operator fun <T> List<T>.component1() = this[0]

    companion object {

        val LIST = """
            {"schema":"autojs6-bridge-plugin-tools-list-v1","tools":[
              {"name":"opencc_convert","tool":"convert","shortName":"opencc","provider":"io.github.example.cc","trust":"official",
               "title":"Convert Chinese text (OpenCC)","description":"Converts Chinese text.",
               "inputSchema":{"type":"object","properties":{"text":{"type":"string","maxLength":16384},"config":{"type":"string","enum":["s2t","t2s"],"default":"s2t"}},"required":["text"],"additionalProperties":false},
               "outputSchema":{"type":"object","properties":{"text":{"type":"string"},"config":{"type":"string"}},"required":["text","config"],"additionalProperties":false},
               "risk":"readonly","timeoutMs":10000,"maxConcurrency":4,"outputs":[],"imageParameters":[],"fileParameters":[]},
              {"name":"quant_quantize","tool":"quantize","shortName":"quant","provider":"io.github.example.quant","trust":"official",
               "description":"Quantizes an image.",
               "inputSchema":{"type":"object","properties":{"image":{"type":"string","description":"The image to quantize.","x-autojs6-type":"image"},"colors":{"type":"integer","minimum":2,"maximum":256}},"required":["image"],"additionalProperties":false},
               "risk":"write","timeoutMs":60000,"maxConcurrency":1,"outputs":[{"name":"image","mimeType":"image/png","description":"the quantized PNG"}],"imageParameters":["image"],"fileParameters":[]},
              {"name":"fake_echo","tool":"echo","shortName":"fake","provider":"org.example.fake","trust":"user_granted",
               "description":"Echoes.","inputSchema":{"type":"object","properties":{},"additionalProperties":false},
               "risk":"destructive","timeoutMs":5000},
              {"name":"broken","tool":"broken","shortName":"x","provider":"p","trust":"official","description":"no schema","risk":"readonly","timeoutMs":1}
            ],"providers":[
              {"packageName":"io.github.example.cc","versionCode":145,"trust":"official","status":"available","shortName":"opencc","tools":1},
              {"packageName":"io.github.example.quant","versionCode":3,"trust":"official","status":"available","shortName":"quant","tools":1},
              {"packageName":"org.example.fake","trust":"user_granted","status":"available","shortName":"fake","tools":1,"diagnostics":["deadbeef: inputSchema is not closed"],"toolsHidden":true}
            ]}
        """.trimIndent()
    }
}
