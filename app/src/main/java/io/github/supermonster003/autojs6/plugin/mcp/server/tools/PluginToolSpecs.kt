package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * How a self-described plugin tool (roadmap P12, decisions D33 / D34) appears in `tools/list`: its name and schema as
 * the plugin wrote them (the host already checked both), the plugin's description with a note on where it comes from,
 * annotations derived from its risk, and for image and file parameters a note on the values AutoJs6 accepts. A tool
 * with binary outputs also takes the optional [OUTPUT_PATHS] (decision D47): AutoJs6 writes every output to the working
 * directory, at the path given there or at an automatic one, and the result lists the paths.
 */
object PluginToolSpecs {

    /** The extra argument that names where outputs go; added only when the plugin's own schema does not use the name. */
    const val OUTPUT_PATHS = "outputPaths"

    /** Whether [tool] takes [OUTPUT_PATHS]. */
    fun takesOutputPaths(tool: PluginToolInfo): Boolean =
        tool.outputs.isNotEmpty() && properties(tool.inputSchema)[OUTPUT_PATHS] == null

    fun hints(tool: PluginToolInfo): ToolHints = ToolHints(
        readOnly = tool.readOnly,
        destructive = tool.risk == PluginToolInfo.RISK_DESTRUCTIVE,
        idempotent = false,
        openWorld = tool.risk == PluginToolInfo.RISK_NETWORK || tool.imageParameters.isNotEmpty(),
    )

    fun description(tool: PluginToolInfo): String = buildString {
        append(tool.description.trim())
        append(" (AutoJs6 plugin ").append(tool.provider)
        if (tool.userGranted) append(", a third-party plugin the user allowed")
        append(".)")
        if (tool.outputs.isNotEmpty()) {
            append(" Writes ").append(tool.outputs.joinToString { "${it.name} (${if (it.isDirectory) "a directory" else it.mimeType})" })
            append(" into the AutoJs6 working directory and returns the paths.")
        }
    }

    /** The plugin's input schema with the notes on image, file and output arguments. */
    fun inputSchema(tool: PluginToolInfo): JsonObject {
        val properties = properties(tool.inputSchema)
        val notedProperties = buildJsonObject {
            properties.forEach { (name, value) ->
                val property = value as? JsonObject
                val note = when (name) {
                    in tool.imageParameters -> IMAGE_NOTE
                    in tool.fileParameters -> FILE_NOTE
                    in tool.directoryParameters -> DIRECTORY_NOTE
                    else -> null
                }
                if (property == null || note == null) {
                    put(name, value)
                } else {
                    put(name, JsonObject(property + ("description" to JsonPrimitive(listOfNotNull(property.text("description"), note).joinToString(" ")))))
                }
            }
            if (takesOutputPaths(tool)) {
                putJsonObject(OUTPUT_PATHS) {
                    put("type", "object")
                    put("description", buildString {
                        append("Optional paths in the AutoJs6 working directory for the outputs, by output name (${tool.outputs.joinToString { it.name }}). ")
                        append("Outputs without a path go to plugin-outputs/${tool.shortName}/ with an automatic name. An existing file is not replaced.")
                        if (tool.outputs.any { it.isDirectory }) append(" A directory output may name an existing directory; files already in it are kept and reported as conflicts.")
                    })
                    putJsonObject("properties") {
                        tool.outputs.forEach { output ->
                            putJsonObject(output.name) {
                                put("type", "string")
                                put("description", listOfNotNull(output.description, if (output.isDirectory) "a directory path" else output.mimeType).joinToString(", "))
                            }
                        }
                    }
                    put("additionalProperties", false)
                }
            }
        }
        return JsonObject(tool.inputSchema + ("properties" to notedProperties))
    }

    /** The MCP output schema: the plugin's, unless the result must also list written outputs. */
    fun outputSchema(tool: PluginToolInfo): JsonObject? = tool.outputSchema?.takeIf { tool.outputs.isEmpty() }

    fun toSdkTool(tool: PluginToolInfo): Tool {
        val schema = inputSchema(tool)
        val hints = hints(tool)
        val title = tool.title ?: tool.name
        return Tool(
            name = tool.name,
            title = title,
            description = description(tool),
            inputSchema = ToolSchema(properties = properties(schema), required = required(schema).takeIf { it.isNotEmpty() }),
            outputSchema = outputSchema(tool)?.let { ToolSchema(properties = properties(it), required = required(it).takeIf { names -> names.isNotEmpty() }) },
            annotations = ToolAnnotations(
                title = title,
                readOnlyHint = hints.readOnly,
                destructiveHint = hints.destructive,
                idempotentHint = hints.idempotent,
                openWorldHint = hints.openWorld,
            ),
        )
    }

    /** What decides whether a listed tool changed for clients: everything [toSdkTool] shows. */
    fun signature(tool: PluginToolInfo): String = toSdkTool(tool).toString()

    private const val IMAGE_NOTE = "AutoJs6 accepts \"screen\" (a screenshot taken now) or an image path; relative paths are in the AutoJs6 working directory."

    private const val FILE_NOTE = "AutoJs6 accepts a file path; relative paths are in the AutoJs6 working directory."

    private const val DIRECTORY_NOTE = "AutoJs6 accepts a directory path, whose files it hands to the plugin; relative paths are in the AutoJs6 working directory."

    private fun properties(schema: JsonObject): JsonObject = schema["properties"] as? JsonObject ?: JsonObject(emptyMap())

    private fun required(schema: JsonObject): List<String> =
        (schema["required"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

    private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
}
