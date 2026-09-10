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
     * host build that ships the MCP contract module and the capability broker (roadmap P1.4).
     */
    const val REQUIRED_HOST_VERSION = 5279L

    /** Default TCP port of the MCP endpoint (decision D16); loopback only unless the user opts in. */
    const val DEFAULT_PORT = 9637
    const val ENDPOINT_PATH = "/mcp"

    /**
     * The MCP Kotlin SDK the listener is built with, reported to the host as
     * `mcpServerSdkVersion`; `McpServerPluginRuntimeInfoTest` keeps it equal to the Gradle catalog.
     */
    const val SDK_VERSION = "0.15.0"
}
