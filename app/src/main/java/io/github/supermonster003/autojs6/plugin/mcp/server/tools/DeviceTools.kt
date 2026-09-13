package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Workspace

class DeviceTools : ToolFlows {
    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? {
        if (spec.name !in ToolCatalog.DEVICE_ACTION_NAMES) return null
        var method = spec.bridge!!.method
        val args = buildJsonArray {
            when (spec.name) {
                ToolCatalog.EDITOR_OPEN -> {
                    add(WorkspacePath.normalize(ToolArguments.string(arguments, Workspace.PATH)!!, false))
                    add(buildJsonObject {
                        put(Workspace.LINE, ToolArguments.long(arguments, Workspace.LINE, 1))
                        put(Workspace.COLUMN, ToolArguments.long(arguments, Workspace.COLUMN, 1))
                    })
                }
                ToolCatalog.APP_LAUNCH -> {
                    val pkg = ToolArguments.string(arguments, Workspace.PACKAGE_NAME)
                    val name = ToolArguments.string(arguments, Workspace.APP_NAME)
                    if ((pkg == null) == (name == null)) invalid("provide exactly one of packageName or appName")
                    method = if (pkg != null) "launchPackage" else "launchApp"
                    add(nonBlank(pkg ?: name!!))
                }
                ToolCatalog.APP_LIST -> add(buildJsonObject { put(Workspace.QUERY, ToolArguments.string(arguments, Workspace.QUERY, "")) })
                ToolCatalog.CLIPBOARD_SET -> add(ToolArguments.string(arguments, Workspace.TEXT)!!)
                ToolCatalog.TOAST -> add(nonBlank(ToolArguments.string(arguments, Workspace.TEXT)!!))
            }
        }
        return ToolFlow(spec.timeoutMs) { caller ->
            val result = caller.callOrThrow(spec.bridge.module, method, args, spec.timeoutMs, spec.permissions)
            val shaped = buildJsonObject {
                when (spec.name) {
                    ToolCatalog.APP_LIST -> {
                        val obj = result as? JsonObject ?: throw ToolFailureException(ToolFailure.internal("the host returned an invalid app list"))
                        obj.filterKeys { it != "schema" }.forEach { (key, value) -> put(key, value) }
                    }
                    ToolCatalog.CLIPBOARD_GET -> {
                        val text = (result as? JsonPrimitive)?.contentOrNull ?: ""
                        if (text.toByteArray(Charsets.UTF_8).size > ToolCatalog.Device.MAX_TEXT_BYTES)
                            throw ToolFailureException(ToolFailure.limitExceeded("clipboard text exceeds the tool limit"))
                        put(Workspace.TEXT, text)
                        put("hasText", text.isNotEmpty())
                    }
                    ToolCatalog.DEVICE_ENSURE_ACCESSIBILITY -> {
                        if ((result as? JsonPrimitive)?.booleanOrNull != true)
                            throw ToolFailureException(ToolFailure(ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING,
                                "AutoJs6 could not enable its accessibility service", ToolFailure.HINT_ACCESSIBILITY_MANUAL))
                        put("enabled", true)
                    }
                    ToolCatalog.EDITOR_OPEN, ToolCatalog.APP_LAUNCH -> {
                        if ((result as? JsonPrimitive)?.booleanOrNull != true)
                            throw ToolFailureException(ToolFailure.actionFailed("AutoJs6 could not open the requested target", "check that the file or application exists and Android permits opening it"))
                        put("opened", true)
                        if (spec.name == ToolCatalog.EDITOR_OPEN) {
                            put(Workspace.PATH, args[0]); put(Workspace.LINE, args[1].jsonObject[Workspace.LINE]!!); put(Workspace.COLUMN, args[1].jsonObject[Workspace.COLUMN]!!)
                        }
                    }
                    ToolCatalog.CLIPBOARD_SET -> put("written", true)
                    ToolCatalog.TOAST -> put("shown", true)
                }
            }
            ToolFlowResult(shaped)
        }
    }

    private fun nonBlank(value: String): String = value.also { if (it.isBlank() || '\u0000' in it) invalid("text must be non-blank and contain no NUL") }
    private fun invalid(message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments(message))
}
