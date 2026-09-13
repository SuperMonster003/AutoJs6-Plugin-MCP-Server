package io.github.supermonster003.autojs6.plugin.mcp.server.store

import org.junit.Assert.*
import org.junit.Test

class ServerLifecycleStoreTest {
    @Test fun `fresh or corrupt storage never asks the host to auto start`() {
        listOf(null, "", "{}", "[]", "invalid", "{\"userStopped\":null}", "{\"userStopped\":\"false\"}").forEach {
            assertTrue(ServerLifecycleStore.decode(it))
        }
    }
    @Test fun `only a persisted successful start clears user stopped`() {
        assertFalse(ServerLifecycleStore.decode("{\"userStopped\":false}"))
        assertTrue(ServerLifecycleStore.decode("{\"userStopped\":true}"))
    }
}
