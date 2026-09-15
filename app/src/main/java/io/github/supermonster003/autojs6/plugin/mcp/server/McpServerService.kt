package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import io.github.supermonster003.autojs6.plugin.mcp.server.host.McpServerRuntime
import io.github.supermonster003.autojs6.plugin.mcp.server.host.SessionStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.server.LanReminder
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ServerStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import org.autojs.plugin.mcp.server.api.McpServerContract
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Foreground service that keeps the `:mcp_server` process alive while the listener of
 * [McpServerRuntime] runs.
 *
 * The listener configuration comes from the runtime's config store; a start intent may override
 * the port and the developer-mode switch for one run. Callers: the runtime itself when the host
 * opens a session ([ACTION_KEEP_ALIVE]), the plugin's own settings page (P4.2), and `adb shell`
 * during development:
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
 * The notification shows the endpoint, whether AutoJs6 is attached, and the paired client count,
 * with a Stop action; when notifications are blocked a toast names the endpoint once instead.
 * An idle auto-stop (roadmap P6) leaves one auto-cancelling notification behind that says why
 * the endpoint is gone.
 * `adb shell dumpsys activity service <pkg>/.McpServerService` prints the status, the paired
 * clients, and the host session, and in developer mode also the bearer token: until the settings
 * page (P4.2) exists, the adb control plane is the only way to read it. Nothing printed there
 * reaches logcat.
 */
class McpServerService : Service() {

