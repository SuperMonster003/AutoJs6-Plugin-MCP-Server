package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostAvailability
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.HostBridgeClient
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** One plugin behind a family, as `app.pluginCapabilities` lists it. */
data class PluginFamilyPlugin(
    val packageName: String,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val enabled: Boolean = false,
    val authorized: Boolean = false,
    val compatible: Boolean = false,
    val requiresHostVersion: Long? = null,
)

/** The live state of one plugin family (`ocr`, `barcode`, `opencv`, `pinyin`, `pinyin4j`, `mediainfo`, `docs`). */
data class PluginFamilyStatus(
    val id: String,
    val status: String,
    val methods: List<String> = emptyList(),
    val plugins: List<PluginFamilyPlugin> = emptyList(),
    val detail: String? = null,
) {

    val available: Boolean
        get() = status == STATUS_AVAILABLE

    companion object {

        const val STATUS_AVAILABLE = "available"
        const val STATUS_NOT_INSTALLED = "not_installed"
        const val STATUS_DISABLED = "disabled"
        const val STATUS_INCOMPATIBLE = "incompatible"

        /** How a status reads in an error message. */
        fun describe(status: String?): String = when (status) {
            STATUS_AVAILABLE -> "available"
            STATUS_NOT_INSTALLED -> "not installed"
            STATUS_DISABLED -> "installed but disabled in AutoJs6"
            STATUS_INCOMPATIBLE -> "installed but not compatible with this AutoJs6 build"
            null -> "not reported by AutoJs6"
            else -> status
        }
    }
}

/**
 * The host's `app.pluginCapabilities` answer (AutoJs6 6.8.0, MCP roadmap P11 D27): the state of
 * every plugin family behind the bridge modules, read afresh by the host on each call.
 */
