package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Workspace

/**
 * Scoped file operations use the host explorer's file bridge, including its refresh notifications.
 * `files_list` / `files_stat` also accept absolute shared storage paths while the switch of roadmap
 * P11 D26 is on ([PathAccess]); their entries then come back as absolute paths with the shared
 * storage `root`, so a model can hand them to the recognition tools. Every other file tool stays in
 * the working directory.
 */
class FileTools(private val access: PathAccess = PathAccess.WORKSPACE_ONLY) : ToolFlows {
    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? {
        if (spec.name !in ToolCatalog.FILE_NAMES) return null
        val raw = ToolArguments.string(arguments, Workspace.PATH, ".")!!
        val target = if (spec.name in READABLE) access.readable(spec.name, Workspace.PATH, raw, allowRoot = true) else ReadTarget(WorkspacePath.normalize(raw, allowRoot = false), shared = false)
        val path = target.path
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
            val method = spec.bridge!!
            val result = caller.callOrThrow(method.module, method.method, args, spec.timeoutMs, target.permissions(spec.permissions)) as? JsonObject
                ?: throw ToolFailureException(ToolFailure.internal("the host returned an invalid file result"))
            ToolFlowResult(if (target.shared) sharedResult(result) else JsonObject(result.filterKeys { it != "schema" && it != Workspace.ROOT }))
        }
    }

    /** A shared storage answer: the host's paths are relative to its `root`; they come back absolute. */
    private fun sharedResult(result: JsonObject): JsonObject {
        val root = (result[Workspace.ROOT] as? JsonPrimitive)?.contentOrNull?.trimEnd('/')
            ?: throw ToolFailureException(ToolFailure.internal("the host returned a shared storage result without its root"))
        fun absolute(element: JsonElement?): JsonElement? = (element as? JsonPrimitive)?.contentOrNull?.let { path ->
            JsonPrimitive(if (path == "." || path.isEmpty()) root else "$root/$path")
        }
        return buildJsonObject {
            result.forEach { (key, value) ->
                when (key) {
                    "schema" -> Unit
                    "path" -> put(key, absolute(value) ?: value)
                    "entries" -> put(key, JsonArray((value as? JsonArray).orEmpty().map { entry ->
                        val item = entry as? JsonObject ?: return@map entry
                        JsonObject(item.mapValues { (name, field) -> if (name == "path") absolute(field) ?: field else field })
                    }))
                    else -> put(key, value)
                }
            }
            put("sharedStorage", true)
        }
    }

    private companion object {
        /** The file tools that read and may therefore reach shared storage. */
        val READABLE = setOf(ToolCatalog.FILES_LIST, ToolCatalog.FILES_STAT)
    }
}
