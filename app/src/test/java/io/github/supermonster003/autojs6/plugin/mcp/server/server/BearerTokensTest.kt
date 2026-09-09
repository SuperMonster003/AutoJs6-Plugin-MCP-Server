package io.github.supermonster003.autojs6.plugin.mcp.server.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class BearerTokensTest {

    @Test
    fun generatedTokensAreUnpaddedBase64UrlOf32Bytes() {
        val token = BearerTokens.generate()
        assertEquals(BearerTokens.TOKEN_LENGTH, token.length)
        assertTrue(token, BearerTokens.isWellFormed(token))
        assertNotEquals(token, BearerTokens.generate())
        val seeded = BearerTokens.generate(SecureRandom.getInstance("SHA1PRNG").apply { setSeed(7L) })
        assertTrue(seeded, BearerTokens.isWellFormed(seeded))
        listOf(null, "", "short", "a".repeat(44), "a".repeat(42) + "+", "a".repeat(42) + "=").forEach {
            assertFalse("expected malformed: $it", BearerTokens.isWellFormed(it))
        }
    }

    @Test
    fun base64UrlMatchesTheStandardEncoderForEveryLength() {
        val random = java.util.Random(11L)
        val standard = java.util.Base64.getUrlEncoder().withoutPadding()
        (0..64).forEach { length ->
            val bytes = ByteArray(length).also(random::nextBytes)
            assertEquals("length $length", standard.encodeToString(bytes), BearerTokens.base64Url(bytes))
        }
    }

    @Test
    fun authorizationHeaderYieldsTheTokenForBearerOnly() {
        assertEquals("abc", BearerTokens.parseAuthorization("Bearer abc"))
        assertEquals("abc", BearerTokens.parseAuthorization("bearer abc"))
        assertEquals("abc", BearerTokens.parseAuthorization("  BEARER   abc  "))
        listOf(null, "", "Bearer", "Bearer ", "Bearerabc", "Basic abc", "Bearer a b", "Token abc").forEach {
            assertNull("expected rejection of ${it?.let { value -> "\"$value\"" }}", BearerTokens.parseAuthorization(it))
        }
    }

    @Test
    fun comparisonAcceptsOnlyTheExactTokenAndFailsClosed() {
        val token = BearerTokens.generate()
        assertTrue(BearerTokens.constantTimeEquals(token, token))
        assertFalse(BearerTokens.constantTimeEquals(token, token.dropLast(1)))
        assertFalse(BearerTokens.constantTimeEquals(token, token + "x"))
        assertFalse(BearerTokens.constantTimeEquals(token, BearerTokens.generate()))
        assertFalse(BearerTokens.constantTimeEquals(token, null))
        assertFalse(BearerTokens.constantTimeEquals(token, ""))
        assertFalse(BearerTokens.constantTimeEquals(null, token))
        assertFalse(BearerTokens.constantTimeEquals("", ""))
        assertFalse(BearerTokens.constantTimeEquals("   ", "   "))
    }

    @Test
    fun fingerprintAndTailAreStableAndShort() {
        val token = "A".repeat(43)
        assertEquals(BearerTokens.fingerprint(token), BearerTokens.fingerprint(token))
        assertEquals(16, BearerTokens.fingerprint(token).length)
        assertTrue(BearerTokens.fingerprint(token).all { it in "0123456789abcdef" })
        assertNotEquals(BearerTokens.fingerprint(token), BearerTokens.fingerprint("B".repeat(43)))
        assertEquals("AAAA", BearerTokens.tail(token))
        assertEquals("xyz", BearerTokens.tail("xyz"))
    }
}
