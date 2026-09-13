package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.content.Context
import android.util.Log
import java.io.IOException
import kotlinx.serialization.json.*

/** Remembers a user's stop even when the host was dead and could not receive its callback. */
class ServerLifecycleStore(context: Context) {
    private val document = ProcessSharedFile(context, FILE_NAME)
    var userStopped: Boolean
        get() = decode(document.read())
        set(value) {
            try { document.write(buildJsonObject { put(KEY_USER_STOPPED, value) }.toString()) }
            catch (error: IOException) { Log.w("McpServerLifecycle", "Cannot store user intent (${error.javaClass.simpleName})") }
        }

    companion object {
        const val FILE_NAME = "lifecycle.json"
        private const val KEY_USER_STOPPED = "userStopped"
        internal fun decode(text: String?): Boolean = runCatching {
            text?.let { Json.parseToJsonElement(it).jsonObject[KEY_USER_STOPPED]?.jsonPrimitive?.takeIf { value -> !value.isString }?.booleanOrNull }
        }.getOrNull() ?: true
    }
}
