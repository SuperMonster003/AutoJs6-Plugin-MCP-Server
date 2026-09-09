package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpHttpServer
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ServerStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfigStore
import java.util.concurrent.Executors

/**
 * Foreground service that keeps the `:mcp_server` process alive while [McpHttpServer] listens.
 *
 * The listener configuration comes from [ServerConfigStore]; a start intent may override the
 * port and the developer-mode switch for one run. Callers: the host broker (roadmap P1 / P2) and
 * the plugin's own settings page (P4.2), both under the plugin's UID, and `adb shell` during
 * development:
 *
 * ```
 * adb shell am start-foreground-service -n <pkg>/.McpServerService -a <pkg>.action.START_SERVER
 * adb shell am start-foreground-service -n <pkg>/.McpServerService -a <pkg>.action.STOP_SERVER
 * ```
 *
 * (`am startservice` on API 24 / 25.) The manifest guards the component with
 * `android.permission.DUMP`, which adb shell and the system hold but third-party apps cannot obtain.
 *
 * Stop through [ACTION_STOP] (see [stopIntent]) rather than `Context.stopService()`: on API 31+
 * a `stopService()` that lands while a `startForegroundService()` is still pending brings the
 * service down before `startForeground()` ran and the system kills the process with
 * `ForegroundServiceDidNotStartInTimeException`.
 *
 * A listener that cannot bind leaves a `failed` status (`port_in_use`, `bind_failed`, or
 * `invalid_config`) in the log and stops the service; the host session (P2.3) reports it.
 */
class McpServerService : Service() {

    private val lifecycle = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "mcp-server-lifecycle") }

    private lateinit var configStore: ServerConfigStore
    private lateinit var server: McpHttpServer

    @Volatile
    private var foreground = false

    override fun onCreate() {
        super.onCreate()
        configStore = ServerConfigStore(this)
        server = McpHttpServer(this, ::onStatusChanged)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = resolveConfig(intent)
        // Always enter the foreground first: a service launched through startForegroundService()
        // must call startForeground() promptly, even when the command only asks it to stop.
        enterForeground(McpHttpServer.endpointUrl(config))
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stop requested")
                foreground = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> lifecycle.execute {
                val status = try {
                    server.start(config)
                } catch (e: Throwable) {
                    Log.e(TAG, "Unexpected failure while starting the MCP endpoint", e)
                    ServerStatus.failed("internal", e.message ?: e.javaClass.name)
                }
                if (status.isFailed) {
                    Log.w(TAG, "MCP endpoint not started: ${status.errorCode}: ${status.message}")
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Stop synchronously so the port is free by the time the next start command (a new service
        // instance on this same main thread) tries to bind it.
        lifecycle.shutdown()
        server.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun resolveConfig(intent: Intent?): ServerConfig {
        val stored = configStore.load()
        val port = intent?.takeIf { it.hasExtra(EXTRA_PORT) }?.getIntExtra(EXTRA_PORT, stored.port)
        val developerMode = intent?.takeIf { it.hasExtra(EXTRA_DEVELOPER_MODE) }?.getBooleanExtra(EXTRA_DEVELOPER_MODE, stored.developerMode)
        return stored.withOverrides(port = port, developerMode = developerMode)
    }

    private fun onStatusChanged(status: ServerStatus) {
        if (!foreground) return
        val text = when {
            status.isRunning -> status.endpointUrl.orEmpty()
            status.isFailed -> "${status.errorCode}: ${status.message}"
            else -> status.state
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun enterForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foreground = true
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW),
            )
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_stat_mcp_server)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    companion object {

        const val ACTION_START = "${McpServerPlugin.PACKAGE_NAME}.action.START_SERVER"
        const val ACTION_STOP = "${McpServerPlugin.PACKAGE_NAME}.action.STOP_SERVER"

        /** Optional int extra of [ACTION_START]: overrides the stored port for this run. */
        const val EXTRA_PORT = "port"

        /**
         * Optional boolean extra of [ACTION_START]: overrides the stored developer-mode switch for
         * this run, so an adb session can let the MCP Inspector's browser page reach the endpoint.
         */
        const val EXTRA_DEVELOPER_MODE = "developer_mode"

        private const val CHANNEL_ID = "mcp_server"
        private const val NOTIFICATION_ID = 0x4D43
        private const val TAG = "McpServerService"

        fun startIntent(context: Context, port: Int? = null, developerMode: Boolean? = null): Intent =
            Intent(context, McpServerService::class.java).setAction(ACTION_START).also { intent ->
                port?.let { intent.putExtra(EXTRA_PORT, it) }
                developerMode?.let { intent.putExtra(EXTRA_DEVELOPER_MODE, it) }
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, McpServerService::class.java).setAction(ACTION_STOP)
    }
}
