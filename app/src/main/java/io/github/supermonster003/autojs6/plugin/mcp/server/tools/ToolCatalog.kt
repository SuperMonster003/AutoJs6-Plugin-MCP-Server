package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.DevicePingTool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.autojs.plugin.mcp.server.api.McpServerContract

/**
 * The tool table of the plugin (appendix A.2). P2.3 shipped the minimal set that proves the whole
 * path (`device_ping` locally, `device_info` and `script_run` through the host); P3.1 completes the
 * `script` group; P3.2 adds `ui` / `ui_gesture` flows; P3.3 adds the `screen` flows and image content.
 * Later phases append rows. `ToolCatalogTest` guards names, schemas, defaults, and a snapshot.
 */
object ToolCatalog {

    const val DEVICE_PING = DevicePingTool.NAME
    const val DEVICE_INFO = "device_info"
    const val SCRIPT_RUN = "script_run"
    const val SCRIPT_RUN_FILE = "script_run_file"
    const val SCRIPT_STOP = "script_stop"
    const val SCRIPT_STOP_ALL = "script_stop_all"
    const val SCRIPT_LIST = "script_list"
    const val CONSOLE_TAIL = "console_tail"
    const val UI_DUMP = "ui_dump"
    const val UI_FIND = "ui_find"
    const val UI_CURRENT_WINDOW = "ui_current_window"
    const val UI_EXPLAIN_SELECTOR = "ui_explain_selector"
    const val UI_WAIT_FOR = "ui_wait_for"
    const val UI_CLICK = "ui_click"
    const val UI_LONG_CLICK = "ui_long_click"
    const val UI_SET_TEXT = "ui_set_text"
    const val UI_SCROLL = "ui_scroll"
    const val UI_PRESS_KEY = "ui_press_key"
    const val UI_SWIPE = "ui_swipe"
    const val UI_GESTURE = "ui_gesture"
    const val SCREEN_CAPTURE = "screen_capture"
    const val SCREEN_STATE = "screen_state"

    /** Screenshot options shared by the catalog and ScreenTools (P3.3). */
    object Screen {
        const val SCALE = "scale"
        const val MAX_WIDTH = "maxWidth"
        const val FORMAT = "format"
        const val QUALITY = "quality"
        const val REGION = "region"
        const val DEFAULT_LONG_EDGE = 1280
        const val DEFAULT_QUALITY = 70
        const val MAX_WIDTH_LIMIT = 8192L
        const val CAPTURE_TIMEOUT_MS = 120_000L
        const val CONSENT_TIMEOUT_MS = 60_000L
        const val HOST_CAPTURE_TIMEOUT_MS = 15_000L
        const val MAX_ATTEMPTS = 12
        val FORMATS = listOf("jpeg", "png", "webp")
    }

    /** `script_run` / `script_run_file` argument names and bounds, shared with the executor and the tests. */
    object ScriptRun {
        const val SOURCE = "source"
        const val PATH = "path"
        const val NAME = "name"
        const val WORKING_DIRECTORY = "workingDirectory"
        const val ARGUMENTS = "arguments"
        const val TIMEOUT_MS = "timeoutMs"
        const val WAIT_FOR_COMPLETION = "waitForCompletion"
        const val CAPTURE_CONSOLE = "captureConsole"
        const val MAX_CONSOLE_LINES = "maxConsoleLines"

        const val DEFAULT_NAME = "mcp_script"
        const val DEFAULT_TIMEOUT_MS = 60_000L
        const val MIN_TIMEOUT_MS = 1_000L
        const val DEFAULT_MAX_CONSOLE_LINES = 200L
        const val MAX_SOURCE_BYTES = 512 * 1024
        const val MAX_NAME_LENGTH = 128
        const val MAX_PATH_LENGTH = 512
        const val MAX_WORKING_DIRECTORY_LENGTH = 512

        /** The host refuses an `engines.execScript` start timeout above this (its own ceiling). */
        const val MAX_START_TIMEOUT_MS = 60_000L
    }

