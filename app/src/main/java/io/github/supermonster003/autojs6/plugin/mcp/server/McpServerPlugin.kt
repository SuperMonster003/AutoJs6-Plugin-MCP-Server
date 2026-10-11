package io.github.supermonster003.autojs6.plugin.mcp.server

import org.autojs.plugin.common.api.PluginActions

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D5
 * and phase P1.1); the JVM manifest contract test fails when the manifest drifts from them.
 */
object McpServerPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.mcp.server"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    const val ID = "mcp-server"
    const val ENGINE = "mcp-server"
    const val VARIANT = "default"
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [McpServerPluginService]. */
    const val SERVICE_ACTION = "org.autojs.plugin.MCP_SERVER"
    const val SERVICE_CATEGORY = "mcp-server"

    /** Discovery contract of [McpServerPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `IMcpServerPlugin` AIDL that the host defines in its
     * `mcp-server-api` module (roadmap P1.1). Until that contract is staged in `libs/`, the
     * service exposes a placeholder Binder carrying only this descriptor.
     */
    const val SERVICE_DESCRIPTOR = "org.autojs.plugin.mcp.server.api.IMcpServerPlugin"

    /**
     * Minimum AutoJs6 `versionCode` able to list this plugin. The current value is the 6.8.0
     * host build whose compact dumps keep node snapshots and relocate `{nodeRef, snapshotId}`
     * targets with the fixes of builds 5290 - 5297 (roadmap P10); earlier builds from 5279 on
     * served plugin 1.0.x, which kept its own references.
     */
    const val REQUIRED_HOST_VERSION = 5298L

    /**
     * First AutoJs6 build that binds the listener with `BIND_ALLOW_ACTIVITY_STARTS` (host commit
     * 6b3ae51de6): on Android 14 and later the pairing dialog then also opens while AutoJs6 is in
     * the foreground, not only while a window of this plugin is.
     */
    const val FOREGROUND_PAIRING_HOST_VERSION = 5333L

    /** Whether a host of [hostVersionCode] lets the pairing dialog open in front of AutoJs6. */
    fun showsPairingOverHost(hostVersionCode: Long?): Boolean = hostVersionCode != null && hostVersionCode >= FOREGROUND_PAIRING_HOST_VERSION

    /** Default TCP port of the MCP endpoint (decision D16); loopback only unless the user opts in. */
    const val DEFAULT_PORT = 9637
    const val ENDPOINT_PATH = "/mcp"

    /**
     * The MCP Kotlin SDK the listener is built with, reported to the host as
     * `mcpServerSdkVersion`; `McpServerPluginRuntimeInfoTest` keeps it equal to the Gradle catalog.
     */
    const val SDK_VERSION = "0.15.0"
}
