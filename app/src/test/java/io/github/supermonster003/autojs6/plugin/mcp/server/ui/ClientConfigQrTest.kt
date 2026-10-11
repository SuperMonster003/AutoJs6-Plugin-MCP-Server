package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientConfigQrTest {

    private val token = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCdEfGh"

    /** Reads [matrix] back with ZXing's own decoder, four pixels per module. */
    private fun decode(matrix: BitMatrix): String {
        val scale = 4
        val size = matrix.width * scale
        val pixels = IntArray(size * size) { i -> if (matrix[(i % size) / scale, (i / size) / scale]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        return QRCodeReader().decode(bitmap, mapOf(DecodeHintType.PURE_BARCODE to true, DecodeHintType.CHARACTER_SET to "UTF-8")).text
    }

    @Test
    fun `every client snippet and the token round-trip through the QR code`() {
        val texts = McpClient.entries.flatMap { client ->
            listOf("http://127.0.0.1:9637/mcp", "http://192.168.3.213:9637/mcp").map { endpoint -> ClientConfigSnippet.create(client, endpoint, token).text }
        } + token
        texts.forEach { text ->
            assertTrue(text, ClientConfigQr.fits(text))
            val matrix = ClientConfigQr.matrix(text)!!
            assertEquals(matrix.width, matrix.height)
            assertEquals(text, decode(matrix))
        }
    }

    @Test
    fun `non-ASCII text survives and oversized or empty text is refused`() {
        val text = "AutoJs6 连接 مرحبا 日本語"
        assertEquals(text, decode(ClientConfigQr.matrix(text)!!))
        assertFalse(ClientConfigQr.fits(""))
        assertFalse(ClientConfigQr.fits("x".repeat(ClientConfigQr.MAX_TEXT_BYTES + 1)))
        assertNull(ClientConfigQr.matrix("x".repeat(ClientConfigQr.MAX_TEXT_BYTES + 1)))
        assertTrue(ClientConfigQr.fits("x".repeat(ClientConfigQr.MAX_TEXT_BYTES)))
    }
}
