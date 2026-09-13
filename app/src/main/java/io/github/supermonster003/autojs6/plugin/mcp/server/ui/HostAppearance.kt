package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.content.Context
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.os.LocaleList
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as HostSettings
import java.util.Locale

/** Optional host snapshot. A missing, old, or inaccessible provider uses Android's configuration. */
data class HostAppearance(val languageTag: String, val dark: Boolean, val primary: Int, val accent: Int) {
    @SuppressLint("AppBundleLocaleChanges") // The single APK contains every locale; no language split is distributed.
    fun wrap(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(languageTag)))
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return context.createConfigurationContext(config)
    }

    companion object {
        fun read(context: Context): HostAppearance {
            val config = context.resources.configuration
            val fallback = HostAppearance(config.locales[0].toLanguageTag(),
                config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
                0xff00695c.toInt(), 0xff008577.toInt())
            return runCatching {
                val settings = context.contentResolver.call(Uri.parse(HostSettings.CONTENT_URI), HostSettings.METHOD_GET_SETTINGS, null, null)
                    ?: return fallback
                if (settings.getInt(HostSettings.KEY_PROTOCOL_VERSION) != HostSettings.PROTOCOL_VERSION) return fallback
                val tag = settings.getString(HostSettings.KEY_RESOLVED_LANGUAGE_TAG)?.takeIf { Locale.forLanguageTag(it).language.isNotEmpty() }
                    ?: fallback.languageTag
                fallback.copy(languageTag = tag, dark = settings.getBoolean(HostSettings.KEY_DARK_MODE_ACTIVE, fallback.dark),
                    primary = settings.getInt(HostSettings.KEY_THEME_COLOR_PRIMARY, fallback.primary),
                    accent = settings.getInt(HostSettings.KEY_THEME_COLOR_ACCENT, fallback.accent))
            }.getOrDefault(fallback)
        }
    }
}
