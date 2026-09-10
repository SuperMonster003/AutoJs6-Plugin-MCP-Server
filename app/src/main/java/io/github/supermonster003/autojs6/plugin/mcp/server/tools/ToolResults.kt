package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The two result shapes every tool answers with (appendix A.1): a success carries the JSON
 * result both as text and as `structuredContent`; a failure is `isError: true` with the text
 * `code: message (hint)` and the same fields under `structuredContent.error`.
 */
object ToolResults {

    fun success(structured: JsonObject): CallToolResult = CallToolResult(
        content = listOf(TextContent(text = structured.toString())),
        structuredContent = structured,
        isError = false,
    )

    fun failure(failure: ToolFailure): CallToolResult = CallToolResult(
        content = listOf(TextContent(text = failure.render())),
        structuredContent = failureStructure(failure),
        isError = true,
    )

    /** The JSON-RPC `result` object of a failure, for the tool gate that answers without the SDK. */
    fun failureJson(failure: ToolFailure): JsonObject = buildJsonObject {
        put("content", JsonArray(listOf(buildJsonObject {
            put("type", "text")
            put("text", failure.render())
        })))
        put("isError", true)
        put("structuredContent", failureStructure(failure))
    }

    private fun failureStructure(failure: ToolFailure): JsonObject = buildJsonObject { put("error", failure.toJson()) }
}
