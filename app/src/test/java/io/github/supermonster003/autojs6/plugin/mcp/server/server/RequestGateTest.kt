package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestGateTest {

    private val loopback = GatePolicy.loopback()

    private fun request(
        method: String = "POST",
        host: String? = "127.0.0.1:9637",
        origin: String? = null,
        contentLength: Long? = 120L,
    ) = GateRequest(method, host, origin, contentLength)

    private fun reject(decision: GateDecision): GateDecision.Reject {
        assertTrue("expected a rejection, got $decision", decision is GateDecision.Reject)
        return decision as GateDecision.Reject
    }

    @Test
    fun loopbackPolicyAcceptsLoopbackNamesOnly() {
        listOf("127.0.0.1:9637", "localhost", "LocalHost:9637", "[::1]:9637").forEach { host ->
            assertEquals(host, GateDecision.Allow(), RequestGate.evaluate(request(host = host), loopback))
        }
        val foreign = reject(RequestGate.evaluate(request(host = "192.168.1.20:9637"), loopback))
        assertEquals(RequestGate.STATUS_FORBIDDEN, foreign.status)
        assertEquals(RequestGate.JSON_RPC_INVALID_REQUEST, foreign.code)
        assertTrue(foreign.message, foreign.message.contains("Host not allowed"))
        val missing = reject(RequestGate.evaluate(request(host = null), loopback))
        assertEquals("Invalid Host header", missing.message)
        assertEquals("Invalid Host header", reject(RequestGate.evaluate(request(host = "evil example"), loopback)).message)
        assertEquals("Invalid Host header", reject(RequestGate.evaluate(request(host = "user@127.0.0.1"), loopback)).message)
    }

    @Test
    fun lanPolicyAddsCurrentAddressesAndExtraNamesAndFollowsRefreshes() {
        val policy = GatePolicy.lan(listOf("192.168.1.20", "10.0.0.5"), extraHosts = listOf("Phone.local"))
        listOf("192.168.1.20:9637", "10.0.0.5", "phone.local:9637", "127.0.0.1:9637", "localhost").forEach { host ->
            assertEquals(host, GateDecision.Allow(), RequestGate.evaluate(request(host = host), policy))
        }
        reject(RequestGate.evaluate(request(host = "192.168.1.21:9637"), policy))

        val refreshed = GatePolicy.lan(listOf("192.168.1.21"), extraHosts = listOf("phone.local"))
        assertEquals(GateDecision.Allow(), RequestGate.evaluate(request(host = "192.168.1.21:9637"), refreshed))
        reject(RequestGate.evaluate(request(host = "192.168.1.20:9637"), refreshed))
    }

    @Test
    fun policyForConfigFollowsTheBindScope() {
        val config = ServerConfig(extraAllowedHosts = listOf("phone.local"), developerMode = true)
        val loopbackPolicy = GatePolicy.forConfig(config, lanAddresses = listOf("192.168.1.20"))
        assertEquals(GatePolicy.LOOPBACK_HOSTS + "phone.local", loopbackPolicy.allowedHosts)
        assertTrue(loopbackPolicy.developerMode)
        val lanPolicy = GatePolicy.forConfig(config.copy(bindScope = BindScope.LAN), lanAddresses = listOf("192.168.1.20"))
        assertEquals(GatePolicy.LOOPBACK_HOSTS + setOf("192.168.1.20", "phone.local"), lanPolicy.allowedHosts)
        assertEquals(RequestGate.MAX_REQUEST_BODY_BYTES, lanPolicy.maxBodyBytes)
        assertTrue(GatePolicy.lan(listOf("not a host")).allowedHosts == GatePolicy.LOOPBACK_HOSTS)
    }

    @Test
    fun loopbackOriginsAreAcceptedEverywhereAndCorsNeedsDeveloperMode() {
        val origin = "http://localhost:6274"
        // The conformance suite's localhost-host-valid-accepted check: a loopback Origin passes, without CORS headers.
        assertEquals(GateDecision.Allow(corsOrigin = null), RequestGate.evaluate(request(origin = origin), loopback))
        assertEquals("Origin not allowed", reject(RequestGate.evaluate(request(origin = "http://evil.example"), loopback)).message)
        val developer = GatePolicy.loopback(developerMode = true)
        assertEquals(GateDecision.Allow(corsOrigin = origin), RequestGate.evaluate(request(origin = origin), developer))
        assertEquals(GateDecision.Allow(corsOrigin = "http://127.0.0.1:6274"), RequestGate.evaluate(request(origin = " http://127.0.0.1:6274 "), developer))
        listOf("http://evil.example", "null", "http://localhost.evil:6274", "file://localhost").forEach { bad ->
            assertEquals(bad, "Origin not allowed", reject(RequestGate.evaluate(request(origin = bad), developer)).message)
        }
    }

    @Test
    fun preflightIsAnsweredOnlyForAcceptedOrigins() {
        val origin = "http://localhost:6274"
        val developer = GatePolicy.loopback(developerMode = true)
        assertEquals(GateDecision.Preflight(origin), RequestGate.evaluate(request(method = "OPTIONS", origin = origin, contentLength = null), developer))
        assertEquals(GateDecision.Preflight(origin), RequestGate.evaluate(request(method = "options", origin = origin, contentLength = null), developer))
        assertEquals(GateDecision.Allow(), RequestGate.evaluate(request(method = "OPTIONS", contentLength = null), developer))
        assertEquals(
            "Cross-origin access needs developer mode",
            reject(RequestGate.evaluate(request(method = "OPTIONS", origin = origin, contentLength = null), loopback)).message,
        )
    }

    @Test
    fun declaredBodiesAboveTheCeilingAreRefusedBeforeReading() {
        assertEquals(GateDecision.Allow(), RequestGate.evaluate(request(contentLength = RequestGate.MAX_REQUEST_BODY_BYTES), loopback))
        assertEquals(GateDecision.Allow(), RequestGate.evaluate(request(contentLength = null), loopback))
        val tooLarge = reject(RequestGate.evaluate(request(contentLength = RequestGate.MAX_REQUEST_BODY_BYTES + 1), loopback))
        assertEquals(RequestGate.STATUS_PAYLOAD_TOO_LARGE, tooLarge.status)
        assertTrue(tooLarge.message, tooLarge.message.contains(RequestGate.MAX_REQUEST_BODY_BYTES.toString()))
        val small = GatePolicy.loopback().copy(maxBodyBytes = 10)
        assertEquals(GateDecision.Allow(), RequestGate.evaluate(request(contentLength = 10), small))
        assertEquals(RequestGate.STATUS_PAYLOAD_TOO_LARGE, reject(RequestGate.evaluate(request(contentLength = 11), small)).status)
    }

    @Test
    fun hostIsCheckedBeforeOriginAndBodySize() {
        val decision = reject(RequestGate.evaluate(request(host = "evil.example", origin = "http://evil.example", contentLength = Long.MAX_VALUE), loopback))
        assertTrue(decision.message, decision.message.startsWith("Host not allowed"))
    }
}
