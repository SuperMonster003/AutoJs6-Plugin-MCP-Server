package io.github.supermonster003.autojs6.plugin.mcp.server

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.mcp.server.server.AddressClass
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairedClient
import io.github.supermonster003.autojs6.plugin.mcp.server.server.PairingGate
import io.github.supermonster003.autojs6.plugin.mcp.server.store.PairedClientStore
import io.github.supermonster003.autojs6.plugin.mcp.server.store.TokenStore
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.PluginGroupSwitch
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolPermissionStore
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.McpSettingsReceiver
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in device helpers for the committed evidence tools (`tools/evidence`). Every test is skipped
 * unless its instrumentation argument is given, so ordinary runs only report three skips.
 *
 * - `-e mcpEvidenceToken true` reports the current token through the instrumentation status bundle
 *   (`am instrument -r`), so the PC tool keeps it in memory without a file on either side.
 * - `-e mcpEvidencePair <client name>` records that client as paired over loopback (the fingerprint the
 *   gate derives from the name, reported in the status bundle), so a scenario that is not about pairing
 *   runs without a phone-side prompt. A pending prompt cannot be answered from here: `am instrument`
 *   force-stops the whole package when it starts and ends, listener process included.
 * - `-e mcpEvidenceRevoke <fingerprint>` removes a paired client the way the settings page does.
 * - `-e mcpEvidenceSharedRead true|false` sets the "Allow reading shared storage" switch (roadmap P11 D26) and
 *   reports the previous value, so a scenario can restore it afterwards.
 *
 * Only an adb operator with an instrumentation APK signed by the same key reaches these paths; the
 * threat model already treats adb as trusted (developer-mode `dumpsys` prints the token as well).
 */
@RunWith(AndroidJUnit4::class)
class EvidenceHelperTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()

    @Test fun reportToken() {
        assumeTrue("pass -e mcpEvidenceToken true", arguments.getString(ARG_TOKEN) == "true")
        instrumentation.sendStatus(STATUS_CODE, Bundle().apply { putString(ARG_TOKEN, TokenStore(context).current()) })
    }

    @Test fun pairClient() {
        val name = arguments.getString(ARG_PAIR)
        assumeTrue("pass -e mcpEvidencePair <client name>", name != null)
        assertTrue("client name must be 1 to 64 characters", name!!.isNotBlank() && name.length <= 64)
        val fingerprint = PairingGate.fingerprintOf(name, AddressClass.LOOPBACK)
        val now = System.currentTimeMillis()
        val clients = PairedClientStore(context)
        clients.put(PairedClient(fingerprint, name, "evidence", AddressClass.LOOPBACK, now, now))
        assertTrue(clients.all().any { it.fingerprint == fingerprint })
        McpSettingsReceiver.send(context, McpSettingsReceiver.REFRESH)
        instrumentation.sendStatus(STATUS_CODE, Bundle().apply { putString(ARG_PAIR, fingerprint) })
    }

    @Test fun revokePairing() {
        val fingerprint = arguments.getString(ARG_REVOKE)
        assumeTrue("pass -e mcpEvidenceRevoke <fingerprint>", fingerprint != null)
        assertTrue("malformed fingerprint", FINGERPRINT.matches(fingerprint!!))
        val clients = PairedClientStore(context)
        clients.remove(fingerprint)
        assertTrue(clients.all().none { it.fingerprint == fingerprint })
        McpSettingsReceiver.send(context, McpSettingsReceiver.REFRESH)
    }

    @Test fun setSharedStorageRead() {
        val value = arguments.getString(ARG_SHARED_READ)
        assumeTrue("pass -e mcpEvidenceSharedRead true|false", value != null)
        assertTrue("value must be true or false", value == "true" || value == "false")
        val store = ToolPermissionStore(context)
        val previous = store.load().allowSharedStorageRead
        store.update { it.copy(allowSharedStorageRead = value == "true") }
        assertTrue(store.load().allowSharedStorageRead == (value == "true"))
        McpSettingsReceiver.send(context, McpSettingsReceiver.REFRESH)
        instrumentation.sendStatus(STATUS_CODE, Bundle().apply { putString(ARG_SHARED_READ, previous.toString()) })
    }

    /**
     * Sets the switches of one plugin tool group (roadmap P12): `-e mcpEvidencePluginGroup <shortName>:<enabled>:<writes>` with
     * `true`, `false` or `default` for each; reports the previous `<enabled>:<writes>` so the scenario can restore it.
     */
    @Test fun setPluginGroup() {
        val value = arguments.getString(ARG_PLUGIN_GROUP)
        assumeTrue("pass -e mcpEvidencePluginGroup <shortName>:<enabled>:<writes>", value != null)
        val parts = value!!.split(':')
        assertTrue("expected <shortName>:<enabled>:<writes>", parts.size == 3 && parts[1] in CHOICES && parts[2] in CHOICES)
        fun choice(text: String): Boolean? = if (text == "default") null else text == "true"
        val shortName = parts[0]
        val store = ToolPermissionStore(context)
        val previous = store.load().pluginGroups[shortName]
        val next = PluginGroupSwitch(choice(parts[1]), choice(parts[2]))
        store.update { permissions ->
            permissions.copy(pluginGroups = if (next == PluginGroupSwitch()) permissions.pluginGroups - shortName else permissions.pluginGroups + (shortName to next))
        }
        assertTrue((store.load().pluginGroups[shortName] ?: PluginGroupSwitch()) == next)
        McpSettingsReceiver.send(context, McpSettingsReceiver.REFRESH)
        fun text(choice: Boolean?) = choice?.toString() ?: "default"
        instrumentation.sendStatus(STATUS_CODE, Bundle().apply { putString(ARG_PLUGIN_GROUP, "${text(previous?.enabled)}:${text(previous?.writes)}") })
    }

    private companion object {
        const val ARG_PLUGIN_GROUP = "mcpEvidencePluginGroup"
        val CHOICES = setOf("true", "false", "default")
        const val ARG_TOKEN = "mcpEvidenceToken"
        const val ARG_PAIR = "mcpEvidencePair"
        const val ARG_REVOKE = "mcpEvidenceRevoke"
        const val ARG_SHARED_READ = "mcpEvidenceSharedRead"
        const val STATUS_CODE = 2
        val FINGERPRINT = Regex("[0-9a-f]{24}")
    }
}
