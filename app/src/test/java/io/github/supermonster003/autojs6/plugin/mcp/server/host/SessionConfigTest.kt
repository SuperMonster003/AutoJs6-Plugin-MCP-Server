package io.github.supermonster003.autojs6.plugin.mcp.server.host

import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import org.autojs.plugin.mcp.server.api.McpServerContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SessionConfigTest {

    @Test fun `settings extension uses saved port scope and developer mode on reconnect`() {
        val saved = io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig(10001, BindScope.LAN, true, listOf("phone.local"))
        val active = io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig(10002)
        assertEquals(saved, SessionConfig(useSavedSettings = true).listenerConfiguration(saved, active))
        assertEquals(saved.copy(port = 9637, bindScope = BindScope.LOOPBACK), SessionConfig().listenerConfiguration(saved))
        assertEquals(active.copy(port = 10003), SessionConfig(port = 10003).listenerConfiguration(saved, active))
    }

    private fun rejected(block: () -> SessionConfig): String = try {
        block()
        fail("expected the configuration to be rejected")
        ""
    } catch (e: IllegalArgumentException) {
        e.message.orEmpty()
    }

    @Test
    fun `missing values take the contract defaults`() {
        val config = SessionConfig.fromValues(null, null, null, null, null)
        assertEquals(McpServerContract.DEFAULT_PORT, config.port)
        assertEquals(BindScope.LOOPBACK, config.bindScope)
        assertNull(config.hostLabel)
    }

    @Test
    fun `explicit values are normalized`() {
        val config = SessionConfig.fromValues(1, 9700, " LAN ", "Stateful", "  AutoJs6  ")
        assertEquals(9700, config.port)
        assertEquals(BindScope.LAN, config.bindScope)
        assertEquals("AutoJs6", config.hostLabel)
        assertNull(SessionConfig.fromValues(1, null, null, null, "   ").hostLabel)
    }

    @Test
    fun `rejections start with the contract error code`() {
        assertTrue(rejected { SessionConfig.fromValues(null, 80, null, null, null) }.startsWith("invalid_config: port must be within 1024..65535"))
        assertTrue(rejected { SessionConfig.fromValues(null, null, "bluetooth", null, null) }.startsWith("invalid_config: unknown bind scope"))
        assertTrue(rejected { SessionConfig.fromValues(null, null, null, "dual", null) }.startsWith("invalid_config: protocol mode dual is not supported"))
        assertTrue(rejected { SessionConfig.fromValues(null, null, null, null, "x".repeat(65)) }.startsWith("invalid_config: host label exceeds 64"))
        assertTrue(rejected { SessionConfig.fromValues(2, null, null, null, null) }.startsWith("unsupported_contract: contract version 2"))
    }

    @Test
    fun `the status model defaults to a stopped listener without a host`() {
        val status = SessionStatus(state = McpServerContract.STATE_STOPPED)
        assertEquals(0, status.port)
        assertEquals(McpServerContract.BIND_SCOPE_LOOPBACK, status.bindScope)
        assertEquals(McpServerContract.PROTOCOL_MODE_STATEFUL, status.protocolMode)
        assertEquals(false, status.hostAvailable)
        assertTrue(status.endpoints.isEmpty())
    }
}
