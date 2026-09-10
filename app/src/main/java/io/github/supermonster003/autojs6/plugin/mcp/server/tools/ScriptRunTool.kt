package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.ScriptRun
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract

/**
 * Maps `script_run` / `script_run_file` arguments onto the host's `engines.execScript(name, source,
 * options)` / `engines.execScriptFile(path, options)` and shapes the answer into the P3.1 result:
 * `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, `exception` (message and
 * line), the newest `console` lines. Pure: `ScriptRunToolTest` covers both directions.
 *
 * The host starts the script within `options.timeoutMs` (its own ceiling is 60 s) and, when
 * `waitMs` is set, waits that long for it to finish while capturing the console. The bridge
 * timeout must outlast the wait, so it is the wait plus a grace period, and the wait itself is
 * capped so the whole call fits the contract ceiling. A script still running after the wait is
 * not killed: the result says `status = running` and names `script_stop`.
 */
object ScriptRunTool {

    /** What one call sends to the host and how its answer is shaped. */
    data class Prepared(
        val args: JsonArray,
        val waitMs: Long,
        val captureConsole: Boolean,
        val bridgeTimeoutMs: Long,
        val maxConsoleLines: Int,
    )

    const val STATUS_FINISHED = "finished"
    const val STATUS_ERROR = "error"
    const val STATUS_RUNNING = "running"

    /** Added to the wait before the bridge gives up, so a script that just finished still answers. */
    const val BRIDGE_GRACE_MS = 10_000L

    /** The longest wait a call may ask for; `timeoutMs` above it is clamped silently. */
    const val MAX_WAIT_MS = McpServerContract.MAX_TOOL_TIMEOUT_MS - BRIDGE_GRACE_MS

    /** `script_run`: `engines.execScript(name, source, options)`. */
    fun prepare(arguments: JsonObject): Prepared {
        val source = ToolArguments.string(arguments, ScriptRun.SOURCE).orEmpty()
        val name = normalizeName(ToolArguments.string(arguments, ScriptRun.NAME))
        val options = options(arguments)
        return prepared(JsonArray(listOf(JsonPrimitive(name), JsonPrimitive(source), options.json)), options)
    }

    /** `script_run_file`: `engines.execScriptFile(path, options)`; the host keeps the path inside the working directory. */
    fun prepareFile(arguments: JsonObject): Prepared {
        val path = ToolArguments.string(arguments, ScriptRun.PATH).orEmpty().trim()
        if (path.isEmpty()) throw ToolArgumentException(ToolFailure.invalidArguments("${ToolCatalog.SCRIPT_RUN_FILE}: path must not be blank"))
        val options = options(arguments)
        return prepared(JsonArray(listOf(JsonPrimitive(path), options.json)), options)
    }

    /**
     * The task name AutoJs6 shows. The host wraps the source in a `StringScriptSource`, which is
     * JavaScript by construction and appends `.js` to the name it displays, so a suffix given by
     * the client is dropped rather than doubled (`demo.js.js`).
     */
    fun normalizeName(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        val stripped = if (trimmed.endsWith(".js", ignoreCase = true)) trimmed.dropLast(3).trimEnd() else trimmed
        return stripped.ifEmpty { ScriptRun.DEFAULT_NAME }
    }

