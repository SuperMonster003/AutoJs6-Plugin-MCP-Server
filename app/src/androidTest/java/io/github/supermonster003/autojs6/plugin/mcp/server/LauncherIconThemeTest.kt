package io.github.supermonster003.autojs6.plugin.mcp.server

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.zip.ZipFile
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class LauncherIconThemeTest {

    @Test
    fun launcherIconKeepsItsGeometryInBothThemes() {
        assertThemePair(R.mipmap.ic_launcher)
    }

    @Test
    fun roundLauncherIconKeepsItsGeometryInBothThemes() {
        assertThemePair(R.mipmap.ic_launcher_round)
    }

    @Test
    fun launcherIconMatchesTheCompleteRoundArtwork() {
        assertMatchesRoundArtwork(R.mipmap.ic_launcher)
    }

    @Test
    fun roundLauncherIconMatchesTheCompleteRoundArtwork() {
        assertMatchesRoundArtwork(R.mipmap.ic_launcher_round)
    }

    private fun assertMatchesRoundArtwork(resourceId: Int) {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ZipFile(context.applicationInfo.sourceDir).use { apk ->
            for ((mode, directory) in listOf(
                Configuration.UI_MODE_NIGHT_NO to "mipmap",
                Configuration.UI_MODE_NIGHT_YES to "mipmap-night",
            )) {
                // Read the actual packaged PNG; normal resource lookup selects the adaptive XML.
                val path = Regex("res/$directory(?:-v\\d+)?/ic_launcher_round\\.png")
                val entry = requireNotNull(apk.entries().asSequence().singleOrNull { path.matches(it.name) })
                val reference = apk.getInputStream(entry).use { requireNotNull(BitmapFactory.decodeStream(it)) }
                val config = Configuration(context.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
                }
                val themed = context.createConfigurationContext(config)
                val actual = render(themed.resources.getDrawable(resourceId, themed.theme), reference.width)
                try {
                    val expectedBounds = glyphBounds(reference)
                    val actualBounds = glyphBounds(actual)
                    Log.i("LauncherIconThemeTest", "$directory: PNG glyph=$expectedBounds, adaptive glyph=$actualBounds")
                    for ((expected, observed) in listOf(
                        expectedBounds.left to actualBounds.left,
                        expectedBounds.top to actualBounds.top,
                        expectedBounds.right to actualBounds.right,
                        expectedBounds.bottom to actualBounds.bottom,
                    )) {
                        assertTrue("The complete PNG's margins must be preserved: expected $expectedBounds, actual $actualBounds",
                            abs(expected - observed) <= 1)
                    }
                    var intersection = 0
                    var union = 0
                    for (y in 0 until reference.height) {
                        for (x in 0 until reference.width) {
                            val expectedGlyph = isGlyph(reference.getPixel(x, y))
                            val actualGlyph = isGlyph(actual.getPixel(x, y))
                            if (expectedGlyph && actualGlyph) intersection++
                            if (expectedGlyph || actualGlyph) union++
                        }
                    }
                    // Independently rasterized layers can differ at antialiased edge pixels.
                    val overlap = intersection.toDouble() / union
                    Log.i("LauncherIconThemeTest", "$directory: glyph overlap=$overlap")
                    assertTrue("The frame, letters and ports must match the PNG (overlap=$overlap)", overlap >= 0.9)
                } finally {
                    actual.recycle()
                    reference.recycle()
                }
            }
        }
    }

    private fun glyphBounds(bitmap: Bitmap): Rect {
        val bounds = Rect(bitmap.width, bitmap.height, 0, 0)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (isGlyph(bitmap.getPixel(x, y))) {
                    bounds.left = minOf(bounds.left, x)
                    bounds.top = minOf(bounds.top, y)
                    bounds.right = maxOf(bounds.right, x + 1)
                    bounds.bottom = maxOf(bounds.bottom, y + 1)
                }
            }
        }
        assertFalse("The reference glyph must be visible", bounds.isEmpty)
        return bounds
    }

    private fun isGlyph(pixel: Int): Boolean = Color.alpha(pixel) >= 240 &&
        Color.red(pixel) >= 240 && Color.green(pixel) >= 240 && Color.blue(pixel) >= 240

    private fun assertThemePair(resourceId: Int) {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drawables = listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES).map { mode ->
            val config = Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            }
            val themed = context.createConfigurationContext(config)
            themed.resources.getDrawable(resourceId, themed.theme).also { icon ->
                assertTrue("Both themes must select adaptive icons (night mode = $mode)", icon is AdaptiveIconDrawable)
            }
        }
        val day = render(drawables[0])
        val night = render(drawables[1])
        try {
            assertFalse("The background colors should differ", day.sameAs(night))
            var whitePixels = 0
            for (y in 0 until day.height) {
                for (x in 0 until day.width) {
                    val lightPixel = day.getPixel(x, y)
                    val darkPixel = night.getPixel(x, y)
                    assertEquals("The icon mask must stay the same at $x,$y", Color.alpha(lightPixel), Color.alpha(darkPixel))
                    val lightGlyph = lightPixel == Color.WHITE
                    val darkGlyph = darkPixel == Color.WHITE
                    assertEquals("The white glyph must stay the same at $x,$y", lightGlyph, darkGlyph)
                    if (lightGlyph) whitePixels++
                }
            }
            assertTrue("The glyph comparison must include visible pixels", whitePixels > 0)
        } finally {
            day.recycle()
            night.recycle()
        }
    }

    private fun render(drawable: Drawable, size: Int = 96): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(Canvas(bitmap))
        }
}
