package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Pinyin
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Pinyin4j
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * `pinyin_convert` and `pinyin4j_convert` (roadmap P11 D38): plain-text tools over the host's
 * `pinyin` and `pinyin4j` modules, with only the common options exposed.
 *
 * - `pinyin_convert` maps onto `pinyin.convert(text, {style, mode, segment, heteronym})` (the
 *   Pinyin plugin: phrase dictionary and word segmentation). Segmentation defaults to on here; it
 *   splits longer text into words before the dictionary lookup (2026-10-15 on a device, "银行行长"
 *   read the same with and without it, so the readings come from the dictionary itself).
 * - `pinyin4j_convert` maps onto `pinyin4j.readings(text, {romanization, tone?, case?})` (the
 *   Pinyin4j plugin: one row of readings per code point, the code point itself when it has none).
 *   The text result joins the first reading of each row with the separator, which is what
 *   `pinyin4j.of` returns, so one host method covers both the joined form and `allReadings`.
 *
 * Both answer the host's string matrix as `rows`; the text shows alternatives joined by `|`.
 */
class PinyinTools : ToolFlows {

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.PINYIN_CONVERT -> {
            val text = text(spec, arguments)
            val style = ToolArguments.string(arguments, Pinyin.STYLE, "tone")!!
            val mode = ToolArguments.string(arguments, Pinyin.MODE, "normal")!!
            val segment = ToolArguments.boolean(arguments, Pinyin.SEGMENT, true)
            val heteronym = ToolArguments.boolean(arguments, Pinyin.HETERONYM, false)
            val options = buildJsonObject {
                put("style", style.uppercase())
                put("mode", mode.uppercase())
                put("segment", segment)
                put("heteronym", heteronym)
            }
            ToolFlow(spec.timeoutMs) { caller ->
                val rows = rows(spec, caller.callOrThrow("pinyin", "convert", JsonArray(listOf(JsonPrimitive(text), options)), spec.timeoutMs, spec.permissions))
                val joined = rows.joinToString(" ") { it.joinToString("|") }
                val structured = buildJsonObject {
                    put("pinyin", joined)
                    put("rows", rowsJson(rows))
                    put(Pinyin.STYLE, style)
                    put(Pinyin.MODE, mode)
                    put(Pinyin.SEGMENT, segment)
                    put(Pinyin.HETERONYM, heteronym)
                }
                ToolFlowResult(structured, joined)
            }
        }
        ToolCatalog.PINYIN4J_CONVERT -> {
            val text = text(spec, arguments)
            val romanization = ToolArguments.string(arguments, Pinyin4j.ROMANIZATION, "hanyu")!!
            val hanyu = romanization == "hanyu"
            if (!hanyu) {
                listOf(Pinyin4j.TONE, Pinyin4j.CASE).firstOrNull { arguments[it] != null && arguments[it] !is JsonNull }?.let { name ->
                    invalid(spec, "$name applies to hanyu only; $romanization has its own fixed spelling")
                }
            }
            val tone = ToolArguments.string(arguments, Pinyin4j.TONE, "mark")!!
            val case = ToolArguments.string(arguments, Pinyin4j.CASE, "lower")!!
            val separator = ToolArguments.string(arguments, Pinyin4j.SEPARATOR, " ")!!
            val allReadings = ToolArguments.boolean(arguments, Pinyin4j.ALL_READINGS, false)
            val options = buildJsonObject {
                put("romanization", Pinyin4j.HOST_ROMANIZATIONS.getValue(romanization))
                if (hanyu) {
                    put("tone", Pinyin4j.HOST_TONES.getValue(tone))
                    put("case", Pinyin4j.HOST_CASES.getValue(case))
                }
            }
            ToolFlow(spec.timeoutMs) { caller ->
                val rows = rows(spec, caller.callOrThrow("pinyin4j", "readings", JsonArray(listOf(JsonPrimitive(text), options)), spec.timeoutMs, spec.permissions))
                val joined = rows.joinToString(separator) { row -> if (allReadings) row.joinToString("|") else row.first() }
                val structured = buildJsonObject {
                    put("text", joined)
                    put("rows", rowsJson(if (allReadings) rows else rows.map { listOf(it.first()) }))
                    put(Pinyin4j.ROMANIZATION, romanization)
                    if (hanyu) put(Pinyin4j.TONE, tone)
                    if (hanyu) put(Pinyin4j.CASE, case)
                    put(Pinyin4j.ALL_READINGS, allReadings)
                }
                ToolFlowResult(structured, joined)
            }
        }
        else -> null
    }

    private fun text(spec: ToolSpec, arguments: JsonObject): String {
        ToolArguments.validate(spec, arguments)
        val text = ToolArguments.string(arguments, Pinyin.TEXT).orEmpty()
        if (text.isBlank()) invalid(spec, "text must not be empty")
        return text
    }

    /** The host's `[[reading, ...], ...]`; a row is never empty (the host puts the source character there when it has no reading). */
    private fun rows(spec: ToolSpec, result: JsonElement): List<List<String>> {
        val matrix = result as? JsonArray ?: ImageInputs.badHost("${spec.bridge?.key} returned no list of rows")
        return matrix.map { row ->
            val readings = (row as? JsonArray)?.map { (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.contentOrNull ?: ImageInputs.badHost("a reading is not a string") }
                ?: ImageInputs.badHost("a row of ${spec.bridge?.key} is not a list")
            readings.ifEmpty { listOf("") }
        }
    }

    private fun rowsJson(rows: List<List<String>>): JsonArray = JsonArray(rows.map { row -> JsonArray(row.map { JsonPrimitive(it) }) })

    private fun invalid(spec: ToolSpec, message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments("${spec.name}: $message"))
}
