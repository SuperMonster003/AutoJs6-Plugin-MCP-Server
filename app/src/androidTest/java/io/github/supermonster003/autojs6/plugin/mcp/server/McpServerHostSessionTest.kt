package io.github.supermonster003.autojs6.plugin.mcp.server

import android.content.Context
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.mcp.server.host.McpServerRuntime
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpErrors
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpHttpServer
import io.github.supermonster003.autojs6.plugin.mcp.server.store.PairedClientStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.store.TokenStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolGroup
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissionStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissions
import org.autojs.plugin.mcp.server.api.IMcpHostCapabilityBroker
import org.autojs.plugin.mcp.server.api.IMcpHostCapabilityCallback
import org.autojs.plugin.mcp.server.api.IMcpServerCallback
import org.autojs.plugin.mcp.server.api.McpServerContract
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

/**
 * On-device evidence for roadmap P2.3: drives [McpServerRuntime] the way the host does through
 * `IMcpServerPlugin.openServer` and the session Binder, with a fake capability broker standing
 * in for AutoJs6, and then exercises the resulting listener like a PC client.
 *
 * The sequence covers: `openServer` -> status `running` with the host attached and the endpoint
 * list -> `tools/list` shows the catalog -> pairing -> `device_info` and `script_run` reach the
 * broker with the documented envelope -> host failures map onto plugin codes -> a switched-off
 * group hides its tools and answers `TOOL_DISABLED` -> `stop` / `close` end the listener; plus
 * `already_open`, `invalid_config`, and tools without a host session answering `HOST_UNAVAILABLE`.
 * Roadmap P3.2 adds the `ui` group: `ui_dump` -> compact text and `#n` references, `ui_click` by
 * reference (relocation, exact selector), a stale reference, `ui_set_text`, `ui_scroll`,
 * `ui_press_key` on the `keys` module, `ui_current_window`, and the `ui_gesture` switch (D22).
 */
