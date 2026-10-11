package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.shared.RequestHandlerExtra
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotification
import io.modelcontextprotocol.kotlin.sdk.types.ProgressNotificationParams
import io.modelcontextprotocol.kotlin.sdk.types.RequestId
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.net.URI

/**
 * SSE resumability of the stateful mount (roadmap P9, SEP-1699): event ids, the priming event and
 * its retry interval, replay after `Last-Event-ID`, a GET that takes over a running POST stream, the
 * standalone stream resumed through the SDK, and a POST whose client really went away on CIO.
 */
class SseResumptionTest {

    private data class Event(val id: Long?, val retry: Long?, val data: String) {
        val json: JsonObject? get() = data.takeIf { it.isNotBlank() }?.let { Json.parseToJsonElement(it).jsonObject }
    }

    private fun parse(text: String): List<Event> = text.replace("\r\n", "\n").split("\n\n").mapNotNull { block -> eventOf(block.lines()) }

    private fun eventOf(lines: List<String>): Event? {
        val fields = lines.filter { it.isNotEmpty() && !it.startsWith(":") }
        if (fields.isEmpty()) return null
        fun field(name: String) = fields.filter { it.startsWith("$name:") }.map { it.removePrefix("$name:").removePrefix(" ") }
        return Event(field("id").firstOrNull()?.toLong(), field("retry").firstOrNull()?.toLong(), field("data").joinToString("\n"))
    }

    /** Reads the next event with an id from a streaming body, skipping comments and the empty flush event. */
    private suspend fun ByteReadChannel.nextEvent(): Event {
        while (true) {
            val lines = mutableListOf<String>()
            while (true) {
                val line = readUTF8Line() ?: error("the stream ended before an event")
                if (line.isEmpty()) break
                lines += line
            }
            eventOf(lines)?.takeIf { it.id != null }?.let { return it }
        }
    }

    private fun mcpServer(): Server = McpServerFactory.create("1.0.0-test").also { server ->
        server.addTool(name = "plain_tool", description = "answers at once") {
            CallToolResult(content = listOf(TextContent(text = "ok plain_tool")))
        }
        server.addTool(name = "slow_tool", description = "two progress notifications, then the result") { request ->
            val related = currentCoroutineContext()[RequestHandlerExtra.Key]?.requestId
            val token = request.meta?.progressToken ?: RequestId.StringId("none")
            delay(STEP_MS)
            notification(ProgressNotification(ProgressNotificationParams(token, 1.0, 3.0, "one")), related)
            delay(STEP_MS)
            notification(ProgressNotification(ProgressNotificationParams(token, 2.0, 3.0, "two")), related)
            delay(STEP_MS)
            CallToolResult(content = listOf(TextContent(text = "ok slow_tool")))
        }
        server.addTool(name = "gated_tool", description = "one progress notification, then waits for the test") { request ->
            val related = currentCoroutineContext()[RequestHandlerExtra.Key]?.requestId
            val token = request.meta?.progressToken ?: RequestId.StringId("none")
            notification(ProgressNotification(ProgressNotificationParams(token, 1.0, 3.0, "one")), related)
            gate.await()
            notification(ProgressNotification(ProgressNotificationParams(token, 2.0, 3.0, "two")), related)
            CallToolResult(content = listOf(TextContent(text = "ok gated_tool")))
        }
    }

    private val gate = CompletableDeferred<Unit>()

    /** A plain HTTP/1.1 client against the mount on a real CIO listener, as an MCP client sees it. */
    private class CioClient(port: Int) {
        private val url = URI("http://127.0.0.1:$port$PATH").toURL()

        fun open(method: String, sessionId: String?, lastEventId: String? = null): HttpURLConnection =
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", if (method == "GET") "text/event-stream" else ACCEPT)
                setRequestProperty(PROTOCOL_HEADER, NEW_PROTOCOL)
                sessionId?.let { setRequestProperty(SESSION_HEADER, it) }
                lastEventId?.let { setRequestProperty(LAST_EVENT_ID, it) }
            }

