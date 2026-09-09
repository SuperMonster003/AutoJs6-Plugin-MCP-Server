package io.github.supermonster003.autojs6.plugin.mcp.server.server

import java.security.MessageDigest

/** Where a client connects from; part of its identity because LAN and USB clients are trusted differently. */
enum class AddressClass(val id: String) {

    LOOPBACK("loopback"),
    LAN("lan");

    companion object {

        fun fromId(id: String?): AddressClass? = entries.firstOrNull { it.id == id }

        /** Classifies a remote host string (`127.0.0.1`, `::1`, `localhost`, or a LAN address). */
        fun ofRemoteHost(remoteHost: String?): AddressClass {
            val host = remoteHost?.trim()?.lowercase().orEmpty()
            val bracketed = if (host.contains(':') && !host.startsWith('[')) "[$host]" else host
            return if (host.isEmpty() || HostHeaders.isLoopbackName(bracketed) || HostHeaders.isLoopbackName(host)) {
                LOOPBACK
            } else {
                LAN
            }
        }
    }
}

/**
 * The client as the gate sees it. [fingerprint] is derived from the name and the address class
 * only: the bearer token has already been verified by then, and rotating it must not undo a
 * pairing (roadmap P2.2); the version is informational because clients update often.
 */
data class ClientIdentity(
    val name: String,
    val version: String?,
    val addressClass: AddressClass,
) {

    val fingerprint: String = PairingGate.fingerprintOf(name, addressClass)
}

/** A client the user confirmed on the phone. */
data class PairedClient(
    val fingerprint: String,
    val name: String,
    val version: String?,
    val addressClass: AddressClass,
    val firstPairedAt: Long,
    val lastSeenAt: Long,
)

/** A confirmation the phone is waiting for. */
data class PairingRequest(
    val id: Long,
    val client: ClientIdentity,
    val requestedAt: Long,
    val expiresAt: Long,
)

sealed class PairingDecision {

    /** Paired, or the method needs no pairing. */
    object Allowed : PairingDecision()

    /** Not paired yet: the phone shows a confirmation until [expiresAt]; [firstRequest] is true when this call created it. */
    data class Required(val fingerprint: String, val expiresAt: Long, val firstRequest: Boolean) : PairingDecision()

    /** Denied, expired, or over the client limit; the client may try again after [retryAt]. */
    data class Denied(val fingerprint: String, val retryAt: Long, val reason: String) : PairingDecision()
}

/** Persistence of paired clients; the Android store and the in-memory test double implement it. */
interface PairedClientRepository {

    fun all(): List<PairedClient>

    fun put(client: PairedClient)

    fun remove(fingerprint: String): Boolean
}

/**
 * The first-use pairing state machine (roadmap P2.2), pure Kotlin.
 *
 * An unpaired client may initialize and list tools, resources, and prompts. Its first gated call
 * (`tools/call`, `resources/read`, `resources/subscribe`, `prompts/get`) creates a pairing request
 * that the phone must confirm within [windowMs]; until then every gated call answers
 * [PairingDecision.Required]. A denial or a timeout puts the client into a [cooldownMs] cooldown
 * with [PairingDecision.Denied]; afterwards the next gated call may ask again. A confirmation
 * within the window stores the client in the repository, and a revocation removes it.
 */
