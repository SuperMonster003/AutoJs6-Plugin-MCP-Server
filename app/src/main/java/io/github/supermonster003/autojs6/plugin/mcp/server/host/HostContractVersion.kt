package io.github.supermonster003.autojs6.plugin.mcp.server.host

/**
 * Which control-plane contract the plugin declares as `mcpServerContractVersion` (roadmap P10).
 *
 * The host reads the declared version before it opens a server and calls `openServer` for 1 or
 * `openServerV2` for 2; a host that only knows version 1 rejects a plugin declaring 2 as invalid.
 * The plugin therefore declares 2 only to host builds that accept it. Every host the plugin
 * supports (`McpServerPlugin.REQUIRED_HOST_VERSION`) does; an older one still gets version 1, so it
 * can read the plugin's metadata and report the required host version instead of a contract
 * error. Both transactions stay implemented.
 */
object HostContractVersion {

    const val V1 = 1
    const val V2 = 2

    /** First host build accepting contract version 2 (AutoJs6 6.8.0 / 5283, commit 2201068c9e). */
    const val V2_MIN_HOST_VERSION = 5283L

    /** The version to declare to an installed host of [hostVersionCode]; null when the host is not installed. */
    fun forHostVersion(hostVersionCode: Long?): Int =
        if (hostVersionCode != null && hostVersionCode >= V2_MIN_HOST_VERSION) V2 else V1
}
