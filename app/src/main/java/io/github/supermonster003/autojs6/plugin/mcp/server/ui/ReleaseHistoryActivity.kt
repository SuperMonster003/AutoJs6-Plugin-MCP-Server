package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.supermonster003.autojs6.plugin.mcp.server.R
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.compose.McpPageTheme
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.compose.ReleaseHistoryScreen

/**
 * Renders the bundled changelog for the current locale (English when that one is missing) as one group per released
 * version, under the title "Release history" with the plugin name as the subtitle.
 *
 * The settings pages extend [SettingsPageActivity], an `android.app.Activity`; this Compose page extends
 * [ComponentActivity] instead and takes the same language, night mode and colors from [HostAppearance] and
 * [PageColors], so it looks like the settings page that opens it.
 */
class ReleaseHistoryActivity : ComponentActivity() {
    private lateinit var appearance: HostAppearance

    override fun attachBaseContext(newBase: Context) {
        appearance = HostAppearance.read(newBase)
        super.attachBaseContext(appearance.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appearance.themeResource)
        val colors = PageColors(appearance)
        enableEdgeToEdge(
            statusBarStyle = if (colors.lightPrimary) SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            else SystemBarStyle.dark(Color.TRANSPARENT),
            // Below Android 8 the navigation bar cannot draw dark icons, so a light page keeps a dark bar there.
            navigationBarStyle = if (colors.dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, colors.legacyNavigationBar),
        )
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings_release_history)
        val candidates = ReleaseHistoryAssetPolicy.candidatesFor(resources.configuration.locales[0])
        val entries = loadReleaseHistory(candidates) { path ->
            assets.open(path).use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        }
        setContent {
            McpPageTheme(colors) {
                ReleaseHistoryScreen(entries = entries, onBack = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The host language, night mode or theme colors changed while this page was in the background.
        if (HostAppearance.read(this) != appearance) recreate()
    }
}
