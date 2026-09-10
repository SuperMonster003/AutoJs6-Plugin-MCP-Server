package io.github.supermonster003.autojs6.plugin.mcp.server.host

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.DevicePingTool
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerService
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BinderBridgeTransport
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostAvailability
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostBridgeClient
import io.github.supermonster003.autojs6.plugin.mcp.server.mcpServerPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpHttpServer
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingCoordinator
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingRequest
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ServerStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfigStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.CatalogToolExecutor
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissionStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.autojs.plugin.mcp.server.api.IMcpHostCapabilityBroker
import org.autojs.plugin.mcp.server.api.IMcpServerCallback
import org.autojs.plugin.mcp.server.api.McpServerContract
import java.io.PrintWriter
import java.util.UUID
import java.util.concurrent.Executors

/**
 * The one MCP server of the `:mcp_server` process (roadmap P2.3): the listener, the tool
 * registry, the host session, and the foreground service that keeps the process alive.
 *
 * Three callers drive it: the host through `IMcpServerPlugin.openServer` and the session Binder,
 * the foreground service through its start / stop intents (adb during development, the settings
 * page in P4.2), and the pairing coordinator through events. The listener outlives the host: when
 * AutoJs6 dies, the bridge marks itself degraded, every host-backed tool answers
 * `HOST_UNAVAILABLE`, and the next `openServer` attaches the new broker to the running listener.
 */
@SuppressLint("StaticFieldLeak") // [instance] holds the application context only
class McpServerRuntime private constructor(context: Context) : PairingCoordinator.EventListener {

    private val context: Context = context.applicationContext

    val configStore: ServerConfigStore = ServerConfigStore(this.context)

    val toolPermissions: ToolPermissionStore = ToolPermissionStore(this.context)

