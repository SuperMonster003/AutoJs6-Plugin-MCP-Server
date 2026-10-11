package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The two result shapes every tool answers with (appendix A.1): a success carries the JSON
 * result both as text and as `structuredContent` (a tool whose natural form is plain text, such
 * as the compact node tree of `ui_dump`, sends that text instead of the JSON), plus an image
 * block for `screen_capture`; a failure is
 * `isError: true` with the text `code: message (hint)` and the same fields under
 * `structuredContent.error`, except for tools that declare an `outputSchema`: their errors carry
 * the text only, since clients validate any `structuredContent` against that schema, errors
 * included (the TypeScript SDK's `Client.callTool` does).
 */
object ToolResults {

    fun success(structured: JsonObject, text: String = structured.toString(), image: ImageContent? = null): CallToolResult = CallToolResult(
        content = listOfNotNull(TextContent(text = text), image),
        structuredContent = structured,
        isError = false,
    )

    fun failure(failure: ToolFailure): CallToolResult = CallToolResult(
        content = listOf(TextContent(text = failure.render())),
        structuredContent = failureStructure(failure),
        isError = true,
    )

    /** [result] as [spec] may send it: without `structuredContent` for an error of a tool with an output schema. */
    fun forSpec(spec: ToolSpec, result: CallToolResult): CallToolResult =
        if (spec.outputSchema != null && result.isError == true && result.structuredContent != null) result.copy(structuredContent = null) else result

    /**
     * The JSON-RPC `result` object of a failure, for the gates that answer without the SDK;
     * [structured] is false for a tool with an output schema.
     */
    fun failureJson(failure: ToolFailure, structured: Boolean = true): JsonObject = buildJsonObject {
        put("content", JsonArray(listOf(buildJsonObject {
            put("type", "text")
            put("text", failure.render())
        })))
        put("isError", true)
        if (structured) put("structuredContent", failureStructure(failure))
    }

    private fun failureStructure(failure: ToolFailure): JsonObject = buildJsonObject { put("error", failure.toJson()) }
}
