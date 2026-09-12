package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeError
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Screen
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract
import kotlin.io.encoding.Base64
import kotlin.math.roundToInt

/**
 * P3.3: the host owns capture, consent, cropping and encoding. This flow only moves bounded
 * descriptor bytes into MCP image content. Captures are serialized, including consent, and
 * their complete queue/consent/retry budget is bounded. The host reuses a live projection on
 * repeated requests and releases it with its session; no stale authorization flag is cached here.
 */
class ScreenTools(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) : ToolFlows {

    private val captureLock = Mutex()

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.SCREEN_CAPTURE -> {
            val options = Options.parse(arguments)
            ToolFlow(spec.timeoutMs) { caller ->
                withTimeoutOrNull(spec.timeoutMs) {
                    captureLock.withLock { capture(caller, options) }
                } ?: throw ToolFailureException(ToolFailure.timeout("screen", "capture", spec.timeoutMs).copy(hint = CONSENT_HINT))
            }
        }
        ToolCatalog.SCREEN_STATE -> ToolFlow(spec.timeoutMs) { caller ->
            val on = caller.callOrThrow("device", "isScreenOn", NO_ARGS, STATE_TIMEOUT_MS, DEVICE_PERMISSION)
            val screen = screenInfo(caller)
            val isOn = (on as? JsonPrimitive)?.booleanOrNull ?: badHost("device.isScreenOn returned no boolean")
            ToolFlowResult(buildJsonObject {
                put("screenOn", isOn)
                screen.forEach { (key, value) -> put(key, value) }
            })
        }
        else -> null
    }

    private suspend fun capture(caller: BridgeCaller, requested: Options): ToolFlowResult {
        val startedAt = clock()
        val screen = screenInfo(caller)
        val width = screen.positiveInt("width")
        val height = screen.positiveInt("height")
        val croppedWidth = requested.region?.let { minOf(it.right, width) - minOf(it.left, width) } ?: width
        val croppedHeight = requested.region?.let { minOf(it.bottom, height) - minOf(it.top, height) } ?: height
        if (croppedWidth <= 0 || croppedHeight <= 0) invalid("region lies outside the current screen")
        var scale = requested.scale
        var maxWidth = requested.maxWidth
        if (scale == null && maxWidth == null) {
            // The host's maxWidth is the horizontal edge, while the default in D14 is the longest edge.
            maxWidth = minOf(croppedWidth, (Screen.DEFAULT_LONG_EDGE.toDouble() * croppedWidth / maxOf(croppedWidth, croppedHeight)).roundToInt().coerceAtLeast(1))
        }
        var quality = requested.quality
        var projection = false
        var captureAttempts = 0
        while (captureAttempts < Screen.MAX_ATTEMPTS) {
            val options = buildJsonObject {
                put(Screen.FORMAT, requested.format)
                put(Screen.QUALITY, quality)
                scale?.let { put(Screen.SCALE, it) }
                maxWidth?.let { put(Screen.MAX_WIDTH, it) }
                requested.region?.let { put(Screen.REGION, it.json) }
            }
            val module = if (projection) "image" else "accessibility"
            val method = if (projection) "captureScreen" else "screenshot"
            val permissions = if (projection) IMAGE_PERMISSION else ACCESSIBILITY_PERMISSION
            val outcome = caller.call(module, method, JsonArray(listOf(options)), Screen.HOST_CAPTURE_TIMEOUT_MS, permissions)
            val fallback = when (outcome) {
                is BridgeOutcome.Failed -> outcome.failure.category == BridgeError.CATEGORY_UNAVAILABLE
                is BridgeOutcome.Ok -> (outcome.result as? JsonObject)?.get("fallback") == JsonPrimitive("media_projection")
            }
            if (!projection && fallback) {
                projection = true
                val permission = caller.call("media_projection", "requestScreenCapture", JsonArray(listOf(buildJsonObject { put("orientation", "auto") })), Screen.CONSENT_TIMEOUT_MS, CAPTURE_PERMISSION)
                if (permission is BridgeOutcome.Failed) throw ToolFailureException(permission.failure.copy(hint = CONSENT_HINT))
                continue
            }
            captureAttempts++
            when (outcome) {
                is BridgeOutcome.Failed -> {
                    if (outcome.failure.code != ToolErrorCodes.LIMIT_EXCEEDED) {
                        throw ToolFailureException(if (projection) outcome.failure.copy(hint = CONSENT_HINT) else outcome.failure)
                    }
                }
                is BridgeOutcome.Ok -> {
                    val metadata = outcome.result as? JsonObject ?: badHost("the screenshot has no metadata")
                    val payload = outcome.payload ?: badHost("the host returned no screenshot descriptor; update AutoJs6 to a build supporting encoded image.captureScreen options")
                    val expectedMime = "image/${requested.format}"
                    if (payload.mime != expectedMime || metadata["mime"] != JsonPrimitive(expectedMime)) badHost("the screenshot MIME type does not match the requested format")
                    if (metadata.positiveInt("bytes") != payload.data.size) badHost("the screenshot byte count does not match its payload")
                    val outputWidth = metadata.positiveInt("width")
                    val outputHeight = metadata.positiveInt("height")
                    val encodedBytes = base64Size(payload.data.size)
                    if (encodedBytes <= McpServerContract.MAX_SCREENSHOT_ENCODED_BYTES) {
                        val result = buildJsonObject {
                            put("width", outputWidth)
                            put("height", outputHeight)
                            put("format", requested.format)
                            put("mime", expectedMime)
                            put("quality", quality)
                            put("bytes", payload.data.size)
                            put("encodedBytes", encodedBytes)
                            put("durationMs", (clock() - startedAt).coerceAtLeast(0))
                            put("source", if (projection) "media_projection" else "accessibility")
                            put("attempts", captureAttempts)
                            put("adjusted", captureAttempts > 1)
                            if (captureAttempts > 1) put("hint", "quality or dimensions were reduced to keep the base64 image within 4 MiB")
                            put("requested", requested.json)
                        }
                        return ToolFlowResult(result, image = ImageContent(data = Base64.encode(payload.data), mimeType = expectedMime))
                    }
                }
            }
            // Keep the requested format. PNG cannot reduce quality, so it reduces dimensions immediately.
            if (requested.format != "png" && quality > 25) {
                quality = (quality - 15).coerceAtLeast(25)
            } else if (scale != null) {
                scale *= 0.7
            } else {
                val previousWidth = checkNotNull(maxWidth)
                maxWidth = (previousWidth * 0.7).toInt().coerceAtLeast(1)
                if (maxWidth == previousWidth) break
            }
        }
        throw ToolFailureException(ToolFailure.limitExceeded("the screenshot still exceeds 4 MiB after $captureAttempts bounded attempts"))
    }

    private suspend fun screenInfo(caller: BridgeCaller): JsonObject {
        val info = caller.callOrThrow("device", "info", NO_ARGS, STATE_TIMEOUT_MS, DEVICE_PERMISSION) as? JsonObject
        val screen = info?.get("screen") as? JsonObject ?: badHost("device.info returned no screen dimensions")
        val width = screen.positiveInt("width")
        val height = screen.positiveInt("height")
        return buildJsonObject {
            // Host dimensions already follow display rotation; its cached Configuration orientation can lag behind.
            put("width", width)
            put("height", height)
            put("orientation", if (width > height) "landscape" else "portrait")
            listOf("rotation", "rotationDegrees", "density", "densityDpi").forEach { key -> screen[key]?.let { put(key, it) } }
        }
    }

    private class Region(val left: Int, val top: Int, val right: Int, val bottom: Int, val json: JsonObject)

    private class Options(val scale: Double?, val maxWidth: Int?, val format: String, val quality: Int, val region: Region?, val json: JsonObject) {
        companion object {
            fun parse(arguments: JsonObject): Options {
                ToolArguments.validate(ToolCatalog.screenCapture, arguments)
                val scale = (arguments[Screen.SCALE] as? JsonPrimitive)?.doubleOrNull
                val maxWidth = (arguments[Screen.MAX_WIDTH] as? JsonPrimitive)?.doubleOrNull?.toInt()
                if (scale != null && (!scale.isFinite() || scale !in 0.0001..1.0)) invalid("scale must be within [0.0001, 1]")
                if (scale != null && maxWidth != null) invalid("choose either scale or maxWidth")
                val region = (arguments[Screen.REGION] as? JsonObject)?.let { json ->
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
                return Options(scale, maxWidth, ToolArguments.string(arguments, Screen.FORMAT, "jpeg")!!,
                    (arguments[Screen.QUALITY] as? JsonPrimitive)?.doubleOrNull?.toInt() ?: Screen.DEFAULT_QUALITY, region,
                    JsonObject(arguments.filterValues { it != JsonNull }))
            }
        }
    }

    companion object {
        const val CONSENT_HINT = "unlock the phone and approve AutoJs6 screen capture when prompted, then retry screen_capture; denied, expired or revoked consent is requested again"
        private const val STATE_TIMEOUT_MS = 5_000L
        private val NO_ARGS = JsonArray(emptyList())
        private val DEVICE_PERMISSION = listOf("device")
        private val ACCESSIBILITY_PERMISSION = listOf("accessibility", "screen_capture")
        private val CAPTURE_PERMISSION = listOf("screen_capture")
        private val IMAGE_PERMISSION = listOf("image", "screen_capture")

        fun base64Size(bytes: Int): Long = ((bytes.toLong() + 2) / 3) * 4

        private fun JsonObject.positiveInt(key: String): Int = (get(key) as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 }
            ?: badHost("the host returned an invalid screenshot $key")

        private fun invalid(message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments("screen_capture: $message"))
        private fun badHost(message: String): Nothing = throw ToolFailureException(ToolFailure.internal(message))
    }
}
