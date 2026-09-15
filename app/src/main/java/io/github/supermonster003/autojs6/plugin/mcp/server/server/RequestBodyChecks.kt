package io.github.supermonster003.autojs6.plugin.mcp.server.server

/**
 * Structural checks on a buffered POST body before any JSON parser sees it (roadmap P6).
 *
 * The nesting depth of arrays and objects is capped because `kotlinx.serialization` recurses once
 * per level: a body of a few thousand `[` would overflow the stack of the parsing thread, and on
 * Android an uncaught `StackOverflowError` in a coroutine ends the process. The scan runs on the
 * raw bytes, skips string literals (with their escapes), and stops as soon as the limit is
 * exceeded, so it costs one pass over at most the body ceiling. A batch that repeats a request id
 * is refused as well, because the transport keys the streams of a POST by request id and a
 * repeated id would leave one of the two requests without an answer.
 */
object RequestBodyChecks {

    /** Deeper JSON than this is refused; the plugin's own documents nest fewer than ten levels. */
    const val MAX_JSON_DEPTH = 64

    /** The JSON-RPC message of a body nested deeper than [MAX_JSON_DEPTH]. */
    const val MESSAGE_TOO_DEEP = "Request body nests deeper than $MAX_JSON_DEPTH levels"

    const val MESSAGE_DUPLICATE_ID = "Request ids within one body must be unique"

    /**
     * The deepest nesting of `[` / `{` outside string literals in [bytes], or `limit + 1` as soon
     * as the nesting exceeds [limit]. Bytes that are not JSON structure are ignored, so a body
     * that is not JSON at all reports its bracket depth and is left to the parser.
     */
    fun nestingDepth(bytes: ByteArray, limit: Int = MAX_JSON_DEPTH): Int {
        var depth = 0
        var deepest = 0
        var inString = false
        var escaped = false
        for (byte in bytes) {
            val c = byte.toInt()
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\'.code -> escaped = true
                    c == '"'.code -> inString = false
                }
                continue
            }
            when (c) {
                '"'.code -> inString = true
                '['.code, '{'.code -> {
                    depth += 1
                    if (depth > deepest) {
                        deepest = depth
                        if (deepest > limit) return limit + 1
                    }
                }
                ']'.code, '}'.code -> if (depth > 0) depth -= 1
            }
        }
        return deepest
    }

    /** True when [bytes] nest deeper than [limit]. */
    fun isTooDeep(bytes: ByteArray, limit: Int = MAX_JSON_DEPTH): Boolean = nestingDepth(bytes, limit) > limit

    /** True when two request objects of the body carry the same id (notifications have none). */
    fun hasDuplicateIds(calls: JsonRpcCalls): Boolean {
        val ids = calls.ids.filterNotNull().map { it.toString() }
        return ids.size != ids.toSet().size
    }
}
