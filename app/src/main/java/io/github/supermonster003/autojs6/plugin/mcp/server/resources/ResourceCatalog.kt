package io.github.supermonster003.autojs6.plugin.mcp.server.resources

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.Locale

/** Read-only MCP views of existing host capabilities. No workspace data is cached or logged. */
class ResourceCatalog(
    private val callerFor: (clientName: String?) -> BridgeCaller,
    private val permissions: () -> ToolPermissions,
) {
    fun install(server: Server, clientNameOf: (String?) -> String?) {
        // The SDK's default list handler has no metadata/pagination hook, and its default
        // matcher supports single segments only. Use its public per-session handlers for
        // RFC 6570 reserved paths and dynamic group filtering, through the same transport.
        server.onConnect {
            server.sessions.values.forEach { session ->
                session.setRequestHandler<ListResourcesRequest>(Method.Defined.ResourcesList) { request, _ ->
                    list(request.params?.cursor, clientNameOf(session.sessionId))
                }
                session.setRequestHandler<ListResourceTemplatesRequest>(Method.Defined.ResourcesTemplatesList) { request, _ ->
                    if (request.params?.cursor != null) invalidCursor()
                    templates(clientNameOf(session.sessionId))
                }
                session.setRequestHandler<ReadResourceRequest>(Method.Defined.ResourcesRead) { request, _ ->
                    read(request.params.uri, clientNameOf(session.sessionId))
                }
            }
        }
    }

    /** The docs template is optional (D20): it appears only while the host reports an eligible Offline Docs plugin. */
    suspend fun templates(clientName: String? = null): ListResourceTemplatesResult = guarded {
        val templates = if (permissions().isEnabled(ToolGroup.FILES)) buildList {
            add(ResourceTemplate(ResourceUri.WORKSPACE_TEMPLATE, "workspace", "Read a workspace file. Encode each path segment; nested paths retain their slashes. Text and binary data are limited to 1 MiB."))
            add(ResourceTemplate(ResourceUri.SAMPLES_TEMPLATE, "samples", "Read a built-in host sample. A trailing slash lists a sample directory. Read autojs6://samples/ to discover paths."))
            if (docsAvailable(callerFor(clientName))) {
                add(ResourceTemplate(ResourceUri.DOCS_TEMPLATE, "docs", "Read a page of the offline AutoJs6 documentation provided by the installed Offline Docs plugin. A trailing slash lists a directory. Read autojs6://docs/ to discover pages."))
            }
        } else emptyList()
        ListResourceTemplatesResult(resourceTemplates = templates, meta = CACHE_META)
    }

    suspend fun list(cursor: String? = null, clientName: String? = null): ListResourcesResult = guarded {
        if (cursor != null && !CURSOR.matches(cursor)) invalidCursor()
        val policy = permissions()
        var sampleStatus = "disabled"
        var truncated = false
        var docsStatus = "disabled"
        var docsTruncated = false
        val resources = buildList {
            if (policy.isEnabled(ToolGroup.DEVICE)) add(Resource(ResourceUri.DEVICE, "device_info", "Current host device information", "application/json"))
            if (policy.isEnabled(ToolGroup.SCRIPT)) add(Resource(ResourceUri.CONSOLE, "console_tail", "The latest 100 host console entries", "application/json"))
            if (policy.isEnabled(ToolGroup.FILES)) {
                add(Resource(ResourceUri.SAMPLES, "samples", "Index of the host's built-in samples", "application/json"))
                try {
                    val index = hostDirectory("listSamples", ".", true, callerFor(clientName))
                    sampleStatus = if (index["available"]?.jsonPrimitive?.booleanOrNull == true) "available" else "unavailable"
                    truncated = index["truncated"]?.jsonPrimitive?.booleanOrNull == true
                    index["entries"]?.jsonArray.orEmpty().forEach { entry ->
                        val item = entry.jsonObject
                        if (item["type"]?.jsonPrimitive?.content == "file") {
                            val path = WorkspacePath.normalize(item["path"]!!.jsonPrimitive.content, false)
                            add(Resource(ResourceUri.sample(path), path, "Built-in AutoJs6 sample", representation(path).first))
                        }
                    }
                } catch (e: ToolFailureException) {
                    sampleStatus = e.failure.code
                }
                // Offline documentation is optional (D20): nothing is listed unless the host relays an eligible plugin.
                try {
                    val index = hostDirectory("listDocs", ".", true, callerFor(clientName))
                    if (index["available"]?.jsonPrimitive?.booleanOrNull == true) {
                        docsStatus = "available"
                        docsTruncated = index["truncated"]?.jsonPrimitive?.booleanOrNull == true
                        add(Resource(ResourceUri.DOCS, "docs", "Index of the offline AutoJs6 documentation (Offline Docs plugin)", "application/json"))
                        index["entries"]?.jsonArray.orEmpty().forEach { entry ->
                            val item = entry.jsonObject
                            if (item["type"]?.jsonPrimitive?.content == "file") {
                                val path = WorkspacePath.normalize(item["path"]!!.jsonPrimitive.content, false)
                                val mime = representation(path).first
                                // Pages only; stylesheets, scripts, images and fonts stay reachable through the index.
                                if (mime == "text/html") add(Resource(ResourceUri.doc(path), path, "Offline AutoJs6 documentation page", mime))
                            }
                        }
                    } else {
                        docsStatus = index["status"]?.jsonPrimitive?.takeIf { it.isString }?.content ?: "unavailable"
                    }
                } catch (e: ToolFailureException) {
                    docsStatus = e.failure.code
                }
            }
        }
        val signature = MessageDigest.getInstance("SHA-256").digest(resources.joinToString("\n") { it.uri }.toByteArray())
            .take(8).joinToString("") { "%02x".format(it.toInt() and 255) }
        val offset = if (cursor == null) 0 else {
            val parts = cursor.split(':')
            if (parts[1] != signature) invalidCursor()
            parts[2].toIntOrNull()?.takeIf { it in 1 until resources.size } ?: invalidCursor()
        }
        val end = (offset + PAGE_SIZE).coerceAtMost(resources.size)
        ListResourcesResult(resources.subList(offset, end),
            nextCursor = if (end < resources.size) "v1:$signature:$end" else null,
            meta = buildJsonObject {
                CACHE_META.forEach { (key, value) -> put(key, value) }
                put("sampleCatalogStatus", sampleStatus)
                put("sampleCatalogTruncated", truncated)
                put("docsCatalogStatus", docsStatus)
                put("docsCatalogTruncated", docsTruncated)
            })
    }

    suspend fun read(uri: String, clientName: String? = null): ReadResourceResult = guarded {
        val resource = ResourceUri.parse(uri)
        val group = when (resource.kind) {
            ResourceUri.Kind.WORKSPACE, ResourceUri.Kind.SAMPLES, ResourceUri.Kind.DOCS -> ToolGroup.FILES
            ResourceUri.Kind.DEVICE -> ToolGroup.DEVICE
            ResourceUri.Kind.CONSOLE -> ToolGroup.SCRIPT
        }
        requireEnabled(group)
        val caller = callerFor(clientName)
        val contents: ResourceContents = when (resource.kind) {
            ResourceUri.Kind.DEVICE -> TextResourceContents(
                caller.callOrThrow("device", "info", ScriptTools.NO_ARGS, TIMEOUT_MS, listOf("device")).toString(), uri, "application/json")
            ResourceUri.Kind.CONSOLE -> TextResourceContents(
                ScriptTools.shapeTail(caller.callOrThrow("console", "tail", ScriptTools.tailArgs(buildJsonObject { put("lines", 100) }),
                    TIMEOUT_MS, listOf("console"))).toString(), uri, "application/json")
            ResourceUri.Kind.SAMPLES if resource.directory -> directoryContents("listSamples", resource, uri, caller, ResourceUri::sample)
            ResourceUri.Kind.DOCS if resource.directory -> directoryContents("listDocs", resource, uri, caller, ResourceUri::doc)
            else -> {
                val (mime, encoding) = representation(resource.path)
                val result = if (resource.kind == ResourceUri.Kind.WORKSPACE) {
                    val args = ToolArguments.validate(ToolCatalog.filesRead, buildJsonObject {
                        put("path", resource.path); put("encoding", encoding); put("maxBytes", MAX_BYTES)
                    })
                    FileTools().planFor(ToolCatalog.filesRead, args)!!.run(caller).structured
                } else caller.callOrThrow("app", if (resource.kind == ResourceUri.Kind.DOCS) "readDoc" else "readSample", buildJsonArray {
                    add(resource.path); add(buildJsonObject { put("encoding", encoding); put("maxBytes", MAX_BYTES) })
                }, TIMEOUT_MS, listOf("app.query")).jsonObject
                val content = result["content"]?.jsonPrimitive?.takeIf { it.isString }?.content
                    ?: throw ToolFailureException(ToolFailure.internal("Invalid resource payload"))
                val meta = JsonObject(result.filterKeys { it in setOf("bytes", "totalBytes", "truncated", "encoding") })
                if (encoding == "base64") BlobResourceContents(content, uri, mime, meta)
                else TextResourceContents(content, uri, mime, meta)
            }
        }
        requireEnabled(group)
        ReadResourceResult(listOf(contents), CACHE_META)
    }

    /** A host directory index with the child URIs of [uriOf] and without the host's schema and asset root. */
    private suspend fun directoryContents(
        method: String, resource: ResourceUri, uri: String, caller: BridgeCaller, uriOf: (String, Boolean) -> String,
    ): TextResourceContents {
        val index = hostDirectory(method, resource.path, false, caller)
        val entries = index["entries"]?.jsonArray.orEmpty().map { entry ->
            val item = entry.jsonObject
            val path = WorkspacePath.normalize(item["path"]!!.jsonPrimitive.content, false)
            JsonObject(item + ("uri" to JsonPrimitive(uriOf(path, item["type"]?.jsonPrimitive?.content == "directory"))))
        }
        return TextResourceContents(JsonObject(index.filterKeys { it != "schema" && it != "root" } + ("entries" to JsonArray(entries))).toString(), uri, "application/json")
    }

    private suspend fun hostDirectory(method: String, path: String, recursive: Boolean, caller: BridgeCaller, maxEntries: Int = MAX_SAMPLE_ENTRIES): JsonObject =
        caller.callOrThrow("app", method, buildJsonArray {
            add(path); add(buildJsonObject { put("recursive", recursive); put("maxEntries", maxEntries) })
        }, TIMEOUT_MS, listOf("app.query")).jsonObject

    private suspend fun docsAvailable(caller: BridgeCaller): Boolean = try {
        hostDirectory("listDocs", ".", false, caller, maxEntries = 1)["available"]?.jsonPrimitive?.booleanOrNull == true
    } catch (_: ToolFailureException) {
        false
    }

    private fun requireEnabled(group: ToolGroup) {
        if (!permissions().isEnabled(group)) throw ToolFailureException(ToolFailure.toolDisabled("resource", group.id))
    }

    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: McpException) {
        throw e
    } catch (e: ToolArgumentException) {
        throw McpException(RPCError.ErrorCode.INVALID_PARAMS, "Invalid resource request", e.failure.toJson())
    } catch (e: ToolFailureException) {
        // The SDK logs exception messages. Keep host paths and content only in the response data.
        val code = if (e.failure.code == ToolErrorCodes.INVALID_ARGUMENTS) RPCError.ErrorCode.INVALID_PARAMS else -32000
        throw McpException(code, "Resource request failed: ${e.failure.code}", e.failure.toJson())
    } catch (_: Exception) {
        throw McpException(RPCError.ErrorCode.INTERNAL_ERROR, "Invalid host resource response")
    }

    companion object {
        const val MAX_BYTES = 1024 * 1024
        const val MAX_SAMPLE_ENTRIES = 2000
        const val PAGE_SIZE = 100
        const val TIMEOUT_MS = 15_000L
        val CACHE_META = buildJsonObject { put("ttlMs", 0); put("cacheScope", "private") }
        private val CURSOR = Regex("v1:[0-9a-f]{16}:[0-9]{1,4}")

        private fun invalidCursor(): Nothing = throw McpException(RPCError.ErrorCode.INVALID_PARAMS, "Invalid or expired resource cursor; restart resources/list")

        fun representation(path: String): Pair<String, String> = when (path.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "js", "mjs", "cjs" -> "text/javascript" to "utf-8"
            "json" -> "application/json" to "utf-8"
            "md" -> "text/markdown" to "utf-8"
            "html", "htm" -> "text/html" to "utf-8"
            "xml" -> "application/xml" to "utf-8"
            "txt", "csv", "yaml", "yml", "css", "sh", "ini", "properties" -> "text/plain" to "utf-8"
            "png" -> "image/png" to "base64"
            "jpg", "jpeg" -> "image/jpeg" to "base64"
            "webp" -> "image/webp" to "base64"
            else -> "application/octet-stream" to "base64"
        }
    }
}
