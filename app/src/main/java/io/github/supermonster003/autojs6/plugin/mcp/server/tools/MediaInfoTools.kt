package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Images
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.MediaInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * `mediainfo_read` (roadmap P11 D38): the host's `mediainfo.read(path, {includeInform, includeSections})`
 * through the AutoJs6 MediaInfo plugin. The host's default snapshot (`autojs6-node-mediainfo-snapshot-v1`)
 * groups the fields of MediaInfo's text report by track kind (`general`, `video`, `audio`, ...), one
 * object per track with camelCase keys and MediaInfo's display strings; the result keeps those
 * `sections` whole and adds one summary line per track for the text content. Paths are workspace
 * paths until the shared storage switch (D26) lands; the host checks the canonical path as well.
 */
class MediaInfoTools(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) : ToolFlows {

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? {
        if (spec.name != ToolCatalog.MEDIAINFO_READ) return null
        ToolArguments.validate(spec, arguments)
        val raw = ToolArguments.string(arguments, MediaInfo.PATH)?.trim().orEmpty()
        val path = WorkspacePath.normalize(raw, allowRoot = false)
        val report = ToolArguments.boolean(arguments, MediaInfo.REPORT, false)
        val timeoutMs = ToolArguments.long(arguments, Images.TIMEOUT_MS, MediaInfo.DEFAULT_TIMEOUT_MS).coerceIn(Images.MIN_TIMEOUT_MS, spec.maxTimeoutMs)
        val options = buildJsonObject {
            put("includeInform", report)
            put("includeSections", true)
        }
        return ToolFlow(timeoutMs) { caller ->
            val startedAt = clock()
            val snapshot = when (val outcome = caller.call("mediainfo", "read", JsonArray(listOf(JsonPrimitive(path), options)), timeoutMs, spec.permissions)) {
                is BridgeOutcome.Ok -> outcome.result as? JsonObject ?: ImageInputs.badHost("mediainfo.read returned no snapshot")
                is BridgeOutcome.Failed -> throw ToolFailureException(mapFailure(outcome.failure))
            }
            val sections = snapshot["sections"] as? JsonObject ?: JsonObject(emptyMap())
            val lines = summary(sections)
            val inform = (snapshot["inform"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            val kept = inform.take(MediaInfo.MAX_REPORT_CHARS)
            val structured = buildJsonObject {
                put("path", raw)
                (snapshot["fileName"] as? JsonPrimitive)?.contentOrNull?.let { put("fileName", it) }
                (snapshot["sizeBytes"] as? JsonPrimitive)?.longOrNull?.let { put("sizeBytes", it) }
                put("trackCounts", buildJsonObject { sections.forEach { (kind, tracks) -> put(kind, (tracks as? JsonArray)?.size ?: 0) } })
                put("sections", sections)
                if (report) {
                    put("report", kept)
                    put("reportTruncated", kept.length < inform.length)
                }
                put("durationMs", (clock() - startedAt).coerceAtLeast(0))
            }
            val text = buildString {
                append(lines.ifEmpty { listOf("(MediaInfo found no tracks)") }.joinToString("\n"))
                if (report && kept.isNotBlank()) append("\n\n").append(kept.trimEnd())
            }
            ToolFlowResult(structured, text)
        }
    }

    /** One line per track: its kind and number, then the fields worth a glance, or the first few when none of them is present. */
    private fun summary(sections: JsonObject): List<String> = sections.flatMap { (kind, value) ->
        val tracks = (value as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
        tracks.mapIndexed { index, track ->
            val fields = track.mapNotNull { (key, field) -> (field as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }?.let { key to it } }.toMap()
            val wanted = SUMMARY_KEYS[kind] ?: SUMMARY_KEYS.getValue("other")
            val chosen = wanted.mapNotNull { key -> fields[key]?.let { key to it } }.ifEmpty { fields.entries.take(FALLBACK_FIELDS).map { it.key to it.value } }
            val label = if (tracks.size > 1) "$kind #${index + 1}" else kind
            "$label: " + chosen.joinToString(", ") { (key, field) -> "$key=$field" }
        }
    }

    /** A missing or unusable MediaInfo plugin is `PLUGIN_UNAVAILABLE` rather than the generic host mapping of `unavailable`. */
    private fun mapFailure(failure: ToolFailure): ToolFailure =
        if (failure.category == BridgeError.CATEGORY_UNAVAILABLE && failure.module == "mediainfo") {
            ToolFailure.pluginUnavailable(ToolCatalog.MEDIAINFO_READ, ToolGroup.MEDIAINFO.family!!, "not installed, disabled, or not usable", failure.message)
        } else {
            failure
        }

    companion object {

        /** Field keys as the host derives them from the report labels ("Overall bit rate" -> overallBitRate, "Channel(s)" -> channels). */
        val SUMMARY_KEYS: Map<String, List<String>> = mapOf(
            "general" to listOf("format", "duration", "fileSize", "overallBitRate", "title", "album", "performer", "trackName", "recordedDate", "encodedDate"),
            "video" to listOf("format", "codecId", "width", "height", "displayAspectRatio", "frameRate", "bitRate", "duration", "rotation", "hdrFormat"),
            "audio" to listOf("format", "channels", "samplingRate", "bitRate", "duration", "language", "title"),
            "text" to listOf("format", "language", "title"),
            "image" to listOf("format", "width", "height", "colorSpace", "bitDepth"),
            "menu" to listOf("format"),
            "other" to listOf("format", "type", "title"),
        )

        private const val FALLBACK_FIELDS = 6
    }
}