    /**
     * The P3.1 result. The host answers with its execution record (`id`, `sourceName`, `engineName`,
     * `cwd`, ...) plus `outcome` / `finished` / `waitedMs` / `error` when it waited or captured the
     * console, and `console.entries` for the captured window. `status` is `finished` for a
     * successful completion, `error` when the script threw (with `exception.message` and, when
     * Rhino appended `(<file>#<line>)`, `exception.line`), else `running`; `durationMs` is the
     * time between the start and the completion, or the time waited. The console is trimmed to the
     * newest [Prepared.maxConsoleLines] entries with `consoleCount` and `consoleTruncated`.
     */
    fun shape(result: JsonElement, prepared: Prepared): JsonObject {
        val root = result as? JsonObject ?: return buildJsonObject {
            put("status", STATUS_RUNNING)
            put("result", result)
        }
        val outcome = (root["outcome"] as? JsonPrimitive)?.contentOrNull
        val finished = (root["finished"] as? JsonPrimitive)?.booleanOrNull ?: false
        val status = when {
            outcome == "exception" -> STATUS_ERROR
            outcome == "success" && finished -> STATUS_FINISHED
            else -> STATUS_RUNNING
        }
        val executionId = (root["id"] as? JsonPrimitive)?.contentOrNull
        return buildJsonObject {
            root["id"]?.let { put("executionId", it) }
            root["sourceName"]?.let { put("name", it) }
            root["engineName"]?.let { put("engine", it) }
            root["cwd"]?.let { put("workingDirectory", it) }
            put("status", status)
            put("waited", prepared.waitMs > 0L)
            put("waitMs", prepared.waitMs)
            root["waitedMs"]?.let { put("durationMs", it) }
            if (status == STATUS_ERROR) put("exception", exceptionOf((root["error"] as? JsonPrimitive)?.contentOrNull))
            (root["console"] as? JsonObject)?.let { console ->
                val entries = console["entries"] as? JsonArray ?: JsonArray(emptyList())
                val kept = entries.takeLast(prepared.maxConsoleLines)
                put("console", JsonArray(kept))
                put("consoleCount", (console["count"] as? JsonPrimitive)?.longOrNull ?: entries.size.toLong())
                put("consoleTruncated", kept.size < entries.size || ((console["truncated"] as? JsonPrimitive)?.booleanOrNull ?: false))
            }
            if (status == STATUS_RUNNING) {
                val id = executionId ?: "?"
                put(
                    "hint",
                    if (prepared.waitMs > 0L) {
                        "still running after ${prepared.waitMs} ms; script_stop with executionId $id stops it, script_list shows it, console_tail follows its output"
                    } else {
                        "started without waiting; script_list shows it, script_stop with executionId $id stops it, console_tail follows its output"
                    },
                )
            }
        }
    }

    /** `{message, line?}` from the host's error text; Rhino ends it with `(<file>#<line>)`. */
    fun exceptionOf(message: String?): JsonObject = buildJsonObject {
        put("message", message ?: "the script threw")
        LINE_SUFFIX.find(message.orEmpty())?.groupValues?.get(1)?.toIntOrNull()?.let { put("line", it) }
    }

    private class Options(val json: JsonObject, val waitMs: Long, val startTimeoutMs: Long, val captureConsole: Boolean, val maxConsoleLines: Int)

    private fun options(arguments: JsonObject): Options {
        val timeoutMs = ToolArguments.long(arguments, ScriptRun.TIMEOUT_MS, ScriptRun.DEFAULT_TIMEOUT_MS)
            .coerceIn(ScriptRun.MIN_TIMEOUT_MS, McpServerContract.MAX_TOOL_TIMEOUT_MS)
        val waitForCompletion = ToolArguments.boolean(arguments, ScriptRun.WAIT_FOR_COMPLETION, true)
        val captureConsole = ToolArguments.boolean(arguments, ScriptRun.CAPTURE_CONSOLE, true)
        val maxConsoleLines = ToolArguments.long(arguments, ScriptRun.MAX_CONSOLE_LINES, ScriptRun.DEFAULT_MAX_CONSOLE_LINES)
            .coerceIn(1L, McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong())
            .toInt()
        val startTimeoutMs = minOf(timeoutMs, ScriptRun.MAX_START_TIMEOUT_MS)
        val waitMs = if (waitForCompletion) minOf(timeoutMs, MAX_WAIT_MS) else 0L
        val json = buildJsonObject {
            put("timeoutMs", startTimeoutMs)
            put("waitMs", waitMs)
            put("captureConsole", captureConsole)
            ToolArguments.objectOrNull(arguments, ScriptRun.ARGUMENTS)?.takeIf { it.isNotEmpty() }?.let { put("arguments", it) }
            ToolArguments.string(arguments, ScriptRun.WORKING_DIRECTORY)?.trim()?.takeIf { it.isNotEmpty() && it != "." }?.let { put("cwd", it) }
        }
        return Options(json, waitMs, startTimeoutMs, captureConsole, maxConsoleLines)
    }

    private fun prepared(args: JsonArray, options: Options): Prepared = Prepared(
        args = args,
        waitMs = options.waitMs,
        captureConsole = options.captureConsole,
        bridgeTimeoutMs = maxOf(options.waitMs, options.startTimeoutMs) + BRIDGE_GRACE_MS,
        maxConsoleLines = options.maxConsoleLines,
    )

    private val LINE_SUFFIX = Regex("#(\\d+)\\)?\\s*$")
}
