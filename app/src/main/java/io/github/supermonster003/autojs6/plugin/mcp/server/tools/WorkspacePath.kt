package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure

/** Lexical boundary before Binder; the host also checks canonical paths and symlinks. */
object WorkspacePath {
    const val MAX_BYTES = 4096

    fun normalize(raw: String, allowRoot: Boolean = true): String {
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_BYTES || raw.isBlank() ||
            raw.startsWith('/') || '\\' in raw || raw.any { it.isISOControl() } ||
            Regex("^[A-Za-z]:").containsMatchIn(raw)
        ) fail()
        val path = raw.removePrefix("./").removeSuffix("/")
        if (Regex("^[A-Za-z]:").containsMatchIn(path)) fail()
        if (path == "." || path.isEmpty()) {
            if (allowRoot && raw in listOf(".", "./")) return "."
            fail()
        }
        if (path.split('/').any { it.isEmpty() || it == "." || it == ".." }) fail()
        return path
    }

    private fun fail(): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments(
        "path must be a POSIX path inside the AutoJs6 working directory; absolute paths, traversal, control characters, and root mutations are refused",
    ))
}
