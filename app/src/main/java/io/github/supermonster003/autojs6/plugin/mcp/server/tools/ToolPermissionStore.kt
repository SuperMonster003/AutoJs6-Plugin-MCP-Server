package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ProcessSharedFile
import java.io.IOException

/**
 * Persists the [ToolPermissions] as one JSON document next to the other stores (roadmap P2.3,
 * decision D6). Re-read on every [load]: the settings page (P4.2) writes it from the main
 * process while the listener runs in `:mcp_server`.
 */
class ToolPermissionStore(context: Context) {

    private val document = ProcessSharedFile(context, FILE_NAME)

    fun load(): ToolPermissions = ToolPermissions.decode(document.read())

    fun save(permissions: ToolPermissions) {
        try {
            document.write(permissions.encode())
        } catch (e: IOException) {
            Log.w(TAG, "Cannot store the tool group switches (${e.javaClass.simpleName})")
        }
    }

    fun update(transform: (ToolPermissions) -> ToolPermissions): ToolPermissions = transform(load()).also(::save)

    /** Drops every override; the next [load] yields the D6 defaults. */
    fun reset() {
        try {
            document.write(null)
        } catch (e: IOException) {
            Log.w(TAG, "Cannot reset the tool group switches (${e.javaClass.simpleName})")
        }
    }

    companion object {

        const val FILE_NAME = "tool_groups.json"

        private const val TAG = "ToolPermissionStore"
    }
}
