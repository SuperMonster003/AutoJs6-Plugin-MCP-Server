package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ToolGate
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.types.McpException
import io.modelcontextprotocol.kotlin.sdk.types.RPCError
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Runs one tool call; the registry hands it the catalog row and the SDK request. */
fun interface ToolExecutor {

    suspend fun execute(spec: ToolSpec, connection: ClientConnection, request: CallToolRequest): CallToolResult
}

/**
 * Registers the enabled rows of the catalog on the SDK server, in catalog order, and keeps the
 * registration in step with the group switches (roadmap P2.3, decision D6) and with the plugin
 * families the host reports (roadmap P11, decisions D27 / D28): [refresh] re-reads both, adds and
 * removes tools and sends `notifications/tools/list_changed` when something changed, and
 * [disabledFailure] answers calls to switched-off or unavailable tools through the tool gate.
 *
 * A plugin-family row ([ToolGroup.isPluginFamily]) is listed only while its group is on, the open
 * session grants every host method it uses, and the host reports its family as available; the
 * [availability] probe supplies that state (cached for a few seconds) and starts out as
 * [PluginToolAvailability.NONE] on [install], so the rows join on the first [refresh], which the
 * tool gate runs before every `tools/list` and `tools/call`.
 */
class ToolRegistry(
    private val catalog: List<ToolSpec>,
    private val permissions: () -> ToolPermissions,
    private val availability: suspend () -> PluginToolAvailability = { PluginToolAvailability.NONE },
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

    /** The names currently registered on the SDK server, in list order. */
    val enabledNames: List<String>
        get() = synchronized(registered) { registered.toList() }

    /** The plugin-family state the registration reflects, for `device_info` and the dump. */
    val pluginAvailability: PluginToolAvailability
        get() = currentAvailability

    /** Registers the enabled tools on a fresh [server]; called once per listener start. */
    fun install(server: Server) {
        this.server = server
        current = permissions()
        currentAvailability = PluginToolAvailability.NONE
        synchronized(registered) {
            registered.clear()
            wanted(current, currentAvailability).forEach { spec -> register(server, spec) }
        }
        server.onConnect {
            server.sessions.values.forEach { session ->
                session.setRequestHandler<ListToolsRequest>(Method.Defined.ToolsList) { request, _ ->
                    if (request.params?.cursor != null) throw McpException(RPCError.ErrorCode.INVALID_PARAMS, "Invalid tool cursor")
                    refresh()
                    ListToolsResult(
                        tools = wanted(current, currentAvailability).map { it.toSdkTool() },
                        meta = buildJsonObject { put("ttlMs", 0); put("cacheScope", "private") },
                    )
                }
            }
        }
    }

    fun detach() {
        server = null
        synchronized(registered) { registered.clear() }
    }

    /** Re-reads the switches and the plugin-family state; true when the registration changed (and the clients were told). */
    override suspend fun refresh(): Boolean = mutex.withLock {
        val target = server ?: return false
        val next = permissions()
        val nextAvailability = availability()
        current = next
        currentAvailability = nextAvailability
        val wanted = wanted(next, nextAvailability)
        val changed = synchronized(registered) {
            if (wanted.map { it.name } == registered.toList()) {
                false
            } else {
                // Re-register from scratch so tools/list keeps the catalog order after a group comes back.
                registered.toList().forEach { name -> target.removeTool(name) }
                registered.clear()
                wanted.forEach { spec -> register(target, spec) }
                true
            }
        }
        if (changed) {
            target.sessions.keys.toList().forEach { sessionId -> runCatching { target.sendToolListChanged(sessionId) } }
        }
        changed
    }

    override fun disabledFailure(toolName: String): ToolFailure? {
        val spec = catalog.firstOrNull { it.name == toolName } ?: return null
        if (!current.isEnabled(spec.group)) return ToolFailure.toolDisabled(spec.name, spec.group.id)
        return if (spec.group.isPluginFamily || spec.gatedByGrant) currentAvailability.unavailableFailure(spec) else null
    }

    private fun wanted(permissions: ToolPermissions, availability: PluginToolAvailability): List<ToolSpec> =
        catalog.filter { spec -> permissions.isEnabled(spec.group) && ((!spec.group.isPluginFamily && !spec.gatedByGrant) || availability.allows(spec)) }

    private fun register(server: Server, spec: ToolSpec) {
        server.addTool(spec.toSdkTool()) { request -> ToolResults.forSpec(spec, executor.execute(spec, this, request)) }
        registered += spec.name
    }
}
