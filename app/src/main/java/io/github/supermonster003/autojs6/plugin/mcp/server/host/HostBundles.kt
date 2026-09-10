package io.github.supermonster003.autojs6.plugin.mcp.server.host

import android.os.Bundle
import org.autojs.plugin.mcp.server.api.McpServerContract

/** Bundle codecs of the session Binder; the keys are the contract literals. */
internal object HostBundles {

    fun sessionConfig(bundle: Bundle?): SessionConfig = SessionConfig.fromValues(
        contractVersion = bundle?.takeIf { it.containsKey(McpServerContract.KEY_CONTRACT_VERSION) }?.getInt(McpServerContract.KEY_CONTRACT_VERSION),
        port = bundle?.takeIf { it.containsKey(McpServerContract.KEY_SERVER_CONFIG_PORT) }?.getInt(McpServerContract.KEY_SERVER_CONFIG_PORT),
        bindScope = bundle?.getString(McpServerContract.KEY_SERVER_CONFIG_BIND_SCOPE),
        protocolMode = bundle?.getString(McpServerContract.KEY_SERVER_CONFIG_PROTOCOL_MODE),
        hostLabel = bundle?.getString(McpServerContract.KEY_SERVER_CONFIG_HOST_LABEL),
    )

    fun status(status: SessionStatus): Bundle = Bundle().apply {
        putInt(McpServerContract.KEY_CONTRACT_VERSION, McpServerContract.CONTRACT_VERSION)
        putString(McpServerContract.KEY_STATUS_STATE, status.state)
        putStringArray(McpServerContract.KEY_STATUS_ENDPOINTS, status.endpoints.toTypedArray())
        putInt(McpServerContract.KEY_STATUS_PORT, status.port)
        putString(McpServerContract.KEY_STATUS_BIND_SCOPE, status.bindScope)
        putString(McpServerContract.KEY_STATUS_PROTOCOL_MODE, status.protocolMode)
        putInt(McpServerContract.KEY_STATUS_PAIRED_CLIENT_COUNT, status.pairedClientCount)
        putInt(McpServerContract.KEY_STATUS_ACTIVE_SESSION_COUNT, status.activeSessionCount)
        putLong(McpServerContract.KEY_STATUS_STARTED_AT, status.startedAt)
        putBoolean(McpServerContract.KEY_STATUS_HOST_AVAILABLE, status.hostAvailable)
        status.lastErrorCode?.let { putString(McpServerContract.KEY_STATUS_LAST_ERROR_CODE, it) }
        status.lastError?.let { putString(McpServerContract.KEY_STATUS_LAST_ERROR, it.take(McpServerContract.MAX_ERROR_MESSAGE_BYTES)) }
    }

    fun event(type: String, text: String?, clientName: String?, toolName: String?, timestamp: Long): Bundle = Bundle().apply {
        putInt(McpServerContract.KEY_CONTRACT_VERSION, McpServerContract.CONTRACT_VERSION)
        putString(McpServerContract.KEY_EVENT_TYPE, type)
        text?.let { putString(McpServerContract.KEY_EVENT_TEXT, it.take(McpServerContract.MAX_EVENT_TEXT_BYTES)) }
        putLong(McpServerContract.KEY_EVENT_TIMESTAMP, timestamp)
        clientName?.let { putString(McpServerContract.KEY_EVENT_CLIENT_NAME, it.take(McpServerContract.MAX_CLIENT_NAME_BYTES)) }
        toolName?.let { putString(McpServerContract.KEY_EVENT_TOOL_NAME, it) }
    }

    /** The `REASON_*` / `ERROR_*` code of a stop or destroy Bundle, defaulting to `user_request`. */
    fun reasonCode(bundle: Bundle?): String =
        bundle?.getString(McpServerContract.KEY_ERROR_CODE)?.takeIf { it.isNotBlank() } ?: McpServerContract.REASON_USER_REQUEST

    fun reasonMessage(bundle: Bundle?): String? =
        bundle?.getString(McpServerContract.KEY_ERROR_MESSAGE)?.takeIf { it.isNotBlank() }?.take(McpServerContract.MAX_ERROR_MESSAGE_BYTES)
}
