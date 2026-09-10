package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UiSelectorsTest {

    private fun parse(json: String) = UiSelectors.parse("ui_find", Json.parseToJsonElement(json))

    private fun rejected(json: String): String = try {
        parse(json)
        fail("expected $json to be rejected")
        ""
    } catch (e: ToolArgumentException) {
        assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, e.failure.code)
        e.failure.message
    }

    @Test
    fun `every dialect key passes through with its type`() {
        val parsed = parse(
            """{"text":"Wi-Fi","textContains":"Wi","textMatches":"(?i)wi.*","desc":"d","descContains":"dc","descMatches":"d.*",""" +
                    """"id":"com.android.settings:id/search","idMatches":".*search","className":"android.widget.TextView","classNameMatches":".*View",""" +
                    """"clickable":true,"enabled":false,"scrollable":true,"depth":3,"boundsInside":{"left":0,"top":10,"right":100,"bottom":110},"boundsContains":{"left":-5,"top":0,"right":1,"bottom":1}}""",
        )
        assertEquals(UiSelectors.KEYS, parsed.keys.toList())
        assertEquals("Wi-Fi", parsed["text"]?.jsonPrimitive?.content)
        assertEquals(true, parsed["clickable"]?.jsonPrimitive?.content?.toBoolean())
        assertEquals(3, parsed["depth"]?.jsonPrimitive?.content?.toInt())
        assertEquals(10, parsed["boundsInside"]?.jsonObject?.get("top")?.jsonPrimitive?.content?.toInt())
        assertEquals(-5, parsed["boundsContains"]?.jsonObject?.get("left")?.jsonPrimitive?.content?.toInt())
        assertEquals("com.android.settings:id/search", parsed["id"]?.jsonPrimitive?.content)
        assertEquals("android.widget.TextView", parsed["className"]?.jsonPrimitive?.content)
    }

    @Test
    fun `short ids and class names become whole-value patterns and nulls are ignored`() {
        val parsed = parse("""{"id":"search","className":"TextView","text":null}""")
        assertEquals(setOf("idMatches", "classNameMatches"), parsed.keys)
        val idPattern = parsed["idMatches"]!!.jsonPrimitive.content
        val classPattern = parsed["classNameMatches"]!!.jsonPrimitive.content
        assertTrue(Regex(idPattern).matches("com.android.settings:id/search"))
        assertTrue(Regex(idPattern).matches("search"))
        assertTrue(!Regex(idPattern).matches("com.android.settings:id/search_bar"))
        assertTrue(Regex(classPattern).matches("android.widget.TextView"))
        assertTrue(!Regex(classPattern).matches("android.widget.AutoCompleteTextView"))
        assertTrue(Regex(UiSelectors.shortIdPattern("a.b")).matches("x:id/a.b"))
        assertTrue(!Regex(UiSelectors.shortIdPattern("a.b")).matches("x:id/aXb"))
        assertNull(parsed["text"])
    }

    @Test
    fun `violations name the condition`() {
        assertEquals("ui_find: selector must be an object with at least one condition", rejected("\"text\""))
        assertEquals("ui_find: selector needs at least one condition (${UiSelectors.KEYS.joinToString(", ")})", rejected("{}"))
        assertEquals("ui_find: selector needs at least one condition (${UiSelectors.KEYS.joinToString(", ")})", rejected("""{"text":null}"""))
        assertEquals("ui_find: selector does not accept a condition named textStartsWith; use ${UiSelectors.KEYS.joinToString(", ")}", rejected("""{"textStartsWith":"a"}"""))
        assertEquals("ui_find: selector.text must be a string", rejected("""{"text":1}"""))
        assertEquals("ui_find: selector.text must not be blank", rejected("""{"text":"  "}"""))
        assertEquals("ui_find: selector.clickable must be true or false", rejected("""{"clickable":"yes"}"""))
        assertEquals("ui_find: selector.depth must be an integer", rejected("""{"depth":1.5}"""))
        assertEquals("ui_find: selector.depth must be at least 0", rejected("""{"depth":-1}"""))
        assertEquals("ui_find: selector.boundsInside must be an object with integer left, top, right, and bottom", rejected("""{"boundsInside":{"left":0,"top":0,"right":1}}"""))
        assertEquals("ui_find: selector.boundsInside must be an object with integer left, top, right, and bottom", rejected("""{"boundsInside":{"left":0,"top":0,"right":1,"bottom":1,"width":9}}"""))
        assertTrue(rejected("""{"textMatches":"("}""").startsWith("ui_find: selector.textMatches is not a valid regular expression"))
        assertTrue(rejected("""{"id":"search","idMatches":"x"}""").startsWith("ui_find: selector gives idMatches twice"))
        assertTrue(rejected("""{"text":"${"x".repeat(UiSelectors.MAX_STRING_LENGTH + 1)}"}""").endsWith("characters"))
    }

    @Test
    fun `relocation and exact selectors carry the fingerprint`() {
        val node = UiNodeInfo("android.widget.Switch", "On", "Wi-Fi switch", "android:id/switch_widget", Bounds(900, 250, 1060, 346), clickable = true, enabled = true)

        val relocation = UiSelectors.relocation(node)
        assertEquals(listOf("className", "id", "text", "desc", "boundsInside"), relocation.keys.toList())
        val inside = Bounds.fromJson(relocation["boundsInside"])!!
        assertEquals(Bounds(852, 202, 1108, 394), inside)

        val exact = UiSelectors.exact(node)
        assertEquals(Bounds(900, 250, 1060, 346), Bounds.fromJson(exact["boundsInside"]))
        assertEquals(Bounds(900, 250, 1060, 346), Bounds.fromJson(exact["boundsContains"]))

        val small = UiSelectors.relocation(UiNodeInfo("", "", "", "", Bounds(10, 10, 30, 20), false, true))
        assertEquals(listOf("boundsInside"), small.keys.toList())
        assertEquals(Bounds(0, 5, 40, 25), Bounds.fromJson(small["boundsInside"]))

        val longText = UiSelectors.relocation(node.copy(text = "t".repeat(UiSelectors.MAX_FINGERPRINT_TEXT + 1)))
        assertNull(longText["text"])
        assertEquals("className=\"android.widget.Switch\" boundsInside=[900,250][1060,346]", UiSelectors.describe(UiSelectors.exact(node.copy(text = "", desc = "", id = ""))).replace(" boundsContains=[900,250][1060,346]", ""))
        assertEquals("clickable=true", UiSelectors.describe(parse("""{"clickable":true}""")))
    }

    @Test
    fun `bounds helpers measure distance and expansion`() {
        val a = Bounds(0, 0, 100, 50)
        assertEquals(50, a.centerX)
        assertEquals(25, a.centerY)
        assertEquals(100, a.width)
        assertEquals(0, a.distanceTo(a))
        assertEquals(20, a.distanceTo(Bounds(5, 5, 105, 55)))
        assertEquals(Bounds(0, 0, 105, 52), a.expanded(5, 2))
        assertEquals("[0,0][100,50]", a.compact())
        assertNull(Bounds.fromJson(JsonNull))
        assertNull(Bounds.fromJson(Json.parseToJsonElement("""{"left":0}""")))
        assertEquals(a, Bounds.fromJson(a.toJson()))
    }
}
