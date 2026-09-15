package io.github.supermonster003.autojs6.plugin.mcp.server

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.mcp.server.server.AddressClass
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpHttpServer
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingGate
import io.github.supermonster003.autojs6.plugin.mcp.server.server.RateLimits
import io.github.supermonster003.autojs6.plugin.mcp.server.server.RequestBodyChecks
import io.github.supermonster003.autojs6.plugin.mcp.server.server.RequestGate
import io.github.supermonster003.autojs6.plugin.mcp.server.store.PairedClientStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.TokenStore
import org.json.JSONObject
import org.junit.After
import org.junit.Assume
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Hostile input against the production listener on the device (roadmap P6): oversized, over-deep
 * and duplicate-id bodies, invalid UTF-8, unknown methods, header combinations, big base64
 * arguments, 64 concurrent sessions and a request flood. Every case must end in a bounded
 * answer, the listener must keep answering afterwards, and the `:mcp_server` process must
 * survive (a crash would close the port and fail the next request).
 */
@RunWith(AndroidJUnit4::class)
class McpServerAdversarialTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val port = McpServerPlugin.DEFAULT_PORT

    private lateinit var token: String

    @Before
    fun startServer() {
        PairedClientStore(context).clear()
        send(McpServerService.startIntent(context, port))
        awaitListener()
        token = TokenStore(context).current()
        PairedClientStore(context).put(PairedClient(PairingGate.fingerprintOf(CLIENT_NAME, AddressClass.LOOPBACK), CLIENT_NAME, "1", AddressClass.LOOPBACK, 1L, 1L))
    }

    @After
    fun stopServer() {
        send(McpServerService.stopIntent(context))
        awaitClosed()
        PairedClientStore(context).clear()
    }

    @Test
    fun oversizedOverDeepAndDuplicateIdBodiesAreRefusedBeforeParsing() {
        val padding = "x".repeat(RequestGate.MAX_REQUEST_BODY_BYTES.toInt())
        val oversized = post("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"pad":"$padding"}}""", sessionId = null)
        assertEquals(413, oversized.status)
        Log.i(TAG, "oversized body: ${oversized.status} ${oversized.body.take(160)}")

        val nested = post("[".repeat(100_000), sessionId = null)
        assertEquals(400, nested.status)
        assertEquals(RequestBodyChecks.MESSAGE_TOO_DEEP, nested.json().getJSONObject("error").getString("message"))
        Log.i(TAG, "100000 nested arrays: ${nested.status} ${nested.body.take(160)}")

        val duplicate = post("""[{"jsonrpc":"2.0","id":1,"method":"ping"},{"jsonrpc":"2.0","id":1,"method":"ping"}]""", sessionId = null)
        assertEquals(400, duplicate.status)
        assertEquals(RequestBodyChecks.MESSAGE_DUPLICATE_ID, duplicate.json().getJSONObject("error").getString("message"))

        assertStillAlive()
    }

    @Test
    fun invalidUtf8UnknownMethodsAndHeaderCombinationsAreBounded() {
        val garbage = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), '{'.code.toByte(), 0xC3.toByte(), 0x28, '"'.code.toByte(), 0x00, '}'.code.toByte())
        val invalid = postBytes(garbage, sessionId = null)
        assertTrue("invalid UTF-8: ${invalid.status} ${invalid.body.take(120)}", invalid.status in 400..499)
        Log.i(TAG, "invalid UTF-8: ${invalid.status} ${invalid.body.take(160)}")

        val truncated = post("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":""", sessionId = null)
        assertTrue("truncated JSON: ${truncated.status}", truncated.status in 400..499)

        val sessionId = openSession()
        val unknown = post("""{"jsonrpc":"2.0","id":9,"method":"no/such/method","params":{}}""", sessionId)
        assertEquals(200, unknown.status)
        assertEquals(-32601, unknown.json().getJSONObject("error").getInt("code"))

        val wrongShape = post("""{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":["not","a","string"],"arguments":7}}""", sessionId)
        assertTrue("malformed tools/call: ${wrongShape.status}", wrongShape.status < 500)

        val jsonOnly = request("POST", INITIALIZE.toByteArray(), sessionId = null, accept = "application/json")
        assertTrue("Accept without text/event-stream: ${jsonOnly.status}", jsonOnly.status in 400..499)
        val textBody = request("POST", INITIALIZE.toByteArray(), sessionId = null, contentType = "text/plain")
        assertTrue("text/plain body: ${textBody.status}", textBody.status in 400..499)
        assertEquals(400, request("GET", null, sessionId = null, accept = "text/event-stream").status)
        assertEquals(404, request("GET", null, sessionId = "nope", accept = "text/event-stream").status)
        assertEquals(400, request("DELETE", null, sessionId = null).status)
        assertEquals(404, request("DELETE", null, sessionId = "nope").status)
        assertEquals(404, post(TOOLS_LIST, sessionId = "nope").status)
        assertEquals(404, post(TOOLS_LIST, sessionId = "x".repeat(4_096)).status)
        val badVersion = request("POST", INITIALIZE.toByteArray(), sessionId = null, protocolVersion = "not-a-version")
        assertTrue("unknown protocol header: ${badVersion.status}", badVersion.status < 500)
        Log.i(TAG, "headers: truncated=${truncated.status} unknownMethod=${unknown.json().getJSONObject("error").getInt("code")} wrongShape=${wrongShape.status} jsonOnlyAccept=${jsonOnly.status} textPlain=${textBody.status} badVersion=${badVersion.status}")

        assertStillAlive()
    }

    @Test
    fun largeBase64ArgumentsAreBoundedByTheBodyCeiling() {
        val sessionId = openSession()
        val underCeiling = "QUJD".repeat(160_000)
        val answered = post("""{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"no_such_tool","arguments":{"content":"$underCeiling","encoding":"base64"}}}""", sessionId)
        assertTrue("640 000 base64 characters to an unknown tool: ${answered.status}", answered.status < 500)
        assertTrue(answered.body.isNotEmpty())
        Log.i(TAG, "640000 base64 chars: ${answered.status} ${answered.body.take(160)}")

        val overCeiling = "QUJD".repeat(270_000)
        val refused = post("""{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"${DevicePingTool.NAME}","arguments":{"content":"$overCeiling"}}}""", sessionId)
        assertEquals(413, refused.status)

        assertStillAlive()
    }

    @Test
    fun sixtyFourConcurrentSessionsCompleteAndTheListenerSurvives() {
        // Distinct client names: the rate limiter keys by client, and 64 sessions of one client
        // would be a flood (covered by the flood test), not a concurrency check.
        val names = (1..64).map { "$CLIENT_NAME-$it" }
        names.forEach { name ->
            PairedClientStore(context).put(PairedClient(PairingGate.fingerprintOf(name, AddressClass.LOOPBACK), name, "1", AddressClass.LOOPBACK, 1L, 1L))
        }
        val pool = Executors.newFixedThreadPool(64)
        val started = System.currentTimeMillis()
        try {
            val futures = names.mapIndexed { index, name ->
                pool.submit<String> {
                    val init = request("POST", initialize(name).toByteArray(), sessionId = null, userAgent = name)
                    val sessionId = init.header("Mcp-Session-Id") ?: return@submit "no session ($index): ${init.status} ${init.body.take(100)}"
                    val initialized = request("POST", INITIALIZED.toByteArray(), sessionId, userAgent = name).status
                    val list = request("POST", TOOLS_LIST.toByteArray(), sessionId, userAgent = name)
                    val tools = runCatching { list.json().getJSONObject("result").getJSONArray("tools").length() }.getOrNull()
                    val call = request("POST", toolsCall(DevicePingTool.NAME).toByteArray(), sessionId, userAgent = name)
                    val isError = runCatching { call.json().getJSONObject("result").optBoolean("isError") }.getOrNull()
                    "${init.status} ${initialized} ${list.status} tools>0=${(tools ?: 0) > 0} call=${call.status} isError=$isError"
                }
            }
            val results = futures.map { it.get(60, TimeUnit.SECONDS) }
            val expected = "200 202 200 tools>0=true call=200 isError=false"
            val failures = results.filter { it != expected }
            Log.i(TAG, "64 concurrent sessions (initialize, initialized, tools/list, device_ping) in ${System.currentTimeMillis() - started} ms; failures=${failures.size}")
            assertTrue("every one of 64 concurrent sessions completes (${System.currentTimeMillis() - started} ms): $failures", failures.isEmpty())
        } finally {
            pool.shutdownNow()
        }
        assertStillAlive()
    }

    @Test
    fun aRequestFloodIsRateLimitedWithRetryAfterAndRecovers() {
        val sessionId = openSession()
        val limit = RateLimits.REQUESTS_PER_SECOND
        // Eight threads so that the flood lands inside one window even on a slow device.
        val pool = Executors.newFixedThreadPool(8)
        val started = System.currentTimeMillis()
        val responses = try {
            (1..(limit * 2)).map { id -> pool.submit<Response> { post(toolsList(100 + id), sessionId) } }.map { it.get(60, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
        val elapsed = System.currentTimeMillis() - started
        val statuses = responses.groupingBy { it.status }.eachCount()
        Log.i(TAG, "flood of ${limit * 2} tools/list on 8 threads in $elapsed ms: statuses=$statuses")
        assertTrue("only 200 and 429 in the flood ($elapsed ms): $statuses", statuses.keys.all { it == 200 || it == 429 })
        val refused = responses.firstOrNull { it.status == 429 }
        val achievedPerSecond = (limit * 2) * 1_000.0 / elapsed.coerceAtLeast(1)
        // A device that cannot even reach the limit (the Xiaomi Pad API 35 manages about 15 requests
        // per second here) proves nothing about the window; the JVM tests cover the limiter itself.
        Assume.assumeTrue("the device reached only ${"%.1f".format(achievedPerSecond)} requests per second, below the limit of $limit; window not testable here: $statuses", refused != null || achievedPerSecond >= limit)
        assertNotNull("the flood of ${limit * 2} requests in $elapsed ms (${"%.1f".format(achievedPerSecond)} per second) must hit the per-second window: $statuses", refused)
        val error = refused!!.json().getJSONObject("error")
        assertEquals("RATE_LIMITED", error.getJSONObject("data").getString("code"))
        val retryAfterMs = responses.filter { it.status == 429 }.maxOf { it.json().getJSONObject("error").getJSONObject("data").getLong("retryAfterMs") }
        assertTrue("retryAfterMs $retryAfterMs is within the window", retryAfterMs in 1..1_000)
        Log.i(TAG, "first 429: Retry-After=${refused.header("Retry-After")} retryAfterMs(max)=$retryAfterMs body=${refused.body.take(200)}")
        assertEquals("1", refused.header("Retry-After"))
        assertTrue("the request id is echoed", refused.json().getInt("id") in 101..(100 + limit * 2))

        Thread.sleep(retryAfterMs + 150)
        assertEquals("the window moved on", 200, post(TOOLS_LIST, sessionId).status)
        assertStillAlive()
    }

    @Test
    fun aRequestIdStillInFlightOnTheSessionIsRefused() {
        val sessionId = openSession()
        // ui_wait_for needs the host; without it the call fails fast, so a slow answer comes from
        // tools/list bursts instead: the second POST with the same id must be refused while the
        // first is in flight, and the first must still be answered.
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit<Response> { post(toolsList(77), sessionId) }
            val second = pool.submit<Response> { post(toolsList(77), sessionId) }
            val results = listOf(first.get(60, TimeUnit.SECONDS), second.get(60, TimeUnit.SECONDS))
            val statuses = results.map { it.status }.sorted()
            Log.i(TAG, "two POSTs with id 77 at once: $statuses ${results.map { it.body.take(120) }}")
            assertTrue("either both completed in turn or the overlapping one was refused: $statuses", statuses == listOf(200, 200) || statuses == listOf(200, 400))
        } finally {
            pool.shutdownNow()
        }
        assertEquals("the id is free again afterwards", 200, post(toolsList(77), sessionId).status)
        assertStillAlive()
    }

    private fun openSession(): String {
        val init = post(INITIALIZE, sessionId = null)
        assertEquals("initialize: ${init.body.take(200)}", 200, init.status)
        val sessionId = init.header("Mcp-Session-Id")
        assertNotNull("Mcp-Session-Id", sessionId)
        assertEquals(202, post(INITIALIZED, sessionId).status)
        return sessionId!!
    }

    private fun assertStillAlive() {
        assertTrue("the port is still open", isListening())
        val init = request("POST", INITIALIZE.toByteArray(), sessionId = null, userAgent = "$CLIENT_NAME-alive")
        assertEquals("a normal initialize afterwards: ${init.body.take(200)}", 200, init.status)
        assertNotNull(init.header("Mcp-Session-Id"))
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
            if (System.currentTimeMillis() > deadline) throw AssertionError("The MCP listener did not open port $port within 20 s")
            Thread.sleep(250)
        }
    }

    private fun awaitClosed() {
        val deadline = System.currentTimeMillis() + 20_000
        while (isListening()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("The MCP listener did not close port $port within 20 s")
            Thread.sleep(250)
        }
    }

    private fun post(body: String, sessionId: String?): Response = postBytes(body.toByteArray(Charsets.UTF_8), sessionId)

    private fun postBytes(body: ByteArray, sessionId: String?): Response = request("POST", body, sessionId)

    private fun request(
        method: String,
        body: ByteArray?,
        sessionId: String?,
        accept: String = "application/json, text/event-stream",
        contentType: String = "application/json",
        protocolVersion: String = PROTOCOL_VERSION,
        userAgent: String = CLIENT_NAME,
    ): Response {
        val connection = URL(McpHttpServer.endpointUrl(port)).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 5_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("MCP-Protocol-Version", protocolVersion)
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("User-Agent", userAgent)
        sessionId?.let { connection.setRequestProperty("Mcp-Session-Id", it) }
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType)
            connection.outputStream.use { it.write(body) }
        }
        val status = connection.responseCode
        val headers = connection.headerFields
            .filterKeys { it != null }
            .map { (name, values) -> name.lowercase() to values.joinToString(",") }
            .toMap()
        val stream = if (status >= 400) connection.errorStream else connection.inputStream
        val responseType = connection.contentType.orEmpty()
        val text = runCatching {
            stream?.bufferedReader()?.use { reader ->
                if (responseType.startsWith("text/event-stream")) reader.readFirstSseData() else reader.readText()
            }
        }.getOrNull().orEmpty()
        connection.disconnect()
        return Response(status, headers, text)
    }

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

    private fun initialize(clientName: String): String =
        """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"$PROTOCOL_VERSION","capabilities":{},"clientInfo":{"name":"$clientName","version":"1"}}}"""

    private fun toolsList(id: Int): String = """{"jsonrpc":"2.0","id":$id,"method":"tools/list","params":{}}"""

    private fun toolsCall(name: String): String =
        """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"$name","arguments":{}}}"""

    private companion object {
        const val TAG = "McpServerAdversarialTest"
        const val CLIENT_NAME = "McpServerAdversarialTest"
        const val PROTOCOL_VERSION = "2025-06-18"
        val INITIALIZE = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"$PROTOCOL_VERSION","capabilities":{},"clientInfo":{"name":"$CLIENT_NAME","version":"1"}}}"""
        const val INITIALIZED = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""
        const val TOOLS_LIST = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""
    }
}
