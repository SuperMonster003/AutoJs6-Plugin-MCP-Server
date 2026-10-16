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
    const val SCRIPT_CATALOG = "script_catalog"
    const val SCRIPT_REGISTERED_RUN = "script_registered_run"
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
    const val FILES_LIST = "files_list"
    const val FILES_STAT = "files_stat"
    const val FILES_READ = "files_read"
    const val FILES_WRITE = "files_write"
    const val FILES_MKDIR = "files_mkdir"
    const val FILES_RENAME = "files_rename"
    const val FILES_DELETE = "files_delete"
    const val EDITOR_OPEN = "editor_open"
    const val APP_LAUNCH = "app_launch"
    const val APP_LIST = "app_list"
    const val CLIPBOARD_GET = "clipboard_get"
    const val CLIPBOARD_SET = "clipboard_set"
    const val DEVICE_ENSURE_ACCESSIBILITY = "device_ensure_accessibility"
    const val TOAST = "toast"
    const val SHELL_EXEC = "shell_exec"
    const val OCR_RECOGNIZE = "ocr_recognize"
    const val BARCODE_DETECT = "barcode_detect"
    const val OPENCV_FIND_IMAGE = "opencv_find_image"
    const val OPENCV_FIND_COLOR = "opencv_find_color"
    const val OPENCV_FIND_MULTI_COLORS = "opencv_find_multi_colors"
    const val PINYIN_CONVERT = "pinyin_convert"
    const val PINYIN4J_CONVERT = "pinyin4j_convert"
    const val MEDIAINFO_READ = "mediainfo_read"

    val FILE_NAMES = listOf(FILES_LIST, FILES_STAT, FILES_READ, FILES_WRITE, FILES_MKDIR, FILES_RENAME, FILES_DELETE)
    val DEVICE_ACTION_NAMES = listOf(EDITOR_OPEN, APP_LAUNCH, APP_LIST, CLIPBOARD_GET, CLIPBOARD_SET, DEVICE_ENSURE_ACCESSIBILITY, TOAST)

    object Files {
        const val MAX_BYTES = 1024 * 1024
        const val MAX_ENTRIES = 2000L
    }

    object Device {
        const val MAX_TEXT_BYTES = 64 * 1024
        const val ENSURE_TIMEOUT_MS = 15_000L
    }

    object Shell {
        const val DEFAULT_TIMEOUT_MS = 15_000L
        const val MAX_TIMEOUT_MS = 120_000L
        const val DEFAULT_OUTPUT_BYTES = 64 * 1024L
        const val MAX_OUTPUT_BYTES = 256 * 1024L
        const val MAX_COMMAND_BYTES = 16 * 1024
    }

    /** Argument keys for workspace, application, device, and shell tools (P3.4). */
    object Workspace {
        const val PATH = "path"
        const val RECURSIVE = "recursive"
        const val MAX_ENTRIES = "maxEntries"
        const val ENCODING = "encoding"
        const val MAX_BYTES = "maxBytes"
        const val CONTENT = "content"
        const val CREATE_DIRS = "createDirs"
        const val OVERWRITE = "overwrite"
        const val TO = "to"
        const val LINE = "line"
        const val COLUMN = "column"
        const val PACKAGE_NAME = "packageName"
        const val APP_NAME = "appName"
        const val QUERY = "query"
        const val TEXT = "text"
        const val CMD = "cmd"
        const val ROOT = "root"
        const val TIMEOUT_MS = "timeoutMs"
        const val MAX_OUTPUT_BYTES = "maxOutputBytes"
    }

    /** The `image` argument and host handle budgets every plugin-family tool shares (roadmap P11 D23 / D24 / D38, see ImageInputs). */
    object Images {
        const val IMAGE = "image"
        const val REGION = "region"
        const val TIMEOUT_MS = "timeoutMs"
        /** The `image` value that works on the phone screen instead of a file. */
        const val SCREEN = "screen"
        const val MIN_TIMEOUT_MS = 1_000L
        /** Capturing the screen into a host image handle; the host retries while the system throttles accessibility screenshots. */
        const val CAPTURE_TIMEOUT_MS = 15_000L
        /** Reading a file into a host image handle. */
        const val READ_TIMEOUT_MS = 30_000L
        /** Releasing a handle afterwards; a failure here is not reported. */
        const val RECYCLE_TIMEOUT_MS = 5_000L
        /** The last sentence of every tool description with an `image` argument (roadmap P11 D39, see RateLimiter.countsAsScreenshot). */
        const val SCREEN_QUOTA_NOTE = "Reading the screen counts against the limit of 30 screenshots per minute per client that screen_capture also uses."
        /** Roadmap P11 D26: what the read-only tools accept beyond the working directory, see PathAccess. */
        const val SHARED_STORAGE_NOTE = "While \"Allow reading shared storage\" is switched on in the MCP Server settings on the phone, absolute paths in " +
                "shared storage (such as /storage/emulated/0/DCIM/...) are accepted too, read-only; Android/data and Android/obb never are."
        /** Description of the `image` argument. */
        const val IMAGE_DESCRIPTION = "\"screen\" for the current screen, a POSIX path relative to the AutoJs6 working directory, or an absolute shared " +
                "storage path while reading shared storage is switched on; no traversal"
        /** Description of a path argument of a read-only tool. */
        const val READABLE_PATH_DESCRIPTION = "POSIX path relative to the AutoJs6 working directory, or an absolute shared storage path while reading " +
                "shared storage is switched on; no traversal"
    }

    /** `ocr_recognize` argument names and bounds (roadmap P11 D23 / D24 / D25 / D38), shared with OcrTools and the tests. */
    object Ocr {
        const val IMAGE = Images.IMAGE
        const val REGION = Images.REGION
        const val ENGINE = "engine"
        const val TIMEOUT_MS = Images.TIMEOUT_MS
        const val SCREEN = Images.SCREEN
        const val DEFAULT_TIMEOUT_MS = 60_000L
        const val MIN_TIMEOUT_MS = Images.MIN_TIMEOUT_MS
        const val READ_TIMEOUT_MS = Images.READ_TIMEOUT_MS
        const val RECYCLE_TIMEOUT_MS = Images.RECYCLE_TIMEOUT_MS
        val ENGINES = listOf("auto", "mlkit", "paddle", "rapid")
        /** Tool engine names to the host engine ids of the OCR plugins. */
        val HOST_ENGINES = mapOf("mlkit" to "mlkit-ocr", "paddle" to "paddle-ocr", "rapid" to "rapid-ocr")
    }

    /** `barcode_detect` argument names and bounds (roadmap P11 D23 / D24 / D38), shared with BarcodeTools and the tests. */
    object Barcode {
        const val FORMATS = "formats"
        const val MAX_RESULTS = "maxResults"
        const val DEFAULT_MAX_RESULTS = 10L
        /** The host's `barcode` module caps a detection at 128 codes. */
        const val MAX_RESULTS_LIMIT = 128L
        const val DEFAULT_TIMEOUT_MS = 30_000L
        /** The symbologies of the ML Kit Barcode plugin, as the host names them in results. */
        val FORMAT_NAMES = listOf("AZTEC", "CODABAR", "CODE_39", "CODE_93", "CODE_128", "DATA_MATRIX", "EAN_8", "EAN_13", "ITF", "PDF_417", "QR_CODE", "UPC_A", "UPC_E")
    }

    /** Argument names and bounds of the `opencv` tools (roadmap P11 D23 / D24 / D38), shared with OpenCvTools and the tests. */
    object OpenCv {
        const val TEMPLATE = "template"
        const val THRESHOLD = "threshold"
        const val MAX_RESULTS = "maxResults"
        const val COLOR = "color"
        const val POINTS = "points"
        const val DEFAULT_MATCH_THRESHOLD = 0.9
        const val DEFAULT_MAX_RESULTS = 5L
        const val MAX_RESULTS_LIMIT = 100L
        /** The host's default per-channel tolerance of `image.findColor` / `findMultiColors`. */
        const val DEFAULT_COLOR_THRESHOLD = 4L
        const val MAX_COLOR_THRESHOLD = 255L
        const val MAX_POINTS = 64
        /** Offsets of `opencv_find_multi_colors` lie within this distance of the first point, in either direction. */
        const val MAX_OFFSET = 65_535L
        const val DEFAULT_TIMEOUT_MS = 30_000L
    }

    /** `script_catalog` / `script_registered_run` argument names and bounds (roadmap P11 D42), shared with RegisteredScriptTools and the tests. */
    object Registered {
        const val QUERY = "query"
        const val LIMIT = "limit"
        const val ID = "id"
        const val PATH = "path"
        const val ARGUMENTS = "arguments"
        const val TIMEOUT_MS = "timeoutMs"
        const val DEFAULT_LIMIT = 100L
        /** The host's catalog cap. */
        const val MAX_LIMIT = 500L
        const val MAX_QUERY_BYTES = 256
        /** The host's bound on the serialized arguments (`AgentScriptExecutionState.MAX_PARAMETERS_BYTES`). */
        const val MAX_ARGUMENTS_BYTES = 16 * 1024
        /** A manifest without `timeoutMs` runs for this long (host default). */
        const val DEFAULT_SCRIPT_TIMEOUT_MS = 60_000L
        const val CATALOG_TIMEOUT_MS = 30_000L
        const val READ_TIMEOUT_MS = 15_000L
        /** On top of the script's own timeout: the host answers once the script stopped. */
        const val EXEC_GRACE_MS = 5_000L
    }

    /** `pinyin_convert` argument names and values (roadmap P11 D38), shared with PinyinTools and the tests. */
    object Pinyin {
        const val TEXT = "text"
        const val STYLE = "style"
        const val MODE = "mode"
        const val SEGMENT = "segment"
        const val HETERONYM = "heteronym"
        /** UTF-8 bytes of `text`; well inside the host's 32 KiB argument limit even after JSON escaping. */
        const val MAX_TEXT_BYTES = 8 * 1024
        const val DEFAULT_TIMEOUT_MS = 30_000L
        /** Tool style names; the host takes them upper case (`PinyinStyle`). */
        val STYLES = listOf("tone", "tone2", "to3ne", "normal", "initials", "first_letter")
        val MODES = listOf("normal", "surname", "place_name")
    }

    /** `pinyin4j_convert` argument names and values (roadmap P11 D38), shared with PinyinTools and the tests. */
    object Pinyin4j {
        const val TONE = "tone"
        const val CASE = "case"
        const val SEPARATOR = "separator"
        const val ROMANIZATION = "romanization"
        const val ALL_READINGS = "allReadings"
        const val MAX_SEPARATOR_LENGTH = 16
        val TONES = listOf("mark", "number", "none")
        val CASES = listOf("lower", "upper")
        val ROMANIZATIONS = listOf("hanyu", "tongyong", "wade_giles", "mps2", "yale", "gwoyeu")
        /** Tool romanization names to the host's `Pinyin4jOptions.normalizeRomanization` names. */
        val HOST_ROMANIZATIONS = mapOf(
            "hanyu" to "HANYU", "tongyong" to "TONGYONG", "wade_giles" to "WADE_GILES", "mps2" to "MPS2", "yale" to "YALE", "gwoyeu" to "GWOYEU",
        )
        val HOST_TONES = mapOf("mark" to "WITH_TONE_MARK", "number" to "WITH_TONE_NUMBER", "none" to "WITHOUT_TONE")
        val HOST_CASES = mapOf("lower" to "LOWERCASE", "upper" to "UPPERCASE")
    }

    /** `mediainfo_read` argument names and bounds (roadmap P11 D38), shared with MediaInfoTools and the tests. */
    object MediaInfo {
        const val PATH = "path"
        const val REPORT = "report"
        /** The MediaInfo plugin gives up a parse after 30 s; the file transfer comes on top. */
        const val DEFAULT_TIMEOUT_MS = 60_000L
        /** Characters of the text report kept in a result. */
        const val MAX_REPORT_CHARS = 64 * 1024
    }

    /** Screenshot options shared by the catalog and ScreenTools (P3.3). */
    object Screen {
        const val SCALE = "scale"
        const val MAX_WIDTH = "maxWidth"
        const val FORMAT = "format"
        const val QUALITY = "quality"
        const val REGION = "region"
        const val ANNOTATE = "annotate"
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
        const val SNAPSHOT_ID = "snapshotId"
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

    // Output schemas (roadmap P9) for the tools whose structured result is stable; see JsonSchemas.outputSchema.

    private val deviceInfoOutput: JsonObject = JsonSchemas.outputSchema(
        linkedMapOf(
            "schema" to JsonSchemas.string("Result schema id, autojs6-bridge-device-info-v1"),
            "build" to JsonSchemas.openObject("Android build: brand, manufacturer, model, sdkInt, release, abis and more"),
            "screen" to JsonSchemas.openObject("Screen size in pixels (following rotation), density, rotation, brightness"),
            "battery" to JsonSchemas.openObject("Battery percent, charging state and temperature, when available"),
            "memory" to JsonSchemas.openObject("Total and available memory in bytes"),
            "host" to JsonSchemas.openObject("AutoJs6 package, version and process"),
            "accessibility" to JsonSchemas.openObject("Whether the AutoJs6 accessibility service is running and operational"),
            "screenOn" to JsonSchemas.boolean("Whether the screen is on"),
            "locale" to JsonSchemas.string("Device locale tag"),
            "timeZone" to JsonSchemas.string("Device time zone id"),
            "uptimeMs" to JsonSchemas.integer("Milliseconds since boot"),
            "now" to JsonSchemas.integer("Device time, milliseconds since the epoch"),
            "plugins" to JsonSchemas.openObject(
                "Plugin families behind the plugin-family tools (roadmap P11): families[] with id, status (available, not_installed, disabled, " +
                        "incompatible), the tools each brings, its tool group switch, the plugins and a hint; absent when AutoJs6 cannot report them",
            ),
            "pluginTools" to JsonSchemas.openObject(
                "Plugins that describe their own tools (roadmap P12): plugins[] with shortName, packageName, trust, status, the group switches, " +
                        "tools[] with name, risk and whether it is listed, and a hint; absent when AutoJs6 offers no plugin tools",
            ),
        ),
    )

    private val screenStateOutput: JsonObject = JsonSchemas.outputSchema(
        linkedMapOf(
            "screenOn" to JsonSchemas.boolean("Whether the screen is on"),
            "width" to JsonSchemas.integer("Current width in pixels, following rotation"),
            "height" to JsonSchemas.integer("Current height in pixels, following rotation"),
            "orientation" to JsonSchemas.string("Derived from width and height", enum = listOf("portrait", "landscape")),
            "rotation" to JsonSchemas.integer("Display rotation constant (0 to 3)"),
            "rotationDegrees" to JsonSchemas.integer("Display rotation in degrees"),
            "density" to JsonSchemas.number("Logical density factor"),
            "densityDpi" to JsonSchemas.integer("Density in dots per inch"),
        ),
        required = listOf("screenOn", "width", "height", "orientation"),
    )

    private val scriptRunOutput: JsonObject = JsonSchemas.outputSchema(
        linkedMapOf(
            "executionId" to JsonSchemas.integer("Id for script_stop, script_list and console_tail"),
            "name" to JsonSchemas.string("Script name in the AutoJs6 task list"),
            "engine" to JsonSchemas.string("Engine that runs the script"),
            "workingDirectory" to JsonSchemas.string("Working directory of the script"),
            "status" to JsonSchemas.string(
                "finished, error (the script threw; see exception) or running (still running after the wait, or not waited for)",
                enum = listOf(ScriptRunTool.STATUS_FINISHED, ScriptRunTool.STATUS_ERROR, ScriptRunTool.STATUS_RUNNING),
            ),
            "waited" to JsonSchemas.boolean("Whether the call waited for the script"),
            "waitMs" to JsonSchemas.integer("How long the call was willing to wait"),
            "durationMs" to JsonSchemas.integer("How long the call waited"),
            "exception" to JsonSchemas.openObject("For status error: message and, when known, line"),
            "console" to JsonSchemas.array("Newest console lines of the script", JsonSchemas.openObject("id, level, levelName, time and text")),
            "consoleCount" to JsonSchemas.integer("Console lines the script printed"),
            "consoleTruncated" to JsonSchemas.boolean("Whether older console lines were left out"),
            "hint" to JsonSchemas.string("For status running: how to stop or follow the script"),
        ),
        required = listOf("status"),
    )

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
                "(schema autojs6-bridge-device-info-v1). No hardware identifiers. With AutoJs6 6.8.0 or later, plugins lists the plugin " +
                "families (OCR, barcode, OpenCV, Pinyin, Pinyin4j, MediaInfo, offline docs) as available, not_installed, disabled or incompatible, " +
                "the tools each brings and what to do in the AutoJs6 plugin center, so you can tell the user which plugin to install.",
        inputSchema = JsonSchemas.objectSchema(),
        group = ToolGroup.DEVICE,
        outputSchema = deviceInfoOutput,
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
        outputSchema = scriptRunOutput,
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
        outputSchema = scriptRunOutput,
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


    private const val NODE_REF_DESCRIPTION = "A #n reference from ui_dump; AutoJs6 finds the node again in the current window before acting, and " +
            "NODE_REF_STALE asks for a new ui_dump when the window changed or the node moved, changed, or is gone"
    private const val SNAPSHOT_ID_DESCRIPTION = "The snapshotId of the ui_dump the nodeRef comes from; defaults to the newest dump. AutoJs6 keeps the " +
            "newest 8 dumps"
    private const val TARGET_NOTE = " Give nodeRef or selector, not both."

    val uiDump: ToolSpec = ToolSpec(
        name = UI_DUMP,
        title = "Dump UI tree",
        description = "Dumps the accessibility node tree of the active window as compact text: one node per line with a #n reference, " +
                "an indent per depth, the short class name, the state markers that apply (clickable, long_clickable, checkable, checked, " +
                "scrollable, editable, focused, selected, !enabled, hidden), the text in quotes, desc=, id= (the full resource id), and " +
                "the position (bounds [l,t][r,b] for a node with children, c=(x,y) for a leaf). Pass a #n reference as nodeRef to " +
                "ui_click, ui_long_click, ui_set_text, or ui_scroll: AutoJs6 keeps the newest 8 dumps (snapshotId) and finds the node " +
                "again when acting, so a reference works until the window changes or the node moves or changes. Call it before acting " +
                "and again after the screen changed. format json returns the nodes as objects with every flag and format xml the " +
                "uiautomator-style export; neither has references.",
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
                "timeoutMs for the first match, and returns up to limit of them with bounds, center, and a selector that pins each node " +
                "(pass it as selector to the action tools). An empty count is not an error; ui_explain_selector tells which condition fails.",
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
                "for up to timeoutMs, and answers TIMEOUT when the state is not reached; an appeared node comes with a selector that " +
                "pins it. Use it after an action that opens a screen or dismisses a dialog.",
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
                "adds to the current text instead of replacing it and needs a selector. Works without focus or the keyboard; " +
                "ACTION_FAILED means the node is not editable or not enabled." + TARGET_NOTE,
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
                "Images above the 4 MiB base64 limit are retried at lower quality or smaller dimensions; metadata reports adjustments. " +
                "With annotate the #n references of the newest ui_dump are drawn on the image. " +
                "At most 30 captures per minute per client; a RATE_LIMITED result names the wait in retryAfterMs.",
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
            Screen.ANNOTATE to JsonSchemas.boolean(
                "Draw the #n references of the newest ui_dump on the image (a box and a label per node, a small square at the centre of " +
                        "a leaf) so they can be matched to what is visible; the result's annotation field reports what was drawn",
                default = false,
            ),
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
        outputSchema = screenStateOutput,
        bridge = BridgeMethod("device", "isScreenOn"),
        permissions = listOf("device"),
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    private fun pathSchema() = JsonSchemas.string("POSIX path relative to the AutoJs6 working directory; no absolute paths or traversal", maxLength = WorkspacePath.MAX_BYTES)
    private fun readablePathSchema() = JsonSchemas.string(Images.READABLE_PATH_DESCRIPTION, maxLength = WorkspacePath.MAX_BYTES)

    val filesList = fileSpec(FILES_LIST, "List files", "Lists workspace files with metadata. Results are bounded and report truncation. " +
        Images.SHARED_STORAGE_NOTE + " A shared storage listing answers absolute paths and its root.", "list",
        linkedMapOf(Workspace.PATH to readablePathSchema(), Workspace.RECURSIVE to JsonSchemas.boolean("Include subdirectories", default = false),
            Workspace.MAX_ENTRIES to JsonSchemas.integer("Maximum entries", minimum = 1, maximum = Files.MAX_ENTRIES, default = 500)), emptyList())
    val filesStat = fileSpec(FILES_STAT, "File metadata", "Returns existence, type, size, and modification time of a workspace path. " +
        Images.SHARED_STORAGE_NOTE, "stat", linkedMapOf(Workspace.PATH to readablePathSchema()))
    val filesRead = fileSpec(FILES_READ, "Read file", "Reads up to 1 MiB. Use encoding base64 for binary data; encoding, bytes, totalBytes, and truncated identify the representation and limit.", "read",
        linkedMapOf(Workspace.PATH to pathSchema(), Workspace.ENCODING to JsonSchemas.string("Content encoding", enum = listOf("utf-8", "base64"), default = "utf-8"),
            Workspace.MAX_BYTES to JsonSchemas.integer("Maximum raw file bytes", minimum = 1, maximum = Files.MAX_BYTES.toLong(), default = Files.MAX_BYTES.toLong())))
    val filesWrite = fileSpec(FILES_WRITE, "Write file", "Writes UTF-8 text and refreshes the host explorer. Content is limited to 1 MiB and the negotiated Binder request budget (normally 96 KiB including JSON escaping); oversized calls fail before writing.", "write",
        linkedMapOf(Workspace.PATH to pathSchema(), Workspace.CONTENT to JsonSchemas.string("UTF-8 text", maxLength = Files.MAX_BYTES),
            Workspace.CREATE_DIRS to JsonSchemas.boolean("Create missing parents", default = true), Workspace.OVERWRITE to JsonSchemas.boolean("Replace an existing file", default = true)),
        listOf(Workspace.PATH, Workspace.CONTENT), writes = true)
    val filesMkdir = fileSpec(FILES_MKDIR, "Create directory", "Creates a workspace directory and missing parents, then refreshes the host explorer.", "mkdir", writes = true)
    val filesRename = fileSpec(FILES_RENAME, "Rename file", "Moves a workspace file or directory to another workspace path and refreshes the host explorer.", "rename",
        linkedMapOf(Workspace.PATH to pathSchema(), Workspace.TO to pathSchema(), Workspace.OVERWRITE to JsonSchemas.boolean("Replace an existing destination", default = false)), listOf(Workspace.PATH, Workspace.TO), writes = true)
    val filesDelete = fileSpec(FILES_DELETE, "Delete file", "Deletes a workspace entry. The separate files_delete group is off by default. The workspace root cannot be deleted.", "delete",
        linkedMapOf(Workspace.PATH to pathSchema(), Workspace.RECURSIVE to JsonSchemas.boolean("Delete a directory and its contents", default = false)), writes = true, deletes = true)

    val editorOpen = ToolSpec(EDITOR_OPEN, "Open editor", "Opens a workspace file in the AutoJs6 editor at a one-based line and column. Lines outside the file are ignored by the editor.",
        JsonSchemas.objectSchema(linkedMapOf(Workspace.PATH to pathSchema(),
            Workspace.LINE to JsonSchemas.integer("One-based line", minimum = 1, maximum = 1_000_000, default = 1),
            Workspace.COLUMN to JsonSchemas.integer("One-based column", minimum = 1, maximum = 1_000_000, default = 1)), listOf(Workspace.PATH)),
        ToolGroup.FILES, bridge = BridgeMethod("app", "editFile"), permissions = listOf("app.activity"))
    val appLaunch = ToolSpec(APP_LAUNCH, "Launch application", "Opens an installed Android application. Provide exactly one of packageName or appName.",
        JsonSchemas.objectSchema(linkedMapOf(Workspace.PACKAGE_NAME to JsonSchemas.string("Android package name", maxLength = 512),
            Workspace.APP_NAME to JsonSchemas.string("Application label", maxLength = 512))),
        ToolGroup.DEVICE, bridge = BridgeMethod("app", "launchPackage"), permissions = listOf("app.launch"), hints = ToolHints(openWorld = true))
    val appList = ToolSpec(APP_LIST, "List applications", "Lists up to 1000 Android applications visible to AutoJs6, optionally matching a package name or label. Android package visibility restrictions apply.",
        JsonSchemas.objectSchema(linkedMapOf(Workspace.QUERY to JsonSchemas.string("Case-insensitive package or label substring", maxLength = 512, default = ""))),
        ToolGroup.DEVICE, bridge = BridgeMethod("package_manager", "listApps"), permissions = listOf("package_manager"), hints = ToolHints(readOnly = true, idempotent = true))
    val clipboardGet = ToolSpec(CLIPBOARD_GET, "Read clipboard", "Reads clipboard text (up to 64 KiB). Android may restrict clipboard access while AutoJs6 is in the background.",
        JsonSchemas.objectSchema(), ToolGroup.DEVICE, bridge = BridgeMethod("clipboard", "getText"), permissions = listOf("clipboard"), hints = ToolHints(readOnly = true, idempotent = true))
    val clipboardSet = ToolSpec(CLIPBOARD_SET, "Write clipboard", "Replaces clipboard text, including an empty string to clear it.",
        JsonSchemas.objectSchema(linkedMapOf(Workspace.TEXT to JsonSchemas.string("Clipboard text", maxLength = Device.MAX_TEXT_BYTES)), listOf(Workspace.TEXT)),
        ToolGroup.DEVICE, bridge = BridgeMethod("clipboard", "setText"), permissions = listOf("clipboard"))
    val deviceEnsureAccessibility = ToolSpec(DEVICE_ENSURE_ACCESSIBILITY, "Enable accessibility", "Asks AutoJs6 to enable its accessibility service using its configured secure-settings, root, or Shizuku strategy. Waits up to 10 s for an operational service; failure includes manual activation guidance.",
        JsonSchemas.objectSchema(), ToolGroup.DEVICE, bridge = BridgeMethod("accessibility", "ensureEnabled"), permissions = listOf("accessibility"), timeoutMs = Device.ENSURE_TIMEOUT_MS)
    val toast = ToolSpec(TOAST, "Show toast", "Shows a short Android toast on the phone.",
        JsonSchemas.objectSchema(linkedMapOf(Workspace.TEXT to JsonSchemas.string("Toast text", maxLength = 4096)), listOf(Workspace.TEXT)),
        ToolGroup.DEVICE, bridge = BridgeMethod("toast", "toast"), permissions = listOf("toast"))
    val shellExec = ToolSpec(SHELL_EXEC, "Execute shell command", "Runs an Android shell command in the host workspace. The shell group is off by default; root also requires the separate allow root switch and a shell.root host grant. Reports exit code, timeout, stdout, stderr, and truncation. maxOutputBytes bounds stdout and stderr together.",
        JsonSchemas.objectSchema(linkedMapOf(Workspace.CMD to JsonSchemas.string("Shell command", maxLength = Shell.MAX_COMMAND_BYTES),
            Workspace.ROOT to JsonSchemas.boolean("Request root privileges", default = false),
            Workspace.TIMEOUT_MS to JsonSchemas.integer("Process timeout in milliseconds", minimum = 1, maximum = Shell.MAX_TIMEOUT_MS, default = Shell.DEFAULT_TIMEOUT_MS),
            Workspace.MAX_OUTPUT_BYTES to JsonSchemas.integer("Combined UTF-8 output byte limit", minimum = 1, maximum = Shell.MAX_OUTPUT_BYTES, default = Shell.DEFAULT_OUTPUT_BYTES)), listOf(Workspace.CMD)),
        ToolGroup.SHELL, bridge = BridgeMethod("shell", "exec"), permissions = listOf("shell"), timeoutMs = Shell.DEFAULT_TIMEOUT_MS + 2_000,
        maxTimeoutMs = Shell.MAX_TIMEOUT_MS + 2_000, hints = ToolHints(destructive = true, openWorld = true))

    val ocrRecognize = ToolSpec(
        name = OCR_RECOGNIZE,
        title = "Recognize text (OCR)",
        description = "Recognize text on the phone screen or in an image file on the phone with an installed AutoJs6 OCR plugin " +
                "(ML Kit, Paddle OCR or Rapid OCR). image is \"screen\" for the current screen, or a POSIX path relative to the AutoJs6 working " +
                "directory for an image file. region crops before recognition, in screen pixels or file pixels. engine picks the OCR plugin; auto " +
                "takes the first enabled one in ML Kit, Paddle, Rapid order. Returns the recognized lines as text and, as structured content, " +
                "items with text, confidence and bounds (file bounds are in the file's own pixels even when AutoJs6 downscaled the image to read it). " +
                "The screenshot is taken and recognized inside AutoJs6; no image leaves the phone. A first Paddle call loads its model and can take " +
                "tens of seconds; timeoutMs defaults to 60 s. This tool exists only while an OCR plugin is installed and enabled in AutoJs6. " + Images.SHARED_STORAGE_NOTE + " " + Images.SCREEN_QUOTA_NOTE,
        group = ToolGroup.OCR,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Ocr.IMAGE to JsonSchemas.string(Images.IMAGE_DESCRIPTION, maxLength = WorkspacePath.MAX_BYTES),
                Ocr.REGION to JsonSchemas.bounds("Crop before recognition, in screen pixels for the screen or file pixels for a file; right and bottom are exclusive"),
                Ocr.ENGINE to JsonSchemas.string("OCR plugin to use; auto picks the first enabled one in ML Kit, Paddle, Rapid order", default = "auto", enum = Ocr.ENGINES),
                Ocr.TIMEOUT_MS to JsonSchemas.integer("How long to wait for the recognition, in milliseconds", minimum = Ocr.MIN_TIMEOUT_MS, maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS, default = Ocr.DEFAULT_TIMEOUT_MS),
            ),
            required = listOf(Ocr.IMAGE),
        ),
        bridge = BridgeMethod("ocr", "recognize"),
        permissions = listOf("ocr", "image", "accessibility", "screen_capture"),
        timeoutMs = Ocr.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = false, openWorld = true),
        hostMethods = listOf(
            BridgeMethod("accessibility", "readScreenText"), BridgeMethod("media_projection", "requestScreenCapture"),
            BridgeMethod("image", "readImage"), BridgeMethod("ocr", "recognize"), BridgeMethod("image", "recycle"),
        ),
    )

    val barcodeDetect = ToolSpec(
        name = BARCODE_DETECT,
        title = "Detect barcodes and QR codes",
        description = "Detect barcodes and QR codes on the phone screen or in an image file on the phone with the AutoJs6 ML Kit Barcode plugin. " +
                "image is \"screen\" for the current screen, or a POSIX path relative to the AutoJs6 working directory for an image file. " +
                "formats limits the symbologies (all by default); region crops before detection, in screen pixels or file pixels. Returns one line " +
                "per code (format and raw value) and, as structured content, items with rawValue, displayValue, format, valueType, bounds and " +
                "cornerPoints (file coordinates are in the file's own pixels even when AutoJs6 downscaled the image to read it). The screenshot " +
                "stays inside AutoJs6; no image leaves the phone. This tool exists only while the barcode plugin is installed and enabled in AutoJs6. " + Images.SHARED_STORAGE_NOTE + " " + Images.SCREEN_QUOTA_NOTE,
        group = ToolGroup.BARCODE,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Images.IMAGE to JsonSchemas.string(Images.IMAGE_DESCRIPTION, maxLength = WorkspacePath.MAX_BYTES),
                Barcode.FORMATS to JsonSchemas.array(
                    "Only detect these symbologies; omit for all",
                    JsonSchemas.string("Barcode symbology", enum = Barcode.FORMAT_NAMES),
                    minItems = 1,
                    maxItems = Barcode.FORMAT_NAMES.size,
                ),
                Images.REGION to JsonSchemas.bounds("Crop before detection, in screen pixels for the screen or file pixels for a file; right and bottom are exclusive"),
                Barcode.MAX_RESULTS to JsonSchemas.integer("Report at most this many codes", minimum = 1, maximum = Barcode.MAX_RESULTS_LIMIT, default = Barcode.DEFAULT_MAX_RESULTS),
                Images.TIMEOUT_MS to JsonSchemas.integer("How long to wait for the detection, in milliseconds", minimum = Images.MIN_TIMEOUT_MS, maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS, default = Barcode.DEFAULT_TIMEOUT_MS),
            ),
            required = listOf(Images.IMAGE),
        ),
        bridge = BridgeMethod("barcode", "detectAll"),
        permissions = listOf("barcode", "image", "accessibility", "screen_capture"),
        timeoutMs = Barcode.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = false, openWorld = true),
        hostMethods = imageHostMethods(BridgeMethod("barcode", "detectAll")),
    )

    val opencvFindImage = ToolSpec(
        name = OPENCV_FIND_IMAGE,
        title = "Find an image (template matching)",
        description = "Find where a smaller template image appears on the phone screen or in an image file on the phone (OpenCV template " +
                "matching through the AutoJs6 OpenCV plugin). image is \"screen\" for the current screen, or a POSIX path relative to the AutoJs6 " +
                "working directory; template is the path of the image to look for, at the same pixel scale as image (for the screen, cut from a " +
                "full-resolution screenshot). threshold is the lowest similarity accepted (0..1); region limits the search, in screen pixels or file " +
                "pixels; maxResults caps the matches, best first and without overlaps. Returns bounds, center and score of each match; screen " +
                "coordinates are device pixels. This tool exists only while the OpenCV plugin is installed and enabled in AutoJs6. " + Images.SHARED_STORAGE_NOTE + " " + Images.SCREEN_QUOTA_NOTE,
        group = ToolGroup.OPENCV,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Images.IMAGE to JsonSchemas.string(Images.IMAGE_DESCRIPTION, maxLength = WorkspacePath.MAX_BYTES),
                OpenCv.TEMPLATE to JsonSchemas.string("The template image: ${Images.READABLE_PATH_DESCRIPTION}", maxLength = WorkspacePath.MAX_BYTES),
                OpenCv.THRESHOLD to JsonSchemas.number("Lowest similarity a match needs, from 0 to 1", minimum = 0.0, maximum = 1.0, default = OpenCv.DEFAULT_MATCH_THRESHOLD),
                Images.REGION to JsonSchemas.bounds("Search only inside this rectangle, in screen pixels or file pixels; right and bottom are exclusive"),
                OpenCv.MAX_RESULTS to JsonSchemas.integer("Report at most this many matches, best first", minimum = 1, maximum = OpenCv.MAX_RESULTS_LIMIT, default = OpenCv.DEFAULT_MAX_RESULTS),
                Images.TIMEOUT_MS to JsonSchemas.integer("How long to wait for the search, in milliseconds", minimum = Images.MIN_TIMEOUT_MS, maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS, default = OpenCv.DEFAULT_TIMEOUT_MS),
            ),
            required = listOf(Images.IMAGE, OpenCv.TEMPLATE),
        ),
        bridge = BridgeMethod("image", "matchTemplate"),
        permissions = listOf("image", "accessibility", "screen_capture"),
        timeoutMs = OpenCv.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = false, openWorld = true),
        hostMethods = imageHostMethods(BridgeMethod("image", "matchTemplate")),
    )

    val opencvFindColor = ToolSpec(
        name = OPENCV_FIND_COLOR,
        title = "Find a color",
        description = "Find the first pixel of a color on the phone screen or in an image file on the phone (AutoJs6 OpenCV plugin). image is " +
                "\"screen\" for the current screen, or a POSIX path relative to the AutoJs6 working directory. color is #RRGGBB or #RGB; threshold " +
                "is the difference allowed in each of the red, green and blue channels; region limits the search, in screen pixels or file pixels. " +
                "Pixels are searched row by row from the top left. Returns found and the point; screen coordinates are device pixels. This tool " +
                "exists only while the OpenCV plugin is installed and enabled in AutoJs6. " + Images.SHARED_STORAGE_NOTE + " " + Images.SCREEN_QUOTA_NOTE,
        group = ToolGroup.OPENCV,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Images.IMAGE to JsonSchemas.string(Images.IMAGE_DESCRIPTION, maxLength = WorkspacePath.MAX_BYTES),
                OpenCv.COLOR to JsonSchemas.string("The color to find, #RRGGBB or #RGB", maxLength = 7),
                OpenCv.THRESHOLD to JsonSchemas.integer("Difference allowed in each RGB channel, from 0 (exact) to 255", minimum = 0, maximum = OpenCv.MAX_COLOR_THRESHOLD, default = OpenCv.DEFAULT_COLOR_THRESHOLD),
                Images.REGION to JsonSchemas.bounds("Search only inside this rectangle, in screen pixels or file pixels; right and bottom are exclusive"),
                Images.TIMEOUT_MS to JsonSchemas.integer("How long to wait for the search, in milliseconds", minimum = Images.MIN_TIMEOUT_MS, maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS, default = OpenCv.DEFAULT_TIMEOUT_MS),
            ),
            required = listOf(Images.IMAGE, OpenCv.COLOR),
        ),
        bridge = BridgeMethod("image", "findColor"),
        permissions = listOf("image", "accessibility", "screen_capture"),
        timeoutMs = OpenCv.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = false, openWorld = true),
        hostMethods = imageHostMethods(BridgeMethod("image", "findColor")),
    )

    val opencvFindMultiColors = ToolSpec(
        name = OPENCV_FIND_MULTI_COLORS,
        title = "Find a multi-point color pattern",
        description = "Find the first point whose color matches and whose neighbors at given offsets match their colors too (multi-point color " +
                "search, AutoJs6 OpenCV plugin), on the phone screen or in an image file on the phone. image is \"screen\" for the current screen, " +
                "or a POSIX path relative to the AutoJs6 working directory. color is the color of the point itself (#RRGGBB or #RGB); points lists " +
                "the neighbors as {x, y, color}, with x and y relative to that point in the pixels of image. threshold is the difference allowed in " +
                "each RGB channel for color and the average difference allowed for the neighbors; region limits where the point may lie. Returns " +
                "found and the point; screen coordinates are device pixels. This tool exists only while the OpenCV plugin is installed and enabled in AutoJs6. " + Images.SHARED_STORAGE_NOTE + " " + Images.SCREEN_QUOTA_NOTE,
        group = ToolGroup.OPENCV,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Images.IMAGE to JsonSchemas.string(Images.IMAGE_DESCRIPTION, maxLength = WorkspacePath.MAX_BYTES),
                OpenCv.COLOR to JsonSchemas.string("Color of the point itself, #RRGGBB or #RGB", maxLength = 7),
                OpenCv.POINTS to JsonSchemas.array(
                    "Neighbors that must match too, relative to the point",
                    JsonSchemas.objectSchema(
                        linkedMapOf(
                            "x" to JsonSchemas.integer("Horizontal offset from the point; negative is to the left", minimum = -OpenCv.MAX_OFFSET, maximum = OpenCv.MAX_OFFSET),
                            "y" to JsonSchemas.integer("Vertical offset from the point; negative is above", minimum = -OpenCv.MAX_OFFSET, maximum = OpenCv.MAX_OFFSET),
                            "color" to JsonSchemas.string("Color at that offset, #RRGGBB or #RGB", maxLength = 7),
                        ),
                        required = listOf("x", "y", "color"),
                    ),
                    minItems = 1,
                    maxItems = OpenCv.MAX_POINTS,
                ),
                OpenCv.THRESHOLD to JsonSchemas.integer("Allowed color difference, from 0 (exact) to 255", minimum = 0, maximum = OpenCv.MAX_COLOR_THRESHOLD, default = OpenCv.DEFAULT_COLOR_THRESHOLD),
                Images.REGION to JsonSchemas.bounds("Only points inside this rectangle, in screen pixels or file pixels; right and bottom are exclusive"),
                Images.TIMEOUT_MS to JsonSchemas.integer("How long to wait for the search, in milliseconds", minimum = Images.MIN_TIMEOUT_MS, maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS, default = OpenCv.DEFAULT_TIMEOUT_MS),
            ),
            required = listOf(Images.IMAGE, OpenCv.COLOR, OpenCv.POINTS),
        ),
        bridge = BridgeMethod("image", "findMultiColors"),
        permissions = listOf("image", "accessibility", "screen_capture"),
        timeoutMs = OpenCv.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = false, openWorld = true),
        hostMethods = imageHostMethods(BridgeMethod("image", "findMultiColors")),
    )

    val pinyinConvert = ToolSpec(
        name = PINYIN_CONVERT,
        title = "Convert Chinese to pinyin (Pinyin plugin)",
        description = "Convert Chinese text to Hanyu Pinyin with the AutoJs6 Pinyin plugin, which reads whole words from a phrase dictionary, so a " +
                "character with several readings usually gets the one its word needs (\"银行\" -> yín háng); segment (on by default) splits longer text " +
                "into words before the lookup. Readings come from the dictionary, so names and rare words can differ from usage. style picks the " +
                "output (tone marks by default, tone2 / to3ne with digits, normal without tones, initials, first_letter); mode surname or place_name " +
                "uses the readings of names; heteronym lists every reading of each character. Returns the pinyin as text, readings joined by spaces and " +
                "alternatives by |, and the rows as structured content. For other romanizations (Wade-Giles, Yale, ...) or the readings of single " +
                "characters use pinyin4j_convert. This tool exists only while the Pinyin plugin is installed and enabled in AutoJs6.",
        group = ToolGroup.PINYIN,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Pinyin.TEXT to JsonSchemas.string("Text to convert; characters that are not Chinese stay as they are", maxLength = Pinyin.MAX_TEXT_BYTES),
                Pinyin.STYLE to JsonSchemas.string("Output style: tone (zhōng), tone2 (zho1ng), to3ne (zhong1), normal (zhong), initials (zh), first_letter (z)", default = "tone", enum = Pinyin.STYLES),
                Pinyin.MODE to JsonSchemas.string("normal, surname for personal names, or place_name for place names", default = "normal", enum = Pinyin.MODES),
                Pinyin.SEGMENT to JsonSchemas.boolean("Split the text into words before the dictionary lookup; helps longer sentences", default = true),
                Pinyin.HETERONYM to JsonSchemas.boolean("List every reading of each character instead of the most likely one", default = false),
            ),
            required = listOf(Pinyin.TEXT),
        ),
        bridge = BridgeMethod("pinyin", "convert"),
        permissions = listOf("pinyin"),
        timeoutMs = Pinyin.DEFAULT_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val pinyin4jConvert = ToolSpec(
        name = PINYIN4J_CONVERT,
        title = "Romanize Chinese characters (Pinyin4j plugin)",
        description = "Romanize Chinese text character by character with the AutoJs6 Pinyin4j plugin. Unlike pinyin_convert it looks at each " +
                "character alone, without word context, so a character with several readings gets its first dictionary reading; in return it offers " +
                "other romanizations (romanization: hanyu, tongyong, wade_giles, mps2, yale, gwoyeu) and, with allReadings, every reading of each character. " +
                "For hanyu, tone chooses tone marks (default), tone numbers or none, and case lower or upper; other romanizations do not take tone or case. " +
                "Returns the readings joined by separator (a space by default) as text, and one row per character as structured content. This tool " +
                "exists only while the Pinyin4j plugin is installed and enabled in AutoJs6.",
        group = ToolGroup.PINYIN4J,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Pinyin.TEXT to JsonSchemas.string("Text to romanize; characters that are not Chinese stay as they are", maxLength = Pinyin.MAX_TEXT_BYTES),
                Pinyin4j.ROMANIZATION to JsonSchemas.string("Romanization system", default = "hanyu", enum = Pinyin4j.ROMANIZATIONS),
                Pinyin4j.TONE to JsonSchemas.string("Hanyu only: tone marks (zhōng), tone numbers (zhong1) or none (zhong)", default = "mark", enum = Pinyin4j.TONES),
                Pinyin4j.CASE to JsonSchemas.string("Hanyu only: lower or upper case", default = "lower", enum = Pinyin4j.CASES),
                Pinyin4j.SEPARATOR to JsonSchemas.string("Placed between the characters of the text result", maxLength = Pinyin4j.MAX_SEPARATOR_LENGTH, default = " "),
                Pinyin4j.ALL_READINGS to JsonSchemas.boolean("List every reading of each character (joined by | in the text) instead of the first one", default = false),
            ),
            required = listOf(Pinyin.TEXT),
        ),
        bridge = BridgeMethod("pinyin4j", "readings"),
        permissions = listOf("pinyin4j"),
        timeoutMs = Pinyin.DEFAULT_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val mediainfoRead = ToolSpec(
        name = MEDIAINFO_READ,
        title = "Read media file information (MediaInfo)",
        description = "Read the technical details and tags of an audio, video or image file on the phone with the AutoJs6 MediaInfo plugin: container " +
                "format, duration, bit rates, codecs, resolution, frame rate, channels, sampling rate, title, artist and more. path is a POSIX path " +
                "relative to the AutoJs6 working directory. Returns one summary line per track as text and every field of every track (general, video, " +
                "audio, text, image, menu, other) as structured content; report adds the full MediaInfo text report. Values are MediaInfo's display " +
                "strings (\"5 min 3 s\", \"1 920 pixels\"). This tool exists only while the MediaInfo plugin is installed and enabled in AutoJs6. " + Images.SHARED_STORAGE_NOTE,
        group = ToolGroup.MEDIAINFO,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                MediaInfo.PATH to readablePathSchema(),
                MediaInfo.REPORT to JsonSchemas.boolean("Also return MediaInfo's full text report (up to 64 Ki characters)", default = false),
                Images.TIMEOUT_MS to JsonSchemas.integer("How long to wait for the analysis, in milliseconds", minimum = Images.MIN_TIMEOUT_MS, maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS, default = MediaInfo.DEFAULT_TIMEOUT_MS),
            ),
            required = listOf(MediaInfo.PATH),
        ),
        bridge = BridgeMethod("mediainfo", "read"),
        permissions = listOf("media", "media.metadata"),
        timeoutMs = MediaInfo.DEFAULT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = true),
    )

    val scriptCatalog = ToolSpec(
        name = SCRIPT_CATALOG,
        title = "List registered scripts",
        description = "Lists the scripts the user registered for models in the AutoJs6 working directory (a script with an @agent JSDoc header, " +
                "or a project with an agent object in project.json): id, path, description, parameter schema, the result it reports, risk " +
                "(readonly, normal, sensitive), confirm (never, before-run) and timeout. query filters by id, description, tags and examples. Run one " +
                "with script_registered_run; before running a script whose confirm is before-run or whose risk is sensitive, ask the user. " +
                "Needs AutoJs6 6.8.0 or later.",
        group = ToolGroup.SCRIPT,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Registered.QUERY to JsonSchemas.string("Case-insensitive text to look for in ids, descriptions, tags and examples", maxLength = Registered.MAX_QUERY_BYTES),
                Registered.LIMIT to JsonSchemas.integer("Return at most this many scripts", minimum = 1, maximum = Registered.MAX_LIMIT, default = Registered.DEFAULT_LIMIT),
            ),
        ),
        bridge = BridgeMethod("agent", "listScripts"),
        permissions = listOf("agent"),
        timeoutMs = Registered.CATALOG_TIMEOUT_MS,
        hints = ToolHints(readOnly = true, idempotent = true),
        gatedByGrant = true,
    )

    val scriptRegisteredRun = ToolSpec(
        name = SCRIPT_REGISTERED_RUN,
        title = "Run a registered script",
        description = "Runs a script from script_catalog with arguments that AutoJs6 checks against the script's registered parameters " +
                "(defaults filled in, unknown names refused), waits for it and stops it after its registered timeout (timeoutMs may only " +
                "shorten it). Give id, or path when several scripts share an id. Returns the status (finished, error, stopped, timeout), " +
                "the value the script reported with ai.agent.result, and the newest console lines. A registered script can act on the phone " +
                "like script_run; ask the user first when its catalog entry says confirm before-run or risk sensitive. Needs AutoJs6 6.8.0 or later.",
        group = ToolGroup.SCRIPT,
        inputSchema = JsonSchemas.objectSchema(
            linkedMapOf(
                Registered.ID to JsonSchemas.string("Registered id from script_catalog; give id or path, not both", maxLength = 128),
                Registered.PATH to JsonSchemas.string("Path from script_catalog; needed when several scripts share an id", maxLength = WorkspacePath.MAX_BYTES),
                Registered.ARGUMENTS to JsonSchemas.primitiveMap("Values for the script's parameters as script_catalog describes them; strings, numbers and booleans only"),
                Registered.TIMEOUT_MS to JsonSchemas.integer("Stop the script earlier than its registered timeout, in milliseconds", minimum = Images.MIN_TIMEOUT_MS, maximum = McpServerContract.MAX_TOOL_TIMEOUT_MS),
            ),
        ),
        bridge = BridgeMethod("agent", "execRegistered"),
        permissions = listOf("agent", "agent.exec", "engines", "engines.exec"),
        timeoutMs = Registered.DEFAULT_SCRIPT_TIMEOUT_MS,
        maxTimeoutMs = McpServerContract.MAX_TOOL_TIMEOUT_MS,
        hints = ToolHints(destructive = true, openWorld = true),
        hostMethods = listOf(BridgeMethod("agent", "listScripts"), BridgeMethod("agent", "readManifest"), BridgeMethod("agent", "execRegistered")),
        gatedByGrant = true,
    )

    /** The host methods of a tool that takes its image through ImageInputs and runs [operation] on the handle. */
    private fun imageHostMethods(operation: BridgeMethod): List<BridgeMethod> = listOf(
        BridgeMethod("accessibility", "captureScreenImage"), BridgeMethod("media_projection", "requestScreenCapture"),
        BridgeMethod("image", "readImage"), operation, BridgeMethod("image", "recycle"),
    )

    private fun fileSpec(name: String, title: String, description: String, method: String,
        properties: LinkedHashMap<String, JsonObject> = linkedMapOf(Workspace.PATH to pathSchema()), required: List<String> = listOf(Workspace.PATH),
        writes: Boolean = false, deletes: Boolean = false) = ToolSpec(name, title, description,
        JsonSchemas.objectSchema(properties, required), if (deletes) ToolGroup.FILES_DELETE else ToolGroup.FILES,
        bridge = BridgeMethod("files", method), permissions = listOf("files") + when { deletes -> listOf("files.delete"); writes -> listOf("files.write"); else -> emptyList() },
        hints = ToolHints(readOnly = !writes, destructive = writes, idempotent = !writes))

    val all: List<ToolSpec> = listOf(
        devicePing, deviceInfo,
        scriptRun, scriptRunFile, scriptStop, scriptStopAll, scriptList, consoleTail, scriptCatalog, scriptRegisteredRun,
        uiDump, uiFind, uiCurrentWindow, uiExplainSelector, uiWaitFor, uiClick, uiLongClick, uiSetText, uiScroll, uiPressKey,
        uiSwipe, uiGesture,
        screenCapture, screenState,
        filesList, filesStat, filesRead, filesWrite, filesMkdir, filesRename, filesDelete, editorOpen,
        appLaunch, appList, clipboardGet, clipboardSet, deviceEnsureAccessibility, toast, shellExec,
        // Plugin-family rows (roadmap P11): listed only while the host grants their methods and reports the plugin as available.
        ocrRecognize, barcodeDetect, opencvFindImage, opencvFindColor, opencvFindMultiColors, pinyinConvert, pinyin4jConvert, mediainfoRead,
    )

    val byName: Map<String, ToolSpec> = all.associateBy { it.name }

    /** The groups the catalog uses, for the `mcpServerToolGroups` capability. */
    val groups: List<ToolGroup> = ToolGroup.entries.filter { group -> all.any { it.group == group } }

    /** The built-in rows the switches enable: what a session lists when the host reports no plugin family. */
    fun enabled(permissions: ToolPermissions): List<ToolSpec> = all.filter { permissions.isEnabled(it.group) && !it.group.isPluginFamily }

    /** The rows a session lists: the built-in rows the switches enable plus the plugin-family rows [availability] allows. */
    fun enabled(permissions: ToolPermissions, availability: PluginToolAvailability): List<ToolSpec> =
        all.filter { permissions.isEnabled(it.group) && (!it.group.isPluginFamily || availability.allows(it)) }

    /** The rows of the plugin-family groups, in catalog order. */
    val pluginFamilyRows: List<ToolSpec>
        get() = all.filter { it.group.isPluginFamily }

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
        put(Ui.SNAPSHOT_ID, JsonSchemas.string(SNAPSHOT_ID_DESCRIPTION, maxLength = HostSnapshots.MAX_SNAPSHOT_ID_LENGTH))
        put(Ui.SELECTOR, JsonSchemas.selector("Conditions that identify the node; the first match in pre-order is used"))
        if (coordinates) {
            put(Ui.X, JsonSchemas.integer("x of a coordinate tap in device pixels (ui_gesture group)", minimum = 0, maximum = Ui.MAX_COORDINATE))
            put(Ui.Y, JsonSchemas.integer("y of a coordinate tap in device pixels (ui_gesture group)", minimum = 0, maximum = Ui.MAX_COORDINATE))
        }
    }

    private val PRETTY = Json { prettyPrint = true; prettyPrintIndent = "  " }
}
