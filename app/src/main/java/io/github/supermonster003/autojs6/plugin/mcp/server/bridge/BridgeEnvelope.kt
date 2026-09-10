package io.github.supermonster003.autojs6.plugin.mcp.server.bridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The plugin side of the host's Node Bridge envelope (roadmap D10, protocol document "Bridge
 * request"): `{id, module, method, args, timeoutMs, permissions}`. Pure Kotlin so the JVM tests
 * cover the wire shape without Android.
 */
data class BridgeRequest(
    val id: String,
    val module: String,
    val method: String,
    val args: JsonArray = JsonArray(emptyList()),
    val timeoutMs: Long,
    val permissions: List<String> = emptyList(),
) {

    /** `module.method`, the key the host grant is expressed in. */
    val key: String
        get() = "$module.$method"

    fun toJson(): String = buildJsonObject {
        put("id", id)
        put("module", module)
        put("method", method)
        put("args", args)
        put("timeoutMs", timeoutMs)
        put("permissions", JsonArray(permissions.map { JsonPrimitive(it) }))
    }.toString()
}

/** The `error` object of a failed bridge response, as the host's `NodeBridgeError` writes it. */
data class BridgeError(
    val name: String,
    val message: String,
    val code: String,
    val category: String,
    val module: String? = null,
    val method: String? = null,
    val missingCapabilities: List<String> = emptyList(),
) {

    companion object {

        /** Categories the host assigns (`NodeBridgeErrorCategory`); the plugin maps them in [ToolFailure]. */
        const val CATEGORY_UNAVAILABLE = "unavailable"
        const val CATEGORY_PERMISSION_DENIED = "permission-denied"
        const val CATEGORY_CAPABILITY_DENIED = "capability-denied"
        const val CATEGORY_PROVIDER_FAILED = "provider-failed"
        const val CATEGORY_INVALID_REQUEST = "invalid-request"
        const val CATEGORY_RESOURCE_LIMIT = "resource-limit"
        const val CATEGORY_RATE_LIMITED = "rate-limited"
        const val CATEGORY_TIMEOUT = "timeout"
        const val CATEGORY_PROCESS_DEAD = "process-dead"
        const val CATEGORY_RUNTIME_ERROR = "runtime-error"

        /** Codes of the errors the plugin synthesizes itself (the host uses its own vocabulary). */
        const val CODE_PLUGIN_PROCESS_DEAD = "plugin-host-process-dead"
        const val CODE_PLUGIN_TIMEOUT = "plugin-bridge-timeout"
        const val CODE_PLUGIN_MALFORMED = "plugin-bridge-malformed-response"

        fun processDead(message: String, module: String? = null, method: String? = null): BridgeError =
            BridgeError("Error", message, CODE_PLUGIN_PROCESS_DEAD, CATEGORY_PROCESS_DEAD, module, method)

        fun timeout(module: String?, method: String?, timeoutMs: Long): BridgeError =
            BridgeError("Error", "the host did not answer within $timeoutMs ms", CODE_PLUGIN_TIMEOUT, CATEGORY_TIMEOUT, module, method)

        fun malformed(message: String): BridgeError =
            BridgeError("Error", message, CODE_PLUGIN_MALFORMED, CATEGORY_RUNTIME_ERROR)

        /** Tolerant: every field falls back to a default, so a partial error object never hides the failure. */
        fun fromJson(element: JsonElement?, fallbackMessage: String? = null): BridgeError {
            val json = element as? JsonObject
            val message = json.string("message")?.takeIf { it.isNotBlank() } ?: fallbackMessage ?: "the host reported an error without a message"
            val code = json.string("code") ?: CODE_PLUGIN_MALFORMED
            return BridgeError(
                name = json.string("name") ?: "Error",
                message = message,
                code = code,
                category = json.string("category") ?: CATEGORY_RUNTIME_ERROR,
                module = json.string("module"),
                method = json.string("method"),
                missingCapabilities = (json?.get("missingCapabilities") as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty(),
            )
        }

        private fun JsonObject?.string(key: String): String? = (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}

/** One answer of the host: `{id, ok: true, result}` or `{id, ok: false, error}`. */
sealed class BridgeResponse {

    abstract val id: String?

    data class Success(override val id: String?, val result: JsonElement) : BridgeResponse()

    data class Failure(override val id: String?, val error: BridgeError) : BridgeResponse()

    companion object {

        /**
         * [ok] and [errorMessage] are the Bundle mirrors the broker sends next to the JSON; they win
         * over a JSON that cannot be parsed, so a broken document still yields a failure with a message.
         */
        fun parse(json: String?, ok: Boolean?, errorMessage: String?): BridgeResponse {
            val root = json?.let { text -> runCatching { Json.parseToJsonElement(text) }.getOrNull() } as? JsonObject
            if (root == null) {
                return Failure(null, BridgeError.malformed(errorMessage?.takeIf { it.isNotBlank() } ?: "the host sent no readable response"))
            }
            val id = root["id"]?.let { (it as? JsonPrimitive)?.contentOrNull }
            val succeeded = ok ?: (root["ok"] as? JsonPrimitive)?.booleanOrNull ?: false
            return if (succeeded) {
                Success(id, root["result"] ?: JsonNull)
            } else {
                Failure(id, BridgeError.fromJson(root["error"], errorMessage))
            }
        }
    }
}

/** Reads a JSON array of strings; anything else yields an empty list. */
internal fun JsonElement?.stringList(): List<String> =
    (this as? JsonArray)?.jsonArray?.mapNotNull { (it as? JsonPrimitive)?.jsonPrimitive?.contentOrNull }.orEmpty()
