package io.github.supermonster003.autojs6.plugin.mcp.server

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mcp.server.api.McpServerCapabilityKeys

/** Collects the installed package version and the localized metadata of this plugin. */
internal fun Context.mcpServerPluginRuntimeInfo(): McpServerPluginRuntimeInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    return McpServerPluginRuntimeInfo(
        name = getString(R.string.app_name),
        description = getString(R.string.plugin_description),
        instruction = resources.openRawResource(R.raw.plugin_instruction)
            .bufferedReader()
            .use { it.readText() },
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = versionCode,
        versionDate = getString(R.string.plugin_version_date),
    )
}

/** Maps the pure-data view onto the host contract parcelable. */
internal fun McpServerPluginRuntimeInfo.toPluginInfo(): PluginInfo {
    val runtimeInfo = this
    return PluginInfo().apply {
        name = runtimeInfo.name
        description = runtimeInfo.description
        instruction = runtimeInfo.instruction
        author = runtimeInfo.author
        collaborators = null
        versionName = runtimeInfo.versionName
        versionCode = runtimeInfo.versionCode
        versionDate = runtimeInfo.versionDate
        id = runtimeInfo.id
        engine = runtimeInfo.engine
        variant = runtimeInfo.variant
        supportedAbis = runtimeInfo.supportedAbis
        capabilities = runtimeInfo.capabilitiesBundle()
    }
}

/**
 * The capabilities the host reads before it opens a server (roadmap P2.3): the required host
 * version, the contract version, the tool groups, the protocol versions, and the SDK version.
 */
internal fun McpServerPluginRuntimeInfo.capabilitiesBundle(): Bundle = Bundle().apply {
    putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiresHostVersion)
    putInt(McpServerCapabilityKeys.CONTRACT_VERSION, contractVersion)
    putStringArray(McpServerCapabilityKeys.TOOL_GROUPS, toolGroups.toTypedArray())
    putStringArray(McpServerCapabilityKeys.PROTOCOL_VERSIONS, protocolVersions.toTypedArray())
    putString(McpServerCapabilityKeys.SDK_VERSION, sdkVersion)
}
