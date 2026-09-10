package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ToolArgumentsTest {

    private val spec = ToolSpec(
        name = "demo_tool",
        title = "Demo",
        description = "demo",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                "text" to JsonSchemas.string("text", maxLength = 5, enum = listOf("abc", "de")),
                "count" to JsonSchemas.integer("count", minimum = 1, maximum = 10),
                "ratio" to JsonSchemas.integer("ratio"),
                "flag" to JsonSchemas.boolean("flag"),
                "values" to JsonSchemas.primitiveMap("values"),
            ),
            required = listOf("text"),
        ),
        group = ToolGroup.DEVICE,
    )

    private fun arguments(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun rejected(json: String): String = try {
        ToolArguments.validate(spec, arguments(json))
        fail("expected $json to be rejected")
        ""
    } catch (e: ToolArgumentException) {
        assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, e.failure.code)
        e.failure.message
    }

    @Test
    fun `valid arguments pass through untouched`() {
        val values = arguments("""{"text":"abc","count":3,"flag":true,"values":{"a":1,"b":null,"c":"x"},"ratio":2.0}""")
        assertEquals(values, ToolArguments.validate(spec, values))
        assertEquals("abc", ToolArguments.string(values, "text"))
        assertEquals(3L, ToolArguments.long(values, "count", 9L))
        assertEquals(9L, ToolArguments.long(values, "missing", 9L))
        assertEquals(true, ToolArguments.boolean(values, "flag", false))
        assertEquals(false, ToolArguments.boolean(values, "missing", false))
        assertEquals(3, ToolArguments.objectOrNull(values, "values")?.size)
        assertEquals("fallback", ToolArguments.string(values, "missing", "fallback"))
    }

    @Test
    fun `nulls for optional names and missing bodies are accepted`() {
        val values = arguments("""{"text":"de","count":null}""")
        assertEquals(values, ToolArguments.validate(spec, values))
        assertTrue(ToolArguments.validate(spec.copy(inputSchema = JsonSchemas.objectSchema()), null).isEmpty())
        assertTrue(ToolArguments.validate(spec.copy(inputSchema = JsonSchemas.objectSchema()), JsonObject(emptyMap())).isEmpty())
    }

    @Test
    fun `every violation names the argument`() {
        assertEquals("demo_tool requires the argument text", rejected("""{}"""))
        assertEquals("demo_tool requires the argument text", rejected("""{"text":null}"""))
        assertEquals("demo_tool does not accept an argument named extra", rejected("""{"text":"abc","extra":1}"""))
        assertEquals("demo_tool: text must be a string", rejected("""{"text":1}"""))
        assertEquals("demo_tool: text is longer than 5 bytes", rejected("""{"text":"abcdef"}"""))
        assertEquals("demo_tool: text must be one of abc, de", rejected("""{"text":"abcd"}"""))
        assertEquals("demo_tool: count must be an integer", rejected("""{"text":"abc","count":"3"}"""))
        assertEquals("demo_tool: count must be an integer", rejected("""{"text":"abc","count":1.5}"""))
        assertEquals("demo_tool: count must be at least 1", rejected("""{"text":"abc","count":0}"""))
        assertEquals("demo_tool: count must be at most 10", rejected("""{"text":"abc","count":11}"""))
        assertEquals("demo_tool: flag must be true or false", rejected("""{"text":"abc","flag":"true"}"""))
        assertEquals("demo_tool: values must be an object", rejected("""{"text":"abc","values":[1]}"""))
        assertEquals("demo_tool: values values must be strings, numbers, booleans, or null", rejected("""{"text":"abc","values":{"nested":{"a":1}}}"""))
    }
}
