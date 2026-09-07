package io.github.supermonster003.autojs6.plugin.mcp.server

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()`.
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
}
