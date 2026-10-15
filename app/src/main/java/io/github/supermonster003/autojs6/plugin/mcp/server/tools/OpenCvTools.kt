package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Images
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.OpenCv
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The `opencv` group (roadmap P11 D23 / D24 / D38): template matching and color search through
 * the OpenCV methods of the host's `image` module, which run on the AutoJs6 OpenCV plugin.
 *
 * - `opencv_find_image` takes the image and the template as session handles and calls
 *   `image.matchTemplate` (best first, no overlaps). Both must be at one pixel scale: when the
 *   host downscaled only one of them (D45), template matching would compare different scales, so
 *   the call is refused instead of answering an empty list.
 * - `opencv_find_color` / `opencv_find_multi_colors` call `image.findColor` /
 *   `image.findMultiColors` on the image handle; the neighbor offsets are scaled to decoded pixels
 *   for a downscaled file.
 *
 * Every handle is recycled in the same call ([ImageInputs.withImage]); points and bounds are
 * reported in the image's own pixels (device pixels for the screen).
 */
class OpenCvTools(
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
    private val access: PathAccess = PathAccess.WORKSPACE_ONLY,
) : ToolFlows {

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.OPENCV_FIND_IMAGE -> {
            val options = MatchOptions.parse(arguments, access)
            ToolFlow(options.timeoutMs) { caller -> findImage(caller, options) }
        }
        ToolCatalog.OPENCV_FIND_COLOR, ToolCatalog.OPENCV_FIND_MULTI_COLORS -> {
            val options = ColorOptions.parse(spec, arguments, access)
            ToolFlow(options.timeoutMs) { caller -> findColor(caller, options) }
        }
        else -> null
    }

    private suspend fun findImage(caller: BridgeCaller, options: MatchOptions): ToolFlowResult {
        val startedAt = clock()
        val tool = ToolCatalog.OPENCV_FIND_IMAGE
        return ImageInputs.withImage(caller, tool, options.image) { image ->
            image.checkRegion(tool, options.region)
            ImageInputs.withFile(caller, tool, options.template) { template ->
                if (image.sampleSize != template.sampleSize) {
                    ImageInputs.invalid(tool, "AutoJs6 reads the image (${image.sourceWidth} x ${image.sourceHeight}) at scale ${format(image.scale)} " +
                            "and the template (${template.sourceWidth} x ${template.sourceHeight}) at scale ${format(template.scale)} to stay within " +
                            "4096 px; template matching needs both at one scale, so crop the larger file first")
                }
                val searchWidth = options.region?.width ?: image.sourceWidth
                val searchHeight = options.region?.height ?: image.sourceHeight
                if (template.sourceWidth > searchWidth || template.sourceHeight > searchHeight) {
                    ImageInputs.invalid(tool, "the template (${template.sourceWidth} x ${template.sourceHeight}) is larger than the search area ($searchWidth x $searchHeight)")
                }
                val hostOptions = buildJsonObject {
                    put("threshold", options.threshold)
                    put("limit", options.maxResults)
                    options.region?.let { put("region", it.hostArray(tool, image.scale, image.width, image.height)) }
                }
                val result = caller.callOrThrow("image", "matchTemplate", JsonArray(listOf(image.ref, template.ref, hostOptions)), options.timeoutMs, IMAGE_PERMISSIONS)
                val matches = matches(result, image, template)
                val structured = buildJsonObject {
                    put("image", options.image.raw)
                    put("source", sourceOf(options.image))
                    put(OpenCv.TEMPLATE, options.templateRaw)
                    put("templateWidth", template.sourceWidth)
                    put("templateHeight", template.sourceHeight)
                    put(OpenCv.THRESHOLD, options.threshold)
                    put("count", matches.size)
                    put("matches", JsonArray(matches))
                    options.region?.let { put("region", it.json) }
                    image.describe(this)
                    put("durationMs", (clock() - startedAt).coerceAtLeast(0))
                }
                val text = if (matches.isEmpty()) {
                    "(no match with similarity >= ${format(options.threshold)})"
                } else {
                    matches.joinToString("\n") { match ->
                        val center = match["center"] as JsonObject
                        "center (${center["x"]}, ${center["y"]}) score ${format((match["score"] as JsonPrimitive).doubleOrNull ?: 0.0)}"
                    }
                }
                ToolFlowResult(structured, text)
            }
        }
    }

    private suspend fun findColor(caller: BridgeCaller, options: ColorOptions): ToolFlowResult {
        val startedAt = clock()
        val tool = options.tool
        return ImageInputs.withImage(caller, tool, options.image) { image ->
            image.checkRegion(tool, options.region)
            val hostOptions = buildJsonObject {
                put("threshold", options.threshold)
                options.region?.let { put("region", it.hostArray(tool, image.scale, image.width, image.height)) }
            }
            val args = if (options.points == null) {
                JsonArray(listOf(image.ref, JsonPrimitive(options.color), hostOptions))
            } else {
                val offsets = buildJsonArray {
                    options.points.forEach { point ->
                        add(buildJsonArray {
                            add(JsonPrimitive((point.x * image.scale).roundToInt()))
                            add(JsonPrimitive((point.y * image.scale).roundToInt()))
                            add(JsonPrimitive(point.color))
                        })
                    }
                }
                JsonArray(listOf(image.ref, JsonPrimitive(options.color), offsets, hostOptions))
            }
            val method = if (options.points == null) "findColor" else "findMultiColors"
            val result = caller.callOrThrow("image", method, args, options.timeoutMs, IMAGE_PERMISSIONS)
            val point = point(result, image)
            val structured = buildJsonObject {
                put("image", options.image.raw)
                put("source", sourceOf(options.image))
                put("found", point != null)
                point?.let { (x, y) -> put("point", buildJsonObject { put("x", x); put("y", y) }) }
                put(OpenCv.COLOR, options.color)
                options.points?.let { points -> put(OpenCv.POINTS, JsonArray(points.map { it.json })) }
                put(OpenCv.THRESHOLD, options.threshold)
                options.region?.let { put("region", it.json) }
                image.describe(this)
                put("durationMs", (clock() - startedAt).coerceAtLeast(0))
            }
            val text = point?.let { (x, y) -> "found at ($x, $y)" } ?: if (options.points == null) "(color not found)" else "(color pattern not found)"
            ToolFlowResult(structured, text)
        }
    }

    /** `[{x, y, score}]` of `image.matchTemplate` (top-left corners in decoded pixels) as bounds, center and score in the image's own pixels. */
    private fun matches(result: JsonElement, image: ImageHandle, template: ImageHandle): List<JsonObject> {
        val array = when (result) {
            is JsonArray -> result
            is JsonNull -> JsonArray(emptyList())
            else -> ImageInputs.badHost("the template matching result is not a list")
        }
        return array.map { element ->
            val match = element as? JsonObject ?: ImageInputs.badHost("a template match is not an object")
            val x = (match["x"] as? JsonPrimitive)?.doubleOrNull ?: ImageInputs.badHost("a template match has no x")
            val y = (match["y"] as? JsonPrimitive)?.doubleOrNull ?: ImageInputs.badHost("a template match has no y")
            val left = image.toSource(x)
            val top = image.toSource(y)
            val right = image.toSource(x + template.width)
            val bottom = image.toSource(y + template.height)
            buildJsonObject {
                put("bounds", buildJsonObject { put("left", left); put("top", top); put("right", right); put("bottom", bottom) })
                put("center", buildJsonObject { put("x", (left + right) / 2); put("y", (top + bottom) / 2) })
                (match["score"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }?.let { put("score", it) }
            }
        }
    }

    /** `{x, y}` of `image.findColor` / `findMultiColors` (decoded pixels) in the image's own pixels, or null when nothing matched. */
    private fun point(result: JsonElement, image: ImageHandle): Pair<Int, Int>? {
        if (result is JsonNull) return null
        val json = result as? JsonObject ?: ImageInputs.badHost("the color search result is not a point")
        val x = (json["x"] as? JsonPrimitive)?.doubleOrNull ?: ImageInputs.badHost("the color search result has no x")
        val y = (json["y"] as? JsonPrimitive)?.doubleOrNull ?: ImageInputs.badHost("the color search result has no y")
        return image.pixelToSource(x, y)
    }

    private class MatchOptions(val image: ImageArgument, val template: ReadTarget, val templateRaw: String, val threshold: Double, val region: ImageRegion?, val maxResults: Long, val timeoutMs: Long) {

        companion object {

            fun parse(arguments: JsonObject, access: PathAccess): MatchOptions {
                val tool = ToolCatalog.OPENCV_FIND_IMAGE
                ToolArguments.validate(ToolCatalog.opencvFindImage, arguments)
                val image = ImageInputs.parseImage(tool, arguments, access)
                val template = ImageInputs.parsePath(tool, arguments, OpenCv.TEMPLATE, access)
                val templateRaw = ToolArguments.string(arguments, OpenCv.TEMPLATE)!!.trim()
                val threshold = (arguments[OpenCv.THRESHOLD] as? JsonPrimitive)?.doubleOrNull ?: OpenCv.DEFAULT_MATCH_THRESHOLD
                val region = ImageInputs.parseRegion(tool, arguments)
                val maxResults = ToolArguments.long(arguments, OpenCv.MAX_RESULTS, OpenCv.DEFAULT_MAX_RESULTS)
                val timeoutMs = ToolArguments.long(arguments, Images.TIMEOUT_MS, OpenCv.DEFAULT_TIMEOUT_MS).coerceIn(Images.MIN_TIMEOUT_MS, ToolCatalog.opencvFindImage.maxTimeoutMs)
                return MatchOptions(image, template, templateRaw, threshold, region, maxResults, timeoutMs)
            }
        }
    }

    private class Neighbor(val x: Int, val y: Int, val color: String) {

        val json: JsonObject
            get() = buildJsonObject { put("x", x); put("y", y); put("color", color) }
    }

    private class ColorOptions(
        val tool: String,
        val image: ImageArgument,
        val color: String,
        val points: List<Neighbor>?,
        val threshold: Long,
        val region: ImageRegion?,
        val timeoutMs: Long,
    ) {

        companion object {

            fun parse(spec: ToolSpec, arguments: JsonObject, access: PathAccess): ColorOptions {
                val tool = spec.name
                ToolArguments.validate(spec, arguments)
                val image = ImageInputs.parseImage(tool, arguments, access)
                val color = color(tool, OpenCv.COLOR, ToolArguments.string(arguments, OpenCv.COLOR))
                val points = if (tool == ToolCatalog.OPENCV_FIND_MULTI_COLORS) {
                    val array = arguments[OpenCv.POINTS] as? JsonArray ?: ImageInputs.invalid(tool, "points must be an array")
                    if (array.isEmpty() || array.size > OpenCv.MAX_POINTS) ImageInputs.invalid(tool, "points must list 1 to ${OpenCv.MAX_POINTS} neighbors")
                    array.mapIndexed { index, element ->
                        val point = element as? JsonObject ?: ImageInputs.invalid(tool, "points[$index] must be an object with x, y and color")
                        if (point.keys != setOf("x", "y", "color")) ImageInputs.invalid(tool, "points[$index] must contain exactly x, y and color")
                        Neighbor(offset(tool, "points[$index].x", point["x"]), offset(tool, "points[$index].y", point["y"]),
                            color(tool, "points[$index].color", (point["color"] as? JsonPrimitive)?.takeIf { it.isString }?.content))
                    }
                } else {
                    null
                }
                val threshold = ToolArguments.long(arguments, OpenCv.THRESHOLD, OpenCv.DEFAULT_COLOR_THRESHOLD)
                val region = ImageInputs.parseRegion(tool, arguments)
                val timeoutMs = ToolArguments.long(arguments, Images.TIMEOUT_MS, OpenCv.DEFAULT_TIMEOUT_MS).coerceIn(Images.MIN_TIMEOUT_MS, spec.maxTimeoutMs)
                return ColorOptions(tool, image, color, points, threshold, region, timeoutMs)
            }

            private fun color(tool: String, key: String, value: String?): String =
                value?.let(ImageInputs::normalizeColor) ?: ImageInputs.invalid(tool, "$key must be a color like #RRGGBB or #RGB")

            private fun offset(tool: String, key: String, value: JsonElement?): Int {
                val number = (value as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull
                if (number == null || !number.isFinite() || number != number.toLong().toDouble() || abs(number) > OpenCv.MAX_OFFSET) {
                    ImageInputs.invalid(tool, "$key must be an integer within [-${OpenCv.MAX_OFFSET}, ${OpenCv.MAX_OFFSET}]")
                }
                return number.toInt()
            }
        }
    }

    companion object {

        private val IMAGE_PERMISSIONS = listOf("image")

        private fun sourceOf(image: ImageArgument): String = if (image.screen) "screen" else "file"

        private fun format(value: Double): String = String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
    }
}
