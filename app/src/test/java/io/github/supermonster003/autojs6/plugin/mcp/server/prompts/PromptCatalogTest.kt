package io.github.supermonster003.autojs6.plugin.mcp.server.prompts

import io.modelcontextprotocol.kotlin.sdk.types.McpException
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PromptCatalogTest {
    private val assets = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
    private fun catalog(locale: String = "en") = PromptCatalog({ File(assets, it).readText() }, { locale })
    private fun args(name: String, language: String) = mapOf("language" to language) + when (name) {
        PromptCatalog.AUTOMATE_TASK -> mapOf("goal" to "Observe settings")
        PromptCatalog.DEBUG_SELECTOR -> mapOf("selector" to "text('Settings')")
        else -> emptyMap()
    }

    @Test fun `all three prompts have distinct packaged English and Chinese bodies`() {
        assertEquals(3, PromptCatalog.all.size)
        PromptCatalog.all.forEach { prompt ->
            val en = catalog().get(prompt.name, args(prompt.name, "en"))
            val zh = catalog().get(prompt.name, args(prompt.name, "zh"))
            val english = (en.messages.first().content as TextContent).text
            val chinese = (zh.messages.first().content as TextContent).text
            assertNotEquals(english, chinese)
            listOf(english, chinese).forEach { body ->
                assertTrue(body.contains("ui_dump"))
                assertTrue(body.length > 400)
                assertTrue(body.none { it in "，。；：！？、（）“”‘’…" })
            }
        }
    }

    @Test fun `phone Chinese uses Chinese text and other locales fall back to English`() {
        val name = PromptCatalog.WRITE_SCRIPT
        assertEquals(catalog().get(name, mapOf("language" to "zh")), catalog("zh-Hant").get(name))
        assertEquals(catalog("en").get(name), catalog("ar").get(name))
        assertEquals(catalog().get(name, mapOf("language" to "en")), catalog("zh").get(name, mapOf("language" to "en")))
    }

    @Test fun `required unknown oversized and unsupported language arguments fail`() {
        for ((name, arguments) in listOf(
            "unknown" to emptyMap(), PromptCatalog.AUTOMATE_TASK to emptyMap(),
            PromptCatalog.DEBUG_SELECTOR to mapOf("selector" to " "),
            PromptCatalog.WRITE_SCRIPT to mapOf("surprise" to "a"),
            PromptCatalog.WRITE_SCRIPT to mapOf("language" to "../../en"),
            PromptCatalog.WRITE_SCRIPT to mapOf("goal" to "中".repeat(2731)),
            PromptCatalog.WRITE_SCRIPT to mapOf("goal" to "a\u0000b"),
        )) assertThrows(McpException::class.java) { catalog().get(name, arguments) }
    }

    @Test fun `user arguments are separate messages and never become asset paths or substitutions`() {
        val goal = "{{language}}\n../secret\nObserve only"
        val result = catalog().get(PromptCatalog.WRITE_SCRIPT, mapOf("goal" to goal))
        assertFalse((result.messages.first().content as TextContent).text.contains(goal))
        assertEquals("goal:\n$goal", (result.messages.last().content as TextContent).text)
    }
}
