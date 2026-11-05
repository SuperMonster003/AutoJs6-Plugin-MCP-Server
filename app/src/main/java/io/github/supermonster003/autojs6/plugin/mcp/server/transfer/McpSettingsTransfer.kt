package io.github.supermonster003.autojs6.plugin.mcp.server.transfer

import io.github.supermonster003.autojs6.plugin.mcp.server.server.AddressClass
import io.github.supermonster003.autojs6.plugin.mcp.server.server.BearerTokens
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingGate
import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolGroup
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissions
import org.autojs.plugin.common.api.PluginSettingsTransferProvider.Exported
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import org.autojs.plugin.common.api.AutoJs6PluginSettingsTransferContract as Contract

/**
 * What [McpSettingsTransfer] reads and writes: the four private documents of the plugin. A read answers null (or an
 * empty list) when the document does not exist yet; a write that fails throws [IOException].
 */
internal interface McpSettingsStorage {

    fun serverConfig(): ServerConfig?

    @Throws(IOException::class)
    fun writeServerConfig(config: ServerConfig)

    fun toolPermissions(): ToolPermissions?

    @Throws(IOException::class)
    fun writeToolPermissions(permissions: ToolPermissions)

    fun token(): String?

    @Throws(IOException::class)
    fun writeToken(token: String)

    fun pairedClients(): List<PairedClient>

    @Throws(IOException::class)
    fun writePairedClients(clients: List<PairedClient>)
}

/**
 * The settings document of AutoJs6's settings transfer (contract v1, roadmap P13, host settings roadmap P18.2). Four
 * stores, each present only when the plugin has the document behind it:
 *
 * - `server`, the keys of `config.json` ([ServerConfig.KEYS]) as typed values: `port` int, `bind_scope` string
 *   (`loopback` or `lan`), `developer_mode` bool, `extra_allowed_hosts` string (host names joined by commas, empty for
 *   none), `lan_reminder` bool, `idle_stop_minutes` int.
 * - `tools`, the switches of `tool_groups.json`: `group.<id>` bool for every [ToolGroup], `allow_shell_root` and
 *   `allow_shared_storage_read` bool, and `plugin.<short name>.enabled` / `plugin.<short name>.writes` bool for the
 *   plugin tool groups the user switched.
 * - `token`, `bearer` string: the bearer token. An export never issues one.
 * - `pairing`, one json entry per paired client keyed by its fingerprint: `name`, `version` (string or null),
 *   `addressClass` (`loopback` or `lan`), `firstPairedAt` and `lastSeenAt` (epoch milliseconds).
 *
 * The bearer token and the paired clients are secrets, so a document that holds either is sensitive and AutoJs6 seals
 * it. Import is lenient: unknown stores and keys, other tags and values the settings page could not have saved are
 * skipped (an unknown store counts its entries, a store or an entry that is not an object counts 1); so is a pairing
 * whose fingerprint does not belong to its name and address class, and a new pairing beyond
 * [PairingGate.MAX_PAIRED_CLIENTS]. Keys the document leaves out keep their values, and imported pairings join the
 * list (the same fingerprint is replaced, none is removed), so the same document can be imported again. Each store is
 * written once; a failed write fails the import. Pure JVM.
 */
internal class McpSettingsTransfer(private val storage: McpSettingsStorage) {

    /** What the running server does after an import, see [Outcome.followUp]. */
    enum class FollowUp { NOTHING, REFRESH, APPLY, RESTART }

    /**
     * The result of an import: [serverChanged] when the saved server configuration changed, [listenerChanged] when it
     * changed in what the listener uses (port, bind scope, developer mode, extra host names; see
     * [ServerConfig.sameListener]).
     */
    data class Outcome(val written: Int, val skipped: Int, val serverChanged: Boolean, val listenerChanged: Boolean) {

        /**
         * [FollowUp.RESTART] when the listener of this process runs ([listening]) and its settings changed: the
         * provider answers `restartNeeded` and the process ends, and AutoJs6 binds again and reopens the session with
         * the new settings. Otherwise the running server takes the change like a save on the settings page: a changed
         * server configuration is applied ([FollowUp.APPLY]), other entries refresh the tool list ([FollowUp.REFRESH]).
         */
        fun followUp(listening: Boolean): FollowUp = when {
            written == 0 -> FollowUp.NOTHING
            listenerChanged && listening -> FollowUp.RESTART
            serverChanged -> FollowUp.APPLY
            else -> FollowUp.REFRESH
        }
    }

