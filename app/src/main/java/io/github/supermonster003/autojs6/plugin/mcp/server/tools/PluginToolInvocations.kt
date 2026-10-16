package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One `plugin_tools.invoke` request, ready for the host bridge. */
data class PluginToolCall(
    val args: JsonArray,
    val permissions: List<String>,
    val timeoutMs: Long,
    /** Output name to the working-directory path AutoJs6 writes it to. */
    val outputs: Map<String, String>,
)

/**
 * Turns an MCP call of a self-described tool (roadmap P12) into a `plugin_tools.invoke` request and its answer back into
 * an MCP result. The host checks the arguments against the plugin's schema and resolves image and file arguments; this
 * side adds what only the MCP session knows: the permission tokens the call needs (`plugin_tools.user_granted` for a
 * user-granted plugin, `screen_capture` for `"screen"`, `files` and, with the shared storage switch, `files.shared_read`
 * for paths, `files.write` for outputs), the output paths (decision D47) and a time limit above the tool's own.
 */
object PluginToolInvocations {

    const val MODULE = "plugin_tools"
    const val INVOKE = "invoke"
    const val TOKEN = "plugin_tools"
    const val TOKEN_USER_GRANTED = "plugin_tools.user_granted"
    const val TOKEN_SCREEN = "screen_capture"
    const val TOKEN_FILES = "files"
    const val TOKEN_FILES_WRITE = "files.write"

    /** Where outputs without an explicit path go, under the working directory. */
    const val OUTPUT_DIRECTORY = "plugin-outputs"

    /** Image outputs up to this size come back as an image block too (decision D47). */
    const val PREVIEW_MAX_BYTES = 1024L * 1024L

    /** The bridge request outlives the tool's own limit by this much, so the tool's limit decides. */
    private const val TIMEOUT_MARGIN_MS = 2_000L

    fun prepare(tool: PluginToolInfo, arguments: JsonObject, access: PathAccess, now: Long, sequence: Int = 0): PluginToolCall {
        val permissions = linkedSetOf(TOKEN)
        if (tool.userGranted) permissions += TOKEN_USER_GRANTED
        val forwarded = LinkedHashMap<String, JsonElement>()
        arguments.forEach { (name, value) ->
            if (name == PluginToolSpecs.OUTPUT_PATHS && PluginToolSpecs.takesOutputPaths(tool)) return@forEach
            val path = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            forwarded[name] = when {
                path == null || (name !in tool.imageParameters && name !in tool.fileParameters) -> value
                name in tool.imageParameters && path.trim() == ToolCatalog.Images.SCREEN -> {
                    permissions += TOKEN_SCREEN
                    JsonPrimitive(ToolCatalog.Images.SCREEN)
                }
                else -> {
                    val target = access.readable(tool.name, name, path)
                    target.permissions(listOf(TOKEN_FILES)).forEach { permissions += it }
                    JsonPrimitive(target.path)
                }
            }
        }
        val outputs = outputPaths(tool, arguments, now, sequence)
        if (outputs.isNotEmpty()) permissions += TOKEN_FILES_WRITE
        val args = buildJsonArray {
            add(tool.name)
            add(JsonObject(forwarded))
            if (outputs.isNotEmpty()) {
                add(buildJsonObject {
                    putJsonObject("outputs") { outputs.forEach { (name, path) -> put(name, path) } }
                })
            }
        }
        return PluginToolCall(args, permissions.toList(), tool.timeoutMs + TIMEOUT_MARGIN_MS, outputs)
    }