    /** `script_stop` argument names. */
    object ScriptStop {
        const val EXECUTION_ID = "executionId"
    }

    /** `console_tail` argument names and bounds (the host's `console.tail`). */
    object ConsoleTail {
        const val LINES = "lines"
        const val SINCE_ID = "sinceId"
        const val LEVEL = "level"
        const val DEFAULT_LINES = 100L
        val LEVELS: List<String> = listOf("verbose", "debug", "info", "warn", "error", "assert")
    }

    /** `ui_*` argument names, enumerations, and bounds (roadmap P3.2), shared with `UiTools` and the tests. */
    object Ui {
        const val FORMAT = "format"
        const val MAX_DEPTH = "maxDepth"
        const val MAX_NODES = "maxNodes"
        const val VISIBLE_ONLY = "visibleOnly"
        const val WINDOW = "window"
        const val SELECTOR = "selector"
        const val NODE_REF = "nodeRef"
        const val LIMIT = "limit"
        const val TIMEOUT_MS = "timeoutMs"
        const val STATE = "state"
        const val X = "x"
        const val Y = "y"
        const val TEXT = "text"
        const val APPEND = "append"
        const val DIRECTION = "direction"
        const val TIMES = "times"
        const val KEY = "key"
        const val X1 = "x1"
        const val Y1 = "y1"
        const val X2 = "x2"
        const val Y2 = "y2"
        const val DURATION_MS = "durationMs"
        const val POINTS = "points"

        const val FORMAT_TEXT = "text"
        const val FORMAT_JSON = "json"
        const val FORMAT_XML = "xml"
        val FORMATS: List<String> = listOf(FORMAT_TEXT, FORMAT_JSON, FORMAT_XML)
        const val WINDOW_ACTIVE = "active"
        const val WINDOW_ALL = "all"
        val WINDOWS: List<String> = listOf(WINDOW_ACTIVE, WINDOW_ALL)
        const val STATE_APPEAR = "appear"
        const val STATE_DISAPPEAR = "disappear"
        val STATES: List<String> = listOf(STATE_APPEAR, STATE_DISAPPEAR)
        const val DIRECTION_FORWARD = "forward"
        val FORWARD_DIRECTIONS: Set<String> = setOf(DIRECTION_FORWARD, "down", "right")
        val DIRECTIONS: List<String> = listOf(DIRECTION_FORWARD, "backward", "down", "up", "right", "left")
        const val KEY_BACK = "back"
        const val KEY_HOME = "home"
        const val KEY_RECENTS = "recents"
        const val KEY_NOTIFICATIONS = "notifications"
        const val KEY_QUICK_SETTINGS = "quick_settings"
        const val KEY_POWER_DIALOG = "power_dialog"
        const val KEY_LOCK_SCREEN = "lock_screen"
        val KEYS: List<String> = listOf(KEY_BACK, KEY_HOME, KEY_RECENTS, KEY_NOTIFICATIONS, KEY_QUICK_SETTINGS, KEY_POWER_DIALOG, KEY_LOCK_SCREEN)

        const val DEFAULT_MAX_NODES = 200L
        val MAX_NODES_LIMIT: Long = McpServerContract.MAX_DUMP_NODES.toLong()
        const val DEFAULT_FIND_LIMIT = 10L
        const val MAX_FIND_LIMIT = 100L
        const val MAX_FIND_TIMEOUT_MS = 60_000L
        const val DEFAULT_WAIT_TIMEOUT_MS = 10_000L
        const val MIN_WAIT_TIMEOUT_MS = 100L
        const val MAX_WAIT_TIMEOUT_MS = 120_000L
        const val MAX_TIMES = 20L
        const val MAX_TEXT_BYTES = 16 * 1024
        const val MAX_COORDINATE = 65_535L
        const val DEFAULT_SWIPE_DURATION_MS = 300L

        /** The host limit `MAX_ACCESSIBILITY_GESTURE_DURATION_MS`; longer gestures are refused here. */
        const val MAX_GESTURE_DURATION_MS = 10_000L
        const val MAX_POINTS = 64
    }