    fun exportSettings(): Exported {
        val settings = JSONObject()
        storage.serverConfig()?.let { config ->
            settings.put(
                STORE_SERVER, JSONObject()
                    .put(ServerConfig.KEY_PORT, typed(Contract.TYPE_INT, config.port))
                    .put(ServerConfig.KEY_BIND_SCOPE, typed(Contract.TYPE_STRING, config.bindScope.id))
                    .put(ServerConfig.KEY_DEVELOPER_MODE, typed(Contract.TYPE_BOOL, config.developerMode))
                    .put(ServerConfig.KEY_EXTRA_ALLOWED_HOSTS, typed(Contract.TYPE_STRING, config.extraAllowedHosts.joinToString(HOST_SEPARATOR)))
                    .put(ServerConfig.KEY_LAN_REMINDER, typed(Contract.TYPE_BOOL, config.lanReminder))
                    .put(ServerConfig.KEY_IDLE_STOP_MINUTES, typed(Contract.TYPE_INT, config.idleStopMinutes)),
            )
        }
        storage.toolPermissions()?.let { permissions ->
            val tools = JSONObject()
            ToolGroup.entries.forEach { group -> tools.put(GROUP_PREFIX + group.id, typed(Contract.TYPE_BOOL, permissions.isEnabled(group))) }
            tools.put(KEY_ALLOW_SHELL_ROOT, typed(Contract.TYPE_BOOL, permissions.allowShellRoot))
            tools.put(KEY_ALLOW_SHARED_STORAGE_READ, typed(Contract.TYPE_BOOL, permissions.allowSharedStorageRead))
            permissions.pluginGroups.toSortedMap().forEach { (shortName, switch) ->
                switch.enabled?.let { tools.put(PLUGIN_PREFIX + shortName + ENABLED_SUFFIX, typed(Contract.TYPE_BOOL, it)) }
                switch.writes?.let { tools.put(PLUGIN_PREFIX + shortName + WRITES_SUFFIX, typed(Contract.TYPE_BOOL, it)) }
            }
            settings.put(STORE_TOOLS, tools)
        }
        val token = storage.token()
        if (token != null) settings.put(STORE_TOKEN, JSONObject().put(KEY_BEARER, typed(Contract.TYPE_STRING, token)))
        val clients = storage.pairedClients()
        if (clients.isNotEmpty()) {
            val pairing = JSONObject()
            clients.forEach { client -> pairing.put(client.fingerprint, typed(Contract.TYPE_JSON, recordOf(client))) }
            settings.put(STORE_PAIRING, pairing)
        }
        val entries = settings.keys().asSequence().sumOf { settings.getJSONObject(it).length() }
        return Exported(settings, entries, sensitive = token != null || clients.isNotEmpty())
    }

    /** Writes the valid entries of [settings]; see the class for what counts as skipped. */
    @Throws(IOException::class)
    fun importSettings(settings: JSONObject): Outcome {
        val counts = Counts()
        settings.keys().forEach { storeName ->
            if (storeName !in STORES) counts.skipped += (settings.opt(storeName) as? JSONObject)?.length() ?: 1
        }
        var serverChanged = false
        var listenerChanged = false
        STORES.forEach { storeName ->
            val store = settings.opt(storeName) ?: return@forEach
            if (store !is JSONObject) {
                counts.skipped++
                return@forEach
            }
            when (storeName) {
                STORE_SERVER -> importServer(store, counts)?.let { (before, after) ->
                    serverChanged = after != before
                    listenerChanged = !before.sameListener(after)
                }
                STORE_TOOLS -> importTools(store, counts)
                STORE_TOKEN -> importToken(store, counts)
                STORE_PAIRING -> importPairing(store, counts)
            }
        }
        return Outcome(counts.written, counts.skipped, serverChanged, listenerChanged)
    }

