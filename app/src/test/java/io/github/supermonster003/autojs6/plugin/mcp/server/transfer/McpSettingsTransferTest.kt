package io.github.supermonster003.autojs6.plugin.mcp.server.transfer

import io.github.supermonster003.autojs6.plugin.mcp.server.server.AddressClass
import io.github.supermonster003.autojs6.plugin.mcp.server.server.BearerTokens
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingGate
import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.PluginGroupSwitch
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolGroup
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissions
import io.github.supermonster003.autojs6.plugin.mcp.server.transfer.McpSettingsTransfer.FollowUp
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import org.autojs.plugin.common.api.AutoJs6PluginSettingsTransferContract as Contract

/** The settings document of the settings transfer (roadmap P13, host settings roadmap P18.2). */
class McpSettingsTransferTest {

    private class MemoryStorage(
        var config: ServerConfig? = null,
        var tools: ToolPermissions? = null,
        var token: String? = null,
        var clients: List<PairedClient> = emptyList(),
    ) : McpSettingsStorage {

        val writes = mutableListOf<String>()
        var failing: String? = null

        override fun serverConfig(): ServerConfig? = config
        override fun writeServerConfig(config: ServerConfig) = write(McpSettingsTransfer.STORE_SERVER) { this.config = config }
        override fun toolPermissions(): ToolPermissions? = tools
        override fun writeToolPermissions(permissions: ToolPermissions) = write(McpSettingsTransfer.STORE_TOOLS) { tools = permissions }
        override fun token(): String? = token
        override fun writeToken(token: String) = write(McpSettingsTransfer.STORE_TOKEN) { this.token = token }
        override fun pairedClients(): List<PairedClient> = clients
        override fun writePairedClients(clients: List<PairedClient>) = write(McpSettingsTransfer.STORE_PAIRING) { this.clients = clients }

        private fun write(store: String, apply: () -> Unit) {
            if (store == failing) throw IOException("cannot write $store")
            writes += store
            apply()
        }
    }

    private val token = BearerTokens.generate()

    private fun client(name: String, addressClass: AddressClass = AddressClass.LAN, lastSeenAt: Long = 1_700_000_100_000L) =
        PairedClient(PairingGate.fingerprintOf(name, addressClass), name, "1.0", addressClass, 1_700_000_000_000L, lastSeenAt)

    private fun stored() = MemoryStorage(
        config = ServerConfig(port = 9100, bindScope = BindScope.LAN, developerMode = true, extraAllowedHosts = listOf("pc.local", "build-box"), lanReminder = false, idleStopMinutes = 30),
        tools = ToolPermissions(
            overrides = mapOf(ToolGroup.SHELL to true, ToolGroup.SCREEN to false),
            allowShellRoot = true,
            pluginGroups = mapOf("paddle" to PluginGroupSwitch(enabled = true), "files.extra" to PluginGroupSwitch(enabled = false, writes = true)),
        ),
        token = token,
        clients = listOf(client("Claude Code"), client("Cursor", AddressClass.LOOPBACK).copy(version = null)),
    )

    /** `JSONObject.similar` is missing from the `org.json` of `android.jar` that the unit tests compile against. */
    private fun canonical(value: Any?): Any? = when (value) {
        is JSONObject -> value.keys().asSequence().associateWith { canonical(value.get(it)) }.toSortedMap()
        is JSONArray -> (0 until value.length()).map { canonical(value.get(it)) }
        else -> value
    }

    private fun entry(type: String, value: Any): JSONObject = JSONObject().put(Contract.VALUE_KEY_TYPE, type).put(Contract.VALUE_KEY_VALUE, value)

    private fun record(name: String, addressClass: String) = JSONObject()
        .put(McpSettingsTransfer.RECORD_NAME, name)
        .put(McpSettingsTransfer.RECORD_VERSION, "2.1")
        .put(McpSettingsTransfer.RECORD_ADDRESS_CLASS, addressClass)
        .put(McpSettingsTransfer.RECORD_FIRST_PAIRED_AT, 1_700_000_000_000L)
        .put(McpSettingsTransfer.RECORD_LAST_SEEN_AT, 1_700_000_500_000L)