class PairingGate(
    private val repository: PairedClientRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val windowMs: Long = PAIRING_WINDOW_MS,
    private val cooldownMs: Long = PAIRING_COOLDOWN_MS,
    private val maxClients: Int = MAX_PAIRED_CLIENTS,
    private val listener: Listener? = null,
) {

    interface Listener {

        fun onPairingRequested(request: PairingRequest) {}

        fun onPaired(client: PairedClient) {}

        fun onDenied(request: PairingRequest, reason: String) {}
    }

    private class Pending(val request: PairingRequest)

    private class Cooldown(val until: Long, val reason: String)

    private val pending = LinkedHashMap<String, Pending>()
    private val cooldowns = HashMap<String, Cooldown>()
    private var nextRequestId = 1L

    fun now(): Long = clock()

    fun isGated(method: String): Boolean = method in GATED_METHODS

    fun isPaired(fingerprint: String): Boolean = repository.all().any { it.fingerprint == fingerprint }

    @Synchronized
    fun pendingRequests(): List<PairingRequest> {
        expire(clock())
        return pending.values.map { it.request }
    }

    @Synchronized
    fun check(identity: ClientIdentity, method: String): PairingDecision {
        val now = clock()
        expire(now)
        val fingerprint = identity.fingerprint
        val paired = repository.all().firstOrNull { it.fingerprint == fingerprint }
        if (paired != null) {
            if (now - paired.lastSeenAt >= TOUCH_INTERVAL_MS) {
                repository.put(paired.copy(lastSeenAt = now, version = identity.version ?: paired.version))
            }
            return PairingDecision.Allowed
        }
        if (!isGated(method)) return PairingDecision.Allowed
        cooldowns[fingerprint]?.let { cooldown ->
            return PairingDecision.Denied(fingerprint, cooldown.until, cooldown.reason)
        }
        pending[fingerprint]?.let { entry ->
            return PairingDecision.Required(fingerprint, entry.request.expiresAt, firstRequest = false)
        }
        if (repository.all().size >= maxClients) {
            val until = now + cooldownMs
            cooldowns[fingerprint] = Cooldown(until, REASON_LIMIT)
            return PairingDecision.Denied(fingerprint, until, REASON_LIMIT)
        }
        val request = PairingRequest(nextRequestId++, identity, now, now + windowMs)
        pending[fingerprint] = Pending(request)
        listener?.onPairingRequested(request)
        return PairingDecision.Required(fingerprint, request.expiresAt, firstRequest = true)
    }

    /** Confirms the pending request of [fingerprint]; false when there is none or it already expired. */
    @Synchronized
    fun approve(fingerprint: String): Boolean {
        val now = clock()
        expire(now)
        val entry = pending.remove(fingerprint) ?: return false
        val client = entry.request.client
        val paired = PairedClient(fingerprint, client.name, client.version, client.addressClass, now, now)
        repository.put(paired)
        cooldowns.remove(fingerprint)
        listener?.onPaired(paired)
        return true
    }

    /** Refuses the pending request of [fingerprint] and starts its cooldown; false when there is none. */
    @Synchronized
    fun deny(fingerprint: String): Boolean {
        val now = clock()
        expire(now)
        val entry = pending.remove(fingerprint) ?: return false
        cooldowns[fingerprint] = Cooldown(now + cooldownMs, REASON_DENIED)
        listener?.onDenied(entry.request, REASON_DENIED)
        return true
    }

    /** Forgets a paired client and any pending request; the next gated call pairs again. */
    @Synchronized
    fun revoke(fingerprint: String): Boolean {
        pending.remove(fingerprint)
        cooldowns.remove(fingerprint)
        return repository.remove(fingerprint)
    }

    private fun expire(now: Long) {
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now >= entry.value.request.expiresAt) {
                iterator.remove()
                cooldowns[entry.key] = Cooldown(now + cooldownMs, REASON_TIMEOUT)
                listener?.onDenied(entry.value.request, REASON_TIMEOUT)
            }
        }
        cooldowns.entries.removeAll { now >= it.value.until }
    }

    companion object {

        /** Mirrors `McpServerContract` (roadmap A.7). */
        const val PAIRING_WINDOW_MS = 60_000L
        const val PAIRING_COOLDOWN_MS = 30_000L
        const val MAX_PAIRED_CLIENTS = 32
        const val MAX_CLIENT_NAME_BYTES = 256

        const val REASON_DENIED = "denied"
        const val REASON_TIMEOUT = "timeout"
        const val REASON_LIMIT = "limit"

        const val UNKNOWN_CLIENT_NAME = "unknown-client"

        private const val TOUCH_INTERVAL_MS = 60_000L

        val GATED_METHODS: Set<String> = setOf("tools/call", "resources/read", "resources/subscribe", "prompts/get")

        fun fingerprintOf(name: String, addressClass: AddressClass): String {
            val digest = MessageDigest.getInstance("SHA-256").digest("${normalizeName(name)}\n${addressClass.id}".toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.substring(0, 24)
        }

        /** Trims and caps a client name at [MAX_CLIENT_NAME_BYTES] UTF-8 bytes; blank names become [UNKNOWN_CLIENT_NAME]. */
        fun normalizeName(name: String?): String {
            val trimmed = name?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
            if (trimmed.isEmpty()) return UNKNOWN_CLIENT_NAME
            var result = trimmed
            while (result.toByteArray(Charsets.UTF_8).size > MAX_CLIENT_NAME_BYTES) {
                result = result.dropLast(1)
            }
            return result
        }
    }
}
