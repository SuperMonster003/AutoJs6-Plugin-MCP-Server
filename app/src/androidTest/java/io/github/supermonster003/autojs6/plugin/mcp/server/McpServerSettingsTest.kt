package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.mcp.server.server.*
import io.github.supermonster003.autojs6.plugin.mcp.server.store.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.*
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.*
import org.autojs.plugin.mcp.server.api.McpServerContract as C
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Cross-process settings, actual Android windows, and HTTP effects; all private files restored. */
@RunWith(AndroidJUnit4::class)
class McpServerSettingsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val saved = linkedMapOf<String, String?>()
    private var activity: Activity? = null
    private var port = 18437
    private var session: String? = null
    private var token = ""
    private var requestId = 0

    @Before fun prepare() {
        listOf("config.json", "tool_groups.json", "paired_clients.json", "token.json", "lifecycle.json", "status.json").forEach {
            saved[it] = ProcessSharedFile(context, it).read()
        }
        ServerConfigStore(context).save(ServerConfig(port = port))
        ToolPermissionStore(context).reset()
        PairedClientStore(context).clear()
        token = TokenStore(context).current()
    }

    @After fun restore() {
        activity?.let { current -> instrumentation.runOnMainSync { current.finish() } }
        sendService(McpServerService.stopIntent(context))
        await("listener stopped") { !listening(18437) && !listening(18438) }
        saved.forEach { (name, value) -> ProcessSharedFile(context, name).write(value) }
    }

    @Test fun settingsValidatePortsRestoreTheirStateAndProtectSecretWindows() {
        var settings = launch()
        await("settings loaded") { onMain { views(settings.window.decorView).filterIsInstance<TextView>().any { it.text.toString() == settings.getString(R.string.settings_port_value, port) } } }
        onMain { click(settings, R.string.settings_port_value, port) }
        onMain {
            val dialog = checkNotNull(settings.dialog)
            val input = views(dialog.window!!.decorView).filterIsInstance<EditText>().single()
            input.setText("80")
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertNotNull(input.error)
            assertTrue(dialog.isShowing)
            input.setText("18438")
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        }
        await("port persisted") { ServerConfigStore(context).load().port == 18438 }
        val previousActivity = settings
        onMain { settings.recreate() }
        await("new settings activity resumed") { onMain {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<McpServerSettingsActivity>().firstOrNull { it !== previousActivity }
                ?.also { settings = it } != null
        } }
        activity = settings
        await("port restored") { onMain { views(settings.window.decorView).filterIsInstance<TextView>().any { it.text.toString() == settings.getString(R.string.settings_port_value, 18438) } } }
        // A recreated activity can render its saved state before it resumes on older Android.
        // Secret dialogs intentionally ignore requests while the activity is in the background.
        await("recreated settings in foreground") { onMain { settings.hasWindowFocus() } }
        saveEvidence("settings")
        onMain { click(settings, R.string.settings_show_token) }
        await("protected dialog") { onMain { settings.secretDialog?.isShowing == true } }
        onMain {
            val popup = settings.secretDialog!!
            assertTrue(popup.window!!.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            assertFalse(views(settings.window.decorView).filterIsInstance<TextView>().any { it.text.toString().contains(token) })
            popup.dismiss()
            assertFalse(popup.isShowing)
        }
        val historyMonitor = instrumentation.addMonitor(ReleaseHistoryActivity::class.java.name, null, false)
        onMain { click(settings, R.string.settings_release_history) }
        val history = instrumentation.waitForMonitorWithTimeout(historyMonitor, 10_000)
        instrumentation.removeMonitor(historyMonitor)
        assertNotNull(history)
        onMain {
            assertEquals(settings.getString(R.string.settings_release_history), history.title)
            assertTrue(views(history.window.decorView).filterIsInstance<TextView>().any { it.text.toString().contains("1.0.0") })
        }
        await("history in foreground") { onMain { history.hasWindowFocus() } }
        saveEvidence("history")
        onMain {
            history.finish()
            settings.finish()
        }
    }

    @Test fun privateSettingsReceiverHotAppliesPortGroupsTokenAndStop() {
        sendService(McpServerService.startIntent(context, port))
        await("listener started") { listening(port) && !ServerLifecycleStore(context).userStopped }
        assertFalse(ServerLifecycleStore(context).userStopped)
        initialize()
        val name = "McpSettingsTest"
        val fingerprint = PairingGate.fingerprintOf(name, AddressClass.LOOPBACK)
        PairedClientStore(context).put(PairedClient(fingerprint, name, "1", AddressClass.LOOPBACK, 1L, 1L))
        assertFalse(call("tools/call", JSONObject().put("name", "device_ping").put("arguments", JSONObject())).second.getJSONObject("result").optBoolean("isError"))

        val stream = connection("GET", token)
        val reader = stream.inputStream.bufferedReader()
        val worker = Executors.newSingleThreadExecutor()
        val changed = worker.submit<Boolean> {
            generateSequence { reader.readLine() }.any { it.startsWith("data:") && it.contains("notifications/tools/list_changed") }
        }
        try {
            ToolPermissionStore(context).update { it.with(ToolGroup.SCRIPT, false) }
            McpSettingsReceiver.send(context, McpSettingsReceiver.REFRESH)
            assertTrue("clients receive a tool list change", changed.get(15, TimeUnit.SECONDS))
        } finally { stream.disconnect(); worker.shutdownNow() }
        val list = call("tools/list").second.getJSONObject("result")
        assertEquals(0, list.getJSONObject("_meta").getInt("ttlMs"))
        val names = list.getJSONArray("tools").let { tools -> (0 until tools.length()).map { tools.getJSONObject(it).getString("name") } }
        assertFalse(names.contains("script_run"))

        val previous = token
        token = TokenStore(context).rotate()
        assertEquals(401, call("ping", authorization = previous).first)
        assertEquals(200, call("ping").first)
        assertFalse(call("tools/call", JSONObject().put("name", "device_ping").put("arguments", JSONObject())).second.getJSONObject("result").optBoolean("isError"))

        ServerConfigStore(context).update { it.copy(port = 18438) }
        McpSettingsReceiver.send(context, McpSettingsReceiver.APPLY)
        await("new listener and released old port") { !listening(18437) && listening(18438) }
        port = 18438
        session = null
        initialize()
        await("shared running status") { ServerStatusStore(context).load().port == port }
        PairedClientStore(context).remove(fingerprint)
        assertEquals(McpErrors.PAIRING_REQUIRED, call("tools/call", JSONObject().put("name", "device_ping").put("arguments", JSONObject())).second.getJSONObject("error").getInt("code"))

        McpSettingsReceiver.send(context, McpSettingsReceiver.STOP)
        await("stop through settings") { !listening(port) && ServerLifecycleStore(context).userStopped }
        McpSettingsReceiver.send(context, McpSettingsReceiver.APPLY)
        Thread.sleep(500)
        assertFalse("editing a stopped server must not start it", listening(port))
    }

    private fun launch(): McpServerSettingsActivity =
        (instrumentation.startActivitySync(Intent(context, McpServerSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as McpServerSettingsActivity).also { activity = it }

    private fun initialize() {
        val params = JSONObject().put("protocolVersion", "2025-06-18").put("capabilities", JSONObject())
            .put("clientInfo", JSONObject().put("name", "McpSettingsTest").put("version", "1"))
        assertEquals(200, call("initialize", params).first)
        assertNotNull(session)
        assertTrue(call("notifications/initialized").first in 200..202)
    }

    private fun connection(method: String, credential: String): HttpURLConnection =
        (URL("http://127.0.0.1:$port/mcp").openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 5000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json, text/event-stream")
            setRequestProperty("Authorization", "Bearer $credential")
            setRequestProperty("MCP-Protocol-Version", "2025-06-18")
            session?.let { setRequestProperty("Mcp-Session-Id", it) }
        }

    private fun call(method: String, params: JSONObject = JSONObject(), authorization: String = token): Pair<Int, JSONObject> {
        val body = JSONObject().put("jsonrpc", "2.0").put("method", method).put("params", params)
        val id = ++requestId
        if (!method.startsWith("notifications/")) body.put("id", id)
        val connection = connection("POST", authorization)
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = connection.responseCode
            if (method == "initialize") session = connection.getHeaderField("Mcp-Session-Id")
            val input = if (status >= 400) connection.errorStream else connection.inputStream
            val text = input?.bufferedReader()?.use { reader ->
                if (connection.contentType.orEmpty().startsWith("text/event-stream")) {
                    generateSequence { reader.readLine() }.filter { it.startsWith("data:") }.map { it.removePrefix("data:").trim() }
                        .firstOrNull { JSONObject(it).optInt("id", -1) == id }.orEmpty()
                } else reader.readText()
            }.orEmpty()
            return status to if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally { connection.disconnect() }
    }

    private fun sendService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }
    private fun saveEvidence(name: String) {
        if (InstrumentationRegistry.getArguments().getString("mcpUiEvidence") != "true") return
        // Called only on non-secret pages; never capture the token or client configuration popup.
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: return
        try {
            context.cacheDir.resolve("p4-$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { screenshot.recycle() }
    }
    private fun listening(port: Int): Boolean = runCatching { Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200) }; true }.getOrDefault(false)
    private fun await(description: String, condition: () -> Boolean) {
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (!condition()) { if (System.nanoTime() >= until) fail("Timeout: $description"); Thread.sleep(100) }
    }
    private fun <T> onMain(action: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(action) }
        return result!!.getOrThrow()
    }
    private fun views(root: View): List<View> = listOf(root) + if (root is ViewGroup) (0 until root.childCount).flatMap { views(root.getChildAt(it)) } else emptyList()
    private fun click(activity: Activity, string: Int, vararg args: Any) {
        val text = activity.getString(string, *args)
        views(activity.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == text && it.isClickable }.performClick()
    }
}
