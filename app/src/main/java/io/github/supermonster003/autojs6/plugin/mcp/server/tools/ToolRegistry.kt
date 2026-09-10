package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.server.ToolGate
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Runs one tool call; the registry hands it the catalog row and the SDK request. */
fun interface ToolExecutor {

    suspend fun execute(spec: ToolSpec, connection: ClientConnection, request: CallToolRequest): CallToolResult
}

/**
 * Registers the enabled rows of the catalog on the SDK server, in catalog order, and keeps the
 * registration in step with the group switches (roadmap P2.3, decision D6): [refresh] adds and
 * removes tools and sends `notifications/tools/list_changed` when something changed, and
 * [disabledFailure] answers calls to switched-off tools through the tool gate.
 */
class ToolRegistry(
    private val catalog: List<ToolSpec>,
    private val permissions: () -> ToolPermissions,
    private val executor: ToolExecutor,
) : ToolGate {

    private val mutex = Mutex()

    @Volatile
    private var server: Server? = null

    @Volatile
    private var current: ToolPermissions = ToolPermissions.DEFAULT

    private val registered = LinkedHashSet<String>()

    /** The names currently registered on the SDK server, in list order. */
    val enabledNames: List<String>
        get() = synchronized(registered) { registered.toList() }

    /** Registers the enabled tools on a fresh [server]; called once per listener start. */
    fun install(server: Server) {
        this.server = server
        current = permissions()
        synchronized(registered) {
            registered.clear()
            catalog.filter { current.isEnabled(it.group) }.forEach { spec -> register(server, spec) }
        }
    }

    fun detach() {
        server = null
        synchronized(registered) { registered.clear() }
    }

    /** Re-reads the switches; true when the registration changed (and the clients were told). */
    override suspend fun refresh(): Boolean = mutex.withLock {
        val target = server ?: return false
        val next = permissions()
        if (next.effective == current.effective) return false
        current = next
        val wanted = catalog.filter { next.isEnabled(it.group) }
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
        return if (current.isEnabled(spec.group)) null else ToolFailure.toolDisabled(spec.name, spec.group.id)
    }

    private fun register(server: Server, spec: ToolSpec) {
        server.addTool(spec.toSdkTool()) { request -> executor.execute(spec, this, request) }
        registered += spec.name
    }
}
