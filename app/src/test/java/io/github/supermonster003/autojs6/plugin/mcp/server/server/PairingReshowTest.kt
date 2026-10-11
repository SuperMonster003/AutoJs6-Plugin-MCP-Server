package io.github.supermonster003.autojs6.plugin.mcp.server.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The settings page re-shows a pending pairing prompt the listener could not start (roadmap P9). */
class PairingReshowTest {

    private fun request(id: Long, name: String, expiresAt: Long) =
        PairingRequest(id, ClientIdentity(name, null, AddressClass.LOOPBACK), requestedAt = expiresAt - 60_000, expiresAt = expiresAt)

    @Test
    fun `the newest unexpired request is shown`() {
        val older = request(1, "claude-code", expiresAt = 50_000)
        val newer = request(2, "cursor", expiresAt = 70_000)
        val expired = request(3, "codex", expiresAt = 9_000)
        assertEquals(newer, PairingCoordinator.nextToReshow(listOf(older, expired, newer), now = 10_000, reshown = emptySet()))
    }

    @Test
    fun `a request is re-shown once and expired requests never`() {
        val first = request(1, "claude-code", expiresAt = 50_000)
        val second = request(2, "cursor", expiresAt = 70_000)
        assertEquals(first, PairingCoordinator.nextToReshow(listOf(first, second), now = 10_000, reshown = setOf(second.client.fingerprint)))
        assertNull(PairingCoordinator.nextToReshow(listOf(first, second), now = 10_000, reshown = setOf(first.client.fingerprint, second.client.fingerprint)))
        assertNull(PairingCoordinator.nextToReshow(listOf(first), now = 50_000, reshown = emptySet()))
        assertNull(PairingCoordinator.nextToReshow(emptyList(), now = 0, reshown = emptySet()))
    }
}
