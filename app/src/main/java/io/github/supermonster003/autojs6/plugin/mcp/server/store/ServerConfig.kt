package io.github.supermonster003.autojs6.plugin.mcp.server.store

import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import io.github.supermonster003.autojs6.plugin.mcp.server.server.HostHeaders

/**
 * Where the listener binds (roadmap D6): the loopback interface for `adb forward`, or every
 * interface after the user's explicit LAN opt-in.
 */
enum class BindScope(val id: String, val bindAddress: String) {

    LOOPBACK("loopback", "127.0.0.1"),
    LAN("lan", "0.0.0.0");

    companion object {

        fun fromId(id: String?): BindScope? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The listener configuration (roadmap P2.1). It is a plain value so the JVM tests cover the
 * validation and the persistence codec; [ServerConfigStore] keeps it in the plugin's private storage
 * and the host may override [port] and [bindScope] through `openServer` (P2.3).
 *
 * [extraAllowedHosts] are additional `Host` header names accepted in LAN mode, such as an mDNS
 * name; the current IPv4 addresses are added at runtime. [developerMode] lets the MCP Inspector's
 * browser page (a loopback origin) reach the endpoint through CORS; it is off by default.
 */
data class ServerConfig(
    val port: Int = McpServerPlugin.DEFAULT_PORT,
    val bindScope: BindScope = BindScope.LOOPBACK,
    val developerMode: Boolean = false,
    val extraAllowedHosts: List<String> = emptyList(),
) {

    val bindAddress: String
        get() = bindScope.bindAddress

    /** Reasons this configuration cannot be applied; empty when it is usable. */
    fun problems(): List<String> = buildList {
        if (!isValidPort(port)) {
            add("port must be between $MIN_PORT and $MAX_PORT, got $port")
        }
        if (extraAllowedHosts.size > MAX_EXTRA_ALLOWED_HOSTS) {
            add("at most $MAX_EXTRA_ALLOWED_HOSTS extra host names are allowed, got ${extraAllowedHosts.size}")
        }
        extraAllowedHosts.forEach { host ->
            if (normalizeHost(host) != host) add("invalid host name: $host")
        }
    }

    fun withOverrides(port: Int? = null, bindScope: BindScope? = null, developerMode: Boolean? = null): ServerConfig =
        copy(
            port = port ?: this.port,
            bindScope = bindScope ?: this.bindScope,
            developerMode = developerMode ?: this.developerMode,
        )

    /** String-only form shared by the preference store and the host-facing Bundles. */
    fun toMap(): Map<String, String> = mapOf(
        KEY_PORT to port.toString(),
        KEY_BIND_SCOPE to bindScope.id,
        KEY_DEVELOPER_MODE to developerMode.toString(),
        KEY_EXTRA_ALLOWED_HOSTS to extraAllowedHosts.joinToString(","),
    )

    companion object {

        const val MIN_PORT = 1024
        const val MAX_PORT = 65535
        const val MAX_EXTRA_ALLOWED_HOSTS = 8

        const val KEY_PORT = "port"
        const val KEY_BIND_SCOPE = "bind_scope"
        const val KEY_DEVELOPER_MODE = "developer_mode"
        const val KEY_EXTRA_ALLOWED_HOSTS = "extra_allowed_hosts"

        val KEYS: List<String> = listOf(KEY_PORT, KEY_BIND_SCOPE, KEY_DEVELOPER_MODE, KEY_EXTRA_ALLOWED_HOSTS)

        fun isValidPort(port: Int): Boolean = port in MIN_PORT..MAX_PORT

        /**
         * The lowercase host name a `Host` header would carry for [host], without a port; null when
         * it is not a DNS name, an IPv4 literal, or a bracketed IPv6 literal.
         */
        fun normalizeHost(host: String): String? = HostHeaders.hostnameOf(host)

        /**
         * Tolerant decoder for persisted or received values: every missing or invalid entry falls
         * back to its default instead of failing, so a stale preference never blocks the server.
         */
        fun fromMap(values: Map<String, String?>): ServerConfig {
            val defaults = ServerConfig()
            val port = values[KEY_PORT]?.trim()?.toIntOrNull()?.takeIf(::isValidPort) ?: defaults.port
            val bindScope = BindScope.fromId(values[KEY_BIND_SCOPE]?.trim()?.lowercase()) ?: defaults.bindScope
            val developerMode = values[KEY_DEVELOPER_MODE]?.trim()?.lowercase()?.toBooleanStrictOrNull() ?: defaults.developerMode
            val extraAllowedHosts = values[KEY_EXTRA_ALLOWED_HOSTS].orEmpty()
                .split(',')
                .mapNotNull { normalizeHost(it) }
                .distinct()
                .take(MAX_EXTRA_ALLOWED_HOSTS)
            return ServerConfig(port, bindScope, developerMode, extraAllowedHosts)
        }
    }
}