data class PluginFamilyReport(
    val hostVersionCode: Long? = null,
    val pluginCenterComponent: String? = null,
    val families: Map<String, PluginFamilyStatus> = emptyMap(),
) {

    fun isAvailable(family: String): Boolean = families[family]?.available == true

    /** `family=status` pairs for logs and the dump. */
    fun describe(): String = families.values.joinToString(",", "{", "}") { "${it.id}=${it.status}" }

    companion object {

        const val SCHEMA = "autojs6-bridge-app-plugin-capabilities-v1"

        /** Tolerant: unknown fields are ignored, a missing or foreign schema yields null. */
        fun parse(element: JsonElement?): PluginFamilyReport? {
            val root = element as? JsonObject ?: return null
            if (root["schema"]?.string() != SCHEMA) return null
            val families = LinkedHashMap<String, PluginFamilyStatus>()
            (root["families"] as? JsonArray)?.forEach { item ->
                val family = item as? JsonObject ?: return@forEach
                val id = family["id"]?.string() ?: return@forEach
                val status = family["status"]?.string() ?: return@forEach
                families[id] = PluginFamilyStatus(
                    id = id,
                    status = status,
                    methods = (family["methods"] as? JsonArray)?.mapNotNull { it.string() }.orEmpty(),
                    plugins = (family["plugins"] as? JsonArray)?.mapNotNull { plugin ->
                        val record = plugin as? JsonObject ?: return@mapNotNull null
                        PluginFamilyPlugin(
                            packageName = record["packageName"]?.string() ?: return@mapNotNull null,
                            versionName = record["versionName"]?.string(),
                            versionCode = (record["versionCode"] as? JsonPrimitive)?.longOrNull,
                            enabled = (record["enabled"] as? JsonPrimitive)?.booleanOrNull == true,
                            authorized = (record["authorized"] as? JsonPrimitive)?.booleanOrNull == true,
                            compatible = (record["compatible"] as? JsonPrimitive)?.booleanOrNull == true,
                            requiresHostVersion = (record["requiresHostVersion"] as? JsonPrimitive)?.longOrNull,
                        )
                    }.orEmpty(),
                    detail = family["detail"]?.string(),
                )
            }
            return PluginFamilyReport(
                hostVersionCode = (root["hostVersionCode"] as? JsonPrimitive)?.longOrNull,
                pluginCenterComponent = (root["pluginCenter"] as? JsonObject)?.get("component")?.string(),
                families = families,
            )
        }

        private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
}

/**
 * Whether the plugin-family tools may be listed right now (roadmap P11 D27 / D28). The grant of
 * the open host session is the ceiling ([granted], null without a session; an empty set means the
 * host did not say and the broker enforces the grant itself), the host's live [report] decides the
 * availability of each family. [stale] marks a report kept from an earlier probe because the host
 * did not answer the last one; [hostDetail] explains a missing report.
 */
data class PluginToolAvailability(
    val granted: Set<String>?,
    val report: PluginFamilyReport?,
    val stale: Boolean = false,
    val hostDetail: String? = null,
) {

    fun allows(spec: ToolSpec): Boolean = unavailableFailure(spec) == null

    /** Why [spec] is not listed, or null when it may run; always null for the built-in groups. */
    fun unavailableFailure(spec: ToolSpec): ToolFailure? {
        val family = spec.group.family ?: return null
        val granted = granted ?: return ToolFailure.hostUnavailable()
        if (granted.isNotEmpty()) {
            val missing = spec.hostMethods.filter { it.key !in granted }
            if (missing.isNotEmpty()) {
                return ToolFailure(
                    ToolErrorCodes.CAPABILITY_DENIED,
                    "the host grant does not include ${missing.joinToString { it.key }}",
                    ToolFailure.HINT_CAPABILITY,
                    module = missing.first().module,
                    method = missing.first().method,
                )
            }
        }
        val report = report ?: return ToolFailure.pluginUnavailable(spec.name, family, PluginFamilyStatus.describe(null), hostDetail)
        val status = report.families[family]
            ?: return ToolFailure.pluginUnavailable(spec.name, family, PluginFamilyStatus.describe(null), "AutoJs6 does not know the $family family")
        if (!status.available) return ToolFailure.pluginUnavailable(spec.name, family, PluginFamilyStatus.describe(status.status), status.detail)
        return null
    }

    /** One line for logs and the dump. */
    fun describe(): String = when {
        granted == null -> "session=none"
        report == null -> "session=open report=none${hostDetail?.let { " ($it)" }.orEmpty()}"
        else -> "session=open stale=$stale families=${report.describe()}"
    }

    companion object {

        /** No host session: every plugin-family tool is hidden. */
        val NONE = PluginToolAvailability(null, null)
    }
}

/**
 * Soft probe of the plugin families (roadmap P11 D27 / D28): asks the open session's host for
 * `app.pluginCapabilities`, keeps the answer for [ttlMs], and starts over when the session (its
 * [HostBridgeClient]) changes. [invalidate] drops the cache, for package changes and new sessions.
 * A host that cannot answer (dead, or too old to know the method) leaves the plugin-family tools
 * hidden; a host that stops answering keeps the last report, marked stale, so a short host
 * restart does not make the tools flap.
 */
class PluginFamilyProbe(
    private val bridge: () -> HostBridgeClient?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ttlMs: Long = TTL_MS,
    private val timeoutMs: Long = TIMEOUT_MS,
) {

    private val mutex = Mutex()

    @Volatile
    private var cached: PluginToolAvailability = PluginToolAvailability.NONE

    @Volatile
    private var cachedAt = Long.MIN_VALUE

    @Volatile
    private var cachedClient: HostBridgeClient? = null

    /** Probes made against the host, for tests and the dump. */
    @Volatile
    var probeCount: Int = 0
        private set

    /** The newest availability without probing; [PluginToolAvailability.NONE] before the first probe. */
    val last: PluginToolAvailability
        get() = cached

    /** Drops the cache; the next [current] asks the host again. */
    fun invalidate() {
        cachedAt = Long.MIN_VALUE
    }

    suspend fun current(): PluginToolAvailability = mutex.withLock {
        val client = bridge()
        if (client == null) {
            cachedClient = null
            cached = PluginToolAvailability.NONE
            cachedAt = clock()
            return cached
        }
        val sameClient = client === cachedClient
        val age = clock() - cachedAt
        if (sameClient && age in 0 until ttlMs) return cached
        val previous = cached.takeIf { sameClient }
        cachedClient = client
        cached = probe(client, previous)
        cachedAt = clock()
        cached
    }

    private suspend fun probe(client: HostBridgeClient, previous: PluginToolAvailability?): PluginToolAvailability {
        val granted = client.info.methods
        if (!client.isGranted(MODULE, METHOD)) {
            return PluginToolAvailability(granted, null, hostDetail = "this AutoJs6 build does not report plugin capabilities ($MODULE.$METHOD); update AutoJs6")
        }
        if (client.availability != HostAvailability.AVAILABLE) {
            return previous?.copy(stale = true) ?: PluginToolAvailability(granted, null, hostDetail = ToolFailure.hostUnavailable().message)
        }
        probeCount++
        return when (val outcome = client.call(MODULE, METHOD, NO_ARGS, timeoutMs, PERMISSIONS)) {
            is BridgeOutcome.Ok -> PluginFamilyReport.parse(outcome.result)?.let { PluginToolAvailability(granted, it) }
                ?: PluginToolAvailability(granted, null, hostDetail = "AutoJs6 answered $MODULE.$METHOD with an unknown shape")
            is BridgeOutcome.Failed -> when (outcome.failure.code) {
                ToolErrorCodes.HOST_UNAVAILABLE, ToolErrorCodes.TIMEOUT ->
                    previous?.copy(stale = true) ?: PluginToolAvailability(granted, null, hostDetail = outcome.failure.message)
                else -> PluginToolAvailability(granted, null, hostDetail = outcome.failure.render())
            }
        }
    }

    companion object {

        const val MODULE = "app"
        const val METHOD = "pluginCapabilities"

        /** How long one answer stands in for the host's live state. */
        const val TTL_MS = 5_000L

        /** The probe is a package manager query on the host; it must not hold up `tools/list` for long. */
        const val TIMEOUT_MS = 5_000L

        private val NO_ARGS = JsonArray(emptyList())
        private val PERMISSIONS = listOf("app.query")
    }
}
