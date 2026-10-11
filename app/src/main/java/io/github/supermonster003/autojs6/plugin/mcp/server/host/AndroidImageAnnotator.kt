package io.github.supermonster003.autojs6.plugin.mcp.server.host

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.AnnotationMark
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ImageAnnotator
import java.io.ByteArrayOutputStream

/**
 * Draws `#n` references on a screenshot for `screen_capture(annotate: true)`: an outline around each
 * node and its reference on a filled label above the node (inside its top edge when there is no
 * room above). Sizes follow the image's shorter side, so labels stay readable after scaling. The
 * image is decoded and encoded in the `:mcp_server` process; nothing is written to storage.
 */
object AndroidImageAnnotator : ImageAnnotator {

    /** A saturated pink that stands out on light and dark interfaces alike. */
    private const val MARK_COLOR = 0xFFFF1F8F.toInt()

    override fun annotate(image: ByteArray, mime: String, quality: Int, marks: List<AnnotationMark>): ByteArray? = runCatching {
        val format = formatOf(mime) ?: return null
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size, BitmapFactory.Options().apply { inMutable = true }) ?: return null
        try {
            draw(Canvas(bitmap), bitmap.width, bitmap.height, marks)
            ByteArrayOutputStream(image.size + image.size / 4).use { out ->
                if (bitmap.compress(format, quality.coerceIn(1, 100), out)) out.toByteArray() else null
            }
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()

    private fun draw(canvas: Canvas, width: Int, height: Int, marks: List<AnnotationMark>) {
        val unit = maxOf(1f, minOf(width, height) / 360f)
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * unit
            color = MARK_COLOR
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = MARK_COLOR
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f * unit
            typeface = Typeface.DEFAULT_BOLD
        }
        val metrics = text.fontMetrics
        val padding = 1.5f * unit
        val labelHeight = metrics.descent - metrics.ascent + 2 * padding
        for (mark in marks) {
            canvas.drawRect(mark.left.toFloat(), mark.top.toFloat(), mark.right.toFloat(), mark.bottom.toFloat(), outline)
            val labelWidth = text.measureText(mark.ref) + 2 * padding
            val top = if (mark.top - labelHeight >= 0) mark.top - labelHeight else mark.top.toFloat()
            val left = mark.left.toFloat().coerceAtMost(width - labelWidth).coerceAtLeast(0f)
            canvas.drawRect(left, top, left + labelWidth, top + labelHeight, label)
            canvas.drawText(mark.ref, left + padding, top + padding - metrics.ascent, text)
        }
    }

    @Suppress("DEPRECATION")
    private fun formatOf(mime: String): Bitmap.CompressFormat? = when (mime) {
        "image/jpeg" -> Bitmap.CompressFormat.JPEG
        "image/png" -> Bitmap.CompressFormat.PNG
        "image/webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        else -> null
    }
}
