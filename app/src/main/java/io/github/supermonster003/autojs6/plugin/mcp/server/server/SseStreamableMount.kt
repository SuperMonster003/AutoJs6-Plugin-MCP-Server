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
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.RPCError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

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
 * call's lifetime, so the route stays in the call until the transport closes the stream.
 *
 * The transport keys the response streams of a session by request id, so a second POST that
 * reuses an id still in flight would take over the mapping and leave the first POST without an
 * answer (its stream would stay open until the client gives up). Such a POST is refused with
 * `400` before it reaches the transport (roadmap P6); ids are free again once their stream closed.
 *
 * Resumability (roadmap P9, SEP-1699): every event carries an id from the session's
 * [SessionEventStore], clients of protocol `2025-11-25` or newer first receive a priming event
 * with an id and [RETRY_INTERVAL_MS], and a client that lost a stream reconnects with a GET that
 * carries `Last-Event-ID`. The transport only stores events while the stream's call is alive, so a
 * POST whose client went away keeps its call (at most [DETACHED_STREAM_LIMIT_MS]) until the
 * transport closes the stream; a GET that resumes it receives the kept events after its id and
 * then takes over the stream, including the final response. A stream that already ended is
 * replayed from the store and the GET ends. A resumed standalone stream goes through the SDK.
 */
internal fun Application.mcpStreamableSse(path: String, retention: SseEventRetention = SseEventRetention(), block: () -> Server) {
    pluginOrNull(SSE) ?: install(SSE)
    val sessions = Sessions()
    val inFlight = InFlightIds()
    routing {
        route(path) {
            // Ktor's SSE route commits the response headers before its handler runs, so the session
            // id of a resumed standalone stream is echoed ahead of that (as the SDK mount does), and
            // a GET without a live session or with a Last-Event-ID the session does not know is
            // refused here with 400 / 404 instead of an empty 200 event stream (roadmap P6, P9).
            intercept(ApplicationCallPipeline.Plugins) {
                if (call.request.httpMethod == HttpMethod.Get) {
                    val sessionId = call.request.header(SESSION_ID_HEADER)
                    val session = sessionId?.let { sessions[it] }
                    val lastEventId = call.request.header(LAST_EVENT_ID_HEADER)
                    when {
                        sessionId.isNullOrEmpty() -> {
                            call.rejectRpc(HttpStatusCode.BadRequest, "Bad Request: No valid session ID provided")
                            finish()
                        }
                        session == null -> {
                            call.rejectRpc(HttpStatusCode.NotFound, "Session not found")
                            finish()
                        }
                        lastEventId != null && session.store.streamOf(lastEventId) == null -> {
                            call.rejectRpc(HttpStatusCode.BadRequest, MESSAGE_UNKNOWN_EVENT_ID)
                            finish()
                        }
                        else -> call.response.header(SESSION_ID_HEADER, sessionId)
                    }
                }
            }
            sse {
                val session = existingSession(call, sessions) ?: return@sse
                val lastEventId = call.request.header(LAST_EVENT_ID_HEADER)
                val streamId = lastEventId?.let { session.store.streamOf(it) }
                if (lastEventId != null && streamId != null && streamId != STANDALONE_STREAM_ID) {
                    session.resume(streamId, lastEventId.toLong(), this)
                    return@sse
                }
                session.transport.handleRequest(this, call)
                // The SDK replays a resumed standalone stream and returns; its connection stays open
                // like a new standalone stream until the client leaves or the transport closes.
                if (streamId == STANDALONE_STREAM_ID) awaitCancellation()
            }
            post {
                val session = sessionFor(sessions, retention, inFlight, block) ?: return@post
                val transport = session.transport
                val sessionId = transport.sessionId
                val ids = if (sessionId == null) emptyList() else call.attributes.getOrNull(JSON_RPC_CALLS)?.ids?.filterNotNull()?.map { it.toString() }.orEmpty()
                val claimed = if (sessionId == null) null else inFlight.claim(sessionId, ids)
                if (claimed == false) {
                    call.rejectRpc(HttpStatusCode.BadRequest, MESSAGE_ID_IN_FLIGHT)
                    return@post
                }
                try {
                    coroutineScope {
                        val stream = PostResponseStream(call, coroutineContext, session)
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
                            // Whether the client read the stream to its end, went away or resumed it
                            // with a GET, the call has to outlive the stream: the transport stores and
                            // routes the stream's events only while it is alive.
                            withContext(NonCancellable) {
                                withTimeoutOrNull(DETACHED_STREAM_LIMIT_MS.milliseconds) { stream.awaitClosed() }
                            }
                        }
                        handling.join()
                    }
                } finally {
                    if (sessionId != null) inFlight.release(sessionId, ids)
                }
            }
            delete {
                val session = existingSession(call, sessions) ?: return@delete
                session.transport.handleRequest(null, call)
            }
        }
    }
}

