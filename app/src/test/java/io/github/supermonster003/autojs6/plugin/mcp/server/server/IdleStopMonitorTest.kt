package io.github.supermonster003.autojs6.plugin.mcp.server.server

import org.junit.Assert.assertEquals
import org.junit.Test

class IdleStopMonitorTest {

    private var now = 1_000L
    private val monitor = IdleStopMonitor { now }

    @Test
    fun theCountdownRunsFromTheLastActivity() {
        assertEquals(60_000L, monitor.remainingMs(60_000L))
        now += 20_000L
        assertEquals(40_000L, monitor.remainingMs(60_000L))
        now += 50_000L
        assertEquals(0L, monitor.remainingMs(60_000L))
        assertEquals(0L, monitor.remainingMs(10_000L))
    }

    @Test
    fun aTouchRestartsTheCountdown() {
        now += 59_000L
        monitor.touch()
        assertEquals(now, monitor.lastActivityAt)
        assertEquals(60_000L, monitor.remainingMs(60_000L))
        now += 60_000L
        assertEquals(0L, monitor.remainingMs(60_000L))
    }

    @Test
    fun anInFlightRequestIsNeverIdleAndItsEndCountsAsActivity() {
        monitor.begin()
        assertEquals(1, monitor.inFlightCount)
        now += 90_000L
        assertEquals(60_000L, monitor.remainingMs(60_000L))
        monitor.begin()
        monitor.end()
        assertEquals(1, monitor.inFlightCount)
        assertEquals(60_000L, monitor.remainingMs(60_000L))
        monitor.end()
        assertEquals(0, monitor.inFlightCount)
        assertEquals(now, monitor.lastActivityAt)
        assertEquals(60_000L, monitor.remainingMs(60_000L))
        now += 60_000L
        assertEquals(0L, monitor.remainingMs(60_000L))
    }
}
