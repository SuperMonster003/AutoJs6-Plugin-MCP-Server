package io.github.supermonster003.autojs6.plugin.mcp.server

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ApplicationTextPunctuationTest {
    @Test
    fun `application and generated text use approved punctuation`() {
        val projectRoot = findProjectRoot()
        val sourceRoots = listOf(
            projectRoot.resolve("app/src/main"),
            projectRoot.resolve(".changelog"),
            projectRoot.resolve(".readme"),
            projectRoot.resolve("README.md"),
            projectRoot.resolve("ROADMAP.md"),
        )
        val violations = mutableListOf<String>()
        sourceRoots.forEach { sourceRoot ->
            Files.walk(sourceRoot).use { paths ->
                paths.forEach { path: Path ->
                    if (!Files.isRegularFile(path) || !path.isApplicationText()) return@forEach
                    nonAsciiPunctuation(String(Files.readAllBytes(path), Charsets.UTF_8))
                        .forEach { codePoint ->
                            violations +=
                                "${projectRoot.relativize(path)}: " +
                                "U+${codePoint.toString(16).uppercase().padStart(4, '0')} " +
                                "(${codePoint.toChar()})"
                        }
                }
            }
        }

        assertTrue(
            "Packaged text contains unapproved punctuation:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private fun Path.isApplicationText(): Boolean =
        toString().endsWith(".xml") ||
            toString().endsWith(".md") ||
            toString().endsWith(".json")

    private fun isNonAsciiPunctuation(codePoint: Int): Boolean {
        if (codePoint <= ASCII_MAXIMUM || codePoint == FOLLOW_VALUE_SEPARATOR) return false
        return when (Character.getType(codePoint)) {
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt(),
            Character.START_PUNCTUATION.toInt(),
            Character.END_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
            Character.FINAL_QUOTE_PUNCTUATION.toInt(),
            Character.OTHER_PUNCTUATION.toInt(),
            -> true
            else -> false
        }
    }

    private fun nonAsciiPunctuation(text: String): Set<Int> = buildSet {
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (isNonAsciiPunctuation(codePoint)) add(codePoint)
            index += Character.charCount(codePoint)
        }
    }

    private companion object {
        const val ASCII_MAXIMUM = 0x7F
        const val FOLLOW_VALUE_SEPARATOR = 0x00B7
    }
}
