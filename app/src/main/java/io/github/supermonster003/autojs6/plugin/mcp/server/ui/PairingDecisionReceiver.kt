package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingCoordinator

/**
 * Receives the allow / deny action of the pairing notification (roadmap P2.2). It is not
 * exported: only this app can send it, which is also how the instrumentation tests confirm a
 * pairing without a user.
 */
class PairingDecisionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DECISION) return
        val fingerprint = intent.getStringExtra(EXTRA_FINGERPRINT) ?: return
        val allow = intent.getBooleanExtra(EXTRA_ALLOW, false)
        PairingCoordinator.instance?.decide(fingerprint, allow)
    }

    companion object {

        const val ACTION_DECISION = "${McpServerPlugin.PACKAGE_NAME}.action.PAIRING_DECISION"
        const val EXTRA_FINGERPRINT = "fingerprint"
        const val EXTRA_ALLOW = "allow"

        fun intent(context: Context, fingerprint: String, allow: Boolean): Intent =
            Intent(context, PairingDecisionReceiver::class.java)
                .setAction(ACTION_DECISION)
                .putExtra(EXTRA_FINGERPRINT, fingerprint)
                .putExtra(EXTRA_ALLOW, allow)
    }
}
