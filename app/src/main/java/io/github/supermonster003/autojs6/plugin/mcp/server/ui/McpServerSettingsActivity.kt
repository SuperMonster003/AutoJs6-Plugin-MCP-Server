package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.github.supermonster003.autojs6.plugin.mcp.server.LocalNetworkAccess
import io.github.supermonster003.autojs6.plugin.mcp.server.LocalNetworkPermissionActivity
import io.github.supermonster003.autojs6.plugin.mcp.server.mcpServerPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.mcp.server.R
import io.github.supermonster003.autojs6.plugin.mcp.server.host.SessionStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.server.BackgroundRestriction
import io.github.supermonster003.autojs6.plugin.mcp.server.server.LanAddressWatcher
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpHttpServer
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ServerStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.store.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.*
import org.autojs.plugin.mcp.server.api.McpServerContract as C
import java.util.concurrent.Executors
import java.util.Locale

/** Settings run in the main process; only the private receiver reaches the listener process. */
class McpServerSettingsActivity : SettingsPageActivity() {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { Thread(it, "mcp-settings-store") }
    private val configStore by lazy { ServerConfigStore(this) }
    private val groups by lazy { ToolPermissionStore(this) }
    private val tokens by lazy { TokenStore(this) }
    private val clients by lazy { PairedClientStore(this) }
    private val statuses by lazy { ServerStatusStore(this) }
    private var config = ServerConfig()
    private var status = SessionStatus(C.STATE_STOPPED)
    private var paired = emptyList<PairedClient>()
    private var lanUrls = emptyList<String>()
    private var ready = false
    private var resumed = false
    private var reading = false
    private var rendering = false
    internal var dialog: AlertDialog? = null
        private set
    internal var secretDialog: AlertDialog? = null
        private set
    private lateinit var stateText: TextView
    private lateinit var endpointText: TextView
    private lateinit var portButton: Button
    private lateinit var stopButton: Button
    private lateinit var idleStopButton: Button
    private lateinit var restrictedHint: TextView
    private lateinit var batteryButton: Button
    private lateinit var tokenText: TextView
    private lateinit var lan: Switch
    private lateinit var lanAddressText: TextView
    private lateinit var lanHint: TextView
    private lateinit var lanReminder: Switch
    private lateinit var developer: Switch
    private lateinit var rootShell: Switch
    private lateinit var pairedRows: LinearLayout
    private val groupSwitches = linkedMapOf<ToolGroup, Switch>()
    private val refresh = object : Runnable {
        override fun run() {
            if (resumed && !reading) work {}
            if (resumed) main.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page(getString(R.string.settings_title))
        card(R.string.settings_connection).also { box ->
            stateText = label(getString(R.string.settings_loading), box, 20f)
            endpointText = label("", box).apply { setTextIsSelectable(true) }
            label(getString(R.string.settings_connection_hint), box).setTextColor(secondary)
            button(R.string.settings_copy_adb, box) { if (ready) copy(ClientConfigSnippet.adbForward(config.port), false) }
            stopButton = button(R.string.server_action_stop, box) { work { McpSettingsReceiver.send(this, McpSettingsReceiver.STOP) } }
            idleStopButton = button(R.string.settings_idle_stop_off, box, ::chooseIdleStop)
            label(getString(R.string.settings_idle_stop_hint), box).setTextColor(secondary)
            restrictedHint = label(getString(R.string.settings_background_restricted), box).apply { visibility = View.GONE }
            batteryButton = button(R.string.settings_battery_settings, box, ::batterySettings).apply { visibility = View.GONE }
            button(R.string.settings_open_host, box, ::openHost)
            button(R.string.settings_notifications, box, ::notifications)
        }
        card(R.string.settings_network).also { box ->
            if (Build.VERSION.SDK_INT >= 37) {
                label(getString(R.string.local_network_explanation), box).setTextColor(secondary)
                button(R.string.local_network_allow, box, ::localNetworkPermission)
            }
            portButton = button(R.string.settings_port, box, ::editPort)
            lan = toggle(R.string.settings_lan, box) { enabled ->
                if (enabled) confirm(R.string.settings_lan, R.string.settings_lan_warning) {
                    if (LocalNetworkAccess.isGranted(this)) saveConfig { it.copy(bindScope = BindScope.LAN) }
                    else {
                        lan.isChecked = false
                        localNetworkPermission()
                    }
                } else saveConfig { it.copy(bindScope = BindScope.LOOPBACK) }
            }
            lanAddressText = label("", box).apply { setTextIsSelectable(true); visibility = View.GONE }
            lanHint = label(getString(R.string.settings_lan_hint), box).apply { setTextColor(secondary); visibility = View.GONE }
            lanReminder = toggle(R.string.settings_lan_reminder, box) { enabled -> saveConfig { it.copy(lanReminder = enabled) } }
            label(getString(R.string.settings_network_hint), box).setTextColor(secondary)
        }
        card(R.string.settings_token).also { box ->
            tokenText = label("", box)
            label(getString(R.string.settings_token_hint), box).setTextColor(secondary)
            button(R.string.settings_show_token, box) { work {
                val token = tokens.current()
                main.post { if (resumed) showSecret(getString(R.string.settings_token), token) }
            } }
            button(R.string.settings_rotate_token, box) { confirm(R.string.settings_rotate_token, R.string.settings_rotate_warning) {
                work {
                    val fresh = tokens.rotate()
                    check(tokens.current() == fresh)
                    McpSettingsReceiver.send(this, McpSettingsReceiver.REFRESH)
                }
            } }
        }
        card(R.string.settings_paired_clients).also { box ->
            pairedRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            box.addView(pairedRows)
            button(R.string.settings_revoke_all, box) { confirm(R.string.settings_revoke_all, R.string.settings_revoke_warning) {
                work { clients.clear(); check(clients.all().isEmpty()); McpSettingsReceiver.send(this, McpSettingsReceiver.REFRESH) }
            } }
        }
        card(R.string.settings_tools).also { box ->
            label(getString(R.string.settings_tools_hint), box).setTextColor(secondary)
            ToolGroup.entries.forEach { group ->
                val title = groupTitle(group)
                groupSwitches[group] = toggle(title, box) { enabled ->
                    val save = { work { groups.update { it.with(group, enabled) }; McpSettingsReceiver.send(this, McpSettingsReceiver.REFRESH) } }
                    if (enabled && !group.defaultEnabled) confirm(getString(title), getString(R.string.settings_dangerous_group, getString(title)), save)
                    else save()
                }
            }
            rootShell = toggle(R.string.settings_shell_root, box) { enabled ->
                val save = { work { groups.update { it.copy(allowShellRoot = enabled) }; McpSettingsReceiver.send(this, McpSettingsReceiver.REFRESH) } }
                if (enabled) confirm(getString(R.string.settings_shell_root), getString(R.string.settings_root_warning), save) else save()
            }
        }
        card(R.string.settings_clients).also { box ->
            label(getString(R.string.settings_clients_hint), box).setTextColor(secondary)
            McpClient.entries.forEach { client -> button(clientTitle(client), box) { chooseEndpoint(client) } }
        }
        card(R.string.settings_more).also { box ->
            developer = toggle(R.string.settings_developer, box) { enabled ->
                if (enabled) confirm(R.string.settings_developer, R.string.settings_developer_warning) { saveConfig { it.copy(developerMode = true) } }
                else saveConfig { it.copy(developerMode = false) }
            }
            button(R.string.settings_release_history, box) { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) }
            button(R.string.settings_about, box) {
                val info = mcpServerPluginRuntimeInfo()
                dialog = AlertDialog.Builder(this).setTitle(R.string.settings_about)
                    .setMessage(getString(R.string.settings_about_text, info.versionName, info.versionCode))
                    .setPositiveButton(android.R.string.ok, null).showThemed()
            }
        }
        showClients(emptyList(), force = true)
    }

