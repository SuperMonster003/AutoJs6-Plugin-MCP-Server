package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Activity
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.mcp.server.store.*
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.*
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Accessibility labels, keyboard navigation and the clipboard path of the settings page (roadmap P4).
 * The listener is never started here; only the tool group file is touched and restored.
 */
@RunWith(AndroidJUnit4::class)
class McpServerSettingsAccessibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private var activity: McpServerSettingsActivity? = null
    private var savedGroups: String? = null

    @Before fun prepare() {
        savedGroups = ProcessSharedFile(context, "tool_groups.json").read()
    }

    @After fun restore() {
        activity?.let { current -> instrumentation.runOnMainSync { current.finish() } }
        ProcessSharedFile(context, "tool_groups.json").write(savedGroups)
    }

    @Test fun everyControlCarriesALabelAndCardTitlesAreHeadings() {
        val settings = launch()
        awaitLoaded(settings)
        val unlabeled = onMain {
            views(settings.window.decorView).filter { view ->
                view !is ViewGroup && view.isShown && (view.isClickable || view.isLongClickable || view is EditText)
            }.filter { view -> label(view).isBlank() }.map { it.javaClass.simpleName }
        }
        assertEquals("views without a text or content description: $unlabeled", emptyList<String>(), unlabeled)
        val back = onMain { views(settings.window.decorView).filterIsInstance<ImageButton>().firstOrNull { it.contentDescription == settings.getString(R.string.settings_back) } }
        assertNotNull("the toolbar navigation button announces itself", back)
        assertTrue(onMain { back!!.isClickable && back.isFocusable })
        if (Build.VERSION.SDK_INT >= 28) {
            val titles = CARD_TITLES.map { settings.getString(it) }
            val headings = onMain { views(settings.window.decorView).filterIsInstance<TextView>().filter { it.text.toString() in titles } }
            assertEquals(titles.size, headings.size)
            assertTrue("card titles are accessibility headings", onMain { headings.all { it.isAccessibilityHeading } })
        }
        // What a screen reader sees: every actionable node in the window exposes a name.
        val nameless = mutableListOf<String>()
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        var seen = ""
        await("accessibility tree published (last seen: $seen)") {
            val roots = automation.windows.mapNotNull { it.root } + listOfNotNull(automation.rootInActiveWindow)
            seen = roots.joinToString { it.packageName.toString() }
            val root = roots.firstOrNull { it.packageName == context.packageName } ?: return@await false
            nameless.clear()
            nodes(root).filter { node -> node.isVisibleToUser && (node.isClickable || node.isCheckable || node.isEditable) }
                .filter { node -> node.text.isNullOrBlank() && node.contentDescription.isNullOrBlank() }
                .forEach { nameless += it.className.toString() }
            true
        }
        assertEquals("actionable accessibility nodes without a name: $nameless", emptyList<String>(), nameless)
    }

    @Test fun keyboardTabTraversalReachesEveryEnabledControlAndOperatesThem() {
        val settings = launch()
        awaitLoaded(settings)
        val targets = onMain { views(settings.window.decorView).filter { it is Button && it.isEnabled && it.isShown } }
        assertTrue("the page has controls", targets.size > 10)
        val back = onMain { views(settings.window.decorView).filterIsInstance<ImageButton>().first { it.contentDescription == settings.getString(R.string.settings_back) } }
        val focused = linkedSetOf<View>()
        repeat(targets.size + 8) {
            key(KeyEvent.KEYCODE_TAB)
            onMain { settings.currentFocus?.let { focused += it } }
        }
        val missed = onMain { (targets + back).filter { it !in focused }.map { label(it) } }
        val visited = onMain { focused.map { it.javaClass.simpleName + ":" + label(it) } }
        assertEquals("controls the Tab key never reached: $missed; visited $visited", emptyList<String>(), missed)

        // Enter on a focused button runs its action.
        val about = onMain { views(settings.window.decorView).filterIsInstance<Button>().first { it.text.toString() == settings.getString(R.string.settings_about) } }
        onMain { assertTrue(about.requestFocus()) }
        key(KeyEvent.KEYCODE_ENTER)
        await("about dialog opened from the keyboard") { onMain { settings.dialog?.isShowing == true } }
        onMain { settings.dialog!!.dismiss() }
        await("settings focused again") { onMain { settings.hasWindowFocus() } }

        // Enter on a focused switch toggles a default-on group and persists it for the listener process.
        val script = onMain { views(settings.window.decorView).filterIsInstance<Switch>().first { it.text.toString() == settings.getString(R.string.settings_group_script) } }
        await("script group shown enabled") { onMain { script.isChecked } }
        onMain { assertTrue(script.requestFocus()) }
        key(KeyEvent.KEYCODE_ENTER)
        await("script group persisted off") { !ToolPermissionStore(context).load().isEnabled(ToolGroup.SCRIPT) }
        await("switch shows the committed value") { onMain { !script.isChecked } }
        onMain { assertTrue(script.requestFocus()) }
        key(KeyEvent.KEYCODE_ENTER)
        await("script group persisted on") { ToolPermissionStore(context).load().isEnabled(ToolGroup.SCRIPT) }
        await("switch shows the committed value") { onMain { script.isChecked } }
    }

    @Test fun copyingTheClaudeCodeSnippetPutsTheGeneratedCommandOnTheClipboard() {
        val settings = launch()
        awaitLoaded(settings)
        val config = ServerConfigStore(context).load()
        assertEquals("the check expects the loopback endpoint only", BindScope.LOOPBACK, config.bindScope)
        val expected = ClientConfigSnippet.create(McpClient.CLAUDE_CODE, "http://127.0.0.1:${config.port}/mcp", TokenStore(context).current()).text
        onMain { click(settings, R.string.settings_client_claude) }
        await("snippet dialog") { onMain { settings.secretDialog?.isShowing == true } }
        onMain { settings.secretDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick() }
        val clipboard = onMain { context.getSystemService(ClipboardManager::class.java) }
        var text = ""
        await("clipboard filled") {
            text = onMain { clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString().orEmpty() }
            text.isNotEmpty()
        }
        assertEquals(expected, text)
        if (Build.VERSION.SDK_INT >= 33) {
            assertTrue("the credential clip is marked sensitive", onMain { clipboard.primaryClip?.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true })
        }
        if (InstrumentationRegistry.getArguments().getString("mcpClipboardEvidence") == "true") {
            // Opt-in only: the PC side pastes this file unchanged into the client and deletes it.
            context.cacheDir.resolve("p4-claude-snippet.txt").writeText(text)
        } else if (Build.VERSION.SDK_INT >= 28) {
            onMain { clipboard.clearPrimaryClip() }
        }
        onMain { settings.secretDialog?.dismiss() }
    }

    private fun launch(): McpServerSettingsActivity =
        (instrumentation.startActivitySync(Intent(context, McpServerSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as McpServerSettingsActivity).also { activity = it }

    private fun awaitLoaded(settings: McpServerSettingsActivity) {
        val port = ServerConfigStore(context).load().port
        await("settings loaded") { onMain { views(settings.window.decorView).filterIsInstance<TextView>().any { it.text.toString() == settings.getString(R.string.settings_port_value, port) } } }
        await("settings focused") { onMain { settings.hasWindowFocus() } }
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        instrumentation.waitForIdleSync()
    }

    private fun label(view: View): String = (view as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() } ?: view.contentDescription?.toString().orEmpty()

    private fun nodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        listOf(root) + (0 until root.childCount).mapNotNull { root.getChild(it) }.flatMap { nodes(it) }

    private fun await(description: String, condition: () -> Boolean) {
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (!condition()) { if (System.nanoTime() >= until) fail("Timeout: $description"); Thread.sleep(100) }
    }
    private fun <T> onMain(action: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(action) }
        return result!!.getOrThrow()
    }
    private fun views(root: View): List<View> = listOf(root) + if (root is ViewGroup) (0 until root.childCount).flatMap { views(root.getChildAt(it)) } else emptyList()
    private fun click(activity: Activity, string: Int, vararg args: Any) {
        val text = activity.getString(string, *args)
        views(activity.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == text && it.isClickable }.performClick()
    }

    private companion object {
        val CARD_TITLES = listOf(
            R.string.settings_connection, R.string.settings_network, R.string.settings_token, R.string.settings_paired_clients,
            R.string.settings_tools, R.string.settings_clients, R.string.settings_more,
        )
    }
}
