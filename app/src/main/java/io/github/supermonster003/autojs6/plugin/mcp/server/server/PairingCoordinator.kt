package io.github.supermonster003.autojs6.plugin.mcp.server.server

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.github.supermonster003.autojs6.plugin.mcp.server.R
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.PairingConfirmActivity
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.PairingDecisionReceiver
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Puts a pairing request in front of the user (roadmap P2.2) and feeds the decision back into
 * the [PairingGate]: a high-priority notification with allow / deny actions is always posted,
 * and the confirmation dialog is started as well when the screen is unlocked (Android 10+ may
 * still keep a service from starting it, in which case the notification is the way in). Both
 * paths, the dialog and the notification actions, end in [decide]; whichever way a request is
 * settled (decided, expired, or the listener stopped), its notification is cancelled and the
 * dialog is told to close through [addSettledListener].
 *
 * One coordinator exists per running listener; [instance] lets the dialog and the broadcast
 * receiver in the `:mcp_server` process find it. A listener process that dies takes its pending
 * requests with it but not their notifications, so a new coordinator first clears the pairing
 * notifications a previous process left in the shade (roadmap P6 lifecycle matrix).
 */
@SuppressLint("StaticFieldLeak") // [instance] holds the application context only
class PairingCoordinator(context: Context, private val tokenTail: () -> String) : PairingGate.Listener {

    /** Forwarded to the host session (roadmap P2.3) as `pairing_requested` / `client_paired` events. */
    interface EventListener {

        fun onPairingRequested(request: PairingRequest)

        fun onPaired(client: PairedClient)

        fun onDenied(request: PairingRequest, reason: String)
    }

    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var gate: PairingGate

    @Volatile
    var eventListener: EventListener? = null

    private val settledListeners = CopyOnWriteArrayList<(String) -> Unit>()

    init {
        clearStaleNotifications()
    }

    fun attach(gate: PairingGate) {
        this.gate = gate
    }

    /** Applies the user's answer for [fingerprint]; false when nothing was pending for it. */
    fun decide(fingerprint: String, allow: Boolean): Boolean {
        val applied = if (allow) gate.approve(fingerprint) else gate.deny(fingerprint)
        Log.i(TAG, "Pairing decision ${if (allow) "allow" else "deny"} for $fingerprint: ${if (applied) "applied" else "nothing pending"}")
        if (!applied) settle(fingerprint)
        return applied
    }

    fun dispose() {
        handler.removeCallbacksAndMessages(null)
        gate.pendingRequests().forEach { settle(it.client.fingerprint) }
    }

    /** [listener] receives the fingerprint of every request that stops being pending; called on the main thread. */
    fun addSettledListener(listener: (String) -> Unit) {
        settledListeners += listener
    }

    fun removeSettledListener(listener: (String) -> Unit) {
        settledListeners -= listener
    }

    override fun onPairingRequested(request: PairingRequest) {
        Log.i(TAG, "Pairing requested by \"${request.client.name}\" (${request.client.addressClass.id}), fingerprint ${request.client.fingerprint}")
        eventListener?.onPairingRequested(request)
        handler.post {
            val tail = tokenTail()
            notify(request, tail)
            if (!isKeyguardLocked()) {
                runCatching {
                    context.startActivity(PairingConfirmActivity.intent(context, request, tail).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { Log.w(TAG, "Confirmation dialog could not be started (${it.javaClass.simpleName}); the notification remains") }
            }
            handler.postDelayed({ settle(request.client.fingerprint) }, (request.expiresAt - gate.now()).coerceAtLeast(0L))
        }
    }

    override fun onPaired(client: PairedClient) {
        Log.i(TAG, "Client paired: \"${client.name}\" (${client.addressClass.id})")
        settle(client.fingerprint)
        eventListener?.onPaired(client)
    }

    override fun onDenied(request: PairingRequest, reason: String) {
        Log.i(TAG, "Pairing $reason for \"${request.client.name}\"")
        settle(request.client.fingerprint)
        eventListener?.onDenied(request, reason)
    }

    private fun notify(request: PairingRequest, tail: String) {
        val client = request.client
        val addressText = context.getString(
            if (client.addressClass == AddressClass.LAN) R.string.pairing_address_lan else R.string.pairing_address_loopback,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.pairing_channel_name), NotificationManager.IMPORTANCE_HIGH),
            )
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
        }
        val lan = client.addressClass == AddressClass.LAN
        val text = context.getString(R.string.pairing_notification_text, client.name, addressText, tail)
        val notification = builder
            .setSmallIcon(R.drawable.ic_stat_mcp_server)
            .setContentTitle(context.getString(if (lan) R.string.pairing_notification_title_lan else R.string.pairing_notification_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(if (lan) context.getString(R.string.pairing_lan_warning) + "\n" + text else text))
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    requestCode(request, 0),
                    PairingConfirmActivity.intent(context, request, tail),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(actionOf(request, allow = true))
            .addAction(actionOf(request, allow = false))
            .build()
        notifications.notify(TAG_PAIRING, notificationId(client.fingerprint), notification)
    }

    private fun actionOf(request: PairingRequest, allow: Boolean): Notification.Action {
        val intent = PairingDecisionReceiver.intent(context, request.client.fingerprint, allow)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(request, if (allow) 1 else 2),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = context.getString(if (allow) R.string.pairing_allow else R.string.pairing_deny)
        @Suppress("DEPRECATION")
        return Notification.Action.Builder(R.drawable.ic_stat_mcp_server, title, pending).build()
    }

    private fun settle(fingerprint: String) {
        notifications.cancel(TAG_PAIRING, notificationId(fingerprint))
        handler.post { settledListeners.forEach { it(fingerprint) } }
    }

    /** Cancels pairing notifications whose requests died with a previous listener process. */
    private fun clearStaleNotifications() {
        val stale = runCatching { notifications.activeNotifications.filter { it.tag == TAG_PAIRING } }.getOrDefault(emptyList())
        stale.forEach { notifications.cancel(it.tag, it.id) }
        if (stale.isNotEmpty()) Log.i(TAG, "Cleared ${stale.size} pairing notification(s) of a previous listener process")
    }

    private fun isKeyguardLocked(): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    private fun requestCode(request: PairingRequest, slot: Int): Int = (request.id * 4 + slot).toInt() and 0x7FFFFFFF

    private fun notificationId(fingerprint: String): Int = NOTIFICATION_BASE + (fingerprint.hashCode() and 0xFFFF)

    companion object {

        const val CHANNEL_ID = "mcp_pairing"

        private const val TAG = "PairingCoordinator"
        private const val TAG_PAIRING = "${McpServerPlugin.PACKAGE_NAME}.pairing"
        private const val NOTIFICATION_BASE = 0x50410000

        /** The coordinator of the running listener in the `:mcp_server` process; null while stopped. */
        @Volatile
        var instance: PairingCoordinator? = null
    }
}