    override fun onResume() {
        super.onResume()
        if (HostAppearance.read(this) != appearance) { recreate(); return }
        resumed = true
        main.post(refresh)
    }

    override fun onPause() {
        resumed = false
        main.removeCallbacks(refresh)
        secretDialog?.dismiss()
        secretDialog = null
        dialog?.dismiss()
        dialog = null
        super.onPause()
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        io.shutdown()
        super.onDestroy()
    }

    private fun work(action: () -> Unit) {
        if (io.isShutdown) return
        reading = true
        io.execute {
            val result = runCatching {
                action()
                val config = configStore.load()
                // While the listener is down the LAN addresses come from the interfaces directly.
                val lanUrls = if (config.bindScope == BindScope.LAN) McpHttpServer.endpointUrls(config, LanAddressWatcher.currentAddresses()).drop(1) else emptyList()
                Snapshot(config, statuses.load(), groups.load(), clients.all(), tokens.tail(), lanUrls, BackgroundRestriction.isRestricted(this))
            }
            main.post {
                reading = false
                if (!isDestroyed) result.onSuccess(::render).onFailure { toast(R.string.settings_save_failed) }
            }
        }
    }

    private data class Snapshot(
        val config: ServerConfig, val status: SessionStatus, val groups: ToolPermissions, val clients: List<PairedClient>, val tokenTail: String,
        val lanUrls: List<String>, val restricted: Boolean,
    )

