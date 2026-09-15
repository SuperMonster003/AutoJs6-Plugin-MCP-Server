package io.github.supermonster003.autojs6.plugin.mcp.server.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestBodyChecksTest {

    private fun depth(text: String, limit: Int = RequestBodyChecks.MAX_JSON_DEPTH): Int =
        RequestBodyChecks.nestingDepth(text.toByteArray(Charsets.UTF_8), limit)

    @Test
    fun depthCountsArraysAndObjectsAndIgnoresBracketsInsideStrings() {
        assertEquals(0, depth("42"))
        assertEquals(1, depth("""{"jsonrpc":"2.0","id":1,"method":"ping"}"""))
        assertEquals(3, depth("""{"params":{"arguments":{"a":1}}}"""))
        assertEquals(2, depth("""[{"id":1},{"id":2}]"""))
        assertEquals(1, depth("""{"text":"[[[[{{{{"}"""))
        assertEquals(1, depth("""{"text":"a\"[\\"}"""))
        assertEquals(2, depth("""{"a":[]}"""))
    }

    @Test
    fun depthStopsEarlyAboveTheLimit() {
        val nested = "[".repeat(100_000)
        assertEquals(RequestBodyChecks.MAX_JSON_DEPTH + 1, depth(nested))
        assertTrue(RequestBodyChecks.isTooDeep(nested.toByteArray()))
        assertEquals(5, depth("[".repeat(5), limit = 4))
        assertEquals(4, depth("[".repeat(4), limit = 4))
        assertFalse(RequestBodyChecks.isTooDeep("[".repeat(RequestBodyChecks.MAX_JSON_DEPTH).toByteArray()))
    }

    @Test
    fun unbalancedAndNonJsonBytesNeverThrow() {
        assertEquals(0, depth("]]]}}}"))
        assertEquals(1, depth("[]]]]["))
        val garbage = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), '['.code.toByte(), 0xC3.toByte(), '{'.code.toByte(), 0x00)
        assertEquals(2, RequestBodyChecks.nestingDepth(garbage))
        assertEquals(0, RequestBodyChecks.nestingDepth(ByteArray(0)))
    }

    @Test
    fun duplicateIdsAreDetectedAcrossRequestObjectsOnly() {
        val duplicate = JsonRpcCalls.parse("""[{"jsonrpc":"2.0","id":1,"method":"ping"},{"jsonrpc":"2.0","id":1,"method":"ping"}]""")
        assertNotNull(duplicate)
        assertTrue(RequestBodyChecks.hasDuplicateIds(duplicate!!))

        val distinct = JsonRpcCalls.parse("""[{"jsonrpc":"2.0","id":1,"method":"ping"},{"jsonrpc":"2.0","id":"1","method":"ping"},{"jsonrpc":"2.0","method":"notifications/initialized"}]""")
        assertNotNull(distinct)
        // 1 and "1" are different JSON values; notifications carry no id.
        assertFalse(RequestBodyChecks.hasDuplicateIds(distinct!!))

        val single = JsonRpcCalls.parse("""{"jsonrpc":"2.0","id":7,"method":"ping"}""")
        assertFalse(RequestBodyChecks.hasDuplicateIds(single!!))
    }
}