    /** The saved configuration before and after, or null when nothing was written. */
    private fun importServer(store: JSONObject, counts: Counts): Pair<ServerConfig, ServerConfig>? {
        val before = storage.serverConfig() ?: ServerConfig()
        var config = before
        var accepted = 0
        store.keys().forEach { key ->
            val next = when (key) {
                ServerConfig.KEY_PORT -> wholeNumber(store.tagged(key, Contract.TYPE_INT))
                    ?.takeIf { it in ServerConfig.MIN_PORT..ServerConfig.MAX_PORT }?.let { config.copy(port = it.toInt()) }
                ServerConfig.KEY_BIND_SCOPE -> BindScope.fromId(store.tagged(key, Contract.TYPE_STRING) as? String)?.let { config.copy(bindScope = it) }
                ServerConfig.KEY_DEVELOPER_MODE -> (store.tagged(key, Contract.TYPE_BOOL) as? Boolean)?.let { config.copy(developerMode = it) }
                ServerConfig.KEY_EXTRA_ALLOWED_HOSTS -> (store.tagged(key, Contract.TYPE_STRING) as? String)?.let(::hostsOf)?.let { config.copy(extraAllowedHosts = it) }
                ServerConfig.KEY_LAN_REMINDER -> (store.tagged(key, Contract.TYPE_BOOL) as? Boolean)?.let { config.copy(lanReminder = it) }
                ServerConfig.KEY_IDLE_STOP_MINUTES -> wholeNumber(store.tagged(key, Contract.TYPE_INT))
                    ?.takeIf { it in 0..ServerConfig.MAX_IDLE_STOP_MINUTES }?.let { config.copy(idleStopMinutes = it.toInt()) }
                else -> null
            }
            if (next == null) counts.skipped++ else {
                config = next
                accepted++
            }
        }
        if (accepted == 0) return null
        storage.writeServerConfig(config)
        counts.written += accepted
        return before to config
    }

    private fun importTools(store: JSONObject, counts: Counts) {
        var permissions = storage.toolPermissions() ?: ToolPermissions.DEFAULT
        var accepted = 0
        store.keys().forEach { key ->
            val on = store.tagged(key, Contract.TYPE_BOOL) as? Boolean
            val next = when {
                on == null -> null
                key == KEY_ALLOW_SHELL_ROOT -> permissions.copy(allowShellRoot = on)
                key == KEY_ALLOW_SHARED_STORAGE_READ -> permissions.copy(allowSharedStorageRead = on)
                key.startsWith(GROUP_PREFIX) -> ToolGroup.fromId(key.removePrefix(GROUP_PREFIX))?.let { permissions.with(it, on) }
                key.endsWith(ENABLED_SUFFIX) -> pluginNameOf(key, ENABLED_SUFFIX)?.let { permissions.withPluginGroup(it, on) }
                key.endsWith(WRITES_SUFFIX) -> pluginNameOf(key, WRITES_SUFFIX)?.let { permissions.withPluginWrites(it, on) }
                else -> null
            }
            if (next == null) counts.skipped++ else {
                permissions = next
                accepted++
            }
        }
        if (accepted == 0) return
        storage.writeToolPermissions(permissions)
        counts.written += accepted
    }

    private fun importToken(store: JSONObject, counts: Counts) {
        var token: String? = null
        store.keys().forEach { key ->
            val value = if (key == KEY_BEARER) store.tagged(key, Contract.TYPE_STRING) as? String else null
            if (BearerTokens.isWellFormed(value)) token = value else counts.skipped++
        }
        token?.let {
            storage.writeToken(it)
            counts.written++
        }
    }

    private fun importPairing(store: JSONObject, counts: Counts) {
        val clients = LinkedHashMap<String, PairedClient>()
        storage.pairedClients().forEach { clients[it.fingerprint] = it }
        var accepted = 0
        store.keys().asSequence().sorted().forEach { fingerprint ->
            val client = (store.tagged(fingerprint, Contract.TYPE_JSON) as? JSONObject)?.let { clientOf(fingerprint, it) }
            if (client == null || (client.fingerprint !in clients && clients.size >= PairingGate.MAX_PAIRED_CLIENTS)) {
                counts.skipped++
            } else {
                clients[client.fingerprint] = client
                accepted++
            }
        }
        if (accepted == 0) return
        storage.writePairedClients(clients.values.toList())
        counts.written += accepted
    }

    private class Counts {
        var written = 0
        var skipped = 0
    }

