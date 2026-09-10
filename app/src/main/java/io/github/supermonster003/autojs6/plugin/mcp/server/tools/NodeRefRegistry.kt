package io.github.supermonster003.autojs6.plugin.mcp.server.tools

/**
 * The node references of the `ui` tools (roadmap D12, P3.2): `ui_dump` numbers the nodes it
 * lists `#n1`, `#n2`, ... and the action tools accept such a reference instead of a selector.
 * The registry keeps one snapshot: the newest `ui_dump` replaces it, `ui_find` / `ui_wait_for`
 * append their matches to it with the next numbers, so a reference stays valid until the next
 * dump or until it is [ttlMs] old. A reference carries the node's fingerprint (class, text,
 * description, id, bounds); `UiTools` relocates the node with it before acting and answers
 * `NODE_REF_STALE` when nothing matches any more.
 *
 * Pure Kotlin and thread-safe; `NodeRefRegistryTest` covers it.
 */
class NodeRefRegistry(
    private val clock: () -> Long = System::currentTimeMillis,
    private val ttlMs: Long = TTL_MS,
) {

    /** One registered node. */
    class Entry(val number: Int, val node: UiNodeInfo, val createdAt: Long) {

        val ref: String
            get() = refOf(number)
    }

    /** What [replace] / [append] registered: the snapshot id and the references, in node order. */
    class Registration(val snapshotId: String, val entries: List<Entry>) {

        val refs: List<String>
            get() = entries.map { it.ref }
    }

    /** The answer of [resolve]. */
    sealed class Resolution {

        data class Found(val entry: Entry, val snapshotId: String) : Resolution()

        /** [detail] completes the sentence `#n12 ...` of the `NODE_REF_STALE` message. */
        data class Stale(val detail: String) : Resolution()
    }

    private val lock = Any()
    private var snapshotCounter = 0
    private var snapshotId: String? = null
    private var nextNumber = 1
    private val entries = LinkedHashMap<Int, Entry>()

    /** The id of the current snapshot, or null before the first dump. */
    val currentSnapshotId: String?
        get() = synchronized(lock) { snapshotId }

    val size: Int
        get() = synchronized(lock) { entries.size }

    /** A new snapshot holding [nodes] as `#n1` ... ; every earlier reference is dropped. */
    fun replace(nodes: List<UiNodeInfo>): Registration = synchronized(lock) {
        entries.clear()
        nextNumber = 1
        snapshotId = "s${++snapshotCounter}"
        register(nodes)
    }

    /** Adds [nodes] to the current snapshot with the next numbers; starts a snapshot when there is none. */
    fun append(nodes: List<UiNodeInfo>): Registration = synchronized(lock) {
        if (snapshotId == null) {
            snapshotId = "s${++snapshotCounter}"
            nextNumber = 1
        }
        register(nodes)
    }

    /** Looks a reference (`#n12`, `n12`, or `12`) up in the current snapshot. */
    fun resolve(ref: String): Resolution {
        val number = parse(ref) ?: return Resolution.Stale("is not a node reference; ui_dump lists nodes as #n1, #n2, ...")
        synchronized(lock) {
            val id = snapshotId ?: return Resolution.Stale("refers to a node, but no ui_dump has been taken yet")
            val entry = entries[number] ?: return Resolution.Stale("is not in the current snapshot $id (${entries.size} nodes)")
            if (clock() - entry.createdAt > ttlMs) {
                entries.remove(number)
                return Resolution.Stale("expired; references live ${ttlMs / 1000} s")
            }
            return Resolution.Found(entry, id)
        }
    }

    fun clear() = synchronized(lock) {
        entries.clear()
        snapshotId = null
        nextNumber = 1
    }

    /** `s3: 37 refs` for the dump. */
    fun describe(): String = synchronized(lock) { "${snapshotId ?: "none"}: ${entries.size} refs" }

    private fun register(nodes: List<UiNodeInfo>): Registration {
        val now = clock()
        val added = nodes.map { node -> Entry(nextNumber++, node, now).also { entries[it.number] = it } }
        return Registration(snapshotId!!, added)
    }

    companion object {

        /** How long a reference stays valid after the dump or find that created it. */
        const val TTL_MS = 60_000L

        fun refOf(number: Int): String = "#n$number"

        /** The number of `#n12`, `n12`, or `12`; null for anything else. */
        fun parse(ref: String?): Int? {
            val trimmed = ref?.trim().orEmpty()
            val digits = when {
                trimmed.startsWith("#n") -> trimmed.substring(2)
                trimmed.startsWith("n") -> trimmed.substring(1)
                else -> trimmed
            }
            return digits.toIntOrNull()?.takeIf { it >= 1 }
        }
    }
}
