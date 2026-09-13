package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Workspace
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

class ShellTools(private val permissions: () -> ToolPermissions) : ToolFlows {
    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? {
        if (spec.name != ToolCatalog.SHELL_EXEC) return null
        val command = ToolArguments.string(arguments, Workspace.CMD)!!
        if (command.isBlank() || '\u0000' in command) throw ToolArgumentException(ToolFailure.invalidArguments("cmd must be non-blank and contain no NUL"))
        val root = ToolArguments.boolean(arguments, Workspace.ROOT, false)
        val timeout = ToolArguments.long(arguments, Workspace.TIMEOUT_MS, ToolCatalog.Shell.DEFAULT_TIMEOUT_MS)
        val maximum = ToolArguments.long(arguments, Workspace.MAX_OUTPUT_BYTES, ToolCatalog.Shell.DEFAULT_OUTPUT_BYTES)
        val args = buildJsonArray {
            add(command)
            add(buildJsonObject { put(Workspace.ROOT, root); put(Workspace.TIMEOUT_MS, timeout); put(Workspace.MAX_OUTPUT_BYTES, maximum) })
        }
        return ToolFlow(timeout + 2_000) { caller ->
            val policy = permissions()
            if (!policy.isEnabled(ToolGroup.SHELL)) throw ToolFailureException(ToolFailure.toolDisabled(spec.name, ToolGroup.SHELL.id))
            if (root && !policy.allowShellRoot) throw ToolFailureException(ToolFailure(ToolErrorCodes.TOOL_DISABLED,
                "root shell is switched off", "enable the separate allow root setting on the phone"))
            val result = caller.callOrThrow("shell", "exec", args, timeout + 2_000,
                if (root) spec.permissions + "shell.root" else spec.permissions) as? JsonObject
                ?: throw ToolFailureException(ToolFailure.internal("the host returned an invalid shell result"))
            ToolFlowResult(shape(result, maximum.toInt()))
        }
    }

    /** The plugin also enforces the output budget when connected to an older host. */
    internal fun shape(result: JsonObject, maximum: Int): JsonObject {
        fun text(key: String): String = (result[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw ToolFailureException(ToolFailure.internal("the host shell result is missing $key"))
        fun prefix(value: String, limit: Int): String {
            val bytes = value.toByteArray(Charsets.UTF_8)
            return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE)
                .decode(ByteBuffer.wrap(bytes, 0, bytes.size.coerceAtMost(limit))).toString()
        }
        val rawOut = text("stdout")
        val rawErr = text("stderr")
        val out = prefix(rawOut, maximum)
        val err = prefix(rawErr, maximum - out.toByteArray(Charsets.UTF_8).size)
        return buildJsonObject {
            result.filterKeys { it != "schema" }.forEach { (key, value) -> put(key, value) }
            put("stdout", out); put("stderr", err)
            put("outputBytes", out.toByteArray(Charsets.UTF_8).size + err.toByteArray(Charsets.UTF_8).size)
            put("truncated", (result["truncated"] as? JsonPrimitive)?.booleanOrNull == true || out != rawOut || err != rawErr)
        }
    }
}
