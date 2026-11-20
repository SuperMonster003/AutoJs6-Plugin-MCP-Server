package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Activity
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.McpServerSettingsActivity
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.ReleaseHistoryAssetPolicy
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.loadReleaseHistory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plugin center P21.8: the release history page follows the M3 model of the official plugins
 * (`AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md`). Its title is "Release history" with the plugin name as the subtitle,
 * releases expand and collapse, the settings button opens it, and a keyboard reaches every release.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseHistoryPageInstrumentationTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @After fun finishPages() {
        instrumentation.setInTouchMode(true)
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
    }

    /** Unmerged: the count chips sit inside the clickable header, which merges its children. */
    private fun exists(tag: String): Boolean =
        runCatching { compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    /** Scrolls the history until the node tagged [tag] is on screen (the lazy list composes only what it shows). */
    private fun scrollTo(tag: String) {
        compose.onNodeWithTag("mcp.releaseHistory.list").performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
    }

    private fun resumed(): Activity? {
        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    private fun assertTitleAndSubtitle() {
        val inTopBar = hasAnyAncestor(hasTestTag("mcp.topBar"))
        compose.onNode(hasText(context.getString(R.string.settings_release_history)) and inTopBar).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.app_name)) and inTopBar).assertIsDisplayed()
    }

    private fun versions(scenario: ActivityScenario<ReleaseHistoryActivity>): List<String> {
        var versions = emptyList<String>()
        scenario.onActivity { activity ->
            val candidates = ReleaseHistoryAssetPolicy.candidatesFor(activity.resources.configuration.locales[0])
            versions = loadReleaseHistory(candidates) { path ->
                activity.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
            }.map { it.version }
        }
        return versions
    }

    @Test fun theTitleIsReleaseHistoryAndTheSubtitleThePluginName() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("mcp.releaseHistory.list") }
            assertTitleAndSubtitle()
            assertFalse(exists("mcp.releaseHistory.empty"))
            scenario.onActivity { assertEquals(it.getString(R.string.settings_release_history), it.title) }
        }
    }

    /**
     * The newest release starts expanded, the others show their category counts; a header expands or collapses its
     * release, the top bar expands or collapses all of them, and the choice survives recreation.
     */
    @Test fun releasesExpandAndCollapse() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("mcp.releaseHistory.list") }
            val versions = versions(scenario)
            assertTrue("releases: ${versions.size}", versions.size > 2)
            val (newest, second) = versions
            assertTrue(exists("mcp.releaseHistory.body.$newest"))
            assertFalse(exists("mcp.releaseHistory.counts.$newest"))
            // A long newest release pushes the next one below the screen, where the lazy list has not composed it yet.
            scrollTo("mcp.releaseHistory.entry.$second")
            assertFalse(exists("mcp.releaseHistory.body.$second"))
            assertTrue(exists("mcp.releaseHistory.counts.$second"))

            compose.onNodeWithTag("mcp.releaseHistory.entry.$second").performClick()
            compose.waitUntil(TIMEOUT) { exists("mcp.releaseHistory.body.$second") }
            scrollTo("mcp.releaseHistory.entry.$newest")
            compose.onNodeWithTag("mcp.releaseHistory.entry.$newest").performClick()
            compose.waitUntil(TIMEOUT) { !exists("mcp.releaseHistory.body.$newest") && exists("mcp.releaseHistory.counts.$newest") }

            scenario.recreate()
            compose.waitUntil(TIMEOUT) { exists("mcp.releaseHistory.list") }
            assertTrue(exists("mcp.releaseHistory.body.$second"))
            assertFalse(exists("mcp.releaseHistory.body.$newest"))

            compose.onNodeWithTag("mcp.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.settings_release_history_expand_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { exists("mcp.releaseHistory.body.$newest") }
            compose.onNodeWithTag("mcp.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.settings_release_history_collapse_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { !exists("mcp.releaseHistory.body.$newest") && !exists("mcp.releaseHistory.body.$second") }
        }
    }

    /** The "Release history" button of the settings page opens this page (the foreground activity), not a dialog. */
    @Test fun theSettingsButtonOpensThePage() {
        val intent = Intent(context, McpServerSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val settings = instrumentation.startActivitySync(intent) as McpServerSettingsActivity
        val label = settings.getString(R.string.settings_release_history)
        instrumentation.runOnMainSync {
            views(settings.window.decorView).filterIsInstance<Button>().first { it.text.toString() == label }.performClick()
        }
        compose.waitUntil(TIMEOUT) { resumed() is ReleaseHistoryActivity && exists("mcp.releaseHistory.list") }
        assertTitleAndSubtitle()
    }

    /** Tab walks the back button and every release header, each scrolled into view, down to the oldest release. */
    @Test fun theReleaseHistoryCanBeReadToTheEndFromTheKeyboard() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("mcp.releaseHistory.list") }
            val versions = versions(scenario)
            awaitWindowFocus(scenario)
            leaveTouchMode()
            val visited = tabCycle(scenario)
            fun expect(ok: Boolean, what: String) { if (!ok) fail("$what; visited: $visited; ${focusedWindow()}") }
            expect("mcp.topBar.navigation" in visited, "Tab never reached the back button")
            expect("mcp.releaseHistory.toggleAll" in visited, "Tab never reached the expand / collapse all button")
            val expected = versions.map { "mcp.releaseHistory.entry.$it" }
            val entries = visited.filter { it.startsWith("mcp.releaseHistory.entry.") }
            expect(entries == expected, "the releases in Tab order: expected $expected, got $entries")
        }
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    /** API 24 learns about leaving touch mode only from a key, so an up arrow follows. */
    private fun leaveTouchMode() {
        instrumentation.setInTouchMode(false)
        key(KeyEvent.KEYCODE_DPAD_UP)
    }

    /** The test tag of the focused control (or of its nearest tagged ancestor), null when nothing has focus. */
    private fun focusedTag(): String? {
        val node = compose.onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull() ?: return null
        return generateSequence(node) { it.parent }.mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.firstOrNull() ?: UNTAGGED
    }

    /** Runs [command] as the shell user (it connects UiAutomation) and returns its output. */
    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }

    /**
     * The focused window and app as `dumpsys window` reports them, for diagnostics only. API 33 and 35 leave
     * mCurrentFocus out of `dumpsys window windows`, so the whole dump is read.
     */
    private fun focusedWindow(): String = runCatching {
        shell("dumpsys window").lineSequence().map(String::trim)
            .filter { it.startsWith("mCurrentFocus=") || it.startsWith("mFocusedApp=") }.distinct().joinToString()
            .ifEmpty { "mCurrentFocus not in dumpsys" }
    }.getOrElse { "dumpsys failed: $it" }

    private fun windowFocused(scenario: ActivityScenario<*>): Boolean = try {
        var focused = false
        compose.waitUntil(TIMEOUT) { scenario.onActivity { focused = it.hasWindowFocus() }; focused }
        true
    } catch (timeout: ComposeTimeoutException) {
        false
    }

    /**
     * Keys go to the focused window: while a system dialog has focus (an ANR or crash dialog of another app on a slow
     * emulator, say), every Tab would be lost. When focus does not come, the test closes system dialogs (error dialogs
     * take that as "wait" or "close") and brings the page back to the front, once, before it gives up.
     */
    private fun awaitWindowFocus(scenario: ActivityScenario<*>, visited: List<String> = emptyList()) {
        if (windowFocused(scenario)) return
        val before = focusedWindow()
        Log.w(LOG_TAG, "no window focus after $TIMEOUT ms ($before), closing system dialogs")
        shell("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
        scenario.onActivity { it.startActivity(Intent(it, it.javaClass).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        if (!windowFocused(scenario)) {
            fail("the page has no window focus after $TIMEOUT ms, nor after closing system dialogs; visited: $visited; " +
                "before: $before; after: ${focusedWindow()}")
        }
        Log.i(LOG_TAG, "window focus is back: ${focusedWindow()}")
    }

    /** Presses Tab until focus returns to a control seen before; every focused control must be on screen. */
    private fun tabCycle(scenario: ActivityScenario<*>): List<String> {
        val visited = mutableListOf<String>()
        repeat(MAX_TABS) {
            awaitWindowFocus(scenario, visited)
            key(KeyEvent.KEYCODE_TAB)
            val tag = focusedTag() ?: return@repeat
            if (tag in visited) return visited
            compose.onNode(isFocused(), useUnmergedTree = true).assertIsDisplayed()
            visited += tag
        }
        return visited
    }

    private fun views(root: View): List<View> =
        listOf(root) + if (root is ViewGroup) (0 until root.childCount).flatMap { views(root.getChildAt(it)) } else emptyList()

    private companion object {
        const val LOG_TAG = "ReleaseHistoryTest"
        const val TIMEOUT = 15_000L
        const val MAX_TABS = 120
        const val UNTAGGED = "(untagged)"
    }
}
