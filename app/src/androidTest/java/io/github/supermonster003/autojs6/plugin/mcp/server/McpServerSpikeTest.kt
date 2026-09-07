package io.github.supermonster003.autojs6.plugin.mcp.server

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * Roadmap P0.2 on-device evidence: starts [McpServerService], then drives the Streamable HTTP
 * endpoint on the loopback interface exactly like a PC client would after `adb forward`.
 *
 * The stateful sequence (`initialize` -> `notifications/initialized` -> `tools/list` ->
 * `tools/call device_ping`) must succeed; the behaviour of a request without a session is only
 * recorded in logcat because the SDK, not this plugin, decides it (roadmap D9).
 */
@RunWith(AndroidJUnit4::class)
class McpServerSpikeTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val port = McpServerPlugin.DEFAULT_PORT

    @Before
    fun startServer() {
        val intent = McpServerService.startIntent(context, port)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        awaitListener()
    }

    @After
    fun stopServer() {
        context.stopService(Intent(context, McpServerService::class.java))
    }

    @Test
    fun statefulSessionListsAndCallsDevicePing() {
        val init = post(initializeRequest(), sessionId = null)
        assertEquals("initialize status", 200, init.status)
        val sessionId = init.header("mcp-session-id")
        assertNotNull("initialize must return Mcp-Session-Id", sessionId)
        val initResult = init.json().getJSONObject("result")
        assertEquals(McpServerPlugin.ID, initResult.getJSONObject("serverInfo").getString("name"))
        assertTrue(initResult.getJSONObject("capabilities").has("tools"))
        Log.i(TAG, "initialize: protocolVersion=${initResult.optString("protocolVersion")} session=$sessionId")

        val initialized = post(initializedNotification(), sessionId)
        assertTrue("initialized notification status ${initialized.status}", initialized.status in 200..202)

        val list = post(toolsListRequest(), sessionId)
        assertEquals("tools/list status", 200, list.status)
        val tools = list.json().getJSONObject("result").getJSONArray("tools")
        val names = (0 until tools.length()).map { tools.getJSONObject(it).getString("name") }
        assertEquals(listOf(DevicePingTool.NAME), names)

        val startedAt = System.nanoTime()
        val call = post(toolsCallRequest(DevicePingTool.NAME), sessionId)
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
        assertEquals("tools/call status", 200, call.status)
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
    fun requestWithoutSessionIsAnsweredDeterministically() {
        val list = post(toolsListRequest(), sessionId = null)
        Log.i(TAG, "tools/list without a session: status=${list.status} body=${list.body.take(300)}")
        assertTrue("unexpected status ${list.status}", list.status == 200 || list.status in 400..499)
    }

    private fun awaitListener() {
        val deadline = System.currentTimeMillis() + 20_000
        while (true) {
            try {
                Socket().use { it.connect(InetSocketAddress(McpHttpServer.LOOPBACK_HOST, port), 500) }
                return
            } catch (e: Exception) {
                if (System.currentTimeMillis() > deadline) {
                    throw AssertionError("The MCP listener did not open port $port within 20 s", e)
                }
                Thread.sleep(250)
            }
        }
    }

    private fun post(body: String, sessionId: String?): Response {
        val connection = URL(McpHttpServer.endpointUrl(port)).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json, text/event-stream")
        connection.setRequestProperty("MCP-Protocol-Version", PROTOCOL_VERSION)
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

    /** Reads until the first `data:` line, which carries the JSON-RPC response of a POST. */
    private fun BufferedReader.readFirstSseData(): String {
        while (true) {
            val line = readLine() ?: return ""
            if (line.startsWith("data:")) {
                return line.removePrefix("data:").trim()
            }
        }
    }

    private class Response(val status: Int, val headers: Map<String, String>, val body: String) {
        fun header(name: String): String? = headers[name.lowercase()]
        fun json(): JSONObject = JSONObject(body)
    }

    private fun initializeRequest(): String = """
        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"$PROTOCOL_VERSION",
        "capabilities":{},"clientInfo":{"name":"McpServerSpikeTest","version":"1"}}}
    """.trimIndent()

    private fun initializedNotification(): String = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""

    private fun toolsListRequest(): String = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""

    private fun toolsCallRequest(name: String): String =
        """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"$name","arguments":{}}}"""

    private companion object {
        const val TAG = "McpServerSpikeTest"
        const val PROTOCOL_VERSION = "2025-06-18"
    }
}