    private val lifecycle = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "mcp-server-command") }
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var runtime: McpServerRuntime

    @Volatile
    private var foreground = false

    /** True once the runtime asked this instance to go away; a later start command revives it. */
    @Volatile
    var finishing = false
        private set

    @Volatile
    private var lastStartId = 0

    /** Start commands whose task has not finished yet; a finish request waits for them. */
    private val pendingStarts = AtomicInteger()

    @Volatile
    private var toastShown = false

    /** Armed while a LAN listener runs with the reminder on; fires [postLanReminder] (P5.1). */
    private var lanReminderArmed = false

    private val lanReminderTask = Runnable { postLanReminder() }

    override fun onCreate() {
        super.onCreate()
        runtime = McpServerRuntime.get(this)
        runtime.attachService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        lastStartId = startId
        finishing = false
        runtime.attachService(this)
        // Always enter the foreground first: a service launched through startForegroundService()
        // must call startForeground() promptly, even when the command only asks it to stop.
        enterForeground(starting = action != ACTION_STOP && !runtime.isListening)
        when (action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stop requested")
                lifecycle.execute { runtime.stop(McpServerContract.REASON_USER_REQUEST) }
                if (!runtime.isListening) finishForeground()
            }
            ACTION_KEEP_ALIVE -> if (!runtime.isListening) {
                // The listener the runtime started for a host session is gone already.
                finishForeground()
            }
            else -> {
                val config = resolveConfig(intent)
                pendingStarts.incrementAndGet()
                lifecycle.execute {
                    val status = try {
                        runtime.start(config)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Unexpected failure while starting the MCP endpoint", e)
                        ServerStatus.failed(McpServerContract.ERROR_INTERNAL, e.message ?: e.javaClass.name)
                    } finally {
                        pendingStarts.decrementAndGet()
                    }
                    if (status.isFailed) {
                        Log.w(TAG, "MCP endpoint not started: ${status.errorCode}: ${status.message}")
                        mainHandler.post { finishForeground() }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        lifecycle.shutdown()
        foreground = false
        mainHandler.removeCallbacks(lanReminderTask)
        lanReminderArmed = false
        // Synchronous, so the port is free by the time the next start command (a new service
        // instance on this same main thread) tries to bind it.
        runtime.detachService(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        val out = writer ?: return
        runtime.dump(out, developerMode = runtime.activeConfig?.developerMode == true)
    }

    /**
     * Leaves the foreground and stops; called by the runtime once the listener is down. A start
     * command that arrived after the decision keeps the instance alive: one whose task is still
     * queued or running is awaited (it finishes on its own when it fails), one the system has
     * accepted but not delivered yet makes `stopSelfResult` refuse the stop. Either way a listener
     * started by that command is never torn down by a stale stop.
     */
    fun finishForeground() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { finishForeground() }
            return
        }
        if (!foreground) return
        if (pendingStarts.get() > 0) return
        finishing = true
        foreground = false
        disarmLanReminder()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // A refresh the stop sequence posted just before can still be queued in the system and
        // would outlive the foreground removal as a plain notification (seen on MIUI); cancel it.
        notifications().cancel(NOTIFICATION_ID)
        if (!stopSelfResult(lastStartId)) {
            // A newer start command is being handled; stay in the foreground for it.
            finishing = false
            runtime.attachService(this)
            enterForeground(starting = !runtime.isListening)
        }
    }

    /** Re-renders the notification from the runtime status; safe from any thread. */
    fun refreshNotification() {
        mainHandler.post {
            if (!foreground) return@post
            val snapshot = runtime.statusSnapshot()
            if (snapshot.state == ServerStatus.STATE_STOPPING || snapshot.state == ServerStatus.STATE_STOPPED) {
                // The runtime finishes this service after a stop (or restarts the listener at once
                // on a configuration change); a "stopped" rendering would only race the removal.
                syncLanReminder(snapshot)
                return@post
            }
            notifications().notify(NOTIFICATION_ID, buildNotification(snapshot, starting = false))
            if (snapshot.state == ServerStatus.STATE_RUNNING && !toastShown && !notifications().areNotificationsEnabled()) {
                toastShown = true
                Toast.makeText(this, getString(R.string.server_notifications_disabled, snapshot.endpoints.firstOrNull().orEmpty()), Toast.LENGTH_LONG).show()
            }
            syncLanReminder(snapshot)
        }
    }

    /** Arms or disarms the daily LAN reminder so it matches the listener state; main thread only. */
    private fun syncLanReminder(snapshot: SessionStatus) {
        val wanted = foreground && LanReminder.wanted(snapshot.state == ServerStatus.STATE_RUNNING, runtime.activeConfig)
        if (wanted && !lanReminderArmed) {
            lanReminderArmed = true
            mainHandler.postDelayed(lanReminderTask, LanReminder.INTERVAL_MS)
        } else if (!wanted) {
            disarmLanReminder()
        }
    }

    private fun disarmLanReminder() {
        if (!lanReminderArmed) return
        lanReminderArmed = false
        mainHandler.removeCallbacks(lanReminderTask)
        notifications().cancel(LAN_REMINDER_ID)
    }

    private fun postLanReminder() {
        lanReminderArmed = false
        val snapshot = runtime.statusSnapshot()
        if (!foreground || !LanReminder.wanted(snapshot.state == ServerStatus.STATE_RUNNING, runtime.activeConfig)) return
        val manager = notifications()
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(LAN_CHANNEL_ID, getString(R.string.server_lan_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
            )
            Notification.Builder(this, LAN_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val text = getString(R.string.server_lan_reminder_text, LanReminder.endpoint(snapshot.endpoints))
        manager.notify(LAN_REMINDER_ID, builder
            .setSmallIcon(R.drawable.ic_stat_mcp_server)
            .setContentIntent(settingsPendingIntent())
            .setContentTitle(getString(R.string.server_lan_reminder_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(stopAction())
            .build())
        syncLanReminder(snapshot)
    }

    private fun resolveConfig(intent: Intent?): ServerConfig {
        val stored = runtime.configStore.load()
        val port = intent?.takeIf { it.hasExtra(EXTRA_PORT) }?.getIntExtra(EXTRA_PORT, stored.port)
        val developerMode = intent?.takeIf { it.hasExtra(EXTRA_DEVELOPER_MODE) }?.getBooleanExtra(EXTRA_DEVELOPER_MODE, stored.developerMode)
        return stored.withOverrides(port = port, developerMode = developerMode)
    }

    private fun enterForeground(starting: Boolean) {
        val notification = buildNotification(runtime.statusSnapshot(), starting)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foreground = true
    }

    /** One auto-cancelling notification saying the listener stopped itself after [minutes] idle minutes; safe from any thread. */
    fun notifyIdleStop(minutes: Int) {
        val text = getString(R.string.server_idle_stopped, minutes)
        notifications().notify(IDLE_STOP_NOTIFICATION_ID, serviceChannelBuilder()
            .setSmallIcon(R.drawable.ic_stat_mcp_server)
            .setContentIntent(settingsPendingIntent())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build())
    }

    /** A builder on the service channel (created on demand from API 26 on). */
    private fun serviceChannelBuilder(): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications().createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW),
            )
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

    private fun buildNotification(snapshot: SessionStatus, starting: Boolean): Notification {
        val builder = serviceChannelBuilder()
        val text = when {
            snapshot.state == ServerStatus.STATE_RUNNING -> getString(R.string.server_listening, snapshot.endpoints.firstOrNull().orEmpty())
            snapshot.state == ServerStatus.STATE_FAILED -> getString(R.string.server_state_failed, "${snapshot.lastErrorCode}: ${snapshot.lastError}")
            starting || snapshot.state == ServerStatus.STATE_STARTING -> getString(R.string.server_state_starting)
            else -> getString(R.string.server_state_stopped)
        }
        val details = getString(if (snapshot.hostAvailable) R.string.server_host_connected else R.string.server_host_disconnected) +
                ", " + getString(R.string.server_paired_clients, snapshot.pairedClientCount)
        return builder
            .setSmallIcon(R.drawable.ic_stat_mcp_server)
            .setContentIntent(settingsPendingIntent())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText("$text\n$details"))
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(stopAction())
            .build()
    }

    private fun settingsPendingIntent(): PendingIntent = PendingIntent.getActivity(this, 1,
        Intent(this, io.github.supermonster003.autojs6.plugin.mcp.server.ui.McpServerSettingsActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun stopAction(): Notification.Action {
        val stopPending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 0, stopIntent(this), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, stopIntent(this), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        @Suppress("DEPRECATION")
        return Notification.Action.Builder(R.drawable.ic_stat_mcp_server, getString(R.string.server_action_stop), stopPending).build()
    }

    private fun notifications(): NotificationManager = getSystemService(NotificationManager::class.java)

    companion object {

        const val ACTION_START = "${McpServerPlugin.PACKAGE_NAME}.action.START_SERVER"
        const val ACTION_STOP = "${McpServerPlugin.PACKAGE_NAME}.action.STOP_SERVER"

        /** Sent by the runtime: enter the foreground for a listener that is already running. */
        const val ACTION_KEEP_ALIVE = "${McpServerPlugin.PACKAGE_NAME}.action.KEEP_ALIVE"

        /** Optional int extra of [ACTION_START]: overrides the stored port for this run. */
        const val EXTRA_PORT = "port"

        /**
         * Optional boolean extra of [ACTION_START]: overrides the stored developer-mode switch for
         * this run, so an adb session can let the MCP Inspector's browser page reach the endpoint.
         */
        const val EXTRA_DEVELOPER_MODE = "developer_mode"

        private const val CHANNEL_ID = "mcp_server"
        private const val LAN_CHANNEL_ID = "mcp_server_lan"
        internal const val NOTIFICATION_ID = 0x4D43
        private const val LAN_REMINDER_ID = 0x4D44

        /** The idle auto-stop notice (roadmap P6); a separate id so the foreground notification's removal leaves it. */
        internal const val IDLE_STOP_NOTIFICATION_ID = 0x4D45
        private const val TAG = "McpServerService"

        fun startIntent(context: Context, port: Int? = null, developerMode: Boolean? = null): Intent =
            Intent(context, McpServerService::class.java).setAction(ACTION_START).also { intent ->
                port?.let { intent.putExtra(EXTRA_PORT, it) }
                developerMode?.let { intent.putExtra(EXTRA_DEVELOPER_MODE, it) }
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, McpServerService::class.java).setAction(ACTION_STOP)

        fun keepAliveIntent(context: Context): Intent =
            Intent(context, McpServerService::class.java).setAction(ACTION_KEEP_ALIVE)
    }
}
