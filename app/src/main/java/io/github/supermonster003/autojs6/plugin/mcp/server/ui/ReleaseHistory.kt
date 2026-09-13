package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import java.util.Locale

object ReleaseHistory {
    fun language(locale: Locale): String = when (locale.language) {
        "zh" -> when {
            locale.country in setOf("HK", "MO") -> "zh-Hant-HK"
            locale.script == "Hant" || locale.country == "TW" -> "zh-Hant-TW"
            else -> "zh-Hans"
        }
        "ar", "es", "fr", "ja", "ko", "ru", "en" -> locale.language
        else -> "en"
    }

    fun load(locale: Locale, read: (String) -> String): String {
        val path = "doc/CHANGELOG-${language(locale)}.md"
        return runCatching { read(path) }.getOrElse { read("doc/CHANGELOG-en.md") }
    }
}
