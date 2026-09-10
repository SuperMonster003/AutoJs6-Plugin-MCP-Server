package io.github.supermonster003.autojs6.plugin.mcp.server.bridge

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostBridgeClientTest {

    private class FakeTransport(
        override val brokerInfo: BrokerInfo = BrokerInfo(
            brokerId = "broker-1",
            methods = setOf("device.info", "engines.execScript", "slow.call", "accessibility.dump"),
            maxConcurrentCalls = 2,
            defaultTimeoutMs = 1_000L,
            maxTimeoutMs = 5_000L,
        ),
    ) : BridgeTransport {

        val requests = mutableListOf<BridgeRequest>()
        val clientNames = mutableListOf<String?>()
        val pendingReplies = mutableListOf<Pair<BridgeRequest, (BridgeReply) -> Unit>>()
        var deathListener: (() -> Unit)? = null
        var linkResult = true
        var unlinked = false

        var handler: (BridgeRequest, (BridgeReply) -> Unit) -> Unit = { request, reply ->
            reply(BridgeReply("""{"id":"${request.id}","ok":true,"result":{"echo":"${request.method}"}}""", true, null))
        }

        override fun dispatch(request: BridgeRequest, clientName: String?, onReply: (BridgeReply) -> Unit) {
            requests += request
            clientNames += clientName
            handler(request, onReply)
        }

        override fun linkToDeath(onDeath: () -> Unit): Boolean {
            deathListener = onDeath
            return linkResult
        }

        override fun unlinkToDeath() {
            unlinked = true
        }

        fun hold(): FakeTransport = apply { handler = { request, reply -> pendingReplies += request to reply } }

        fun failWith(category: String, module: String? = null, name: String = "Error", message: String = "boom"): FakeTransport = apply {
            handler = { request, reply ->
                val moduleJson = module?.let { ""","module":"$it"""" }.orEmpty()
                reply(
                    BridgeReply(
                        """{"id":"${request.id}","ok":false,"error":{"name":"$name","message":"$message","code":"host-code","category":"$category"$moduleJson,"method":"${request.method}"}}""",
                        false,
                        message,
                    ),
                )
            }
        }
    }

    @Test
    fun `successful call returns the result and sends the bridge envelope`() = runBlocking {
        val transport = FakeTransport()
        val client = HostBridgeClient(transport)
        val args = JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive(2)))

        val outcome = client.call("device", "info", args, timeoutMs = 700L, permissions = listOf("device"), clientName = "Claude Code")

        val ok = outcome as BridgeOutcome.Ok
        assertEquals("info", ok.result.jsonObject["echo"]?.jsonPrimitive?.content)
        assertEquals(HostAvailability.AVAILABLE, client.availability)
        assertEquals(listOf("Claude Code"), transport.clientNames)
        val envelope = Json.parseToJsonElement(transport.requests.single().toJson()).jsonObject
        assertEquals("mcp-1", envelope["id"]?.jsonPrimitive?.content)
        assertEquals("device", envelope["module"]?.jsonPrimitive?.content)
        assertEquals("info", envelope["method"]?.jsonPrimitive?.content)
        assertEquals(args, envelope["args"]?.jsonArray)
        assertEquals(700L, envelope["timeoutMs"]?.jsonPrimitive?.content?.toLong())
        assertEquals(listOf("device"), envelope["permissions"]?.jsonArray?.map { it.jsonPrimitive.content })
        assertEquals(1L, client.callCount)
    }

    @Test
    fun `request ids increase monotonically and timeouts are clamped to the grant`() = runBlocking {
        val transport = FakeTransport()
        val client = HostBridgeClient(transport)

        repeat(3) { client.call("device", "info", timeoutMs = 60_000L) }

        assertEquals(listOf("mcp-1", "mcp-2", "mcp-3"), transport.requests.map { it.id })
        assertTrue(transport.requests.all { it.timeoutMs == 5_000L })
        assertEquals(3L, client.callCount)
    }

    @Test
    fun `a host that never answers yields TIMEOUT after the grace period`() = runBlocking {
        val transport = FakeTransport().hold()
        val client = HostBridgeClient(transport)

        val startedAt = System.currentTimeMillis()
        val outcome = client.call("slow", "call", timeoutMs = 50L)
        val elapsed = System.currentTimeMillis() - startedAt

        val failure = (outcome as BridgeOutcome.Failed).failure
        assertEquals(ToolErrorCodes.TIMEOUT, failure.code)
        assertEquals("slow", failure.module)
        assertEquals("call", failure.method)
        assertTrue("waited $elapsed ms", elapsed >= 50L && elapsed < HostBridgeClient.GRACE_MS + 2_000L)
        assertEquals(HostAvailability.AVAILABLE, client.availability)
    }

    @Test
    fun `host death fails in-flight calls and degrades the client`() = runBlocking {
        val transport = FakeTransport().hold()
        var died: HostBridgeClient? = null
        val client = HostBridgeClient(transport, onDeath = { died = it })

        val inFlight: Deferred<BridgeOutcome> = async { client.call("slow", "call", timeoutMs = 4_000L) }
        while (transport.pendingReplies.isEmpty()) delay(10)
        transport.deathListener!!.invoke()

        val failure = (inFlight.await() as BridgeOutcome.Failed).failure
        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, failure.code)
        assertEquals(HostAvailability.DEGRADED, client.availability)
        assertTrue(died === client)

        val later = client.call("device", "info")
        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, (later as BridgeOutcome.Failed).failure.code)
        assertEquals("the second call must not reach the host", 1, transport.requests.size)
    }

    @Test
    fun `a failed death link degrades the client at once`() {
        val transport = FakeTransport().apply { linkResult = false }
        var died = false
        val client = HostBridgeClient(transport, onDeath = { died = true })

        assertEquals(HostAvailability.DEGRADED, client.availability)
        assertTrue(died)
    }

    @Test
    fun `host error categories map onto the plugin codes`() = runBlocking {
        val expectations = listOf(
            Triple(BridgeError.CATEGORY_UNAVAILABLE, "accessibility", ToolErrorCodes.A11Y_SERVICE_NOT_RUNNING),
            Triple(BridgeError.CATEGORY_UNAVAILABLE, "device", ToolErrorCodes.HOST_UNAVAILABLE),
            Triple(BridgeError.CATEGORY_PROCESS_DEAD, "device", ToolErrorCodes.HOST_UNAVAILABLE),
            Triple(BridgeError.CATEGORY_CAPABILITY_DENIED, "device", ToolErrorCodes.CAPABILITY_DENIED),
            Triple(BridgeError.CATEGORY_PERMISSION_DENIED, "device", ToolErrorCodes.CAPABILITY_DENIED),
            Triple(BridgeError.CATEGORY_RESOURCE_LIMIT, "device", ToolErrorCodes.LIMIT_EXCEEDED),
            Triple(BridgeError.CATEGORY_RATE_LIMITED, "device", ToolErrorCodes.RATE_LIMITED),
            Triple(BridgeError.CATEGORY_TIMEOUT, "device", ToolErrorCodes.TIMEOUT),
            Triple(BridgeError.CATEGORY_INVALID_REQUEST, "device", ToolErrorCodes.INVALID_ARGUMENTS),
            Triple(BridgeError.CATEGORY_PROVIDER_FAILED, "device", ToolErrorCodes.HOST_ERROR),
            Triple(BridgeError.CATEGORY_RUNTIME_ERROR, "device", ToolErrorCodes.HOST_ERROR),
            Triple("something-new", "device", ToolErrorCodes.HOST_ERROR),
        )
        expectations.forEach { (category, module, expectedCode) ->
            val transport = FakeTransport().failWith(category, module, name = "TypeError", message = "nope")
            val outcome = HostBridgeClient(transport).call("device", "info")
            val failure = (outcome as BridgeOutcome.Failed).failure
            assertEquals("category $category", expectedCode, failure.code)
            assertEquals(category, failure.category)
            assertEquals("host-code", failure.hostCode)
            assertEquals("TypeError: nope", failure.message)
            assertEquals(module, failure.module)
        }
    }

    @Test
    fun `calls beyond the concurrency ceiling fail with LIMIT_EXCEEDED after the queue wait`() = runBlocking {
        val transport = FakeTransport().hold()
        val client = HostBridgeClient(transport)
        val first = async { client.call("slow", "call", timeoutMs = 4_000L) }
        val second = async { client.call("slow", "call", timeoutMs = 4_000L) }
        while (transport.pendingReplies.size < 2) delay(10)

        val startedAt = System.currentTimeMillis()
        val third = client.call("slow", "call", timeoutMs = 100L)
        val elapsed = System.currentTimeMillis() - startedAt

        val failure = (third as BridgeOutcome.Failed).failure
        assertEquals(ToolErrorCodes.LIMIT_EXCEEDED, failure.code)
        assertTrue("waited $elapsed ms", elapsed >= 100L && elapsed < 3_000L)
        assertEquals("the queued call must not be dispatched", 2, transport.requests.size)
        transport.pendingReplies.forEach { (request, reply) -> reply(BridgeReply("""{"id":"${request.id}","ok":true,"result":1}""", true, null)) }
        assertTrue(first.await() is BridgeOutcome.Ok)
        assertTrue(second.await() is BridgeOutcome.Ok)
    }

    @Test
    fun `a method outside the grant is refused without a dispatch`() = runBlocking {
        val transport = FakeTransport()
        val outcome = HostBridgeClient(transport).call("shell", "exec")

        val failure = (outcome as BridgeOutcome.Failed).failure
        assertEquals(ToolErrorCodes.CAPABILITY_DENIED, failure.code)
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun `an empty grant list means the host decides`() = runBlocking {
        val transport = FakeTransport(BrokerInfo(methods = emptySet()))
        val outcome = HostBridgeClient(transport).call("shell", "exec")

        assertTrue(outcome is BridgeOutcome.Ok)
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun `detach fails pending calls and refuses new ones`() = runBlocking {
        val transport = FakeTransport().hold()
        val client = HostBridgeClient(transport)
        val inFlight = async { client.call("slow", "call", timeoutMs = 4_000L) }
        while (transport.pendingReplies.isEmpty()) delay(10)

        client.detach()

        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, (inFlight.await() as BridgeOutcome.Failed).failure.code)
        assertEquals(HostAvailability.DETACHED, client.availability)
        assertTrue(transport.unlinked)
        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, (client.call("device", "info") as BridgeOutcome.Failed).failure.code)
    }

    @Test
    fun `an unreadable reply becomes HOST_ERROR carrying the bundle message`() = runBlocking {
        val transport = FakeTransport().apply { handler = { _, reply -> reply(BridgeReply("not json", false, "broker exploded")) } }
        val outcome = HostBridgeClient(transport).call("device", "info")

        val failure = (outcome as BridgeOutcome.Failed).failure
        assertEquals(ToolErrorCodes.HOST_ERROR, failure.code)
        assertEquals("broker exploded", failure.message)
        assertEquals(BridgeError.CODE_PLUGIN_MALFORMED, failure.hostCode)
    }

    @Test
    fun `a dispatch that throws becomes HOST_UNAVAILABLE`() = runBlocking {
        val transport = FakeTransport().apply { handler = { _, _ -> throw IllegalStateException("binder gone") } }
        val outcome = HostBridgeClient(transport).call("device", "info")

        val failure = (outcome as BridgeOutcome.Failed).failure
        assertEquals(ToolErrorCodes.HOST_UNAVAILABLE, failure.code)
        assertEquals("binder gone", failure.message)
    }

    @Test
    fun `payload descriptors of a reply are closed`() = runBlocking {
        var closed = false
        val transport = FakeTransport().apply {
            handler = { request, reply ->
                reply(BridgeReply("""{"id":"${request.id}","ok":true,"result":{"payload":{"kind":"descriptor"}}}""", true, null, BridgePayload(12L, "image/jpeg") { closed = true }))
            }
        }
        val outcome = HostBridgeClient(transport).call("device", "info")

        assertTrue(outcome is BridgeOutcome.Ok)
        assertTrue(closed)
        assertNull((outcome as BridgeOutcome.Ok).result.jsonObject["missing"])
        assertFalse(transport.unlinked)
    }

    @Test
    fun `the broker info is exposed and the grant lookup works`() {
        val client = HostBridgeClient(FakeTransport())

        assertEquals("broker-1", client.info.brokerId)
        assertTrue(client.isGranted("device", "info"))
        assertFalse(client.isGranted("shell", "exec"))
        assertTrue(client.info.supportsBrokerContract)
        assertTrue(JsonObject(emptyMap()).isEmpty())
    }
}
