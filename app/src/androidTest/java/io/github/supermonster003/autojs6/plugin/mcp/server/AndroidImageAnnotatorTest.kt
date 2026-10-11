package io.github.supermonster003.autojs6.plugin.mcp.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import io.github.supermonster003.autojs6.plugin.mcp.server.host.AndroidImageAnnotator
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.AnnotationMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/** `screen_capture(annotate: true)` drawing on real encoded images (roadmap P9). */
class AndroidImageAnnotatorTest {

    private fun encoded(format: Bitmap.CompressFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        return ByteArrayOutputStream().use { out -> bitmap.compress(format, 90, out); bitmap.recycle(); out.toByteArray() }
    }

    /** The annotation pink, with room for compression artifacts. */
    private fun isMarked(pixel: Int): Boolean = Color.red(pixel) > 200 && Color.green(pixel) < 120 && Color.blue(pixel) in 60..220

    private fun isWhite(pixel: Int): Boolean = Color.red(pixel) > 235 && Color.green(pixel) > 235 && Color.blue(pixel) > 235

    @Test
    fun outlinesAndLabelsAreDrawnAndTheFormatAndSizeAreKept() {
        @Suppress("DEPRECATION")
        val formats = mapOf("image/jpeg" to Bitmap.CompressFormat.JPEG, "image/png" to Bitmap.CompressFormat.PNG, "image/webp" to Bitmap.CompressFormat.WEBP)
        formats.forEach { (mime, format) ->
            // Shorter side 720 px: outlines are 3 px wide, labels about 29 px high with 3 px padding.
            val marks = listOf(AnnotationMark("#n1", 80, 400, 600, 800), AnnotationMark("#n2", 0, 0, 240, 120))
            val annotated = AndroidImageAnnotator.annotate(encoded(format), mime, 90, marks)
            assertNotNull(mime, annotated)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(annotated, 0, annotated!!.size, options)
            assertEquals(mime, options.outMimeType)
            assertEquals(WIDTH, options.outWidth)
            assertEquals(HEIGHT, options.outHeight)

            val bitmap = BitmapFactory.decodeByteArray(annotated, 0, annotated.size)
            try {
                assertTrue("$mime: left outline of #n1", isMarked(bitmap.getPixel(80, 600)))
                assertTrue("$mime: label above #n1", isMarked(bitmap.getPixel(100, 372)))
                assertTrue("$mime: label of #n2 inside its top edge", isMarked(bitmap.getPixel(1, 1)))
                assertTrue("$mime: inside #n1 stays untouched", isWhite(bitmap.getPixel(340, 600)))
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test
    fun undecodableImagesAndUnknownFormatsAreRefused() {
        val mark = listOf(AnnotationMark("#n1", 0, 0, 10, 10))
        assertNull(AndroidImageAnnotator.annotate(byteArrayOf(1, 2, 3), "image/jpeg", 70, mark))
        assertNull(AndroidImageAnnotator.annotate(encoded(Bitmap.CompressFormat.PNG), "image/gif", 70, mark))
    }

    private companion object {
        const val WIDTH = 720
        const val HEIGHT = 1440
    }
}
