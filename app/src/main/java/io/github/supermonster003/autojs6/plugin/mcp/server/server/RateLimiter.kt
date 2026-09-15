package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog

/**
 * The per-client ceilings of the listener (roadmap P6): how many requests a client may send per
 * second and how many screenshots it may take per minute. Both are sliding windows, so a burst
 * up to the limit is fine and the `retryAfterMs` of a refusal is exact. The host grant keeps its
 * own accessibility query rate on top of these.
 */
data class RateLimits(
    val requestsPerWindow: Int = REQUESTS_PER_SECOND,
    val requestWindowMs: Long = 1_000L,
    val screenshotsPerWindow: Int = SCREENSHOTS_PER_MINUTE,
    val screenshotWindowMs: Long = 60_000L,
    /** Clients tracked at once; the least recently seen one is dropped beyond this. */
    val maxTrackedClients: Int = MAX_TRACKED_CLIENTS,
    /** A client silent for this long is forgotten (its windows are empty by then anyway). */
    val idleForgetMs: Long = IDLE_FORGET_MS,
) {

    init {
        require(requestsPerWindow > 0 && screenshotsPerWindow > 0) { "limits must be positive" }
        require(requestWindowMs > 0 && screenshotWindowMs > 0) { "windows must be positive" }
        require(maxTrackedClients > 0) { "maxTrackedClients must be positive" }
    }

    companion object {

        const val REQUESTS_PER_SECOND = 20
        const val SCREENSHOTS_PER_MINUTE = 30
        const val MAX_TRACKED_CLIENTS = 256
        const val IDLE_FORGET_MS = 5L * 60L * 1_000L
    }
}

/** The answer of the limiter: [retryAfterMs] is 0 when [allowed]. */
data class RateVerdict(val allowed: Boolean, val retryAfterMs: Long, val limit: Int, val windowMs: Long) {

    /** The `Retry-After` header value: whole seconds, rounded up, at least one. */
    val retryAfterSeconds: Long
        get() = ((retryAfterMs + 999L) / 1_000L).coerceAtLeast(1L)
}

/**
 * Sliding-window counters keyed by client (roadmap P6). [clock] returns monotonic milliseconds;
 * the default is the JVM's monotonic clock so that the JVM tests can inject their own. All
 * state is guarded by the limiter itself: a request costs one map lookup and a deque trim.
 */
class RateLimiter(
    val limits: RateLimits = RateLimits(),
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
) {

    private class Client {
        val requests = ArrayDeque<Long>()
        val screenshots = ArrayDeque<Long>()
        var lastSeenAt = 0L
    }

    private val clients = LinkedHashMap<String, Client>()

    /** Clients with a window on record; for tests and diagnostics. */
    val trackedClientCount: Int
        get() = synchronized(this) { clients.size }

    fun acquireRequest(key: String): RateVerdict = acquire(key, screenshot = false)

    fun acquireScreenshot(key: String): RateVerdict = acquire(key, screenshot = true)

    /** Drops the windows of [key], for example when its session closed. */
    fun forget(key: String) {
        synchronized(this) { clients.remove(key) }
    }

    private fun acquire(key: String, screenshot: Boolean): RateVerdict = synchronized(this) {
        val now = clock()
        val limit = if (screenshot) limits.screenshotsPerWindow else limits.requestsPerWindow
        val window = if (screenshot) limits.screenshotWindowMs else limits.requestWindowMs
        prune(now, keep = key)
        val client = clients.remove(key) ?: Client()
        clients[key] = client // re-inserted last: the map is in least-recently-seen order
        client.lastSeenAt = now
        val stamps = if (screenshot) client.screenshots else client.requests
        while (stamps.isNotEmpty() && now - stamps.first() >= window) stamps.removeFirst()
        if (stamps.size >= limit) {
            val retryAfter = (stamps.first() + window - now).coerceAtLeast(1L)
            return RateVerdict(allowed = false, retryAfterMs = retryAfter, limit = limit, windowMs = window)
        }
        stamps.addLast(now)
        RateVerdict(allowed = true, retryAfterMs = 0L, limit = limit, windowMs = window)
    }

    /** Forgets idle clients and, when a new one needs room above the ceiling, the least recently seen ones. */
    private fun prune(now: Long, keep: String) {
        val iterator = clients.entries.iterator()
        while (iterator.hasNext()) {
            val (key, client) = iterator.next()
            if (key == keep) continue
            if (now - client.lastSeenAt >= limits.idleForgetMs) iterator.remove()
        }
        val needsRoom = !clients.containsKey(keep)
        while (needsRoom && clients.size >= limits.maxTrackedClients) {
            val oldest = clients.keys.firstOrNull { it != keep } ?: break
            clients.remove(oldest)
        }
    }

    companion object {

        /** Tools that count against the screenshot window. */
        val SCREENSHOT_TOOLS: Set<String> = setOf(ToolCatalog.SCREEN_CAPTURE)
    }
}
