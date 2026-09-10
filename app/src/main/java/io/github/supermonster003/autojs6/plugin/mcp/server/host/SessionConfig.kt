package io.github.supermonster003.autojs6.plugin.mcp.server.host

import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import org.autojs.plugin.mcp.server.api.McpServerContract

/**
 * What the host asks for in `openServer` / `updateConfig` (contract keys `KEY_SERVER_CONFIG_*`),
 * decoded from primitives so the JVM tests cover the validation. A rejected value throws an
 * `IllegalArgumentException` whose message starts with the contract error code, which Binder
 * carries back to the host unchanged.
 */
data class SessionConfig(
    val port: Int = McpServerContract.DEFAULT_PORT,
    val bindScope: BindScope = BindScope.LOOPBACK,
    val hostLabel: String? = null,
) {

    companion object {

        const val MAX_HOST_LABEL_LENGTH = 64

        fun fromValues(
            contractVersion: Int?,
            port: Int?,
            bindScope: String?,
            protocolMode: String?,
            hostLabel: String?,
        ): SessionConfig {
            if (contractVersion != null && !McpServerContract.supportsContractVersion(contractVersion)) {
                throw IllegalArgumentException("${McpServerContract.ERROR_UNSUPPORTED_CONTRACT}: contract version $contractVersion is not supported by this plugin build")
            }
            val effectivePort = port ?: McpServerContract.DEFAULT_PORT
            if (!McpServerContract.isValidPort(effectivePort)) {
                invalid("port must be within ${McpServerContract.MIN_PORT}..${McpServerContract.MAX_PORT}, got $effectivePort")
            }
            val scope = bindScope?.let { BindScope.fromId(it.trim().lowercase()) ?: invalid("unknown bind scope: $it") } ?: BindScope.LOOPBACK
            val mode = protocolMode?.trim()?.lowercase() ?: McpServerContract.PROTOCOL_MODE_STATEFUL
            if (mode != McpServerContract.PROTOCOL_MODE_STATEFUL) {
                invalid("protocol mode $mode is not supported; only ${McpServerContract.PROTOCOL_MODE_STATEFUL} is mounted (roadmap D9)")
            }
            val label = hostLabel?.trim()?.takeIf { it.isNotEmpty() }
            if (label != null && label.length > MAX_HOST_LABEL_LENGTH) {
                invalid("host label exceeds $MAX_HOST_LABEL_LENGTH characters")
            }
            return SessionConfig(effectivePort, scope, label)
        }

        private fun invalid(detail: String): Nothing =
            throw IllegalArgumentException("${McpServerContract.ERROR_INVALID_CONFIG}: $detail")
    }
}

/** The listener state as the session Binder reports it (contract keys `KEY_STATUS_*`). */
data class SessionStatus(
    val state: String,
    val endpoints: List<String> = emptyList(),
    val port: Int = 0,
    val bindScope: String = McpServerContract.BIND_SCOPE_LOOPBACK,
    val protocolMode: String = McpServerContract.PROTOCOL_MODE_STATEFUL,
    val pairedClientCount: Int = 0,
    val activeSessionCount: Int = 0,
    val startedAt: Long = 0L,
    val hostAvailable: Boolean = false,
    val lastErrorCode: String? = null,
    val lastError: String? = null,
)
