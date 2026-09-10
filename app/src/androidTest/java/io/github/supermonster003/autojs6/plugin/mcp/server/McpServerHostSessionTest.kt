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
        assertEquals(ToolCatalog.all.map { it.name }, names)
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
        assertEquals("success", runResult.getString("outcome"))
        assertEquals(1, runResult.getJSONObject("console").getJSONArray("entries").length())
        assertTrue(runResult.getJSONObject("console").getBoolean("truncated"))
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

        val failed = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"fail please"}"""), sessionId).json().getJSONObject("result")
        assertTrue(failed.getBoolean("isError"))
        val failedText = failed.getJSONArray("content").getJSONObject(0).getString("text")
        assertTrue(failedText, failedText.startsWith("HOST_ERROR: ScriptError: fail please"))
        assertEquals("HOST_ERROR", failed.getJSONObject("structuredContent").getJSONObject("error").getString("code"))

        val invalid = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"x","timeoutMs":5}"""), sessionId).json().getJSONObject("result")
        assertTrue(invalid.getBoolean("isError"))
        assertTrue(invalid.getJSONArray("content").getJSONObject(0).getString("text").startsWith("INVALID_ARGUMENTS: script_run: timeoutMs must be at least 1000"))

        awaitCondition("tool_call events") {
            callback.events.count { it.getString(McpServerContract.KEY_EVENT_TYPE) == McpServerContract.EVENT_TOOL_CALL && it.getString(McpServerContract.KEY_EVENT_CLIENT_NAME) == CLIENT_NAME } >= 3
        }
        assertTrue(callback.events.any { it.getString(McpServerContract.KEY_EVENT_TOOL_NAME) == ToolCatalog.SCRIPT_RUN })

        ToolPermissionStore(context).save(ToolPermissions.DEFAULT.with(ToolGroup.SCRIPT, false))
        assertEquals(listOf(ToolCatalog.DEVICE_PING, ToolCatalog.DEVICE_INFO), toolNames(post(toolsListRequest(), sessionId)))
        val disabled = post(toolsCallRequest(ToolCatalog.SCRIPT_RUN, """{"source":"x"}"""), sessionId).json().getJSONObject("result")
        assertTrue(disabled.getBoolean("isError"))
        assertTrue(disabled.getJSONArray("content").getJSONObject(0).getString("text").startsWith("TOOL_DISABLED: tool script_run is switched off by the script group"))
        ToolPermissionStore(context).save(ToolPermissions.DEFAULT)
        assertEquals(ToolCatalog.all.map { it.name }, toolNames(post(toolsListRequest(), sessionId)))

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

        override fun getBrokerInfo(): Bundle = Bundle().apply {
            putInt(McpServerContract.KEY_CONTRACT_VERSION, McpServerContract.CONTRACT_VERSION)
            putInt(McpServerContract.KEY_HOST_CAPABILITY_BROKER_VERSION, McpServerContract.HOST_CAPABILITY_BROKER_CONTRACT_VERSION)
            putString(McpServerContract.KEY_HOST_CAPABILITY_BROKER_ID, "fake-broker")
            putStringArray(McpServerContract.KEY_HOST_CAPABILITY_MODULES, arrayOf("device", "engines"))
            putStringArray(McpServerContract.KEY_GRANT_METHODS, arrayOf("device.info", "engines.execScript"))
            putStringArray(McpServerContract.KEY_GRANT_PERMISSIONS, arrayOf("device", "engines", "engines.exec"))
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
                    if (source.startsWith("fail")) {
                        JSONObject().put("id", id).put("ok", false).put(
                            "error",
                            JSONObject().put("name", "ScriptError").put("message", source).put("code", "script-failed").put("category", "provider-failed").put("module", "engines").put("method", "execScript"),
                        )
                    } else {
                        val entries = JSONArray().put(JSONObject().put("id", 1).put("level", 4).put("levelName", "info").put("time", 1L).put("text", "first"))
                            .put(JSONObject().put("id", 2).put("level", 4).put("levelName", "info").put("time", 2L).put("text", "second"))
                        JSONObject().put("id", id).put("ok", true).put(
                            "result",
                            JSONObject().put("outcome", "success").put("finished", true).put("console", JSONObject().put("count", 2).put("truncated", false).put("entries", entries)),
                        )
                    }
                }
                else -> JSONObject().put("id", id).put("ok", false).put("error", JSONObject().put("name", "Error").put("message", "not granted").put("code", "x").put("category", "capability-denied"))
            }
            callback?.onResponse(Bundle().apply {
                putString(McpServerContract.KEY_BRIDGE_RESPONSE_JSON, response.toString())
                putBoolean(McpServerContract.KEY_BRIDGE_RESPONSE_OK, response.getBoolean("ok"))
                if (!response.getBoolean("ok")) putString(McpServerContract.KEY_BRIDGE_ERROR_MESSAGE, response.getJSONObject("error").getString("message"))
            })
        }

        override fun destroy(reason: Bundle?) = Unit
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
        val text = stream?.bufferedReader()?.use { reader ->
            if (contentType.startsWith("text/event-stream")) reader.readFirstSseData() else reader.readText()
        }.orEmpty()
        connection.disconnect()
        return Response(status, headers, text)
    }

    private fun BufferedReader.readFirstSseData(): String {
        while (true) {
            val line = readLine() ?: return ""
            if (line.startsWith("data:")) return line.removePrefix("data:").trim()
        }
    }

    private class Response(val status: Int, val headers: Map<String, String>, val body: String) {
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
    }
}
