package io.github.supermonster003.autojs6.plugin.mcp.server.resources

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.*
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ResourceCatalogTest {
    private var policy = ToolPermissions.DEFAULT
    private var sampleCount = 2
    private var failure: ToolFailure? = null
    private var afterCall: () -> Unit = {}
    private val requests = mutableListOf<BridgeRequest>()
    private val names = mutableListOf<String?>()
    private val catalog = ResourceCatalog({ clientName ->
        names += clientName
        BridgeCaller { module, method, args, timeout, permissions ->
            requests += BridgeRequest("test", module, method, args, timeout, permissions)
            afterCall()
            failure?.let { return@BridgeCaller BridgeOutcome.Failed(it) }
            val result = when ("$module.$method") {
                "app.listSamples" -> buildJsonObject {
                    put("schema", "samples-v1"); put("root", "sample"); put("available", true); put("truncated", false)
                    putJsonArray("entries") { repeat(sampleCount) { i -> add(buildJsonObject {
                        put("path", "目录/sample $i.js"); put("name", "sample $i.js"); put("type", "file")
                    }) } }
                }
                "files.read", "app.readSample" -> buildJsonObject {
                    val encoding = args[1].jsonObject["encoding"]!!.jsonPrimitive.content
                    put("schema", "file-v1"); put("root", "private-root"); put("encoding", encoding)
                    put("content", if (encoding == "base64") "AP8=" else "console.log('ok');")
                    put("bytes", 2); put("totalBytes", 9); put("truncated", true)
                }
                "console.tail" -> buildJsonObject { put("schema", "console-v1"); put("count", 0); putJsonArray("entries") {} }
                else -> buildJsonObject { put("api", 24) }
            }
            BridgeOutcome.Ok(result, 1)
        }
    }, { policy })

    @Test fun `fixed resource identities and reserved nested templates parse`() {
        assertEquals(ResourceUri.Kind.DEVICE, ResourceUri.parse(ResourceUri.DEVICE).kind)
        assertEquals(ResourceUri.Kind.CONSOLE, ResourceUri.parse(ResourceUri.CONSOLE).kind)
        assertTrue(ResourceUri.parse(ResourceUri.SAMPLES).directory)
        val uri = ResourceUri.sample("目录/a +#%.js")
        assertEquals("目录/a +#%.js", ResourceUri.parse(uri).path)
        assertEquals("a+b.js", ResourceUri.parse("autojs6://workspace/a+b.js").path)
        assertEquals("dir", ResourceUri.parse("autojs6://samples/dir/").path)
        assertEquals(listOf(ResourceUri.WORKSPACE_TEMPLATE, ResourceUri.SAMPLES_TEMPLATE), catalog.templates().resourceTemplates.map { it.uriTemplate })
    }

    @Test fun `malformed URI authority traversal separators and UTF8 never reach the bridge`() {
        listOf("file:///a", "autojs6:workspace/a", "autojs6://workspace/", "autojs6://workspace/a/",
            "autojs6://user@workspace/a", "autojs6://workspace:12/a", "autojs6://workspace/a?encoding=base64",
            "autojs6://workspace/a#b", "autojs6://workspace/../a", "autojs6://workspace/%2e%2e/a",
            "autojs6://workspace/./a", "autojs6://workspace/a//b", "autojs6://workspace/a%2fb",
            "autojs6://workspace/a%5cb", "autojs6://workspace/a%00b", "autojs6://workspace/%ff",
            "autojs6://workspace/%c0%af", "autojs6://workspace/C%3Aa", "autojs6://workspace/a%", "autojs6://workspace/a b")
            .forEach { uri -> assertThrows(uri, McpException::class.java) { runBlocking { catalog.read(uri) } } }
        assertTrue(requests.isEmpty())
    }

    @Test fun `percent escapes are decoded once and URI and path budgets both apply`() {
        assertEquals("%2e%2e/a.js", ResourceUri.parse("autojs6://workspace/%252e%252e/a.js").path)
        assertThrows(McpException::class.java) { runBlocking { catalog.read("autojs6://workspace/" + "a".repeat(4097)) } }
        assertThrows(McpException::class.java) { ResourceUri.parse("autojs6://workspace/" + "%61".repeat(5000)) }
        assertThrows(McpException::class.java) { runBlocking { catalog.read(ResourceUri.sample("中".repeat(1366))) } }
    }

    @Test fun `workspace resource reuses files read and preserves truncation metadata`() = runBlocking {
        val result = catalog.read("autojs6://workspace/dir/a.js", "test-client")
        val text = result.contents.single() as TextResourceContents
        assertEquals("console.log('ok');", text.text)
        assertEquals("text/javascript", text.mimeType)
        assertTrue(text.meta!!["truncated"]!!.jsonPrimitive.boolean)
        assertFalse(text.meta!!.containsKey("root"))
        assertEquals("files", requests.single().module)
        assertEquals("read", requests.single().method)
        assertEquals(JsonPrimitive(1048576), requests.single().args[1].jsonObject["maxBytes"])
        assertEquals(listOf("files"), requests.single().permissions)
        assertEquals(listOf("test-client"), names)
    }

    @Test fun `binary samples and workspace entries use blob contents`() = runBlocking {
        for (uri in listOf("autojs6://workspace/a.bin", "autojs6://samples/目录/a.png")) {
            assertEquals("AP8=", (catalog.read(uri).contents.single() as BlobResourceContents).blob)
        }
        assertEquals("readSample", requests.last().method)
        assertEquals(listOf("app.query"), requests.last().permissions)
        assertEquals(JsonPrimitive("base64"), requests.last().args[1].jsonObject["encoding"])
    }

    @Test fun `sample directory gives encoded child URIs without exposing asset roots`() = runBlocking {
        val text = catalog.read(ResourceUri.SAMPLES).contents.single() as TextResourceContents
        val root = Json.parseToJsonElement(text.text).jsonObject
        assertFalse(root.containsKey("root")); assertFalse(root.containsKey("schema"))
        val uri = root["entries"]!!.jsonArray.first().jsonObject["uri"]!!.jsonPrimitive.content
        assertEquals("目录/sample 0.js", ResourceUri.parse(uri).path)
        assertEquals(JsonPrimitive(false), requests.single().args[1].jsonObject["recursive"])
    }

    @Test fun `resource listing pages all samples and has private zero TTL metadata`() = runBlocking {
        sampleCount = 205
        var cursor: String? = null
        val uris = mutableListOf<String>()
        do {
            val page = catalog.list(cursor)
            assertTrue(page.resources.size <= 100)
            assertEquals(JsonPrimitive(0), page.meta!!["ttlMs"])
            assertEquals(JsonPrimitive("private"), page.meta!!["cacheScope"])
            uris += page.resources.map { it.uri }
            cursor = page.nextCursor
        } while (cursor != null)
        assertEquals(208, uris.size); assertEquals(uris.size, uris.distinct().size)
        assertTrue(uris.none { it.startsWith("autojs6://workspace/") })
    }

    @Test fun `stale malformed and out of range resource cursors are refused`(): Unit = runBlocking {
        sampleCount = 110
        val cursor = catalog.list().nextCursor!!
        sampleCount = 109
        assertThrows(McpException::class.java) { runBlocking { catalog.list(cursor) } }
        assertThrows(McpException::class.java) { runBlocking { catalog.list("0") } }
        assertThrows(McpException::class.java) { runBlocking { catalog.list("v1:0000000000000000:9999") } }
    }

    @Test fun `group switches filter discovery and refuse reads before dispatch`() = runBlocking {
        policy = policy.with(ToolGroup.FILES, false).with(ToolGroup.DEVICE, false).with(ToolGroup.SCRIPT, false)
        assertTrue(catalog.templates().resourceTemplates.isEmpty())
        assertTrue(catalog.list().resources.isEmpty())
        for (uri in listOf(ResourceUri.DEVICE, ResourceUri.CONSOLE, ResourceUri.SAMPLES, "autojs6://workspace/a.js")) {
            val error = assertThrows(McpException::class.java) { runBlocking { catalog.read(uri) } }
            assertEquals("TOOL_DISABLED", error.data!!.jsonObject["code"]!!.jsonPrimitive.content)
        }
        assertTrue(requests.isEmpty())
    }

    @Test fun `switch revocation during a read also withholds its result`() {
        afterCall = { policy = policy.with(ToolGroup.DEVICE, false) }
        val error = assertThrows(McpException::class.java) { runBlocking { catalog.read(ResourceUri.DEVICE) } }
        assertEquals("TOOL_DISABLED", error.data!!.jsonObject["code"]!!.jsonPrimitive.content)
    }

    @Test fun `console resource uses bounded tail and strips its internal schema`() = runBlocking {
        val text = catalog.read(ResourceUri.CONSOLE).contents.single() as TextResourceContents
        assertFalse(Json.parseToJsonElement(text.text).jsonObject.containsKey("schema"))
        assertEquals(JsonPrimitive(100), requests.single().args.single().jsonObject["lines"])
        assertEquals(listOf("console"), requests.single().permissions)
    }

    @Test fun `host failure keeps discovery usable and messages safe for SDK logs`() = runBlocking {
        failure = ToolFailure.hostUnavailable()
        val page = catalog.list()
        assertEquals(3, page.resources.size)
        assertEquals(JsonPrimitive("HOST_UNAVAILABLE"), page.meta!!["sampleCatalogStatus"])
        failure = ToolFailure(ToolErrorCodes.HOST_ERROR, "secret path /private/workspace/example.js")
        val error = assertThrows(McpException::class.java) { runBlocking { catalog.read(ResourceUri.DEVICE) } }
        assertFalse(error.toString().contains("/private")); assertNull(error.cause)
        assertTrue(error.data.toString().contains("/private"))
    }

    @Test fun `unknown resources are not found and cancellation remains cancellation`() {
        val error = assertThrows(McpException::class.java) { runBlocking { catalog.read("autojs6://docs/a.html") } }
        assertEquals(-32002, error.code)
        afterCall = { throw CancellationException("cancel") }
        assertThrows(CancellationException::class.java) { runBlocking { catalog.read(ResourceUri.DEVICE) } }
    }
}
