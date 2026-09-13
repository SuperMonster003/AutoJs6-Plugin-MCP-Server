package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Workspace

/** Scoped file operations use the host explorer's file bridge, including its refresh notifications. */
class FileTools : ToolFlows {
    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? {
        if (spec.name !in ToolCatalog.FILE_NAMES) return null
        val path = WorkspacePath.normalize(ToolArguments.string(arguments, Workspace.PATH, ".")!!,
            allowRoot = spec.name in listOf(ToolCatalog.FILES_LIST, ToolCatalog.FILES_STAT))
        val options = buildJsonObject {
            when (spec.name) {
                ToolCatalog.FILES_LIST -> {
                    put(Workspace.RECURSIVE, ToolArguments.boolean(arguments, Workspace.RECURSIVE, false))
                    put(Workspace.MAX_ENTRIES, ToolArguments.long(arguments, Workspace.MAX_ENTRIES, 500))
                }
                ToolCatalog.FILES_READ -> {
                    put(Workspace.ENCODING, ToolArguments.string(arguments, Workspace.ENCODING, "utf-8"))
                    put(Workspace.MAX_BYTES, ToolArguments.long(arguments, Workspace.MAX_BYTES, ToolCatalog.Files.MAX_BYTES.toLong()))
                }
                ToolCatalog.FILES_WRITE -> {
                    put(Workspace.CREATE_DIRS, ToolArguments.boolean(arguments, Workspace.CREATE_DIRS, true))
                    put(Workspace.OVERWRITE, ToolArguments.boolean(arguments, Workspace.OVERWRITE, true))
                }
                ToolCatalog.FILES_RENAME -> put(Workspace.OVERWRITE, ToolArguments.boolean(arguments, Workspace.OVERWRITE, false))
                ToolCatalog.FILES_DELETE -> put(Workspace.RECURSIVE, ToolArguments.boolean(arguments, Workspace.RECURSIVE, false))
            }
        }
        val args = buildJsonArray {
            add(path)
            when (spec.name) {
                ToolCatalog.FILES_WRITE -> add(ToolArguments.string(arguments, Workspace.CONTENT)!!)
                ToolCatalog.FILES_RENAME -> add(WorkspacePath.normalize(ToolArguments.string(arguments, Workspace.TO)!!, false))
            }
            if (options.isNotEmpty()) add(options)
        }
        return ToolFlow(spec.timeoutMs) { caller ->
            val target = spec.bridge!!
            val result = caller.callOrThrow(target.module, target.method, args, spec.timeoutMs, spec.permissions) as? JsonObject
                ?: throw ToolFailureException(ToolFailure.internal("the host returned an invalid file result"))
            ToolFlowResult(JsonObject(result.filterKeys { it != "schema" && it != Workspace.ROOT }))
        }
    }
}
