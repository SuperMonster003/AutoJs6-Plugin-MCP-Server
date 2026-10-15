package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Ocr
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * `ocr_recognize` (roadmap P11 D23 / D24 / D25 / D38): text recognition through an AutoJs6 OCR
 * plugin, without an image ever leaving the phone.
 *
 * - `image = "screen"` maps onto `accessibility.readScreenText`, which captures and recognizes
 *   inside the host (accessibility screenshot on Android 11+, MediaProjection otherwise). A
 *   `permission-denied` answer means MediaProjection consent is missing: the flow asks for it once
 *   through `media_projection.requestScreenCapture`, like `screen_capture`, and retries.
 * - A path runs `image.readImage` -> `ocr.recognize` -> `image.recycle` in one call, the recycle
 *   in a `finally`. The host decodes large files downscaled (D45) and reports `scale`; the region
 *   is converted to decoded pixels and the bounds back to file pixels, so callers only ever see
 *   the file's own coordinates.
 *
 * Both answers are the host's `[{text, confidence, bounds}]` list; the result adds the source,
 * the engine, the joined text, and the file dimensions. A missing OCR plugin is reported as
 * `PLUGIN_UNAVAILABLE` rather than the generic mapping of the host's `unavailable` category.
 */
class OcrTools(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) : ToolFlows {

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.OCR_RECOGNIZE -> {
            val options = Options.parse(arguments)
            ToolFlow(options.timeoutMs) { caller -> recognize(caller, options) }
        }
        else -> null
    }

    private suspend fun recognize(caller: BridgeCaller, options: Options): ToolFlowResult {
        val startedAt = clock()
        return if (options.screen) recognizeScreen(caller, options, startedAt) else recognizeFile(caller, options, startedAt)
    }

    private suspend fun recognizeScreen(caller: BridgeCaller, options: Options, startedAt: Long): ToolFlowResult {
        val hostOptions = buildJsonObject {
            options.region?.let { put("region", it.hostRegion(1.0, null, null)) }
            options.hostEngine?.let { put(Ocr.ENGINE, it) }
        }
        var outcome = caller.call("accessibility", "readScreenText", JsonArray(listOf(hostOptions)), options.timeoutMs, SCREEN_PERMISSIONS)
        if (outcome is BridgeOutcome.Failed && outcome.failure.category == BridgeError.CATEGORY_PERMISSION_DENIED) {
            // Below Android 11, or without the accessibility service, the host captures through MediaProjection, which needs consent once.
            val consent = caller.call("media_projection", "requestScreenCapture", CONSENT_ARGS, ToolCatalog.Screen.CONSENT_TIMEOUT_MS, CAPTURE_PERMISSIONS)
            if (consent is BridgeOutcome.Failed) throw ToolFailureException(consent.failure.copy(hint = ScreenTools.CONSENT_HINT))
            outcome = caller.call("accessibility", "readScreenText", JsonArray(listOf(hostOptions)), options.timeoutMs, SCREEN_PERMISSIONS)
        }
        val items = when (outcome) {
            is BridgeOutcome.Ok -> items(outcome.result, 1.0)
            is BridgeOutcome.Failed -> throw ToolFailureException(mapFailure(outcome.failure, consentHint = true))
        }
        return result(options, "screen", items, startedAt) { }
    }

    private suspend fun recognizeFile(caller: BridgeCaller, options: Options, startedAt: Long): ToolFlowResult {
        val handle = caller.callOrThrow("image", "readImage", JsonArray(listOf(JsonPrimitive(options.path))), Ocr.READ_TIMEOUT_MS, IMAGE_PERMISSIONS) as? JsonObject
            ?: badHost("image.readImage returned no image handle")
        val handleId = handle["__autojs6ImageHandle"]?.string() ?: handle["id"]?.string() ?: badHost("image.readImage returned no handle id")
        try {
            val width = handle.positiveInt("width")
            val height = handle.positiveInt("height")
            val sourceWidth = (handle["sourceWidth"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: width
            val sourceHeight = (handle["sourceHeight"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: height
            val scale = (handle["scale"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() && it > 0.0 && it <= 1.0 } ?: 1.0
            val hostOptions = buildJsonObject {
                options.region?.let { region ->
                    if (region.right > sourceWidth || region.bottom > sourceHeight) invalid("region lies outside the image ($sourceWidth x $sourceHeight)")
                    put("region", region.hostRegion(scale, width, height))
                }
                options.hostEngine?.let { put(Ocr.ENGINE, it) }
            }
            val handleRef = buildJsonObject { put("id", handleId); put("__autojs6ImageHandle", handleId) }
            val items = when (val outcome = caller.call("ocr", "recognize", JsonArray(listOf(handleRef, hostOptions)), options.timeoutMs, OCR_PERMISSIONS)) {
                is BridgeOutcome.Ok -> items(outcome.result, scale)
                is BridgeOutcome.Failed -> throw ToolFailureException(mapFailure(outcome.failure, consentHint = false))
            }
            return result(options, "file", items, startedAt) {
                put("width", sourceWidth)
                put("height", sourceHeight)
                put("scale", scale)
                if (scale < 1.0) put("decodedWidth", width)
                if (scale < 1.0) put("decodedHeight", height)
            }
        } finally {
            // The handle belongs to the session; a leaked one would count against the host's handle limit until the session closes.
            val handleRef = buildJsonObject { put("id", handleId); put("__autojs6ImageHandle", handleId) }
            runCatching { caller.call("image", "recycle", JsonArray(listOf(handleRef)), Ocr.RECYCLE_TIMEOUT_MS, IMAGE_PERMISSIONS) }
        }
    }

    private fun result(options: Options, source: String, items: List<JsonObject>, startedAt: Long, extra: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): ToolFlowResult {
        val text = items.mapNotNull { (it["text"] as? JsonPrimitive)?.contentOrNull?.takeIf { line -> line.isNotBlank() } }.joinToString("\n")
        val structured = buildJsonObject {
            put("image", options.image)
            put("source", source)
            put("engine", options.engine)
            put("count", items.size)
            put("items", JsonArray(items))
            put("text", text)
            options.region?.let { put("region", it.json) }
            extra()
            put("durationMs", (clock() - startedAt).coerceAtLeast(0))
        }
        return ToolFlowResult(structured, text.ifEmpty { "(no text recognized)" })
    }

    /** The host list `[{text, confidence, bounds}]` with bounds mapped from decoded pixels back to file pixels by [scale]. */
    private fun items(result: JsonElement, scale: Double): List<JsonObject> {
        val array = result as? JsonArray ?: badHost("the OCR result is not a list")
        return array.map { element ->
            val item = element as? JsonObject ?: badHost("an OCR result item is not an object")
            buildJsonObject {
                put("text", item["text"]?.string().orEmpty())
                (item["confidence"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }?.let { put("confidence", it) }
                (item["bounds"] as? JsonObject)?.let { bounds ->
                    val edges = listOf("left", "top", "right", "bottom").map { key -> (bounds[key] as? JsonPrimitive)?.doubleOrNull ?: return@let }
                    put("bounds", buildJsonObject {
                        put("left", (edges[0] / scale).roundToInt())
                        put("top", (edges[1] / scale).roundToInt())
                        put("right", (edges[2] / scale).roundToInt())
                        put("bottom", (edges[3] / scale).roundToInt())
                    })
                }
            }
        }
    }

    /** A missing or unusable OCR plugin is `PLUGIN_UNAVAILABLE`; the host's `unavailable` category would otherwise read as a host or accessibility problem. */
    private fun mapFailure(failure: ToolFailure, consentHint: Boolean): ToolFailure = when {
        failure.category == BridgeError.CATEGORY_UNAVAILABLE && (failure.module == "ocr" || failure.message.contains("OCR_PLUGIN_REQUIRED") || failure.message.contains("recognition plugin")) ->
            ToolFailure.pluginUnavailable(ToolCatalog.OCR_RECOGNIZE, ToolGroup.OCR.family!!, "not installed, disabled, or not usable", failure.message)
        consentHint && failure.category == BridgeError.CATEGORY_PERMISSION_DENIED -> failure.copy(hint = ScreenTools.CONSENT_HINT)
        else -> failure
    }

    private class Region(val left: Int, val top: Int, val right: Int, val bottom: Int, val json: JsonObject) {

        /** The host's `{x, y, width, height}` form, in decoded pixels when the host downscaled the file ([scale] < 1). */
        fun hostRegion(scale: Double, decodedWidth: Int?, decodedHeight: Int?): JsonObject {
            var x = floor(left * scale).toInt()
            var y = floor(top * scale).toInt()
            var right = ceil(this.right * scale).toInt()
            var bottom = ceil(this.bottom * scale).toInt()
            if (decodedWidth != null) { x = x.coerceIn(0, decodedWidth); right = right.coerceIn(0, decodedWidth) }
            if (decodedHeight != null) { y = y.coerceIn(0, decodedHeight); bottom = bottom.coerceIn(0, decodedHeight) }
            if (right <= x || bottom <= y) invalid("region is too small for the decoded image")
            return buildJsonObject { put("x", x); put("y", y); put("width", right - x); put("height", bottom - y) }
        }
    }

    private class Options(val image: String, val region: Region?, val engine: String, val timeoutMs: Long) {

        val screen: Boolean
            get() = image == Ocr.SCREEN

        /** The normalized workspace path; only for a file source. */
        val path: String
            get() = WorkspacePath.normalize(image, allowRoot = false)

        val hostEngine: String?
            get() = Ocr.HOST_ENGINES[engine]

        companion object {

            fun parse(arguments: JsonObject): Options {
                ToolArguments.validate(ToolCatalog.ocrRecognize, arguments)
                val image = ToolArguments.string(arguments, Ocr.IMAGE)?.trim()?.takeIf { it.isNotEmpty() } ?: invalid("image must be \"screen\" or a file path")
                if (image != Ocr.SCREEN) WorkspacePath.normalize(image, allowRoot = false)
                val region = (arguments[Ocr.REGION] as? JsonObject)?.let { json ->
                    val keys = setOf("left", "top", "right", "bottom")
                    if (json.keys != keys) invalid("region must contain exactly left, top, right and bottom")
                    val edges = keys.associateWith { key ->
                        val edge = (json[key] as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull
                        if (edge == null || !edge.isFinite() || edge !in 0.0..65_535.0 || edge != edge.toInt().toDouble()) invalid("region.$key must be an integer within [0, 65535]")
                        edge.toInt()
                    }
                    Region(edges.getValue("left"), edges.getValue("top"), edges.getValue("right"), edges.getValue("bottom"), json).also {
                        if (it.right <= it.left || it.bottom <= it.top) invalid("region must have positive width and height")
                    }
                }
                val engine = ToolArguments.string(arguments, Ocr.ENGINE, "auto")!!
                val timeoutMs = ToolArguments.long(arguments, Ocr.TIMEOUT_MS, Ocr.DEFAULT_TIMEOUT_MS).coerceIn(Ocr.MIN_TIMEOUT_MS, ToolCatalog.ocrRecognize.maxTimeoutMs)
                return Options(image, region, engine, timeoutMs)
            }
        }
    }

    companion object {

        private val SCREEN_PERMISSIONS = listOf("accessibility", "screen_capture", "ocr")
        private val CAPTURE_PERMISSIONS = listOf("screen_capture")
        private val IMAGE_PERMISSIONS = listOf("image")
        private val OCR_PERMISSIONS = listOf("ocr", "image")
        private val CONSENT_ARGS = buildJsonArray { add(buildJsonObject { put("orientation", "auto") }) }

        private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

        private fun JsonObject.positiveInt(key: String): Int = (get(key) as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 }
            ?: badHost("image.readImage returned an invalid $key")

        private fun invalid(message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments("${ToolCatalog.OCR_RECOGNIZE}: $message"))
        private fun badHost(message: String): Nothing = throw ToolFailureException(ToolFailure.internal(message))
    }
}
