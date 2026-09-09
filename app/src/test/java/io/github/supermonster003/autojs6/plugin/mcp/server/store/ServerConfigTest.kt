package io.github.supermonster003.autojs6.plugin.mcp.server.store

import io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConfigTest {

    @Test
    fun defaultsBindTheLoopbackInterfaceOnTheDefaultPort() {
        val config = ServerConfig()
        assertEquals(McpServerPlugin.DEFAULT_PORT, config.port)
        assertEquals(BindScope.LOOPBACK, config.bindScope)
        assertEquals("127.0.0.1", config.bindAddress)
        assertFalse(config.developerMode)
        assertTrue(config.extraAllowedHosts.isEmpty())
        assertTrue(config.problems().isEmpty())
    }

    @Test
    fun mapRoundTripKeepsEveryField() {
        val config = ServerConfig(
            port = 18080,
            bindScope = BindScope.LAN,
            developerMode = true,
            extraAllowedHosts = listOf("phone.local", "[fe80::1]"),
        )
        assertEquals("0.0.0.0", config.bindAddress)
        assertEquals(config, ServerConfig.fromMap(config.toMap()))
        assertEquals(
            mapOf(
                "port" to "18080",
                "bind_scope" to "lan",
                "developer_mode" to "true",
                "extra_allowed_hosts" to "phone.local,[fe80::1]",
            ),
            config.toMap(),
        )
    }

    @Test
    fun invalidPersistedValuesFallBackToDefaults() {
        val config = ServerConfig.fromMap(
            mapOf(
                ServerConfig.KEY_PORT to "80",
                ServerConfig.KEY_BIND_SCOPE to "wan",
                ServerConfig.KEY_DEVELOPER_MODE to "maybe",
                ServerConfig.KEY_EXTRA_ALLOWED_HOSTS to "bad host, ,GOOD.example:9637,good.example",
            ),
        )
        assertEquals(McpServerPlugin.DEFAULT_PORT, config.port)
        assertEquals(BindScope.LOOPBACK, config.bindScope)
        assertFalse(config.developerMode)
        assertEquals(listOf("good.example"), config.extraAllowedHosts)
        assertEquals(ServerConfig(), ServerConfig.fromMap(emptyMap()))
        assertEquals(ServerConfig(), ServerConfig.fromMap(ServerConfig.KEYS.associateWith { null }))
    }

    @Test
    fun extraHostListIsCappedWhenDecoding() {
        val hosts = (1..12).joinToString(",") { "host$it.example" }
        val config = ServerConfig.fromMap(mapOf(ServerConfig.KEY_EXTRA_ALLOWED_HOSTS to hosts))
        assertEquals(ServerConfig.MAX_EXTRA_ALLOWED_HOSTS, config.extraAllowedHosts.size)
        assertEquals("host1.example", config.extraAllowedHosts.first())
    }

    @Test
    fun problemsReportPortsOutsideTheRangeAndBadHostNames() {
        val port = ServerConfig(port = 70000).problems()
        assertEquals(1, port.size)
        assertTrue(port.single(), port.single().contains("port"))
        assertTrue(ServerConfig(port = 1023).problems().isNotEmpty())
        assertTrue(ServerConfig(port = 1024).problems().isEmpty())
        assertTrue(ServerConfig(port = 65535).problems().isEmpty())

        val hosts = ServerConfig(extraAllowedHosts = listOf("Phone.local", "bad host")).problems()
        assertEquals(2, hosts.size)
        assertTrue(hosts.all { it.contains("invalid host name") })

        val tooMany = ServerConfig(extraAllowedHosts = (1..9).map { "host$it.example" }).problems()
        assertTrue(tooMany.single(), tooMany.single().contains("at most"))
    }

    @Test
    fun overridesReplaceOnlyTheGivenFields() {
        val stored = ServerConfig(port = 18080, bindScope = BindScope.LAN, developerMode = true, extraAllowedHosts = listOf("phone.local"))
        assertEquals(stored, stored.withOverrides())
        assertEquals(stored.copy(port = 9637), stored.withOverrides(port = 9637))
        assertEquals(stored.copy(bindScope = BindScope.LOOPBACK, developerMode = false), stored.withOverrides(bindScope = BindScope.LOOPBACK, developerMode = false))
    }

    @Test
    fun bindScopeIdsAreStable() {
        assertEquals(BindScope.LOOPBACK, BindScope.fromId("loopback"))
        assertEquals(BindScope.LAN, BindScope.fromId("lan"))
        assertEquals(null, BindScope.fromId("LAN"))
        assertEquals(null, BindScope.fromId(null))
    }
}
