package io.github.supermonster003.autojs6.plugin.mcp.server.host

import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostAvailability
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostBridgeClient
import org.autojs.plugin.mcp.server.api.IMcpServerCallback
import org.autojs.plugin.mcp.server.api.IMcpServerSession

/**
 * One open `IMcpServerSession` (roadmap P2.3): the host's view of the listener. Every method is
 * a Binder call from the host; the [verifier] (absent only in the in-process tests) confirms the
 * caller is still the host UID that opened the session. Status and events flow back through the
 * host's oneway [callback]; a dead callback is ignored, the next `openServer` replaces the session.
 */
internal class HostSession(
    private val runtime: McpServerRuntime,
    val id: String,
    val ownerUid: Int,
    @Volatile var config: SessionConfig,
    private val callback: IMcpServerCallback?,
    val bridge: HostBridgeClient,
    private val verifier: HostCallerVerifier?,
) : IMcpServerSession.Stub() {

    @Volatile
    var isOpen: Boolean = true
        private set

    /** Whether the host that opened the session still answers (its broker is alive and attached). */
    val isHostAlive: Boolean
        get() = isOpen && bridge.availability == HostAvailability.AVAILABLE && (callback?.asBinder()?.pingBinder() ?: true)

    override fun getStatus(): Bundle {
        verify()
        return HostBundles.status(runtime.statusSnapshot())
    }

    override fun updateConfig(config: Bundle?) {
        verify()
        runtime.updateSession(this, HostBundles.sessionConfig(config))
    }

    override fun stop(reason: Bundle?) {
        verify()
        runtime.stopFromHost(this, HostBundles.reasonCode(reason), HostBundles.reasonMessage(reason))
    }

    override fun close() {
        verify()
        runtime.closeSession(this)
    }

    /** Sends the current status through the callback; nothing happens once the session is closed. */
    fun publishStatus() {
        if (!isOpen) return
        val target = callback ?: return
        try {
            target.onStatus(HostBundles.status(runtime.statusSnapshot()))
        } catch (error: RemoteException) {
            Log.w(TAG, "Status not delivered to the host (${error.javaClass.simpleName})")
        }
    }

    fun publishEvent(type: String, text: String?, clientName: String? = null, toolName: String? = null) {
        if (!isOpen) return
        val target = callback ?: return
        try {
            target.onEvent(HostBundles.event(type, text, clientName, toolName, System.currentTimeMillis()))
        } catch (error: RemoteException) {
            Log.w(TAG, "Event $type not delivered to the host (${error.javaClass.simpleName})")
        }
    }

    /** Ends the session: the bridge fails its in-flight calls and refuses new ones. */
    fun detach() {
        isOpen = false
        bridge.detach()
    }

    private fun verify() {
        verifier?.enforceSessionOwner(ownerUid)
    }

    private companion object {
        const val TAG = "McpHostSession"
    }
}
