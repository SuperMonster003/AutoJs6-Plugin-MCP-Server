package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeReply
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeRequest
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeTransport
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BrokerInfo
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostAvailability
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostBridgeClient
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginFamiliesTest {

    /** A host answer in the shape of `NodeBridgePluginFamilyStatus.report` (AutoJs6 6.8.0). */
    private val reportJson = """
        {"schema":"autojs6-bridge-app-plugin-capabilities-v1","hostVersionCode":5352,
         "pluginCenter":{"component":"org.autojs.autojs6/org.autojs.autojs.core.plugin.center.PluginCenterActivity"},
         "families":[
           {"id":"ocr","status":"available","methods":["ocr.detectTextBounds","ocr.recognize","ocr.recognizeText","accessibility.readScreenText"],
            "plugins":[{"packageName":"org.autojs.autojs6.plugin.rapid.ocr","versionName":"1.0.6","versionCode":41,"engine":"rapid-ocr","enabled":true,"authorized":true,"compatible":true,"requiresHostVersion":5200}]},
           {"id":"barcode","status":"not_installed","methods":["barcode.detect"],"plugins":[]},
           {"id":"opencv","status":"disabled","methods":["image.findImage"],"plugins":[{"packageName":"org.autojs.autojs6.plugin.opencv","enabled":false,"authorized":true,"compatible":true}],"detail":"disabled in the plugin center"},
           {"id":"docs","status":"incompatible","methods":["app.listDocs","app.readDoc"],"plugins":[],"detail":"needs host 5400"}
         ]}
    """.trimIndent()

    private val ocrSpec = ToolSpec(
        name = "ocr_test_probe",
        title = "OCR probe",
        description = "test row of a plugin-family group",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.OCR,
        bridge = BridgeMethod("ocr", "recognize"),
        permissions = listOf("ocr"),
        hostMethods = listOf(BridgeMethod("ocr", "recognize"), BridgeMethod("image", "readImage"), BridgeMethod("image", "recycle")),
    )

    @Test
    fun `the host report parses tolerantly`() {
        val report = PluginFamilyReport.parse(Json.parseToJsonElement(reportJson))
        assertNotNull(report)
        report!!
        assertEquals(5352L, report.hostVersionCode)
        assertEquals("org.autojs.autojs6/org.autojs.autojs.core.plugin.center.PluginCenterActivity", report.pluginCenterComponent)
        assertEquals(listOf("ocr", "barcode", "opencv", "docs"), report.families.keys.toList())
        assertTrue(report.isAvailable("ocr"))
        assertFalse(report.isAvailable("barcode"))
        assertFalse(report.isAvailable("pinyin"))
        val ocr = report.families.getValue("ocr")
        assertEquals(4, ocr.methods.size)
        assertEquals(1, ocr.plugins.size)
        assertEquals("org.autojs.autojs6.plugin.rapid.ocr", ocr.plugins[0].packageName)
        assertEquals(41L, ocr.plugins[0].versionCode)
        assertEquals(5200L, ocr.plugins[0].requiresHostVersion)
        assertTrue(ocr.plugins[0].enabled && ocr.plugins[0].authorized && ocr.plugins[0].compatible)
        assertEquals("disabled in the plugin center", report.families.getValue("opencv").detail)
        assertFalse(report.families.getValue("opencv").plugins[0].enabled)
        assertEquals("{ocr=available,barcode=not_installed,opencv=disabled,docs=incompatible}", report.describe())

        assertNull(PluginFamilyReport.parse(Json.parseToJsonElement("""{"schema":"something-else","families":[]}""")))
        assertNull(PluginFamilyReport.parse(Json.parseToJsonElement("""[1,2]""")))
        assertNull(PluginFamilyReport.parse(null))
        val empty = PluginFamilyReport.parse(Json.parseToJsonElement("""{"schema":"autojs6-bridge-app-plugin-capabilities-v1"}"""))
        assertEquals(emptyMap<String, PluginFamilyStatus>(), empty?.families)
    }

    @Test
    fun `availability needs a session, the grant, and an available family`() {
        val report = PluginFamilyReport.parse(Json.parseToJsonElement(reportJson))!!
        val granted = setOf("ocr.recognize", "image.readImage", "image.recycle", "app.pluginCapabilities")

        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, PluginToolAvailability.NONE.unavailableFailure(ocrSpec)?.code)
        assertNull("built-in rows are never affected", PluginToolAvailability.NONE.unavailableFailure(ToolCatalog.deviceInfo))

        assertTrue(PluginToolAvailability(granted, report).allows(ocrSpec))
        assertTrue("an empty grant list means the host did not say", PluginToolAvailability(emptySet(), report).allows(ocrSpec))

        val denied = PluginToolAvailability(granted - "image.recycle", report).unavailableFailure(ocrSpec)
        assertEquals(ToolErrorCodes.CAPABILITY_DENIED, denied?.code)
        assertEquals("image", denied?.module)
        assertEquals("recycle", denied?.method)

        val noReport = PluginToolAvailability(granted, null, hostDetail = "too old").unavailableFailure(ocrSpec)
        assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, noReport?.code)
        assertTrue(noReport!!.message, noReport.message.contains("not reported by AutoJs6") && noReport.message.endsWith(": too old"))

        val barcode = ocrSpec.copy(name = "barcode_test_probe", group = ToolGroup.BARCODE, bridge = BridgeMethod("barcode", "detect"), hostMethods = listOf(BridgeMethod("barcode", "detect")))
        val notInstalled = PluginToolAvailability(granted + "barcode.detect", report).unavailableFailure(barcode)
        assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, notInstalled?.code)
        assertEquals("tool barcode_test_probe needs the barcode plugin family, which is not installed", notInstalled?.message)

        val opencv = ocrSpec.copy(name = "opencv_test_probe", group = ToolGroup.OPENCV, bridge = BridgeMethod("image", "findImage"), hostMethods = listOf(BridgeMethod("image", "findImage")))
        val disabled = PluginToolAvailability(granted + "image.findImage", report).unavailableFailure(opencv)
        assertEquals("tool opencv_test_probe needs the opencv plugin family, which is installed but disabled in AutoJs6: disabled in the plugin center", disabled?.message)

        val pinyin = ocrSpec.copy(name = "pinyin_test_probe", group = ToolGroup.PINYIN, bridge = BridgeMethod("pinyin", "convert"), hostMethods = listOf(BridgeMethod("pinyin", "convert")))
        val unknown = PluginToolAvailability(granted + "pinyin.convert", report).unavailableFailure(pinyin)
        assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, unknown?.code)
        assertTrue(unknown!!.message, unknown.message.endsWith("AutoJs6 does not know the pinyin family"))
    }

    @Test
    fun `the probe caches, re-probes on invalidation and session change, and keeps a stale report while the host is away`() = runBlocking {
        var now = 1_000L
        val transport = FakeTransport(reportJson)
        var client: HostBridgeClient? = HostBridgeClient(transport, clock = { now })
        val probe = PluginFamilyProbe({ client }, clock = { now }, ttlMs = 5_000L)

        val first = probe.current()
        assertTrue(first.allows(ocrSpec))
        assertFalse(first.stale)
        assertEquals(1, probe.probeCount)
        now += 4_000L
        assertEquals(first, probe.current())
        assertEquals("fresh answers are reused", 1, probe.probeCount)
        now += 2_000L
        probe.current()
        assertEquals("the answer expires after the ttl", 2, probe.probeCount)
        probe.invalidate()
        probe.current()
        assertEquals("invalidate drops the cache", 3, probe.probeCount)
        assertEquals(listOf("app.pluginCapabilities"), transport.requests.map { it.key }.distinct())
        assertEquals(listOf("app.query"), transport.requests.first().permissions)

        // The host dies: the last report stands in, marked stale, until a new session arrives.
        transport.deathListener?.invoke()
        assertEquals(HostAvailability.DEGRADED, client!!.availability)
        probe.invalidate()
        val stale = probe.current()
        assertTrue(stale.stale)
        assertTrue(stale.allows(ocrSpec))
        assertEquals(3, probe.probeCount)

        // A new session (new client) starts over; without the method in its grant the rows stay hidden.
        val oldTransport = FakeTransport(reportJson, BrokerInfo(methods = setOf("device.info", "ocr.recognize", "image.readImage", "image.recycle")))
        client = HostBridgeClient(oldTransport, clock = { now })
        val old = probe.current()
        assertEquals(3, probe.probeCount)
        assertNull(old.report)
        assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, old.unavailableFailure(ocrSpec)?.code)
        assertTrue(old.unavailableFailure(ocrSpec)!!.message.contains("does not report plugin capabilities"))
        assertEquals("session=open report=none (this AutoJs6 build does not report plugin capabilities (app.pluginCapabilities); update AutoJs6)", old.describe())

        // No session at all: nothing is listed, and the state says so.
        client = null
        assertEquals(PluginToolAvailability.NONE, probe.current())
        assertEquals("session=none", probe.last.describe())
    }

    @Test
    fun `a host that answers with a failure other than its own absence hides the rows`() = runBlocking {
        val transport = FakeTransport(reportJson)
        transport.handler = { request, reply ->
            reply(BridgeReply("""{"id":"${request.id}","ok":false,"error":{"name":"Error","message":"unknown method","category":"invalid-request"}}""", false, "unknown method"))
        }
        val client = HostBridgeClient(transport)
        val probe = PluginFamilyProbe({ client })
        val state = probe.current()
        assertNull(state.report)
        assertFalse(state.stale)
        assertEquals(ToolErrorCodes.PLUGIN_UNAVAILABLE, state.unavailableFailure(ocrSpec)?.code)
        assertTrue(state.describe(), state.describe().startsWith("session=open report=none (INVALID_ARGUMENTS"))
    }

    private class FakeTransport(
        reportJson: String,
        override val brokerInfo: BrokerInfo = BrokerInfo(
            brokerId = "broker-1",
            methods = setOf("device.info", "app.pluginCapabilities", "ocr.recognize", "image.readImage", "image.recycle", "barcode.detect", "image.findImage", "pinyin.convert"),
            maxConcurrentCalls = 2,
            defaultTimeoutMs = 1_000L,
            maxTimeoutMs = 5_000L,
        ),
    ) : BridgeTransport {

        val requests = mutableListOf<BridgeRequest>()
        var deathListener: (() -> Unit)? = null

        var handler: (BridgeRequest, (BridgeReply) -> Unit) -> Unit = { request, reply ->
            reply(BridgeReply("""{"id":"${request.id}","ok":true,"result":$reportJson}""", true, null))
        }

        override fun dispatch(request: BridgeRequest, clientName: String?, onReply: (BridgeReply) -> Unit) {
            requests += request
            handler(request, onReply)
        }

        override fun linkToDeath(onDeath: () -> Unit): Boolean {
            deathListener = onDeath
            return true
        }

        override fun unlinkToDeath() {
            deathListener = null
        }
    }
}