/** How long a client waits before it reconnects a stream, announced with each priming event. */
internal const val RETRY_INTERVAL_MS = 1_000L

/** The longest a POST whose client went away keeps its call for a resume: the longest tool budget plus slack. */
internal const val DETACHED_STREAM_LIMIT_MS = 6L * 60 * 1000

/** The message of the `400` for a request id that is still in flight on the session. */
internal const val MESSAGE_ID_IN_FLIGHT = "Bad Request: a request with this id is still in flight on this session"

/** The message of the `400` for a `Last-Event-ID` the session never issued or no longer remembers. */
internal const val MESSAGE_UNKNOWN_EVENT_ID = "Bad Request: Last-Event-ID is not an event of this session"

/** The SDK's private id of the standalone GET stream (`StreamableHttpServerTransport.STANDALONE_SSE_STREAM_ID`). */
internal const val STANDALONE_STREAM_ID = "_GET_stream"

/** A live session: its transport, the events it keeps and the POST streams that are still open. */
private class McpSession(val transport: StreamableHttpServerTransport, val store: SessionEventStore) {

    val openStreams = ConcurrentHashMap<String, PostResponseStream>()

    /** Resumes POST stream [streamId] after event [sequence] on [reader], the GET that reconnects. */
    suspend fun resume(streamId: String, sequence: Long, reader: ServerSSESession) {
        val stream = openStreams[streamId]
        if (stream != null && stream.attach(reader, sequence)) {
            try {
                stream.awaitClosed()
            } finally {
                stream.detach(reader)
            }
            return
        }
        // The stream already ended: its kept events are all a client can still get.
        store.eventsAfter(streamId, sequence).forEach { reader.send(it.toEvent()) }
    }
}

/** The live sessions by session id (the SDK's own registry of transports is internal). */
private typealias Sessions = ConcurrentHashMap<String, McpSession>

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

/** The session of the `Mcp-Session-Id` header, or null after a 400 / 404 was sent. */
private suspend fun existingSession(call: ApplicationCall, sessions: Sessions): McpSession? {
    val sessionId = call.request.header(SESSION_ID_HEADER)
    if (sessionId.isNullOrEmpty()) {
        call.rejectRpc(HttpStatusCode.BadRequest, "Bad Request: No valid session ID provided")
        return null
    }
    return sessions[sessionId] ?: run {
        call.rejectRpc(HttpStatusCode.NotFound, "Session not found")
        null
    }
}

/** The session of an existing id, or a new one (registered once `initialize` names it). */
private suspend fun RoutingContext.sessionFor(
    sessions: Sessions,
    retention: SseEventRetention,
    inFlight: InFlightIds,
    block: () -> Server,
): McpSession? {
    val sessionId = call.request.header(SESSION_ID_HEADER)
    if (sessionId != null) return sessions[sessionId] ?: existingSession(call, sessions)
    val store = SessionEventStore(retention)
    val transport = StreamableHttpServerTransport(
        StreamableHttpServerTransport.Configuration(
            enableJsonResponse = false,
            eventStore = store,
            retryInterval = RETRY_INTERVAL_MS.milliseconds,
        ),
    )
    val session = McpSession(transport, store)
    val forget = { id: String ->
        if (sessions.remove(id, session)) store.clear()
        inFlight.forget(id)
    }
    transport.setOnSessionInitialized { id -> sessions[id] = session }
    transport.setOnSessionClosed { id -> forget(id) }
    val server = block()
    server.onClose { transport.sessionId?.let(forget) }
    server.createSession(transport)
    return session
}

