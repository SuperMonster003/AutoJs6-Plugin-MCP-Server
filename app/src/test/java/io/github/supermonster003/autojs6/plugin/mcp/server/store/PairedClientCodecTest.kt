package io.github.supermonster003.autojs6.plugin.mcp.server.store

import io.github.supermonster003.autojs6.plugin.mcp.server.server.AddressClass
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairedClientCodecTest {

    private val clients = listOf(
        PairedClient("aaaa", "Claude Code", "2.1", AddressClass.LOOPBACK, 1_000L, 2_000L),
        PairedClient("bbbb", "Cursor", null, AddressClass.LAN, 3_000L, 3_000L),
    )

    @Test
    fun roundTripKeepsEveryField() {
        val json = PairedClientCodec.encode(clients)
        assertTrue(json, json.contains("\"format\":1"))
        assertEquals(clients, PairedClientCodec.decode(json))
    }

    @Test
    fun decodingIsTolerant() {
        assertTrue(PairedClientCodec.decode(null).isEmpty())
        assertTrue(PairedClientCodec.decode("").isEmpty())
        assertTrue(PairedClientCodec.decode("not json").isEmpty())
        assertTrue(PairedClientCodec.decode("[]").isEmpty())
        assertTrue(PairedClientCodec.decode("{\"format\":1}").isEmpty())
        val partial = """
            {"format":1,"clients":[
              {"fingerprint":"aaaa","name":"Claude Code","addressClass":"loopback","firstPairedAt":10},
              {"fingerprint":"","name":"x","addressClass":"lan","firstPairedAt":1,"lastSeenAt":1},
              {"fingerprint":"cccc","name":"y","addressClass":"wan","firstPairedAt":1,"lastSeenAt":1},
              {"fingerprint":"dddd","name":"z","addressClass":"lan","firstPairedAt":"soon","lastSeenAt":1},
              {"fingerprint":"aaaa","name":"Duplicate","addressClass":"loopback","firstPairedAt":10,"lastSeenAt":12},
              "garbage"
            ]}
        """.trimIndent()
        val decoded = PairedClientCodec.decode(partial)
        assertEquals(listOf(PairedClient("aaaa", "Claude Code", null, AddressClass.LOOPBACK, 10L, 10L)), decoded)
    }
}