@RunWith(AndroidJUnit4::class)
class McpServerHostSessionTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var runtime: McpServerRuntime
    private lateinit var token: String

    @Before
    fun setUp() {
        PairedClientStore(context).clear()
        ToolPermissionStore(context).reset()
        runtime = McpServerRuntime.get(context)
        runtime.stop(McpServerContract.REASON_USER_REQUEST)
        awaitPort(open = false)
        token = TokenStore(context).current()
    }

    @After
    fun tearDown() {
        runtime.hostSession?.close()
        runtime.stop(McpServerContract.REASON_USER_REQUEST)
        awaitPort(open = false)
        PairedClientStore(context).clear()
        ToolPermissionStore(context).reset()
    }

    @Test
    fun openServerServesTheCatalogThroughTheHostBroker() {
        val broker = FakeBroker()
        val callback = RecordingCallback()
        val session = runtime.openSession(configBundle(PORT), broker, callback, Process.myUid())

        val running = awaitStatus(session) { it.getString(McpServerContract.KEY_STATUS_STATE) == McpServerContract.STATE_RUNNING }
        assertEquals(listOf(McpHttpServer.endpointUrl(PORT)), running.getStringArray(McpServerContract.KEY_STATUS_ENDPOINTS)?.toList())
        assertEquals(PORT, running.getInt(McpServerContract.KEY_STATUS_PORT))
        assertEquals(McpServerContract.BIND_SCOPE_LOOPBACK, running.getString(McpServerContract.KEY_STATUS_BIND_SCOPE))
        assertEquals(McpServerContract.PROTOCOL_MODE_STATEFUL, running.getString(McpServerContract.KEY_STATUS_PROTOCOL_MODE))
        assertEquals(McpServerContract.CONTRACT_VERSION, running.getInt(McpServerContract.KEY_CONTRACT_VERSION))
        assertTrue(running.getBoolean(McpServerContract.KEY_STATUS_HOST_AVAILABLE))
        assertTrue(running.getLong(McpServerContract.KEY_STATUS_STARTED_AT) > 0L)
        assertTrue("the host callback must have seen a running status", callback.statuses.any { it.getString(McpServerContract.KEY_STATUS_STATE) == McpServerContract.STATE_RUNNING })
        awaitCondition("host probe") { broker.requests.any { it.getString("method") == "info" } }
        Log.i(TAG, "openServer -> running: ${running.getStringArray(McpServerContract.KEY_STATUS_ENDPOINTS)?.toList()}, probe ${broker.requests.first().getString("module")}.${broker.requests.first().getString("method")}")

        val init = post(initializeRequest(), sessionId = null)
        assertEquals(200, init.status)
        val sessionId = init.header("mcp-session-id")
        post(initializedNotification(), sessionId)
        val names = toolNames(post(toolsListRequest(), sessionId))
        assertEquals(ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name }, names)
        val listed = post(toolsListRequest(), sessionId).json().getJSONObject("result").getJSONArray("tools")
        val scriptRun = (0 until listed.length()).map { listed.getJSONObject(it) }.first { it.getString("name") == ToolCatalog.SCRIPT_RUN }
        assertEquals("object", scriptRun.getJSONObject("inputSchema").getString("type"))
        assertEquals("source", scriptRun.getJSONObject("inputSchema").getJSONArray("required").getString(0))
        assertTrue(scriptRun.getJSONObject("inputSchema").getJSONObject("properties").has("maxConsoleLines"))
        assertTrue(scriptRun.getJSONObject("annotations").getBoolean("openWorldHint"))

        val held = post(toolsCallRequest(ToolCatalog.DEVICE_INFO), sessionId)
        val error = held.json().getJSONObject("error")
        assertEquals(McpErrors.PAIRING_REQUIRED, error.getInt("code"))
        val fingerprint = error.getJSONObject("data").getString("fingerprint")
        assertTrue(runtime.server.pairingGate!!.approve(fingerprint))
        awaitCondition("client_paired event") { callback.events.any { it.getString(McpServerContract.KEY_EVENT_TYPE) == McpServerContract.EVENT_CLIENT_PAIRED } }
        assertTrue(callback.events.any { it.getString(McpServerContract.KEY_EVENT_TYPE) == McpServerContract.EVENT_PAIRING_REQUESTED && it.getString(McpServerContract.KEY_EVENT_CLIENT_NAME) == CLIENT_NAME })

        val info = post(toolsCallRequest(ToolCatalog.DEVICE_INFO), sessionId).json().getJSONObject("result")
        assertFalse(info.optBoolean("isError"))
        val infoText = JSONObject(info.getJSONArray("content").getJSONObject(0).getString("text"))
        assertEquals("autojs6-bridge-device-info-v1", infoText.getString("schema"))
        assertEquals("autojs6-bridge-device-info-v1", info.getJSONObject("structuredContent").getString("schema"))
        val infoRequest = broker.requests.last()
        assertEquals("device", infoRequest.getString("module"))
        assertEquals("info", infoRequest.getString("method"))
        assertEquals(listOf("device"), infoRequest.getJSONArray("permissions").toStringList())
        assertEquals(CLIENT_NAME, broker.clientNames.last())
        Log.i(TAG, "device_info through the broker: ${infoText.getJSONObject("host")}")

        val run = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"toast('hi')","name":"demo","timeoutMs":5000,"maxConsoleLines":1}"""), sessionId)
            .json().getJSONObject("result")
        assertFalse(run.toString(), run.optBoolean("isError"))
        val runResult = run.getJSONObject("structuredContent")
        assertEquals("finished", runResult.getString("status"))
        assertEquals(3, runResult.getInt("executionId"))
        assertEquals("demo", runResult.getString("name"))
        assertEquals(12L, runResult.getLong("durationMs"))
        assertEquals(1, runResult.getJSONArray("console").length())
        assertEquals(2, runResult.getInt("consoleCount"))
        assertTrue(runResult.getBoolean("consoleTruncated"))
        assertFalse(runResult.has("hint"))
        val runRequest = broker.requests.last()
        assertEquals("engines", runRequest.getString("module"))
        assertEquals("execScript", runRequest.getString("method"))
        val args = runRequest.getJSONArray("args")
        assertEquals("demo", args.getString(0))
        assertEquals("toast('hi')", args.getString(1))
        assertEquals(5000L, args.getJSONObject(2).getLong("timeoutMs"))
        assertEquals(5000L, args.getJSONObject(2).getLong("waitMs"))
        assertTrue(args.getJSONObject(2).getBoolean("captureConsole"))
        assertEquals(15_000L, runRequest.getLong("timeoutMs"))
        assertEquals(listOf("engines", "engines.exec"), runRequest.getJSONArray("permissions").toStringList())
        Log.i(TAG, "script_run envelope: $runRequest")

        // A slow run with a progress token: the 2 s heartbeat travels on the request's own response
        // stream (related request id) and carries the newest console line of the fake host.
        val progress = post(
            """{"jsonrpc":"2.0","id":31,"method":"tools/call","params":{"name":"${ToolCatalog.SCRIPT_RUN}","arguments":{"source":"slow script","timeoutMs":5000},"_meta":{"progressToken":"hb1"}}}""",
            sessionId,
        )
        val heartbeats = progress.events.map { JSONObject(it) }.filter { it.optString("method") == "notifications/progress" }
        Log.i(TAG, "slow script_run: ${progress.events.size} stream events, ${heartbeats.size} heartbeats")
        assertTrue("a slow script_run sends its heartbeat on the request stream: ${progress.events}", heartbeats.isNotEmpty())
        val heartbeat = heartbeats.first().getJSONObject("params")
        assertEquals("hb1", heartbeat.getString("progressToken"))
        val heartbeatMessage = heartbeat.getString("message")
        assertTrue(heartbeatMessage, heartbeatMessage.startsWith("waiting for AutoJs6 (") && heartbeatMessage.endsWith("; last output: e1"))
        assertTrue(heartbeat.getDouble("total") >= 5000.0)
        assertEquals("finished", progress.json().getJSONObject("result").getJSONObject("structuredContent").getString("status"))

        val failed = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"fail please"}"""), sessionId).json().getJSONObject("result")
        assertTrue(failed.getBoolean("isError"))
        val failedText = failed.getJSONArray("content").getJSONObject(0).getString("text")
        assertTrue(failedText, failedText.startsWith("HOST_ERROR: ScriptError: fail please"))
        assertEquals("HOST_ERROR", failed.getJSONObject("structuredContent").getJSONObject("error").getString("code"))

        val invalid = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"x","timeoutMs":5}"""), sessionId).json().getJSONObject("result")
        assertTrue(invalid.getBoolean("isError"))
        assertTrue(invalid.getJSONArray("content").getJSONObject(0).getString("text").startsWith("INVALID_ARGUMENTS: script_run: timeoutMs must be at least 1000"))

        val thrown = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"throw new Error('boom')","timeoutMs":5000}"""), sessionId).json().getJSONObject("result")
        assertFalse(thrown.toString(), thrown.optBoolean("isError"))
        val thrownResult = thrown.getJSONObject("structuredContent")
        assertEquals("error", thrownResult.getString("status"))
        assertEquals(4, thrownResult.getInt("executionId"))
        assertEquals(2, thrownResult.getJSONObject("exception").getInt("line"))
        assertTrue(thrownResult.getJSONObject("exception").getString("message").startsWith("Error: boom"))

        val runFile = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN_FILE, """{"path":"demo/run.js","timeoutMs":5000,"waitForCompletion":false}"""), sessionId).json().getJSONObject("result")
        assertFalse(runFile.toString(), runFile.optBoolean("isError"))
        val runFileResult = runFile.getJSONObject("structuredContent")
        assertEquals("running", runFileResult.getString("status"))
        assertEquals(7, runFileResult.getInt("executionId"))
        assertFalse(runFileResult.getBoolean("waited"))
        assertTrue(runFileResult.getString("hint").contains("script_stop with executionId 7"))
        val fileRequest = broker.requests.last()
        assertEquals("execScriptFile", fileRequest.getString("method"))
        assertEquals("demo/run.js", fileRequest.getJSONArray("args").getString(0))
        assertEquals(0L, fileRequest.getJSONArray("args").getJSONObject(1).getLong("waitMs"))

        val scripts = post(toolsCallRequest(ToolCatalog.SCRIPT_LIST), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertEquals(1, scripts.getInt("count"))
        assertEquals(7, scripts.getJSONArray("executions").getJSONObject(0).getInt("executionId"))
        assertEquals("run.js", scripts.getJSONArray("executions").getJSONObject(0).getString("name"))
        assertEquals("list", broker.requests.last().getString("method"))
        assertEquals(0, broker.requests.last().getJSONArray("args").length())

        val tail = post(toolsCallRequest(ToolCatalog.CONSOLE_TAIL, """{"lines":2,"level":"warn"}"""), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertEquals(2, tail.getInt("count"))
        assertEquals(2, tail.getJSONArray("entries").length())
        assertEquals(9, tail.getInt("nextSinceId"))
        assertFalse(tail.has("schema"))
        val tailRequest = broker.requests.last()
        assertEquals("console", tailRequest.getString("module"))
        assertEquals("tail", tailRequest.getString("method"))
        assertEquals(2, tailRequest.getJSONArray("args").getJSONObject(0).getInt("lines"))
        assertEquals("warn", tailRequest.getJSONArray("args").getJSONObject(0).getString("level"))
        assertEquals(listOf("console"), tailRequest.getJSONArray("permissions").toStringList())

        val stopped7 = post(toolsCallRequest(ToolCatalog.SCRIPT_STOP, """{"executionId":7}"""), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertEquals(7, stopped7.getInt("executionId"))
        assertTrue(stopped7.getBoolean("stopped"))
        assertEquals("finished", stopped7.getString("state"))
        assertEquals(7, broker.requests.last().getJSONArray("args").getInt(0))

        val stoppedAll = post(toolsCallRequest(ToolCatalog.SCRIPT_STOP_ALL), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertEquals(2, stoppedAll.getInt("stopped"))
        assertEquals("host", broker.requests.last().getJSONArray("args").getJSONObject(0).getString("scope"))
        Log.i(TAG, "script tools through the broker: run (finished / error), run_file, list, tail, stop, stop_all answered")

        awaitCondition("tool_call events") {
            callback.events.count { it.getString(McpServerContract.KEY_EVENT_TYPE) == McpServerContract.EVENT_TOOL_CALL && it.getString(McpServerContract.KEY_EVENT_CLIENT_NAME) == CLIENT_NAME } >= 9
        }
        assertTrue(callback.events.any { it.getString(McpServerContract.KEY_EVENT_TOOL_NAME) == ToolCatalog.SCRIPT_RUN })

        ToolPermissionStore(context).save(ToolPermissions.DEFAULT.with(ToolGroup.SCRIPT, false))
        assertEquals(ToolCatalog.enabled(ToolPermissions.DEFAULT.with(ToolGroup.SCRIPT, false)).map { it.name }, toolNames(post(toolsListRequest(), sessionId)))
        val disabled = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"x"}"""), sessionId).json().getJSONObject("result")
        assertTrue(disabled.getBoolean("isError"))
        assertTrue(disabled.getJSONArray("content").getJSONObject(0).getString("text").startsWith("TOOL_DISABLED: tool script_run is switched off by the script group"))
        ToolPermissionStore(context).save(ToolPermissions.DEFAULT)
        assertEquals(ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name }, toolNames(post(toolsListRequest(), sessionId)))

        session.stop(reasonBundle(McpServerContract.REASON_USER_REQUEST))
        val stopped = session.status
        assertEquals(McpServerContract.STATE_STOPPED, stopped.getString(McpServerContract.KEY_STATUS_STATE))
        assertEquals(0, stopped.getInt(McpServerContract.KEY_STATUS_PORT))
        assertTrue(stopped.getBoolean(McpServerContract.KEY_STATUS_HOST_AVAILABLE))
        awaitPort(open = false)
        session.close()
        assertNull(runtime.bridge)
        assertFalse(runtime.hostAvailable)
        Log.i(TAG, "session stopped and closed; ${broker.requests.size} broker requests, ${callback.events.size} events")
    }

    @Test
    fun uiToolsReachTheBrokerWithNodeReferencesAndTheGestureSwitch() {
        val broker = FakeBroker()
        val session = runtime.openSession(configBundle(PORT), broker, RecordingCallback(), Process.myUid())
        awaitStatus(session) { it.getString(McpServerContract.KEY_STATUS_STATE) == McpServerContract.STATE_RUNNING }
        val init = post(initializeRequest(), sessionId = null)
        val sessionId = init.header("mcp-session-id")
        post(initializedNotification(), sessionId)
        val held = post(toolsCallRequest(ToolCatalog.UI_DUMP), sessionId)
        assertTrue(runtime.server.pairingGate!!.approve(held.json().getJSONObject("error").getJSONObject("data").getString("fingerprint")))

        // ui_dump: the host JSON nodes become the compact text and the #n references of snapshot s1.
        val dump = post(toolsCallRequest(ToolCatalog.UI_DUMP, """{"maxNodes":50}"""), sessionId).json().getJSONObject("result")
        assertFalse(dump.toString(), dump.optBoolean("isError"))
        val dumpText = dump.getJSONArray("content").getJSONObject(0).getString("text")
        assertEquals("window: com.android.settings/.Settings  bounds=[0,0][1080,2400]  nodes=3", dumpText.lines()[0])
        assertEquals("#n2  RecyclerView scrollable id=list [0,200][1080,2400]", dumpText.lines()[2])
        assertEquals("#n3   TextView clickable \"Wi-Fi\" c=(540,250)", dumpText.lines()[3])
        val dumpResult = dump.getJSONObject("structuredContent")
        assertEquals("s1", dumpResult.getString("snapshotId"))
        assertEquals(3, dumpResult.getInt("nodeCount"))
        assertEquals("com.android.settings", dumpResult.getJSONObject("window").getString("packageName"))
        val dumpRequest = broker.requests.last()
        assertEquals("accessibility", dumpRequest.getString("module"))
        assertEquals("dump", dumpRequest.getString("method"))
        assertEquals("json", dumpRequest.getJSONArray("args").getJSONObject(0).getString("format"))
        assertEquals(50, dumpRequest.getJSONArray("args").getJSONObject(0).getInt("maxNodes"))
        assertEquals(listOf("accessibility"), dumpRequest.getJSONArray("permissions").toStringList())
        assertEquals(20_000L, dumpRequest.getLong("timeoutMs"))
        Log.i(TAG, "ui_dump through the broker:\n$dumpText")

        // ui_click by nodeRef: relocation by fingerprint (the node drifted 10 px), then the exact selector.
        val click = post(toolsCallRequest(ToolCatalog.UI_CLICK, """{"nodeRef":"#n3"}"""), sessionId).json().getJSONObject("result")
        assertFalse(click.toString(), click.optBoolean("isError"))
        val clickResult = click.getJSONObject("structuredContent")
        assertEquals("click", clickResult.getString("action"))
        assertEquals("nodeRef", clickResult.getString("via"))
        assertEquals("#n3", clickResult.getJSONObject("target").getString("ref"))
        assertEquals(210, clickResult.getJSONObject("target").getJSONObject("bounds").getInt("top"))
        val relocation = broker.requests[broker.requests.size - 2]
        assertEquals("findAll", relocation.getString("method"))
        val relocationSelector = relocation.getJSONArray("args").getJSONObject(0)
        assertEquals("Wi-Fi", relocationSelector.getString("text"))
        assertEquals("android.widget.TextView", relocationSelector.getString("className"))
        assertEquals(152, relocationSelector.getJSONObject("boundsInside").getInt("top"))
        val clickRequest = broker.requests.last()
        assertEquals("click", clickRequest.getString("method"))
        val exact = clickRequest.getJSONArray("args").getJSONObject(0)
        assertEquals(210, exact.getJSONObject("boundsInside").getInt("top"))
        assertEquals(210, exact.getJSONObject("boundsContains").getInt("top"))
        Log.i(TAG, "ui_click #n3: relocation $relocationSelector -> exact $exact")

        val stale = post(toolsCallRequest(ToolCatalog.UI_CLICK, """{"nodeRef":"#n9"}"""), sessionId).json().getJSONObject("result")
        assertTrue(stale.getBoolean("isError"))
        val staleText = stale.getJSONArray("content").getJSONObject(0).getString("text")
        assertTrue(staleText, staleText.startsWith("NODE_REF_STALE: #n9 is not in the current snapshot s1"))
        assertEquals("NODE_REF_STALE", stale.getJSONObject("structuredContent").getJSONObject("error").getString("code"))

        val setText = post(toolsCallRequest(ToolCatalog.UI_SET_TEXT, """{"selector":{"id":"search"},"text":" more","append":true}"""), sessionId).json().getJSONObject("result")
        assertFalse(setText.toString(), setText.optBoolean("isError"))
        assertEquals("old more", setText.getJSONObject("structuredContent").getString("text"))
        assertEquals("selector", setText.getJSONObject("structuredContent").getString("via"))
        val setTextRequest = broker.requests.last()
        assertEquals("setText", setTextRequest.getString("method"))
        assertEquals("old more", setTextRequest.getJSONArray("args").getString(1))
        assertTrue(setTextRequest.getJSONArray("args").getJSONObject(0).getString("idMatches").contains("search"))

        val scroll = post(toolsCallRequest(ToolCatalog.UI_SCROLL, """{"direction":"down","times":3}"""), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertEquals(1, scroll.getInt("performed"))
        assertEquals(3, scroll.getInt("requested"))
        assertTrue(scroll.getBoolean("atEnd"))
        assertEquals("scrollable", scroll.getString("via"))
        assertEquals(2, broker.requests.count { it.getString("method") == "scrollForward" })

        val key = post(toolsCallRequest(ToolCatalog.UI_PRESS_KEY, """{"key":"notifications"}"""), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertTrue(key.getBoolean("performed"))
        assertEquals("keys", broker.requests.last().getString("module"))
        assertEquals("notifications", broker.requests.last().getString("method"))
        assertEquals(listOf("keys"), broker.requests.last().getJSONArray("permissions").toStringList())

        val window = post(toolsCallRequest(ToolCatalog.UI_CURRENT_WINDOW), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertEquals("com.android.settings", window.getString("packageName"))
        assertFalse(window.has("schema"))
        assertEquals(listOf("app.query", "accessibility"), broker.requests.last().getJSONArray("permissions").toStringList())

        // Decision D22: the coordinate form and the ui_gesture tools follow the ui_gesture switch.
        val coordinates = post(toolsCallRequest(ToolCatalog.UI_CLICK, """{"x":10,"y":20}"""), sessionId).json().getJSONObject("result")
        assertTrue(coordinates.getBoolean("isError"))
        assertTrue(coordinates.getJSONArray("content").getJSONObject(0).getString("text").startsWith("TOOL_DISABLED: ui_click with x and y is a coordinate gesture"))
        val swipeOff = post(toolsCallRequest(ToolCatalog.UI_SWIPE, """{"x1":1,"y1":2,"x2":3,"y2":4}"""), sessionId).json().getJSONObject("result")
        assertTrue(swipeOff.getJSONArray("content").getJSONObject(0).getString("text").startsWith("TOOL_DISABLED: tool ui_swipe is switched off by the ui_gesture group"))
        assertFalse(toolNames(post(toolsListRequest(), sessionId)).contains(ToolCatalog.UI_SWIPE))
        val requestsBeforeGate = broker.requests.size

        ToolPermissionStore(context).save(ToolPermissions.DEFAULT.with(ToolGroup.UI_GESTURE, true))
        assertEquals(ToolCatalog.all.map { it.name }, toolNames(post(toolsListRequest(), sessionId)))
        val tap = post(toolsCallRequest(ToolCatalog.UI_CLICK, """{"x":10,"y":20}"""), sessionId).json().getJSONObject("result")
        assertFalse(tap.toString(), tap.optBoolean("isError"))
        assertEquals("coordinates", tap.getJSONObject("structuredContent").getString("via"))
        val tapRequest = broker.requests.last()
        assertEquals("swipe", tapRequest.getString("method"))
        assertEquals(listOf(10, 20, 10, 20, 100), (0 until 5).map { tapRequest.getJSONArray("args").getInt(it) })
        assertEquals(listOf("accessibility", "accessibility.gesture"), tapRequest.getJSONArray("permissions").toStringList())
        val swipe = post(toolsCallRequest(ToolCatalog.UI_SWIPE, """{"x1":100,"y1":1500,"x2":100,"y2":500}"""), sessionId).json().getJSONObject("result").getJSONObject("structuredContent")
        assertTrue(swipe.getBoolean("performed"))
        assertEquals(300, swipe.getInt("durationMs"))
        assertEquals(100, broker.requests.last().getJSONArray("args").getInt(0))
        Log.i(TAG, "ui group through the broker: dump, click (nodeRef), stale ref, set_text, scroll, press_key, current_window, gate ($requestsBeforeGate requests before it), tap, swipe; ${broker.requests.size} broker requests")

        session.stop(reasonBundle(McpServerContract.REASON_USER_REQUEST))
        awaitPort(open = false)
        session.close()
    }

    @Test
    fun aSecondOpenIsRefusedWhileTheFirstHostIsAlive() {
        val first = runtime.openSession(configBundle(PORT), FakeBroker(), RecordingCallback(), Process.myUid())
        awaitStatus(first) { it.getString(McpServerContract.KEY_STATUS_STATE) == McpServerContract.STATE_RUNNING }
        try {
            runtime.openSession(configBundle(PORT), FakeBroker(), RecordingCallback(), Process.myUid())
            fail("a second openServer must be refused while the first host is alive")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty(), expected.message.orEmpty().startsWith(McpServerContract.ERROR_ALREADY_OPEN))
        }
        first.close()
        val second = runtime.openSession(configBundle(PORT), FakeBroker(), RecordingCallback(), Process.myUid())
        awaitStatus(second) { it.getString(McpServerContract.KEY_STATUS_STATE) == McpServerContract.STATE_RUNNING }
        second.stop(reasonBundle(McpServerContract.REASON_HOST_SHUTDOWN))
        second.close()
    }

    @Test
    fun invalidConfigurationsAreRefusedBeforeAnythingStarts() {
        try {
            runtime.openSession(configBundle(80), FakeBroker(), RecordingCallback(), Process.myUid())
            fail("port 80 must be refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty(), expected.message.orEmpty().startsWith(McpServerContract.ERROR_INVALID_CONFIG))
        }
        try {
            runtime.openSession(configBundle(PORT).apply { putString(McpServerContract.KEY_SERVER_CONFIG_PROTOCOL_MODE, McpServerContract.PROTOCOL_MODE_DUAL) }, FakeBroker(), RecordingCallback(), Process.myUid())
            fail("the dual protocol mode must be refused")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty(), expected.message.orEmpty().startsWith(McpServerContract.ERROR_INVALID_CONFIG))
        }
        assertFalse(runtime.isListening)
        assertNull(runtime.bridge)
    }

    @Test
    fun hostBackedToolsAnswerHostUnavailableWithoutASession() {
        val status = runtime.start(ServerConfig(port = PORT))
        assertTrue(status.isRunning)
        awaitPort(open = true)
        val init = post(initializeRequest(), sessionId = null)
        val sessionId = init.header("mcp-session-id")
        post(initializedNotification(), sessionId)
        val held = post(toolsCallRequest(ToolCatalog.DEVICE_PING), sessionId)
        val fingerprint = held.json().getJSONObject("error").getJSONObject("data").getString("fingerprint")
        assertTrue(runtime.server.pairingGate!!.approve(fingerprint))

        val ping = post(toolsCallRequest(ToolCatalog.DEVICE_PING), sessionId).json().getJSONObject("result")
        assertFalse(ping.optBoolean("isError"))
        assertEquals(context.mcpServerPluginRuntimeInfo().versionName, ping.getJSONObject("structuredContent").getString("versionName"))

        val info = post(toolsCallRequest(ToolCatalog.DEVICE_INFO), sessionId).json().getJSONObject("result")
        assertTrue(info.getBoolean("isError"))
        val text = info.getJSONArray("content").getJSONObject(0).getString("text")
        assertTrue(text, text.startsWith("HOST_UNAVAILABLE: AutoJs6 is not connected to the MCP server"))
        assertFalse(runtime.statusSnapshot().hostAvailable)
        Log.i(TAG, "without a host session: $text")
    }

    // ------------------------------------------------------------------ fakes

    private class FakeBroker : IMcpHostCapabilityBroker.Stub() {

        val requests = CopyOnWriteArrayList<JSONObject>()
        val clientNames = CopyOnWriteArrayList<String?>()
        private var scrolls = 0

        override fun getBrokerInfo(): Bundle = Bundle().apply {
            putInt(McpServerContract.KEY_CONTRACT_VERSION, McpServerContract.CONTRACT_VERSION)
            putInt(McpServerContract.KEY_HOST_CAPABILITY_BROKER_VERSION, McpServerContract.HOST_CAPABILITY_BROKER_CONTRACT_VERSION)
            putString(McpServerContract.KEY_HOST_CAPABILITY_BROKER_ID, "fake-broker")
            putStringArray(McpServerContract.KEY_HOST_CAPABILITY_MODULES, arrayOf("device", "engines", "console", "accessibility", "keys", "app"))
            putStringArray(
                McpServerContract.KEY_GRANT_METHODS,
                arrayOf(
                    "device.info", "engines.execScript", "engines.execScriptFile", "engines.list", "engines.stop", "engines.stopAll", "console.tail",
                    "accessibility.dump", "accessibility.findAll", "accessibility.findOne", "accessibility.click", "accessibility.setText",
                    "accessibility.scrollForward", "accessibility.swipe", "accessibility.back", "keys.notifications", "app.currentWindow",
                ),
            )
            putStringArray(McpServerContract.KEY_GRANT_PERMISSIONS, arrayOf("device", "engines", "engines.exec", "console", "accessibility", "accessibility.gesture", "keys", "app.query"))
            putInt(McpServerContract.KEY_GRANT_MAX_REQUEST_BYTES, McpServerContract.MAX_BRIDGE_INLINE_JSON_BYTES)
            putInt(McpServerContract.KEY_GRANT_MAX_CONCURRENT_CALLS, McpServerContract.MAX_CONCURRENT_TOOL_CALLS)
            putLong(McpServerContract.KEY_GRANT_DEFAULT_TIMEOUT_MS, McpServerContract.DEFAULT_TOOL_TIMEOUT_MS)
            putLong(McpServerContract.KEY_GRANT_MAX_TIMEOUT_MS, McpServerContract.MAX_TOOL_TIMEOUT_MS)
        }

        override fun dispatch(request: Bundle?, callback: IMcpHostCapabilityCallback?) {
            val json = JSONObject(requireNotNull(request?.getString(McpServerContract.KEY_BRIDGE_REQUEST_JSON)))
            requests += json
            clientNames += request?.getString(McpServerContract.KEY_BRIDGE_CLIENT_NAME)
            val id = json.getString("id")
            val response = when ("${json.getString("module")}.${json.getString("method")}") {
                "device.info" -> JSONObject()
                    .put("id", id)
                    .put("ok", true)
                    .put("result", JSONObject().put("schema", "autojs6-bridge-device-info-v1").put("host", JSONObject().put("versionName", "6.8.0").put("pid", 4242)))
                "engines.execScript" -> {
                    val source = json.getJSONArray("args").getString(1)
                    when {
                        source.startsWith("fail") -> JSONObject().put("id", id).put("ok", false).put(
                            "error",
                            JSONObject().put("name", "ScriptError").put("message", source).put("code", "script-failed").put("category", "provider-failed").put("module", "engines").put("method", "execScript"),
                        )
                        source.startsWith("throw") -> ok(
                            id,
                            JSONObject().put("id", 4).put("sourceName", "demo").put("outcome", "exception").put("finished", true).put("waitedMs", 8)
                                .put("error", "Error: boom (\$engine/demo.js#2)"),
                        )
                        else -> {
                            val entries = JSONArray().put(JSONObject().put("id", 1).put("level", 4).put("levelName", "info").put("time", 1L).put("text", "first"))
                                .put(JSONObject().put("id", 2).put("level", 4).put("levelName", "info").put("time", 2L).put("text", "second"))
                            ok(
                                id,
                                JSONObject().put("id", 3).put("sourceName", "demo").put("engineName", "rhino").put("outcome", "success").put("finished", true).put("waitedMs", 12)
                                    .put("console", JSONObject().put("count", 2).put("truncated", false).put("entries", entries)),
                            )
                        }
                    }
                }
                "engines.execScriptFile" -> ok(
                    id,
                    JSONObject().put("id", 7).put("sourceName", "run.js").put("outcome", "running").put("finished", false).put("waitedMs", 0)
                        .put("console", JSONObject().put("count", 0).put("truncated", false).put("entries", JSONArray())),
                )
                "engines.list" -> ok(
                    id,
                    JSONObject().put("schema", "autojs6-bridge-engines-list-v1").put("scope", "host").put("count", 1).put(
                        "executions",
                        JSONArray().put(
                            JSONObject().put("id", 7).put("engineName", "rhino").put("sourceName", "run.js").put("sourcePath", "demo/run.js")
                                .put("workingDirectory", "/sdcard/Scripts").put("state", "running").put("startedAt", 1L).put("uptimeMs", 5L),
                        ),
                    ),
                )
                "engines.stop" -> ok(
                    id,
                    JSONObject().put("schema", "autojs6-bridge-engines-stop-v1").put("scope", "host").put("id", json.getJSONArray("args").getInt(0))
                        .put("sourceName", "run.js").put("state", "finished").put("stopped", true),
                )
                "engines.stopAll" -> ok(
                    id,
                    JSONObject().put("schema", "autojs6-bridge-engines-stop-all-v1").put("scope", json.getJSONArray("args").getJSONObject(0).getString("scope")).put("stopped", 2),
                )
                "console.tail" -> {
                    val options = json.getJSONArray("args").getJSONObject(0)
                    val entries = JSONArray().put(JSONObject().put("id", 8).put("level", 5).put("levelName", "warn").put("time", 8L).put("text", "w1"))
                        .put(JSONObject().put("id", 9).put("level", 6).put("levelName", "error").put("time", 9L).put("text", "e1"))
                    ok(
                        id,
                        JSONObject().put("schema", "autojs6-bridge-console-tail-v1").put("lines", options.optInt("lines")).put("count", 2)
                            .put("nextSinceId", 9).put("latestId", 9).put("total", 40).put("truncated", false).put("entries", entries),
                    )
                }
                "accessibility.dump" -> ok(
                    id,
                    JSONObject().put("schema", "autojs6-bridge-accessibility-dump-v1").put("format", "json").put("window", "active").put("roots", 1)
                        .put("packageName", "com.android.settings").put("activityName", "com.android.settings.Settings").put("nodeCount", 3).put("truncated", false)
                        .put(
                            "nodes",
                            JSONArray()
                                .put(uiNode("android.widget.FrameLayout", "", "", 0, 0, 1080, 2400, depth = 0, childCount = 1))
                                .put(uiNode("androidx.recyclerview.widget.RecyclerView", "", "com.android.settings:id/list", 0, 200, 1080, 2400, depth = 1, childCount = 1, scrollable = true))
                                .put(uiNode("android.widget.TextView", "Wi-Fi", "", 0, 200, 1080, 300, depth = 2, clickable = true)),
                        ),
                )
                "accessibility.findAll" -> {
                    val selector = json.getJSONArray("args").getJSONObject(0)
                    ok(id, JSONArray().apply { if (selector.optString("text") == "Wi-Fi") put(uiNode("android.widget.TextView", "Wi-Fi", "", 0, 210, 1080, 310, depth = 2, clickable = true)) })
                }
                "accessibility.findOne" -> {
                    val selector = json.getJSONArray("args").getJSONObject(0)
                    val node = when {
                        selector.optBoolean("scrollable") -> uiNode("androidx.recyclerview.widget.RecyclerView", "", "com.android.settings:id/list", 0, 200, 1080, 2400, depth = 1, childCount = 1, scrollable = true)
                        selector.has("idMatches") -> uiNode("android.widget.EditText", "old", "com.android.settings:id/search", 0, 100, 1080, 180, depth = 2)
                        else -> null
                    }
                    ok(id, node ?: JSONObject.NULL)
                }
                "accessibility.click", "accessibility.setText", "accessibility.swipe", "accessibility.back", "keys.notifications" -> ok(id, true)
                "accessibility.scrollForward" -> ok(id, scrolls++ == 0)
                "app.currentWindow" -> ok(
                    id,
                    JSONObject().put("schema", "autojs6-bridge-app-current-window-v1").put("packageName", "com.android.settings")
                        .put("activityName", "com.android.settings.Settings").put("accessibilityAvailable", true).put("windows", JSONArray()),
                )
                else -> JSONObject().put("id", id).put("ok", false).put("error", JSONObject().put("name", "Error").put("message", "not granted").put("code", "x").put("category", "capability-denied"))
            }
            val reply = Bundle().apply {
                putString(McpServerContract.KEY_BRIDGE_RESPONSE_JSON, response.toString())
                putBoolean(McpServerContract.KEY_BRIDGE_RESPONSE_OK, response.getBoolean("ok"))
                if (!response.getBoolean("ok")) putString(McpServerContract.KEY_BRIDGE_ERROR_MESSAGE, response.getJSONObject("error").getString("message"))
            }
            // A script source starting with "slow" answers after SLOW_SCRIPT_MS so the 2 s script heartbeat fires.
            val slow = json.getString("method") == "execScript" && json.getJSONArray("args").getString(1).startsWith("slow")
            if (slow) Thread { Thread.sleep(SLOW_SCRIPT_MS); callback?.onResponse(reply) }.start() else callback?.onResponse(reply)
        }

        override fun destroy(reason: Bundle?) = Unit

        private fun ok(id: String, result: Any): JSONObject = JSONObject().put("id", id).put("ok", true).put("result", result)

        /** One node of the host dump JSON (`BridgeNodeDump` entries with the P3.2 state flags). */
        private fun uiNode(
            className: String, text: String, id: String, left: Int, top: Int, right: Int, bottom: Int,
            depth: Int = 0, childCount: Int = 0, clickable: Boolean = false, scrollable: Boolean = false,
        ): JSONObject = JSONObject().put("className", className).put("text", text).put("desc", "").put("id", id)
            .put("bounds", JSONObject().put("left", left).put("top", top).put("right", right).put("bottom", bottom))
            .put("clickable", clickable).put("enabled", true).put("root", 0).put("depth", depth).put("index", if (depth == 0) -1 else 0)
            .put("childCount", childCount).put("visible", true).put("scrollable", scrollable).put("checkable", false).put("checked", false)
            .put("editable", className.endsWith("EditText")).put("focused", false).put("selected", false).put("longClickable", false)
    }

    private class RecordingCallback : IMcpServerCallback.Stub() {

        val statuses = CopyOnWriteArrayList<Bundle>()
        val events = CopyOnWriteArrayList<Bundle>()

        override fun onStatus(status: Bundle?) {
            status?.let { statuses += it }
        }

        override fun onEvent(event: Bundle?) {
            event?.let { events += it }
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun configBundle(port: Int): Bundle = Bundle().apply {
        putInt(McpServerContract.KEY_CONTRACT_VERSION, McpServerContract.CONTRACT_VERSION)
        putInt(McpServerContract.KEY_SERVER_CONFIG_PORT, port)
        putString(McpServerContract.KEY_SERVER_CONFIG_BIND_SCOPE, McpServerContract.BIND_SCOPE_LOOPBACK)
        putString(McpServerContract.KEY_SERVER_CONFIG_PROTOCOL_MODE, McpServerContract.PROTOCOL_MODE_STATEFUL)
        putString(McpServerContract.KEY_SERVER_CONFIG_HOST_LABEL, "AutoJs6 test host")
    }

    private fun reasonBundle(code: String): Bundle = Bundle().apply {
        putInt(McpServerContract.KEY_CONTRACT_VERSION, McpServerContract.CONTRACT_VERSION)
        putString(McpServerContract.KEY_ERROR_CODE, code)
    }

    private fun awaitStatus(session: org.autojs.plugin.mcp.server.api.IMcpServerSession, accept: (Bundle) -> Boolean): Bundle {
        val deadline = System.currentTimeMillis() + 20_000
        while (true) {
            val status = session.status
            if (accept(status)) return status
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("status did not settle within 20 s: ${status.getString(McpServerContract.KEY_STATUS_STATE)} ${status.getString(McpServerContract.KEY_STATUS_LAST_ERROR_CODE)}: ${status.getString(McpServerContract.KEY_STATUS_LAST_ERROR)}")
            }
            Thread.sleep(100)
        }
    }

    private fun awaitCondition(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("$what did not happen within 10 s")
            Thread.sleep(100)
        }
    }

    private fun isListening(): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(McpHttpServer.LOOPBACK_HOST, PORT), 500) }
        true
    } catch (e: Exception) {
        false
    }

    private fun awaitPort(open: Boolean) {
        val deadline = System.currentTimeMillis() + 20_000
        while (isListening() != open) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("port $PORT did not become ${if (open) "open" else "closed"} within 20 s")
            Thread.sleep(200)
        }
    }

    private fun toolNames(response: Response): List<String> {
        val tools = response.json().getJSONObject("result").getJSONArray("tools")
        return (0 until tools.length()).map { tools.getJSONObject(it).getString("name") }
    }

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

    private fun post(body: String, sessionId: String?): Response {
        val connection = URL(McpHttpServer.endpointUrl(PORT)).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5_000
        connection.readTimeout = 30_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json, text/event-stream")
        connection.setRequestProperty("MCP-Protocol-Version", PROTOCOL_VERSION)
        connection.setRequestProperty("Authorization", "Bearer $token")
        sessionId?.let { connection.setRequestProperty("Mcp-Session-Id", it) }
        connection.outputStream.use { it.write(body.toByteArray()) }
        val status = connection.responseCode
        val headers = connection.headerFields.filterKeys { it != null }.map { (name, values) -> name.lowercase() to values.joinToString(",") }.toMap()
        val stream = if (status >= 400) connection.errorStream else connection.inputStream
        val contentType = connection.contentType.orEmpty()
        val events = stream?.bufferedReader()?.use { reader ->
            if (contentType.startsWith("text/event-stream")) reader.readSseData() else listOf(reader.readText())
        }.orEmpty()
        connection.disconnect()
        return Response(status, headers, events.lastOrNull().orEmpty(), events)
    }

    /** Every `data:` payload of the response stream; the last one is the JSON-RPC response. */
    private fun BufferedReader.readSseData(): List<String> {
        val data = mutableListOf<String>()
        while (true) {
            val line = readLine() ?: return data
            if (line.startsWith("data:")) line.removePrefix("data:").trim().takeIf { it.isNotEmpty() }?.let { data += it }
        }
    }

    private class Response(val status: Int, val headers: Map<String, String>, val body: String, val events: List<String> = emptyList()) {
        fun header(name: String): String? = headers[name.lowercase()]
        fun json(): JSONObject = JSONObject(body)
    }

    private fun initializeRequest(): String = """
        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"$PROTOCOL_VERSION",
        "capabilities":{},"clientInfo":{"name":"$CLIENT_NAME","version":"1"}}}
    """.trimIndent()

    private fun initializedNotification(): String = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""

    private fun toolsListRequest(): String = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""

    private fun toolsCallRequest(name: String, arguments: String = "{}"): String =
        """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"$name","arguments":$arguments}}"""

    private companion object {
        const val TAG = "McpServerHostSessionTest"
        const val CLIENT_NAME = "McpServerHostSessionTest"
        const val PROTOCOL_VERSION = "2025-06-18"
        const val PORT = 9639
        const val SLOW_SCRIPT_MS = 2_600L
    }
}
