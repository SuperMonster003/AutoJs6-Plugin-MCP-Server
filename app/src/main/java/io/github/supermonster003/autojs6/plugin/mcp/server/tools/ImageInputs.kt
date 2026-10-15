package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Images
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
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
 * A rectangle in screen pixels or file pixels (`{left, top, right, bottom}`, right and bottom
 * exclusive), as the plugin-family tools accept it in `region` (roadmap P11 D38).
 */
class ImageRegion(val left: Int, val top: Int, val right: Int, val bottom: Int, val json: JsonObject) {

    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    /** The host's `{x, y, width, height}` object form (OCR, barcode), in decoded pixels when the host downscaled the file ([scale] < 1). */
    fun hostRegion(tool: String, scale: Double, decodedWidth: Int?, decodedHeight: Int?): JsonObject {
        val (x, y, w, h) = decoded(tool, scale, decodedWidth, decodedHeight)
        return buildJsonObject { put("x", x); put("y", y); put("width", w); put("height", h) }
    }

    /** The host's `[x, y, width, height]` array form (the OpenCV methods of the `image` module), in decoded pixels. */
    fun hostArray(tool: String, scale: Double, decodedWidth: Int, decodedHeight: Int): JsonArray {
        val edges = decoded(tool, scale, decodedWidth, decodedHeight)
        return buildJsonArray { edges.forEach { add(JsonPrimitive(it)) } }
    }

    private fun decoded(tool: String, scale: Double, decodedWidth: Int?, decodedHeight: Int?): List<Int> {
        var x = floor(left * scale).toInt()
        var y = floor(top * scale).toInt()
        var right = ceil(this.right * scale).toInt()
        var bottom = ceil(this.bottom * scale).toInt()
        if (decodedWidth != null) { x = x.coerceIn(0, decodedWidth); right = right.coerceIn(0, decodedWidth) }
        if (decodedHeight != null) { y = y.coerceIn(0, decodedHeight); bottom = bottom.coerceIn(0, decodedHeight) }
        if (right <= x || bottom <= y) ImageInputs.invalid(tool, "region is too small for the decoded image")
        return listOf(x, y, right - x, bottom - y)
    }
}

/**
 * A session image handle in AutoJs6 (roadmap P11 D23 / D45): the screen captured through
 * `accessibility.captureScreenImage`, or a file read through `image.readImage`. [width] /
 * [height] are the decoded pixels the host works on; [sourceWidth] / [sourceHeight] are the
 * file's own pixels, larger by `1 / [scale]` when the host downscaled a large file.
 */
class ImageHandle(
    val id: String,
    val width: Int,
    val height: Int,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val scale: Double,
    val captureSource: String?,
) {

    /** The handle as a host argument. */
    val ref: JsonObject
        get() = buildJsonObject { put("id", id); put("__autojs6ImageHandle", id) }

    val downscaled: Boolean
        get() = scale < 1.0

    /** The host's decoding step (`inSampleSize`, a power of two); [scale] is only close to its inverse because decoded sizes are rounded. */
    val sampleSize: Int
        get() = (1.0 / scale).roundToInt().coerceAtLeast(1)

    /** Rejects a [region] beyond the image, in its own pixels. */
    fun checkRegion(tool: String, region: ImageRegion?) {
        if (region != null && (region.right > sourceWidth || region.bottom > sourceHeight)) {
            ImageInputs.invalid(tool, "region lies outside the image ($sourceWidth x $sourceHeight)")
        }
    }

    /** A decoded x or y back in the image's own pixels. */
    fun toSource(value: Double): Int = (value / scale).roundToInt()

    /** A decoded pixel's position in the image's own pixels: the file pixel under its center. */
    fun pixelToSource(x: Double, y: Double): Pair<Int, Int> =
        ((x + 0.5) / scale).toInt().coerceIn(0, sourceWidth - 1) to ((y + 0.5) / scale).toInt().coerceIn(0, sourceHeight - 1)

    /** The dimensions every result reports: the image's own size, the scale, and the decoded size when it differs. */
    fun describe(builder: JsonObjectBuilder) {
        builder.put("width", sourceWidth)
        builder.put("height", sourceHeight)
        builder.put("scale", scale)
        if (downscaled) builder.put("decodedWidth", width)
        if (downscaled) builder.put("decodedHeight", height)
        captureSource?.let { builder.put("captureSource", it) }
    }
}

