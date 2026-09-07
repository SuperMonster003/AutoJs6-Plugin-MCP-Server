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
import java.util.concurrent.Executors

/**
 * Foreground service that keeps the `:mcp_server` process alive while [McpHttpServer] listens.
 *
 * Callers: the host broker (roadmap P1 / P2) and the plugin's own settings page (P4.2), both under
 * the plugin's UID, and `adb shell` during development:
 *
 * ```
 * adb shell am start-foreground-service -n <pkg>/.McpServerService -a <pkg>.action.START_SERVER
 * adb shell am start-foreground-service -n <pkg>/.McpServerService -a <pkg>.action.STOP_SERVER
 * ```
 *
 * (`am startservice` on API 24 / 25.) The manifest guards the component with
 * `android.permission.DUMP`, which adb shell and the system hold but third-party apps cannot obtain.
 */
class McpServerService : Service() {

    private val lifecycle = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "mcp-server-lifecycle") }

    private lateinit var server: McpHttpServer

    override fun onCreate() {
        super.onCreate()
        server = McpHttpServer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, McpServerPlugin.DEFAULT_PORT) ?: McpServerPlugin.DEFAULT_PORT
        val stateless = intent?.getBooleanExtra(EXTRA_STATELESS, false) ?: false
        // Always enter the foreground first: a service launched through startForegroundService()
        // must call startForeground() promptly, even when the command only asks it to stop.
        enterForeground(port)
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stop requested")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> lifecycle.execute {
                try {
                    server.start(port, stateless)
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to start the MCP endpoint on port $port", e)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        lifecycle.execute { server.stop() }
        lifecycle.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enterForeground(port: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW),
            )
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_stat_mcp_server)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(McpHttpServer.endpointUrl(port))
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {

        const val ACTION_START = "${McpServerPlugin.PACKAGE_NAME}.action.START_SERVER"
        const val ACTION_STOP = "${McpServerPlugin.PACKAGE_NAME}.action.STOP_SERVER"

        /** Optional int extra of [ACTION_START]; defaults to [McpServerPlugin.DEFAULT_PORT]. */
        const val EXTRA_PORT = "port"

        /** Optional boolean extra of [ACTION_START]: mount the SDK's stateless transport (D9 evaluation only). */
        const val EXTRA_STATELESS = "stateless"

        private const val CHANNEL_ID = "mcp_server"
        private const val NOTIFICATION_ID = 0x4D43
        private const val TAG = "McpServerService"

        fun startIntent(context: Context, port: Int = McpServerPlugin.DEFAULT_PORT): Intent =
            Intent(context, McpServerService::class.java).setAction(ACTION_START).putExtra(EXTRA_PORT, port)

        fun stopIntent(context: Context): Intent =
            Intent(context, McpServerService::class.java).setAction(ACTION_STOP)
    }
}