    private val lifecycle = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "mcp-server-lifecycle") }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private val info by lazy { this.context.mcpServerPluginRuntimeInfo() }

    val registry: ToolRegistry = ToolRegistry(
        catalog = ToolCatalog.all,
        permissions = { toolPermissions.load() },
        executor = CatalogToolExecutor(
            bridge = { bridge },
            local = mapOf(ToolCatalog.DEVICE_PING to { DevicePingTool.payload(this.context, info) }),
            clientNameOf = { sessionId -> server.clientNameOf(sessionId) },
            onToolCall = ::onToolCall,
        ),
    )

    val server: McpHttpServer = McpHttpServer(
        this.context,
        statusListener = ::onServerStatus,
        toolInstaller = { sdkServer, _ -> registry.install(sdkServer) },
        toolGate = registry,
        eventListener = this,
    )

    @Volatile
    private var session: HostSession? = null

    /** The configuration the listener should run with; null after a stop. */
    @Volatile
    var activeConfig: ServerConfig? = null
        private set

    @Volatile
    private var foregroundService: McpServerService? = null

    /** The reason of the last stop, reported as `statusLastErrorCode` while stopped. */
    @Volatile
    private var lastStopReason: String? = null

    val isListening: Boolean
        get() = server.isRunning

    /** The bridge of the open session, or null when no host session is attached. */
    val bridge: HostBridgeClient?
        get() = session?.takeIf { it.isOpen }?.bridge

    /** The open host session, for the instrumentation tests and the dump. */
    internal val hostSession: HostSession?
        get() = session?.takeIf { it.isOpen }

    val hostAvailable: Boolean
        get() = bridge?.availability == HostAvailability.AVAILABLE

    // ------------------------------------------------------------------ listener control

    /**
     * Starts the listener for [config], restarting it when a running listener uses another port
     * or scope, and brings up the foreground service. Synchronous; not for the main thread.
     */
    fun start(config: ServerConfig): ServerStatus {
        val running = server.activeConfiguration
        if (running != null) {
            if (running.port == config.port && running.bindScope == config.bindScope) {
                activeConfig = config
                return server.status
            }
            Log.i(TAG, "Listener configuration changed (${running.port}/${running.bindScope.id} -> ${config.port}/${config.bindScope.id}); restarting")
            lastStopReason = McpServerContract.REASON_CONFIG_CHANGED
            server.stop()
        }
        activeConfig = config
        lastStopReason = null
        val status = server.start(config)
        if (status.isRunning) {
            ensureForeground()
        } else {
            activeConfig = null
        }
        return status
    }

    /** Stops the listener and releases the foreground service; the host session stays attached. */
    fun stop(reasonCode: String, message: String? = null) {
        lastStopReason = reasonCode
        if (server.isRunning || server.status.state == ServerStatus.STATE_STARTING) {
            Log.i(TAG, "Stopping the listener ($reasonCode${message?.let { ": $it" }.orEmpty()})")
        }
        activeConfig = null
        server.stop()
        leaveForeground()
    }

    // ------------------------------------------------------------------ host session

    /**
     * `IMcpServerPlugin.openServer`: attaches the host broker, starts the listener with the host's
     * port and scope on top of the stored configuration, and returns the session Binder at once;
     * the status follows through the callback. A session whose host is still alive is refused
     * with `already_open`; one whose host died or that was closed is replaced.
     */
    internal fun openSession(
        configBundle: Bundle?,
        broker: IMcpHostCapabilityBroker,
        callback: IMcpServerCallback?,
        callerUid: Int,
        verifier: HostCallerVerifier? = null,
    ): HostSession {
        val config = HostBundles.sessionConfig(configBundle)
        val transport = BinderBridgeTransport(broker)
        val replaced: HostSession?
        val session: HostSession
        synchronized(lock) {
            val previous = this.session
            if (previous != null && previous.isHostAlive && transport.isAlive) {
                throw IllegalStateException("${McpServerContract.ERROR_ALREADY_OPEN}: session ${previous.id} is still open for uid ${previous.ownerUid}")
            }
            replaced = previous
            val bridge = HostBridgeClient(transport, onDeath = ::onHostDied)
            session = HostSession(this, "plugin-" + UUID.randomUUID().toString(), callerUid, config, callback, bridge, verifier)
            this.session = session
        }
        replaced?.detach()
        val grant = transport.brokerInfo
        Log.i(TAG, "Session ${session.id} opened by uid $callerUid: broker ${grant.brokerId ?: "?"} v${grant.brokerVersion}, ${grant.methods.size} granted methods, ${grant.maxConcurrentCalls} concurrent calls, host label ${config.hostLabel ?: "-"}")
        lifecycle.execute {
            val status = start(listenerConfig(config))
            session.publishStatus()
            refreshNotification()
            if (status.isRunning) probeHost(session)
        }
        return session
    }

    internal fun updateSession(session: HostSession, config: SessionConfig) {
        synchronized(lock) {
            if (this.session !== session || !session.isOpen) {
                throw IllegalStateException("${McpServerContract.ERROR_INTERNAL}: the session is closed")
            }
            session.config = config
        }
        lifecycle.execute {
            val running = server.activeConfiguration
            if (running != null) {
                start(listenerConfig(config))
            } else {
                activeConfig = null
            }
            session.publishStatus()
        }
    }

    internal fun stopFromHost(session: HostSession, reasonCode: String, message: String?) {
        if (this.session !== session) return
        stop(reasonCode, message)
        session.publishStatus()
    }

    /** `IMcpServerSession.close`: stops the listener as well; the host is going away on purpose. */
    internal fun closeSession(session: HostSession) {
        val current = synchronized(lock) {
            if (this.session === session) {
                this.session = null
                true
            } else {
                false
            }
        }
        if (current) {
            Log.i(TAG, "Session ${session.id} closed by the host")
            if (server.isRunning) stop(McpServerContract.REASON_HOST_SHUTDOWN)
        }
        session.detach()
        refreshNotification()
    }

    /**
     * The listener configuration for a host request: the running listener's configuration (so an
     * adb-started developer mode survives the host attaching), else the stored one, with the host's
     * port and scope applied.
     */
    private fun listenerConfig(config: SessionConfig): ServerConfig =
        (activeConfig ?: configStore.load()).withOverrides(port = config.port, bindScope = config.bindScope)

    private fun onHostDied(client: HostBridgeClient) {
        val current = session
        if (current == null || current.bridge !== client) return
        Log.w(TAG, "AutoJs6 died; session ${current.id} is degraded until the host opens a new one")
        refreshNotification()
    }

    /** One `device.info` round trip after attach, which also proves the P1 Binder path in the log. */
    private fun probeHost(session: HostSession) {
        scope.launch {
            val outcome = session.bridge.call("device", "info", timeoutMs = HOST_PROBE_TIMEOUT_MS, permissions = listOf("device"))
            when (outcome) {
                is BridgeOutcome.Ok -> {
                    val host = (outcome.result as? JsonObject)?.get("host") as? JsonObject
                    val version = host?.get("versionName")?.jsonPrimitive?.contentOrNull
                    val pid = host?.get("pid")?.jsonPrimitive?.contentOrNull
                    Log.i(TAG, "Host probe ok in ${outcome.elapsedMs} ms: AutoJs6 ${version ?: "?"} (pid ${pid ?: "?"})")
                }
                is BridgeOutcome.Failed -> {
                    Log.w(TAG, "Host probe failed: ${outcome.failure.render()}")
                    session.publishEvent(McpServerContract.EVENT_WARNING, "host probe failed: ${outcome.failure.render()}")
                }
            }
        }
    }

    // ------------------------------------------------------------------ status and events

    fun statusSnapshot(): SessionStatus {
        val status = server.status
        val config = server.activeConfiguration ?: activeConfig
        val current = session
        val active = status.isRunning || status.state == ServerStatus.STATE_STARTING
        return SessionStatus(
            state = status.state,
            endpoints = if (status.isRunning) server.endpoints else emptyList(),
            port = if (active) config?.port ?: 0 else 0,
            bindScope = (config?.bindScope ?: BindScope.LOOPBACK).id,
            protocolMode = McpServerContract.PROTOCOL_MODE_STATEFUL,
            pairedClientCount = server.pairedClients.all().size,
            activeSessionCount = server.activeSessionCount,
            startedAt = if (status.isRunning) server.startedAt else 0L,
            hostAvailable = current != null && current.isOpen && current.bridge.availability == HostAvailability.AVAILABLE,
            lastErrorCode = status.errorCode ?: lastStopReason?.takeIf { status.state == ServerStatus.STATE_STOPPED && it != McpServerContract.REASON_USER_REQUEST },
            lastError = status.message,
        )
    }

    private fun onServerStatus(status: ServerStatus) {
        session?.publishStatus()
        refreshNotification()
        if (status.isFailed) {
            leaveForeground()
        }
    }

    private fun onToolCall(toolName: String, clientName: String?) {
        Log.i(TAG, "tool_call $toolName by ${clientName ?: "unknown client"}")
        session?.publishEvent(McpServerContract.EVENT_TOOL_CALL, null, clientName, toolName)
    }

    override fun onPairingRequested(request: PairingRequest) {
        session?.publishEvent(
            McpServerContract.EVENT_PAIRING_REQUESTED,
            "\"${request.client.name}\" (${request.client.addressClass.id}) asks to pair",
            request.client.name,
        )
    }

    override fun onPaired(client: PairedClient) {
        session?.publishEvent(McpServerContract.EVENT_CLIENT_PAIRED, "\"${client.name}\" (${client.addressClass.id}) paired", client.name)
        session?.publishStatus()
        refreshNotification()
    }

    override fun onDenied(request: PairingRequest, reason: String) {
        session?.publishEvent(McpServerContract.EVENT_WARNING, "pairing $reason for \"${request.client.name}\"", request.client.name)
    }

    // ------------------------------------------------------------------ foreground service

    fun attachService(service: McpServerService) {
        foregroundService = service
    }

    /** The service is gone; a listener it was keeping alive stops with it unless the runtime asked it to finish. */
    fun detachService(service: McpServerService) {
        if (foregroundService === service) foregroundService = null
        if (server.isRunning && !service.finishing) {
            stop(McpServerContract.ERROR_INTERNAL, "the foreground service was destroyed")
        }
    }

    private fun ensureForeground() {
        if (foregroundService != null) return
        val intent = McpServerService.keepAliveIntent(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (error: Exception) {
            // ForegroundServiceStartNotAllowedException (API 31+) is an IllegalStateException.
            val warning = "the foreground service could not be started (${error.javaClass.simpleName}); the listener runs until Android reclaims the process"
            Log.w(TAG, warning)
            session?.publishEvent(McpServerContract.EVENT_WARNING, warning)
        }
    }

    private fun leaveForeground() {
        val service = foregroundService ?: return
        foregroundService = null
        mainHandler.post { service.finishForeground() }
    }

    private fun refreshNotification() {
        foregroundService?.refreshNotification()
    }

    // ------------------------------------------------------------------ diagnostics

    /** `dumpsys activity service`: the status, the paired clients, and the host session; the token only in developer mode. */
    fun dump(out: PrintWriter, developerMode: Boolean) {
        val snapshot = statusSnapshot()
        out.println("state: ${snapshot.state}")
        snapshot.endpoints.forEach { out.println("endpoint: $it") }
        snapshot.lastErrorCode?.let { out.println("error: $it: ${snapshot.lastError}") }
        out.println("developerMode: $developerMode")
        out.println("tokenFingerprint: ${server.tokenStore.fingerprint()}")
        if (developerMode) out.println("token: ${server.tokenStore.current()}")
        out.println("activeSessions: ${snapshot.activeSessionCount}")
        val clients = server.pairedClients.all()
        out.println("pairedClients: ${clients.size}")
        clients.forEach { client ->
            out.println("  ${client.fingerprint} \"${client.name}\" ${client.addressClass.id} firstPairedAt=${client.firstPairedAt} lastSeenAt=${client.lastSeenAt}")
        }
        val current = session
        if (current == null) {
            out.println("hostSession: none")
        } else {
            val grant = current.bridge.info
            out.println("hostSession: ${current.id} uid=${current.ownerUid} open=${current.isOpen} host=${current.bridge.availability.id} broker=${grant.brokerId ?: "?"} methods=${grant.methods.size} calls=${current.bridge.callCount}")
        }
        out.println("toolGroups: ${ToolCatalog.summary(toolPermissions.load())}")
        out.println("tools: ${registry.enabledNames.joinToString(",")}")
    }

    companion object {

        private const val TAG = "McpServerRuntime"

        /** The `device.info` probe after attach must answer within this. */
        const val HOST_PROBE_TIMEOUT_MS = 10_000L

        @Volatile
        private var instance: McpServerRuntime? = null

        /** The runtime of this process, created on first use. */
        fun get(context: Context): McpServerRuntime =
            instance ?: synchronized(this) { instance ?: McpServerRuntime(context.applicationContext).also { instance = it } }
    }
}
