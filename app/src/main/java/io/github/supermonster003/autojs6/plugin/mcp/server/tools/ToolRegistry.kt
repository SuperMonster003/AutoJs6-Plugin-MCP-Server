package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ToolGate
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.types.McpException
import io.modelcontextprotocol.kotlin.sdk.types.RPCError
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Runs one tool call; the registry hands it the catalog row and the SDK request. */
fun interface ToolExecutor {

    suspend fun execute(spec: ToolSpec, connection: ClientConnection, request: CallToolRequest): CallToolResult
}

/** Runs one call of a self-described plugin tool (roadmap P12). */
fun interface PluginToolExecutor {

    suspend fun execute(tool: PluginToolInfo, connection: ClientConnection, request: CallToolRequest): CallToolResult
}

/**
 * Keeps the listed rows of the catalog in step with the group switches (roadmap P2.3, decision D6)
 * and with the plugin families the host reports (roadmap P11, decisions D27 / D28): [refresh]
 * re-reads both and sends one `notifications/tools/list_changed` per session when the listed set
 * changed, `tools/list` answers with that set in catalog order, and [disabledFailure] answers calls
 * to switched-off or unavailable tools through the tool gate.
 *
 * Every catalog row is registered on the SDK server once, on [install], and never removed: the SDK
 * notifies every session once per added or removed tool (also through `addTools` / `removeTools`),
 * so changing the SDK registration turned one change into a burst of about twice the catalog size of
 * `tools/list_changed` (seen on a device, 2026-10-15). A hidden row stays unreachable: the gate
 * answers it first, and its SDK handler asks [disabledFailure] again for a request that bypassed
 * the gate.
 *
 * A plugin-family row ([ToolGroup.isPluginFamily]) is listed only while its group is on, the open
 * session grants every host method it uses, and the host reports its family as available; the
 * [availability] probe supplies that state (cached for a few seconds) and starts out as
 * [PluginToolAvailability.NONE] on [install], so the rows join on the first [refresh], which the
 * tool gate runs before every `tools/list` and `tools/call`.
 *
 * The tools plugins describe themselves (roadmap P12, decisions D33 / D34) come from the same probe
 * ([PluginToolAvailability.tools]) and follow the plugin group switches ([ToolPermissions.allows]). They are listed
 * after the catalog rows, sorted by name, and are not registered on the SDK server at all, for the same reason as
 * above: the registry answers `tools/call` itself for every session ([call]), runs a catalog row through its SDK
 * registration and a plugin tool through [pluginExecutor]. A change of a listed plugin tool (it appears, goes, or its
 * plugin changes what clients see of it) is a listed-set change like any other.
 */
