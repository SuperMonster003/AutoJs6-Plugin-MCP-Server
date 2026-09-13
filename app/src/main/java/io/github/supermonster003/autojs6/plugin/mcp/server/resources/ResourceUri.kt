package io.github.supermonster003.autojs6.plugin.mcp.server.resources

import io.github.supermonster003.autojs6.plugin.mcp.server.tools.WorkspacePath
import io.modelcontextprotocol.kotlin.sdk.types.McpException
import io.modelcontextprotocol.kotlin.sdk.types.RPCError
import java.io.ByteArrayOutputStream
import java.net.URI

/** Decodes URI path segments exactly once, before the shared workspace boundary check. */
data class ResourceUri(val kind: Kind, val path: String = ".", val directory: Boolean = false) {
    enum class Kind { WORKSPACE, SAMPLES, DEVICE, CONSOLE }

    companion object {
        const val DEVICE = "autojs6://device/info"
        const val CONSOLE = "autojs6://console/tail"
        const val SAMPLES = "autojs6://samples/"
        const val WORKSPACE_TEMPLATE = "autojs6://workspace/{+path}"
        const val SAMPLES_TEMPLATE = "autojs6://samples/{+path}"
        const val MAX_URI_BYTES = WorkspacePath.MAX_BYTES * 3 + 64

        fun parse(value: String): ResourceUri {
            if (value.toByteArray(Charsets.UTF_8).size > MAX_URI_BYTES) invalid()
            val uri = try { URI(value) } catch (_: Exception) { invalid() }
            if (uri.scheme != "autojs6" || uri.rawQuery != null || uri.rawFragment != null ||
                uri.rawUserInfo != null || uri.port != -1 || uri.isOpaque) invalid()
            if (value == DEVICE) return ResourceUri(Kind.DEVICE)
            if (value == CONSOLE) return ResourceUri(Kind.CONSOLE)
            val kind = when (uri.rawAuthority) {
                "workspace" -> Kind.WORKSPACE
                "samples" -> Kind.SAMPLES
                else -> throw McpException(RPCError.ErrorCode.RESOURCE_NOT_FOUND, "Unknown AutoJs6 resource")
            }
            val raw = uri.rawPath ?: invalid()
            if (!raw.startsWith('/')) invalid()
            val directory = raw.endsWith('/')
            if (kind == Kind.WORKSPACE && directory) invalid()
            if (raw == "/") return ResourceUri(kind, ".", directory = true)
            val segments = raw.removePrefix("/").removeSuffix("/").split('/')
            val path = segments.joinToString("/") { segment ->
                val decoded = decode(segment)
                if (decoded.isEmpty() || '/' in decoded || '\\' in decoded ||
                    decoded == "." || decoded == ".." || decoded.any { it.isISOControl() }) invalid()
                decoded
            }
            return ResourceUri(kind, WorkspacePath.normalize(path, allowRoot = false), directory)
        }

        fun sample(path: String, directory: Boolean = false): String =
            "autojs6://samples/" + path.split('/').joinToString("/") { encode(it) } + if (directory) "/" else ""

        private fun decode(raw: String): String {
            val out = ByteArrayOutputStream()
            var i = 0
            while (i < raw.length) {
                if (raw[i] == '%') {
                    if (i + 2 >= raw.length) invalid()
                    out.write(raw.substring(i + 1, i + 3).toIntOrNull(16) ?: invalid())
                    i += 3
                } else {
                    val end = raw.indexOf('%', i).let { if (it < 0) raw.length else it }
                    out.write(raw.substring(i, end).toByteArray(Charsets.UTF_8))
                    i = end
                }
            }
            return try { out.toByteArray().decodeToString(throwOnInvalidSequence = true) } catch (_: Exception) { invalid() }
        }

        private fun encode(value: String): String = buildString {
            value.toByteArray(Charsets.UTF_8).forEach { byte ->
                val n = byte.toInt() and 255
                if (n in 65..90 || n in 97..122 || n in 48..57 || n.toChar() in "-._~") append(n.toChar())
                else append('%').append("0123456789ABCDEF"[n shr 4]).append("0123456789ABCDEF"[n and 15])
            }
        }

        private fun invalid(): Nothing = throw McpException(RPCError.ErrorCode.INVALID_PARAMS, "Invalid AutoJs6 resource URI")
    }
}
