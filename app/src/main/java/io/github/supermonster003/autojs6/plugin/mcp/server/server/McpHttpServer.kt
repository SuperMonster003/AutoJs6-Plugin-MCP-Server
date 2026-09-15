package io.github.supermonster003.autojs6.plugin.mcp.server.server

import android.content.Context
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.DevicePingTool
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.mcp.server.mcpServerPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.PairedClientStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.store.TokenStore
import io.ktor.server.application.serverConfig
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Owns one Ktor CIO listener and the MCP server mounted on it (roadmap P2.1).
 *
 * [start] binds the address and port of a [ServerConfig] (loopback by default, every interface
 * only in LAN scope), keeps idle connections for [IDLE_TIMEOUT_SECONDS], and never throws: a
 * port that is taken or a bind the system refuses becomes a `failed` [ServerStatus] with the
 * `port_in_use` / `bind_failed` code and a hint. In LAN scope a [LanAddressWatcher] keeps the
 * gate's allowed `Host` list in step with the device's addresses. Every status change reaches
 * [statusListener], which the foreground service and the session Binder (P2.3) observe.
 *
 * Every request must carry the bearer token of [tokenStore], and gated calls of clients the user
 * has not confirmed are held back by the [PairingGate] (roadmap P2.2). The tools come from
 * [toolInstaller] (the catalog registry in production, `device_ping` alone by default) and calls
 * to switched-off tools are answered by [toolGate] before they reach the SDK (roadmap P2.3).
 * Each listener owns a [RateLimiter] with the default [RateLimits] and an [IdleStopMonitor]
 * that the runtime reads for the idle auto-stop option (roadmap P6).
 */