    /** The answer of `plugin_tools.invoke` as the MCP result's structured content and text. */
    fun shape(tool: PluginToolInfo, answer: JsonElement?): Pair<JsonObject, String> {
        val root = answer as? JsonObject ?: throw ToolFailureException(ToolFailure.internal("${tool.name}: AutoJs6 returned an invalid result"))
        val result = root["result"] ?: JsonPrimitive(null as String?)
        val text = (root["text"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        val written = (root["outputs"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val missing = (root["missingOutputs"] as? JsonArray).orEmpty()
        if (tool.outputs.isEmpty()) {
            val structured = result as? JsonObject ?: buildJsonObject { put("result", result) }
            return structured to (text ?: structured.toString())
        }
        val structured = buildJsonObject {
            put("result", result)
            putJsonArray("outputs") { written.forEach { add(it) } }
            if (missing.isNotEmpty()) put("missingOutputs", JsonArray(missing))
        }
        val summary = buildString {
            append(text ?: result.toString())
            written.forEach { output ->
                append("\nWrote ").append(output.text("name")).append(" to ").append(output.text("path"))
                (output["bytes"] as? JsonPrimitive)?.longOrNull?.let { append(" (").append(it).append(" bytes)") }
            }
            if (missing.isNotEmpty()) append("\nThe plugin returned no ").append(missing.joinToString { (it as? JsonPrimitive)?.contentOrNull.orEmpty() })
        }
        return structured to summary
    }

    /** The written image output to show as an image block: the first PNG, JPEG or WebP within [PREVIEW_MAX_BYTES]. */
    fun previewCandidate(structured: JsonObject): Pair<String, String>? {
        val outputs = structured["outputs"] as? JsonArray ?: return null
        return outputs.mapNotNull { it as? JsonObject }.firstNotNullOfOrNull { output ->
            val mime = output.text("mimeType") ?: return@firstNotNullOfOrNull null
            val path = output.text("path") ?: return@firstNotNullOfOrNull null
            val bytes = (output["bytes"] as? JsonPrimitive)?.longOrNull ?: return@firstNotNullOfOrNull null
            (path to mime).takeIf { mime in PREVIEW_TYPES && bytes in 1..PREVIEW_MAX_BYTES }
        }
    }

    private fun outputPaths(tool: PluginToolInfo, arguments: JsonObject, now: Long, sequence: Int): Map<String, String> {
        if (tool.outputs.isEmpty()) return emptyMap()
        val requested = if (PluginToolSpecs.takesOutputPaths(tool)) arguments[PluginToolSpecs.OUTPUT_PATHS] else null
        val given = when (requested) {
            null -> emptyMap()
            is JsonObject -> requested.mapValues { (name, value) ->
                if (tool.outputs.none { it.name == name }) invalid(tool, "${PluginToolSpecs.OUTPUT_PATHS}.$name is not an output of this tool (${tool.outputs.joinToString { it.name }})")
                val raw = (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: invalid(tool, "${PluginToolSpecs.OUTPUT_PATHS}.$name must be a string")
                WorkspacePath.normalize(raw, allowRoot = false)
            }
            else -> invalid(tool, "${PluginToolSpecs.OUTPUT_PATHS} must be an object of output names to paths")
        }
        val stamp = STAMP.get()!!.format(Date(now))
        return tool.outputs.associate { output ->
            val suffix = if (sequence > 0) "-$sequence" else ""
            output.name to (given[output.name] ?: "$OUTPUT_DIRECTORY/${tool.shortName}/${tool.tool}-$stamp$suffix${if (tool.outputs.size > 1) "-${output.name}" else ""}.${extension(output.mimeType)}")
        }
    }

    private fun extension(mimeType: String): String = when (mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)) {
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "text/plain" -> "txt"
        "application/json" -> "json"
        "text/csv" -> "csv"
        "application/pdf" -> "pdf"
        "application/zip" -> "zip"
        else -> "bin"
    }

    private fun invalid(tool: PluginToolInfo, message: String): Nothing =
        throw ToolArgumentException(ToolFailure.invalidArguments("${tool.name}: $message"))

    private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())

    private val PREVIEW_TYPES = setOf("image/png", "image/jpeg", "image/webp")

    private val STAMP = ThreadLocal.withInitial {
        SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).apply { timeZone = TimeZone.getDefault() }
    }
}
