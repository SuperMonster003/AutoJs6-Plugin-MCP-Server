package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.response.header
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.RPCError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * Mounts the SDK's stateful Streamable HTTP transport with server-sent-event responses (roadmap
 * P3.1). The SDK's own `mcpStreamableHttp` hard-codes JSON responses, and in that mode a
 * notification that belongs to a request, such as the progress heartbeat of `script_run`, can
 * only travel on the standalone GET stream, which most clients never open. Here a POST that
 * carries requests is answered on a `text/event-stream` body, so related notifications precede
 * the response on the request's own stream, as the MCP specification describes. Everything else
 * (the `Mcp-Session-Id` header, the GET standalone stream, DELETE, the 202 for a POST of
 * notifications only, and the 4xx rejections) stays the transport's behaviour.
 *
 * The transport needs an SSE session before it looks at a POST but decides only afterwards
 * whether it streams, so the session here starts the response body on its first event and is
 * never started when the transport answered the POST on its own. The transport also returns
 * from the POST before the dispatched handlers answer and ties its stream bookkeeping to the
 * call's lifetime, so the route stays in the call until the transport closes the session and
 * the body is written out (an engine may return from `respond` before that).
 *
 * The transport keys the response streams of a session by request id, so a second POST that
 * reuses an id still in flight would take over the mapping and leave the first POST without an
 * answer (its stream would stay open until the client gives up). Such a POST is refused with
 * `400` before it reaches the transport (roadmap P6); ids are free again once their POST's
 * body was written out.
 */
internal fun Application.mcpStreamableSse(path: String, block: () -> Server) {
    pluginOrNull(SSE) ?: install(SSE)
    val transports = Transports()
    val inFlight = InFlightIds()
    val configuration = StreamableHttpServerTransport.Configuration(enableJsonResponse = false)
    routing {
        route(path) {
            // Ktor's SSE route commits the response headers before its handler runs, so the session
            // id of a resumed standalone stream is echoed ahead of that (as the SDK mount does), and
            // a GET without a live session is refused here with 400 / 404 instead of an empty
            // 200 event stream (roadmap P6).
            intercept(ApplicationCallPipeline.Plugins) {
                if (call.request.httpMethod == HttpMethod.Get) {
                    val sessionId = call.request.header(SESSION_ID_HEADER)
                    when {
                        sessionId.isNullOrEmpty() -> {
                            call.rejectRpc(HttpStatusCode.BadRequest, "Bad Request: No valid session ID provided")
                            finish()
                        }
                        !transports.containsKey(sessionId) -> {
                            call.rejectRpc(HttpStatusCode.NotFound, "Session not found")
                            finish()
                        }
                        else -> call.response.header(SESSION_ID_HEADER, sessionId)
                    }
                }
            }
            sse {
                val transport = existingTransport(call, transports) ?: return@sse
                transport.handleRequest(this, call)
            }
            post {
                val transport = transportFor(transports, configuration, inFlight, block) ?: return@post
                val sessionId = transport.sessionId
                val ids = if (sessionId == null) emptyList() else call.attributes.getOrNull(JSON_RPC_CALLS)?.ids?.filterNotNull()?.map { it.toString() }.orEmpty()
                val claimed = if (sessionId == null) null else inFlight.claim(sessionId, ids)
                if (claimed == false) {
                    call.rejectRpc(HttpStatusCode.BadRequest, MESSAGE_ID_IN_FLIGHT)
                    return@post
                }
                try {
                    coroutineScope {
                        val stream = PostResponseStream(call, coroutineContext)
                        val handling = launch {
                            try {
                                transport.handleRequest(stream, call)
                            } finally {
                                stream.finish()
                            }
                        }
                        if (stream.awaitStarted()) {
                            call.respondBytesWriter { stream.writeTo(this) }
                            stream.awaitWritten()
                        }
                        handling.join()
                    }
                } finally {
                    if (sessionId != null) inFlight.release(sessionId, ids)
                }
            }
            delete {
                val transport = existingTransport(call, transports) ?: return@delete
                transport.handleRequest(null, call)
            }
        }
    }
}

/** The live transports by session id (the SDK's own registry of them is internal). */
private typealias Transports = ConcurrentHashMap<String, StreamableHttpServerTransport>

/** The message of the `400` for a request id that is still in flight on the session. */
internal const val MESSAGE_ID_IN_FLIGHT = "Bad Request: a request with this id is still in flight on this session"

/** The request ids each session is still answering (roadmap P6). */
private class InFlightIds {

    private val ids = ConcurrentHashMap<String, MutableSet<String>>()

