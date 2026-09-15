package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig

/**
 * What [RequestGate] accepts (roadmap P2.1): the `Host` names a request may carry, whether a
 * browser origin on the loopback interface may use the endpoint (developer mode for the MCP
 * Inspector), and the request body ceiling.
 *
 * The LAN list is rebuilt whenever the device's addresses change, so [McpHttpServer] hands the
 * gate a provider instead of a fixed policy.
 */
data class GatePolicy(
    val allowedHosts: Set<String>,
    val developerMode: Boolean = false,
    val maxBodyBytes: Long = RequestGate.MAX_REQUEST_BODY_BYTES,
) {

    fun isHostAllowed(hostname: String): Boolean = hostname in allowedHosts

    companion object {

        /** Names the loopback interface answers to; always allowed because `adb forward` uses them. */
        val LOOPBACK_HOSTS: Set<String> = setOf("localhost", "127.0.0.1", "[::1]")

        fun loopback(extraHosts: Collection<String> = emptyList(), developerMode: Boolean = false): GatePolicy =
            GatePolicy(LOOPBACK_HOSTS + normalized(extraHosts), developerMode)

        fun lan(addresses: Collection<String>, extraHosts: Collection<String> = emptyList(), developerMode: Boolean = false): GatePolicy =
            GatePolicy(LOOPBACK_HOSTS + normalized(addresses) + normalized(extraHosts), developerMode)

        fun forConfig(config: ServerConfig, lanAddresses: Collection<String>): GatePolicy = when (config.bindScope) {
            BindScope.LOOPBACK -> loopback(config.extraAllowedHosts, config.developerMode)
            BindScope.LAN -> lan(lanAddresses, config.extraAllowedHosts, config.developerMode)
        }

        private fun normalized(hosts: Collection<String>): Set<String> =
            hosts.mapNotNull { HostHeaders.hostnameOf(it) }.toSet()
    }
}

/** The request facts the gate looks at; headers are passed raw so the parsing stays testable. */
data class GateRequest(
    val method: String,
    val host: String?,
    val origin: String?,
    val contentLength: Long? = null,
)

sealed class GateDecision {

    /** Let the request through; [corsOrigin] is the origin to echo in CORS headers (developer mode). */
    data class Allow(val corsOrigin: String? = null) : GateDecision()

    /** Answer a CORS preflight for an allowed browser origin without touching the transport. */
    data class Preflight(val corsOrigin: String) : GateDecision()

    /** Refuse with an HTTP status and a JSON-RPC error body. */
    data class Reject(val status: Int, val code: Int, val message: String) : GateDecision()
}

/**
 * DNS rebinding protection and request limits in front of the SDK transport (roadmap P2.1).
 *
 * Order of checks: the `Host` header must parse and be in the policy's list (a malicious page
 * cannot make a browser send an allowed name for a foreign address); an `Origin` header is only
 * accepted in developer mode and only for loopback origins, because MCP clients on the PC never
 * send one; a preflight for an accepted origin is answered here; declared bodies above the
 * ceiling are refused before the transport reads them.
 */
object RequestGate {

    /** Mirrors `McpServerContract.MAX_REQUEST_BODY_BYTES` (roadmap A.7). */
    const val MAX_REQUEST_BODY_BYTES = 1024L * 1024

    const val STATUS_BAD_REQUEST = 400
    const val STATUS_FORBIDDEN = 403
    const val STATUS_PAYLOAD_TOO_LARGE = 413

    /** JSON-RPC `Invalid Request`, the code the SDK also uses for rejected hosts. */
    const val JSON_RPC_INVALID_REQUEST = McpErrors.INVALID_REQUEST

    fun evaluate(request: GateRequest, policy: GatePolicy): GateDecision {
        val hostname = HostHeaders.hostnameOf(request.host)
            ?: return GateDecision.Reject(STATUS_FORBIDDEN, JSON_RPC_INVALID_REQUEST, "Invalid Host header")
        if (!policy.isHostAllowed(hostname)) {
            return GateDecision.Reject(STATUS_FORBIDDEN, JSON_RPC_INVALID_REQUEST, "Host not allowed: $hostname")
        }
        var corsOrigin: String? = null
        val origin = request.origin
        if (origin != null) {
            val originHost = HostHeaders.originHostOf(origin)
            if (!policy.developerMode || originHost == null || !HostHeaders.isLoopbackName(originHost)) {
                return GateDecision.Reject(STATUS_FORBIDDEN, JSON_RPC_INVALID_REQUEST, "Origin not allowed")
            }
            corsOrigin = origin.trim()
        }
        if (corsOrigin != null && request.method.equals("OPTIONS", ignoreCase = true)) {
            return GateDecision.Preflight(corsOrigin)
        }
        val contentLength = request.contentLength
        if (contentLength != null && contentLength > policy.maxBodyBytes) {
            return rejectBodyTooLarge(policy.maxBodyBytes)
        }
        return GateDecision.Allow(corsOrigin)
    }

    /** The `413` answer for a declared or streamed body above [maxBodyBytes]. */
    fun rejectBodyTooLarge(maxBodyBytes: Long): GateDecision.Reject =
        GateDecision.Reject(STATUS_PAYLOAD_TOO_LARGE, JSON_RPC_INVALID_REQUEST, "Request body exceeds $maxBodyBytes bytes")

    /** The `400` answer for a body that fails a structural check (roadmap P6). */
    fun rejectBody(message: String): GateDecision.Reject =
        GateDecision.Reject(STATUS_BAD_REQUEST, JSON_RPC_INVALID_REQUEST, message)
}
