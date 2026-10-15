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

    /** The names `tools/list` currently shows, in catalog order. */
    val enabledNames: List<String>
        get() = synchronized(registered) { registered.toList() }

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
            registered.clear()
            wanted(current, currentAvailability).forEach { spec -> registered += spec.name }
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

    /** Re-reads the switches and the plugin-family state; true when the listed set changed (and the clients were told once). */
    override suspend fun refresh(): Boolean = mutex.withLock {
        val target = server ?: return false
        val next = permissions()
        val nextAvailability = availability()
        current = next
        currentAvailability = nextAvailability
        val names = wanted(next, nextAvailability).map { it.name }
        val changed = synchronized(registered) {
            if (names == registered.toList()) {
                false
            } else {
                registered.clear()
                registered.addAll(names)
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
        server.addTool(spec.toSdkTool()) { request ->
            val result = disabledFailure(spec.name)?.let(ToolResults::failure) ?: executor.execute(spec, this, request)
            ToolResults.forSpec(spec, result)
        }
    }
}
