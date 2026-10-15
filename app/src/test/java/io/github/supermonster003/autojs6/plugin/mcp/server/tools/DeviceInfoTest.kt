package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoTest {

    private val report = PluginFamilyReport.parse(Json.parseToJsonElement(
        """{"schema":"autojs6-bridge-app-plugin-capabilities-v1","hostVersionCode":5352,"pluginCenter":{"component":"org.autojs.autojs6/.PluginCenterActivity"},
           "families":[
             {"id":"ocr","status":"available","methods":["ocr.recognize"],"plugins":[{"packageName":"p.rapid","versionName":"1.0.6","versionCode":36,"enabled":true,"authorized":true,"compatible":true}]},
             {"id":"barcode","status":"not_installed","methods":["barcode.detectAll"],"plugins":[],"detail":"no barcode plugin"},
             {"id":"opencv","status":"disabled","methods":["image.findColor"],"plugins":[{"packageName":"p.opencv","enabled":false,"compatible":true}]},
             {"id":"pinyin4j","status":"incompatible","methods":["pinyin4j.readings"],"plugins":[{"packageName":"p.pinyin4j","compatible":false,"requiresHostVersion":5500}]},
             {"id":"mediainfo","status":"available","methods":["mediainfo.read"],"plugins":[]},
             {"id":"docs","status":"available","methods":["app.readDoc"],"plugins":[]},
             {"id":"future","status":"not_installed","methods":[],"plugins":[]}
           ]}""",
    ))!!

    private fun families(section: JsonObject): Map<String, JsonObject> =
        section["families"]!!.jsonArray.associate { it.jsonObject["id"]!!.jsonPrimitive.content to it.jsonObject }

    @Test
    fun `every reported family gets its tools, its group switch and a hint for what the user can do`() {
        val section = DeviceInfo.section(report, ToolPermissions.DEFAULT.with(ToolGroup.MEDIAINFO, false))
        val families = families(section)
        assertEquals(listOf("ocr", "barcode", "opencv", "pinyin4j", "mediainfo", "docs", "future"), families.keys.toList())
        assertEquals(DeviceInfo.HINT_PLUGIN_CENTER, section["hint"]!!.jsonPrimitive.content)

        val ocr = families.getValue("ocr")
        assertEquals("available", ocr["status"]!!.jsonPrimitive.content)
        assertEquals("ML Kit OCR, Paddle OCR or Rapid OCR", ocr["name"]!!.jsonPrimitive.content)
        assertEquals(listOf("ocr_recognize"), ocr["tools"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertTrue(ocr["toolGroupEnabled"]!!.jsonPrimitive.boolean)
        assertEquals("""[{"packageName":"p.rapid","versionName":"1.0.6","versionCode":36,"enabled":true,"compatible":true}]""", ocr["plugins"].toString())
        assertNull(ocr["hint"])

        val barcode = families.getValue("barcode")
        assertEquals("install the ML Kit Barcode plugin from the AutoJs6 plugin center", barcode["hint"]!!.jsonPrimitive.content)
        assertEquals("no barcode plugin", barcode["detail"]!!.jsonPrimitive.content)
        assertEquals(listOf("barcode_detect"), barcode["tools"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("enable the OpenCV plugin in the AutoJs6 plugin center", families.getValue("opencv")["hint"]!!.jsonPrimitive.content)
        assertEquals(3, families.getValue("opencv")["tools"]!!.jsonArray.size)
        assertTrue(families.getValue("pinyin4j")["hint"]!!.jsonPrimitive.content.startsWith("the installed Pinyin4j plugin needs AutoJs6 build 5500 or later"))

        val media = families.getValue("mediainfo")
        assertFalse(media["toolGroupEnabled"]!!.jsonPrimitive.boolean)
        assertEquals("switch on the mediainfo group in the MCP Server settings on the phone to list its tools", media["hint"]!!.jsonPrimitive.content)

        val docs = families.getValue("docs")
        assertEquals("Offline Docs", docs["name"]!!.jsonPrimitive.content)
        assertEquals(JsonArray(emptyList()), docs["tools"])
        assertEquals("""["autojs6://docs/{+path}"]""", docs["resources"].toString())
        assertNull(docs["toolGroup"])

        val future = families.getValue("future")
        assertEquals("future", future["name"]!!.jsonPrimitive.content)
        assertEquals("install the future plugin from the AutoJs6 plugin center", future["hint"]!!.jsonPrimitive.content)
    }

    @Test
    fun `device info adds the plugins section only when the host reports its families`() {
        val device = Json.parseToJsonElement("""{"schema":"autojs6-bridge-device-info-v1","screenOn":true}""")
        val capabilities = Json.parseToJsonElement("""{"schema":"autojs6-bridge-app-plugin-capabilities-v1","families":[{"id":"ocr","status":"available"}]}""")
        val calls = mutableListOf<String>()
        fun caller(status: BridgeOutcome) = BridgeCaller { module, method, _, timeoutMs, permissions ->
            calls += "$module.$method:$timeoutMs:${permissions.joinToString("+")}"
            if (method == "info") BridgeOutcome.Ok(device, 1L) else status
        }

        val withPlugins = runBlocking { DeviceInfo.read(caller(BridgeOutcome.Ok(capabilities, 1L)), ToolPermissions.DEFAULT, 10_000L) }
        assertEquals(listOf("device.info:10000:device", "app.pluginCapabilities:5000:app.query"), calls)
        assertEquals("autojs6-bridge-device-info-v1", withPlugins["schema"]!!.jsonPrimitive.content)
        assertEquals("ocr", withPlugins["plugins"]!!.jsonObject["families"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content)

        val denied = BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = "not granted", code = "x", category = BridgeError.CATEGORY_CAPABILITY_DENIED, module = "app", method = "pluginCapabilities")))
        assertEquals(device, runBlocking { DeviceInfo.read(caller(denied), ToolPermissions.DEFAULT, 10_000L) })
        assertEquals(device, runBlocking { DeviceInfo.read(caller(BridgeOutcome.Ok(JsonPrimitive("old host"), 1L)), ToolPermissions.DEFAULT, 10_000L) })
    }

    @Test
    fun `the device_info row runs as a flow and its output schema describes plugins`() {
        val flow = DeviceTools().planFor(ToolCatalog.deviceInfo, JsonObject(emptyMap()))
        assertTrue(flow != null)
        val properties = ToolCatalog.deviceInfo.outputSchema!!["properties"]!!.jsonObject
        assertTrue(properties.containsKey("plugins"))
        assertFalse(ToolCatalog.deviceInfo.outputSchema!!.containsKey("required"))
    }
}
