package io.github.supermonster003.autojs6.plugin.mcp.server

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.mcp.server.server.BindFailures
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpErrors
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpHttpServer
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpServerFactory
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ServerStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.store.PairedClientStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.store.TokenStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissions
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.PairingDecisionReceiver
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * On-device evidence for roadmap P0.2, P2.1, and P2.2: starts [McpServerService], then drives the
 * Streamable HTTP endpoint on the loopback interface exactly like a PC client would after
 * `adb forward`.
 *
 * The stateful sequence (`initialize` -> `notifications/initialized` -> `tools/list` ->
 * `tools/call device_ping`) must succeed for a paired client; a request without the bearer token
 * is refused with 401; an unpaired client is held back with `PAIRING_REQUIRED` until the
 * confirmation arrives through the notification action path; a foreign `Host` header must be
 * refused by the request gate; a second listener on the same port must report `port_in_use`.
 */
@RunWith(AndroidJUnit4::class)
class McpServerSpikeTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val port = McpServerPlugin.DEFAULT_PORT

    private lateinit var token: String

    @Before
    fun startServer() {
        PairedClientStore(context).clear()
        send(McpServerService.startIntent(context, port))
        awaitListener()
        token = TokenStore(context).current()
    }

    /** Stops through the production path (`ACTION_STOP`) and waits until the port is closed. */
    @After
    fun stopServer() {
        send(McpServerService.stopIntent(context))
        awaitClosed()
        PairedClientStore(context).clear()
    }

    @Test
    fun statefulSessionListsAndCallsDevicePingOncePaired() {
        val init = post(initializeRequest(), sessionId = null)
        assertEquals("initialize status", 200, init.status)
        val sessionId = init.header("mcp-session-id")
        assertNotNull("initialize must return Mcp-Session-Id", sessionId)
        val initResult = init.json().getJSONObject("result")
        assertEquals(McpServerFactory.SERVER_NAME, initResult.getJSONObject("serverInfo").getString("name"))
        assertEquals(context.mcpServerPluginRuntimeInfo().versionName, initResult.getJSONObject("serverInfo").getString("version"))
        val capabilities = initResult.getJSONObject("capabilities")
        assertTrue(capabilities.getJSONObject("tools").getBoolean("listChanged"))
        assertTrue(capabilities.has("resources"))
        assertTrue(capabilities.has("prompts"))
        Log.i(TAG, "initialize: protocolVersion=${initResult.optString("protocolVersion")} session=$sessionId")

        val initialized = post(initializedNotification(), sessionId)
        assertTrue("initialized notification status ${initialized.status}", initialized.status in 200..202)

        val list = post(toolsListRequest(), sessionId)
        assertEquals("tools/list status", 200, list.status)
        val tools = list.json().getJSONObject("result").getJSONArray("tools")
        val names = (0 until tools.length()).map { tools.getJSONObject(it).getString("name") }
        assertEquals(ToolCatalog.enabled(ToolPermissions.DEFAULT).map { it.name }, names)

        val held = post(toolsCallRequest(DevicePingTool.NAME), sessionId)
        assertEquals("gated call status", 200, held.status)
        val error = held.json().getJSONObject("error")
        assertEquals(McpErrors.PAIRING_REQUIRED, error.getInt("code"))
        val data = error.getJSONObject("data")
        assertEquals(CLIENT_NAME, data.getString("client"))
        assertEquals("loopback", data.getString("addressClass"))
        val fingerprint = data.getString("fingerprint")
        Log.i(TAG, "pairing required: ${error.getString("message")}")

        context.sendBroadcast(PairingDecisionReceiver.intent(context, fingerprint, allow = true))
        awaitDecision(sessionId) { it.optJSONObject("error") == null }
        assertEquals(listOf(CLIENT_NAME), PairedClientStore(context).all().map { it.name })

        val startedAt = System.nanoTime()
        val call = post(toolsCallRequest(DevicePingTool.NAME), sessionId)
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
        assertEquals("tools/call status", 200, call.status)
        assertNull("no error after pairing", call.json().optJSONObject("error"))
        val content = call.json().getJSONObject("result").getJSONArray("content").getJSONObject(0)
        assertEquals("text", content.getString("type"))
        val payload = JSONObject(content.getString("text"))
        val expected = context.mcpServerPluginRuntimeInfo()
        assertEquals(expected.versionName, payload.getString("versionName"))
        assertEquals(expected.versionCode, payload.getLong("versionCode"))
        assertEquals(Build.VERSION.SDK_INT, payload.getInt("androidApi"))
        assertEquals("${context.packageName}:mcp_server", payload.getString("process"))
        Log.i(TAG, "device_ping round trip ${elapsedMillis} ms: $payload")
    }

    @Test
    fun deniedPairingIsReportedWithACooldown() {
        val init = post(initializeRequest(), sessionId = null)
        val sessionId = init.header("mcp-session-id")
        post(initializedNotification(), sessionId)
        val held = post(toolsCallRequest(DevicePingTool.NAME), sessionId)
        val fingerprint = held.json().getJSONObject("error").getJSONObject("data").getString("fingerprint")
        context.sendBroadcast(PairingDecisionReceiver.intent(context, fingerprint, allow = false))
        val error = awaitDecision(sessionId) { it.optJSONObject("error")?.optInt("code") == McpErrors.PAIRING_DENIED }
            .json().getJSONObject("error")
        assertEquals("denied", error.getJSONObject("data").getString("reason"))
        assertTrue(PairedClientStore(context).all().isEmpty())
        Log.i(TAG, "pairing denied: ${error.getString("message")}")
    }

    @Test
    fun requestWithoutTokenIsUnauthorized() {
        val response = post(initializeRequest(), sessionId = null, authorization = null)
        assertEquals(401, response.status)
        assertEquals("Bearer realm=\"autojs6-mcp-server\"", response.header("www-authenticate"))
        assertEquals(McpErrors.UNAUTHORIZED, response.json().getJSONObject("error").getInt("code"))
        val wrong = post(initializeRequest(), sessionId = null, authorization = "Bearer ${"x".repeat(43)}")
        assertEquals(401, wrong.status)
    }

    @Test
    fun requestWithoutSessionIsAnsweredDeterministically() {
        val list = post(toolsListRequest(), sessionId = null)
        Log.i(TAG, "tools/list without a session: status=${list.status} body=${list.body.take(300)}")
        assertTrue("unexpected status ${list.status}", list.status == 200 || list.status in 400..499)
    }

    @Test
    fun foreignHostHeaderIsRefusedByTheRequestGate() {
        val body = initializeRequest().toByteArray()
        val request = "POST ${McpServerPlugin.ENDPOINT_PATH} HTTP/1.1\r\n" +
                "Host: evil.example:$port\r\n" +
                "Authorization: Bearer $token\r\n" +
                "Content-Type: application/json\r\n" +
                "Accept: application/json, text/event-stream\r\n" +
                "MCP-Protocol-Version: $PROTOCOL_VERSION\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
        val statusLine = Socket(McpHttpServer.LOOPBACK_HOST, port).use { socket ->
            socket.soTimeout = 5_000
            socket.getOutputStream().apply {
                write(request.toByteArray())
                write(body)
                flush()
            }
            socket.getInputStream().bufferedReader().readLine().orEmpty()
        }
        Log.i(TAG, "foreign Host header: $statusLine")
        assertTrue(statusLine, statusLine.contains(" 403 "))
    }

    @Test
    fun secondListenerOnTheSamePortReportsPortInUse() {
        val second = McpHttpServer(context)
        val status = second.start(ServerConfig(port = port))
        Log.i(TAG, "second listener on port $port: ${status.state} ${status.errorCode}: ${status.message}")
        assertEquals(ServerStatus.STATE_FAILED, status.state)
        assertEquals(BindFailures.CODE_PORT_IN_USE, status.errorCode)
        assertFalse(second.isRunning)
        assertEquals(status, second.stop())
    }

    @Test
    fun invalidPortIsRefusedWithoutBinding() {
        val status = McpHttpServer(context).start(ServerConfig(port = 80))
        assertEquals(ServerStatus.STATE_FAILED, status.state)
        assertEquals(McpHttpServer.ERROR_INVALID_CONFIG, status.errorCode)
    }

    private fun send(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun isListening(): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(McpHttpServer.LOOPBACK_HOST, port), 500) }
        true
    } catch (e: Exception) {
        false
    }

    private fun awaitListener() {
        val deadline = System.currentTimeMillis() + 20_000
        while (!isListening()) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("The MCP listener did not open port $port within 20 s")
            }
            Thread.sleep(250)
        }
    }

    private fun awaitClosed() {
        val deadline = System.currentTimeMillis() + 20_000
        while (isListening()) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("The MCP listener did not close port $port within 20 s")
            }
            Thread.sleep(250)
        }
    }

    /** Repeats the gated call until the decision taken on the phone is visible to the client. */
    private fun awaitDecision(sessionId: String?, applied: (JSONObject) -> Boolean): Response {
        val deadline = System.currentTimeMillis() + 10_000
        while (true) {
            val response = post(toolsCallRequest(DevicePingTool.NAME), sessionId)
            if (response.status == 200 && applied(response.json())) return response
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("The pairing decision was not applied within 10 s: ${response.status} ${response.body.take(300)}")
            }
            Thread.sleep(250)
        }
    }

    private fun post(body: String, sessionId: String?, authorization: String? = "Bearer $token"): Response {
        val connection = URL(McpHttpServer.endpointUrl(port)).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json, text/event-stream")
        connection.setRequestProperty("MCP-Protocol-Version", PROTOCOL_VERSION)
        authorization?.let { connection.setRequestProperty("Authorization", it) }
        sessionId?.let { connection.setRequestProperty("Mcp-Session-Id", it) }
        connection.outputStream.use { it.write(body.toByteArray()) }
        val status = connection.responseCode
        val headers = connection.headerFields
            .filterKeys { it != null }
            .map { (name, values) -> name.lowercase() to values.joinToString(",") }
            .toMap()
        val stream = if (status >= 400) connection.errorStream else connection.inputStream
        val contentType = connection.contentType.orEmpty()
        val text = stream?.bufferedReader()?.use { reader ->
            if (contentType.startsWith("text/event-stream")) reader.readFirstSseData() else reader.readText()
        }.orEmpty()
        connection.disconnect()
        return Response(status, headers, text)
    }

    /** Reads until the first non-empty `data:` payload, which carries the JSON-RPC response of a POST. */
    private fun BufferedReader.readFirstSseData(): String {
        while (true) {
            val line = readLine() ?: return ""
            if (line.startsWith("data:")) {
                val payload = line.removePrefix("data:").trim()
                if (payload.isNotEmpty()) return payload
            }
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

    private fun toolsCallRequest(name: String): String =
        """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"$name","arguments":{}}}"""

    private companion object {
        const val TAG = "McpServerSpikeTest"
        const val CLIENT_NAME = "McpServerSpikeTest"
        const val PROTOCOL_VERSION = "2025-06-18"
    }
}
