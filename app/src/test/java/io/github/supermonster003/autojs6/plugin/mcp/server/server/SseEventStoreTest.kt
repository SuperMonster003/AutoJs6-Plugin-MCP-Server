package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCEmptyMessage
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCNotification
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ids, per-session and shared budgets, and replay of the SSE event store (roadmap P9, SEP-1699). */
class SseEventStoreTest {

    /** Every test message weighs the number in its method name, so budgets are easy to reason about. */
    private fun message(size: Int, tag: String = "m"): JSONRPCMessage = JSONRPCNotification(method = "$tag/$size")

    private val retention = SseEventRetention(maxBytes = 100) { (it as? JSONRPCNotification)?.method?.substringAfter('/')?.toLong() ?: 2L }

    @Test fun idsIncreaseAcrossSessionsAndNameTheirStream() = runBlocking {
        val first = SessionEventStore(retention, maxBytes = 50, maxEvents = 10)
        val second = SessionEventStore(retention, maxBytes = 50, maxEvents = 10)
        val ids = listOf(first.storeEvent("a", message(1)), second.storeEvent("b", message(1)), first.storeEvent("a", message(1)))
        assertEquals(listOf("1", "2", "3"), ids)
        assertEquals("a", first.streamOf("3"))
        assertNull("another session's id is unknown here", first.streamOf("2"))
        assertNull(first.streamOf("not-a-number"))
    }

    @Test fun eventsAfterKeepOrderPerStreamAndSkipPrimingEvents() = runBlocking {
        val store = SessionEventStore(retention, maxBytes = 50, maxEvents = 10)
        val priming = store.storeEvent("a", JSONRPCEmptyMessage).toLong()
        store.storeEvent("a", message(1, "x"))
        store.storeEvent("b", message(1, "other"))
        store.storeEvent("a", message(1, "y"))
        assertEquals(listOf("x/1", "y/1"), store.eventsAfter("a", priming).map { (it.message as JSONRPCNotification).method })
        assertEquals(listOf("y/1"), store.eventsAfter("a", priming + 1).map { (it.message as JSONRPCNotification).method })
    }

    @Test fun eachSessionKeepsItsNewestEventsWithinItsBudget() = runBlocking {
        val counted = SessionEventStore(retention, maxBytes = 50, maxEvents = 2)
        repeat(4) { counted.storeEvent("a", message(1, "e$it")) }
        assertEquals(listOf("e2/1", "e3/1"), counted.eventsAfter("a", 0).map { (it.message as JSONRPCNotification).method })
        val sized = SessionEventStore(retention, maxBytes = 20, maxEvents = 10)
        sized.storeEvent("b", message(10, "old"))
        sized.storeEvent("b", message(10, "mid"))
        sized.storeEvent("b", message(10, "new"))
        assertEquals(listOf("mid/10", "new/10"), sized.eventsAfter("b", 0).map { (it.message as JSONRPCNotification).method })
        assertEquals(22L, retention.retainedBytes)
    }

    @Test fun anOversizedMessageGetsAnIdButIsNotKept() = runBlocking {
        val store = SessionEventStore(retention, maxBytes = 20, maxEvents = 10)
        val id = store.storeEvent("a", message(21))
        assertEquals("a", store.streamOf(id))
        assertTrue(store.eventsAfter("a", 0).isEmpty())
        assertEquals(0L, retention.retainedBytes)
    }

    @Test fun theSharedBudgetDropsTheOldestEventsOfAnySession() = runBlocking {
        // Each session stays within its own 100-byte budget; together they exceed the shared 100 bytes.
        val first = SessionEventStore(retention, maxBytes = 100, maxEvents = 10)
        val second = SessionEventStore(retention, maxBytes = 100, maxEvents = 10)
        first.storeEvent("a", message(40, "a1"))
        second.storeEvent("b", message(40, "b1"))
        second.storeEvent("b", message(40, "b2"))
        assertTrue("a1 was the oldest event of the listener", first.eventsAfter("a", 0).isEmpty())
        assertEquals(listOf("b1/40", "b2/40"), second.eventsAfter("b", 0).map { (it.message as JSONRPCNotification).method })
        assertEquals(80L, retention.retainedBytes)
        assertEquals(2, retention.retainedEvents)
    }

    @Test fun clearingASessionReleasesItsShareAndForgetsItsIds() = runBlocking {
        val store = SessionEventStore(retention, maxBytes = 60, maxEvents = 10)
        val id = store.storeEvent("a", message(30))
        store.clear()
        assertEquals(0L, retention.retainedBytes)
        assertNull(store.streamOf(id))
    }

    @Test fun theStandaloneReplayGoesThroughTheSdkInterface() = runBlocking {
        val store = SessionEventStore(retention, maxBytes = 60, maxEvents = 10)
        val priming = store.storeEvent(STANDALONE_STREAM_ID, JSONRPCEmptyMessage)
        val later = store.storeEvent(STANDALONE_STREAM_ID, message(1, "changed"))
        val replayed = mutableListOf<String>()
        assertEquals(STANDALONE_STREAM_ID, store.replayEventsAfter(priming) { eventId, _ -> replayed += eventId })
        assertEquals(listOf(later), replayed)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.replayEventsAfter("999") { _, _ -> } } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { store.getStreamIdForEventId(priming) } }
        Unit
    }
}
