package io.github.supermonster003.autojs6.plugin.mcp.server.bridge

import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import org.autojs.plugin.mcp.server.api.IMcpHostCapabilityBroker
import org.autojs.plugin.mcp.server.api.IMcpHostCapabilityCallback
import org.autojs.plugin.mcp.server.api.McpServerContract
import java.util.concurrent.ConcurrentHashMap

/**
 * The Binder side of [HostBridgeClient] (roadmap P2.3): one `IMcpHostCapabilityBroker` of the
 * host, its broker info read once at attach time, and one callback Stub per in-flight request.
 * The Stubs stay referenced until the host answers, because the host only holds a weak proxy.
 */
class BinderBridgeTransport(private val broker: IMcpHostCapabilityBroker) : BridgeTransport {

    override val brokerInfo: BrokerInfo = brokerInfoFromBundle(runCatching { broker.brokerInfo }.getOrNull())

    private val callbacks = ConcurrentHashMap<String, IMcpHostCapabilityCallback.Stub>()

    @Volatile
    private var deathRecipient: IBinder.DeathRecipient? = null

    /** Whether the host process that owns the broker still answers a ping. */
    val isAlive: Boolean
        get() = runCatching { broker.asBinder().pingBinder() }.getOrDefault(false)

    override fun dispatch(request: BridgeRequest, clientName: String?, onReply: (BridgeReply) -> Unit) {
        val bundle = Bundle().apply {
            putInt(McpServerContract.KEY_CONTRACT_VERSION, McpServerContract.CONTRACT_VERSION)
            putString(McpServerContract.KEY_BRIDGE_REQUEST_JSON, request.toJson())
            clientName?.take(McpServerContract.MAX_CLIENT_NAME_BYTES)?.let { putString(McpServerContract.KEY_BRIDGE_CLIENT_NAME, it) }
        }
        val callback = object : IMcpHostCapabilityCallback.Stub() {
            override fun onResponse(response: Bundle?) {
                callbacks.remove(request.id)
                onReply(replyOf(response))
            }
        }
        callbacks[request.id] = callback
        try {
            broker.dispatch(bundle, callback)
        } catch (error: RemoteException) {
            callbacks.remove(request.id)
            throw error
        }
    }

    override fun linkToDeath(onDeath: () -> Unit): Boolean {
        val recipient = IBinder.DeathRecipient { onDeath() }
        return try {
            broker.asBinder().linkToDeath(recipient, 0)
            deathRecipient = recipient
            true
        } catch (_: RemoteException) {
            false
        }
    }

    override fun unlinkToDeath() {
        deathRecipient?.let { recipient -> runCatching { broker.asBinder().unlinkToDeath(recipient, 0) } }
        deathRecipient = null
    }

    private fun replyOf(bundle: Bundle?): BridgeReply {
        if (bundle == null) return BridgeReply(null, false, "the host sent an empty response")
        val descriptor: ParcelFileDescriptor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(McpServerContract.KEY_BRIDGE_PAYLOAD_FD, ParcelFileDescriptor::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(McpServerContract.KEY_BRIDGE_PAYLOAD_FD)
        }
        val payload = descriptor?.let { fd ->
            BridgePayload(
                bytes = bundle.getLong(McpServerContract.KEY_BRIDGE_PAYLOAD_BYTES, 0L),
                mime = bundle.getString(McpServerContract.KEY_BRIDGE_PAYLOAD_MIME),
                opener = {
                    // The MCP contract sends unlinked regular files. Refuse pipes before any blocking read.
                    check(fd.statSize == bundle.getLong(McpServerContract.KEY_BRIDGE_PAYLOAD_BYTES, 0L)) { "host payload must be a regular file of the declared length" }
                    ParcelFileDescriptor.AutoCloseInputStream(fd)
                },
                closer = { fd.close() },
            )
        }
        return BridgeReply(
            responseJson = bundle.getString(McpServerContract.KEY_BRIDGE_RESPONSE_JSON),
            ok = if (bundle.containsKey(McpServerContract.KEY_BRIDGE_RESPONSE_OK)) bundle.getBoolean(McpServerContract.KEY_BRIDGE_RESPONSE_OK) else null,
            errorMessage = bundle.getString(McpServerContract.KEY_BRIDGE_ERROR_MESSAGE),
            payload = payload,
        )
    }
}

/** Decodes `getBrokerInfo()`; a missing Bundle yields the contract defaults. */
fun brokerInfoFromBundle(bundle: Bundle?): BrokerInfo {
    if (bundle == null) return BrokerInfo()
    val defaults = BrokerInfo()
    return BrokerInfo(
        contractVersion = bundle.getInt(McpServerContract.KEY_CONTRACT_VERSION, defaults.contractVersion),
        brokerVersion = bundle.getInt(McpServerContract.KEY_HOST_CAPABILITY_BROKER_VERSION, defaults.brokerVersion),
        brokerId = bundle.getString(McpServerContract.KEY_HOST_CAPABILITY_BROKER_ID),
        engineInfoJson = bundle.getString(McpServerContract.KEY_BRIDGE_ENGINE_INFO),
        modules = bundle.getStringArray(McpServerContract.KEY_HOST_CAPABILITY_MODULES)?.filterNotNull()?.toSet().orEmpty(),
        methods = bundle.getStringArray(McpServerContract.KEY_GRANT_METHODS)?.filterNotNull()?.toSet().orEmpty(),
        permissions = bundle.getStringArray(McpServerContract.KEY_GRANT_PERMISSIONS)?.filterNotNull()?.toSet().orEmpty(),
        maxRequestBytes = bundle.getInt(McpServerContract.KEY_GRANT_MAX_REQUEST_BYTES, defaults.maxRequestBytes),
        maxConcurrentCalls = bundle.getInt(McpServerContract.KEY_GRANT_MAX_CONCURRENT_CALLS, defaults.maxConcurrentCalls),
        accessibilityQueriesPerSecond = bundle.getInt(McpServerContract.KEY_GRANT_ACCESSIBILITY_QUERIES_PER_SECOND, defaults.accessibilityQueriesPerSecond),
        defaultTimeoutMs = bundle.getLong(McpServerContract.KEY_GRANT_DEFAULT_TIMEOUT_MS, defaults.defaultTimeoutMs),
        maxTimeoutMs = bundle.getLong(McpServerContract.KEY_GRANT_MAX_TIMEOUT_MS, defaults.maxTimeoutMs),
    )
}
