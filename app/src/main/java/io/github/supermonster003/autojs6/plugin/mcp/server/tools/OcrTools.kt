package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Ocr
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

/**
 * `ocr_recognize` (roadmap P11 D23 / D24 / D25 / D38): text recognition through an AutoJs6 OCR
 * plugin, without an image ever leaving the phone.
 *
 * - `image = "screen"` maps onto `accessibility.readScreenText`, which captures and recognizes
 *   inside the host (accessibility screenshot on Android 11+, MediaProjection otherwise). A
 *   `permission-denied` answer means MediaProjection consent is missing: the flow asks for it once
 *   through `media_projection.requestScreenCapture`, like `screen_capture`, and retries.
 * - A path runs `image.readImage` -> `ocr.recognize` -> `image.recycle` in one call ([ImageInputs.withFile]), the recycle
 *   in a `finally`. The host decodes large files downscaled (D45) and reports `scale`; the region
 *   is converted to decoded pixels and the bounds back to file pixels, so callers only ever see
 *   the file's own coordinates.
 *
 * Both answers are the host's `[{text, confidence, bounds}]` list; the result adds the source,
 * the engine, the joined text, and the file dimensions. A missing OCR plugin is reported as
 * `PLUGIN_UNAVAILABLE` rather than the generic mapping of the host's `unavailable` category.
 */
class OcrTools(
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
    private val access: PathAccess = PathAccess.WORKSPACE_ONLY,
) : ToolFlows {

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.OCR_RECOGNIZE -> {
            val options = Options.parse(arguments, access)
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
            options.region?.let { put("region", it.hostRegion(TOOL, 1.0, null, null)) }
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

    private suspend fun recognizeFile(caller: BridgeCaller, options: Options, startedAt: Long): ToolFlowResult =
        ImageInputs.withFile(caller, TOOL, options.image.target!!) { handle ->
            handle.checkRegion(TOOL, options.region)
            val hostOptions = buildJsonObject {
                options.region?.let { put("region", it.hostRegion(TOOL, handle.scale, handle.width, handle.height)) }
                options.hostEngine?.let { put(Ocr.ENGINE, it) }
            }
            val items = when (val outcome = caller.call("ocr", "recognize", JsonArray(listOf(handle.ref, hostOptions)), options.timeoutMs, OCR_PERMISSIONS)) {
                is BridgeOutcome.Ok -> items(outcome.result, handle.scale)
                is BridgeOutcome.Failed -> throw ToolFailureException(mapFailure(outcome.failure, consentHint = false))
            }
            result(options, "file", items, startedAt) {
                put("width", handle.sourceWidth)
                put("height", handle.sourceHeight)
                put("scale", handle.scale)
                if (handle.downscaled) put("decodedWidth", handle.width)
                if (handle.downscaled) put("decodedHeight", handle.height)
            }
        }

    private fun result(options: Options, source: String, items: List<JsonObject>, startedAt: Long, extra: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): ToolFlowResult {
        val text = items.mapNotNull { (it["text"] as? JsonPrimitive)?.contentOrNull?.takeIf { line -> line.isNotBlank() } }.joinToString("\n")
        val structured = buildJsonObject {
            put("image", options.image.raw)
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

    private class Options(val image: ImageArgument, val region: ImageRegion?, val engine: String, val timeoutMs: Long) {

        val screen: Boolean
            get() = image.screen

        val hostEngine: String?
            get() = Ocr.HOST_ENGINES[engine]

        companion object {

            fun parse(arguments: JsonObject, access: PathAccess): Options {
                ToolArguments.validate(ToolCatalog.ocrRecognize, arguments)
                val image = ImageInputs.parseImage(TOOL, arguments, access)
                val region = ImageInputs.parseRegion(TOOL, arguments)
                val engine = ToolArguments.string(arguments, Ocr.ENGINE, "auto")!!
                val timeoutMs = ToolArguments.long(arguments, Ocr.TIMEOUT_MS, Ocr.DEFAULT_TIMEOUT_MS).coerceIn(Ocr.MIN_TIMEOUT_MS, ToolCatalog.ocrRecognize.maxTimeoutMs)
                return Options(image, region, engine, timeoutMs)
            }
        }
    }

    companion object {

        private const val TOOL = ToolCatalog.OCR_RECOGNIZE
        private val SCREEN_PERMISSIONS = listOf("accessibility", "screen_capture", "ocr")
        private val CAPTURE_PERMISSIONS = listOf("screen_capture")
        private val OCR_PERMISSIONS = listOf("ocr", "image")
        private val CONSENT_ARGS = buildJsonArray { add(buildJsonObject { put("orientation", "auto") }) }

        private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

        private fun badHost(message: String): Nothing = ImageInputs.badHost(message)
    }
}
