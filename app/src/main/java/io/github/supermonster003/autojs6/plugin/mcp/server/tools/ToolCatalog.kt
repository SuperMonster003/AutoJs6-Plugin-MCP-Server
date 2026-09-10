package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.DevicePingTool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract

/**
 * The tool table of the plugin (roadmap P2.3, appendix A.2). P2.3 ships the minimal set that
 * proves the whole path (`device_ping` locally, `device_info` and `script_run` through the host);
 * later phases append rows. `ToolCatalogTest` guards names, schemas, defaults, and a snapshot.
 */
object ToolCatalog {

    const val DEVICE_PING = DevicePingTool.NAME
    const val DEVICE_INFO = "device_info"
    const val SCRIPT_RUN = "script_run"

    /** `script_run` argument names, shared with the executor and the tests. */
    object ScriptRun {
        const val SOURCE = "source"
        const val NAME = "name"
        const val WORKING_DIRECTORY = "workingDirectory"
        const val ARGUMENTS = "arguments"
        const val TIMEOUT_MS = "timeoutMs"
        const val WAIT_FOR_COMPLETION = "waitForCompletion"
        const val CAPTURE_CONSOLE = "captureConsole"
        const val MAX_CONSOLE_LINES = "maxConsoleLines"

        const val DEFAULT_NAME = "mcp_script"
        const val DEFAULT_TIMEOUT_MS = 60_000L
        const val MIN_TIMEOUT_MS = 1_000L
        const val DEFAULT_MAX_CONSOLE_LINES = 200L
        const val MAX_SOURCE_BYTES = 512 * 1024
        const val MAX_NAME_LENGTH = 128
        const val MAX_WORKING_DIRECTORY_LENGTH = 512

        /** The host refuses an `engines.execScript` start timeout above this (its own ceiling). */
        const val MAX_START_TIMEOUT_MS = 60_000L
    }

    val devicePing: ToolSpec = ToolSpec(
        name = DEVICE_PING,
        title = "Device ping",
        description = DevicePingTool.DESCRIPTION,
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.DEVICE,
        timeoutMs = 5_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val deviceInfo: ToolSpec = ToolSpec(
        name = DEVICE_INFO,
        title = "Device info",
        description = "Returns the device build, screen, battery, memory, AutoJs6 host version and process, " +
                "accessibility service state, screen state, locale, and time zone as AutoJs6 reports them " +
                "(schema autojs6-bridge-device-info-v1). No hardware identifiers.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.DEVICE,
        bridge = BridgeMethod("device", "info"),
        permissions = listOf("device"),
        timeoutMs = 10_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val scriptRun: ToolSpec = ToolSpec(
        name = SCRIPT_RUN,
        title = "Run script",
        description = "Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API), by default " +
                "waits for the script to finish, and returns the outcome plus the console lines it printed. " +
                "Use it for automation steps: toasts, UI actions, file work, app launches. Long-running scripts " +
                "keep running after the wait; the result then says outcome = running.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                ScriptRun.SOURCE to JsonSchemas.string("JavaScript source to execute", maxLength = ScriptRun.MAX_SOURCE_BYTES),
                ScriptRun.NAME to JsonSchemas.string(
                    "Script name shown in the AutoJs6 task list; AutoJs6 appends .js itself, a given .js suffix is dropped",
                    maxLength = ScriptRun.MAX_NAME_LENGTH,
                    default = ScriptRun.DEFAULT_NAME,
                ),
                ScriptRun.WORKING_DIRECTORY to JsonSchemas.string(
                    "POSIX-style path relative to the AutoJs6 working directory; the script cannot escape it",
                    maxLength = ScriptRun.MAX_WORKING_DIRECTORY_LENGTH,
                    default = ".",
                ),
                ScriptRun.ARGUMENTS to JsonSchemas.primitiveMap("Values exposed to the script through engines.myEngine().execArgv"),
                ScriptRun.TIMEOUT_MS to JsonSchemas.integer(
                    "How long to wait for the script to finish, in milliseconds",
                    minimum = ScriptRun.MIN_TIMEOUT_MS,
                    maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS,
                    default = ScriptRun.DEFAULT_TIMEOUT_MS,
                ),
                ScriptRun.WAIT_FOR_COMPLETION to JsonSchemas.boolean("Wait for the script to finish before answering", default = true),
                ScriptRun.CAPTURE_CONSOLE to JsonSchemas.boolean("Include the console lines printed while the script ran", default = true),
                ScriptRun.MAX_CONSOLE_LINES to JsonSchemas.integer(
                    "Keep at most this many of the newest console lines",
                    minimum = 1,
                    maximum = McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong(),
                    default = ScriptRun.DEFAULT_MAX_CONSOLE_LINES,
                ),
            ),
            required = listOf(ScriptRun.SOURCE),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "execScript"),
        permissions = listOf("engines", "engines.exec"),
        timeoutMs = ScriptRun.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(openWorld = true),
    )

    /** Every tool in list order; `tools/list` keeps this order. */
    val all: List<ToolSpec> = listOf(devicePing, deviceInfo, scriptRun)

    val byName: Map<String, ToolSpec> = all.associateBy { it.name }

    /** The groups the catalog uses, for the `mcpServerToolGroups` capability. */
    val groups: List<ToolGroup> = ToolGroup.entries.filter { group -> all.any { it.group == group } }

    fun enabled(permissions: ToolPermissions): List<ToolSpec> = all.filter { permissions.isEnabled(it.group) }

    /** Pretty JSON of the whole catalog; `ToolCatalogTest` compares it with the committed snapshot. */
    fun snapshot(): String = PRETTY.encodeToString(
        JsonArray.serializer(),
        JsonArray(all.map { it.toSnapshotJson() }),
    )

    /** The compact form of the catalog for status and dumps. */
    fun summary(permissions: ToolPermissions): String = buildJsonObject {
        ToolGroup.entries.forEach { group -> put(group.id, permissions.isEnabled(group)) }
    }.toString()

    private val PRETTY = Json { prettyPrint = true; prettyPrintIndent = "  " }
}
