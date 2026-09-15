package io.github.supermonster003.autojs6.plugin.mcp.server.server

import java.util.concurrent.atomic.AtomicInteger

/**
 * The activity the idle auto-stop option counts (roadmap P6): an authenticated `POST` from its
 * arrival to the end of its response, so a running tool call or a script that streams progress
 * never counts as idle, and any other authenticated request (the `GET` notification stream, a
 * `DELETE`) when it arrives. A client that keeps its stream open without sending requests, or a
 * PC that went to sleep without closing it, therefore does not keep the listener alive, and
 * unauthenticated traffic never counts because the marker sits behind the bearer check.
 *
 * Plain state on a monotonic [clock] so the JVM tests cover it; the runtime schedules the check
 * with a delayed main-thread message and never wakes the device for it.
 */
class IdleStopMonitor(private val clock: () -> Long) {

    /** Clock reading of the last counted activity. */
    @Volatile
    var lastActivityAt: Long = clock()
        private set

    private val inFlight = AtomicInteger()

    /** Requests whose response has not completed yet. */
    val inFlightCount: Int
        get() = inFlight.get()

    fun touch() {
        lastActivityAt = clock()
    }

    fun begin() {
        inFlight.incrementAndGet()
        touch()
    }

    fun end() {
        inFlight.decrementAndGet()
        touch()
    }

    /**
     * Milliseconds until the listener has seen no activity for [timeoutMs]: 0 once it has, and
     * a full [timeoutMs] while a request is in flight (its end counts as activity).
     */
    fun remainingMs(timeoutMs: Long): Long =
        if (inFlight.get() > 0) timeoutMs else (lastActivityAt + timeoutMs - clock()).coerceAtLeast(0L)
}
