package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ReleaseHistoryTest {
    @Test fun `all ten asset languages and traditional regions resolve`() {
        listOf("en", "ar", "es", "fr", "ja", "ko", "ru", "zh-Hans", "zh-Hant-TW", "zh-Hant-HK").forEach {
            assertEquals(it, ReleaseHistory.language(Locale.forLanguageTag(it)))
        }
        assertEquals("zh-Hant-HK", ReleaseHistory.language(Locale.forLanguageTag("zh-MO")))
        assertEquals("zh-Hant-TW", ReleaseHistory.language(Locale.forLanguageTag("zh-Hant")))
        assertEquals("en", ReleaseHistory.language(Locale.GERMAN))
    }

    @Test fun `missing localized asset falls back to English`() {
        val reads = mutableListOf<String>()
        val result = ReleaseHistory.load(Locale.JAPAN) { path ->
            reads += path
            if (path.endsWith("-en.md")) "English history" else throw java.io.FileNotFoundException()
        }
        assertEquals("English history", result)
        assertEquals(listOf("doc/CHANGELOG-ja.md", "doc/CHANGELOG-en.md"), reads)
    }
}
