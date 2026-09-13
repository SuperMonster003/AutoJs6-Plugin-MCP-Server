package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.util.Log
import java.io.IOException
import io.github.supermonster003.autojs6.plugin.mcp.server.host.SessionStatus
import kotlinx.serialization.json.*
import org.autojs.plugin.mcp.server.api.McpServerContract as C

/** Small, non-sensitive status snapshot for the separate settings process. Never contains a token. */
class ServerStatusStore(private val context: Context) {
    private val document = ProcessSharedFile(context, "status.json")
    fun save(value: SessionStatus) {
        try { document.write(buildJsonObject {
            put("processId", Process.myPid())
            put(C.KEY_STATUS_STATE, value.state)
            put(C.KEY_STATUS_ENDPOINTS, JsonArray(value.endpoints.map(::JsonPrimitive)))
            put(C.KEY_STATUS_HOST_AVAILABLE, value.hostAvailable)
            put(C.KEY_STATUS_LAST_ERROR_CODE, value.lastErrorCode)
            put(C.KEY_STATUS_PORT, value.port)
        }.toString()) } catch (error: IOException) {
            Log.w("McpServerStatusStore", "Cannot store status (${error.javaClass.simpleName})")
        }
    }
    fun load(): SessionStatus {
        val processes = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses.orEmpty()
        val process = processes.firstOrNull { it.processName == context.packageName + ":mcp_server" } ?: return SessionStatus(C.STATE_STOPPED)
        return runCatching {
            val json = Json.parseToJsonElement(document.read() ?: "{}").jsonObject
            if (json["processId"]?.jsonPrimitive?.intOrNull != process.pid) return SessionStatus(C.STATE_STOPPED)
            SessionStatus(
                state = json[C.KEY_STATUS_STATE]?.jsonPrimitive?.contentOrNull ?: C.STATE_STOPPED,
                endpoints = json[C.KEY_STATUS_ENDPOINTS]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                hostAvailable = json[C.KEY_STATUS_HOST_AVAILABLE]?.jsonPrimitive?.booleanOrNull ?: false,
                lastErrorCode = json[C.KEY_STATUS_LAST_ERROR_CODE]?.jsonPrimitive?.contentOrNull,
                port = json[C.KEY_STATUS_PORT]?.jsonPrimitive?.intOrNull ?: 0,
            )
        }.getOrElse { SessionStatus(C.STATE_STOPPED) }
    }
}
