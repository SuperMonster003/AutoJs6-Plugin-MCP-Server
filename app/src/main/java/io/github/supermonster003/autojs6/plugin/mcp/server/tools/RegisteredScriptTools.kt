package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Registered
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * `script_catalog` and `script_registered_run` (roadmap P11 D42): the scripts a user registered
 * for models with an `@agent` header or an `agent` object in `project.json` (host
 * `docs/dev/agent-script-manifest-v1.md`), as two fixed tools, so the tool list does not grow with
 * the number of scripts.
 *
 * - `script_catalog` maps onto `agent.listScripts({query?, limit})`: the host's default scope (the
 *   working directory and its `agent/` subtree; MCP sessions configure no extra roots), each record
 *   with its path, description, parameter schema, risk and confirmation hint.
 * - `script_registered_run` takes an `id` or a `path`. An id is resolved through the catalog and must
 *   be unique (two directories may register the same id; then the path decides). The flow reads the
 *   manifest (`agent.readManifest`) and runs `agent.execRegistered(path, arguments, {expectedManifest,
 *   timeoutMs, captureConsole})`, so a registration changed after the manifest was read is refused.
 *   The host validates the arguments against the schema and waits for the script; it stops a script
 *   that runs past its timeout.
 *
 * Decided 2026-10-15 for MCP: `risk` / `confirm` are passed through to the client (the run tool is
 * annotated like `script_run`, which already runs arbitrary code); the phone shows no extra prompt.
 */