    private const val RUN_RESULT = " The result carries executionId, status (finished, error, running), durationMs, " +
            "the exception with its line when the script threw, and the newest console lines. A script still " +
            "running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output."

    val devicePing: ToolSpec = ToolSpec(
        name = DEVICE_PING,
        title = "Device ping",
        description = DevicePingTool.DESCRIPTION,
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.DEVICE,
        timeoutMs = 5_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val deviceInfo: ToolSpec = ToolSpec(
        name = DEVICE_INFO,
        title = "Device info",
        description = "Returns the device build, screen, battery, memory, AutoJs6 host version and process, " +
                "accessibility service state, screen state, locale, and time zone as AutoJs6 reports them " +
                "(schema autojs6-bridge-device-info-v1). No hardware identifiers.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.DEVICE,
        bridge = BridgeMethod("device", "info"),
        permissions = listOf("device"),
        timeoutMs = 10_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val scriptRun: ToolSpec = ToolSpec(
        name = SCRIPT_RUN,
        title = "Run script",
        description = "Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API) and by default " +
                "waits for it to finish. Use it for automation steps: toasts, UI actions, file work, app launches." + RUN_RESULT,
        inputSchema = JsonSchemas.objectSchema(
            properties = runProperties(
                ScriptRun.SOURCE to JsonSchemas.string("JavaScript source to execute", maxLength = ScriptRun.MAX_SOURCE_BYTES),
                ScriptRun.NAME to JsonSchemas.string(
                    "Script name shown in the AutoJs6 task list; AutoJs6 appends .js itself, a given .js suffix is dropped",
                    maxLength = ScriptRun.MAX_NAME_LENGTH,
                    default = ScriptRun.DEFAULT_NAME,
                ),
            ),
            required = listOf(ScriptRun.SOURCE),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "execScript"),
        permissions = listOf("engines", "engines.exec"),
        timeoutMs = ScriptRun.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(openWorld = true),
    )

    val scriptRunFile: ToolSpec = ToolSpec(
        name = SCRIPT_RUN_FILE,
        title = "Run script file",
        description = "Runs a script file that already exists on the device (AutoJs6 picks the engine from the suffix) " +
                "and by default waits for it to finish." + RUN_RESULT,
        inputSchema = JsonSchemas.objectSchema(
            properties = runProperties(
                ScriptRun.PATH to JsonSchemas.string(
                    "POSIX-style path of the script file, relative to the working directory; it cannot escape it",
                    maxLength = ScriptRun.MAX_PATH_LENGTH,
                ),
            ),
            required = listOf(ScriptRun.PATH),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "execScriptFile"),
        permissions = listOf("engines", "engines.exec"),
        timeoutMs = ScriptRun.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(openWorld = true),
    )

    val scriptStop: ToolSpec = ToolSpec(
        name = SCRIPT_STOP,
        title = "Stop script",
        description = "Stops one running AutoJs6 script by the executionId that script_run, script_run_file, or script_list reported.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                ScriptStop.EXECUTION_ID to JsonSchemas.integer("Execution id of the script to stop", minimum = 0),
            ),
            required = listOf(ScriptStop.EXECUTION_ID),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "stop"),
        permissions = listOf("engines"),
        timeoutMs = 10_000L,
        hints = ToolHints(idempotent = true),
    )

    val scriptStopAll: ToolSpec = ToolSpec(
        name = SCRIPT_STOP_ALL,
        title = "Stop all scripts",
        description = "Stops every script AutoJs6 is running, including ones started on the phone, and returns how many were stopped.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "stopAll"),
        permissions = listOf("engines"),
        timeoutMs = 10_000L,
        hints = ToolHints(destructive = true, idempotent = true),
    )

