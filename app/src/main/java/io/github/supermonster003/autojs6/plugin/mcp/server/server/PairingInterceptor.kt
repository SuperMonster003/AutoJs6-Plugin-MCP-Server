package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respondText
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The requests of one POST body: the JSON-RPC method and id of every request object, whether the
 * body was a batch, and whether any of them is gated. Notifications carry no id and are never
 * gated. Pure so the JVM tests cover it.
 */
data class JsonRpcCalls(val ids: List<JsonElement?>, val methods: List<String>, val batch: Boolean) {

    fun hasGated(gate: PairingGate): Boolean = methods.any(gate::isGated)

    companion object {

        /** Null when the body is not JSON-RPC at all; the SDK answers the parse error then. */
        fun parse(body: String): JsonRpcCalls? {
            val root = runCatching { Json.parseToJsonElement(body) }.getOrNull() ?: return null
            val elements = when (root) {
                is JsonArray -> root
                is JsonObject -> listOf(root)
                else -> return null
            }
            val requests = elements.filterIsInstance<JsonObject>().filter { it.containsKey("method") && it.containsKey("id") }
            return JsonRpcCalls(
                ids = requests.map { it["id"] },
                methods = requests.mapNotNull { runCatching { it["method"]?.jsonPrimitive?.content }.getOrNull() },
                batch = root is JsonArray,
            )
        }
    }
}

/**
 * First-use pairing in front of the SDK transport (roadmap P2.2), after the bearer check.
 *
 * The client identity comes from the SDK session the request belongs to (the `clientInfo` of
 * its `initialize`), or from the `User-Agent` header when there is no session yet, plus the
 * class of the remote address. A gated request from an unpaired client is answered with a
 * JSON-RPC error (`PAIRING_REQUIRED` or `PAIRING_DENIED`, `data.fingerprint` identifies the
 * confirmation) and never reaches the transport.
 */
fun Application.installPairingGate(server: Server, gate: PairingGate) {
    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.httpMethod != HttpMethod.Post) return@intercept
        val body = call.attributes.getOrNull(BUFFERED_BODY) ?: return@intercept
        val calls = call.attributes.getOrNull(JSON_RPC_CALLS) ?: JsonRpcCalls.parse(String(body, Charsets.UTF_8)) ?: return@intercept
        if (calls.ids.isEmpty() || !calls.hasGated(gate)) return@intercept
        val identity = resolveIdentity(server, call.request.header(SESSION_HEADER), call.request.header(HttpHeaders.UserAgent), call.request.origin.remoteHost)
        val method = calls.methods.first(gate::isGated)
        val document: JsonElement = when (val decision = gate.check(identity, method)) {
            is PairingDecision.Allowed -> return@intercept
            is PairingDecision.Required -> McpErrors.errors(
                calls.ids,
                calls.batch,
                McpErrors.PAIRING_REQUIRED,
                "${McpErrors.CODE_PAIRING_REQUIRED}: confirm \"${identity.name}\" on the phone within ${(decision.expiresAt - gate.now()).coerceAtLeast(0) / 1000} s, then retry",
                buildJsonObject {
                    put("code", McpErrors.CODE_PAIRING_REQUIRED)
                    put("fingerprint", decision.fingerprint)
                    put("client", identity.name)
                    put("addressClass", identity.addressClass.id)
                    put("expiresAt", decision.expiresAt)
                    put("firstRequest", decision.firstRequest)
                },
            )
            is PairingDecision.Denied -> McpErrors.errors(
                calls.ids,
                calls.batch,
                McpErrors.PAIRING_DENIED,
                "${McpErrors.CODE_PAIRING_DENIED}: pairing was ${decision.reason}; retry after ${(decision.retryAt - gate.now()).coerceAtLeast(0) / 1000} s",
                buildJsonObject {
                    put("code", McpErrors.CODE_PAIRING_DENIED)
                    put("fingerprint", decision.fingerprint)
                    put("client", identity.name)
                    put("reason", decision.reason)
                    put("retryAt", decision.retryAt)
                },
            )
        }
        call.respondText(document.toString(), ContentType.Application.Json, HttpStatusCode.OK)
        finish()
    }
}

private const val SESSION_HEADER = "Mcp-Session-Id"

/**
 * The `Mcp-Session-Id` of the HTTP transport is not the id the SDK keys its sessions by, so the
 * session is found through its transport; the client version is unset until `initialize` ran.
 */
internal fun resolveIdentity(server: Server, sessionId: String?, userAgent: String?, remoteHost: String?): ClientIdentity {
    val clientInfo = sessionId?.let { id ->
        runCatching {
            server.sessions.values.firstOrNull { (it.transport as? StreamableHttpServerTransport)?.sessionId == id }?.clientVersion
        }.getOrNull()
    }
    val name = PairingGate.normalizeName(clientInfo?.name ?: userAgent)
    return ClientIdentity(name, clientInfo?.version, AddressClass.ofRemoteHost(remoteHost))
}
