package io.github.supermonster003.autojs6.plugin.mcp.server.server

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * JSON-RPC error codes the plugin adds in front of the SDK (roadmap A.5), all inside the
 * server-defined range, plus the builders of the error documents the interceptors send.
 */
object McpErrors {

    /** JSON-RPC `Invalid Request`, the code the SDK also uses for rejected hosts. */
    const val INVALID_REQUEST = -32600

    const val UNAUTHORIZED = -32001
    const val PAIRING_REQUIRED = -32002
    const val PAIRING_DENIED = -32003

    const val CODE_UNAUTHORIZED = "UNAUTHORIZED"
    const val CODE_PAIRING_REQUIRED = "PAIRING_REQUIRED"
    const val CODE_PAIRING_DENIED = "PAIRING_DENIED"

    fun error(id: JsonElement?, code: Int, message: String, data: JsonObject? = null): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0")
        putJsonObject("error") {
            put("code", code)
            put("message", message)
            if (data != null) put("data", data)
        }
        put("id", id ?: JsonNull)
    }

    /** One error document per request id, as an object for a single request or an array for a batch. */
    fun errors(ids: List<JsonElement?>, batch: Boolean, code: Int, message: String, data: JsonObject? = null): JsonElement {
        val documents = ids.map { error(it, code, message, data) }
        return if (batch) JsonArray(documents) else documents.single()
    }
}
