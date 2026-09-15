package io.github.supermonster003.autojs6.plugin.mcp.server.server

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Whether the user (or the vendor's default battery policy) restricted this app in the
 * background (roadmap P6). Android then stops the app's services, the foreground service
 * included, about a minute after its uid goes idle, which on a phone means: the screen is off
 * and the device is on battery. HyperOS applies that policy to sideloaded apps by default, so
 * the listener disappeared 73 s after the screen went dark on the Xiaomi Pad until the app's
 * battery policy was set to unrestricted.
 */
object BackgroundRestriction {

    /** True when the app's battery usage is restricted (API 28+; older releases have no such switch). */
    fun isRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return runCatching { context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true }.getOrDefault(false)
    }

    const val WARNING = "Android restricts this app in the background; the listener may be stopped while the screen is off. Choose unrestricted battery usage for MCP Server in the system settings."
}