    private fun render(snapshot: Snapshot) {
        config = snapshot.config; status = snapshot.status; ready = true; rendering = true
        try {
            stateText.text = getString(when (status.state) {
                C.STATE_RUNNING -> if (status.hostAvailable) R.string.settings_running else R.string.server_host_disconnected
                C.STATE_STARTING -> R.string.server_state_starting
                C.STATE_STOPPING -> R.string.settings_stopping
                C.STATE_FAILED -> R.string.settings_failed
                C.STATE_STOPPED -> if (status.lastErrorCode == ServerStatus.REASON_IDLE_TIMEOUT) R.string.settings_stopped_idle else R.string.server_state_stopped
                else -> R.string.server_state_stopped
            })
            endpointText.text = status.endpoints.joinToString("\n").ifEmpty { "http://127.0.0.1:${config.port}/mcp" }
            stopButton.isEnabled = status.state in setOf(C.STATE_RUNNING, C.STATE_STARTING)
            idleStopButton.text = if (config.idleStopMinutes > 0) getString(R.string.settings_idle_stop_value, config.idleStopMinutes) else getString(R.string.settings_idle_stop_off)
            restrictedHint.visibility = if (snapshot.restricted) View.VISIBLE else View.GONE
            batteryButton.visibility = restrictedHint.visibility
            portButton.text = getString(R.string.settings_port_value, config.port)
            val lanOn = config.bindScope == BindScope.LAN
            lan.isChecked = lanOn
            lanUrls = snapshot.lanUrls
            lanAddressText.visibility = if (lanOn) View.VISIBLE else View.GONE
            lanHint.visibility = lanAddressText.visibility
            lanAddressText.text = if (lanUrls.isEmpty()) getString(R.string.settings_lan_no_address) else getString(R.string.settings_lan_addresses, lanUrls.joinToString("\n"))
            lanReminder.isChecked = config.lanReminder
            lanReminder.isEnabled = lanOn
            developer.isChecked = config.developerMode
            tokenText.text = getString(R.string.settings_token_masked, snapshot.tokenTail)
            groupSwitches.forEach { (group, view) -> view.isChecked = snapshot.groups.isEnabled(group) }
            rootShell.isChecked = snapshot.groups.allowShellRoot
            rootShell.isEnabled = snapshot.groups.isEnabled(ToolGroup.SHELL)
            showClients(snapshot.clients)
        } finally { rendering = false }
    }

