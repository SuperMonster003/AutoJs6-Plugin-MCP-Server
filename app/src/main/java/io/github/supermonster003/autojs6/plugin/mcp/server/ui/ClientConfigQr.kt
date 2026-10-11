package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * The QR code of a client configuration snippet or of the token on the settings page (roadmap P4 /
 * P9): the same text the copy button copies, UTF-8, error correction level M, a quiet zone of four
 * modules. [matrix] is plain ZXing and covered on the JVM; [bitmap] draws it black on white, as
 * scanners expect, whatever the theme.
 */
object ClientConfigQr {

    /** Below the byte-mode capacity of level M (2331 bytes); every snippet is far shorter. */
    const val MAX_TEXT_BYTES = 2_000

    fun fits(text: String): Boolean = text.isNotEmpty() && text.toByteArray(Charsets.UTF_8).size <= MAX_TEXT_BYTES

    /** One cell per module, quiet zone included; null when [text] does not [fit][fits]. */
    fun matrix(text: String): BitMatrix? {
        if (!fits(text)) return null
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 4,
        )
        return QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    }

    /** The code scaled by a whole number of pixels per module, at most [sizePx] wide when that allows one pixel per module. */
    fun bitmap(text: String, sizePx: Int): Bitmap? {
        val matrix = matrix(text) ?: return null
        val modules = matrix.width
        val scale = maxOf(1, sizePx / modules)
        val size = modules * scale
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val row = y * size
            for (x in 0 until size) {
                pixels[row + x] = if (matrix[x / scale, y / scale]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
