package io.github.supermonster003.autojs6.plugin.mcp.server.tools

/**
 * The host snapshots behind the `#n` references (roadmap P10, host decision D26): `ui_dump` asks
 * AutoJs6 for a compact dump, AutoJs6 keeps the snapshot (the newest [HOST_SNAPSHOTS] per session,
 * least recently used first out) and relocates a reference itself when an action names it as
 * `{nodeRef, snapshotId}`, answering `NODE_REF_STALE` when the window changed or the node no longer
 * matches. The plugin only remembers the newest snapshot, so an action without `snapshotId` means
 * the newest dump, and the positions of its rows for `screen_capture(annotate: true)`.
 *
 * The snapshots belong to one host session; [clear] forgets them when the listener stops or a new
 * session replaces the old one. Pure Kotlin and thread-safe; `HostSnapshotsTest` covers it.
 */
class HostSnapshots(private val clock: () -> Long = System::currentTimeMillis) {

    /**
     * One dump line: its reference and position. A node with children shows its bounds; a leaf
     * shows only its centre, so [bounds] is then a [LEAF_MARK_PX] square around that point.
     */
    data class Row(val ref: String, val bounds: Bounds, val leaf: Boolean, val hidden: Boolean)

    /** The newest dump: the host's snapshot id, when it was taken, and its rows. */
    class Snapshot(val id: String, val takenAt: Long, val rows: List<Row>)

    @Volatile
    private var current: Snapshot? = null

    /** The newest snapshot, or null before the first dump of this session. */
    val latest: Snapshot?
        get() = current

    /** Remembers the snapshot [id] the host returned with the compact [text] of a dump. */
    fun record(id: String, text: String): Snapshot = Snapshot(id, clock(), parseRows(text)).also { current = it }

    fun clear() {
        current = null
    }

    /** `3a1f...:7: 37 rows` for the dump. */
    fun describe(): String = current?.let { "${it.id}: ${it.rows.size} rows" } ?: "none"

    companion object {

        /** Snapshots the host keeps per session (`ThreeStoveAgentContract.MAX_SNAPSHOTS_PER_LINK`). */
        const val HOST_SNAPSHOTS = 8

        /** Side of the square drawn for a leaf, whose dump line gives only its centre. */
        const val LEAF_MARK_PX = 32

        /** Longest snapshot id the host accepts. */
        const val MAX_SNAPSHOT_ID_LENGTH = 80

        private val REF = Regex("(?:#?n)?([1-9][0-9]{0,3})")

        private val LINE = Regex("""^#n([1-9][0-9]{0,3}) +(.*) (?:\[(-?\d+),(-?\d+)]\[(-?\d+),(-?\d+)]|c=\((-?\d+),(-?\d+)\))$""")

        private val MARKERS = setOf("clickable", "long_clickable", "checkable", "checked", "scrollable", "editable", "focused", "selected", "!enabled", "hidden")

        /** `#n12` for `#n12`, `n12`, or `12` (what the host accepts); null for anything else. */
        fun normalizeRef(ref: String?): String? = REF.matchEntire(ref?.trim().orEmpty())?.let { "#n${it.groupValues[1]}" }

        /** The rows of a compact dump, in reference order; header, footer, and unreadable lines are skipped. */
        fun parseRows(text: String): List<Row> = text.lineSequence().mapNotNull { line ->
            val match = LINE.matchEntire(line.trimEnd()) ?: return@mapNotNull null
            val groups = match.groupValues
            // The class name and the markers come before any quoted text, so a text containing "hidden" does not count.
            val markers = groups[2].trimStart().split(' ').drop(1).takeWhile { it in MARKERS }
            val leaf = groups[3].isEmpty()
            val bounds = if (leaf) {
                val x = groups[7].toInt()
                val y = groups[8].toInt()
                Bounds(x - LEAF_MARK_PX / 2, y - LEAF_MARK_PX / 2, x + LEAF_MARK_PX / 2, y + LEAF_MARK_PX / 2)
            } else {
                Bounds(groups[3].toInt(), groups[4].toInt(), groups[5].toInt(), groups[6].toInt())
            }
            Row("#n${groups[1]}", bounds, leaf, "hidden" in markers)
        }.toList()
    }
}
