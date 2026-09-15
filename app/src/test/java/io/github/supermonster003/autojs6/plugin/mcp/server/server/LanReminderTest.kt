package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanReminderTest {

    @Test
    fun remindsOnlyForARunningLanListenerWithTheSwitchOn() {
        val lan = ServerConfig(bindScope = BindScope.LAN)
        assertTrue(LanReminder.wanted(running = true, config = lan))
        assertFalse(LanReminder.wanted(running = false, config = lan))
        assertFalse(LanReminder.wanted(running = true, config = lan.copy(lanReminder = false)))
        assertFalse(LanReminder.wanted(running = true, config = ServerConfig()))
        assertFalse(LanReminder.wanted(running = true, config = null))
    }

    @Test
    fun theIntervalIsOneDay() {
        assertEquals(24L * 60L * 60L * 1000L, LanReminder.INTERVAL_MS)
    }

    @Test
    fun namesTheLanEndpointAheadOfLoopback() {
        assertEquals("http://192.168.1.20:9637/mcp", LanReminder.endpoint(listOf("http://127.0.0.1:9637/mcp", "http://192.168.1.20:9637/mcp")))
        assertEquals("http://127.0.0.1:9637/mcp", LanReminder.endpoint(listOf("http://127.0.0.1:9637/mcp")))
        assertEquals("", LanReminder.endpoint(emptyList()))
    }
}
