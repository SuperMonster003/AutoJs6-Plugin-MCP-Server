package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Barcode
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Images
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

/**
 * `barcode_detect` (roadmap P11 D23 / D24 / D38): barcodes and QR codes through the AutoJs6 ML Kit
 * Barcode plugin, without an image ever leaving the phone.
 *
 * The image comes in as a session handle ([ImageInputs.withImage]: `"screen"` through
 * `accessibility.captureScreenImage`, a path through `image.readImage`), `barcode.detectAll`
 * runs on it, and the handle is recycled in the same call. The host answers its barcode
 * snapshots (`rawValue`, `displayValue`, `formatName`, `valueTypeName`, `boundingBox`,
 * `cornerPoints`, `structured`) in decoded pixels; the tool maps them back to the file's own
 * pixels when the host downscaled a large file (D45). A missing barcode plugin is reported as
 * `PLUGIN_UNAVAILABLE`.
 */
class BarcodeTools(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) : ToolFlows {

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.BARCODE_DETECT -> {
            val options = Options.parse(arguments)
            ToolFlow(options.timeoutMs) { caller -> detect(caller, options) }
        }
        else -> null
    }

    private suspend fun detect(caller: BridgeCaller, options: Options): ToolFlowResult {
        val startedAt = clock()
        return ImageInputs.withImage(caller, TOOL, options.image) { handle ->
            handle.checkRegion(TOOL, options.region)
            val hostOptions = buildJsonObject {
                options.formats?.let { formats -> put(Barcode.FORMATS, JsonArray(formats.map { JsonPrimitive(it) })) }
                put("maxResults", options.maxResults)
                options.region?.let { put("region", it.hostRegion(TOOL, handle.scale, handle.width, handle.height)) }
            }
            val items = when (val outcome = caller.call("barcode", "detectAll", JsonArray(listOf(handle.ref, hostOptions)), options.timeoutMs, BARCODE_PERMISSIONS)) {
                is BridgeOutcome.Ok -> items(outcome.result, handle)
                is BridgeOutcome.Failed -> throw ToolFailureException(mapFailure(outcome.failure))
            }
            val text = items.joinToString("\n") { item ->
                val format = (item["format"] as? JsonPrimitive)?.contentOrNull ?: "UNKNOWN"
                val value = (item["rawValue"] as? JsonPrimitive)?.contentOrNull ?: "(no raw value)"
                "$format: $value"
            }
            val structured = buildJsonObject {
                put("image", options.image)
                put("source", if (options.image == Images.SCREEN) "screen" else "file")
                put("count", items.size)
                put("items", JsonArray(items))
                options.formats?.let { formats -> put(Barcode.FORMATS, JsonArray(formats.map { JsonPrimitive(it) })) }
                options.region?.let { put("region", it.json) }
                handle.describe(this)
                put("durationMs", (clock() - startedAt).coerceAtLeast(0))
            }
            ToolFlowResult(structured, text.ifEmpty { "(no barcode found)" })
        }
    }

    /** The host snapshots with coordinates in the image's own pixels. */
    private fun items(result: JsonElement, handle: ImageHandle): List<JsonObject> {
        val array = when (result) {
            is JsonArray -> result
            is JsonNull -> JsonArray(emptyList())
            else -> ImageInputs.badHost("the barcode result is not a list")
        }
        return array.map { element ->
            val snapshot = element as? JsonObject ?: ImageInputs.badHost("a barcode result item is not an object")
            buildJsonObject {
                put("rawValue", snapshot["rawValue"].stringOrNull())
                put("displayValue", snapshot["displayValue"].stringOrNull())
                put("format", snapshot["formatName"].stringOrNull() ?: "UNKNOWN")
                put("valueType", snapshot["valueTypeName"].stringOrNull() ?: "UNKNOWN")
                (snapshot["boundingBox"] as? JsonObject)?.let { box ->
                    val edges = listOf("left", "top", "right", "bottom").map { key -> (box[key] as? JsonPrimitive)?.doubleOrNull ?: return@let }
                    put("bounds", buildJsonObject {
                        put("left", handle.toSource(edges[0]))
                        put("top", handle.toSource(edges[1]))
                        put("right", handle.toSource(edges[2]))
                        put("bottom", handle.toSource(edges[3]))
                    })
                }
                (snapshot["cornerPoints"] as? JsonArray)?.let { corners ->
                    put("cornerPoints", buildJsonArray {
                        corners.forEach { corner ->
                            val point = corner as? JsonObject ?: return@forEach
                            val x = (point["x"] as? JsonPrimitive)?.doubleOrNull ?: return@forEach
                            val y = (point["y"] as? JsonPrimitive)?.doubleOrNull ?: return@forEach
                            add(buildJsonObject { put("x", handle.toSource(x)); put("y", handle.toSource(y)) })
                        }
                    })
                }
                (snapshot["structured"] as? JsonObject)?.let { put("structured", it) }
            }
        }
    }

    /** A missing or unusable barcode plugin is `PLUGIN_UNAVAILABLE`; the host's `unavailable` category would otherwise read as a host problem. */
    private fun mapFailure(failure: ToolFailure): ToolFailure =
        if (failure.category == BridgeError.CATEGORY_UNAVAILABLE && failure.module == "barcode") {
            ToolFailure.pluginUnavailable(TOOL, ToolGroup.BARCODE.family!!, "not installed, disabled, or not usable", failure.message)
        } else {
            failure
        }

    private class Options(val image: String, val formats: List<String>?, val region: ImageRegion?, val maxResults: Long, val timeoutMs: Long) {

        companion object {

            fun parse(arguments: JsonObject): Options {
                ToolArguments.validate(ToolCatalog.barcodeDetect, arguments)
                val image = ImageInputs.parseImage(TOOL, arguments)
                val formats = (arguments[Barcode.FORMATS] as? JsonArray)?.let { array ->
                    if (array.isEmpty()) ImageInputs.invalid(TOOL, "formats must name at least one symbology; omit it for all")
                    array.map { element ->
                        val name = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
                        if (name == null || name !in Barcode.FORMAT_NAMES) ImageInputs.invalid(TOOL, "formats entries must be one of ${Barcode.FORMAT_NAMES.joinToString(", ")}")
                        name
                    }.distinct()
                }
                val region = ImageInputs.parseRegion(TOOL, arguments)
                val maxResults = ToolArguments.long(arguments, Barcode.MAX_RESULTS, Barcode.DEFAULT_MAX_RESULTS)
                val timeoutMs = ToolArguments.long(arguments, Images.TIMEOUT_MS, Barcode.DEFAULT_TIMEOUT_MS).coerceIn(Images.MIN_TIMEOUT_MS, ToolCatalog.barcodeDetect.maxTimeoutMs)
                return Options(image, formats, region, maxResults, timeoutMs)
            }
        }
    }

    companion object {

        private const val TOOL = ToolCatalog.BARCODE_DETECT
        private val BARCODE_PERMISSIONS = listOf("barcode", "image")

        private fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
}