class RegisteredScriptTools(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) : ToolFlows {

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.SCRIPT_CATALOG -> catalog(spec, arguments)
        ToolCatalog.SCRIPT_REGISTERED_RUN -> run(spec, arguments)
        else -> null
    }

    private fun catalog(spec: ToolSpec, arguments: JsonObject): ToolFlow {
        ToolArguments.validate(spec, arguments)
        val query = ToolArguments.string(arguments, Registered.QUERY)?.trim()?.takeIf { it.isNotEmpty() }
        val limit = ToolArguments.long(arguments, Registered.LIMIT, Registered.DEFAULT_LIMIT)
        val options = buildJsonObject {
            query?.let { put("query", it) }
            put("limit", limit)
        }
        return ToolFlow(spec.timeoutMs) { caller ->
            val scripts = list(caller, options, spec.timeoutMs)
            val structured = buildJsonObject {
                query?.let { put(Registered.QUERY, it) }
                put("count", scripts.size)
                put("limit", limit)
                // The host returns at most `limit` records and stops discovery at its own bounds; a full page may hide more.
                put("truncated", scripts.size >= limit)
                put("scripts", JsonArray(scripts))
            }
            ToolFlowResult(structured, if (scripts.isEmpty()) NO_SCRIPTS else scripts.joinToString("\n", transform = ::line))
        }
    }

    private fun run(spec: ToolSpec, arguments: JsonObject): ToolFlow {
        ToolArguments.validate(spec, arguments)
        val id = ToolArguments.string(arguments, Registered.ID)?.trim()?.takeIf { it.isNotEmpty() }
        val path = ToolArguments.string(arguments, Registered.PATH)?.trim()?.takeIf { it.isNotEmpty() }
        if ((id == null) == (path == null)) invalid("give exactly one of id or path")
        if (path != null && ('\u0000' in path || path.split('/', '\\').any { it == ".." })) invalid("path must not contain .. segments or NUL")
        val values = (arguments[Registered.ARGUMENTS] as? JsonObject) ?: JsonObject(emptyMap())
        values.entries.firstOrNull { (_, value) -> value !is JsonPrimitive || value is JsonNull }?.let { (name, _) ->
            invalid("arguments.$name must be a string, number or boolean; registered scripts take no null, object or array values")
        }
        if (values.toString().toByteArray(Charsets.UTF_8).size > Registered.MAX_ARGUMENTS_BYTES) {
            invalid("arguments exceed ${Registered.MAX_ARGUMENTS_BYTES} bytes of JSON")
        }
        val requestedTimeout = (arguments[Registered.TIMEOUT_MS] as? JsonPrimitive)?.let { ToolArguments.long(arguments, Registered.TIMEOUT_MS, 0L) }
        return ToolFlow(requestedTimeout ?: spec.maxTimeoutMs) { caller ->
            val startedAt = clock()
            val target = path ?: resolve(caller, id!!, spec.timeoutMs)
            val manifest = when (val outcome = caller.call("agent", "readManifest", JsonArray(listOf(JsonPrimitive(target))), Registered.READ_TIMEOUT_MS, CATALOG_PERMISSIONS)) {
                is BridgeOutcome.Ok -> outcome.result as? JsonObject ?: ImageInputs.badHost("agent.readManifest returned no manifest")
                is BridgeOutcome.Failed -> throw ToolFailureException(mapFailure(outcome.failure, id ?: target))
            }
            val manifestTimeout = (manifest["timeoutMs"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 } ?: Registered.DEFAULT_SCRIPT_TIMEOUT_MS
            val timeoutMs = minOf(requestedTimeout ?: manifestTimeout, manifestTimeout)
            val options = buildJsonObject {
                put("expectedManifest", manifest)
                put("timeoutMs", timeoutMs)
                put("captureConsole", true)
            }
            val result = when (val outcome = caller.call("agent", "execRegistered", JsonArray(listOf(JsonPrimitive(target), values, options)),
                timeoutMs + Registered.EXEC_GRACE_MS, spec.permissions)) {
                is BridgeOutcome.Ok -> outcome.result as? JsonObject ?: ImageInputs.badHost("agent.execRegistered returned no result")
                is BridgeOutcome.Failed -> throw ToolFailureException(mapFailure(outcome.failure, id ?: target))
            }
            shape(manifest, target, result, clock() - startedAt)
        }
    }

    private suspend fun list(caller: BridgeCaller, options: JsonObject, timeoutMs: Long): List<JsonObject> {
        val result = caller.callOrThrow("agent", "listScripts", JsonArray(listOf(options)), timeoutMs, CATALOG_PERMISSIONS)
        val array = result as? JsonArray ?: ImageInputs.badHost("agent.listScripts returned no list")
        return array.mapNotNull { it as? JsonObject }
    }

    /** The path of the one registration with this exact [id]; none or several are argument errors. */
    private suspend fun resolve(caller: BridgeCaller, id: String, timeoutMs: Long): String {
        val options = buildJsonObject { put("query", id); put("limit", Registered.MAX_LIMIT) }
        val matches = list(caller, options, timeoutMs).filter { it.string("id") == id }
        return when (matches.size) {
            0 -> throw ToolArgumentException(ToolFailure(ToolErrorCodes.INVALID_ARGUMENTS,
                "${ToolCatalog.SCRIPT_REGISTERED_RUN}: no registered script has the id $id", HINT_CATALOG))
            1 -> matches.single().string("path") ?: ImageInputs.badHost("agent.listScripts returned a record without a path")
            else -> throw ToolArgumentException(ToolFailure(ToolErrorCodes.INVALID_ARGUMENTS,
                "${ToolCatalog.SCRIPT_REGISTERED_RUN}: ${matches.size} scripts register the id $id (${matches.mapNotNull { it.string("path") }.joinToString(", ")}); pass path instead",
                HINT_CATALOG))
        }
    }

    private fun shape(manifest: JsonObject, path: String, result: JsonObject, durationMs: Long): ToolFlowResult {
        val outcome = result.string("outcome") ?: "unknown"
        val status = STATUS[outcome] ?: outcome
        val error = result["error"] as? JsonObject
        val reported = (result["resultReported"] as? JsonPrimitive)?.booleanOrNull == true
        val console = (result["consoleTail"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
        val id = manifest.string("id") ?: path
        val structured = buildJsonObject {
            put("id", id)
            put("path", manifest.string("path") ?: path)
            manifest.string("kind")?.let { put("kind", it) }
            manifest.string("risk")?.let { put("risk", it) }
            manifest.string("confirm")?.let { put("confirm", it) }
            put("status", status)
            put("outcome", outcome)
            result["executionId"]?.takeUnless { it is JsonNull }?.let { put("executionId", it) }
            error?.let { put("error", it) }
            put("resultReported", reported)
            if (reported) put("result", result["result"] ?: JsonNull)
            put("console", JsonArray(console.map { JsonPrimitive(it) }))
            put("durationMs", durationMs.coerceAtLeast(0))
        }
        val text = buildString {
            append(id).append(": ").append(status)
            error?.string("message")?.let { append(" (").append(it).append(")") }
            if (reported) append("\nresult: ").append(result["result"] ?: JsonNull)
            if (console.isNotEmpty()) append("\nconsole:\n").append(console.joinToString("\n"))
        }
        return ToolFlowResult(structured, text)
    }

    /** The host answers catalog failures with the bare catalog code as the message; say what it means. */
    private fun mapFailure(failure: ToolFailure, target: String): ToolFailure = when (failure.message.substringAfter(": ").trim()) {
        "SCRIPT_NOT_REGISTERED" -> failure.copy(code = ToolErrorCodes.INVALID_ARGUMENTS,
            message = "${ToolCatalog.SCRIPT_REGISTERED_RUN}: $target is not a registered script, or its registration changed while the call ran", hint = HINT_CATALOG)
        "TOOL_ARGUMENTS_INVALID" -> failure.copy(code = ToolErrorCodes.INVALID_ARGUMENTS,
            message = "${ToolCatalog.SCRIPT_REGISTERED_RUN}: the arguments do not match the parameters $target registers " +
                    "(types, enumerations, bounds, required names, no extra names, at most 16 KiB)", hint = HINT_CATALOG)
        "CAPABILITY_DENIED" -> failure.copy(code = ToolErrorCodes.CAPABILITY_DENIED,
            message = "${ToolCatalog.SCRIPT_REGISTERED_RUN}: $target lies outside the directories AutoJs6 scans for registered scripts", hint = HINT_CATALOG)
        else -> failure
    }

    private fun line(script: JsonObject): String = buildString {
        append(script.string("id") ?: "?")
        script.string("description")?.let { append(": ").append(it.lines().first()) }
        val parameters = (script["parameters"] as? JsonObject)?.let(::parameterSummary).orEmpty()
        append(" (risk ").append(script.string("risk") ?: "normal").append(", confirm ").append(script.string("confirm") ?: "never")
        if (parameters.isNotEmpty()) append("; parameters ").append(parameters)
        script.string("path")?.let { append("; path ").append(it) }
        append(")")
    }

    /** `name*` for required, `name=default` for a default, `name` otherwise. */
    private fun parameterSummary(schema: JsonObject): String {
        val properties = schema["properties"] as? JsonObject ?: return ""
        val required = (schema["required"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty().toSet()
        return properties.entries.joinToString(", ") { (name, property) ->
            val default = (property as? JsonObject)?.get("default")
            when {
                name in required -> "$name*"
                default != null -> "$name=${(default as? JsonPrimitive)?.contentOrNull ?: default}"
                else -> name
            }
        }
    }

    private fun invalid(message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments("${ToolCatalog.SCRIPT_REGISTERED_RUN}: $message"))

    private companion object {

        val CATALOG_PERMISSIONS = listOf("agent")

        const val NO_SCRIPTS = "(no registered scripts; a script registers itself with an @agent JSDoc header or an agent object in project.json)"
        const val HINT_CATALOG = "call script_catalog for the registered ids, paths and parameters"

        /** The host's execution outcomes as the run statuses of the `script` group. */
        val STATUS = mapOf("success" to "finished", "exception" to "error", "stopped" to "stopped", "timeout" to "timeout")

        fun JsonElement.string(key: String): String? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
}
