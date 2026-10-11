package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import kotlin.math.roundToInt

/** One `#n` reference to draw, in output image pixels. */
data class AnnotationMark(val ref: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * Draws [marks] on an encoded image and encodes it again in [mime] (JPEG and WebP at [quality]);
 * null when the image cannot be decoded or encoded. The Android implementation lives in the
 * service process (`AndroidImageAnnotator`); this interface keeps [ScreenTools] testable on the JVM.
 */
fun interface ImageAnnotator {
    fun annotate(image: ByteArray, mime: String, quality: Int, marks: List<AnnotationMark>): ByteArray?
}

/**
 * Layout of `screen_capture(annotate: true)` (roadmap P3.3 / P9): maps the live node references of
 * the latest `ui_dump` / `ui_find` from screen pixels into the captured image. The image shows the
 * crop region (or the whole screen) scaled uniformly to its output width, so one factor serves both
 * axes. Invisible nodes and nodes outside the image are skipped; at most [MAX_MARKS] are drawn.
 *
 * Pure Kotlin; `ScreenAnnotationsTest` covers it.
 */
object ScreenAnnotations {

    const val MAX_MARKS = 300

    /** The marks to draw and how many live references were left out. */
    data class Layout(val marks: List<AnnotationMark>, val skipped: Int)

    /**
     * [screenWidth] is the current screen width in pixels; [region] is `left, top, right, bottom`
     * of the crop in screen pixels, or null for the whole screen.
     */
    fun layout(entries: List<NodeRefRegistry.Entry>, screenWidth: Int, region: Bounds?, outputWidth: Int, outputHeight: Int): Layout {
        val originX = region?.left ?: 0
        val originY = region?.top ?: 0
        val sourceWidth = region?.let { minOf(it.right, screenWidth) - it.left } ?: screenWidth
        if (sourceWidth <= 0 || outputWidth <= 0 || outputHeight <= 0) return Layout(emptyList(), entries.size)
        val scale = outputWidth.toDouble() / sourceWidth
        val marks = mutableListOf<AnnotationMark>()
        var skipped = 0
        for (entry in entries) {
            val bounds = entry.node.bounds
            val left = ((bounds.left - originX) * scale).roundToInt().coerceIn(0, outputWidth)
            val top = ((bounds.top - originY) * scale).roundToInt().coerceIn(0, outputHeight)
            val right = ((bounds.right - originX) * scale).roundToInt().coerceIn(0, outputWidth)
            val bottom = ((bounds.bottom - originY) * scale).roundToInt().coerceIn(0, outputHeight)
            if (!entry.node.visible || right <= left || bottom <= top || marks.size >= MAX_MARKS) {
                skipped++
                continue
            }
            marks += AnnotationMark(entry.ref, left, top, right, bottom)
        }
        return Layout(marks, skipped)
    }
}
