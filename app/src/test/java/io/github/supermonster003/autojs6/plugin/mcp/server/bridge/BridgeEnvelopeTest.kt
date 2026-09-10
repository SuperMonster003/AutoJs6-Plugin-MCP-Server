package io.github.supermonster003.autojs6.plugin.mcp.server.bridge

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeEnvelopeTest {

    @Test
    fun `request JSON mirrors the host envelope`() {
        val request = BridgeRequest("mcp-7", "engines", "execScript", JsonArray(listOf(JsonPrimitive("a.js"))), 1_500L, listOf("engines", "engines.exec"))

        val json = Json.parseToJsonElement(request.toJson()).jsonObject

        assertEquals(setOf("id", "module", "method", "args", "timeoutMs", "permissions"), json.keys)
        assertEquals("mcp-7", json["id"]?.jsonPrimitive?.content)
        assertEquals("engines.execScript", request.key)
        assertEquals(1, json["args"]?.jsonArray?.size)
        assertEquals(1500L, json["timeoutMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(listOf("engines", "engines.exec"), json["permissions"]?.jsonArray?.map { it.jsonPrimitive.content })
    }

    @Test
    fun `responses parse into success and failure`() {
        val success = BridgeResponse.parse("""{"id":"mcp-1","ok":true,"result":{"a":1}}""", true, null) as BridgeResponse.Success
        assertEquals("mcp-1", success.id)
        assertEquals(1, success.result.jsonObject["a"]?.jsonPrimitive?.content?.toInt())

        val failure = BridgeResponse.parse(
            """{"id":"mcp-2","ok":false,"error":{"name":"Error","message":"denied","code":"c","category":"capability-denied","module":"shell","method":"exec","missingCapabilities":["shell"]}}""",
            false,
            "denied",
        ) as BridgeResponse.Failure
        assertEquals("mcp-2", failure.id)
        assertEquals(BridgeError.CATEGORY_CAPABILITY_DENIED, failure.error.category)
        assertEquals("shell", failure.error.module)
        assertEquals(listOf("shell"), failure.error.missingCapabilities)
    }

    @Test
    fun `the ok flag of the bundle wins over the document and numeric ids survive`() {
        val failure = BridgeResponse.parse("""{"id":42,"ok":true,"result":{}}""", false, "rejected later") as BridgeResponse.Failure
        assertEquals("42", failure.id)
        assertEquals("rejected later", failure.error.message)

        val success = BridgeResponse.parse("""{"id":"x","ok":false}""", true, null) as BridgeResponse.Success
        assertEquals(JsonNull, success.result)
    }

    @Test
    fun `unreadable or partial documents still describe the failure`() {
        val malformed = BridgeResponse.parse("{{{", null, "broken") as BridgeResponse.Failure
        assertNull(malformed.id)
        assertEquals("broken", malformed.error.message)
        assertEquals(BridgeError.CODE_PLUGIN_MALFORMED, malformed.error.code)

        val silent = BridgeResponse.parse(null, null, null) as BridgeResponse.Failure
        assertEquals("the host sent no readable response", silent.error.message)

        val partial = BridgeError.fromJson(Json.parseToJsonElement("""{"category":"timeout"}"""))
        assertEquals("Error", partial.name)
        assertEquals(BridgeError.CATEGORY_TIMEOUT, partial.category)
        assertEquals("the host reported an error without a message", partial.message)
        assertTrue(partial.missingCapabilities.isEmpty())
    }

    @Test
    fun `failures render as code message and hint`() {
        val plain = ToolFailure(ToolErrorCodes.HOST_ERROR, "it broke")
        assertEquals("HOST_ERROR: it broke", plain.render())

        val hinted = ToolFailure.hostUnavailable()
        assertEquals("HOST_UNAVAILABLE: AutoJs6 is not connected to the MCP server (${ToolFailure.HINT_HOST})", hinted.render())

        val json = ToolFailure.fromBridge(BridgeError("RangeError", "too big", "limit", BridgeError.CATEGORY_RESOURCE_LIMIT, "files", "read")).toJson()
        assertEquals(ToolErrorCodes.LIMIT_EXCEEDED, json["code"]?.jsonPrimitive?.content)
        assertEquals("RangeError: too big", json["message"]?.jsonPrimitive?.content)
        assertEquals(ToolFailure.HINT_LIMIT, json["hint"]?.jsonPrimitive?.content)
        assertEquals("limit", json["hostCode"]?.jsonPrimitive?.content)
        assertEquals("files", json["module"]?.jsonPrimitive?.content)
        assertEquals("read", json["method"]?.jsonPrimitive?.content)

        val disabled = ToolFailure.toolDisabled("shell_exec", "shell")
        assertEquals(ToolErrorCodes.TOOL_DISABLED, disabled.code)
        assertTrue(disabled.render().startsWith("TOOL_DISABLED: tool shell_exec is switched off by the shell group ("))
    }
}
