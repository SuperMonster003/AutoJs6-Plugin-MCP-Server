package io.github.supermonster003.autojs6.plugin.mcp.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps `AndroidManifest.xml` and [McpServerPlugin] from drifting apart: the host discovers the
 * plugin through the manifest, while the services and tests use the Kotlin constants.
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val path = findProjectRoot().resolve("app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    @Test
    fun `manifest declares the plugin permission and queries the host package`() {
        val permissions = manifest.children("uses-permission").map { it.androidAttribute("name") }
        assertTrue(PLUGIN_PERMISSION in permissions)
        assertTrue("android.permission.INTERNET" in permissions)

        val queried = manifest.child("queries").children("package").map { it.androidAttribute("name") }
        assertEquals(listOf(McpServerPlugin.HOST_PACKAGE_NAME), queried)
    }

    @Test
    fun `application metadata points at the wake activity and the author string`() {
        val application = manifest.child("application")
        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("@string/app_name", application.androidAttribute("label"))
        assertEquals("@mipmap/ic_launcher", application.androidAttribute("icon"))

        val metaData = application.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(".WakeActivity", metaData["org.autojs.plugin.WAKE_ACTIVITY"])
        assertEquals("@string/plugin_author", metaData["org.autojs.plugin.info.AUTHOR"])

        val activities = application.children("activity")
        assertEquals(listOf(".LocalNetworkPermissionActivity", ".WakeActivity", ".ui.McpServerSettingsActivity", ".ui.ReleaseHistoryActivity", ".ui.PairingConfirmActivity"), activities.map { it.androidAttribute("name") })
        val permission = activities.single { it.androidAttribute("name") == ".LocalNetworkPermissionActivity" }
        assertEquals("true", permission.androidAttribute("exported"))
        assertEquals("true", permission.androidAttribute("excludeFromRecents"))
        assertEquals(PLUGIN_PERMISSION, permission.androidAttribute("permission"))
        assertEquals("@android:style/Theme.Translucent.NoTitleBar", permission.androidAttribute("theme"))
        assertTrue(permission.children("intent-filter").isEmpty())
        val wake = activities.single { it.androidAttribute("name") == ".WakeActivity" }
        assertEquals("true", wake.androidAttribute("exported"))
        assertEquals("true", wake.androidAttribute("excludeFromRecents"))
        assertEquals("true", wake.androidAttribute("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.androidAttribute("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.androidAttribute("theme"))
        val filter = wake.child("intent-filter")
        assertEquals(listOf("org.autojs.plugin.action.WAKE"), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), filter.children("category").map { it.androidAttribute("name") })

        val pairing = activities.last()
        assertEquals("false", pairing.androidAttribute("exported"))
        assertEquals("true", pairing.androidAttribute("excludeFromRecents"))
        assertEquals(":mcp_server", pairing.androidAttribute("process"))
        assertTrue(pairing.children("intent-filter").isEmpty())

        val receivers = application.children("receiver")
        assertEquals(setOf(".ui.PairingDecisionReceiver", ".ui.McpSettingsReceiver"), receivers.map { it.androidAttribute("name") }.toSet())
        receivers.forEach { receiver ->
            assertEquals("false", receiver.androidAttribute("exported"))
            assertEquals(":mcp_server", receiver.androidAttribute("process"))
            assertTrue(receiver.children("intent-filter").isEmpty())
        }
        val settings = activities.single { it.androidAttribute("name") == ".ui.McpServerSettingsActivity" }
        assertEquals("true", settings.androidAttribute("exported"))
        assertEquals(PLUGIN_PERMISSION, settings.androidAttribute("permission"))
        assertNull(settings.androidAttributeOrNull("process"))
        assertEquals(org.autojs.plugin.mcp.server.api.McpServerActions.OPEN_SETTINGS,
            settings.child("intent-filter").children("action").single().androidAttribute("name"))
        val history = activities.single { it.androidAttribute("name") == ".ui.ReleaseHistoryActivity" }
        assertEquals("false", history.androidAttribute("exported"))
    }

    @Test
    fun `info service and mcp server service match the identity constants`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".McpServerPluginInfoService", ".McpServerPluginService", ".McpServerService"), services.keys)

        val info = services.getValue(".McpServerPluginInfoService")
        assertDiscoveryContract(info, McpServerPlugin.INFO_ACTION)
        assertNull(info.androidAttributeOrNull("process"))

        val server = services.getValue(".McpServerPluginService")
        assertDiscoveryContract(server, McpServerPlugin.SERVICE_ACTION)
        assertEquals(":mcp_server", server.androidAttribute("process"))
    }

    @Test
    fun `listener service is a special-use foreground service guarded by the DUMP permission`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        val listener = services.getValue(".McpServerService")
        assertEquals("true", listener.androidAttribute("exported"))
        assertEquals("true", listener.androidAttribute("enabled"))
        assertEquals("android.permission.DUMP", listener.androidAttribute("permission"))
        assertEquals("specialUse", listener.androidAttribute("foregroundServiceType"))
        assertEquals(":mcp_server", listener.androidAttribute("process"))
        assertTrue(listener.children("intent-filter").isEmpty())
        val property = listener.child("property")
        assertEquals("android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE", property.androidAttribute("name"))
        assertTrue(property.androidAttribute("value").isNotBlank())

        val permissions = manifest.children("uses-permission").map { it.androidAttribute("name") }
        assertTrue("android.permission.FOREGROUND_SERVICE" in permissions)
        assertTrue("android.permission.FOREGROUND_SERVICE_SPECIAL_USE" in permissions)
    }

    private fun assertDiscoveryContract(service: Element, action: String) {
        assertEquals("true", service.androidAttribute("exported"))
        assertEquals("true", service.androidAttribute("enabled"))
        assertEquals(PLUGIN_PERMISSION, service.androidAttribute("permission"))
        val filter = service.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf(McpServerPlugin.SERVICE_CATEGORY), filter.children("category").map { it.androidAttribute("name") })
        val metaData = service.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(McpServerPlugin.REQUIRED_HOST_VERSION.toString(), metaData["requiresHostVersion"])
    }

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == tag }
    }

    private fun Element.child(tag: String): Element = children(tag).single()

    private fun Element.androidAttribute(name: String): String =
        androidAttributeOrNull(name) ?: error("Missing android:$name on <$tagName>")

    private fun Element.androidAttributeOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NAMESPACE, name)) getAttributeNS(ANDROID_NAMESPACE, name) else null

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
    }
}
