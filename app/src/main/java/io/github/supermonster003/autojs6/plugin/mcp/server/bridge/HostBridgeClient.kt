package io.github.supermonster003.autojs6.plugin.mcp.server.bridge

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import org.autojs.plugin.mcp.server.api.McpServerContract
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

/** Whether the host broker of the current session still answers. */
enum class HostAvailability(val id: String) {

    /** The broker is attached and alive. */
    AVAILABLE("available"),

    /** The host process died; every tool answers `HOST_UNAVAILABLE` until a new broker arrives. */
    DEGRADED("degraded"),

    /** The session was closed by the host or replaced by a newer one. */
    DETACHED("detached"),
}

/** The outcome of one bridge call as the tools consume it. */
sealed class BridgeOutcome {

    data class Ok(val result: JsonElement, val elapsedMs: Long) : BridgeOutcome()

    data class Failed(val failure: ToolFailure) : BridgeOutcome()
}

/**
 * The plugin's client of the host capability broker (roadmap P2.3): one request id sequence,
 * a per-request timeout, a concurrency ceiling, error classification, and death handling.
 *
 * The transport is abstract so the class and its tests are pure Kotlin; on Android it is a
 * [BinderBridgeTransport]. The host applies the session grant and its own timeout; the client's
 * own timeout is a safety net for a host that never answers, and a broker death fails every
 * in-flight call at once.
 */
class HostBridgeClient(
    private val transport: BridgeTransport,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onDeath: (HostBridgeClient) -> Unit = {},
) {

    val info: BrokerInfo
        get() = transport.brokerInfo

    private val maxConcurrentCalls: Int =
        info.maxConcurrentCalls.coerceIn(1, McpServerContract.MAX_CONCURRENT_TOOL_CALLS)

    private val semaphore = Semaphore(maxConcurrentCalls)
    private val nextId = AtomicLong(1L)
    private val pending = ConcurrentHashMap<String, CancellableContinuation<BridgeResponse>>()

    @Volatile
    var availability: HostAvailability = HostAvailability.AVAILABLE
        private set

    @Volatile
    private var deathHandled = false

    /** Calls made since the client was created, for status lines. */
    val callCount: Long
        get() = nextId.get() - 1L

    init {
        if (!transport.linkToDeath(::onHostDied)) {
            onHostDied()
        }
    }

    fun isGranted(module: String, method: String): Boolean = info.isGranted(module, method)

    /**
     * Dispatches `module.method(args)` and waits for the answer. [timeoutMs] is clamped to the
     * grant's ceiling; a call that finds every slot busy waits at most [QUEUE_WAIT_MS] before it
     * fails with `LIMIT_EXCEEDED`.
     */
    suspend fun call(
        module: String,
        method: String,
        args: JsonArray = JsonArray(emptyList()),
        timeoutMs: Long = info.defaultTimeoutMs,
        permissions: List<String> = emptyList(),
        clientName: String? = null,
    ): BridgeOutcome {
        if (availability != HostAvailability.AVAILABLE) {
            return BridgeOutcome.Failed(ToolFailure.hostUnavailable(unavailableMessage()))
        }
        if (!isGranted(module, method)) {
            return BridgeOutcome.Failed(
                ToolFailure(ToolErrorCodes.CAPABILITY_DENIED, "the host grant does not include $module.$method", ToolFailure.HINT_CAPABILITY, module = module, method = method),
            )
        }
        val effectiveTimeoutMs = timeoutMs.coerceIn(1L, info.maxTimeoutMs.coerceAtLeast(1L))
        if (!semaphore.tryAcquire()) {
            val acquired = withTimeoutOrNull(minOf(effectiveTimeoutMs, QUEUE_WAIT_MS)) {
                semaphore.acquire()
                true
            } ?: false
            if (!acquired) {
                return BridgeOutcome.Failed(ToolFailure.limitExceeded("$maxConcurrentCalls tool calls are already running"))
            }
        }
        try {
            val id = "$ID_PREFIX${nextId.getAndIncrement()}"
            val request = BridgeRequest(id, module, method, args, effectiveTimeoutMs, permissions)
            val startedAt = clock()
            val response = withTimeoutOrNull(effectiveTimeoutMs + GRACE_MS) {
                suspendCancellableCoroutine { continuation ->
                    pending[id] = continuation
                    continuation.invokeOnCancellation { pending.remove(id) }
                    if (availability != HostAvailability.AVAILABLE) {
                        pending.remove(id)
                        continuation.resume(BridgeResponse.Failure(id, BridgeError.processDead(unavailableMessage(), module, method)))
                        return@suspendCancellableCoroutine
                    }
                    try {
                        transport.dispatch(request, clientName) { reply -> complete(id, reply) }
                    } catch (error: Exception) {
                        if (pending.remove(id) != null) {
                            continuation.resume(BridgeResponse.Failure(id, BridgeError.processDead(error.message ?: error.javaClass.name, module, method)))
                        }
                    }
                }
            }
            pending.remove(id)
            return when (response) {
                null -> BridgeOutcome.Failed(ToolFailure.timeout(module, method, effectiveTimeoutMs))
                is BridgeResponse.Success -> BridgeOutcome.Ok(response.result, clock() - startedAt)
                is BridgeResponse.Failure -> BridgeOutcome.Failed(ToolFailure.fromBridge(response.error))
            }
        } finally {
            semaphore.release()
        }
    }

    /** Marks the session closed; in-flight calls fail and later ones answer `HOST_UNAVAILABLE`. */
    fun detach() {
        availability = HostAvailability.DETACHED
        transport.unlinkToDeath()
        failPending("the MCP session was closed")
    }

    private fun complete(id: String, reply: BridgeReply) {
        // Payload descriptors are not consumed by any P2.3 tool; close them so the host's file is released.
        reply.payload?.close()
        val continuation = pending.remove(id) ?: return
        continuation.resume(BridgeResponse.parse(reply.responseJson, reply.ok, reply.errorMessage))
    }

    private fun onHostDied() {
        if (deathHandled) return
        deathHandled = true
        if (availability == HostAvailability.AVAILABLE) {
            availability = HostAvailability.DEGRADED
        }
        failPending("AutoJs6 stopped while the call was running")
        onDeath(this)
    }

    private fun failPending(message: String) {
        val calls = pending.toMap()
        pending.clear()
        calls.forEach { (id, continuation) ->
            if (continuation.isActive) {
                continuation.resume(BridgeResponse.Failure(id, BridgeError.processDead(message)))
            }
        }
    }

    private fun unavailableMessage(): String = when (availability) {
        HostAvailability.AVAILABLE -> "AutoJs6 is not connected to the MCP server"
        HostAvailability.DEGRADED -> "AutoJs6 stopped; the MCP server is waiting for it to come back"
        HostAvailability.DETACHED -> "the AutoJs6 session was closed"
    }

    companion object {

        const val ID_PREFIX = "mcp-"

        /** How long a call waits for a free slot before `LIMIT_EXCEEDED`. */
        const val QUEUE_WAIT_MS = 10_000L

        /** Added to the host-side timeout before the client gives up on its own. */
        const val GRACE_MS = 2_000L
    }
}
