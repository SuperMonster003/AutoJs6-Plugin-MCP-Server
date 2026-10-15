package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure

/** A path a read-only tool may send to the host: a workspace path, or an absolute path into shared storage. */
data class ReadTarget(val path: String, val shared: Boolean) {

    /** The tool's permission tokens, plus `files.shared_read` when the target lies in shared storage (roadmap P11 D26). */
    fun permissions(base: List<String>): List<String> = if (shared) base + PathAccess.TOKEN_SHARED_READ else base
}

/**
 * Where the read-only tools may read (roadmap P11 D26): relative paths are the AutoJs6 working
 * directory, as before; absolute paths are accepted only while the "Allow reading shared storage"
 * switch is on ([sharedReadAllowed], read again for every call) and only inside the primary shared
 * storage ([sharedRoots], for example `/storage/emulated/0` and its `/sdcard` alias), never under
 * `Android/data` or `Android/obb`. This is the plugin's pre-check with a clear message; AutoJs6
 * checks the canonical path again and needs its own all-files access, and a request reaches shared
 * storage only when it declares `files.shared_read` ([ReadTarget.permissions]).
 */
class PathAccess(
    private val sharedRoots: List<String>,
    private val sharedReadAllowed: () -> Boolean,
) {

    /** Whether the switch is on right now. */
    val sharedReadEnabled: Boolean
        get() = sharedReadAllowed()

    /**
     * [raw] as a read target for argument [key] of [tool]. A relative path goes through
     * [WorkspacePath.normalize]; [allowRoot] lets list / stat name the working directory itself.
     */
    fun readable(tool: String, key: String, raw: String, allowRoot: Boolean = false): ReadTarget {
        if (!raw.startsWith('/')) return ReadTarget(WorkspacePath.normalize(raw, allowRoot), shared = false)
        if (!sharedReadAllowed()) {
            throw ToolArgumentException(ToolFailure(
                ToolErrorCodes.CAPABILITY_DENIED,
                "$tool: $key is an absolute path, and reading shared storage is switched off; relative paths point into the AutoJs6 working directory",
                HINT_SWITCH,
            ))
        }
        return ReadTarget(sharedPath(tool, key, raw), shared = true)
    }

    private fun sharedPath(tool: String, key: String, raw: String): String {
        if (raw.toByteArray(Charsets.UTF_8).size > WorkspacePath.MAX_BYTES || '\\' in raw || raw.any { it.isISOControl() }) {
            invalid(tool, "$key must be a plain absolute path without backslashes or control characters")
        }
        val path = raw.trimEnd('/').ifEmpty { "/" }
        val segments = path.removePrefix("/").split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) invalid(tool, "$key must not contain empty, . or .. segments")
        val root = sharedRoots.firstOrNull { path == it || path.startsWith("$it/") }
            ?: invalid(tool, "$key must lie in shared storage (${sharedRoots.joinToString(" or ")}); other absolute paths are not readable")
        val inside = path.removePrefix(root).removePrefix("/").split('/').map { it.lowercase() }
        if (inside.size >= 2 && inside[0] == "android" && inside[1] in HIDDEN) {
            invalid(tool, "$key points into Android/${inside[1]}, which is never readable")
        }
        return path
    }

    private fun invalid(tool: String, message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments("$tool: $message"))

    companion object {

        /** The host permission token for shared storage reads (`NodeBridgePermissionManifest.FILES_SHARED_READ`). */
        const val TOKEN_SHARED_READ = "files.shared_read"

        const val HINT_SWITCH = "switch on \"Allow reading shared storage\" in the MCP Server settings on the phone, or copy the file into the AutoJs6 working directory"

        /** No shared storage: absolute paths are refused as while the switch is off. For tests and old wiring. */
        val WORKSPACE_ONLY = PathAccess(emptyList()) { false }

        private val HIDDEN = setOf("data", "obb")
    }
}