private suspend fun ApplicationCall.rejectRpc(status: HttpStatusCode, message: String) {
    respondText(McpErrors.error(null, RPCError.ErrorCode.CONNECTION_CLOSED, message).toString(), ContentType.Application.Json, status)
}

private fun SessionEventStore.Stored.toEvent(): ServerSentEvent =
    ServerSentEvent(data = McpJson.encodeToString(JSONRPCMessage.serializer(), message), event = "message", id = sequence.toString())

/**
 * The SSE session a POST response is written into. The body starts with the transport's first
 * event, after it appended the stream headers; a first event without content only flushes those
 * headers and goes out as an SSE comment, not as an empty message a client would try to parse.
 * The transport dispatches the requests asynchronously and returns from the POST before the
 * handlers answer, so nothing but its own [close] (after the last response, or when the
 * transport shuts down) ends the stream.
 *
 * The events go to one reader at a time: the POST body, or the GET that resumed the stream after
 * the POST's client went away. Event ids increase, so the reader that takes over after the id it
 * names never sees an event twice, whether the event came from the store or from the transport.
 * Without a reader the events are dropped here; the transport has stored them for the next resume.
 */
private class PostResponseStream(
    override val call: ApplicationCall,
    override val coroutineContext: CoroutineContext,
    private val session: McpSession,
) : ServerSSESession {

    private val lock = Mutex()
    private val chunks = Channel<String>(Channel.UNLIMITED)
    private val started = CompletableDeferred<Boolean>()
    private val written = CompletableDeferred<Unit>()
    private val closed = CompletableDeferred<Unit>()
    private var streamId: String? = null
    private var postReading = true
    private var resumedBy: ServerSSESession? = null
    private var delivered = 0L

    override suspend fun send(event: ServerSentEvent) {
        started.complete(true)
        val sequence = event.id?.toLongOrNull()
        lock.withLock {
            if (sequence != null && streamId == null) {
                streamId = session.store.streamOf(event.id!!)?.also { session.openStreams[it] = this }
            }
            if (sequence != null && sequence <= delivered) return
            val reader = resumedBy
            when {
                reader != null -> try {
                    reader.send(event)
                    if (sequence != null) delivered = sequence
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    resumedBy = null
                }
                postReading -> {
                    if (chunks.trySend(event.wireFormat()).isClosed) error("the response stream of this POST is closed")
                    if (sequence != null) delivered = sequence
                }
            }
        }
    }

    override suspend fun close() {
        lock.withLock {
            postReading = false
            resumedBy = null
            chunks.close()
        }
        streamId?.let { session.openStreams.remove(it, this) }
        closed.complete(Unit)
    }

    /**
     * Hands the stream to [reader] after event [sequence]: the kept events after it first, then
     * whatever the transport emits. False when the stream already ended.
     */
    suspend fun attach(reader: ServerSSESession, sequence: Long): Boolean = lock.withLock {
        val id = streamId
        if (closed.isCompleted || id == null) return false
        postReading = false
        chunks.close()
        resumedBy = null
        delivered = sequence
        for (stored in session.store.eventsAfter(id, sequence)) {
            reader.send(stored.toEvent())
            delivered = stored.sequence
        }
        resumedBy = reader
        true
    }

    /** The GET that resumed the stream is gone; the stream waits for the next one. */
    suspend fun detach(reader: ServerSSESession) {
        lock.withLock { if (resumedBy === reader) resumedBy = null }
    }

    /** True once the transport streams; false when it answered the POST without a stream. */
    suspend fun awaitStarted(): Boolean = started.await()

    /** Suspends until the POST body ended: the stream closed, a GET took it over, or the client went away. */
    suspend fun awaitWritten(): Unit = written.await()

    /** Suspends until the transport closed the stream. */
    suspend fun awaitClosed(): Unit = closed.await()

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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The client went away: later events stay in the store until a GET resumes the stream.
            lock.withLock {
                postReading = false
                chunks.close()
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
private const val LAST_EVENT_ID_HEADER = "Last-Event-ID"