class McpHttpServer(
    context: Context,
    private val statusListener: (ServerStatus) -> Unit = {},
    private val toolInstaller: ((Server, McpServerPluginRuntimeInfo) -> Unit)? = null,
    private val toolGate: ToolGate? = null,
    private val eventListener: PairingCoordinator.EventListener? = null,
) {

    private val context: Context = context.applicationContext

    private val lock = Any()

    /** The bearer token store; shared with the settings page (P4.2). */
    val tokenStore: TokenStore = TokenStore(this.context)

    /** The paired clients; shared with the settings page (P4.2). */
    val pairedClients: PairedClientStore = PairedClientStore(this.context)

    private var engine: EmbeddedServer<*, *>? = null
    private var mcpServer: Server? = null
    private var activeConfig: ServerConfig? = null
    private var lanWatcher: LanAddressWatcher? = null
    private var pairingCoordinator: PairingCoordinator? = null

    /** The per-client windows of the running listener; null while stopped. */
    @Volatile
    var rateLimiter: RateLimiter? = null
        private set

    /** The pairing state machine of the running listener; null while stopped. */
    @Volatile
    var pairingGate: PairingGate? = null
        private set

    /** The activity record of the running listener for the idle auto-stop; null while stopped. */
    @Volatile
    var idleMonitor: IdleStopMonitor? = null
        private set

    @Volatile
    private var policy: GatePolicy = GatePolicy.loopback()

    @Volatile
    var status: ServerStatus = ServerStatus.stopped()
        private set

    /** Epoch milliseconds of the last successful start, 0 while stopped. */
    @Volatile
    var startedAt: Long = 0L
        private set

    /** Every URL the running listener accepts (loopback first, then the LAN addresses); empty while stopped. */
    @Volatile
    var endpoints: List<String> = emptyList()
        private set

    val isRunning: Boolean
        get() = status.isRunning

    /** The configuration of the running listener; null while stopped. */
    val activeConfiguration: ServerConfig?
        get() = synchronized(lock) { activeConfig }

    /** The gate policy currently applied to requests (loopback names while stopped). */
    val currentPolicy: GatePolicy
        get() = policy

    /** MCP sessions the SDK currently holds on the listener. */
    val activeSessionCount: Int
        get() = mcpServer?.sessions?.size ?: 0

    /** The SDK server of the running listener; null while stopped. */
    val sdkServer: Server?
        get() = mcpServer

    /**
     * The client name declared in `initialize` for an SDK session id (the id tool handlers see)
     * or an HTTP `Mcp-Session-Id`; null when the session is unknown or not initialized yet.
     */
    fun clientNameOf(sessionId: String?): String? {
        val id = sessionId ?: return null
        val sessions = mcpServer?.sessions ?: return null
        return runCatching {
            (sessions[id] ?: sessions.values.firstOrNull { (it.transport as? StreamableHttpServerTransport)?.sessionId == id })
                ?.clientVersion?.name
        }.getOrNull()
    }

    /** Starts the listener for [config]; a call while running is a no-op that returns the current status. */
    fun start(config: ServerConfig): ServerStatus = synchronized(lock) {
        if (engine != null) return status
        val problems = config.problems()
        if (problems.isNotEmpty()) {
            return publish(ServerStatus.failed(ERROR_INVALID_CONFIG, problems.joinToString("; ")))
        }
        publish(ServerStatus.starting())
        val info = context.mcpServerPluginRuntimeInfo()
        val server = McpServerFactory.create(info.versionName)
        (toolInstaller ?: { target, runtimeInfo -> DevicePingTool.register(target, context, runtimeInfo) })(server, info)
        val watcher = if (config.bindScope == BindScope.LAN) LanAddressWatcher(context, ::refreshPolicy) else null
        watcher?.start()
        policy = GatePolicy.forConfig(config, watcher?.addresses.orEmpty())
        val coordinator = PairingCoordinator(context) { tokenStore.tail() }
        coordinator.eventListener = eventListener
        val gate = PairingGate(pairedClients, listener = coordinator)
        coordinator.attach(gate)
        val limiter = RateLimiter()
        val monitor = IdleStopMonitor { SystemClock.elapsedRealtime() }
        val token = tokenStore.current()
        Log.i(TAG, "Bearer token fingerprint ${BearerTokens.fingerprint(token)}, ${pairedClients.all().size} paired client(s)")
        val bindStartedAt = SystemClock.elapsedRealtime()
        // The CIO accept loop runs in a root coroutine under the application's parent context; a
        // failed bind() completes start() exceptionally AND fails that coroutine, and on Android an
        // unhandled coroutine exception kills the process. The handler keeps it in the log.
        val rootConfig = serverConfig(applicationEnvironment()) {
            parentCoroutineContext = CoroutineExceptionHandler { _, error ->
                Log.w(TAG, "Listener coroutine failed: ${error.javaClass.name}: ${error.message}")
            }
            module {
                mcpServerModule(
                    server,
                    policy = { this@McpHttpServer.policy },
                    tokenProvider = { tokenStore.current() },
                    pairingGate = gate,
                    toolGate = toolGate,
                    rateLimiter = limiter,
                    activity = monitor,
                )
            }
        }
        val listener = embeddedServer(CIO, rootConfig) {
            connector {
                host = config.bindAddress
                port = config.port
            }
            connectionIdleTimeoutSeconds = IDLE_TIMEOUT_SECONDS
            shutdownGracePeriod = STOP_GRACE_MILLIS
            shutdownTimeout = STOP_TIMEOUT_MILLIS
        }
        try {
            listener.start(wait = false)
        } catch (error: Throwable) {
            watcher?.stop()
            runCatching { listener.stop(0, 0) }
            val failure = BindFailures.classify(error, config.bindAddress, config.port)
            Log.w(TAG, "MCP endpoint could not bind ${config.bindAddress}:${config.port}: ${failure.code}", error)
            return publish(ServerStatus.failed(failure.code, failure.message))
        }
        engine = listener
        mcpServer = server
        activeConfig = config
        lanWatcher = watcher
        pairingCoordinator = coordinator
        pairingGate = gate
        rateLimiter = limiter
        idleMonitor = monitor
        PairingCoordinator.instance = coordinator
        startedAt = System.currentTimeMillis()
        endpoints = endpointUrls(config, watcher?.addresses.orEmpty())
        val endpoint = endpoints.first()
        Log.i(TAG, "MCP endpoint listening on $endpoint [${config.bindScope.id}] (${SystemClock.elapsedRealtime() - bindStartedAt} ms to bind)")
        return publish(ServerStatus.running(endpoint))
    }

    fun stop(): ServerStatus = synchronized(lock) {
        val running = engine ?: return status
        publish(ServerStatus.stopping())
        engine = null
        mcpServer = null
        activeConfig = null
        startedAt = 0L
        endpoints = emptyList()
        lanWatcher?.stop()
        lanWatcher = null
        if (PairingCoordinator.instance === pairingCoordinator) PairingCoordinator.instance = null
        pairingCoordinator?.dispose()
        pairingCoordinator = null
        pairingGate = null
        rateLimiter = null
        idleMonitor = null
        running.stop(STOP_GRACE_MILLIS, STOP_TIMEOUT_MILLIS)
        policy = GatePolicy.loopback()
        Log.i(TAG, "MCP endpoint stopped")
        return publish(ServerStatus.stopped())
    }

    private fun refreshPolicy() {
        synchronized(lock) {
            val config = activeConfig ?: return
            val addresses = lanWatcher?.addresses.orEmpty()
            policy = GatePolicy.forConfig(config, addresses)
            endpoints = endpointUrls(config, addresses)
        }
        Log.i(TAG, "LAN allow list refreshed")
        statusListener(status)
    }

    private fun publish(next: ServerStatus): ServerStatus {
        status = next
        statusListener(next)
        return next
    }

    companion object {

        const val LOOPBACK_HOST = "127.0.0.1"

        /** Mirrors `McpServerContract.ERROR_INVALID_CONFIG`. */
        const val ERROR_INVALID_CONFIG = "invalid_config"

        /** Keep-alive connections idle longer than this are closed (SSE streams are not idle). */
        const val IDLE_TIMEOUT_SECONDS = 60

        private const val STOP_GRACE_MILLIS = 250L
        private const val STOP_TIMEOUT_MILLIS = 1_000L
        private const val TAG = "McpHttpServer"

        fun endpointUrl(port: Int): String = "http://$LOOPBACK_HOST:$port${McpServerPlugin.ENDPOINT_PATH}"

        /** The URL a PC client uses: loopback for adb forward, the first LAN address in LAN scope. */
        fun endpointUrl(config: ServerConfig, lanAddresses: Collection<String> = emptyList()): String {
            val host = when (config.bindScope) {
                BindScope.LOOPBACK -> LOOPBACK_HOST
                BindScope.LAN -> lanAddresses.sorted().firstOrNull() ?: LOOPBACK_HOST
            }
            return "http://$host:${config.port}${McpServerPlugin.ENDPOINT_PATH}"
        }

        /** Every URL the listener accepts for [config]: the loopback one, then the LAN addresses in LAN scope. */
        fun endpointUrls(config: ServerConfig, lanAddresses: Collection<String> = emptyList()): List<String> = buildList {
            add(endpointUrl(config.port))
            if (config.bindScope == BindScope.LAN) {
                lanAddresses.sorted().forEach { add("http://$it:${config.port}${McpServerPlugin.ENDPOINT_PATH}") }
            }
        }
    }
}
