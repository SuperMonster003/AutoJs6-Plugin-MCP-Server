package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
            assertFalse(exists("mcp.releaseHistory.body.$second"))
            assertTrue(exists("mcp.releaseHistory.counts.$second"))

            compose.onNodeWithTag("mcp.releaseHistory.entry.$second").performClick()
            compose.waitUntil(TIMEOUT) { exists("mcp.releaseHistory.body.$second") }
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
            leaveTouchMode()
            val visited = tabCycle()
            assertTrue(visited.toString(), "mcp.topBar.navigation" in visited)
            assertTrue(visited.toString(), "mcp.releaseHistory.toggleAll" in visited)
            assertEquals(versions.map { "mcp.releaseHistory.entry.$it" }, visited.filter { it.startsWith("mcp.releaseHistory.entry.") })
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

    /** Presses Tab until focus returns to a control seen before; every focused control must be on screen. */
    private fun tabCycle(): List<String> {
        val visited = mutableListOf<String>()
        repeat(MAX_TABS) {
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
        const val TIMEOUT = 15_000L
        const val MAX_TABS = 120
        const val UNTAGGED = "(untagged)"
    }
}
