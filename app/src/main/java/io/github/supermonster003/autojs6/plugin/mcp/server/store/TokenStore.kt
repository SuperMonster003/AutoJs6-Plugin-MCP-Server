package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.server.BearerTokens
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the bearer token (roadmap P2.2): generated on first use, wrapped with an AES-GCM key
 * that lives in the Android Keystore, and stored as one JSON document in the plugin's private,
 * never-backed-up storage. [rotate] replaces it; pairings survive a rotation because they are
 * keyed by the client, not by the token. The document is re-read on every access so a rotation
 * from another process takes effect at once; the unwrapped value is cached per document text.
 *
 * When the Keystore cannot provide a key (a few devices ship a broken one), the token is kept
 * unwrapped in the same private file and a warning names the reason; the token value itself
 * never reaches the log.
 */
class TokenStore(context: Context) {

    private val document = ProcessSharedFile(context, FILE_NAME)

    /** The document text last unwrapped and the token it held. */
    @Volatile
    private var cached: Pair<String, String>? = null

    /** The current token, created when none exists yet. */
    fun current(): String {
        document.read()?.let { text -> unwrap(text)?.let { return it } }
        return issue(force = false)
    }

    /** Replaces the token; clients must present the new value from now on. */
    fun rotate(): String = issue(force = true)

    fun fingerprint(): String = BearerTokens.fingerprint(current())

    fun tail(): String = BearerTokens.tail(current())

    fun createdAt(): Long = document.read()?.let(::parse)?.get(KEY_CREATED_AT)?.jsonPrimitive?.longOrNull ?: 0L

    private fun issue(force: Boolean): String {
        var kept: String? = null
        var issued: String? = null
        val stored = try {
            document.update { current ->
                kept = if (force) null else current?.let(::unwrap)
                if (kept != null) return@update current
                val fresh = BearerTokens.generate()
                issued = fresh
                encode(fresh)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Cannot store the token (${e.javaClass.simpleName}); the current one is valid for this run only")
            null
        }
        kept?.let { return it }
        val token = issued ?: BearerTokens.generate()
        if (stored != null) cached = stored to token
        Log.i(TAG, "Token ${if (force) "rotated" else "issued"} (fingerprint ${BearerTokens.fingerprint(token)})")
        return token
    }

    private fun unwrap(text: String): String? {
        cached?.let { (source, token) -> if (source == text) return token }
        val stored = parse(text) ?: return null
        val value = stored[KEY_TOKEN]?.jsonPrimitive?.contentOrNull ?: return null
        val wrapped = stored[KEY_WRAPPED]?.jsonPrimitive?.booleanOrNull ?: true
        val token = if (wrapped) {
            val iv = stored[KEY_IV]?.jsonPrimitive?.contentOrNull ?: return null
            runCatching { decrypt(Base64.decode(value, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP)) }
                .onFailure { Log.w(TAG, "Stored token cannot be unwrapped (${it.javaClass.simpleName}); a new one will be issued") }
                .getOrNull()
        } else {
            value
        }
        return token?.takeIf { BearerTokens.isWellFormed(it) }?.also { cached = text to it }
    }

    private fun encode(token: String): String {
        val wrapped = runCatching { encrypt(token) }
            .onFailure { Log.w(TAG, "Keystore unavailable (${it.javaClass.simpleName}); keeping the token unwrapped in private storage") }
            .getOrNull()
        return buildJsonObject {
            put(KEY_FORMAT, FORMAT)
            put(KEY_CREATED_AT, System.currentTimeMillis())
            if (wrapped != null) {
                put(KEY_TOKEN, Base64.encodeToString(wrapped.second, Base64.NO_WRAP))
                put(KEY_IV, Base64.encodeToString(wrapped.first, Base64.NO_WRAP))
                put(KEY_WRAPPED, true)
            } else {
                put(KEY_TOKEN, token)
                put(KEY_WRAPPED, false)
            }
        }.toString()
    }

    private fun parse(text: String): JsonObject? = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()

    private fun encrypt(plain: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv to cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
    }

    private fun decrypt(cipherText: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {

        const val FILE_NAME = "token.json"

        private const val FORMAT = 1
        private const val KEY_FORMAT = "format"
        private const val KEY_TOKEN = "token"
        private const val KEY_IV = "iv"
        private const val KEY_WRAPPED = "wrapped"
        private const val KEY_CREATED_AT = "created_at"

        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "mcp_server_token"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val TAG = "TokenStore"
    }
}
