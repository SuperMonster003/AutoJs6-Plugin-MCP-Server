package io.github.supermonster003.autojs6.plugin.mcp.server.bridge

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Plugin-level error codes of a tool result (roadmap appendix A.5). */
object ToolErrorCodes {

    const val TOOL_DISABLED = "TOOL_DISABLED"
    const val HOST_UNAVAILABLE = "HOST_UNAVAILABLE"
    const val A11Y_SERVICE_NOT_RUNNING = "A11Y_SERVICE_NOT_RUNNING"
    const val NODE_REF_STALE = "NODE_REF_STALE"
    const val NODE_NOT_FOUND = "NODE_NOT_FOUND"
    const val CAPABILITY_DENIED = "CAPABILITY_DENIED"
    const val LIMIT_EXCEEDED = "LIMIT_EXCEEDED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val TIMEOUT = "TIMEOUT"
    const val HOST_ERROR = "HOST_ERROR"

    /** The arguments failed validation (plugin side) or the host rejected the request shape. */
    const val INVALID_ARGUMENTS = "INVALID_ARGUMENTS"
}

/**
 * A failed tool call as the MCP client sees it: `isError: true` with the text
 * `code: message (hint)` (appendix A.1) plus the same fields as structured content.
 */
data class ToolFailure(
    val code: String,
    val message: String,
    val hint: String? = null,
    val category: String? = null,
    val hostCode: String? = null,
    val module: String? = null,
    val method: String? = null,
) {

    fun render(): String = buildString {
        append(code).append(": ").append(message)
        hint?.let { append(" (").append(it).append(")") }
    }

    fun toJson(): JsonObject = buildJsonObject {
        put("code", code)
        put("message", message)
        hint?.let { put("hint", it) }
        category?.let { put("category", it) }
        hostCode?.let { put("hostCode", it) }
        module?.let { put("module", it) }
        method?.let { put("method", it) }
    }

    companion object {

        const val HINT_HOST = "start AutoJs6 and switch the MCP server on again; the listener keeps running"
        const val HINT_ACCESSIBILITY = "enable the AutoJs6 accessibility service on the phone"
        const val HINT_CAPABILITY = "the host grant or the AutoJs6 permission set does not cover this method"
        const val HINT_TIMEOUT = "raise timeoutMs or split the work into smaller calls"
        const val HINT_RATE = "retry after a short pause"
        const val HINT_LIMIT = "reduce the request or result size, or wait for running calls to finish"

        fun toolDisabled(toolName: String, groupId: String): ToolFailure = ToolFailure(
            ToolErrorCodes.TOOL_DISABLED,
            "tool $toolName is switched off by the $groupId group",
            "enable the group in the plugin settings on the phone",
        )

        fun hostUnavailable(detail: String? = null): ToolFailure = ToolFailure(
            ToolErrorCodes.HOST_UNAVAILABLE,
            detail?.takeIf { it.isNotBlank() } ?: "AutoJs6 is not connected to the MCP server",
            HINT_HOST,
            category = BridgeError.CATEGORY_PROCESS_DEAD,
        )

        fun invalidArguments(message: String): ToolFailure = ToolFailure(ToolErrorCodes.INVALID_ARGUMENTS, message)

        fun timeout(module: String?, method: String?, timeoutMs: Long): ToolFailure = ToolFailure(
            ToolErrorCodes.TIMEOUT,
            "no answer from AutoJs6 within $timeoutMs ms",
            HINT_TIMEOUT,
            category = BridgeError.CATEGORY_TIMEOUT,
            module = module,
            method = method,
        )

        fun limitExceeded(message: String): ToolFailure =
            ToolFailure(ToolErrorCodes.LIMIT_EXCEEDED, message, HINT_LIMIT, category = BridgeError.CATEGORY_RESOURCE_LIMIT)

        fun internal(message: String): ToolFailure = ToolFailure(ToolErrorCodes.HOST_ERROR, message)

        /** Maps the host's error category onto the plugin codes of appendix A.5. */
        fun fromBridge(error: BridgeError): ToolFailure {
            val base = ToolFailure(
                code = ToolErrorCodes.HOST_ERROR,
                message = if (error.name.isNotBlank() && error.name != "Error") "${error.name}: ${error.message}" else error.message,
                category = error.category,
                hostCode = error.code,
                module = error.module,
                method = error.method,
            )
            return when (error.category) {
                BridgeError.CATEGORY_PROCESS_DEAD -> base.copy(code = ToolErrorCodes.HOST_UNAVAILABLE, hint = HINT_HOST)
                BridgeError.CATEGORY_UNAVAILABLE -> if (error.module == "accessibility") {
                    base.copy(code = ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING, hint = HINT_ACCESSIBILITY)
                } else {
                    base.copy(code = ToolErrorCodes.HOST_UNAVAILABLE, hint = HINT_HOST)
                }
                BridgeError.CATEGORY_CAPABILITY_DENIED, BridgeError.CATEGORY_PERMISSION_DENIED ->
                    base.copy(code = ToolErrorCodes.CAPABILITY_DENIED, hint = HINT_CAPABILITY)
                BridgeError.CATEGORY_RESOURCE_LIMIT -> base.copy(code = ToolErrorCodes.LIMIT_EXCEEDED, hint = HINT_LIMIT)
                BridgeError.CATEGORY_RATE_LIMITED -> base.copy(code = ToolErrorCodes.RATE_LIMITED, hint = HINT_RATE)
                BridgeError.CATEGORY_TIMEOUT -> base.copy(code = ToolErrorCodes.TIMEOUT, hint = HINT_TIMEOUT)
                BridgeError.CATEGORY_INVALID_REQUEST -> base.copy(code = ToolErrorCodes.INVALID_ARGUMENTS)
                else -> base
            }
        }
    }
}