    @Test
    fun `the document has the four stores and is sensitive while it holds the token or pairings`() {
        val exported = McpSettingsTransfer(stored()).exportSettings()

        assertEquals(McpSettingsTransfer.STORES.toSet(), exported.settings.keys().asSequence().toSet())
        // server 6, tools 14 groups + 2 flags + 3 plugin switches, token 1, pairing 2.
        assertEquals(6 + ToolGroup.entries.size + 2 + 3 + 1 + 2, exported.entries)
        assertTrue(exported.sensitive)

        val server = exported.settings.getJSONObject(McpSettingsTransfer.STORE_SERVER)
        assertEquals(ServerConfig.KEYS.toSet(), server.keys().asSequence().toSet())
        assertEquals(Contract.TYPE_INT, server.getJSONObject(ServerConfig.KEY_PORT).getString(Contract.VALUE_KEY_TYPE))
        assertEquals("pc.local,build-box", server.getJSONObject(ServerConfig.KEY_EXTRA_ALLOWED_HOSTS).getString(Contract.VALUE_KEY_VALUE))
        val tools = exported.settings.getJSONObject(McpSettingsTransfer.STORE_TOOLS)
        assertTrue(tools.getJSONObject("group.shell").getBoolean(Contract.VALUE_KEY_VALUE))
        assertTrue(tools.getJSONObject("group.files").getBoolean(Contract.VALUE_KEY_VALUE))
        assertTrue(tools.getJSONObject("plugin.files.extra.writes").getBoolean(Contract.VALUE_KEY_VALUE))
        assertFalse(tools.has("plugin.paddle.writes"))
        val cursor = exported.settings.getJSONObject(McpSettingsTransfer.STORE_PAIRING)
            .getJSONObject(PairingGate.fingerprintOf("Cursor", AddressClass.LOOPBACK))
        assertEquals(Contract.TYPE_JSON, cursor.getString(Contract.VALUE_KEY_TYPE))
        assertTrue(cursor.getJSONObject(Contract.VALUE_KEY_VALUE).isNull(McpSettingsTransfer.RECORD_VERSION))

        val withoutSecrets = stored().apply { token = null; clients = emptyList() }
        val plain = McpSettingsTransfer(withoutSecrets).exportSettings()
        assertFalse(plain.sensitive)
        assertEquals(setOf(McpSettingsTransfer.STORE_SERVER, McpSettingsTransfer.STORE_TOOLS), plain.settings.keys().asSequence().toSet())
    }

    @Test
    fun `an empty plugin exports an empty document and issues no token`() {
        val storage = MemoryStorage()

        val exported = McpSettingsTransfer(storage).exportSettings()

        assertEquals(0, exported.settings.length())
        assertEquals(0, exported.entries)
        assertFalse(exported.sensitive)
        assertNull(storage.token)
    }

    @Test
    fun `an exported document imports into a fresh plugin unchanged, once per store`() {
        val exported = McpSettingsTransfer(stored()).exportSettings()
        val target = MemoryStorage()

        val outcome = McpSettingsTransfer(target).importSettings(JSONObject(exported.settings.toString()))

        assertEquals(exported.entries, outcome.written)
        assertEquals(0, outcome.skipped)
        assertEquals(McpSettingsTransfer.STORES, target.writes)
        assertEquals(stored().config, target.config)
        assertEquals(token, target.token)
        assertEquals(stored().clients.toSet(), target.clients.toSet())
        assertEquals(canonical(exported.settings), canonical(McpSettingsTransfer(target).exportSettings().settings))

        val again = McpSettingsTransfer(target).importSettings(JSONObject(exported.settings.toString()))
        assertEquals(exported.entries, again.written)
        assertFalse("importing the same document again changes nothing", again.serverChanged)
        assertEquals(2, target.clients.size)
    }

