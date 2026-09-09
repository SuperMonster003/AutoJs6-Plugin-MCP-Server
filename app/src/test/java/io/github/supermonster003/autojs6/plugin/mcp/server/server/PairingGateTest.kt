package io.github.supermonster003.autojs6.plugin.mcp.server.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingGateTest {

    private class MemoryRepository : PairedClientRepository {
        val clients = LinkedHashMap<String, PairedClient>()
        override fun all(): List<PairedClient> = clients.values.toList()
        override fun put(client: PairedClient) { clients[client.fingerprint] = client }
        override fun remove(fingerprint: String): Boolean = clients.remove(fingerprint) != null
    }

    private class RecordingListener : PairingGate.Listener {
        val requested = mutableListOf<PairingRequest>()
        val paired = mutableListOf<PairedClient>()
        val denied = mutableListOf<Pair<PairingRequest, String>>()
        override fun onPairingRequested(request: PairingRequest) { requested += request }
        override fun onPaired(client: PairedClient) { paired += client }
        override fun onDenied(request: PairingRequest, reason: String) { denied += request to reason }
    }

    private var now = 1_000_000L
    private val repository = MemoryRepository()
    private val listener = RecordingListener()
    private val gate = PairingGate(repository, clock = { now }, windowMs = 60_000, cooldownMs = 30_000, maxClients = 3, listener = listener)
    private val claude = ClientIdentity("Claude Code", "2.1", AddressClass.LOOPBACK)

    @Test
    fun unpairedClientsMayListButMustPairBeforeCalling() {
        assertEquals(PairingDecision.Allowed, gate.check(claude, "initialize"))
        assertEquals(PairingDecision.Allowed, gate.check(claude, "tools/list"))
        assertEquals(PairingDecision.Allowed, gate.check(claude, "resources/list"))
        assertEquals(PairingDecision.Allowed, gate.check(claude, "ping"))

        val first = gate.check(claude, "tools/call")
        assertEquals(PairingDecision.Required(claude.fingerprint, now + 60_000, firstRequest = true), first)
        assertEquals(1, listener.requested.size)
        assertEquals(claude, listener.requested.single().client)
        assertEquals(1, gate.pendingRequests().size)

        now += 5_000
        val second = gate.check(claude, "resources/read")
        assertEquals(PairingDecision.Required(claude.fingerprint, now - 5_000 + 60_000, firstRequest = false), second)
        assertEquals("no second request while one is pending", 1, listener.requested.size)
        assertFalse(gate.isPaired(claude.fingerprint))
    }

    @Test
    fun approvalWithinTheWindowPairsAndAllowsGatedCalls() {
        gate.check(claude, "tools/call")
        now += 30_000
        assertTrue(gate.approve(claude.fingerprint))
        assertTrue(gate.isPaired(claude.fingerprint))
        assertEquals(1, listener.paired.size)
        val paired = repository.clients.getValue(claude.fingerprint)
        assertEquals("Claude Code", paired.name)
        assertEquals("2.1", paired.version)
        assertEquals(AddressClass.LOOPBACK, paired.addressClass)
        assertEquals(now, paired.firstPairedAt)
        assertEquals(PairingDecision.Allowed, gate.check(claude, "tools/call"))
        assertEquals(PairingDecision.Allowed, gate.check(claude.copy(version = "2.2"), "prompts/get"))
        assertTrue(gate.pendingRequests().isEmpty())
        assertFalse("nothing pending anymore", gate.approve(claude.fingerprint))
    }

    @Test
    fun denialStartsACooldownAndANewRequestFollowsIt() {
        gate.check(claude, "tools/call")
        assertTrue(gate.deny(claude.fingerprint))
        assertEquals(listOf(PairingGate.REASON_DENIED), listener.denied.map { it.second })
        val denied = gate.check(claude, "tools/call")
        assertEquals(PairingDecision.Denied(claude.fingerprint, now + 30_000, PairingGate.REASON_DENIED), denied)
        assertEquals(PairingDecision.Allowed, gate.check(claude, "tools/list"))
        assertEquals("no new request during cooldown", 1, listener.requested.size)

        now += 30_000
        val again = gate.check(claude, "tools/call")
        assertTrue(again is PairingDecision.Required && again.firstRequest)
        assertEquals(2, listener.requested.size)
        assertNotEquals(listener.requested[0].id, listener.requested[1].id)
        assertFalse(gate.deny("unknown"))
    }

    @Test
    fun expiredRequestsCountAsTimeoutsAndCannotBeApproved() {
        gate.check(claude, "tools/call")
        now += 60_000
        assertFalse(gate.approve(claude.fingerprint))
        assertEquals(listOf(PairingGate.REASON_TIMEOUT), listener.denied.map { it.second })
        val decision = gate.check(claude, "tools/call")
        assertEquals(PairingDecision.Denied(claude.fingerprint, now + 30_000, PairingGate.REASON_TIMEOUT), decision)
        assertTrue(gate.pendingRequests().isEmpty())
    }

    @Test
    fun revocationForcesANewPairing() {
        gate.check(claude, "tools/call")
        gate.approve(claude.fingerprint)
        assertTrue(gate.revoke(claude.fingerprint))
        assertFalse(gate.revoke(claude.fingerprint))
        assertFalse(gate.isPaired(claude.fingerprint))
        val decision = gate.check(claude, "tools/call")
        assertTrue(decision is PairingDecision.Required && decision.firstRequest)
    }

    @Test
    fun clientLimitRefusesNewPairingsWithACooldown() {
        (1..3).forEach { index ->
            val client = ClientIdentity("client-$index", null, AddressClass.LAN)
            gate.check(client, "tools/call")
            assertTrue(gate.approve(client.fingerprint))
        }
        val decision = gate.check(claude, "tools/call")
        assertEquals(PairingDecision.Denied(claude.fingerprint, now + 30_000, PairingGate.REASON_LIMIT), decision)
        assertEquals(3, listener.requested.size)
    }

    @Test
    fun pairedClientsAreTouchedAtMostOncePerMinute() {
        gate.check(claude, "tools/call")
        gate.approve(claude.fingerprint)
        val pairedAt = now
        now += 30_000
        gate.check(claude, "tools/call")
        assertEquals(pairedAt, repository.clients.getValue(claude.fingerprint).lastSeenAt)
        now += 30_000
        gate.check(claude.copy(version = "3.0"), "tools/list")
        val touched = repository.clients.getValue(claude.fingerprint)
        assertEquals(now, touched.lastSeenAt)
        assertEquals("3.0", touched.version)
        assertEquals(pairedAt, touched.firstPairedAt)
    }

    @Test
    fun identityDependsOnNameAndAddressClassOnly() {
        assertEquals(claude.fingerprint, claude.copy(version = "9").fingerprint)
        assertEquals(claude.fingerprint, ClientIdentity("  Claude   Code ", null, AddressClass.LOOPBACK).fingerprint)
        assertNotEquals(claude.fingerprint, ClientIdentity("claude code", null, AddressClass.LOOPBACK).fingerprint)
        assertNotEquals(claude.fingerprint, claude.copy(addressClass = AddressClass.LAN).fingerprint)
        assertNotEquals(claude.fingerprint, claude.copy(name = "Cursor").fingerprint)
        assertEquals(24, claude.fingerprint.length)
        assertEquals(PairingGate.UNKNOWN_CLIENT_NAME, PairingGate.normalizeName(null))
        assertEquals(PairingGate.UNKNOWN_CLIENT_NAME, PairingGate.normalizeName("   "))
        assertEquals("Claude Code", PairingGate.normalizeName("  Claude \n Code  "))
        val long = PairingGate.normalizeName("x".repeat(300))
        assertEquals(PairingGate.MAX_CLIENT_NAME_BYTES, long.length)
        val multibyte = PairingGate.normalizeName("中".repeat(200))
        assertTrue(multibyte.toByteArray(Charsets.UTF_8).size <= PairingGate.MAX_CLIENT_NAME_BYTES)
    }

    @Test
    fun remoteHostsAreClassifiedAsLoopbackOrLan() {
        listOf("127.0.0.1", "::1", "[::1]", "localhost", "127.5.5.5", null, "").forEach {
            assertEquals(it, AddressClass.LOOPBACK, AddressClass.ofRemoteHost(it))
        }
        listOf("192.168.1.5", "10.0.0.2", "fe80::1", "phone.local").forEach {
            assertEquals(it, AddressClass.LAN, AddressClass.ofRemoteHost(it))
        }
        assertEquals(AddressClass.LAN, AddressClass.fromId("lan"))
        assertEquals(null, AddressClass.fromId("wan"))
    }

    @Test
    fun gatedMethodsAreTheOnesThatActOnTheDevice() {
        listOf("tools/call", "resources/read", "resources/subscribe", "prompts/get").forEach { assertTrue(it, gate.isGated(it)) }
        listOf("initialize", "ping", "tools/list", "resources/list", "resources/templates/list", "prompts/list", "notifications/initialized", "logging/setLevel")
            .forEach { assertFalse(it, gate.isGated(it)) }
    }
}