    companion object {

        const val STORE_SERVER = "server"
        const val STORE_TOOLS = "tools"
        const val STORE_TOKEN = "token"
        const val STORE_PAIRING = "pairing"

        /** The stores in the order they are written. */
        val STORES: List<String> = listOf(STORE_SERVER, STORE_TOOLS, STORE_TOKEN, STORE_PAIRING)

        const val GROUP_PREFIX = "group."
        const val PLUGIN_PREFIX = "plugin."
        const val ENABLED_SUFFIX = ".enabled"
        const val WRITES_SUFFIX = ".writes"
        const val KEY_ALLOW_SHELL_ROOT = "allow_shell_root"
        const val KEY_ALLOW_SHARED_STORAGE_READ = "allow_shared_storage_read"
        const val KEY_BEARER = "bearer"

        const val RECORD_NAME = "name"
        const val RECORD_VERSION = "version"
        const val RECORD_ADDRESS_CLASS = "addressClass"
        const val RECORD_FIRST_PAIRED_AT = "firstPairedAt"
        const val RECORD_LAST_SEEN_AT = "lastSeenAt"

        private const val HOST_SEPARATOR = ","

        private fun typed(tag: String, value: Any): JSONObject = JSONObject().put(Contract.VALUE_KEY_TYPE, tag).put(Contract.VALUE_KEY_VALUE, value)

        /** The value of the entry [name] when it is an object of the [tag]; null otherwise. */
        private fun JSONObject.tagged(name: String, tag: String): Any? {
            val entry = opt(name) as? JSONObject ?: return null
            if (entry.opt(Contract.VALUE_KEY_TYPE) != tag) return null
            return entry.opt(Contract.VALUE_KEY_VALUE)?.takeUnless { it == JSONObject.NULL }
        }

        /** A JSON number without a fraction (`2000.0` counts, `15.5` does not). */
        private fun wholeNumber(value: Any?): Long? = when (value) {
            is Int -> value.toLong()
            is Long -> value
            is Number -> runCatching { BigDecimal(value.toString()).longValueExact() }.getOrNull()
            else -> null
        }

        /** Host names as the settings page saves them: normalized, distinct, at most [ServerConfig.MAX_EXTRA_ALLOWED_HOSTS]. */
        private fun hostsOf(text: String): List<String>? {
            if (text.isEmpty()) return emptyList()
            val hosts = text.split(HOST_SEPARATOR)
            if (hosts.size > ServerConfig.MAX_EXTRA_ALLOWED_HOSTS || hosts.toSet().size != hosts.size) return null
            return hosts.takeIf { list -> list.all { ServerConfig.normalizeHost(it) == it } }
        }

        private fun pluginNameOf(key: String, suffix: String): String? {
            if (!key.startsWith(PLUGIN_PREFIX) || key.length <= PLUGIN_PREFIX.length + suffix.length) return null
            return key.substring(PLUGIN_PREFIX.length, key.length - suffix.length).takeIf { it.isNotBlank() && it.trim() == it }
        }

        private fun recordOf(client: PairedClient): JSONObject = JSONObject()
            .put(RECORD_NAME, client.name)
            .put(RECORD_VERSION, client.version ?: JSONObject.NULL)
            .put(RECORD_ADDRESS_CLASS, client.addressClass.id)
            .put(RECORD_FIRST_PAIRED_AT, client.firstPairedAt)
            .put(RECORD_LAST_SEEN_AT, client.lastSeenAt)

        private fun clientOf(fingerprint: String, record: JSONObject): PairedClient? {
            val name = record.opt(RECORD_NAME) as? String ?: return null
            val version = when (val value = record.opt(RECORD_VERSION)) {
                null, JSONObject.NULL -> null
                is String -> value
                else -> return null
            }
            val addressClass = AddressClass.fromId(record.opt(RECORD_ADDRESS_CLASS) as? String) ?: return null
            val firstPairedAt = wholeNumber(record.opt(RECORD_FIRST_PAIRED_AT))?.takeIf { it >= 0 } ?: return null
            val lastSeenAt = when (val value = record.opt(RECORD_LAST_SEEN_AT)) {
                null, JSONObject.NULL -> firstPairedAt
                else -> wholeNumber(value)?.takeIf { it >= firstPairedAt } ?: return null
            }
            if (PairingGate.fingerprintOf(name, addressClass) != fingerprint) return null
            return PairedClient(fingerprint, PairingGate.normalizeName(name), version, addressClass, firstPairedAt, lastSeenAt)
        }
    }
}