    val scriptList: ToolSpec = ToolSpec(
        name = SCRIPT_LIST,
        title = "List scripts",
        description = "Lists the scripts AutoJs6 is running or starting, with executionId, name, path, working directory, state, and uptime.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("engines", "list"),
        permissions = listOf("engines"),
        timeoutMs = 10_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val consoleTail: ToolSpec = ToolSpec(
        name = CONSOLE_TAIL,
        title = "Console tail",
        description = "Returns the newest lines of the AutoJs6 console, which every script shares; optionally only entries " +
                "after sinceId or at least a level. nextSinceId in the result continues from where this call ended.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                ConsoleTail.LINES to JsonSchemas.integer(
                    "How many of the newest lines to return",
                    minimum = 1,
                    maximum = McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong(),
                    default = ConsoleTail.DEFAULT_LINES,
                ),
                ConsoleTail.SINCE_ID to JsonSchemas.integer("Only entries with an id above this one (the nextSinceId of a previous call)", minimum = 0),
                ConsoleTail.LEVEL to JsonSchemas.string("Lowest level to include", enum = ConsoleTail.LEVELS),
            ),
        ),
        group = ToolGroup.SCRIPT,
        bridge = BridgeMethod("console", "tail"),
        permissions = listOf("console"),
        timeoutMs = 10_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )


    private const val NODE_REF_DESCRIPTION = "A #n reference from the last ui_dump (or ui_find / ui_wait_for); the node is found again by its class, text, " +
            "description, id, and bounds, and NODE_REF_STALE asks for a new ui_dump when it is gone"
    private const val TARGET_NOTE = " Give nodeRef or selector, not both."

    val uiDump: ToolSpec = ToolSpec(
        name = UI_DUMP,
        title = "Dump UI tree",
        description = "Dumps the accessibility node tree of the active window as compact text: one node per line with a #n reference, " +
                "an indent per depth, the short class name, the state markers that apply (clickable, long_clickable, checkable, checked, " +
                "scrollable, editable, focused, selected, !enabled, hidden), the text in quotes, desc=, id= (name part only), and the " +
                "position (bounds [l,t][r,b] for a node with children, c=(x,y) for a leaf). Pass a #n reference as nodeRef to ui_click, " +
                "ui_long_click, ui_set_text, or ui_scroll; references stay valid until the next ui_dump or for 60 s. Call it before acting " +
                "and again after the screen changed. format json returns the nodes as objects with every flag; format xml returns the " +
                "uiautomator-style export.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                Ui.FORMAT to JsonSchemas.string("Output form", enum = Ui.FORMATS, default = Ui.FORMAT_TEXT),
                Ui.MAX_DEPTH to JsonSchemas.integer("Deepest level below the window root to include", minimum = 0, maximum = McpServerContract.MAX_DUMP_DEPTH.toLong(), default = McpServerContract.MAX_DUMP_DEPTH.toLong()),
                Ui.MAX_NODES to JsonSchemas.integer("Most nodes to include, in pre-order", minimum = 1, maximum = Ui.MAX_NODES_LIMIT, default = Ui.DEFAULT_MAX_NODES),
                Ui.VISIBLE_ONLY to JsonSchemas.boolean("Skip nodes that are not visible to the user, with their subtrees", default = true),
                Ui.WINDOW to JsonSchemas.string("The active window only, or every accessibility window (all)", enum = Ui.WINDOWS, default = Ui.WINDOW_ACTIVE),
            ),
        ),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "dump"),
        permissions = listOf("accessibility"),
        timeoutMs = 20_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val uiFind: ToolSpec = ToolSpec(
        name = UI_FIND,
        title = "Find UI nodes",
        description = "Finds the nodes of the active window that match every condition of the selector, optionally waiting up to " +
                "timeoutMs for the first match, and returns up to limit of them with #n references, bounds, and center. An empty count " +
                "is not an error; ui_explain_selector tells which condition fails.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                Ui.SELECTOR to JsonSchemas.selector("The conditions a node must meet (all of them)"),
                Ui.LIMIT to JsonSchemas.integer("Most nodes to return, in pre-order", minimum = 1, maximum = Ui.MAX_FIND_LIMIT, default = Ui.DEFAULT_FIND_LIMIT),
                Ui.TIMEOUT_MS to JsonSchemas.integer("How long to keep looking for a first match, in milliseconds; 0 looks once", minimum = 0, maximum = Ui.MAX_FIND_TIMEOUT_MS, default = 0L),
            ),
            required = listOf(Ui.SELECTOR),
        ),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "findAll"),
        permissions = listOf("accessibility"),
        timeoutMs = 15_000L,
        maxTimeoutMs = Ui.MAX_FIND_TIMEOUT_MS + 15_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val uiCurrentWindow: ToolSpec = ToolSpec(
        name = UI_CURRENT_WINDOW,
        title = "Current window",
        description = "Returns the package and activity in the foreground, whether the AutoJs6 accessibility service is available, and " +
                "the accessibility windows with their type, title, bounds, and focus.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.UI,
        bridge = BridgeMethod("app", "currentWindow"),
        permissions = listOf("app.query", "accessibility"),
        timeoutMs = 10_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val uiExplainSelector: ToolSpec = ToolSpec(
        name = UI_EXPLAIN_SELECTOR,
        title = "Explain selector",
        description = "Explains why a selector matches or not: evaluates its conditions one by one over the active window and reports " +
                "how many nodes pass each step cumulatively, the first failing condition, the matches, and the near misses. Use it when " +
                "ui_find returns nothing.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(Ui.SELECTOR to JsonSchemas.selector("The selector to explain")),
            required = listOf(Ui.SELECTOR),
        ),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "explain"),
        permissions = listOf("accessibility"),
        timeoutMs = 20_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val uiWaitFor: ToolSpec = ToolSpec(
        name = UI_WAIT_FOR,
        title = "Wait for UI node",
        description = "Waits until a node matching the selector appears (default) or disappears, polling the active window every 0.5 s " +
                "for up to timeoutMs, and answers TIMEOUT when the state is not reached. Use it after an action that opens a screen or " +
                "dismisses a dialog.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                Ui.SELECTOR to JsonSchemas.selector("The conditions of the node to wait for"),
                Ui.STATE to JsonSchemas.string("Wait for a match to appear or for every match to disappear", enum = Ui.STATES, default = Ui.STATE_APPEAR),
                Ui.TIMEOUT_MS to JsonSchemas.integer("How long to wait, in milliseconds", minimum = Ui.MIN_WAIT_TIMEOUT_MS, maximum = Ui.MAX_WAIT_TIMEOUT_MS, default = Ui.DEFAULT_WAIT_TIMEOUT_MS),
            ),
            required = listOf(Ui.SELECTOR),
        ),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "findOne"),
        permissions = listOf("accessibility"),
        timeoutMs = 20_000L,
        maxTimeoutMs = Ui.MAX_WAIT_TIMEOUT_MS + 15_000L,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val uiClick: ToolSpec = ToolSpec(
        name = UI_CLICK,
        title = "Click",
        description = "Clicks a node given by nodeRef (a #n reference from the last ui_dump), by selector (the first match in pre-order), " +
                "or by x and y (a coordinate tap, allowed only while the ui_gesture group is enabled). The accessibility click climbs to " +
                "the nearest clickable ancestor when the node itself is not clickable. Returns the node it acted on." + TARGET_NOTE,
        inputSchema = JsonSchemas.objectSchema(properties = targetProperties(coordinates = true)),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "click"),
        permissions = listOf("accessibility"),
        timeoutMs = 20_000L,
    )

    val uiLongClick: ToolSpec = ToolSpec(
        name = UI_LONG_CLICK,
        title = "Long click",
        description = "Long-presses a node given by nodeRef or selector (the accessibility long click climbs to the nearest node that " +
                "accepts it), or by x and y as a 700 ms press at that point (allowed only while the ui_gesture group is enabled)." + TARGET_NOTE,
        inputSchema = JsonSchemas.objectSchema(properties = targetProperties(coordinates = true)),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "longClick"),
        permissions = listOf("accessibility"),
        timeoutMs = 20_000L,
    )

    val uiSetText: ToolSpec = ToolSpec(
        name = UI_SET_TEXT,
        title = "Set text",
        description = "Sets the text of an editable node (an EditText, marked editable by ui_dump) given by nodeRef or selector; append " +
                "adds to the current text instead of replacing it. Works without focus or the keyboard; ACTION_FAILED means the node is " +
                "not editable or not enabled." + TARGET_NOTE,
        inputSchema = JsonSchemas.objectSchema(
            properties = targetProperties(coordinates = false).apply {
                put(Ui.TEXT, JsonSchemas.string("The text to set (or to append)", maxLength = Ui.MAX_TEXT_BYTES))
                put(Ui.APPEND, JsonSchemas.boolean("Append to the current text instead of replacing it", default = false))
            },
            required = listOf(Ui.TEXT),
        ),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "setText"),
        permissions = listOf("accessibility"),
        timeoutMs = 20_000L,
    )

    val uiScroll: ToolSpec = ToolSpec(
        name = UI_SCROLL,
        title = "Scroll",
        description = "Scrolls a node given by nodeRef or selector, or the first scrollable node of the window when neither is given: " +
                "forward, down, and right move towards the end, backward, up, and left towards the start; times repeats the step. " +
                "performed counts the steps the node accepted, fewer than requested means it reached the end." + TARGET_NOTE,
        inputSchema = JsonSchemas.objectSchema(
            properties = targetProperties(coordinates = false).apply {
                put(Ui.DIRECTION, JsonSchemas.string("Scroll direction", enum = Ui.DIRECTIONS, default = Ui.DIRECTION_FORWARD))
                put(Ui.TIMES, JsonSchemas.integer("How many scroll steps to perform", minimum = 1, maximum = Ui.MAX_TIMES, default = 1L))
            },
        ),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "scrollForward"),
        permissions = listOf("accessibility"),
        timeoutMs = 30_000L,
    )

    val uiPressKey: ToolSpec = ToolSpec(
        name = UI_PRESS_KEY,
        title = "Press key",
        description = "Presses a global key through the accessibility service: back, home, recents, notifications (opens the notification " +
                "shade), quick_settings, power_dialog, or lock_screen (Android 9 or later).",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(Ui.KEY to JsonSchemas.string("The key to press", enum = Ui.KEYS)),
            required = listOf(Ui.KEY),
        ),
        group = ToolGroup.UI,
        bridge = BridgeMethod("accessibility", "back"),
        permissions = listOf("accessibility"),
        timeoutMs = 10_000L,
    )

    val uiSwipe: ToolSpec = ToolSpec(
        name = UI_SWIPE,
        title = "Swipe",
        description = "Swipes one finger from (x1, y1) to (x2, y2) in device pixels over durationMs; take the coordinates from ui_dump " +
                "bounds or a screenshot. Part of the ui_gesture group, which is off by default.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                Ui.X1 to JsonSchemas.integer("Start x in device pixels", minimum = 0, maximum = Ui.MAX_COORDINATE),
                Ui.Y1 to JsonSchemas.integer("Start y in device pixels", minimum = 0, maximum = Ui.MAX_COORDINATE),
                Ui.X2 to JsonSchemas.integer("End x in device pixels", minimum = 0, maximum = Ui.MAX_COORDINATE),
                Ui.Y2 to JsonSchemas.integer("End y in device pixels", minimum = 0, maximum = Ui.MAX_COORDINATE),
                Ui.DURATION_MS to JsonSchemas.integer("Duration of the swipe in milliseconds", minimum = 1, maximum = Ui.MAX_GESTURE_DURATION_MS, default = Ui.DEFAULT_SWIPE_DURATION_MS),
            ),
            required = listOf(Ui.X1, Ui.Y1, Ui.X2, Ui.Y2),
        ),
        group = ToolGroup.UI_GESTURE,
        bridge = BridgeMethod("accessibility", "swipe"),
        permissions = listOf("accessibility", "accessibility.gesture"),
        timeoutMs = 15_000L,
        maxTimeoutMs = Ui.MAX_GESTURE_DURATION_MS + 10_000L,
    )

    val uiGesture: ToolSpec = ToolSpec(
        name = UI_GESTURE,
        title = "Gesture",
        description = "Performs a free-path one-finger gesture through the given points over durationMs (at most 10 s): the first point " +
                "is the touch down, the last the lift. Part of the ui_gesture group, which is off by default.",
        inputSchema = JsonSchemas.objectSchema(
            properties = linkedMapOf(
                Ui.DURATION_MS to JsonSchemas.integer("Duration of the whole gesture in milliseconds", minimum = 1, maximum = Ui.MAX_GESTURE_DURATION_MS),
                Ui.POINTS to JsonSchemas.array(
                    "The path as [x, y] pairs in device pixels, at least two",
                    items = JsonSchemas.array("One [x, y] point", items = JsonSchemas.integer("Coordinate in device pixels", minimum = 0, maximum = Ui.MAX_COORDINATE), minItems = 2, maxItems = 2),
                    minItems = 2,
                    maxItems = Ui.MAX_POINTS,
                ),
            ),
            required = listOf(Ui.DURATION_MS, Ui.POINTS),
        ),
        group = ToolGroup.UI_GESTURE,
        bridge = BridgeMethod("accessibility", "gesture"),
        permissions = listOf("accessibility", "accessibility.gesture"),
        timeoutMs = 15_000L,
        maxTimeoutMs = Ui.MAX_GESTURE_DURATION_MS + 10_000L,
    )

    /** Every tool in list order; `tools/list` keeps this order. */
    val screenCapture = ToolSpec(
        name = SCREEN_CAPTURE,
        title = "Capture the screen",
        description = "Capture the phone screen as an MCP image with dimensions, size, duration and capture source. " +
                "Uses accessibility on Android 11+ and falls back to MediaProjection, which requires consent on the phone the first time. " +
                "Defaults to JPEG quality 70 and a longest edge of 1280 pixels. Choose scale or maxWidth to override the size. " +
                "Images above the 4 MiB base64 limit are retried at lower quality or smaller dimensions; metadata reports adjustments.",
        group = ToolGroup.SCREEN,
        inputSchema = JsonSchemas.objectSchema(linkedMapOf(
            Screen.SCALE to buildJsonObject {
                put("type", "number")
                put("description", "Scale after cropping, from 0.0001 to 1; mutually exclusive with maxWidth")
                put("minimum", 0.0001)
                put("maximum", 1)
            },
            Screen.MAX_WIDTH to JsonSchemas.integer("Maximum output width, preserving aspect ratio; mutually exclusive with scale", 1, Screen.MAX_WIDTH_LIMIT),
            Screen.FORMAT to JsonSchemas.string("Image encoding", default = "jpeg", enum = Screen.FORMATS),
            Screen.QUALITY to JsonSchemas.integer("JPEG or WebP quality; PNG ignores quality", 1, 100, Screen.DEFAULT_QUALITY.toLong()),
            Screen.REGION to JsonSchemas.bounds("Crop in current screen pixels before scaling; right and bottom are exclusive"),
        )),
        bridge = BridgeMethod("accessibility", "screenshot"),
        permissions = listOf("accessibility", "screen_capture", "image", "device"),
        timeoutMs = Screen.CAPTURE_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = false, openWorld = true),
    )

    val screenState = ToolSpec(
        name = SCREEN_STATE,
        title = "Read screen state",
        description = "Read whether the screen is on, its current width and height, orientation, rotation, and density. Does not request screen capture consent.",
        group = ToolGroup.SCREEN,
        inputSchema = JsonSchemas.objectSchema(),
        bridge = BridgeMethod("device", "isScreenOn"),
        permissions = listOf("device"),
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val all: List<ToolSpec> = listOf(
        devicePing, deviceInfo,
        scriptRun, scriptRunFile, scriptStop, scriptStopAll, scriptList, consoleTail,
        uiDump, uiFind, uiCurrentWindow, uiExplainSelector, uiWaitFor, uiClick, uiLongClick, uiSetText, uiScroll, uiPressKey,
        uiSwipe, uiGesture,
        screenCapture, screenState,
    )

    val byName: Map<String, ToolSpec> = all.associateBy { it.name }

    /** The groups the catalog uses, for the `mcpServerToolGroups` capability. */
    val groups: List<ToolGroup> = ToolGroup.entries.filter { group -> all.any { it.group == group } }

    fun enabled(permissions: ToolPermissions): List<ToolSpec> = all.filter { permissions.isEnabled(it.group) }

    /** Pretty JSON of the whole catalog; `ToolCatalogTest` compares it with the committed snapshot. */
    fun snapshot(): String = PRETTY.encodeToString(
        JsonArray.serializer(),
        JsonArray(all.map { it.toSnapshotJson() }),
    )

    /** The compact form of the catalog for status and dumps. */
    fun summary(permissions: ToolPermissions): String = buildJsonObject {
        ToolGroup.entries.forEach { group -> put(group.id, permissions.isEnabled(group)) }
    }.toString()

    /** The options both run tools share, after the tool-specific leading properties. */
    private fun runProperties(vararg leading: Pair<String, JsonObject>): LinkedHashMap<String, JsonObject> =
        linkedMapOf(*leading).apply {
            put(
                ScriptRun.WORKING_DIRECTORY,
                JsonSchemas.string(
                    "POSIX-style path relative to the AutoJs6 working directory; the script cannot escape it",
                    maxLength = ScriptRun.MAX_WORKING_DIRECTORY_LENGTH,
                    default = ".",
                ),
            )
            put(ScriptRun.ARGUMENTS, JsonSchemas.primitiveMap("Values exposed to the script through engines.myEngine().execArgv"))
            put(
                ScriptRun.TIMEOUT_MS,
                JsonSchemas.integer(
                    "How long to wait for the script to finish, in milliseconds; a script still running afterwards is not killed",
                    minimum = ScriptRun.MIN_TIMEOUT_MS,
                    maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS,
                    default = ScriptRun.DEFAULT_TIMEOUT_MS,
                ),
            )
            put(ScriptRun.WAIT_FOR_COMPLETION, JsonSchemas.boolean("Wait for the script to finish before answering; false returns the executionId at once", default = true))
            put(ScriptRun.CAPTURE_CONSOLE, JsonSchemas.boolean("Include the console lines printed while the script ran", default = true))
            put(
                ScriptRun.MAX_CONSOLE_LINES,
                JsonSchemas.integer(
                    "Keep at most this many of the newest console lines",
                    minimum = 1,
                    maximum = McpServerContract.MAX_CONSOLE_TAIL_LINES.toLong(),
                    default = ScriptRun.DEFAULT_MAX_CONSOLE_LINES,
                ),
            )
        }

    /** `nodeRef` and `selector` (and the coordinate pair for the click tools) every action tool accepts. */
    private fun targetProperties(coordinates: Boolean): LinkedHashMap<String, JsonObject> = linkedMapOf<String, JsonObject>().apply {
        put(Ui.NODE_REF, JsonSchemas.string(NODE_REF_DESCRIPTION, maxLength = 32))
        put(Ui.SELECTOR, JsonSchemas.selector("Conditions that identify the node; the first match in pre-order is used"))
        if (coordinates) {
            put(Ui.X, JsonSchemas.integer("x of a coordinate tap in device pixels (ui_gesture group)", minimum = 0, maximum = Ui.MAX_COORDINATE))
            put(Ui.Y, JsonSchemas.integer("y of a coordinate tap in device pixels (ui_gesture group)", minimum = 0, maximum = Ui.MAX_COORDINATE))
        }
    }

    private val PRETTY = Json { prettyPrint = true; prettyPrintIndent = "  " }
}
