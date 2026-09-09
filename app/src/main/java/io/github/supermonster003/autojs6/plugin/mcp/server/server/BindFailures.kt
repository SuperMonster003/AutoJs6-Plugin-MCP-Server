package io.github.supermonster003.autojs6.plugin.mcp.server.server

import java.net.BindException

/** A listener that could not start, in the words the host status and the notification show. */
data class BindFailure(val code: String, val message: String)

/**
 * Turns the exception of a failed `bind()` into a stable error code plus a hint (roadmap P2.1).
 * The codes mirror `McpServerContract.ERROR_PORT_IN_USE` and `ERROR_BIND_FAILED`.
 */
object BindFailures {

    const val CODE_PORT_IN_USE = "port_in_use"
    const val CODE_BIND_FAILED = "bind_failed"

    fun classify(error: Throwable, address: String, port: Int): BindFailure {
        val chain = causeChain(error)
        val text = chain.joinToString(" | ") { "${it.javaClass.simpleName}: ${it.message.orEmpty()}" }.lowercase()
        return when {
            text.contains("eacces") || text.contains("permission denied") -> BindFailure(
                CODE_BIND_FAILED,
                "Binding $address:$port was denied; ports below 1024 are not available to apps",
            )
            text.contains("eaddrnotavail") || text.contains("cannot assign requested address") -> BindFailure(
                CODE_BIND_FAILED,
                "Address $address is not available on this device",
            )
            text.contains("eaddrinuse") || text.contains("already in use") || chain.any { it is BindException } -> BindFailure(
                CODE_PORT_IN_USE,
                "Port $port on $address is already in use; stop the other listener or choose a different port",
            )
            else -> BindFailure(
                CODE_BIND_FAILED,
                "Could not bind $address:$port (${chain.first().javaClass.simpleName}: ${chain.first().message ?: "no message"})",
            )
        }
    }

    private fun causeChain(error: Throwable): List<Throwable> {
        val chain = mutableListOf<Throwable>()
        var current: Throwable? = error
        while (current != null && current !in chain && chain.size < 8) {
            chain += current
            current = current.cause
        }
        return chain
    }
}
