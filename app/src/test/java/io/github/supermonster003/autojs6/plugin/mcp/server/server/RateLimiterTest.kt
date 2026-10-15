package io.github.supermonster003.autojs6.plugin.mcp.server.server

import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    private var now = 1_000_000L

    private fun limiter(
        requests: Int = 3,
        requestWindowMs: Long = 1_000L,
        screenshots: Int = 2,
        screenshotWindowMs: Long = 60_000L,
        maxTracked: Int = 4,
        idleForgetMs: Long = 5_000L,
    ) = RateLimiter(RateLimits(requests, requestWindowMs, screenshots, screenshotWindowMs, maxTracked, idleForgetMs)) { now }

    @Test
    fun requestsWithinTheWindowAreAllowedUpToTheLimitThenRefusedWithAnExactRetry() {
        val limiter = limiter()
        repeat(3) { assertTrue(limiter.acquireRequest("a").allowed) }
        now += 300
        val refused = limiter.acquireRequest("a")
        assertFalse(refused.allowed)
        assertEquals(700L, refused.retryAfterMs)
        assertEquals(3, refused.limit)
        assertEquals(1_000L, refused.windowMs)
        assertEquals(1L, refused.retryAfterSeconds)

        now += 699
        assertFalse(limiter.acquireRequest("a").allowed)
        now += 1
        assertTrue("the oldest stamp left the window", limiter.acquireRequest("a").allowed)
    }

    @Test
    fun clientsHaveIndependentWindowsAndScreenshotsAreCountedSeparately() {
        val limiter = limiter()
        repeat(3) { assertTrue(limiter.acquireRequest("a").allowed) }
        assertFalse(limiter.acquireRequest("a").allowed)
        assertTrue("another client is not affected", limiter.acquireRequest("b").allowed)

        assertTrue(limiter.acquireScreenshot("a").allowed)
        assertTrue(limiter.acquireScreenshot("a").allowed)
        val shot = limiter.acquireScreenshot("a")
        assertFalse(shot.allowed)
        assertEquals(2, shot.limit)
        assertEquals(60_000L, shot.windowMs)
        assertEquals(60_000L, shot.retryAfterMs)
        assertEquals(60L, shot.retryAfterSeconds)
        assertTrue("the screenshot window does not consume request stamps", limiter.acquireRequest("b").allowed)
    }

    @Test
    fun retryAfterSecondsRoundsUpAndNeverReportsZero() {
        assertEquals(1L, RateVerdict(false, 1L, 1, 1_000L).retryAfterSeconds)
        assertEquals(2L, RateVerdict(false, 1_001L, 1, 1_000L).retryAfterSeconds)
        assertEquals(1L, RateVerdict(true, 0L, 1, 1_000L).retryAfterSeconds)
    }

    @Test
    fun idleClientsAreForgottenAndTheTrackingCeilingDropsTheLeastRecentlySeen() {
        val limiter = limiter()
        limiter.acquireRequest("a")
        now += 1
        limiter.acquireRequest("b")
        now += 1
        limiter.acquireRequest("c")
        now += 1
        limiter.acquireRequest("d")
        assertEquals(4, limiter.trackedClientCount)

        limiter.acquireRequest("e")
        assertEquals("the ceiling of 4 drops the oldest client", 4, limiter.trackedClientCount)
        repeat(3) { limiter.acquireRequest("a") }
        assertTrue("a was forgotten and starts a fresh window", limiter.acquireRequest("a").allowed.not())

        now += 5_000
        limiter.acquireRequest("f")
        assertEquals("every idle client is gone, only f remains", 1, limiter.trackedClientCount)

        limiter.forget("f")
        assertEquals(0, limiter.trackedClientCount)
    }

    @Test
    fun screenReadingRecognitionCallsCountAsScreenshotsButFilesDoNot() {
        fun counts(name: String, arguments: String?) = RateLimiter.countsAsScreenshot(name, arguments?.let { Json.parseToJsonElement(it) })
        assertTrue(counts("screen_capture", null))
        assertTrue(counts("screen_capture", """{"maxWidth":64}"""))
        assertTrue(counts("ocr_recognize", """{"image":"screen"}"""))
        assertTrue(counts("barcode_detect", """{"image":" screen "}"""))
        assertTrue(counts("opencv_find_image", """{"image":"screen","template":"t.png"}"""))
        assertTrue(counts("opencv_find_multi_colors", """{"image":"screen","color":"#FFF","points":[]}"""))
        assertFalse(counts("ocr_recognize", """{"image":"pics/screen.png"}"""))
        assertFalse(counts("opencv_find_image", """{"image":"a.png","template":"screen"}"""))
        assertFalse(counts("ocr_recognize", """{"image":"SCREEN"}"""))
        assertFalse(counts("ocr_recognize", """{"image":true}"""))
        assertFalse(counts("ocr_recognize", null))
        assertFalse(counts("pinyin_convert", """{"text":"screen","image":"screen"}"""))
        assertFalse(counts("device_ping", """{"image":"screen"}"""))
    }

    @Test
    fun defaultsMatchTheRoadmapAndRejectNonsense() {
        val defaults = RateLimits()
        assertEquals(20, defaults.requestsPerWindow)
        assertEquals(1_000L, defaults.requestWindowMs)
        assertEquals(30, defaults.screenshotsPerWindow)
        assertEquals(60_000L, defaults.screenshotWindowMs)
        assertEquals(setOf("screen_capture"), RateLimiter.SCREENSHOT_TOOLS)
        assertEquals(setOf("ocr_recognize", "barcode_detect", "opencv_find_image", "opencv_find_color", "opencv_find_multi_colors"), RateLimiter.SCREEN_IMAGE_TOOLS)
        assertTrue(ToolCatalog.Images.SCREEN_QUOTA_NOTE.contains("${RateLimits.SCREENSHOTS_PER_MINUTE} screenshots per minute"))
        RateLimiter.SCREEN_IMAGE_TOOLS.forEach { name ->
            assertTrue(name, ToolCatalog.byName.getValue(name).description.endsWith(ToolCatalog.Images.SCREEN_QUOTA_NOTE))
        }
        assertTrue(runCatching { RateLimits(requestsPerWindow = 0) }.isFailure)
        assertTrue(runCatching { RateLimits(screenshotWindowMs = 0) }.isFailure)
    }
}
