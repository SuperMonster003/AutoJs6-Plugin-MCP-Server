package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig

/**
 * The daily "still listening on the local network" reminder (roadmap P5.1): while a running
 * listener accepts LAN connections and the user kept the reminder on, the foreground service
 * posts one dismissable notification every [INTERVAL_MS]. The policy is pure so the JVM tests
 * cover it; the service owns the timer.
 */
object LanReminder {

    const val INTERVAL_MS: Long = 24L * 60L * 60L * 1000L

    /** True when a reminder timer should be armed for the current listener. */
    fun wanted(running: Boolean, config: ServerConfig?): Boolean =
        running && config != null && config.bindScope == BindScope.LAN && config.lanReminder

    /** The address to name in the reminder: the first LAN endpoint, else whatever is listed. */
    fun endpoint(endpoints: List<String>): String =
        endpoints.firstOrNull { !it.startsWith("http://127.0.0.1:") } ?: endpoints.firstOrNull().orEmpty()
}
