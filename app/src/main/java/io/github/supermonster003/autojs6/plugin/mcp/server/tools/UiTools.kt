package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.BridgeOutcome
import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolFailure
import io.github.supermonster003.autojs6.plugin.mcp.server.tools.ToolCatalog.Ui
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract
import kotlin.math.max

/**
 * The flows of the `ui` and `ui_gesture` groups (roadmap P3.2): each tool is one or more host
 * `accessibility` calls plus the plugin-side work the roadmap assigns to it. `ui_dump` turns
 * the host's JSON nodes into the compact tree and registers the `#n` references, the action
 * tools resolve a `nodeRef` by relocating its fingerprint (D12) or a `selector` by `findOne`
 * and then act through an exact selector, `ui_find` / `ui_wait_for` poll, `ui_scroll` repeats,
 * and the coordinate forms are gated by the `ui_gesture` switch (D22). Pure Kotlin over a
 * [BridgeCaller]; `UiToolsTest` drives it with a fake host.
 */
class UiTools(
    private val registry: NodeRefRegistry,
    private val permissions: () -> ToolPermissions,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val pollMs: Long = POLL_MS,
    private val settleMs: Long = SCROLL_SETTLE_MS,
) : ToolFlows {

    /** A located node the action tools act on. */
    private class Target(val node: UiNodeInfo, val ref: String?, val via: String, val selector: JsonObject)

    private class ScreenSize(val width: Int, val height: Int, val readAt: Long)

    @Volatile
    private var screen: ScreenSize? = null

    override fun planFor(spec: ToolSpec, arguments: JsonObject): ToolFlow? = when (spec.name) {
        ToolCatalog.UI_DUMP -> dump(arguments)
        ToolCatalog.UI_FIND -> find(arguments)
        ToolCatalog.UI_CURRENT_WINDOW -> currentWindow()
        ToolCatalog.UI_EXPLAIN_SELECTOR -> explain(arguments)
        ToolCatalog.UI_WAIT_FOR -> waitFor(arguments)
        ToolCatalog.UI_CLICK -> click(ToolCatalog.UI_CLICK, arguments, long = false)
        ToolCatalog.UI_LONG_CLICK -> click(ToolCatalog.UI_LONG_CLICK, arguments, long = true)
        ToolCatalog.UI_SET_TEXT -> setText(arguments)
        ToolCatalog.UI_SCROLL -> scroll(arguments)
        ToolCatalog.UI_PRESS_KEY -> pressKey(arguments)
        ToolCatalog.UI_SWIPE -> swipe(arguments)
        ToolCatalog.UI_GESTURE -> gesture(arguments)
        else -> null
    }

    // ------------------------------------------------------------------ observation

    private fun dump(arguments: JsonObject): ToolFlow {
        val format = ToolArguments.string(arguments, Ui.FORMAT, Ui.FORMAT_TEXT)!!
        val maxDepth = ToolArguments.long(arguments, Ui.MAX_DEPTH, McpServerContract.MAX_DUMP_DEPTH.toLong()).coerceIn(0L, McpServerContract.MAX_DUMP_DEPTH.toLong())
        val maxNodes = ToolArguments.long(arguments, Ui.MAX_NODES, Ui.DEFAULT_MAX_NODES).coerceIn(1L, Ui.MAX_NODES_LIMIT)
        val visibleOnly = ToolArguments.boolean(arguments, Ui.VISIBLE_ONLY, true)
        val window = ToolArguments.string(arguments, Ui.WINDOW, Ui.WINDOW_ACTIVE)!!
        return ToolFlow(DUMP_TIMEOUT_MS) { caller ->
            val options = buildJsonObject {
                put("format", if (format == Ui.FORMAT_XML) Ui.FORMAT_XML else "json")
                put("maxDepth", maxDepth)
                put("maxNodes", maxNodes)
                put("visibleOnly", visibleOnly)
                put("window", window)
            }
            val result = caller.objectOrThrow("accessibility", "dump", JsonArray(listOf(options)), DUMP_TIMEOUT_MS, ACCESSIBILITY)
            val windowInfo = UiNodeInfo.windowOf(result)
            val truncated = result.flag("truncated")
            if (format == Ui.FORMAT_XML) {
                val text = result.string("text")
                val structured = buildJsonObject {
                    put("format", format)
                    put("window", windowInfo)
                    put("roots", result["roots"] ?: JsonPrimitive(1))
                    put("nodeCount", result["nodeCount"] ?: JsonPrimitive(0))
                    put("truncated", truncated)
                    put("textTruncated", result.flag("textTruncated"))
                }
                return@ToolFlow ToolFlowResult(structured, text)
            }
            val nodes = UiNodeInfo.listFromJson(result["nodes"])
            val registration = registry.replace(nodes)
            val structured = buildJsonObject {
                put("snapshotId", registration.snapshotId)
                put("format", format)
                put("window", windowInfo)
                put("roots", result["roots"] ?: JsonPrimitive(1))
                put("nodeCount", nodes.size)
                put("truncated", truncated)
                if (format == Ui.FORMAT_JSON) put("nodes", JsonArray(registration.entries.map { it.node.toJson(it.ref, full = true) }))
            }
            if (format == Ui.FORMAT_JSON) {
                ToolFlowResult(structured)
            } else {
                ToolFlowResult(structured, CompactNodeText.render(windowInfo, registration.entries, truncated, maxNodes, maxDepth))
            }
        }
    }

    private fun find(arguments: JsonObject): ToolFlow {
        val selector = UiSelectors.parse(ToolCatalog.UI_FIND, arguments[Ui.SELECTOR])
        val limit = ToolArguments.long(arguments, Ui.LIMIT, Ui.DEFAULT_FIND_LIMIT).coerceIn(1L, Ui.MAX_FIND_LIMIT).toInt()
        val timeoutMs = ToolArguments.long(arguments, Ui.TIMEOUT_MS, 0L).coerceIn(0L, Ui.MAX_FIND_TIMEOUT_MS)
        return ToolFlow(timeoutMs + QUERY_TIMEOUT_MS) { caller ->
            val startedAt = clock()
            var nodes: List<UiNodeInfo>
            while (true) {
                nodes = UiNodeInfo.listFromJson(caller.callOrThrow("accessibility", "findAll", JsonArray(listOf(selector)), QUERY_TIMEOUT_MS, ACCESSIBILITY))
                if (nodes.isNotEmpty() || clock() - startedAt >= timeoutMs) break
                sleep(pollMs)
            }
            val kept = nodes.take(limit)
            val registration = if (kept.isEmpty()) null else registry.append(kept)
            ToolFlowResult(
                buildJsonObject {
                    registration?.let { put("snapshotId", it.snapshotId) }
                    put("selector", selector)
                    put("count", nodes.size)
                    put("returned", kept.size)
                    put("truncated", nodes.size > kept.size)
                    put("elapsedMs", clock() - startedAt)
                    put("nodes", JsonArray(registration?.entries?.map { it.node.toJson(it.ref) }.orEmpty()))
                    if (nodes.isEmpty()) put("hint", "no node matches ${UiSelectors.describe(selector)} in the active window; ui_dump shows the window, ui_explain_selector reports which condition fails")
                },
            )
        }
    }

    private fun waitFor(arguments: JsonObject): ToolFlow {
        val selector = UiSelectors.parse(ToolCatalog.UI_WAIT_FOR, arguments[Ui.SELECTOR])
        val state = ToolArguments.string(arguments, Ui.STATE, Ui.STATE_APPEAR)!!
        val timeoutMs = ToolArguments.long(arguments, Ui.TIMEOUT_MS, Ui.DEFAULT_WAIT_TIMEOUT_MS).coerceIn(Ui.MIN_WAIT_TIMEOUT_MS, Ui.MAX_WAIT_TIMEOUT_MS)
        val appear = state == Ui.STATE_APPEAR
        return ToolFlow(timeoutMs + QUERY_TIMEOUT_MS) { caller ->
            val startedAt = clock()
            while (true) {
                val node = UiNodeInfo.fromJson(caller.callOrThrow("accessibility", "findOne", JsonArray(listOf(selector)), QUERY_TIMEOUT_MS, ACCESSIBILITY))
                val satisfied = if (appear) node != null else node == null
                val elapsed = clock() - startedAt
                if (satisfied) {
                    val registration = node?.let { registry.append(listOf(it)) }
                    return@ToolFlow ToolFlowResult(
                        buildJsonObject {
                            put("state", state)
                            put("satisfied", true)
                            put("selector", selector)
                            put("elapsedMs", elapsed)
                            registration?.let {
                                put("snapshotId", it.snapshotId)
                                put("node", it.entries.single().node.toJson(it.entries.single().ref))
                            }
                        },
                    )
                }
                if (elapsed >= timeoutMs) {
                    val what = UiSelectors.describe(selector)
                    throw ToolFailureException(
                        ToolFailure(
                            io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes.TIMEOUT,
                            if (appear) "no node matching $what appeared within $timeoutMs ms" else "a node matching $what is still present after $timeoutMs ms",
                            "raise timeoutMs, check the selector with ui_dump or ui_explain_selector, or act first",
                        ),
                    )
                }
                sleep(pollMs)
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }
    }

    private fun currentWindow(): ToolFlow = ToolFlow(QUERY_TIMEOUT_MS) { caller ->
        val result = caller.objectOrThrow("app", "currentWindow", NO_ARGS, QUERY_TIMEOUT_MS, WINDOW)
        ToolFlowResult(JsonObject(result.filterKeys { it != "schema" }))
    }

    private fun explain(arguments: JsonObject): ToolFlow {
        val selector = UiSelectors.parse(ToolCatalog.UI_EXPLAIN_SELECTOR, arguments[Ui.SELECTOR])
        return ToolFlow(DUMP_TIMEOUT_MS) { caller ->
            val result = caller.objectOrThrow("accessibility", "explain", JsonArray(listOf(selector, JsonObject(emptyMap()))), DUMP_TIMEOUT_MS, ACCESSIBILITY)
            val structured = buildJsonObject {
                result.forEach { (key, value) ->
                    when (key) {
                        "schema" -> Unit
                        "matches", "nearMisses" -> put(key, JsonArray(UiNodeInfo.listFromJson(value).map { it.toJson() }))
                        else -> put(key, value)
                    }
                }
            }
            ToolFlowResult(structured, result.string("text").ifEmpty { structured.toString() })
        }
    }

    // ------------------------------------------------------------------ actions

    private fun click(tool: String, arguments: JsonObject, long: Boolean): ToolFlow {
        val hasX = arguments.has(Ui.X)
        val hasY = arguments.has(Ui.Y)
        if (hasX != hasY) fail("$tool needs both x and y for a coordinate click")
        if (hasX && (arguments.has(Ui.NODE_REF) || arguments.has(Ui.SELECTOR))) fail("$tool accepts nodeRef, selector, or x and y, not a combination")
        val method = if (long) "longClick" else "click"
        val action = if (long) "long_click" else "click"
        return ToolFlow(ACTION_TIMEOUT_MS + QUERY_TIMEOUT_MS) { caller ->
            if (hasX) {
                if (!permissions().isEnabled(ToolGroup.UI_GESTURE)) throw ToolFailureException(ToolFailure.coordinatesDisabled(tool))
                val x = ToolArguments.long(arguments, Ui.X, 0L)
                val y = ToolArguments.long(arguments, Ui.Y, 0L)
                checkOnScreen(tool, caller, listOf(x to y))
                val durationMs = if (long) LONG_PRESS_MS else TAP_MS
                performGesture(caller, "swipe", JsonArray(listOf(JsonPrimitive(x), JsonPrimitive(y), JsonPrimitive(x), JsonPrimitive(y), JsonPrimitive(durationMs))), durationMs, "$action at ($x, $y)")
                return@ToolFlow ToolFlowResult(
                    buildJsonObject {
                        put("action", action)
                        put("performed", true)
                        put("via", "coordinates")
                        put("x", x)
                        put("y", y)
                        put("durationMs", durationMs)
                    },
                )
            }
            val target = resolveTarget(tool, arguments, caller)
            performOnNode(
                caller, method, JsonArray(listOf(target.selector)), target,
                hint = if (long) {
                    "Android reports a long click as performed only when the node consumes it, so the press may still have taken effect (check with ui_dump or ui_wait_for); " +
                            "ui_dump marks long_clickable nodes, try such an ancestor, or use x and y with the ui_gesture group"
                } else {
                    "the node may not accept click: ui_dump marks clickable nodes, try such an ancestor, or use x and y with the ui_gesture group"
                },
            )
            ToolFlowResult(actionResult(action, target))
        }
    }

    private fun setText(arguments: JsonObject): ToolFlow {
        val text = ToolArguments.string(arguments, Ui.TEXT).orEmpty()
        val append = ToolArguments.boolean(arguments, Ui.APPEND, false)
        return ToolFlow(ACTION_TIMEOUT_MS + QUERY_TIMEOUT_MS) { caller ->
            val target = resolveTarget(ToolCatalog.UI_SET_TEXT, arguments, caller)
            val value = if (append) target.node.text + text else text
            performOnNode(
                caller, "setText", JsonArray(listOf(target.selector, JsonPrimitive(value))), target,
                hint = "target an enabled editable node (ui_dump marks them editable), for example the EditText itself rather than its container",
            )
            ToolFlowResult(
                buildJsonObject {
                    put("action", "set_text")
                    put("performed", true)
                    put("via", target.via)
                    put("target", target.node.toJson(target.ref))
                    put("text", value)
                    put("appended", append)
                },
            )
        }
    }

    private fun scroll(arguments: JsonObject): ToolFlow {
        val direction = ToolArguments.string(arguments, Ui.DIRECTION, Ui.DIRECTION_FORWARD)!!
        val times = ToolArguments.long(arguments, Ui.TIMES, 1L).coerceIn(1L, Ui.MAX_TIMES).toInt()
        val forward = direction in Ui.FORWARD_DIRECTIONS
        val method = if (forward) "scrollForward" else "scrollBackward"
        return ToolFlow(ACTION_TIMEOUT_MS * times + QUERY_TIMEOUT_MS) { caller ->
            val target = resolveTargetOrNull(ToolCatalog.UI_SCROLL, arguments, caller)
                ?: findFirst(caller, SCROLLABLE_SELECTOR)?.let { Target(it, null, "scrollable", SCROLLABLE_SELECTOR) }
                ?: throw ToolFailureException(ToolFailure.nodeNotFound("no scrollable node in the active window; give nodeRef or selector"))
            var performed = 0
            for (i in 1..times) {
                val result = caller.callOrThrow("accessibility", method, JsonArray(listOf(target.selector)), ACTION_TIMEOUT_MS, ACCESSIBILITY)
                when ((result as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull) {
                    true -> performed++
                    false -> break
                    null -> throw ToolFailureException(ToolFailure.nodeNotFound("${target.node.label()} disappeared before $method could run"))
                }
                if (i < times) sleep(settleMs)
            }
            ToolFlowResult(
                buildJsonObject {
                    put("action", "scroll")
                    put("direction", direction)
                    put("requested", times)
                    put("performed", performed)
                    put("atEnd", performed < times)
                    put("via", target.via)
                    put("target", target.node.toJson(target.ref))
                    if (performed < times) put("hint", "the node stopped scrolling $direction after $performed of $times steps; it is at the end, or another node scrolls")
                },
            )
        }
    }

    private fun pressKey(arguments: JsonObject): ToolFlow {
        val key = ToolArguments.string(arguments, Ui.KEY).orEmpty()
        val (module, method, permissions) = when (key) {
            Ui.KEY_BACK -> Triple("accessibility", "back", ACCESSIBILITY)
            Ui.KEY_HOME -> Triple("accessibility", "home", ACCESSIBILITY)
            Ui.KEY_RECENTS -> Triple("accessibility", "recentApps", ACCESSIBILITY)
            Ui.KEY_NOTIFICATIONS -> Triple("keys", "notifications", KEYS)
            Ui.KEY_QUICK_SETTINGS -> Triple("keys", "quickSettings", KEYS)
            Ui.KEY_POWER_DIALOG -> Triple("keys", "powerDialog", KEYS)
            Ui.KEY_LOCK_SCREEN -> Triple("keys", "lockScreen", KEYS)
            else -> fail("${ToolCatalog.UI_PRESS_KEY}: key must be one of ${Ui.KEYS.joinToString(", ")}")
        }
        return ToolFlow(ACTION_TIMEOUT_MS) { caller ->
            val result = caller.callOrThrow(module, method, NO_ARGS, ACTION_TIMEOUT_MS, permissions)
            if ((result as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull != true) {
                throw ToolFailureException(
                    ToolFailure.actionFailed(
                        "the system refused the $key action",
                        if (key == Ui.KEY_LOCK_SCREEN) "lock_screen needs Android 9 or later; the accessibility service must be running" else "the accessibility service must be running and the screen unlocked; retry once",
                    ),
                )
            }
            ToolFlowResult(buildJsonObject {
                put("key", key)
                put("performed", true)
            })
        }
    }

    private fun swipe(arguments: JsonObject): ToolFlow {
        val x1 = ToolArguments.long(arguments, Ui.X1, 0L)
        val y1 = ToolArguments.long(arguments, Ui.Y1, 0L)
        val x2 = ToolArguments.long(arguments, Ui.X2, 0L)
        val y2 = ToolArguments.long(arguments, Ui.Y2, 0L)
        val durationMs = ToolArguments.long(arguments, Ui.DURATION_MS, Ui.DEFAULT_SWIPE_DURATION_MS).coerceIn(1L, Ui.MAX_GESTURE_DURATION_MS)
        return ToolFlow(durationMs + GESTURE_GRACE_MS) { caller ->
            checkOnScreen(ToolCatalog.UI_SWIPE, caller, listOf(x1 to y1, x2 to y2))
            performGesture(caller, "swipe", JsonArray(listOf(x1, y1, x2, y2, durationMs).map { JsonPrimitive(it) }), durationMs, "swipe from ($x1, $y1) to ($x2, $y2)")
            ToolFlowResult(
                buildJsonObject {
                    put("action", "swipe")
                    put("performed", true)
                    put("from", JsonArray(listOf(JsonPrimitive(x1), JsonPrimitive(y1))))
                    put("to", JsonArray(listOf(JsonPrimitive(x2), JsonPrimitive(y2))))
                    put("durationMs", durationMs)
                },
            )
        }
    }

    private fun gesture(arguments: JsonObject): ToolFlow {
        val durationMs = ToolArguments.long(arguments, Ui.DURATION_MS, 0L).coerceIn(1L, Ui.MAX_GESTURE_DURATION_MS)
        val points = parsePoints(arguments[Ui.POINTS])
        return ToolFlow(durationMs + GESTURE_GRACE_MS) { caller ->
            checkOnScreen(ToolCatalog.UI_GESTURE, caller, points)
            val path = JsonArray(points.map { (x, y) -> JsonArray(listOf(JsonPrimitive(x), JsonPrimitive(y))) })
            performGesture(caller, "gesture", JsonArray(listOf(JsonPrimitive(durationMs), path)), durationMs, "gesture over ${points.size} points")
            ToolFlowResult(
                buildJsonObject {
                    put("action", "gesture")
                    put("performed", true)
                    put("points", points.size)
                    put("durationMs", durationMs)
                },
            )
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * The node an action tool targets: a `nodeRef` is relocated by its fingerprint (the
     * closest of the matches wins, none is `NODE_REF_STALE`), a `selector` by `findOne`
     * (`NODE_NOT_FOUND` without a match).
     */
    private suspend fun resolveTarget(tool: String, arguments: JsonObject, caller: BridgeCaller): Target =
        resolveTargetOrNull(tool, arguments, caller) ?: fail("$tool needs nodeRef (a #n reference from ui_dump) or selector")

    /** [resolveTarget] for a tool whose target is optional: null when neither `nodeRef` nor `selector` is given. */
    private suspend fun resolveTargetOrNull(tool: String, arguments: JsonObject, caller: BridgeCaller): Target? {
        val hasRef = arguments.has(Ui.NODE_REF)
        val hasSelector = arguments.has(Ui.SELECTOR)
        if (hasRef && hasSelector) fail("$tool accepts nodeRef or selector, not both")
        if (hasRef) {
            val ref = ToolArguments.string(arguments, Ui.NODE_REF).orEmpty().trim()
            val entry = when (val resolution = registry.resolve(ref)) {
                is NodeRefRegistry.Resolution.Found -> resolution.entry
                is NodeRefRegistry.Resolution.Stale -> throw ToolFailureException(ToolFailure.nodeRefStale(ref.ifEmpty { "nodeRef" }, resolution.detail))
            }
            val candidates = UiNodeInfo.listFromJson(caller.callOrThrow("accessibility", "findAll", JsonArray(listOf(UiSelectors.relocation(entry.node))), QUERY_TIMEOUT_MS, ACCESSIBILITY))
            val located = candidates.minByOrNull { it.bounds.distanceTo(entry.node.bounds) }
                ?: throw ToolFailureException(ToolFailure.nodeRefStale(entry.ref, "(${entry.node.label()}) is no longer in the active window"))
            return Target(located, entry.ref, "nodeRef", UiSelectors.exact(located))
        }
        if (hasSelector) {
            val selector = UiSelectors.parse(tool, arguments[Ui.SELECTOR])
            val node = findFirst(caller, selector) ?: throw ToolFailureException(ToolFailure.nodeNotFound("no node matches ${UiSelectors.describe(selector)} in the active window"))
            return Target(node, null, "selector", selector)
        }
        return null
    }

    private suspend fun findFirst(caller: BridgeCaller, selector: JsonObject): UiNodeInfo? =
        UiNodeInfo.fromJson(caller.callOrThrow("accessibility", "findOne", JsonArray(listOf(selector)), QUERY_TIMEOUT_MS, ACCESSIBILITY))

    /** Runs a selector action; `true` is success, `false` is `ACTION_FAILED`, `null` (no match) is `NODE_NOT_FOUND`. */
    private suspend fun performOnNode(caller: BridgeCaller, method: String, args: JsonArray, target: Target, hint: String) {
        val result = caller.callOrThrow("accessibility", method, args, ACTION_TIMEOUT_MS, ACCESSIBILITY)
        when ((result as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull) {
            true -> Unit
            false -> throw ToolFailureException(ToolFailure.actionFailed("${target.node.label()} did not perform $method", hint))
            null -> throw ToolFailureException(ToolFailure.nodeNotFound("${target.node.label()} disappeared before $method could run"))
        }
    }

    /** Runs a coordinate gesture; the host answers `true` when the system completed it. */
    private suspend fun performGesture(caller: BridgeCaller, method: String, args: JsonArray, durationMs: Long, what: String) {
        val result = caller.callOrThrow("accessibility", method, args, durationMs + GESTURE_GRACE_MS, GESTURE)
        if ((result as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull != true) {
            throw ToolFailureException(ToolFailure.actionFailed("the system cancelled the $what", "another touch or gesture may have interrupted it; retry, or check that the accessibility service can dispatch gestures"))
        }
    }

    private fun actionResult(action: String, target: Target): JsonObject = buildJsonObject {
        put("action", action)
        put("performed", true)
        put("via", target.via)
        put("target", target.node.toJson(target.ref))
    }

    /** Refuses points beyond the screen; the size comes from `device.info` and is cached for a minute. */
    private suspend fun checkOnScreen(tool: String, caller: BridgeCaller, points: List<Pair<Long, Long>>) {
        val size = screenSize(caller) ?: return
        val limit = max(size.width, size.height).toLong()
        points.firstOrNull { (x, y) -> x > limit || y > limit }?.let { (x, y) ->
            fail("$tool: ($x, $y) is outside the ${size.width}x${size.height} screen")
        }
    }

    private suspend fun screenSize(caller: BridgeCaller): ScreenSize? {
        screen?.takeIf { clock() - it.readAt < SCREEN_CACHE_MS }?.let { return it }
        val outcome = caller.call("device", "info", NO_ARGS, QUERY_TIMEOUT_MS, DEVICE)
        val info = (outcome as? BridgeOutcome.Ok)?.result as? JsonObject ?: return null
        val screenJson = info["screen"] as? JsonObject ?: return null
        val width = (screenJson["width"] as? JsonPrimitive)?.intOrNull ?: return null
        val height = (screenJson["height"] as? JsonPrimitive)?.intOrNull ?: return null
        if (width <= 0 || height <= 0) return null
        return ScreenSize(width, height, clock()).also { screen = it }
    }

    private fun parsePoints(element: JsonElement?): List<Pair<Long, Long>> {
        val array = element as? JsonArray ?: fail("${ToolCatalog.UI_GESTURE}: points must be an array of [x, y] pairs")
        if (array.size < 2) fail("${ToolCatalog.UI_GESTURE}: points needs at least 2 [x, y] pairs")
        if (array.size > Ui.MAX_POINTS) fail("${ToolCatalog.UI_GESTURE}: points accepts at most ${Ui.MAX_POINTS} pairs")
        return array.mapIndexed { index, point ->
            val pair = point as? JsonArray ?: fail("${ToolCatalog.UI_GESTURE}: points[$index] must be an [x, y] pair")
            if (pair.size != 2) fail("${ToolCatalog.UI_GESTURE}: points[$index] must have exactly two integers")
            val x = (pair[0] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull ?: fail("${ToolCatalog.UI_GESTURE}: points[$index] x must be an integer")
            val y = (pair[1] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull ?: fail("${ToolCatalog.UI_GESTURE}: points[$index] y must be an integer")
            if (x < 0 || y < 0 || x > Ui.MAX_COORDINATE || y > Ui.MAX_COORDINATE) fail("${ToolCatalog.UI_GESTURE}: points[$index] must lie within 0..${Ui.MAX_COORDINATE}")
            x to y
        }
    }

    private suspend fun BridgeCaller.objectOrThrow(module: String, method: String, args: JsonArray, timeoutMs: Long, permissions: List<String>): JsonObject =
        callOrThrow(module, method, args, timeoutMs, permissions) as? JsonObject
            ?: throw ToolFailureException(ToolFailure.internal("$module.$method answered without an object"))

    private fun JsonObject.has(name: String): Boolean = this[name].let { it != null && it !is JsonNull }

    private fun JsonObject.flag(name: String): Boolean = (this[name] as? JsonPrimitive)?.booleanOrNull ?: false

    private fun JsonObject.string(name: String): String = (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

    private fun fail(message: String): Nothing = throw ToolArgumentException(ToolFailure.invalidArguments(message))

    companion object {

        /** Interval between two polls of `ui_find` / `ui_wait_for`. */
        const val POLL_MS = 500L

        /** Pause between two scroll steps of `ui_scroll`. */
        const val SCROLL_SETTLE_MS = 300L

        /** Host timeout of a dump or an explanation. */
        const val DUMP_TIMEOUT_MS = 20_000L

        /** Host timeout of a find, a window query, or a screen size read. */
        const val QUERY_TIMEOUT_MS = 10_000L

        /** Host timeout of a node action. */
        const val ACTION_TIMEOUT_MS = 10_000L

        /** Added to a gesture's duration for its host timeout. */
        const val GESTURE_GRACE_MS = 5_000L

        /** Duration of the coordinate tap of `ui_click`. */
        const val TAP_MS = 100L

        /** Duration of the coordinate press of `ui_long_click` (the long-press timeout is 400 ms by default). */
        const val LONG_PRESS_MS = 700L

        /** How long a `device.info` screen size is reused. */
        const val SCREEN_CACHE_MS = 60_000L

        val ACCESSIBILITY: List<String> = listOf("accessibility")
        val GESTURE: List<String> = listOf("accessibility", "accessibility.gesture")
        val KEYS: List<String> = listOf("keys")
        val WINDOW: List<String> = listOf("app.query", "accessibility")
        val DEVICE: List<String> = listOf("device")

        val NO_ARGS: JsonArray = JsonArray(emptyList())

        /** The default target of `ui_scroll`: the first scrollable node in pre-order. */
        val SCROLLABLE_SELECTOR: JsonObject = buildJsonObject { put("scrollable", true) }
    }
}
