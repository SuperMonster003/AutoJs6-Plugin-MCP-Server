package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.modelcontextprotocol.kotlin.sdk.server.EventStore
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCEmptyMessage
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.types.McpJson

/**
 * The budget every [SessionEventStore] of one listener shares (SEP-1699 resumability). Event ids
 * are decimal numbers from one increasing sequence, so they are unique across all streams and
 * sessions of the listener and their order is the order the transport emitted them. When the
 * events kept by all sessions exceed [maxBytes], the oldest ones go first, whichever session they
 * belong to. One lock (this object) guards the stores of a listener.
 */
internal class SseEventRetention(
    val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val estimate: (JSONRPCMessage) -> Long = ::encodedLength,
) {
    private var sequence = 0L
    private var bytes = 0L
    private val retained = HashMap<Long, SessionEventStore>()
    private val order = ArrayDeque<Long>()

    internal val retainedBytes: Long get() = synchronized(this) { bytes }

    internal val retainedEvents: Int get() = synchronized(this) { retained.size }

    internal fun sizeOf(message: JSONRPCMessage): Long = estimate(message)

    /** The next id; called under this lock. */
    internal fun next(): Long = ++sequence

    /** A store kept an event; called under this lock. Evicts the oldest events beyond the budget. */
    internal fun added(sequence: Long, size: Long, store: SessionEventStore) {
        bytes += size
        retained[sequence] = store
        order.addLast(sequence)
        while (bytes > maxBytes) {
            val oldest = order.removeFirstOrNull() ?: break
            retained[oldest]?.dropIfRetained(oldest)
        }
        // Ids a store already dropped stay in the queue until they reach its head or a compaction.
        while (order.isNotEmpty() && order.first() !in retained) order.removeFirst()
        if (order.size > 2 * retained.size + COMPACTION_SLACK) order.retainAll { it in retained }
    }

    /** A store dropped an event, whatever the reason; called under this lock. */
    internal fun removed(sequence: Long, size: Long) {
        if (retained.remove(sequence) != null) bytes -= size
    }

    companion object {
        /** Twice the largest descriptor payload of the host contract (`MAX_BRIDGE_PAYLOAD_BYTES`). */
        const val DEFAULT_MAX_BYTES = 16L * 1024 * 1024
        private const val COMPACTION_SLACK = 1024

        /** Characters of the encoded message: the base64 and ASCII JSON that dominate are one byte each. */
        fun encodedLength(message: JSONRPCMessage): Long =
            if (message === JSONRPCEmptyMessage) 2L else McpJson.encodeToString(JSONRPCMessage.serializer(), message).length.toLong()
    }
}

/**
 * The events of one session, kept so that a client can resume a dropped stream with
 * `Last-Event-ID` (roadmap P9, SEP-1699). The transport stores every event it emits on a stream
 * that still has a call; this store keeps the newest [maxEvents] of them within [maxBytes], and a
 * single message larger than [maxBytes] gets an id but is not kept. The stream of a recent id stays
 * known after its message was dropped, so a resumed stream still finds its owner.
 */
internal class SessionEventStore(
    private val retention: SseEventRetention,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val maxEvents: Int = DEFAULT_MAX_EVENTS,
) : EventStore {

    internal class Stored(val sequence: Long, val streamId: String, val message: JSONRPCMessage, val size: Long)

    private val events = ArrayDeque<Stored>()
    private val streams = object : LinkedHashMap<Long, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>?): Boolean = size > MAX_KNOWN_IDS
    }
    private var bytes = 0L

    override suspend fun storeEvent(streamId: String, message: JSONRPCMessage): String = record(streamId, message).toString()

    internal fun record(streamId: String, message: JSONRPCMessage): Long {
        val size = retention.sizeOf(message)
        synchronized(retention) {
            val sequence = retention.next()
            streams[sequence] = streamId
            if (size <= maxBytes) {
                events.addLast(Stored(sequence, streamId, message, size))
                bytes += size
                while (events.size > maxEvents || bytes > maxBytes) drop(events.removeFirst())
                retention.added(sequence, size, this)
            }
            return sequence
        }
    }

    /** The stream an id of this session was issued for, or null for an unknown or long forgotten id. */
    fun streamOf(eventId: String): String? = eventId.toLongOrNull()?.let { synchronized(retention) { streams[it] } }

    /** The kept events of [streamId] after [sequence], in order; priming events carry no message and are skipped. */
    fun eventsAfter(streamId: String, sequence: Long): List<Stored> = synchronized(retention) {
        events.filter { it.streamId == streamId && it.sequence > sequence && it.message !== JSONRPCEmptyMessage }
    }

    /**
     * The SDK replays only the standalone GET stream through here (the mount resumes POST streams
     * itself, see `mcpStreamableSse`); the mount has already refused ids this session does not know.
     */
    override suspend fun replayEventsAfter(lastEventId: String, sender: suspend (eventId: String, message: JSONRPCMessage) -> Unit): String {
        val sequence = lastEventId.toLongOrNull() ?: throw IllegalArgumentException("Unknown event id")
        val streamId = streamOf(lastEventId) ?: throw IllegalArgumentException("Unknown event id")
        eventsAfter(streamId, sequence).forEach { sender(it.sequence.toString(), it.message) }
        return streamId
    }

    /**
     * Unsupported on purpose: the SDK would answer an unknown id or a busy stream after the event
     * stream headers went out. The mount refuses unknown ids before that, and a standalone stream
     * that reconnects replaces its predecessor instead of being refused.
     */
    override suspend fun getStreamIdForEventId(eventId: String): String? = throw UnsupportedOperationException()

    /** Releases everything this session kept, when the session ends. */
    fun clear() {
        synchronized(retention) {
            while (events.isNotEmpty()) drop(events.removeFirst())
            streams.clear()
        }
    }

    /** Called by the shared budget under its lock. */
    internal fun dropIfRetained(sequence: Long) {
        val index = events.indexOfFirst { it.sequence == sequence }
        if (index >= 0) drop(events.removeAt(index))
    }

    private fun drop(stored: Stored) {
        bytes -= stored.size
        retention.removed(stored.sequence, stored.size)
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 4L * 1024 * 1024
        const val DEFAULT_MAX_EVENTS = 512
        const val MAX_KNOWN_IDS = 4096
    }
}
