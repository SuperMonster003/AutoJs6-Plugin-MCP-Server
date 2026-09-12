package io.github.supermonster003.autojs6.plugin.mcp.server.bridge

import org.autojs.plugin.mcp.server.api.McpServerContract
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What the host's `getBrokerInfo()` Bundle says about one session (protocol document "Broker
 * info"). Pure so tests and the pure client can use it; [BinderBridgeTransport] reads the Bundle.
 */
data class BrokerInfo(
    val contractVersion: Int = McpServerContract.CONTRACT_VERSION,
    val brokerVersion: Int = McpServerContract.HOST_CAPABILITY_BROKER_CONTRACT_VERSION,
    val brokerId: String? = null,
    val engineInfoJson: String? = null,
    val modules: Set<String> = emptySet(),
    val methods: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet(),
    val maxRequestBytes: Int = McpServerContract.MAX_BRIDGE_INLINE_JSON_BYTES,
    val maxConcurrentCalls: Int = McpServerContract.MAX_CONCURRENT_TOOL_CALLS,
    val accessibilityQueriesPerSecond: Int = 0,
    val defaultTimeoutMs: Long = McpServerContract.DEFAULT_TOOL_TIMEOUT_MS,
    val maxTimeoutMs: Long = McpServerContract.MAX_TOOL_TIMEOUT_MS,
) {

    /** An empty method list means the host did not say; the broker enforces the grant anyway. */
    fun isGranted(module: String, method: String): Boolean = methods.isEmpty() || "$module.$method" in methods

    val supportsBrokerContract: Boolean
        get() = McpServerContract.supportsHostCapabilityBrokerContractVersion(brokerVersion)
}

/** A payload descriptor the host attached to a response; the receiver owns and must close it. */
class BridgePayload(
    val bytes: Long,
    val mime: String?,
    private val opener: () -> InputStream,
    private val closer: () -> Unit,
) {

    private val closed = AtomicBoolean(false)

    /** Reads an exact, bounded file payload off the Binder callback thread. The caller owns closing it. */
    fun read(): BridgeBytes {
        require(bytes in 1L..McpServerContract.MAX_BRIDGE_PAYLOAD_BYTES.toLong()) { "invalid host payload length: $bytes" }
        check(!closed.get()) { "host payload is already closed" }
        val data = ByteArray(bytes.toInt())
        opener().use { input ->
            var offset = 0
            while (offset < data.size) {
                val count = input.read(data, offset, data.size - offset)
                check(count > 0) { "host payload ended before its declared length" }
                offset += count
            }
            check(input.read() == -1) { "host payload exceeds its declared length" }
        }
        return BridgeBytes(data, mime)
    }

    fun close() { if (closed.compareAndSet(false, true)) runCatching { closer() } }
}

/** In-memory payload with no descriptor ownership left to the tool. Never included in ordinary logs. */
class BridgeBytes(val data: ByteArray, val mime: String?)

/** One `onResponse` as the transport saw it: the JSON plus the Bundle mirrors. */
class BridgeReply(
    val responseJson: String?,
    val ok: Boolean?,
    val errorMessage: String?,
    val payload: BridgePayload? = null,
)

/**
 * The Binder boundary of [HostBridgeClient], abstracted so the client and its tests stay pure:
 * [BinderBridgeTransport] talks to `IMcpHostCapabilityBroker`, the JVM tests use a fake.
 */
interface BridgeTransport {

    val brokerInfo: BrokerInfo

    /** Sends one request; [onReply] is invoked exactly once, possibly on another thread. Throws when the host is gone. */
    fun dispatch(request: BridgeRequest, clientName: String?, onReply: (BridgeReply) -> Unit)

    /** Registers [onDeath] for the host process; false when the host is already dead. */
    fun linkToDeath(onDeath: () -> Unit): Boolean

    fun unlinkToDeath()
}
