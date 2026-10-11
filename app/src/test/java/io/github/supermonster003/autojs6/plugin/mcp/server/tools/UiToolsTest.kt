package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UiToolsTest {

    /** One recorded host call. */
    private class Call(val module: String, val method: String, val args: JsonArray, val timeoutMs: Long, val permissions: List<String>) {
        val key: String get() = "$module.$method"
        fun arg(index: Int): JsonElement = args[index]
    }

    /** A scripted host: answers by `module.method` from a queue per method, else from a default. */
    private class FakeHost : BridgeCaller {
        val calls = mutableListOf<Call>()
        val answers = mutableMapOf<String, ArrayDeque<JsonElement>>()
        val failures = mutableMapOf<String, BridgeError>()
        var defaultAnswer: (Call) -> JsonElement = { JsonNull }

        fun answer(key: String, vararg values: JsonElement) = apply { answers.getOrPut(key) { ArrayDeque() }.addAll(values) }
        fun answer(key: String, json: String) = answer(key, Json.parseToJsonElement(json))

        override suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome {
            val call = Call(module, method, args, timeoutMs, permissions)
            calls += call
            failures[call.key]?.let { return BridgeOutcome.Failed(ToolFailure.fromBridge(it)) }
            val queued = answers[call.key]
            val value = if (queued != null && queued.isNotEmpty()) queued.removeFirst() else defaultAnswer(call)
            return BridgeOutcome.Ok(value, 1L)
        }
    }

    private var now = 10_000L
    private val sleeps = mutableListOf<Long>()
    private val snapshots = HostSnapshots(clock = { now })
    private var gestureEnabled = false
    private val tools = UiTools(
        snapshots,
        permissions = { ToolPermissions.DEFAULT.with(ToolGroup.UI_GESTURE, gestureEnabled) },
        clock = { now },
        sleep = { ms -> sleeps += ms; now += ms },
        pollMs = 100L,
        settleMs = 10L,
    )

    private fun arguments(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun run(spec: ToolSpec, json: String, host: FakeHost): ToolFlowResult = runBlocking {
        val flow = tools.planFor(spec, ToolArguments.validate(spec, arguments(json))) ?: error("${spec.name} has no flow")
        flow.run(host)
    }

    private fun failure(spec: ToolSpec, json: String, host: FakeHost): ToolFailure = try {
        run(spec, json, host)
        fail("${spec.name} $json must fail")
        error("unreachable")
    } catch (e: ToolFailureException) {
        e.failure
    } catch (e: ToolArgumentException) {
        e.failure
    }

    private fun node(className: String, text: String = "", id: String = "", bounds: String = "0,0,100,50", clickable: Boolean = true, extra: String = ""): String {
        val (l, t, r, b) = bounds.split(",").map { it.trim().toInt() }
        return """{"className":"$className","text":"$text","desc":"","id":"$id","bounds":{"left":$l,"top":$t,"right":$r,"bottom":$b},"clickable":$clickable,"enabled":true$extra}"""
    }

    private fun dumpResult(vararg nodes: String, truncated: Boolean = false): String =
        """{"schema":"autojs6-bridge-accessibility-dump-v1","format":"json","window":"active","roots":1,"packageName":"com.android.settings","activityName":"com.android.settings.Settings","nodeCount":${nodes.size},"truncated":$truncated,"nodes":[${nodes.joinToString(",")}]}"""

    private val COMPACT_TEXT = listOf(
        "window: com.android.settings/.Settings  bounds=[0,0][1080,2400]  nodes=3",
        "#n1 FrameLayout [0,0][1080,2400]",
        "#n2  RecyclerView scrollable id=com.android.settings:id/list [0,200][1080,2400]",
        "#n3   TextView clickable long_clickable \"Wi-Fi\" c=(540,250)",
    ).joinToString("\n")

    /** The host's compact dump: the text with the references and the id of the snapshot it keeps. */
    private fun compactResult(snapshotId: String = "p:1", text: String = COMPACT_TEXT): String =
        """{"schema":"autojs6-bridge-accessibility-dump-v1","format":"compact","window":"active","roots":1,"packageName":"com.android.settings","activityName":"com.android.settings.Settings","nodeCount":3,"truncated":false,"text":${Json.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(text))},"snapshotId":"$snapshotId","windowIdentity":"ab12"}"""

    private fun staleFailure(reason: String) = BridgeError("Error", "NODE_REF_STALE: $reason; take a new compact dump.", "invalid-request", BridgeError.CATEGORY_INVALID_REQUEST, "accessibility", "click")

    private val ROOT = node("android.widget.FrameLayout", bounds = "0,0,1080,2400", clickable = false, extra = ""","root":0,"depth":0,"index":-1,"childCount":2,"visible":true,"scrollable":false,"checkable":false,"checked":false,"editable":false,"focused":false,"selected":false,"longClickable":false""")
    private val LIST = node("androidx.recyclerview.widget.RecyclerView", id = "com.android.settings:id/list", bounds = "0,200,1080,2400", clickable = false, extra = ""","root":0,"depth":1,"index":0,"childCount":1,"visible":true,"scrollable":true,"checkable":false,"checked":false,"editable":false,"focused":false,"selected":false,"longClickable":false""")
    private val WIFI = node("android.widget.TextView", text = "Wi-Fi", bounds = "0,200,1080,300", extra = ""","root":0,"depth":2,"index":0,"childCount":0,"visible":true,"scrollable":false,"checkable":false,"checked":false,"editable":false,"focused":false,"selected":false,"longClickable":true""")

    // ------------------------------------------------------------------ observation

    @Test
    fun `ui_dump returns the compact dump of the host and remembers its snapshot`() {
        val host = FakeHost().answer("accessibility.dump", compactResult())

        val result = run(ToolCatalog.uiDump, """{"maxNodes":50,"maxDepth":5}""", host)

        val call = host.calls.single()
        assertEquals("accessibility.dump", call.key)
        assertEquals(listOf("accessibility"), call.permissions)
        val options = call.arg(0).jsonObject
        assertEquals("compact", options["format"]?.jsonPrimitive?.content)
        assertEquals(50, options["maxNodes"]?.jsonPrimitive?.content?.toInt())
        assertEquals(5, options["maxDepth"]?.jsonPrimitive?.content?.toInt())
        assertEquals(true, options["visibleOnly"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("active", options["window"]?.jsonPrimitive?.content)

        assertEquals("p:1", result.structured["snapshotId"]?.jsonPrimitive?.content)
        assertEquals("text", result.structured["format"]?.jsonPrimitive?.content)
        assertEquals(3, result.structured["nodeCount"]?.jsonPrimitive?.content?.toInt())
        assertEquals("com.android.settings", result.structured["window"]?.jsonObject?.get("packageName")?.jsonPrimitive?.content)
        assertNull(result.structured["nodes"])
        assertEquals(COMPACT_TEXT, result.text)
        assertEquals("p:1", snapshots.latest?.id)
        assertEquals(listOf("#n1", "#n2", "#n3"), snapshots.latest!!.rows.map { it.ref })

        val json = run(ToolCatalog.uiDump, """{"format":"json"}""", host.answer("accessibility.dump", dumpResult(ROOT, WIFI)))
        assertEquals("json", host.calls.last().arg(0).jsonObject["format"]?.jsonPrimitive?.content)
        assertNull(json.structured["snapshotId"])
        val nodes = json.structured["nodes"]!!.jsonArray
        assertEquals(2, nodes.size)
        assertNull(nodes[1].jsonObject["ref"])
        assertEquals(true, nodes[1].jsonObject["longClickable"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(2, nodes[1].jsonObject["depth"]?.jsonPrimitive?.content?.toInt())
        assertEquals(json.structured.toString(), json.text)

        val xml = run(ToolCatalog.uiDump, """{"format":"xml"}""", host.answer("accessibility.dump", """{"schema":"x","format":"xml","roots":1,"nodeCount":9,"truncated":false,"textTruncated":false,"text":"<hierarchy/>"}"""))
        assertEquals("xml", host.calls.last().arg(0).jsonObject["format"]?.jsonPrimitive?.content)
        assertEquals("<hierarchy/>", xml.text)
        assertEquals(9, xml.structured["nodeCount"]?.jsonPrimitive?.content?.toInt())
        // Only the compact form replaces the snapshot the references point to.
        assertEquals("p:1", snapshots.latest?.id)

        val old = failure(ToolCatalog.uiDump, "{}", FakeHost().answer("accessibility.dump", """{"schema":"x","format":"text","roots":1,"nodeCount":0,"truncated":false,"text":"<node/>"}"""))
        assertEquals(ToolErrorCodes.HOST_ERROR, old.code)
        assertTrue(old.message, old.message.contains("5298"))
    }

    @Test
    fun `ui_find polls until a match and returns a selector per node`() {
        val host = FakeHost().answer("accessibility.findAll", JsonArray(emptyList()), JsonArray(emptyList()), Json.parseToJsonElement("[${node("android.widget.Button", "OK")},${node("android.widget.Button", "OK", bounds = "0,60,100,110")}]"))

        val result = run(ToolCatalog.uiFind, """{"selector":{"text":"OK","className":"Button"},"limit":1,"timeoutMs":1000}""", host)

        assertEquals(3, host.calls.size)
        assertEquals(listOf(100L, 100L), sleeps)
        val selector = host.calls[0].arg(0).jsonObject
        assertEquals("OK", selector["text"]?.jsonPrimitive?.content)
        assertEquals(UiSelectors.shortClassPattern("Button"), selector["classNameMatches"]?.jsonPrimitive?.content)
        assertEquals(2, result.structured["count"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1, result.structured["returned"]?.jsonPrimitive?.content?.toInt())
        assertEquals(true, result.structured["truncated"]?.jsonPrimitive?.booleanOrNull)
        val pinned = result.structured["nodes"]!!.jsonArray[0].jsonObject["selector"]!!.jsonObject
        assertEquals("OK", pinned["text"]?.jsonPrimitive?.content)
        assertEquals(Bounds(0, 0, 100, 50), Bounds.fromJson(pinned["boundsInside"]))
        assertEquals(Bounds(0, 0, 100, 50), Bounds.fromJson(pinned["boundsContains"]))
        assertNull(result.structured["snapshotId"])
        assertNull(result.structured["hint"])
        assertNull(snapshots.latest)

        val empty = run(ToolCatalog.uiFind, """{"selector":{"text":"nope"}}""", FakeHost().answer("accessibility.findAll", JsonArray(emptyList())))
        assertEquals(0, empty.structured["count"]?.jsonPrimitive?.content?.toInt())
        assertTrue(empty.structured["hint"]!!.jsonPrimitive.content.startsWith("no node matches text=\"nope\""))
        assertEquals(0, empty.structured["nodes"]!!.jsonArray.size)
    }

    @Test
    fun `ui_wait_for answers when the state is reached and TIMEOUT otherwise`() {
        val host = FakeHost().answer("accessibility.findOne", JsonNull, Json.parseToJsonElement(node("android.widget.TextView", "Done")))
        val appeared = run(ToolCatalog.uiWaitFor, """{"selector":{"text":"Done"},"timeoutMs":5000}""", host)
        assertEquals(true, appeared.structured["satisfied"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("appear", appeared.structured["state"]?.jsonPrimitive?.content)
        assertEquals("Done", appeared.structured["node"]?.jsonObject?.get("selector")?.jsonObject?.get("text")?.jsonPrimitive?.content)
        assertEquals(100L, appeared.structured["elapsedMs"]?.jsonPrimitive?.content?.toLong())

        val gone = run(ToolCatalog.uiWaitFor, """{"selector":{"text":"Loading"},"state":"disappear","timeoutMs":1000}""", FakeHost().answer("accessibility.findOne", Json.parseToJsonElement(node("android.widget.TextView", "Loading")), JsonNull))
        assertEquals(true, gone.structured["satisfied"]?.jsonPrimitive?.booleanOrNull)
        assertNull(gone.structured["node"])

        val timeout = failure(ToolCatalog.uiWaitFor, """{"selector":{"text":"Never"},"timeoutMs":300}""", FakeHost())
        assertEquals(ToolErrorCodes.TIMEOUT, timeout.code)
        assertTrue(timeout.message, timeout.message.startsWith("no node matching text=\"Never\" appeared within 300 ms"))
    }

    @Test
    fun `ui_current_window and ui_explain_selector pass the host records through without the schema`() {
        val host = FakeHost().answer("app.currentWindow", """{"schema":"autojs6-bridge-app-current-window-v1","packageName":"com.a","activityName":"com.a.Main","accessibilityAvailable":true,"windows":[]}""")
        val window = run(ToolCatalog.uiCurrentWindow, "{}", host)
        assertEquals(setOf("packageName", "activityName", "accessibilityAvailable", "windows"), window.structured.keys)
        assertEquals(listOf("app.query", "accessibility"), host.calls.single().permissions)

        val explainHost = FakeHost().answer(
            "accessibility.explain",
            """{"schema":"autojs6-bridge-accessibility-explain-v1","selector":"text(\"OK\")","found":false,"count":0,"failingIndex":0,"failingFilter":"text(\"OK\")","steps":[{"index":0,"filter":"text(\"OK\")","matched":0,"cumulative":0}],"matches":[],"nearMisses":[${node("android.widget.Button", "Ok")}],"text":"explained"}""",
        )
        val explained = run(ToolCatalog.uiExplainSelector, """{"selector":{"text":"OK"}}""", explainHost)
        assertEquals("explained", explained.text)
        assertFalse(explained.structured.containsKey("schema"))
        assertEquals("Ok", explained.structured["nearMisses"]!!.jsonArray[0].jsonObject["text"]?.jsonPrimitive?.content)
        assertEquals(listOf(50, 25), explained.structured["nearMisses"]!!.jsonArray[0].jsonObject["center"]!!.jsonArray.map { it.jsonPrimitive.content.toInt() })
        assertEquals(2, explainHost.calls.single().args.size)
    }

    // ------------------------------------------------------------------ actions

    @Test
    fun `ui_click by nodeRef hands the reference and its snapshot to the host`() {
        val host = FakeHost().answer("accessibility.dump", compactResult("p:1")).answer("accessibility.dump", compactResult("p:2"))
        run(ToolCatalog.uiDump, "{}", host)
        run(ToolCatalog.uiDump, "{}", host)
        host.answer("accessibility.click", JsonPrimitive(true), JsonPrimitive(true), JsonPrimitive(true))

        val result = run(ToolCatalog.uiClick, """{"nodeRef":"#n3"}""", host)

        val click = host.calls[2]
        assertEquals("accessibility.click", click.key)
        assertEquals(1, click.args.size)
        assertEquals(mapOf("nodeRef" to "#n3", "snapshotId" to "p:2"), click.arg(0).jsonObject.mapValues { it.value.jsonPrimitive.content })
        assertEquals(listOf("accessibility"), click.permissions)
        assertEquals("click", result.structured["action"]?.jsonPrimitive?.content)
        assertEquals("nodeRef", result.structured["via"]?.jsonPrimitive?.content)
        assertEquals("#n3", result.structured["target"]?.jsonObject?.get("ref")?.jsonPrimitive?.content)
        assertEquals("p:2", result.structured["target"]?.jsonObject?.get("snapshotId")?.jsonPrimitive?.content)

        // n3 and 3 mean #n3; an explicit snapshotId targets an older dump the host still keeps.
        run(ToolCatalog.uiClick, """{"nodeRef":"n3","snapshotId":"p:1"}""", host)
        assertEquals(mapOf("nodeRef" to "#n3", "snapshotId" to "p:1"), host.calls[3].arg(0).jsonObject.mapValues { it.value.jsonPrimitive.content })
        run(ToolCatalog.uiLongClick, """{"nodeRef":"3"}""", host.answer("accessibility.longClick", JsonPrimitive(true)))
        assertEquals("accessibility.longClick", host.calls[4].key)
        assertEquals("#n3", host.calls[4].arg(0).jsonObject["nodeRef"]?.jsonPrimitive?.content)
    }

    @Test
    fun `ui_click reports stale references missing selectors and refused actions`() {
        val noDump = failure(ToolCatalog.uiClick, """{"nodeRef":"#n2"}""", FakeHost())
        assertEquals(ToolErrorCodes.NODE_REF_STALE, noDump.code)
        assertEquals("#n2 refers to a node, but no ui_dump has been taken yet", noDump.message)

        val host = FakeHost().answer("accessibility.dump", compactResult())
        run(ToolCatalog.uiDump, "{}", host)

        host.failures["accessibility.click"] = staleFailure("window changed")
        val stale = failure(ToolCatalog.uiClick, """{"nodeRef":"#n2"}""", host)
        assertEquals(ToolErrorCodes.NODE_REF_STALE, stale.code)
        assertEquals("#n2 is stale: window changed", stale.message)
        assertTrue(stale.hint!!.contains("ui_dump"))

        host.failures["accessibility.click"] = BridgeError("Error", "AutoJs6 accessibility bridge requires an enabled accessibility capability provider in the current process.", "x", BridgeError.CATEGORY_UNAVAILABLE, "accessibility", "click")
        assertEquals(ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING, failure(ToolCatalog.uiClick, """{"nodeRef":"#n2"}""", host).code)

        val malformed = failure(ToolCatalog.uiClick, """{"nodeRef":"#x2"}""", host)
        assertEquals(ToolErrorCodes.NODE_REF_STALE, malformed.code)
        assertTrue(malformed.message, malformed.message.startsWith("#x2 is not a node reference"))

        val notPerformed = failure(ToolCatalog.uiClick, """{"nodeRef":"#n1"}""", FakeHost().answer("accessibility.dump", compactResult()).answer("accessibility.click", JsonPrimitive(false)).also { run(ToolCatalog.uiDump, "{}", it) })
        assertEquals(ToolErrorCodes.ACTION_FAILED, notPerformed.code)
        assertEquals("#n1 did not perform click", notPerformed.message)

        val notFound = failure(ToolCatalog.uiLongClick, """{"selector":{"text":"Missing"}}""", FakeHost())
        assertEquals(ToolErrorCodes.NODE_NOT_FOUND, notFound.code)
        assertEquals("no node matches text=\"Missing\" in the active window", notFound.message)

        val refused = failure(ToolCatalog.uiClick, """{"selector":{"text":"Wi-Fi"}}""", FakeHost().answer("accessibility.findOne", Json.parseToJsonElement(WIFI)).answer("accessibility.click", JsonPrimitive(false)))
        assertEquals(ToolErrorCodes.ACTION_FAILED, refused.code)
        assertEquals("TextView \"Wi-Fi\" did not perform click", refused.message)

        val vanished = failure(ToolCatalog.uiClick, """{"selector":{"text":"Wi-Fi"}}""", FakeHost().answer("accessibility.findOne", Json.parseToJsonElement(WIFI)).answer("accessibility.click", JsonNull))
        assertEquals(ToolErrorCodes.NODE_NOT_FOUND, vanished.code)

        assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, failure(ToolCatalog.uiClick, "{}", FakeHost()).code)
        assertEquals("ui_click accepts nodeRef or selector, not both", failure(ToolCatalog.uiClick, """{"nodeRef":"#n1","selector":{"text":"x"}}""", FakeHost()).message)
        assertEquals("ui_click: snapshotId goes with nodeRef", failure(ToolCatalog.uiClick, """{"selector":{"text":"x"},"snapshotId":"p:1"}""", FakeHost()).message)
        assertEquals("ui_click needs both x and y for a coordinate click", failure(ToolCatalog.uiClick, """{"x":1}""", FakeHost()).message)
        assertEquals("ui_click accepts nodeRef, selector, or x and y, not a combination", failure(ToolCatalog.uiClick, """{"x":1,"y":2,"nodeRef":"#n1"}""", FakeHost()).message)
    }

    @Test
    fun `coordinate clicks follow the ui_gesture switch and the screen size`() {
        val disabled = failure(ToolCatalog.uiClick, """{"x":10,"y":20}""", FakeHost())
        assertEquals(ToolErrorCodes.TOOL_DISABLED, disabled.code)
        assertTrue(disabled.message, disabled.message.startsWith("ui_click with x and y is a coordinate gesture"))

        gestureEnabled = true
        val host = FakeHost().answer("device.info", """{"schema":"autojs6-bridge-device-info-v1","screen":{"width":1080,"height":2400}}""").answer("accessibility.swipe", JsonPrimitive(true), JsonPrimitive(true))
        val tap = run(ToolCatalog.uiClick, """{"x":10,"y":20}""", host)
        assertEquals("device.info", host.calls[0].key)
        val swipe = host.calls[1]
        assertEquals("accessibility.swipe", swipe.key)
        assertEquals(listOf(10, 20, 10, 20, 100), swipe.args.map { it.jsonPrimitive.content.toInt() })
        assertEquals(listOf("accessibility", "accessibility.gesture"), swipe.permissions)
        assertEquals("coordinates", tap.structured["via"]?.jsonPrimitive?.content)
        assertEquals(100, tap.structured["durationMs"]?.jsonPrimitive?.content?.toInt())

        val press = run(ToolCatalog.uiLongClick, """{"x":10,"y":20}""", host)
        assertEquals(1, host.calls.count { it.key == "device.info" })
        assertEquals(700, press.structured["durationMs"]?.jsonPrimitive?.content?.toInt())
        assertEquals(listOf(10, 20, 10, 20, 700), host.calls.last().args.map { it.jsonPrimitive.content.toInt() })

        val outside = failure(ToolCatalog.uiClick, """{"x":3000,"y":20}""", host)
        assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, outside.code)
        assertEquals("ui_click: (3000, 20) is outside the 1080x2400 screen", outside.message)

        val cancelled = failure(ToolCatalog.uiClick, """{"x":1,"y":1}""", host.answer("accessibility.swipe", JsonPrimitive(false)))
        assertEquals(ToolErrorCodes.ACTION_FAILED, cancelled.code)
    }

    @Test
    fun `ui_set_text replaces or appends through the located node`() {
        val field = node("android.widget.EditText", "old", id = "com.a:id/query", extra = "")
        val host = FakeHost().answer("accessibility.findOne", Json.parseToJsonElement(field), Json.parseToJsonElement(field)).answer("accessibility.setText", JsonPrimitive(true), JsonPrimitive(true))

        val replaced = run(ToolCatalog.uiSetText, """{"selector":{"id":"query"},"text":"new"}""", host)
        assertEquals("new", host.calls[1].arg(1).jsonPrimitive.content)
        assertEquals(UiSelectors.shortIdPattern("query"), host.calls[1].arg(0).jsonObject["idMatches"]?.jsonPrimitive?.content)
        assertEquals("new", replaced.structured["text"]?.jsonPrimitive?.content)
        assertEquals(false, replaced.structured["appended"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("selector", replaced.structured["via"]?.jsonPrimitive?.content)

        val appended = run(ToolCatalog.uiSetText, """{"selector":{"id":"query"},"text":" more","append":true}""", host)
        assertEquals("old more", host.calls[3].arg(1).jsonPrimitive.content)
        assertEquals("old more", appended.structured["text"]?.jsonPrimitive?.content)

        val refused = failure(ToolCatalog.uiSetText, """{"selector":{"id":"query"},"text":"x"}""", FakeHost().answer("accessibility.findOne", Json.parseToJsonElement(field)).answer("accessibility.setText", JsonPrimitive(false)))
        assertEquals(ToolErrorCodes.ACTION_FAILED, refused.code)
        assertTrue(refused.hint!!.contains("editable"))

        val refHost = FakeHost().answer("accessibility.dump", compactResult()).answer("accessibility.setText", JsonPrimitive(true))
        run(ToolCatalog.uiDump, "{}", refHost)
        val byRef = run(ToolCatalog.uiSetText, """{"nodeRef":"#n3","text":"new"}""", refHost)
        assertEquals("#n3", refHost.calls[1].arg(0).jsonObject["nodeRef"]?.jsonPrimitive?.content)
        assertEquals("new", refHost.calls[1].arg(1).jsonPrimitive.content)
        assertEquals("nodeRef", byRef.structured["via"]?.jsonPrimitive?.content)
        val append = failure(ToolCatalog.uiSetText, """{"nodeRef":"#n3","text":"x","append":true}""", refHost)
        assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, append.code)
        assertTrue(append.message, append.message.contains("append needs a selector"))
        assertEquals(2, refHost.calls.size)
    }

    @Test
    fun `ui_scroll maps directions repeats and stops at the end`() {
        val host = FakeHost().answer("accessibility.findOne", Json.parseToJsonElement(LIST)).answer("accessibility.scrollForward", JsonPrimitive(true), JsonPrimitive(true), JsonPrimitive(false))

        val result = run(ToolCatalog.uiScroll, """{"direction":"down","times":5}""", host)

        assertEquals("accessibility.findOne", host.calls[0].key)
        assertEquals(true, host.calls[0].arg(0).jsonObject["scrollable"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(3, host.calls.count { it.key == "accessibility.scrollForward" })
        assertEquals(listOf(10L, 10L), sleeps)
        assertEquals(2, result.structured["performed"]?.jsonPrimitive?.content?.toInt())
        assertEquals(5, result.structured["requested"]?.jsonPrimitive?.content?.toInt())
        assertEquals(true, result.structured["atEnd"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("scrollable", result.structured["via"]?.jsonPrimitive?.content)
        assertTrue(result.structured["hint"]!!.jsonPrimitive.content.contains("after 2 of 5 steps"))

        val up = run(ToolCatalog.uiScroll, """{"selector":{"id":"list"},"direction":"up"}""", FakeHost().answer("accessibility.findOne", Json.parseToJsonElement(LIST)).answer("accessibility.scrollBackward", JsonPrimitive(true)))
        assertEquals(1, up.structured["performed"]?.jsonPrimitive?.content?.toInt())
        assertEquals(false, up.structured["atEnd"]?.jsonPrimitive?.booleanOrNull)
        assertNull(up.structured["hint"])

        val none = failure(ToolCatalog.uiScroll, "{}", FakeHost())
        assertEquals(ToolErrorCodes.NODE_NOT_FOUND, none.code)
        assertTrue(none.message.startsWith("no scrollable node"))

        val refHost = FakeHost().answer("accessibility.dump", compactResult()).answer("accessibility.scrollForward", JsonPrimitive(true), JsonPrimitive(false))
        run(ToolCatalog.uiDump, "{}", refHost)
        val byRef = run(ToolCatalog.uiScroll, """{"nodeRef":"#n2","times":3}""", refHost)
        assertEquals(2, refHost.calls.count { it.key == "accessibility.scrollForward" })
        assertTrue(refHost.calls.drop(1).all { it.arg(0).jsonObject["nodeRef"]?.jsonPrimitive?.content == "#n2" })
        assertEquals(1, byRef.structured["performed"]?.jsonPrimitive?.content?.toInt())
        assertEquals("#n2", byRef.structured["target"]?.jsonObject?.get("ref")?.jsonPrimitive?.content)
        refHost.failures["accessibility.scrollForward"] = staleFailure("node no longer matches")
        assertEquals("#n2 is stale: node no longer matches", failure(ToolCatalog.uiScroll, """{"nodeRef":"#n2"}""", refHost).message)
    }

    @Test
    fun `ui_press_key maps onto the accessibility and keys modules`() {
        val host = FakeHost().defaultAnswerTrue()
        assertEquals(true, run(ToolCatalog.uiPressKey, """{"key":"back"}""", host).structured["performed"]?.jsonPrimitive?.booleanOrNull)
        run(ToolCatalog.uiPressKey, """{"key":"home"}""", host)
        run(ToolCatalog.uiPressKey, """{"key":"recents"}""", host)
        run(ToolCatalog.uiPressKey, """{"key":"notifications"}""", host)
        run(ToolCatalog.uiPressKey, """{"key":"quick_settings"}""", host)
        run(ToolCatalog.uiPressKey, """{"key":"power_dialog"}""", host)
        run(ToolCatalog.uiPressKey, """{"key":"lock_screen"}""", host)
        assertEquals(
            listOf("accessibility.back", "accessibility.home", "accessibility.recentApps", "keys.notifications", "keys.quickSettings", "keys.powerDialog", "keys.lockScreen"),
            host.calls.map { it.key },
        )
        assertEquals(listOf("accessibility"), host.calls[0].permissions)
        assertEquals(listOf("keys"), host.calls[3].permissions)
        assertTrue(host.calls.all { it.args.isEmpty() })

        val refused = failure(ToolCatalog.uiPressKey, """{"key":"lock_screen"}""", FakeHost().answer("keys.lockScreen", JsonPrimitive(false)))
        assertEquals(ToolErrorCodes.ACTION_FAILED, refused.code)
        assertTrue(refused.hint!!.contains("Android 9"))
        assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, failure(ToolCatalog.uiPressKey, """{"key":"menu"}""", FakeHost()).code)
    }

    @Test
    fun `ui_swipe and ui_gesture validate their paths and send the host shapes`() {
        gestureEnabled = true
        val host = FakeHost().defaultAnswerTrue()
        val swipe = run(ToolCatalog.uiSwipe, """{"x1":100,"y1":1500,"x2":100,"y2":500}""", host)
        assertEquals(listOf(100, 1500, 100, 500, 300), host.calls.last().args.map { it.jsonPrimitive.content.toInt() })
        assertEquals(5300L, host.calls.last().timeoutMs)
        assertEquals(true, swipe.structured["performed"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(listOf(100, 500), swipe.structured["to"]!!.jsonArray.map { it.jsonPrimitive.content.toInt() })

        val gesture = run(ToolCatalog.uiGesture, """{"durationMs":800,"points":[[10,10],[20,20],[30,10]]}""", host)
        val args = host.calls.last().args
        assertEquals(800, args[0].jsonPrimitive.content.toInt())
        assertEquals(3, args[1].jsonArray.size)
        assertEquals(listOf(20, 20), args[1].jsonArray[1].jsonArray.map { it.jsonPrimitive.content.toInt() })
        assertEquals(3, gesture.structured["points"]?.jsonPrimitive?.content?.toInt())

        assertEquals("ui_gesture: points needs at least 2 [x, y] pairs", failure(ToolCatalog.uiGesture, """{"durationMs":100,"points":[[1,1]]}""", FakeHost()).message)
        assertEquals("ui_gesture: points[1] must have exactly two integers", failure(ToolCatalog.uiGesture, """{"durationMs":100,"points":[[1,1],[2]]}""", FakeHost()).message)
        assertEquals("ui_gesture: points[0] must lie within 0..65535", failure(ToolCatalog.uiGesture, """{"durationMs":100,"points":[[-1,1],[2,2]]}""", FakeHost()).message)
        assertEquals("ui_gesture: points[0] x must be an integer", failure(ToolCatalog.uiGesture, """{"durationMs":100,"points":[["a",1],[2,2]]}""", FakeHost()).message)

        val cancelled = failure(ToolCatalog.uiSwipe, """{"x1":1,"y1":1,"x2":2,"y2":2,"durationMs":50}""", FakeHost().answer("accessibility.swipe", JsonPrimitive(false)))
        assertEquals(ToolErrorCodes.ACTION_FAILED, cancelled.code)
        assertTrue(cancelled.message.startsWith("the system cancelled the swipe from (1, 1) to (2, 2)"))
    }

    @Test
    fun `host failures travel through the flows unchanged`() {
        val host = FakeHost().apply {
            failures["accessibility.dump"] = BridgeError("Error", "AutoJs6 accessibility bridge requires an enabled accessibility capability provider in the current process.", "x", BridgeError.CATEGORY_UNAVAILABLE, "accessibility", "dump")
            failures["keys.notifications"] = BridgeError("Error", "AutoJs6 keys bridge requires an enabled accessibility capability provider in the current process.", "x", BridgeError.CATEGORY_UNAVAILABLE, "keys", "notifications")
        }
        val dump = failure(ToolCatalog.uiDump, "{}", host)
        assertEquals(ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING, dump.code)
        assertTrue(dump.hint!!.contains("Android 13"))
        val key = failure(ToolCatalog.uiPressKey, """{"key":"notifications"}""", host)
        assertEquals(ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING, key.code)
        assertNull(tools.planFor(ToolCatalog.scriptRun, JsonObject(emptyMap())))
    }

    private fun FakeHost.defaultAnswerTrue(): FakeHost = apply { defaultAnswer = { JsonPrimitive(true) } }
}
