package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ServerStatus
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ProcessSharedFile
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfigStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerLifecycleStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerStatusStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.TokenStore
import org.autojs.plugin.mcp.server.api.McpServerContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * The idle auto-stop option on a device (roadmap P6): a listener whose clients send no request
 * for the configured minutes stops itself, records the plugin-only `idle_timeout` reason, does
 * not count as a user stop, and leaves one auto-cancelling notification behind. All private
 * files are restored.
 */
@RunWith(AndroidJUnit4::class)
class McpServerIdleStopTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val saved = linkedMapOf<String, String?>()
    private val port = 18439

    @Before
    fun prepare() {
        listOf("config.json", "lifecycle.json", "status.json").forEach { saved[it] = ProcessSharedFile(context, it).read() }
        ServerConfigStore(context).save(ServerConfig(port = port, idleStopMinutes = 1))
        notifications().cancel(McpServerService.IDLE_STOP_NOTIFICATION_ID)
    }

    @After
    fun restore() {
        sendService(McpServerService.stopIntent(context))
        await("listener stopped", 20) { !listening(port) }
        notifications().cancel(McpServerService.IDLE_STOP_NOTIFICATION_ID)
        saved.forEach { (name, value) -> ProcessSharedFile(context, name).write(value) }
    }

    @Test
    fun aListenerWithoutRequestsStopsItselfAfterTheConfiguredMinutes() {
        sendService(McpServerService.startIntent(context, port))
        await("listener started", 20) { listening(port) }
        assertFalse(ServerLifecycleStore(context).userStopped)
        // One authenticated request, so the countdown provably runs from real client traffic.
        assertEquals(200, initialize())
        val startedAt = System.nanoTime()
        await("idle stop", 110) { !listening(port) }
        val elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt)
        assertTrue("stopped $elapsedSeconds s after the last request, expected about 60", elapsedSeconds in 50..110)
        await("shared stopped status", 10) { ServerStatusStore(context).load().state == McpServerContract.STATE_STOPPED }
        assertEquals(ServerStatus.REASON_IDLE_TIMEOUT, ServerStatusStore(context).load().lastErrorCode)
        assertFalse("an idle stop is not a user stop", ServerLifecycleStore(context).userStopped)
        if (Build.VERSION.SDK_INT >= 23) {
            await("idle notification", 10) { notifications().activeNotifications.any { it.id == McpServerService.IDLE_STOP_NOTIFICATION_ID } }
            // The foreground notification goes with the service; its removal is posted to the main thread.
            await("foreground notification removed", 10) { notifications().activeNotifications.none { it.id == McpServerService.NOTIFICATION_ID } }
        }
    }

    private fun initialize(): Int {
        val connection = URL("http://127.0.0.1:$port/mcp").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Host", "127.0.0.1:$port")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json, text/event-stream")
            connection.setRequestProperty("Authorization", "Bearer ${TokenStore(context).current()}")
            connection.outputStream.use {
                it.write("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"McpServerIdleStopTest","version":"1"}}}""".toByteArray())
            }
            val status = connection.responseCode
            (if (status < 400) connection.inputStream else connection.errorStream)?.use { it.readBytes() }
            return status
        } finally {
            connection.disconnect()
        }
    }

    private fun sendService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }

    private fun notifications(): NotificationManager = context.getSystemService(NotificationManager::class.java)

    private fun listening(port: Int): Boolean =
        runCatching { Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200) }; true }.getOrDefault(false)

    private fun await(description: String, seconds: Long, condition: () -> Boolean) {
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds)
        while (!condition()) {
            if (System.nanoTime() >= until) fail("Timeout: $description")
            Thread.sleep(250)
        }
    }
}