/**
 * The `image` argument of the plugin-family tools (roadmap P11 D23 / D24 / D38): `"screen"` or a
 * path relative to the AutoJs6 working directory. A tool takes the image as a session handle,
 * works on it, and recycles it in the same call ([withImage]); the picture never leaves AutoJs6.
 */
object ImageInputs {

    private val SCREEN_PERMISSIONS = listOf("accessibility", "screen_capture", "image")
    private val CAPTURE_PERMISSIONS = listOf("screen_capture")
    private val IMAGE_PERMISSIONS = listOf("image")
    private val CONSENT_ARGS = buildJsonArray { add(buildJsonObject { put("orientation", "auto") }) }

    /** The `image` value of a file source, validated, or [Images.SCREEN]. */
    fun parseImage(tool: String, arguments: JsonObject, key: String = Images.IMAGE): String {
        val image = ToolArguments.string(arguments, key)?.trim()?.takeIf { it.isNotEmpty() } ?: invalid(tool, "$key must be \"screen\" or a file path")
        if (image != Images.SCREEN) WorkspacePath.normalize(image, allowRoot = false)
        return image
    }

    /** A workspace path argument that must name a file (the template of `opencv_find_image`). */
    fun parsePath(tool: String, arguments: JsonObject, key: String): String {
        val path = ToolArguments.string(arguments, key)?.trim()?.takeIf { it.isNotEmpty() } ?: invalid(tool, "$key must be a file path")
        WorkspacePath.normalize(path, allowRoot = false)
        return path
    }