    @Test
    fun `unknown stores and keys, other tags and values the settings page cannot save are skipped`() {
        val target = MemoryStorage(config = ServerConfig(port = 9100))
        val document = JSONObject()
            .put("future", JSONObject().put("a", entry(Contract.TYPE_BOOL, true)).put("b", entry(Contract.TYPE_INT, 1)))
            .put(McpSettingsTransfer.STORE_TOKEN, "not an object")
            .put(
                McpSettingsTransfer.STORE_SERVER, JSONObject()
                    .put(ServerConfig.KEY_PORT, entry(Contract.TYPE_INT, 80))
                    .put(ServerConfig.KEY_BIND_SCOPE, entry(Contract.TYPE_STRING, "wifi"))
                    .put(ServerConfig.KEY_DEVELOPER_MODE, entry(Contract.TYPE_INT, 1))
                    .put(ServerConfig.KEY_EXTRA_ALLOWED_HOSTS, entry(Contract.TYPE_STRING, "pc.local,Bad Host"))
                    .put(ServerConfig.KEY_IDLE_STOP_MINUTES, entry(Contract.TYPE_INT, 2000))
                    .put(ServerConfig.KEY_LAN_REMINDER, entry(Contract.TYPE_BOOL, false))
                    .put("future_key", entry(Contract.TYPE_BOOL, true)),
            )
            .put(
                McpSettingsTransfer.STORE_TOOLS, JSONObject()
                    .put("group.unknown", entry(Contract.TYPE_BOOL, true))
                    .put("group.shell", entry(Contract.TYPE_BOOL, true))
                    .put("plugin..enabled", entry(Contract.TYPE_BOOL, true))
                    .put("plugin.ocr.writes", entry(Contract.TYPE_STRING, "yes"))
                    .put(McpSettingsTransfer.KEY_ALLOW_SHELL_ROOT, "not an entry"),
            )
            .put(
                McpSettingsTransfer.STORE_PAIRING, JSONObject()
                    .put("0123456789abcdef01234567", entry(Contract.TYPE_JSON, record("Claude Code", "lan")))
                    .put(PairingGate.fingerprintOf("Cursor", AddressClass.LOOPBACK), entry(Contract.TYPE_JSON, record("Cursor", "wifi")))
                    .put(PairingGate.fingerprintOf("Codex", AddressClass.LAN), entry(Contract.TYPE_JSON, record("Codex", "lan"))),
            )

        val outcome = McpSettingsTransfer(target).importSettings(document)

        // future 2, a store that is not an object 1, server 6, tools 4, pairing 2.
        assertEquals(15, outcome.skipped)
        assertEquals(3, outcome.written)
        assertEquals(ServerConfig(port = 9100, lanReminder = false), target.config)
        assertEquals(mapOf(ToolGroup.SHELL to true), target.tools?.overrides)
        assertFalse(target.tools!!.allowShellRoot)
        assertEquals(listOf("Codex"), target.clients.map { it.name })
        assertNull(target.token)
        assertEquals(listOf(McpSettingsTransfer.STORE_SERVER, McpSettingsTransfer.STORE_TOOLS, McpSettingsTransfer.STORE_PAIRING), target.writes)
    }

    @Test
    fun `numbers without a fraction are accepted, numbers with one are not`() {
        val target = MemoryStorage()
        val document = JSONObject().put(
            McpSettingsTransfer.STORE_SERVER, JSONObject()
                .put(ServerConfig.KEY_PORT, entry(Contract.TYPE_INT, 9100.0))
                .put(ServerConfig.KEY_IDLE_STOP_MINUTES, entry(Contract.TYPE_INT, 15.5)),
        )

        val outcome = McpSettingsTransfer(target).importSettings(document)

        assertEquals(1, outcome.written)
        assertEquals(1, outcome.skipped)
        assertEquals(ServerConfig(port = 9100), target.config)
    }

