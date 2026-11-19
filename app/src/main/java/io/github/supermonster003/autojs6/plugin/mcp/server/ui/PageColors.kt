package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.graphics.Color

/**
 * The colors of the plugin's own pages, derived from the host appearance (theme colors and night mode). The View
 * settings pages ([SettingsPageActivity]) and the Compose release history page ([ReleaseHistoryActivity]) both use
 * them, so the pages look the same.
 */
internal class PageColors(private val appearance: HostAppearance) {
    val dark: Boolean get() = appearance.dark

    /** The app bar (and status bar) color: the host's primary theme color. */
    val primary: Int get() = appearance.primary

    /** Text and icons on [primary]. */
    val onPrimary: Int get() = if (lightPrimary) Color.BLACK else Color.WHITE

    /** True when [primary] is light enough for dark status bar icons. */
    val lightPrimary: Boolean get() = Color.luminance(appearance.primary) > .179

    val text: Int get() = if (dark) Color.WHITE else 0xff202522.toInt()
    val secondary: Int get() = if (dark) 0xffbdbdbd.toInt() else 0xff575757.toInt()
    val surface: Int get() = if (dark) 0xff242424.toInt() else Color.WHITE
    val pageBackground: Int get() = blend(if (dark) 0xff141414.toInt() else 0xfff5f5f5.toInt(), appearance.primary, .04f)

    /** The accent color, darkened or lightened until it reads on [surface] (contrast 4.5 or more). */
    val control: Int get() {
        val base = appearance.accent or (0xff shl 24)
        val target = if (dark) Color.WHITE else Color.BLACK
        return (0..20).asSequence().map { blend(base, target, it / 20f) }
            .firstOrNull { contrast(it, surface) >= 4.5 } ?: target
    }

    /** The background of the page buttons, also the container of the release history category labels. */
    val controlContainer: Int get() = blend(surface, appearance.primary, .14f)

    /** Hairlines between the groups of a list. */
    val divider: Int get() = blend(pageBackground, secondary, .3f)

    /** The navigation bar below Android 8, which cannot draw dark navigation bar icons. */
    val legacyNavigationBar: Int get() = 0xff141414.toInt()

    companion object {
        fun blend(background: Int, foreground: Int, amount: Float): Int = Color.rgb(
            (Color.red(background) * (1 - amount) + Color.red(foreground) * amount).toInt(),
            (Color.green(background) * (1 - amount) + Color.green(foreground) * amount).toInt(),
            (Color.blue(background) * (1 - amount) + Color.blue(foreground) * amount).toInt(),
        )

        fun contrast(first: Int, second: Int): Double {
            val a = Color.luminance(first) + .05
            val b = Color.luminance(second) + .05
            return maxOf(a, b) / minOf(a, b)
        }
    }
}