    /** True when every id of [requestIds] was free and is now claimed; false (and nothing claimed) otherwise. */
    fun claim(sessionId: String, requestIds: List<String>): Boolean {
        if (requestIds.isEmpty()) return true
        val set = ids.getOrPut(sessionId) { ConcurrentHashMap.newKeySet() }
        synchronized(set) {
            if (requestIds.any { it in set }) return false
            set.addAll(requestIds)
        }
        return true
    }

    fun release(sessionId: String, requestIds: List<String>) {
        if (requestIds.isEmpty()) return
        val set = ids[sessionId] ?: return
        synchronized(set) { set.removeAll(requestIds.toSet()) }
    }

    fun forget(sessionId: String) {
        ids.remove(sessionId)
    }
}

/** The transport of the `Mcp-Session-Id` header, or null after a 400 / 404 was sent. */
private suspend fun existingTransport(call: ApplicationCall, transports: Transports): StreamableHttpServerTransport? {
    val sessionId = call.request.header(SESSION_ID_HEADER)
    if (sessionId.isNullOrEmpty()) {
        call.rejectRpc(HttpStatusCode.BadRequest, "Bad Request: No valid session ID provided")
        return null
    }
    return transports[sessionId] ?: run {
        call.rejectRpc(HttpStatusCode.NotFound, "Session not found")
        null
    }
}

/** The transport of an existing session, or a new one (registered once `initialize` names it). */
private suspend fun RoutingContext.transportFor(
    transports: Transports,
    configuration: StreamableHttpServerTransport.Configuration,
    inFlight: InFlightIds,
    block: () -> Server,
): StreamableHttpServerTransport? {
    val sessionId = call.request.header(SESSION_ID_HEADER)
    if (sessionId != null) return transports[sessionId] ?: existingTransport(call, transports)
    val transport = StreamableHttpServerTransport(configuration)
    transport.setOnSessionInitialized { id -> transports[id] = transport }
    transport.setOnSessionClosed { id ->
        transports.remove(id, transport)
        inFlight.forget(id)
    }
    val server = block()
    server.onClose {
        transport.sessionId?.let {
            transports.remove(it, transport)
            inFlight.forget(it)
        }
    }
    server.createSession(transport)
    return transport
}

private suspend fun ApplicationCall.rejectRpc(status: HttpStatusCode, message: String) {
    respondText(McpErrors.error(null, RPCError.ErrorCode.CONNECTION_CLOSED, message).toString(), ContentType.Application.Json, status)
}

/**
 * The SSE session a POST response is written into. The body starts with the transport's first
 * event, after it appended the stream headers; a first event without content only flushes those
 * headers and goes out as an SSE comment, not as an empty message a client would try to parse.
 * The transport dispatches the requests asynchronously and returns from the POST before the
 * handlers answer, so nothing but its own [close] (after the last response, or when the
 * transport shuts down) ends the body.
 */
private class PostResponseStream(
    override val call: ApplicationCall,
    override val coroutineContext: CoroutineContext,
) : ServerSSESession {

    private val chunks = Channel<String>(Channel.UNLIMITED)
    private val started = CompletableDeferred<Boolean>()
    private val written = CompletableDeferred<Unit>()

    override suspend fun send(event: ServerSentEvent) {
        started.complete(true)
        if (chunks.trySend(event.wireFormat()).isClosed) error("the response stream of this POST is closed")
    }

    override suspend fun close() {
        chunks.close()
    }

    /** True once the transport streams; false when it answered the POST without a stream. */
    suspend fun awaitStarted(): Boolean = started.await()

    /** Suspends until the body was written out (or the writer failed with the client gone). */
    suspend fun awaitWritten(): Unit = written.await()

    /** Called when the transport returned from the POST: without a first event it answered on its own. */
    fun finish() {
        started.complete(false)
    }

    suspend fun writeTo(channel: ByteWriteChannel) {
        try {
            for (chunk in chunks) {
                channel.writeStringUtf8(chunk)
                channel.flush()
            }
        } finally {
            written.complete(Unit)
        }
    }
}

private fun ServerSentEvent.wireFormat(): String = buildString {
    val hasContent = !id.isNullOrEmpty() || !event.isNullOrEmpty() || retry != null || !data.isNullOrEmpty() || !comments.isNullOrEmpty()
    if (!hasContent) {
        append(":\n\n")
        return@buildString
    }
    comments?.lineSequence()?.forEach { append(": ").append(it).append('\n') }
    id?.let { append("id: ").append(it).append('\n') }
    event?.let { append("event: ").append(it).append('\n') }
    retry?.let { append("retry: ").append(it).append('\n') }
    data?.lineSequence()?.forEach { append("data: ").append(it).append('\n') }
    append('\n')
}

private const val SESSION_ID_HEADER = "Mcp-Session-Id"