class ToolRegistry(
    private val catalog: List<ToolSpec>,
    private val permissions: () -> ToolPermissions,
    private val availability: suspend () -> PluginToolAvailability = { PluginToolAvailability.NONE },
    private val pluginExecutor: PluginToolExecutor = PluginToolExecutor { tool, _, _ -> ToolResults.failure(ToolFailure.internal("no executor for ${tool.name}")) },
    private val executor: ToolExecutor,
) : ToolGate {

    private val mutex = Mutex()

    @Volatile
    private var server: Server? = null

    @Volatile
    private var current: ToolPermissions = ToolPermissions.DEFAULT

    @Volatile
    private var currentAvailability: PluginToolAvailability = PluginToolAvailability.NONE

    private val registered = LinkedHashSet<String>()

    /** What clients see of the listed plugin tools; a change in any of them is a listed-set change. */
    private var registeredSignatures: List<String> = emptyList()

    @Volatile
    private var listedPluginTools: List<PluginToolInfo> = emptyList()

    /** The names `tools/list` currently shows: the catalog rows in catalog order, then the plugin tools by name. */
    val enabledNames: List<String>
        get() = synchronized(registered) { registered.toList() }

    /** The self-described plugin tools `tools/list` currently shows. */
    val pluginTools: List<PluginToolInfo>
        get() = listedPluginTools

    /** The plugin-family state the registration reflects, for `device_info` and the dump. */
    val pluginAvailability: PluginToolAvailability
        get() = currentAvailability

    /** Registers every catalog row on a fresh [server] and lists the enabled ones; called once per listener start. */
    fun install(server: Server) {
        this.server = server
        current = permissions()
        currentAvailability = PluginToolAvailability.NONE
        catalog.forEach { spec -> register(server, spec) }
        synchronized(registered) {
            listedPluginTools = emptyList()
            registeredSignatures = emptyList()
            registered.clear()
            wanted(current, currentAvailability).forEach { spec -> registered += spec.name }
        }
        server.onConnect {
            server.sessions.values.forEach { session ->
                session.setRequestHandler<ListToolsRequest>(Method.Defined.ToolsList) { request, _ ->
                    if (request.params?.cursor != null) throw McpException(RPCError.ErrorCode.INVALID_PARAMS, "Invalid tool cursor")
                    refresh()
                    ListToolsResult(
                        tools = wanted(current, currentAvailability).map { it.toSdkTool() } + listedPluginTools.map(PluginToolSpecs::toSdkTool),
                        meta = buildJsonObject { put("ttlMs", 0); put("cacheScope", "private") },
                    )
                }
                session.setRequestHandler<CallToolRequest>(Method.Defined.ToolsCall) { request, _ ->
                    call(server, session.sessionId, request)
                }
            }
        }
    }

    /**
     * Answers `tools/call` for one session: a catalog row through its SDK registration (as the SDK itself would), a
     * listed plugin tool through [pluginExecutor], anything else with the SDK's "not found".
     */
    private suspend fun call(server: Server, sessionId: String, request: CallToolRequest): CallToolResult {
        val name = request.params.name
        val connection = server.clientConnection(sessionId)
        server.tools[name]?.let { return runRegistered(it, connection, request) }
        disabledFailure(name)?.let { failure ->
            return ToolResults.failure(failure).let { if (hasOutputSchema(name)) it.copy(structuredContent = null) else it }
        }
        val tool = listedPluginTools.firstOrNull { it.name == name }
            ?: return CallToolResult(content = listOf(TextContent(text = "Tool $name not found")), isError = true)
        val result = try {
            pluginExecutor.execute(tool, connection, request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ToolResults.failure(ToolFailure.internal("${tool.name} failed: ${e.message ?: e.javaClass.simpleName}"))
        }
        return if (PluginToolSpecs.outputSchema(tool) != null && result.isError == true) result.copy(structuredContent = null) else result
    }

    /** What the SDK does for a registered tool: run its handler, and turn an exception into an `isError` result. */
    private suspend fun runRegistered(tool: RegisteredTool, connection: ClientConnection, request: CallToolRequest): CallToolResult = try {
        tool.handler.invoke(connection, request)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        CallToolResult(content = listOf(TextContent(text = "Error executing tool ${request.params.name}: ${e.message}")), isError = true)
    }

    fun detach() {
        server = null
        synchronized(registered) { registered.clear() }
    }

    /** Re-reads the switches and the plugin-family state; true when the listed set changed (and the clients were told once). */
    override suspend fun refresh(): Boolean = mutex.withLock {
        val target = server ?: return false
        val next = permissions()
        val nextAvailability = availability()
        current = next
        currentAvailability = nextAvailability
        val plugins = pluginToolsFor(next, nextAvailability)
        val names = wanted(next, nextAvailability).map { it.name } + plugins.map { it.name }
        val signatures = plugins.map(PluginToolSpecs::signature)
        val changed = synchronized(registered) {
            listedPluginTools = plugins
            if (names == registered.toList() && signatures == registeredSignatures) {
                false
            } else {
                registered.clear()
                registered.addAll(names)
                registeredSignatures = signatures
                true
            }
        }
        if (changed) {
            target.sessions.keys.toList().forEach { sessionId -> runCatching { target.sendToolListChanged(sessionId) } }
        }
        changed
    }

    override fun disabledFailure(toolName: String): ToolFailure? {
        val spec = catalog.firstOrNull { it.name == toolName } ?: return pluginToolFailure(toolName)
        if (!current.isEnabled(spec.group)) return ToolFailure.toolDisabled(spec.name, spec.group.id)
        return if (spec.group.isPluginFamily || spec.gatedByGrant) currentAvailability.unavailableFailure(spec) else null
    }

    override fun hasOutputSchema(toolName: String): Boolean {
        catalog.firstOrNull { it.name == toolName }?.let { return it.outputSchema != null }
        return reportedPluginTool(toolName)?.let { PluginToolSpecs.outputSchema(it) != null } == true
    }

    /** A self-described tool reads the screen when one of its image arguments is `"screen"` (roadmap P11 D39, applied to P12 tools). */
    override fun countsAsScreenshot(toolName: String, arguments: JsonElement?): Boolean {
        val tool = reportedPluginTool(toolName) ?: return false
        val values = arguments as? JsonObject ?: return false
        return tool.imageParameters.any { name ->
            (values[name] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim() == ToolCatalog.Images.SCREEN
        }
    }

    private fun reportedPluginTool(toolName: String): PluginToolInfo? =
        currentAvailability.tools?.tools?.firstOrNull { it.name == toolName }?.takeIf { catalog.none { spec -> spec.name == toolName } }

    /**
     * Why a self-described tool may not run: the host session is gone or does not grant `plugin_tools.invoke`, its
     * plugin group or the group's switch for tools that are not read-only is off, or its plugin is not available. Null
     * for a tool that may run and for names nobody reported.
     */
    private fun pluginToolFailure(toolName: String): ToolFailure? {
        val availability = currentAvailability
        val report = availability.tools ?: return null
        val tool = report.tools.firstOrNull { it.name == toolName }
        if (tool == null) {
            val shortName = toolName.substringBefore('_', "")
            val provider = report.providers.firstOrNull { it.shortName == shortName && !it.available } ?: return null
            return ToolFailure.pluginUnavailable(toolName, shortName, provider.status, provider.detail)
        }
        val granted = availability.granted ?: return ToolFailure.hostUnavailable()
        if (granted.isNotEmpty() && INVOKE_METHOD !in granted) {
            return ToolFailure(ToolErrorCodes.CAPABILITY_DENIED, "the host grant does not include $INVOKE_METHOD", ToolFailure.HINT_CAPABILITY,
                module = PluginToolInvocations.MODULE, method = PluginToolInvocations.INVOKE)
        }
        if (!current.isPluginGroupEnabled(tool.shortName, tool.userGranted)) return ToolFailure.toolDisabled(tool.name, tool.shortName)
        if (!tool.readOnly && !current.arePluginWritesEnabled(tool.shortName)) {
            return ToolFailure(
                ToolErrorCodes.TOOL_DISABLED,
                "tool ${tool.name} is a ${tool.risk} tool, and the ${tool.shortName} group does not allow tools that write, delete or reach the network",
                "enable that option of the ${tool.shortName} group in the plugin settings on the phone",
            )
        }
        return null
    }

    /** The plugin tools to list: reported by the host, granted for invocation, switched on, and not shadowing a catalog row. */
    private fun pluginToolsFor(permissions: ToolPermissions, availability: PluginToolAvailability): List<PluginToolInfo> {
        val report = availability.tools ?: return emptyList()
        val granted = availability.granted ?: return emptyList()
        if (granted.isNotEmpty() && INVOKE_METHOD !in granted) return emptyList()
        val builtIn = catalog.map { it.name }.toSet()
        return report.tools.filter { it.name !in builtIn && permissions.allows(it) }.sortedBy { it.name }
    }

    private fun wanted(permissions: ToolPermissions, availability: PluginToolAvailability): List<ToolSpec> =
        catalog.filter { spec -> permissions.isEnabled(spec.group) && ((!spec.group.isPluginFamily && !spec.gatedByGrant) || availability.allows(spec)) }

    private fun register(server: Server, spec: ToolSpec) {
        server.addTool(spec.toSdkTool()) { request ->
            val result = disabledFailure(spec.name)?.let(ToolResults::failure) ?: executor.execute(spec, this, request)
            ToolResults.forSpec(spec, result)
        }
    }

    private companion object {
        const val INVOKE_METHOD = "${PluginToolInvocations.MODULE}.${PluginToolInvocations.INVOKE}"
    }
}
