package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RegisteredScriptToolsTest {

    private class Call(val module: String, val method: String, val args: JsonArray, val timeoutMs: Long, val permissions: List<String>) {
        val key: String get() = "$module.$method"
    }

    private class FakeCaller(val answers: MutableMap<String, (Call) -> BridgeOutcome> = mutableMapOf()) : BridgeCaller {

        val calls = mutableListOf<Call>()

        override suspend fun call(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): BridgeOutcome {
            val call = Call(module, method, args, timeoutMs, permissions)
            calls += call
            return answers[call.key]?.invoke(call) ?: error("unexpected call ${call.key}")
        }
    }

    private val tools = RegisteredScriptTools(clock = { 1_000L })

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    private val coffee = json("""{"id":"coffee","path":"/storage/emulated/0/Scripts/agent/coffee","kind":"project","description":"Order a coffee\nfor the user",
        "parameters":{"type":"object","properties":{"product":{"type":"string"},"size":{"type":"string","enum":["M","L"],"default":"L"},"note":{"type":"string"}},"required":["product"],"additionalProperties":false},
        "risk":"sensitive","confirm":"before-run","timeoutMs":120000,"examples":[],"tags":["coffee"],"updatedAt":5}""").jsonObject
    private val clean = json("""{"id":"clean","path":"/storage/emulated/0/Scripts/clean.js","kind":"file","description":"Clean downloads",
        "parameters":{"type":"object","properties":{},"additionalProperties":false},"risk":"normal","confirm":"never","timeoutMs":60000}""").jsonObject
    private val finished = json("""{"executionId":7,"finished":true,"outcome":"success","error":null,"result":{"total":12.5},"resultReported":true,
        "consoleCaptureMode":"global-window","consoleTail":["10:00:00 I ordered"]}""")

    private fun run(spec: ToolSpec, caller: FakeCaller, arguments: String): ToolFlowResult = runBlocking {
        tools.planFor(spec, json(arguments).jsonObject)!!.run(caller)
    }

    @Test
    fun `the catalog passes query and limit and summarizes each script`() {
        val caller = FakeCaller(mutableMapOf("agent.listScripts" to { BridgeOutcome.Ok(JsonArray(listOf(coffee, clean)), 1L) }))
        val result = run(ToolCatalog.scriptCatalog, caller, """{"query":" coffee ","limit":2}""")

        val call = caller.calls.single()
        assertEquals(listOf("agent"), call.permissions)
        assertEquals("""{"query":"coffee","limit":2}""", call.args[0].toString())
        assertEquals(2, result.structured["count"]!!.jsonPrimitive.content.toInt())
        assertEquals("true", result.structured["truncated"]!!.jsonPrimitive.content)
        assertEquals(coffee, result.structured["scripts"]!!.jsonArray[0])
        assertEquals(
            listOf(
                "coffee: Order a coffee (risk sensitive, confirm before-run; parameters product*, size=L, note; path /storage/emulated/0/Scripts/agent/coffee)",
                "clean: Clean downloads (risk normal, confirm never; path /storage/emulated/0/Scripts/clean.js)",
            ).joinToString("\n"),
            result.text,
        )

        val empty = FakeCaller(mutableMapOf("agent.listScripts" to { BridgeOutcome.Ok(JsonArray(emptyList()), 1L) }))
        val none = run(ToolCatalog.scriptCatalog, empty, "{}")
        assertEquals("""{"limit":100}""", empty.calls.single().args[0].toString())
        assertTrue(none.text.startsWith("(no registered scripts"))
        assertEquals("false", none.structured["truncated"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a unique id is resolved, the manifest read and the run carries it with the shorter timeout`() {
        val caller = FakeCaller(mutableMapOf(
            "agent.listScripts" to { BridgeOutcome.Ok(JsonArray(listOf(coffee, clean)), 1L) },
            "agent.readManifest" to { BridgeOutcome.Ok(coffee, 1L) },
            "agent.execRegistered" to { BridgeOutcome.Ok(finished, 1L) },
        ))
        val result = run(ToolCatalog.scriptRegisteredRun, caller, """{"id":"coffee","arguments":{"product":"latte"},"timeoutMs":90000}""")

        assertEquals(listOf("agent.listScripts", "agent.readManifest", "agent.execRegistered"), caller.calls.map { it.key })
        assertEquals("""{"query":"coffee","limit":500}""", caller.calls[0].args[0].toString())
        assertEquals("/storage/emulated/0/Scripts/agent/coffee", caller.calls[1].args[0].jsonPrimitive.content)
        val exec = caller.calls[2]
        assertEquals(listOf("agent", "agent.exec", "engines", "engines.exec"), exec.permissions)
        assertEquals("/storage/emulated/0/Scripts/agent/coffee", exec.args[0].jsonPrimitive.content)
        assertEquals("""{"product":"latte"}""", exec.args[1].toString())
        val options = exec.args[2].jsonObject
        assertEquals(coffee, options["expectedManifest"])
        assertEquals(90_000L, options["timeoutMs"]!!.jsonPrimitive.content.toLong())
        assertEquals("true", options["captureConsole"]!!.jsonPrimitive.content)
        assertEquals(95_000L, exec.timeoutMs)

        val structured = result.structured
        assertEquals("coffee", structured["id"]!!.jsonPrimitive.content)
        assertEquals("finished", structured["status"]!!.jsonPrimitive.content)
        assertEquals("sensitive", structured["risk"]!!.jsonPrimitive.content)
        assertEquals("before-run", structured["confirm"]!!.jsonPrimitive.content)
        assertEquals("""{"total":12.5}""", structured["result"].toString())
        assertEquals(7, structured["executionId"]!!.jsonPrimitive.content.toInt())
        assertEquals("coffee: finished\nresult: {\"total\":12.5}\nconsole:\n10:00:00 I ordered", result.text)
    }

    @Test
    fun `a path skips the catalog, the registered timeout caps the run and failures keep their status`() {
        val failed = json("""{"executionId":8,"finished":true,"outcome":"exception","error":{"code":"SCRIPT_FAILED","message":"boom"},"result":null,"resultReported":false,"consoleTail":[]}""")
        val caller = FakeCaller(mutableMapOf(
            "agent.readManifest" to { BridgeOutcome.Ok(clean, 1L) },
            "agent.execRegistered" to { BridgeOutcome.Ok(failed, 1L) },
        ))
        val result = run(ToolCatalog.scriptRegisteredRun, caller, """{"path":"clean.js","timeoutMs":200000}""")
        assertEquals(listOf("agent.readManifest", "agent.execRegistered"), caller.calls.map { it.key })
        assertEquals("clean.js", caller.calls[0].args[0].jsonPrimitive.content)
        assertEquals("{}", caller.calls[1].args[1].toString())
        assertEquals(60_000L, caller.calls[1].args[2].jsonObject["timeoutMs"]!!.jsonPrimitive.content.toLong())
        assertEquals("error", result.structured["status"]!!.jsonPrimitive.content)
        assertFalse(result.structured.containsKey("result"))
        assertEquals("clean: error (boom)", result.text)
    }

    @Test
    fun `ids that match nothing or several scripts and the host catalog codes are explained`() {
        val twin = JsonObject(coffee + ("path" to json("\"/storage/emulated/0/Scripts/other/coffee\"")))
        listOf(JsonArray(listOf(clean)) to "no registered script has the id coffee", JsonArray(listOf(coffee, twin)) to "2 scripts register the id coffee").forEach { (scripts, expected) ->
            val caller = FakeCaller(mutableMapOf("agent.listScripts" to { BridgeOutcome.Ok(scripts, 1L) }))
            try {
                run(ToolCatalog.scriptRegisteredRun, caller, """{"id":"coffee"}""")
                fail("expected a failure")
            } catch (e: ToolArgumentException) {
                assertEquals(ToolErrorCodes.INVALID_ARGUMENTS, e.failure.code)
                assertTrue(e.failure.message, e.failure.message.contains(expected))
            }
        }

        fun hostFailure(message: String, category: String) = BridgeOutcome.Failed(ToolFailure.fromBridge(BridgeError(name = "Error", message = message, code = "x", category = category, module = "agent", method = "execRegistered")))
        listOf(
            hostFailure("TOOL_ARGUMENTS_INVALID", BridgeError.CATEGORY_INVALID_REQUEST) to (ToolErrorCodes.INVALID_ARGUMENTS to "the arguments do not match"),
            hostFailure("SCRIPT_NOT_REGISTERED", BridgeError.CATEGORY_INVALID_REQUEST) to (ToolErrorCodes.INVALID_ARGUMENTS to "is not a registered script"),
            hostFailure("CAPABILITY_DENIED", BridgeError.CATEGORY_CAPABILITY_DENIED) to (ToolErrorCodes.CAPABILITY_DENIED to "lies outside the directories"),
            hostFailure("LIMIT_EXCEEDED", BridgeError.CATEGORY_RESOURCE_LIMIT) to (ToolErrorCodes.LIMIT_EXCEEDED to "LIMIT_EXCEEDED"),
        ).forEach { (failure, expected) ->
            val caller = FakeCaller(mutableMapOf("agent.readManifest" to { BridgeOutcome.Ok(clean, 1L) }, "agent.execRegistered" to { failure }))
            try {
                run(ToolCatalog.scriptRegisteredRun, caller, """{"path":"clean.js","arguments":{"a":1}}""")
                fail("expected a failure")
            } catch (e: ToolFailureException) {
                assertEquals(expected.first, e.failure.code)
                assertTrue(e.failure.message, e.failure.message.contains(expected.second))
            }
        }
    }

    @Test
    fun `arguments are checked before any host call`() {
        listOf(
            ToolCatalog.scriptRegisteredRun to "{}" to "give exactly one of id or path",
            ToolCatalog.scriptRegisteredRun to """{"id":"a","path":"b"}""" to "give exactly one of id or path",
            ToolCatalog.scriptRegisteredRun to """{"path":"../x.js"}""" to "must not contain .. segments",
            ToolCatalog.scriptRegisteredRun to """{"id":"a","arguments":{"x":null}}""" to "arguments.x must be a string, number or boolean",
            ToolCatalog.scriptRegisteredRun to """{"id":"a","arguments":{"x":"${"a".repeat(17000)}"}}""" to "arguments exceed 16384 bytes",
            ToolCatalog.scriptRegisteredRun to """{"id":"a","timeoutMs":10}""" to "timeoutMs must be at least 1000",
            ToolCatalog.scriptCatalog to """{"limit":501}""" to "limit must be at most 500",
        ).forEach { (input, expected) ->
            val (spec, arguments) = input
            try {
                tools.planFor(spec, json(arguments).jsonObject)
                fail("${spec.name} $arguments should be rejected")
            } catch (e: ToolArgumentException) {
                assertTrue("${spec.name} -> ${e.failure.message}", e.failure.message.contains(expected))
            }
        }
        assertNull(tools.planFor(ToolCatalog.scriptRun, json("""{"source":"1"}""").jsonObject))
    }

    @Test
    fun `the rows are grant-gated script rows with the host methods they use`() {
        assertEquals(ToolGroup.SCRIPT, ToolCatalog.scriptCatalog.group)
        assertEquals(listOf("agent.listScripts"), ToolCatalog.scriptCatalog.hostMethods.map { it.key })
        assertEquals(listOf("agent.listScripts", "agent.readManifest", "agent.execRegistered"), ToolCatalog.scriptRegisteredRun.hostMethods.map { it.key })
        assertTrue(ToolCatalog.scriptCatalog.gatedByGrant && ToolCatalog.scriptRegisteredRun.gatedByGrant)
        assertTrue(ToolCatalog.scriptCatalog.hints.readOnly)
        assertTrue(ToolCatalog.scriptRegisteredRun.hints.destructive && ToolCatalog.scriptRegisteredRun.hints.openWorld)
        assertEquals(300_000L, ToolCatalog.scriptRegisteredRun.maxTimeoutMs)
    }
}
