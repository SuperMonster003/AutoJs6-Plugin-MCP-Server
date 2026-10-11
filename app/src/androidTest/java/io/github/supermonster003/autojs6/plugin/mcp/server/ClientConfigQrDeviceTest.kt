package io.github.supermonster003.autojs6.plugin.mcp.server

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.ClientConfigQr
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.ClientConfigSnippet
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.McpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The settings page QR bitmap decodes back to the snippet it shows (roadmap P9). */
class ClientConfigQrDeviceTest {

    @Test
    fun theBitmapDecodesToTheSnippetAndFitsTheRequestedWidth() {
        val text = ClientConfigSnippet.create(McpClient.GENERIC, "http://127.0.0.1:9637/mcp", "A".repeat(43)).text
        val bitmap = ClientConfigQr.bitmap(text, 600)!!
        try {
            assertEquals(bitmap.width, bitmap.height)
            assertTrue("${bitmap.width} px", bitmap.width in 300..600)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val binary = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
            assertEquals(text, QRCodeReader().decode(binary, mapOf(DecodeHintType.PURE_BARCODE to true, DecodeHintType.CHARACTER_SET to "UTF-8")).text)
        } finally {
            bitmap.recycle()
        }
    }
}