    /** The optional `{left, top, right, bottom}` argument [key]. */
    fun parseRegion(tool: String, arguments: JsonObject, key: String = Images.REGION): ImageRegion? = (arguments[key] as? JsonObject)?.let { json ->
        val keys = setOf("left", "top", "right", "bottom")
        if (json.keys != keys) invalid(tool, "$key must contain exactly left, top, right and bottom")
        val edges = keys.associateWith { edgeKey ->
            val edge = (json[edgeKey] as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull
            if (edge == null || !edge.isFinite() || edge !in 0.0..MAX_EDGE || edge != edge.toInt().toDouble()) invalid(tool, "$key.$edgeKey must be an integer within [0, 65535]")
            edge.toInt()
        }
        ImageRegion(edges.getValue("left"), edges.getValue("top"), edges.getValue("right"), edges.getValue("bottom"), json).also {
            if (it.right <= it.left || it.bottom <= it.top) invalid(tool, "$key must have positive width and height")
        }
    }

    /**
     * Takes [image] as a handle, runs [block], and recycles the handle afterwards, also when
     * [block] throws: a leaked handle counts against the host's per-session limit until the session closes.
     */
    suspend fun <T> withImage(caller: BridgeCaller, tool: String, image: String, block: suspend (ImageHandle) -> T): T {
        val handle = if (image == Images.SCREEN) capture(caller, tool) else read(caller, tool, WorkspacePath.normalize(image, allowRoot = false))
        try {
            return block(handle)
        } finally {
            recycle(caller, handle)
        }
    }

    /** Like [withImage] for an argument that is always a file path (a template), even one named `screen`. */
    suspend fun <T> withFile(caller: BridgeCaller, tool: String, path: String, block: suspend (ImageHandle) -> T): T {
        val handle = read(caller, tool, WorkspacePath.normalize(path, allowRoot = false))
        try {
            return block(handle)
        } finally {
            recycle(caller, handle)
        }
    }

    /**
     * The screen as a handle (`accessibility.captureScreenImage`: an accessibility screenshot on
     * Android 11+, MediaProjection otherwise). A `permission-denied` answer means MediaProjection
     * consent is missing: it is requested once, like `screen_capture`, and the capture retried.
     */
    suspend fun capture(caller: BridgeCaller, tool: String): ImageHandle {
        var outcome = caller.call("accessibility", "captureScreenImage", JsonArray(emptyList()), Images.CAPTURE_TIMEOUT_MS, SCREEN_PERMISSIONS)
        if (outcome is BridgeOutcome.Failed && outcome.failure.category == BridgeError.CATEGORY_PERMISSION_DENIED) {
            val consent = caller.call("media_projection", "requestScreenCapture", CONSENT_ARGS, ToolCatalog.Screen.CONSENT_TIMEOUT_MS, CAPTURE_PERMISSIONS)
            if (consent is BridgeOutcome.Failed) throw ToolFailureException(consent.failure.copy(hint = ScreenTools.CONSENT_HINT))
            outcome = caller.call("accessibility", "captureScreenImage", JsonArray(emptyList()), Images.CAPTURE_TIMEOUT_MS, SCREEN_PERMISSIONS)
        }
        return when (outcome) {
            is BridgeOutcome.Ok -> handleOf(outcome.result, "accessibility.captureScreenImage")
            is BridgeOutcome.Failed -> throw ToolFailureException(acquireFailure(outcome.failure, consentHint = true))
        }
    }

    /** A workspace file as a handle (`image.readImage`); the host downscales large files (D45) and reports the scale. */
    suspend fun read(caller: BridgeCaller, tool: String, path: String): ImageHandle =
        when (val outcome = caller.call("image", "readImage", JsonArray(listOf(JsonPrimitive(path))), Images.READ_TIMEOUT_MS, IMAGE_PERMISSIONS)) {
            is BridgeOutcome.Ok -> handleOf(outcome.result, "image.readImage")
            is BridgeOutcome.Failed -> throw ToolFailureException(acquireFailure(outcome.failure, consentHint = false))
        }

    /** Releases [handle]; a failure here is not reported, the session close releases it at the latest. */
    suspend fun recycle(caller: BridgeCaller, handle: ImageHandle) {
        runCatching { caller.call("image", "recycle", JsonArray(listOf(handle.ref)), Images.RECYCLE_TIMEOUT_MS, IMAGE_PERMISSIONS) }
    }

    /** The handle JSON of `image.readImage` / `accessibility.captureScreenImage`. */
    fun handleOf(result: JsonElement, method: String): ImageHandle {
        val json = result as? JsonObject ?: badHost("$method returned no image handle")
        val id = json["__autojs6ImageHandle"]?.string() ?: json["id"]?.string() ?: badHost("$method returned no handle id")
        val width = (json["width"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: badHost("$method returned an invalid width")
        val height = (json["height"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: badHost("$method returned an invalid height")
        val sourceWidth = (json["sourceWidth"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: width
        val sourceHeight = (json["sourceHeight"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: height
        val scale = (json["scale"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() && it > 0.0 && it <= 1.0 } ?: 1.0
        return ImageHandle(id, width, height, sourceWidth, sourceHeight, scale, json["captureSource"]?.string())
    }

    /** Taking a handle failed: a full handle table gets a hint that names the cause; a refused consent gets the consent hint. */
    private fun acquireFailure(failure: ToolFailure, consentHint: Boolean): ToolFailure = when {
        failure.category == BridgeError.CATEGORY_RESOURCE_LIMIT -> failure.copy(hint = HINT_HANDLES)
        consentHint && failure.category == BridgeError.CATEGORY_PERMISSION_DENIED -> failure.copy(hint = ScreenTools.CONSENT_HINT)
        else -> failure
    }

    /** The upper-case `#RRGGBB` form of a `#RGB` / `#RRGGBB` color argument, or null when it is neither. */
    fun normalizeColor(value: String): String? {
        val match = COLOR.matchEntire(value.trim()) ?: return null
        val digits = match.groupValues[1].uppercase()
        return "#" + if (digits.length == 3) digits.map { "$it$it" }.joinToString("") else digits
    }

    fun invalid(tool: String, message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments("$tool: $message"))

    fun badHost(message: String): Nothing = throw ToolFailureException(ToolFailure.internal(message))

    private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    /** Hint for the host's `resource-limit` answer while taking an image handle. */
    const val HINT_HANDLES = "AutoJs6 keeps only a few images per session (twice the concurrent call limit); wait for running recognition calls to finish and retry"

    private const val MAX_EDGE = 65_535.0
    private val COLOR = Regex("#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})")
}
