package io.github.supermonster003.autojs6.plugin.mcp.server.server

/**
 * Parsing of the `Host` and `Origin` request headers for the DNS rebinding protection of
 * [RequestGate] (roadmap P2.1). Pure Kotlin so the JVM tests cover every accepted and rejected
 * shape; the SDK's own validator is bypassed because the allowed list changes at runtime.
 */
object HostHeaders {

    private val HOST_NAME = Regex("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*")
    private val IPV6_LITERAL = Regex("\\[[0-9a-f:.]+(%[a-z0-9_.-]+)?]")
    private val LOOPBACK_IPV4 = Regex("127\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")

    /**
     * The lowercase host name of a `Host` header without its port (`Example:8080` -> `example`,
     * `[::1]:9637` -> `[::1]`); null when the header is missing, blank, or malformed.
     */
    fun hostnameOf(hostHeader: String?): String? {
        val value = hostHeader?.trim()?.lowercase() ?: return null
        if (value.isEmpty() || value.any { it.isWhitespace() || it.isISOControl() }) return null
        val host: String
        val portSuffix: String
        if (value.startsWith('[')) {
            val end = value.indexOf(']')
            if (end < 0) return null
            host = value.substring(0, end + 1)
            portSuffix = value.substring(end + 1)
            if (!IPV6_LITERAL.matches(host)) return null
        } else {
            val colon = value.indexOf(':')
            host = if (colon >= 0) value.substring(0, colon) else value
            portSuffix = if (colon >= 0) value.substring(colon) else ""
            if (!HOST_NAME.matches(host)) return null
        }
        if (portSuffix.isNotEmpty() && !isValidPortSuffix(portSuffix)) return null
        return host
    }

    /**
     * The lowercase host name of an `Origin` header (`http://localhost:6274` -> `localhost`); null
     * for the opaque `null` origin, non-HTTP schemes, or anything that is not `scheme://host[:port]`.
     */
    fun originHostOf(origin: String?): String? {
        val value = origin?.trim() ?: return null
        val schemeEnd = value.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = value.substring(0, schemeEnd).lowercase()
        if (scheme != "http" && scheme != "https") return null
        val authority = value.substring(schemeEnd + 3)
        if (authority.isEmpty() || authority.any { it == '/' || it == '?' || it == '#' || it == '@' }) return null
        return hostnameOf(authority)
    }

    /** `localhost`, the whole `127.0.0.0/8` block, and the IPv6 loopback literal. */
    fun isLoopbackName(host: String): Boolean =
        host == "localhost" || host == "[::1]" || LOOPBACK_IPV4.matches(host)

    private fun isValidPortSuffix(suffix: String): Boolean {
        if (suffix.length < 2 || suffix[0] != ':') return false
        val digits = suffix.substring(1)
        if (digits.length > 5 || digits.any { it !in '0'..'9' }) return false
        return digits.toInt() in 1..65535
    }
}
