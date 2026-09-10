package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.ConsoleTail
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.ScriptStop
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Argument encoding and result shaping of the `script` group tools other than the two run tools
 * (roadmap P3.1): `script_stop` -> `engines.stop(id)`, `script_stop_all` -> `engines.stopAll({scope:
 * "host"})`, `script_list` -> `engines.list()`, `console_tail` -> `console.tail(options)`. Pure:
 * `ScriptToolsTest` covers it.
 *
 * `engines.list` and `engines.stop` carry the host-process semantics by themselves; `stopAll` needs
 * the explicit `host` scope because the plain call keeps the Node plugin's own-process meaning.
 */
object ScriptTools {

    val NO_ARGS: JsonArray = JsonArray(emptyList())

    /** `engines.stopAll({scope: "host"})`. */
    val STOP_ALL_ARGS: JsonArray = JsonArray(listOf(buildJsonObject { put("scope", "host") }))

    /** `engines.stop(executionId)`. */
    fun stopArgs(arguments: JsonObject): JsonArray =
        JsonArray(listOf(JsonPrimitive(ToolArguments.long(arguments, ScriptStop.EXECUTION_ID, -1L))))

    /** `console.tail({lines, sinceId?, level?})`; absent options stay absent so the host defaults apply. */
    fun tailArgs(arguments: JsonObject): JsonArray = JsonArray(
        listOf(
            buildJsonObject {
                put("lines", ToolArguments.long(arguments, ConsoleTail.LINES, ConsoleTail.DEFAULT_LINES))
                arguments[ConsoleTail.SINCE_ID]?.takeIf { it !is JsonNull }?.let { put("sinceId", it) }
                ToolArguments.string(arguments, ConsoleTail.LEVEL)?.let { put("level", it) }
            },
        ),
    )

    /** `{executionId, name, state, stopped}` from the host's `engines.stop` record. */
    fun shapeStop(result: JsonElement): JsonObject {
        val root = objectOf(result)
        return buildJsonObject {
            root["id"]?.let { put("executionId", it) }
            root["sourceName"]?.let { put("name", it) }
            root["state"]?.let { put("state", it) }
            put("stopped", root["stopped"] ?: JsonPrimitive(false))
        }
    }

    /** `{stopped}`: how many executions the host stopped. */
    fun shapeStopAll(result: JsonElement): JsonObject = buildJsonObject {
        put("stopped", objectOf(result)["stopped"] ?: JsonPrimitive(0))
    }

    /** `{count, executions[{executionId, name, engine, path, workingDirectory, state, startedAt, uptimeMs}]}`. */
    fun shapeList(result: JsonElement): JsonObject {
        val root = objectOf(result)
        val executions = (root["executions"] as? JsonArray)?.map { element ->
            val execution = element as? JsonObject ?: return@map element
            buildJsonObject {
                execution["id"]?.let { put("executionId", it) }
                execution["sourceName"]?.let { put("name", it) }
                execution["engineName"]?.let { put("engine", it) }
                execution["sourcePath"]?.let { put("path", it) }
                execution["workingDirectory"]?.let { put("workingDirectory", it) }
                execution["state"]?.let { put("state", it) }
                execution["startedAt"]?.let { put("startedAt", it) }
                execution["uptimeMs"]?.let { put("uptimeMs", it) }
            }
        }.orEmpty()
        return buildJsonObject {
            put("count", root["count"] ?: JsonPrimitive(executions.size))
            put("executions", JsonArray(executions))
        }
    }

    /** The host's `console.tail` record without its schema tag: `entries`, `count`, `nextSinceId`, `latestId`, `total`, `truncated`, `evicted`, ... */
    fun shapeTail(result: JsonElement): JsonObject = JsonObject(objectOf(result).filterKeys { it != "schema" })

    private fun objectOf(result: JsonElement): JsonObject = result as? JsonObject ?: buildJsonObject { put("result", result) }
}
