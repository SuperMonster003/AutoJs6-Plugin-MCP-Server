package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.ScriptRun
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract

/**
 * Maps `script_run` arguments onto the host's `engines.execScript(name, source, options)` and
 * shapes the answer (roadmap P2.3). Pure: `ScriptRunToolTest` covers both directions.
 *
 * The host starts the script within `options.timeoutMs` (its own ceiling is 60 s) and, when
 * `waitMs` is set, waits that long for it to finish while capturing the console. The bridge
 * timeout must outlast the wait, so it is the wait plus a grace period, and the wait itself is
 * capped so the whole call fits the contract ceiling.
 */
object ScriptRunTool {

    /** What one call sends to the host and how its answer is trimmed. */
    data class Prepared(
        val name: String,
        val args: JsonArray,
        val waitMs: Long,
        val bridgeTimeoutMs: Long,
        val maxConsoleLines: Int,
    )

    /** Added to the wait before the bridge gives up, so a script that just finished still answers. */
    const val BRIDGE_GRACE_MS = 10_000L

    /** The longest wait a call may ask for; `timeoutMs` above it is clamped silently. */
    const val MAX_WAIT_MS = McpServerContract.MAX_TOOL_TIMEOUT_MS - BRIDGE_GRACE_MS

    fun prepare(arguments: JsonObject): Prepared {
        val source = ToolArguments.string(arguments, ScriptRun.SOURCE).orEmpty()
        val name = normalizeName(ToolArguments.string(arguments, ScriptRun.NAME))
        val timeoutMs = ToolArguments.long(arguments, ScriptRun.TIMEOUT_MS, ScriptRun.DEFAULT_TIMEOUT_MS)
            .coerceIn(ScriptRun.MIN_TIMEOUT_MS, McpServerContract.MAX_TOOL_TIMEOUT_MS)
        val waitForCompletion = ToolArguments.boolean(arguments, ScriptRun.WAIT_FOR_COMPLETION, true)
        val captureConsole = ToolArguments.boolean(arguments, ScriptRun.CAPTURE_CONSOLE, true)
        val maxConsoleLines = ToolArguments.long(arguments, ScriptRun.MAX_CONSOLE_LINES, ScriptRun.DEFAULT_MAX_CONSOLE_LINES)
            .coerceIn(1L, McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong())
            .toInt()
        val startTimeoutMs = minOf(timeoutMs, ScriptRun.MAX_START_TIMEOUT_MS)
        val waitMs = if (waitForCompletion) minOf(timeoutMs, MAX_WAIT_MS) else 0L
        val options = buildJsonObject {
            put("timeoutMs", startTimeoutMs)
            put("waitMs", waitMs)
            put("captureConsole", captureConsole)
            ToolArguments.objectOrNull(arguments, ScriptRun.ARGUMENTS)?.takeIf { it.isNotEmpty() }?.let { put("arguments", it) }
            ToolArguments.string(arguments, ScriptRun.WORKING_DIRECTORY)?.trim()?.takeIf { it.isNotEmpty() && it != "." }?.let { put("cwd", it) }
        }
        return Prepared(
            name = name,
            args = JsonArray(listOf(JsonPrimitive(name), JsonPrimitive(source), options)),
            waitMs = waitMs,
            bridgeTimeoutMs = maxOf(waitMs, startTimeoutMs) + BRIDGE_GRACE_MS,
            maxConsoleLines = maxConsoleLines,
        )
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
     * Keeps the newest [maxConsoleLines] console entries of the host result; `console.returned`
     * says how many are left and `console.truncated` flips when lines were dropped here.
     */
    fun shape(result: JsonElement, maxConsoleLines: Int): JsonObject {
        val root = result as? JsonObject ?: return buildJsonObject { put("result", result) }
        val console = root["console"] as? JsonObject ?: return root
        val entries = console["entries"] as? JsonArray ?: return root
        if (entries.size <= maxConsoleLines) return root
        val kept = entries.takeLast(maxConsoleLines)
        val trimmed = JsonObject(
            console + mapOf(
                "entries" to JsonArray(kept),
                "returned" to JsonPrimitive(kept.size),
                "truncated" to JsonPrimitive(true),
            ),
        )
        return JsonObject(root + ("console" to trimmed))
    }
}
