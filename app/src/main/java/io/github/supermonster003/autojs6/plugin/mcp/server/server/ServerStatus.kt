package io.github.supermonster003.autojs6.plugin.mcp.server.server

/**
 * The listener state as [McpHttpServer] publishes it (roadmap P2.1). The state names mirror the
 * `STATE_*` constants of `McpServerContract`, so the session Binder (P2.3) forwards them as is.
 */
data class ServerStatus(
    val state: String,
    val endpointUrl: String? = null,
    val errorCode: String? = null,
    val message: String? = null,
    val since: Long = System.currentTimeMillis(),
) {

    val isRunning: Boolean
        get() = state == STATE_RUNNING

    val isFailed: Boolean
        get() = state == STATE_FAILED

    companion object {

        const val STATE_STOPPED = "stopped"
        const val STATE_STARTING = "starting"
        const val STATE_RUNNING = "running"
        const val STATE_STOPPING = "stopping"
        const val STATE_FAILED = "failed"

        fun stopped(): ServerStatus = ServerStatus(STATE_STOPPED)

        fun starting(): ServerStatus = ServerStatus(STATE_STARTING)

        fun running(endpointUrl: String): ServerStatus = ServerStatus(STATE_RUNNING, endpointUrl = endpointUrl)

        fun stopping(): ServerStatus = ServerStatus(STATE_STOPPING)

        fun failed(errorCode: String, message: String): ServerStatus =
            ServerStatus(STATE_FAILED, errorCode = errorCode, message = message)
    }
}