        fun post(body: String, sessionId: String?): HttpURLConnection = open("POST", sessionId).apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
        }

        fun initialize(): String {
            val init = post(initializeBody(NEW_PROTOCOL), null)
            assertEquals(200, init.responseCode)
            init.inputStream.use { it.readBytes() }
            val sessionId = init.getHeaderField(SESSION_HEADER)!!
            assertEquals(202, post(INITIALIZED, sessionId).responseCode)
            return sessionId
        }
    }

    private fun withCio(server: Server, block: (CioClient) -> Unit) {
        val engine = embeddedServer(CIO, host = "127.0.0.1", port = 0) { mcpServerModule(server, policy = { GatePolicy.loopback() }) }.start(wait = false)
        try {
            block(CioClient(runBlocking { engine.engine.resolvedConnectors().first().port }))
        } finally {
            engine.stop(100, 1_000)
        }
    }

    /** Reads lines up to the end of the first event whose lines satisfy [until]. */
    private fun java.io.BufferedReader.readUntil(until: (List<String>) -> Boolean): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = readLine() ?: error("the stream ended early")
            if (line.isEmpty() && until(lines)) return lines
            lines += line
        }
    }

    private fun ApplicationTestBuilder.mount(server: Server) {
        application { mcpServerModule(server, policy = { GatePolicy.loopback() }) }
    }

    private suspend fun ApplicationTestBuilder.post(body: String, sessionId: String?, protocol: String = NEW_PROTOCOL): HttpResponse =
        client.post(PATH) { postHeaders(sessionId, protocol); setBody(body) }

    private fun io.ktor.client.request.HttpRequestBuilder.postHeaders(sessionId: String?, protocol: String) {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Accept, ACCEPT)
        header(PROTOCOL_HEADER, protocol)
        sessionId?.let { header(SESSION_HEADER, it) }
        header(HttpHeaders.Host, HOST)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.getHeaders(sessionId: String, lastEventId: String? = null) {
        header(HttpHeaders.Accept, "text/event-stream")
        header(PROTOCOL_HEADER, NEW_PROTOCOL)
        header(SESSION_HEADER, sessionId)
        header(HttpHeaders.Host, HOST)
        lastEventId?.let { header(LAST_EVENT_ID, it) }
    }

    private suspend fun ApplicationTestBuilder.initialize(protocol: String = NEW_PROTOCOL): String {
        val init = post(initializeBody(protocol), sessionId = null, protocol = protocol)
        assertEquals(HttpStatusCode.OK, init.status)
        val sessionId = init.headers[SESSION_HEADER]!!
        assertEquals(HttpStatusCode.Accepted, post(INITIALIZED, sessionId, protocol).status)
        return sessionId
    }

    @Test fun newClientsGetAPrimingEventWithARetryAndEveryEventCarriesAnId() = testApplication {
        mount(mcpServer())
        val sessionId = initialize()
        val events = parse(post(SLOW_CALL, sessionId).bodyAsText())
        val priming = events.first()
        assertNotNull("the priming event has an id", priming.id)
        assertEquals(RETRY_INTERVAL_MS, priming.retry)
        assertTrue("the priming event carries no message", priming.data.isBlank())
        val messages = events.drop(1)
        assertEquals(listOf("notifications/progress", "notifications/progress", null), messages.map { it.json!!["method"]?.jsonPrimitive?.contentOrNull })
        val ids = events.map { it.id!! }
        assertEquals("ids increase within the stream", ids.sorted(), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun olderClientsGetIdsButNoPrimingEvent() = testApplication {
        mount(mcpServer())
        val sessionId = initialize(OLD_PROTOCOL)
        val events = parse(post(PLAIN_CALL, sessionId, OLD_PROTOCOL).bodyAsText())
        assertEquals(1, events.size)
        assertNull(events.single().retry)
        assertNotNull(events.single().id)
        assertEquals(3, events.single().json!!["id"]!!.jsonPrimitive.int)
    }

    @Test fun anUnknownLastEventIdIsRefusedBeforeTheStreamStarts() = testApplication {
        mount(mcpServer())
        val sessionId = initialize()
        val response = client.get(PATH) { getHeaders(sessionId, lastEventId = "999999") }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains(MESSAGE_UNKNOWN_EVENT_ID))
    }

    @Test fun anEndedStreamIsReplayedFromTheStoreAndTheGetEnds() = testApplication {
        mount(mcpServer())
        val sessionId = initialize()
        val primingId = parse(post(PLAIN_CALL, sessionId).bodyAsText()).first().id!!
        val replay = client.get(PATH) { getHeaders(sessionId, lastEventId = primingId.toString()) }
        assertEquals(HttpStatusCode.OK, replay.status)
        val events = parse(replay.bodyAsText())
        assertEquals(listOf(3), events.map { it.json!!["id"]!!.jsonPrimitive.int })
        assertTrue(events.single().id!! > primingId)
    }

    @Test fun theStandaloneStreamResumesThroughTheSdkAndStaysOpen() = testApplication {
        val server = mcpServer()
        mount(server)
        val sessionId = initialize()
        val primingId = client.prepareGet(PATH) { getHeaders(sessionId) }.execute { it.bodyAsChannel().nextEvent().id!! }
        client.prepareGet(PATH) { getHeaders(sessionId, lastEventId = primingId.toString()) }.execute { resumed ->
            assertEquals(HttpStatusCode.OK, resumed.status)
            val body = resumed.bodyAsChannel()
            // The SDK registers the resumed stream only after its replay, a moment after the headers;
            // a notification sent in between is stored but not delivered, so keep sending until one arrives.
            val event = coroutineScope {
                val sender = launch {
                    while (true) {
                        server.sendToolListChanged(server.sessions.keys.single())
                        delay(200)
                    }
                }
                try {
                    withTimeout(5_000) { body.nextEvent() }
                } finally {
                    sender.cancel()
                }
            }
            assertEquals("notifications/tools/list_changed", event.json!!["method"]!!.jsonPrimitive.content)
            assertTrue(event.id!! > primingId)
        }
    }

    @Test fun aGetWithLastEventIdTakesOverARunningPostStreamOnTheCioEngine() = withCio(mcpServer()) { client ->
        val sessionId = client.initialize()
        val call = client.post(GATED_CALL, sessionId)
        assertEquals(200, call.responseCode)
        val postReader = call.inputStream.bufferedReader()
        val primingId = eventOf(postReader.readUntil { lines -> lines.any { it.startsWith("id:") } })!!.id!!
        postReader.readUntil { lines -> lines.any { it.contains("\"one\"") } }
        val resumed = CompletableFuture.supplyAsync {
            client.open("GET", sessionId, lastEventId = primingId.toString()).let { get ->
                assertEquals(200, get.responseCode)
                get.inputStream.bufferedReader().readText()
            }
        }
        // The tool waits until the GET had time to take the stream over, then finishes.
        Thread.sleep(TAKEOVER_MS)
        gate.complete(Unit)
        val events = parse(resumed.get(5, TimeUnit.SECONDS))
        assertEquals(listOf("one", "two", null), events.map { it.json!!["params"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull })
        assertEquals(9, events.last().json!!["id"]!!.jsonPrimitive.int)
        assertTrue(events.all { it.id!! > primingId })
        // The POST body ended when the GET took the stream over: no event after "one" went there.
        assertTrue(parse(postReader.readText()).none { it.json != null })
    }


    @Test fun aClientThatDroppedThePostResumesItOnTheCioEngine() = withCio(mcpServer()) { client ->
        val sessionId = client.initialize()
        val call = client.post(SLOW_CALL, sessionId)
        assertEquals(200, call.responseCode)
        val primingId = eventOf(call.inputStream.bufferedReader().readUntil { lines -> lines.any { it.startsWith("id:") } })!!.id!!
        call.disconnect()

        // Both progress notifications are emitted while no client listens; the result comes later.
        Thread.sleep(STEP_MS * 2 + STEP_MS / 2)
        val resume = client.open("GET", sessionId, lastEventId = primingId.toString())
        assertEquals(200, resume.responseCode)
        val events = parse(resume.inputStream.bufferedReader().readText())
        assertEquals(listOf("notifications/progress", "notifications/progress", null), events.map { it.json!!["method"]?.jsonPrimitive?.contentOrNull })
        assertEquals(listOf("one", "two"), events.take(2).map { it.json!!["params"]!!.jsonObject["message"]!!.jsonPrimitive.content })
        assertEquals(7, events.last().json!!["id"]!!.jsonPrimitive.int)
        assertTrue(events.all { it.id!! > primingId })
    }


    private companion object {
        const val PATH = "/mcp"
        const val HOST = "localhost"
        const val ACCEPT = "application/json, text/event-stream"
        const val PROTOCOL_HEADER = "MCP-Protocol-Version"
        const val SESSION_HEADER = "Mcp-Session-Id"
        const val LAST_EVENT_ID = "Last-Event-ID"
        const val NEW_PROTOCOL = "2025-11-25"
        const val OLD_PROTOCOL = "2025-06-18"
        const val STEP_MS = 200L
        const val INITIALIZED = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""
        const val PLAIN_CALL = """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"plain_tool","arguments":{}}}"""
        const val TAKEOVER_MS = 300L
        const val GATED_CALL = """{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"gated_tool","arguments":{},"_meta":{"progressToken":"g"}}}"""
        const val SLOW_CALL = """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"slow_tool","arguments":{},"_meta":{"progressToken":"p"}}}"""

        fun initializeBody(protocol: String) =
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"$protocol","capabilities":{},"clientInfo":{"name":"SseResumptionTest","version":"1"}}}"""
    }
}
