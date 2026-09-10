package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.DevicePingTool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract

/**
 * The tool table of the plugin (appendix A.2). P2.3 shipped the minimal set that proves the whole
 * path (`device_ping` locally, `device_info` and `script_run` through the host); P3.1 completes the
 * `script` group. Later phases append rows. `ToolCatalogTest` guards names, schemas, defaults, and
 * a snapshot.
 */
object ToolCatalog {

    const val DEVICE_PING = DevicePingTool.NAME
    const val DEVICE_INFO = "device_info"
    const val SCRIPT_RUN = "script_run"
    const val SCRIPT_RUN_FILE = "script_run_file"
    const val SCRIPT_STOP = "script_stop"
    const val SCRIPT_STOP_ALL = "script_stop_all"
    const val SCRIPT_LIST = "script_list"
    const val CONSOLE_TAIL = "console_tail"

    /** `script_run` / `script_run_file` argument names and bounds, shared with the executor and the tests. */
    object ScriptRun {
        const val SOURCE = "source"
        const val PATH = "path"
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
        const val MAX_PATH_LENGTH = 512
        const val MAX_WORKING_DIRECTORY_LENGTH = 512

        /** The host refuses an `engines.execScript` start timeout above this (its own ceiling). */
        const val MAX_START_TIMEOUT_MS = 60_000L
    }

    /** `script_stop` argument names. */
    object ScriptStop {
        const val EXECUTION_ID = "executionId"
    }

    /** `console_tail` argument names and bounds (the host's `console.tail`). */
    object ConsoleTail {
        const val LINES = "lines"
        const val SINCE_ID = "sinceId"
        const val LEVEL = "level"
        const val DEFAULT_LINES = 100L
        val LEVELS: List<String> = listOf("verbose", "debug", "info", "warn", "error", "assert")
    }

    private const val RUN_RESULT = " The result carries executionId, status (finished, error, running), durationMs, " +
            "the exception with its line when the script threw, and the newest console lines. A script still " +
            "running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output."

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
        description = "Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API) and by default " +
                "waits for it to finish. Use it for automation steps: toasts, UI actions, file work, app launches." + RUN_RESULT,
        inputSchema = JsonSchemas.objectSchema(
            properties = runProperties(
                ScriptRun.SOURCE to JsonSchemas.string("JavaScript source to execute", maxLength = ScriptRun.MAX_SOURCE_BYTES),
                ScriptRun.NAME to JsonSchemas.string(
                    "Script name shown in the AutoJs6 task list; AutoJs6 appends .js itself, a given .js suffix is dropped",
                    maxLength = ScriptRun.MAX_NAME_LENGTH,
                    default = ScriptRun.DEFAULT_NAME,
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

    val scriptRunFile: ToolSpec = ToolSpec(
        name = SCRIPT_RUN_FILE,
        title = "Run script file",
        description = "Runs a script file that already exists on the device (AutoJs6 picks the engine from the suffix) " +
                "and by default waits for it to finish." + RUN_RESULT,
        inputSchema = JsonSchemas.objectSchema(
            properties = runProperties(
                ScriptRun.PATH to JsonSchemas.string(
                    "POSIX-style path of the script file, relative to the working directory; it cannot escape it",
                    maxLength = ScriptRun.MAX_PATH_LENGTH,
                ),
            ),
            required = listOf(ScriptRun.PATH),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "execScriptFile"),
        permissions = listOf("engines", "engines.exec"),
        timeoutMs = ScriptRun.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(openWorld = true),
    )

    val scriptStop: ToolSpec = ToolSpec(
        name = SCRIPT_STOP,
        title = "Stop script",
        description = "Stops one running AutoJs6 script by the executionId that script_run, script_run_file, or script_list reported.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                ScriptStop.EXECUTION_ID to JsonSchemas.integer("Execution id of the script to stop", minimum = 0),
            ),
            required = listOf(ScriptStop.EXECUTION_ID),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "stop"),
        permissions = listOf("engines"),
        timeoutMs = 10_000L,
        hints = ToolHints(idempotent = true),
    )

    val scriptStopAll: ToolSpec = ToolSpec(
        name = SCRIPT_STOP_ALL,
        title = "Stop all scripts",
        description = "Stops every script AutoJs6 is running, including ones started on the phone, and returns how many were stopped.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "stopAll"),
        permissions = listOf("engines"),
        timeoutMs = 10_000L,
        hints = ToolHints(destructive = true, idempotent = true),
    )

    val scriptList: ToolSpec = ToolSpec(
        name = SCRIPT_LIST,
        title = "List scripts",
        description = "Lists the scripts AutoJs6 is running or starting, with executionId, name, path, working directory, state, and uptime.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "list"),
        permissions = listOf("engines"),
        timeoutMs = 10_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val consoleTail: ToolSpec = ToolSpec(
        name = CONSOLE_TAIL,
        title = "Console tail",
        description = "Returns the newest lines of the AutoJs6 console, which every script shares; optionally only entries " +
                "after sinceId or at least a level. nextSinceId in the result continues from where this call ended.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                ConsoleTail.LINES to JsonSchemas.integer(
                    "How many of the newest lines to return",
                    minimum = 1,
                    maximum = McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong(),
                    default = ConsoleTail.DEFAULT_LINES,
                ),
                ConsoleTail.SINCE_ID to JsonSchemas.integer("Only entries with an id above this one (the nextSinceId of a previous call)", minimum = 0),
                ConsoleTail.LEVEL to JsonSchemas.string("Lowest level to include", enum = ConsoleTail.LEVELS),
            ),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("console", "tail"),
        permissions = listOf("console"),
        timeoutMs = 10_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    /** Every tool in list order; `tools/list` keeps this order. */
    val all: List<ToolSpec> = listOf(devicePing, deviceInfo, scriptRun, scriptRunFile, scriptStop, scriptStopAll, scriptList, consoleTail)

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

    /** The options both run tools share, after the tool-specific leading properties. */
    private fun runProperties(vararg leading: Pair<String, JsonObject>): LinkedHashMap<String, JsonObject> =
        linkedMapOf(*leading).apply {
            put(
                ScriptRun.WORKING_DIRECTORY,
                JsonSchemas.string(
                    "POSIX-style path relative to the AutoJs6 working directory; the script cannot escape it",
                    maxLength = ScriptRun.MAX_WORKING_DIRECTORY_LENGTH,
                    default = ".",
                ),
            )
            put(ScriptRun.ARGUMENTS, JsonSchemas.primitiveMap("Values exposed to the script through engines.myEngine().execArgv"))
            put(
                ScriptRun.TIMEOUT_MS,
                JsonSchemas.integer(
                    "How long to wait for the script to finish, in milliseconds; a script still running afterwards is not killed",
                    minimum = ScriptRun.MIN_TIMEOUT_MS,
                    maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS,
                    default = ScriptRun.DEFAULT_TIMEOUT_MS,
                ),
            )
            put(ScriptRun.WAIT_FOR_COMPLETION, JsonSchemas.boolean("Wait for the script to finish before answering; false returns the executionId at once", default = true))
            put(ScriptRun.CAPTURE_CONSOLE, JsonSchemas.boolean("Include the console lines printed while the script ran", default = true))
            put(
                ScriptRun.MAX_CONSOLE_LINES,
                JsonSchemas.integer(
                    "Keep at most this many of the newest console lines",
                    minimum = 1,
                    maximum = McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong(),
                    default = ScriptRun.DEFAULT_MAX_CONSOLE_LINES,
                ),
            )
        }

    private val PRETTY = Json { prettyPrint = true; prettyPrintIndent = "  " }
}
