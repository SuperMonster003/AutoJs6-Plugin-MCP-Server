package io.github.supermonster003.autojs6.plugin.mcp.server.server

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The bearer token format and its checks (roadmap P2.2): 32 random bytes as unpadded base64url
 * (43 characters), a constant-time comparison that never short-circuits on length, and the
 * derived values the UI and the pairing records may show (fingerprint, last characters). Tokens
 * themselves never appear in logs, notifications, or errors.
 */
object BearerTokens {

    const val TOKEN_BYTES = 32
    const val TOKEN_LENGTH = 43
    const val SCHEME = "Bearer"
    const val REALM = "autojs6-mcp-server"

    private val TOKEN_ALPHABET = Regex("[A-Za-z0-9_-]{$TOKEN_LENGTH}")

    fun generate(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return base64Url(bytes)
    }

    fun isWellFormed(token: String?): Boolean = token != null && TOKEN_ALPHABET.matches(token)

    /** The token of an `Authorization: Bearer <token>` header; null when the header is absent or malformed. */
    fun parseAuthorization(header: String?): String? {
        val value = header?.trim() ?: return null
        if (value.length <= SCHEME.length + 1) return null
        if (!value.regionMatches(0, SCHEME, 0, SCHEME.length, ignoreCase = true)) return null
        if (!value[SCHEME.length].isWhitespace()) return null
        val token = value.substring(SCHEME.length).trim()
        return token.takeIf { it.isNotEmpty() && it.none { char -> char.isWhitespace() } }
    }

    /**
     * True only when both values are present and identical. The comparison runs over the SHA-256
     * digests of both values, so the time taken does not depend on where they differ or on their
     * lengths; an absent or blank expected token fails closed.
     */
    fun constantTimeEquals(expected: String?, presented: String?): Boolean {
        if (expected.isNullOrBlank() || presented.isNullOrEmpty()) return false
        val left = sha256(expected)
        val right = sha256(presented)
        var difference = 0
        for (index in left.indices) {
            difference = difference or (left[index].toInt() xor right[index].toInt())
        }
        return difference == 0
    }

    /** First 16 hex characters of the SHA-256 digest; safe to log and to show. */
    fun fingerprint(token: String): String = sha256(token).joinToString("") { "%02x".format(it) }.substring(0, 16)

    /** The last [count] characters, the part the pairing dialog shows to identify the token. */
    fun tail(token: String, count: Int = 4): String = token.takeLast(count)

    /** Unpadded base64url (RFC 4648 section 5), hand-rolled because `java.util.Base64` needs API 26. */
    internal fun base64Url(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].toInt() and 0xFF
            val second = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xFF else -1
            val third = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else -1
            out.append(BASE64_URL_ALPHABET[first shr 2])
            out.append(BASE64_URL_ALPHABET[((first and 0x03) shl 4) or (if (second >= 0) second shr 4 else 0)])
            if (second >= 0) out.append(BASE64_URL_ALPHABET[((second and 0x0F) shl 2) or (if (third >= 0) third shr 6 else 0)])
            if (third >= 0) out.append(BASE64_URL_ALPHABET[third and 0x3F])
            index += 3
        }
        return out.toString()
    }

    private const val BASE64_URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
}