    private fun saveConfig(transform: (ServerConfig) -> ServerConfig) = work {
        configStore.update(transform)
        McpSettingsReceiver.send(this, McpSettingsReceiver.APPLY)
    }

    private fun editPort() {
        if (!ready) return
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(5))
            setText(String.format(Locale.ROOT, "%d", config.port)); selectAll(); contentDescription = getString(R.string.settings_port)
        }
        val box = LinearLayout(this).apply { setPadding(dp(24), dp(8), dp(24), dp(8)); addView(input, LinearLayout.LayoutParams(-1, -2)) }
        dialog = AlertDialog.Builder(this).setTitle(R.string.settings_port).setMessage(R.string.settings_port_hint).setView(box)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.settings_save, null).create().also { popup ->
                popup.setOnShowListener { popup.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val port = input.text.toString().toIntOrNull()
                    if (port == null || port !in 1024..65535) input.error = getString(R.string.settings_port_hint)
                    else { saveConfig { it.copy(port = port) }; popup.dismiss() }
                } }
                popup.show()
                applyDialogTheme(popup)
            }
    }

    private fun chooseIdleStop() {
        if (!ready) return
        val labels = IDLE_STOP_CHOICES.map { minutes ->
            if (minutes == 0) getString(R.string.settings_idle_stop_choice_off) else getString(R.string.settings_idle_stop_choice, minutes)
        }.toTypedArray()
        dialog = AlertDialog.Builder(this).setTitle(R.string.settings_idle_stop)
            .setSingleChoiceItems(labels, IDLE_STOP_CHOICES.indexOf(config.idleStopMinutes)) { popup, which ->
                saveConfig { it.copy(idleStopMinutes = IDLE_STOP_CHOICES[which]) }
                popup.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null).showThemed()
    }

    private fun batterySettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }.onFailure { toast(R.string.settings_save_failed) }
    }

    private fun toggle(titleId: Int, parent: LinearLayout, change: (Boolean) -> Unit): Switch = Switch(this).apply {
        setText(titleId); textSize = 16f; setTextColor(textColor); minHeight = dp(56)
        setPadding(0, dp(8), 0, dp(8))
        tintSwitch(this)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
        setOnCheckedChangeListener { _, enabled ->
            if (!rendering && ready) {
                // Show the committed value until confirmation and persistence complete.
                rendering = true; isChecked = !enabled; rendering = false
                change(enabled)
            }
        }
    }

    private fun showClients(next: List<PairedClient>, force: Boolean = false) {
        if (!force && paired == next) return
        paired = next
        pairedRows.removeAllViews()
        if (next.isEmpty()) label(getString(R.string.settings_no_clients), pairedRows)
        next.forEach { client ->
            val address = getString(if (client.addressClass.id == "lan") R.string.pairing_address_lan else R.string.pairing_address_loopback)
            label("${client.name}\n$address", pairedRows)
            button(R.string.settings_revoke, pairedRows) {
                confirm(getString(R.string.settings_revoke_client, client.name), getString(R.string.settings_revoke_warning)) {
                    work { clients.remove(client.fingerprint); check(clients.all().none { it.fingerprint == client.fingerprint }); McpSettingsReceiver.send(this, McpSettingsReceiver.REFRESH) }
                }
            }.contentDescription = getString(R.string.settings_revoke_client, client.name)
        }
    }

    private fun chooseEndpoint(client: McpClient) {
        if (!ready) return
        val endpoints = (listOf("http://127.0.0.1:${config.port}/mcp") + status.endpoints + lanUrls).distinct()
        val show: (String) -> Unit = { endpoint -> work {
            val snippet = ClientConfigSnippet.create(client, endpoint, tokens.current())
            main.post { if (resumed) showSecret(getString(clientTitle(client)), snippet.text, snippet.environmentCommand, getString(clientHint(client))) }
        } }
        if (endpoints.size == 1) show(endpoints.single())
        else dialog = AlertDialog.Builder(this).setTitle(R.string.settings_choose_endpoint)
            .setItems(endpoints.toTypedArray()) { _, which -> show(endpoints[which]) }.setNegativeButton(android.R.string.cancel, null).showThemed()
    }

    private fun showSecret(title: String, text: String, environment: String? = null, hint: String? = null) {
        secretDialog?.dismiss()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(8)) }
        if (hint != null) label(hint, box).setTextColor(secondary)
        val secret = label(text, box, 14f).apply {
            typeface = android.graphics.Typeface.MONOSPACE; isSaveEnabled = false
            if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            if (Build.VERSION.SDK_INT >= 30) importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        val builder = AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.settings_copy) { _, _ -> copy(text, true) }
        if (environment != null) builder.setNeutralButton(R.string.settings_copy_environment) { _, _ -> copy(environment, true) }
        secretDialog = builder.create().also { popup ->
            popup.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            popup.setOnDismissListener { secret.text = "" }
            popup.show()
            applyDialogTheme(popup)
        }
    }

    private fun copy(text: String, sensitive: Boolean) {
        val clip = ClipData.newPlainText(getString(R.string.app_name), text)
        if (sensitive && Build.VERSION.SDK_INT >= 33) clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        toast(R.string.settings_copied)
    }

    private fun confirm(titleId: Int, messageId: Int, action: () -> Unit) = confirm(getString(titleId), getString(messageId), action)

    private fun confirm(title: String, message: String, action: () -> Unit) {
        dialog = AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> action() }.showThemed()
    }

    private fun openHost() {
        val intent = packageManager.getLaunchIntentForPackage(McpServerPlugin.HOST_PACKAGE_NAME)
        if (intent == null || runCatching { startActivity(intent); true }.getOrDefault(false).not()) toast(R.string.settings_host_unavailable)
    }

    private fun notifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            runCatching { startActivity(intent) }.onFailure { toast(R.string.settings_save_failed) }
        }
    }

    private fun localNetworkPermission() {
        if (Build.VERSION.SDK_INT < 37) return
        if (LocalNetworkAccess.isGranted(this)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } else {
            startActivity(Intent(this, LocalNetworkPermissionActivity::class.java))
        }
    }

    private fun toast(id: Int) = Toast.makeText(this, id, Toast.LENGTH_SHORT).show()

    companion object {
        /** Minutes offered by the idle auto-stop dialog; 0 is off (the default). */
        internal val IDLE_STOP_CHOICES = listOf(0, 5, 15, 30, 60, 120)

        private fun groupTitle(group: ToolGroup): Int = when (group) {
            ToolGroup.SCRIPT -> R.string.settings_group_script
            ToolGroup.UI -> R.string.settings_group_ui
            ToolGroup.UI_GESTURE -> R.string.settings_group_gestures
            ToolGroup.SCREEN -> R.string.settings_group_screen
            ToolGroup.FILES -> R.string.settings_group_files
            ToolGroup.FILES_DELETE -> R.string.settings_group_delete
            ToolGroup.DEVICE -> R.string.settings_group_device
            ToolGroup.SHELL -> R.string.settings_group_shell
        }
        private fun clientTitle(client: McpClient): Int = when (client) {
            McpClient.CLAUDE_CODE -> R.string.settings_client_claude
            McpClient.CURSOR -> R.string.settings_client_cursor
            McpClient.CODEX -> R.string.settings_client_codex
            McpClient.GENERIC -> R.string.settings_client_generic
        }
        private fun clientHint(client: McpClient): Int = when (client) {
            McpClient.CLAUDE_CODE -> R.string.settings_claude_hint
            McpClient.CURSOR -> R.string.settings_cursor_hint
            McpClient.CODEX -> R.string.settings_codex_hint
            McpClient.GENERIC -> R.string.settings_generic_hint
        }
    }
}
