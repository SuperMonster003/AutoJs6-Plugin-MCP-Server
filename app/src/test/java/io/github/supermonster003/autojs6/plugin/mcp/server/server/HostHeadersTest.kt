package io.github.supermonster003.autojs6.plugin.mcp.server.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostHeadersTest {

    @Test
    fun hostNamesLoseTheirPortAndCase() {
        assertEquals("127.0.0.1", HostHeaders.hostnameOf("127.0.0.1:9637"))
        assertEquals("localhost", HostHeaders.hostnameOf("LOCALHOST"))
        assertEquals("localhost", HostHeaders.hostnameOf(" localhost:1 "))
        assertEquals("[::1]", HostHeaders.hostnameOf("[::1]:9637"))
        assertEquals("[::1]", HostHeaders.hostnameOf("[::1]"))
        assertEquals("[fe80::1%wlan0]", HostHeaders.hostnameOf("[FE80::1%wlan0]:9637"))
        assertEquals("phone.local", HostHeaders.hostnameOf("phone.local"))
        assertEquals("192.168.1.20", HostHeaders.hostnameOf("192.168.1.20:65535"))
    }

    @Test
    fun malformedHostHeadersAreRejected() {
        listOf(
            null, "", "   ", "host:0", "host:65536", "host:99999", "host:abc", "host:", "[::1", "[::1]x",
            "a b", "host/path", "-bad.example", "bad-.example", "bad..example", "host\tname", "user@host", "[::1]:99999",
        ).forEach { header ->
            assertNull("expected rejection of ${header?.let { "\"$it\"" }}", HostHeaders.hostnameOf(header))
        }
    }

    @Test
    fun originHostsComeFromHttpOriginsOnly() {
        assertEquals("localhost", HostHeaders.originHostOf("http://localhost:6274"))
        assertEquals("127.0.0.1", HostHeaders.originHostOf("HTTPS://127.0.0.1"))
        assertEquals("[::1]", HostHeaders.originHostOf("http://[::1]:6274"))
        assertEquals("inspector.example", HostHeaders.originHostOf("https://Inspector.Example"))
        listOf(null, "", "null", "localhost", "ftp://localhost", "http://", "http://user@localhost", "http://localhost/path", "http://localhost?x", "://localhost")
            .forEach { origin ->
                assertNull("expected rejection of ${origin?.let { "\"$it\"" }}", HostHeaders.originHostOf(origin))
            }
    }

    @Test
    fun loopbackNamesCoverLocalhostAndTheLoopbackBlocks() {
        listOf("localhost", "127.0.0.1", "127.255.0.9", "[::1]").forEach { assertTrue(it, HostHeaders.isLoopbackName(it)) }
        listOf("192.168.1.1", "localhost.evil", "[::2]", "127.0.0", "1270.0.0.1").forEach { assertFalse(it, HostHeaders.isLoopbackName(it)) }
    }
}
