package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenAnnotationsTest {

    private fun entries(vararg nodes: Pair<Bounds, Boolean>): List<HostSnapshots.Row> =
        nodes.mapIndexed { i, (bounds, visible) -> HostSnapshots.Row("#n${i + 1}", bounds, leaf = false, hidden = !visible) }

    @Test
    fun `the whole screen is scaled uniformly by the output width`() {
        val layout = ScreenAnnotations.layout(entries(Bounds(0, 0, 1080, 200) to true, Bounds(540, 1200, 1080, 2400) to true), 1080, null, 540, 1200)
        assertEquals(listOf(AnnotationMark("#n1", 0, 0, 540, 100), AnnotationMark("#n2", 270, 600, 540, 1200)), layout.marks)
        assertEquals(0, layout.skipped)
    }

    @Test
    fun `a crop region moves the origin, clips nodes at its edges and drops nodes outside`() {
        val region = Bounds(100, 500, 600, 1500)
        val layout = ScreenAnnotations.layout(
            entries(
                Bounds(150, 550, 350, 650) to true, // inside
                Bounds(0, 400, 200, 700) to true, // across the top left corner
                Bounds(700, 600, 900, 700) to true, // right of the crop
                Bounds(200, 600, 300, 700) to false, // invisible
            ),
            1080, region, 250, 500,
        )
        assertEquals(listOf(AnnotationMark("#n1", 25, 25, 125, 75), AnnotationMark("#n2", 0, 0, 50, 100)), layout.marks)
        assertEquals(2, layout.skipped)
    }

    @Test
    fun `at most MAX_MARKS are drawn and degenerate geometry draws nothing`() {
        val many = entries(*Array(ScreenAnnotations.MAX_MARKS + 5) { Bounds(0, 0, 10, 10) to true })
        val capped = ScreenAnnotations.layout(many, 100, null, 100, 100)
        assertEquals(ScreenAnnotations.MAX_MARKS, capped.marks.size)
        assertEquals(5, capped.skipped)
        assertEquals(ScreenAnnotations.Layout(emptyList(), 1), ScreenAnnotations.layout(entries(Bounds(0, 0, 10, 10) to true), 100, Bounds(200, 0, 300, 10), 50, 50))
    }
}
