package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.github.supermonster003.autojs6.plugin.mcp.server.R
import io.github.supermonster003.autojs6.plugin.mcp.server.server.AddressClass
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingCoordinator
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingRequest

/**
 * The pairing confirmation (roadmap P2.2): a dialog over a translucent activity that names the
 * client, where it connects from, and the last characters of the token, with allow / deny
 * buttons. It closes by itself when the request expires or is settled elsewhere (a notification
 * action, the listener stopping); dismissing it decides nothing, the notification stays
 * available until then. A new request re-uses the activity (`singleTop`) and replaces the dialog.
 */
class PairingConfirmActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    private val settled: (String) -> Unit = { fingerprint -> if (fingerprint == shownFingerprint) finish() }

    private var dialog: AlertDialog? = null

    private var shownFingerprint: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PairingCoordinator.instance?.addSettledListener(settled)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    override fun onDestroy() {
        PairingCoordinator.instance?.removeSettledListener(settled)
        handler.removeCallbacksAndMessages(null)
        dismissQuietly()
        super.onDestroy()
    }

    private fun render(intent: Intent) {
        val fingerprint = intent.getStringExtra(EXTRA_FINGERPRINT)
        val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, 0L)
        val remaining = expiresAt - System.currentTimeMillis()
        if (fingerprint.isNullOrEmpty() || remaining <= 0L) {
            finish()
            return
        }
        handler.removeCallbacksAndMessages(null)
        dismissQuietly()
        shownFingerprint = fingerprint
        val client = intent.getStringExtra(EXTRA_CLIENT_NAME).orEmpty()
        val addressClass = AddressClass.fromId(intent.getStringExtra(EXTRA_ADDRESS_CLASS)) ?: AddressClass.LOOPBACK
        val tokenTail = intent.getStringExtra(EXTRA_TOKEN_TAIL).orEmpty()
        val lan = addressClass == AddressClass.LAN
        val addressText = getString(if (lan) R.string.pairing_address_lan else R.string.pairing_address_loopback)
        val message = getString(R.string.pairing_dialog_message, client, addressText, tokenTail)
        dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle(if (lan) R.string.pairing_dialog_title_lan else R.string.pairing_dialog_title)
            .setMessage(if (lan) getString(R.string.pairing_lan_warning) + "\n\n" + message else message)
            .setPositiveButton(R.string.pairing_allow) { _, _ -> decide(fingerprint, allow = true) }
            .setNegativeButton(R.string.pairing_deny) { _, _ -> decide(fingerprint, allow = false) }
            .setOnDismissListener { if (!isFinishing) finish() }
            .create()
            .also { it.show() }
        handler.postDelayed({ finish() }, remaining)
    }

    private fun dismissQuietly() {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialog = null
    }

    private fun decide(fingerprint: String, allow: Boolean) {
        PairingCoordinator.instance?.decide(fingerprint, allow)
        finish()
    }

    companion object {

        const val EXTRA_FINGERPRINT = "${McpServerPlugin.PACKAGE_NAME}.extra.FINGERPRINT"
        const val EXTRA_CLIENT_NAME = "${McpServerPlugin.PACKAGE_NAME}.extra.CLIENT_NAME"
        const val EXTRA_ADDRESS_CLASS = "${McpServerPlugin.PACKAGE_NAME}.extra.ADDRESS_CLASS"
        const val EXTRA_TOKEN_TAIL = "${McpServerPlugin.PACKAGE_NAME}.extra.TOKEN_TAIL"
        const val EXTRA_EXPIRES_AT = "${McpServerPlugin.PACKAGE_NAME}.extra.EXPIRES_AT"

        fun intent(context: Context, request: PairingRequest, tokenTail: String): Intent =
            Intent(context, PairingConfirmActivity::class.java)
                .putExtra(EXTRA_FINGERPRINT, request.client.fingerprint)
                .putExtra(EXTRA_CLIENT_NAME, request.client.name)
                .putExtra(EXTRA_ADDRESS_CLASS, request.client.addressClass.id)
                .putExtra(EXTRA_TOKEN_TAIL, tokenTail)
                .putExtra(EXTRA_EXPIRES_AT, request.expiresAt)
    }
}
