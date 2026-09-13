package io.github.supermonster003.autojs6.plugin.mcp.server

import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog
import io.modelcontextprotocol.kotlin.sdk.types.SUPPORTED_PROTOCOL_VERSIONS
import org.autojs.plugin.mcp.server.api.McpServerContract

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()` and
 * `IMcpServerPlugin.getInfo()` / `getCapabilities()` (roadmap P2.3).
 *
 * Android-specific lookups (package version, localized strings, raw resources) happen in
 * [mcpServerPluginRuntimeInfo]; this class keeps the mapping itself testable on the JVM.
 */
data class McpServerPluginRuntimeInfo(
    val name: String,
    val description: String,
    val instruction: String?,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
) {
    val author: String get() = McpServerPlugin.AUTHOR
    val id: String get() = McpServerPlugin.ID
    val engine: String get() = McpServerPlugin.ENGINE
    val variant: String get() = McpServerPlugin.VARIANT

    /** Empty on purpose: the plugin ships no native code and runs on any ABI. */
    val supportedAbis: Array<String> get() = emptyArray()

    val requiresHostVersion: Long get() = McpServerPlugin.REQUIRED_HOST_VERSION

    /** The control-plane contract version this build implements (`mcpServerContractVersion`). */
    val contractVersion: Int get() = McpServerContract.CONTRACT_VERSION

    /** The tool groups the catalog uses, in the D6 order (`mcpServerToolGroups`). */
    val toolGroups: List<String> get() = ToolCatalog.groups.map { it.id }

    /** MCP protocol versions the SDK negotiates, newest first (`mcpServerProtocolVersions`). */
    val protocolVersions: List<String> get() = SUPPORTED_PROTOCOL_VERSIONS.sortedDescending()

    val sdkVersion: String get() = McpServerPlugin.SDK_VERSION
    val settingsVersion: Int get() = McpServerContract.SETTINGS_VERSION
}