    @Test
    fun `imported pairings join the list up to its capacity and replace the same client`() {
        val existing = (0 until PairingGate.MAX_PAIRED_CLIENTS - 1).map { client("Client $it") }
        val target = MemoryStorage(clients = existing)
        val document = JSONObject().put(
            McpSettingsTransfer.STORE_PAIRING, JSONObject()
                .put(existing[0].fingerprint, entry(Contract.TYPE_JSON, record("Client 0", "lan")))
                .put(PairingGate.fingerprintOf("Agent A", AddressClass.LAN), entry(Contract.TYPE_JSON, record("Agent A", "lan")))
                .put(PairingGate.fingerprintOf("Agent B", AddressClass.LAN), entry(Contract.TYPE_JSON, record("Agent B", "lan"))),
        )

        val outcome = McpSettingsTransfer(target).importSettings(document)

        assertEquals(2, outcome.written)
        assertEquals(1, outcome.skipped)
        assertEquals(PairingGate.MAX_PAIRED_CLIENTS, target.clients.size)
        assertEquals(1_700_000_500_000L, target.clients.single { it.name == "Client 0" }.lastSeenAt)
        assertEquals(1, target.clients.count { it.name.startsWith("Agent") })
    }

    @Test
    fun `a running listener restarts only when its settings change`() {
        fun outcomeOf(store: String, key: String, value: JSONObject): McpSettingsTransfer.Outcome =
            McpSettingsTransfer(MemoryStorage(config = ServerConfig(port = 9100), token = token))
                .importSettings(JSONObject().put(store, JSONObject().put(key, value)))

        val port = outcomeOf(McpSettingsTransfer.STORE_SERVER, ServerConfig.KEY_PORT, entry(Contract.TYPE_INT, 9200))
        assertEquals(FollowUp.RESTART, port.followUp(listening = true))
        assertEquals(FollowUp.APPLY, port.followUp(listening = false))

        val scope = outcomeOf(McpSettingsTransfer.STORE_SERVER, ServerConfig.KEY_BIND_SCOPE, entry(Contract.TYPE_STRING, "lan"))
        assertEquals(FollowUp.RESTART, scope.followUp(listening = true))

        val reminder = outcomeOf(McpSettingsTransfer.STORE_SERVER, ServerConfig.KEY_LAN_REMINDER, entry(Contract.TYPE_BOOL, false))
        assertTrue(reminder.serverChanged)
        assertEquals(FollowUp.APPLY, reminder.followUp(listening = true))

        val samePort = outcomeOf(McpSettingsTransfer.STORE_SERVER, ServerConfig.KEY_PORT, entry(Contract.TYPE_INT, 9100))
        assertEquals(FollowUp.REFRESH, samePort.followUp(listening = true))

        val newToken = outcomeOf(McpSettingsTransfer.STORE_TOKEN, McpSettingsTransfer.KEY_BEARER, entry(Contract.TYPE_STRING, BearerTokens.generate()))
        assertEquals(FollowUp.REFRESH, newToken.followUp(listening = true))

        val badToken = outcomeOf(McpSettingsTransfer.STORE_TOKEN, McpSettingsTransfer.KEY_BEARER, entry(Contract.TYPE_STRING, "short"))
        assertEquals(0, badToken.written)
        assertEquals(1, badToken.skipped)
        assertEquals(FollowUp.NOTHING, badToken.followUp(listening = true))
    }

    @Test
    fun `a failed write fails the import`() {
        val target = MemoryStorage().apply { failing = McpSettingsTransfer.STORE_TOOLS }
        val document = JSONObject().put(McpSettingsTransfer.STORE_TOOLS, JSONObject().put("group.shell", entry(Contract.TYPE_BOOL, true)))

        assertThrows(IOException::class.java) { McpSettingsTransfer(target).importSettings(document) }
    }
}
