package io.github.supermonster003.autojs6.plugin.mcp.server.host

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler

/**
 * Watches package changes while the listener runs (roadmap P11 D28): installing, removing,
 * replacing, or enabling / disabling any app may change which plugin families are available, so
 * the runtime drops its probe cache and tells the clients when the tool list changed, without
 * waiting for their next request. One install fires several of these broadcasts, so the callback
 * is debounced on [handler].
 */
internal class PackageChangeWatcher(
    private val context: Context,
    private val handler: Handler,
    private val debounceMs: Long = DEBOUNCE_MS,
    private val onChanged: () -> Unit,
) {

    private val notify = Runnable { onChanged() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action !in ACTIONS) return
            handler.removeCallbacks(notify)
            handler.postDelayed(notify, debounceMs)
        }
    }

    private var registered = false

    @Synchronized
    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            ACTIONS.forEach(::addAction)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The package actions are protected broadcasts only the system can send; a receiver registered as
            // RECEIVER_NOT_EXPORTED did not get them on Android 15 (Xiaomi 23046RP50C), so the receiver is exported.
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        registered = true
    }

    @Synchronized
    fun stop() {
        if (!registered) return
        registered = false
        handler.removeCallbacks(notify)
        runCatching { context.unregisterReceiver(receiver) }
    }

    companion object {

        /** Broadcasts of one install arrive within this window and trigger one refresh. */
        const val DEBOUNCE_MS = 500L

        val ACTIONS: Set<String> = setOf(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REPLACED,
        )
    }
}
