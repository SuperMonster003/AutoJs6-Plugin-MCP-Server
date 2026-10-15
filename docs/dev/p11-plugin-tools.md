# P11 plugin capability tools: developer notes

Roadmap P11 turns the capabilities of other AutoJs6 plugins (OCR, barcode, OpenCV image search,
Pinyin / Pinyin4j, MediaInfo) into MCP tools. The host side (AutoJs6 6.8.0, decisions D23 - D28
and D45) landed first: OCR engine auto-selection, `accessibility.captureScreenImage`, downsampled
`image.readImage`, the model-session path policy with `files.shared_read`, `app.pluginCapabilities`,
and the wider MCP default grant. This page records the plugin side as it lands.

## Soft probing and the dynamic catalog (D27 / D28)

The plugin never binds a plugin process itself; it asks AutoJs6. Two layers decide whether a
plugin-family tool is listed:

1. **The grant is the ceiling.** `getBrokerInfo()` of the open session lists the methods the host
   allows (`BrokerInfo.methods`, read once when the session opens). A tool row declares every host
   method it may call in `ToolSpec.hostMethods` (default: its `bridge` method; flows list all of
   them). A plugin-family row is listed only while the grant covers all of them. An old host whose
   grant lacks `app.pluginCapabilities` lists none of them, so the 37 built-in tools are unaffected.
2. **The host decides availability.** `app.pluginCapabilities` (schema
   `autojs6-bridge-app-plugin-capabilities-v1`) reports every family with `status` =
   `available` / `not_installed` / `disabled` / `incompatible`, its methods, the plugins behind it
   and a `detail`. `PluginFamilyProbe` calls it with a 5 s timeout and keeps the answer for 5 s
   (`PluginFamilyProbe.TTL_MS`); the cache is dropped on package changes, when a session opens or
   closes, and when the host dies. A host that stops answering (`HOST_UNAVAILABLE`, `TIMEOUT`) keeps
   the last report, marked `stale`, so a short host restart does not make the tools flap; any other
   failure, an unknown answer shape, or a missing method hides the rows and records why.

`ToolRegistry.refresh()` reads both layers together with the group switches, updates the listed set
(`tools/list` answers it in catalog order), and sends one `notifications/tools/list_changed` per
session when the set changed. Every catalog row stays registered on the SDK server from `install()`
on: the SDK notifies every session once per added or removed tool (also through `addTools` /
`removeTools`), so the earlier "remove everything, register the wanted rows" approach sent about
twice the catalog size of notifications per change (172 for two changes on a device, 2026-10-15;
released 1.1.0 did the same on every group switch). A hidden row stays unreachable: the gate answers
it, and its SDK handler asks `disabledFailure` again for a request that bypassed the gate.
The tool gate runs it before every `tools/list` and `tools/call`; the runtime also runs it after a
session opens or closes, after the host dies, and after a package broadcast, so clients learn about
a newly installed plugin without asking. `install()` starts with `PluginToolAvailability.NONE`:
plugin-family rows join on the first refresh.

### Package changes

`PackageChangeWatcher` registers a runtime receiver for `ACTION_PACKAGE_ADDED` / `REMOVED` /
`CHANGED` / `REPLACED` (data scheme `package`) while the listener runs, debounced 500 ms because
one install fires several of them. Each change invalidates the probe and refreshes the registry.
Android 11+ delivers package broadcasts only for visible packages, so the manifest queries the
`org.autojs.plugin.action.WAKE` intent that every AutoJs6 plugin's wake activity declares; without it
the broadcasts of other plugins never arrived and a change showed up only on the next request.
Device scenario: `tools/evidence/p11_list_changed.py` (`pm disable-user` / `pm enable` of a plugin,
the push within about 1 s, exactly one notification per change).
Enabling or disabling a plugin inside the AutoJs6 plugin center sends no broadcast; the 5 s cache
and the refresh before every request cover it.

### Groups and the settings page

`ToolGroup` gained six groups with a `family`: `ocr`, `barcode`, `opencv`, `pinyin`, `pinyin4j`,
`mediainfo`, all on by default (read-only tools, the same risk class as `screen`). The settings
page lists them after the built-in groups under a hint that they need an AutoJs6 plugin; the group
switch wins over the host state (`TOOL_DISABLED` first). `ToolCatalog.groups` and the
`mcpServerToolGroups` capability list a group only once it has rows, so the capability keeps its
eight entries until the tool items land.

### Errors while a row is hidden

A `tools/call` for a hidden plugin-family tool goes through the tool gate like a switched-off tool
and answers an `isError` result instead of the SDK's "tool not found":

| Code | When |
|---|---|
| `TOOL_DISABLED` | the group switch is off |
| `HOST_UNAVAILABLE` | no host session is attached |
| `CAPABILITY_DENIED` | the session grant lacks one of the tool's `hostMethods` (`module` / `method` name the first) |
| `PLUGIN_UNAVAILABLE` | the host reports the family as `not_installed`, `disabled` or `incompatible`, does not know it, or cannot report plugin capabilities at all; the message carries the host's `detail`, the hint points at the AutoJs6 plugin center and `device_info` |

`dumpsys activity service` prints `pluginFamilies: session=open stale=false families={ocr=available,...} probes=N`.

### Tests

- JVM: `PluginFamiliesTest` (report parsing, the availability rule, the probe's cache / invalidation /
  session change / stale handling / failure handling), `ToolRegistryTest` (plugin-family rows appear
  and vanish with the grant and the report, the gate reasons), `ToolCatalogTest` and
  `ToolPermissionsTest` (the new groups).
- Instrumentation and devices: recorded with the tool items and the P11 test item in `ROADMAP.md`.

## ocr_recognize (D23 / D24 / D25 / D38)

The first plugin-family tool (`OcrTools`, group `ocr`): one tool with an `image` argument (D38).

| `image` | Host calls | Notes |
|---|---|---|
| `"screen"` | `accessibility.readScreenText({region?, engine?})` | capture and recognition happen inside AutoJs6 (D23); a `permission-denied` answer means MediaProjection consent is missing (Android 10 and below, or no accessibility service), so the flow asks once through `media_projection.requestScreenCapture` and retries, like `screen_capture` |
| a workspace path | `image.readImage(path)` -> `ocr.recognize(handle, {region?, engine?})` -> `image.recycle(handle)` | the recycle runs in a `finally`; the host decodes large files downscaled (D45) and the flow converts `region` to decoded pixels and the bounds back to file pixels, so callers only see the file's own coordinates (`scale`, `decodedWidth` / `decodedHeight` are reported when the host downscaled) |

Arguments: `image` (required), `region` (`{left, top, right, bottom}`, exclusive right / bottom, screen or file pixels),
`engine` (`auto` / `mlkit` / `paddle` / `rapid`, mapped to the host engine ids `mlkit-ocr` / `paddle-ocr` / `rapid-ocr`; `auto` sends
nothing and the host picks the first enabled plugin in that order, D25), `timeoutMs` (default 60 s because a first Paddle call loads
its model, 1 s .. 300 s). Only workspace-relative paths are accepted until the shared storage switch (D26) lands.

Result: the content text is the recognized lines joined with newlines (`(no text recognized)` when empty); the structured content is
`{image, source: "screen" | "file", engine, count, items[{text, confidence?, bounds?}], text, region?, width?, height?, scale?, decodedWidth?, decodedHeight?, durationMs}`.
The host answers `[{text, confidence, bounds}]` for both paths (`readScreenText` caps the list at the Agent contract's item and byte
limits); `confidence` and `bounds` are omitted when the plugin gives none.

Errors: a missing or disabled OCR plugin comes back from the host as the `unavailable` category (`OCR_PLUGIN_REQUIRED` from
`readScreenText`, "requires an enabled compatible recognition plugin" from `ocr.recognize`); the flow maps both to
`PLUGIN_UNAVAILABLE` instead of the generic `A11Y_SERVICE_NOT_RUNNING` / `HOST_UNAVAILABLE` of that category. A refused consent is
`CAPABILITY_DENIED` with the `screen_capture` consent hint. Bad arguments are rejected before any host call, except a `region` beyond the
file, which needs the handle's dimensions and is rejected after `image.readImage` (the handle is still recycled).

`ToolCatalog.enabled(permissions)` now lists the built-in rows only; `enabled(permissions, availability)` adds the plugin-family rows,
and `ToolCatalog.groups` / `mcpServerToolGroups` carry `ocr` from this row on.

Tests: `OcrToolsTest` (JVM, fake caller: argument mapping, scale conversion, recycle on failure, PLUGIN_UNAVAILABLE mapping, consent
retry, argument checks), `McpServerHostSessionTest.pluginFamilyToolsFollowTheHostReportAndRunThroughTheHost` (instrumentation, fake
broker: the row appears with the report, both paths run, the row leaves when the plugin goes away).

## Image inputs shared by the image tools

`ImageInputs` holds what every tool with an `image` argument (D38) needs: `"screen"` or a workspace path, a `region`
(`{left, top, right, bottom}`, right and bottom exclusive, in screen pixels or file pixels), and a session image handle that is
taken, used and recycled in one call (`withImage`; `withFile` for an argument that is always a path, such as a template, even one
named `screen`).

| Source | Host call | Notes |
|---|---|---|
| `"screen"` | `accessibility.captureScreenImage()` | accessibility screenshot on Android 11+, MediaProjection otherwise (D23); `permission-denied` asks once for consent through `media_projection.requestScreenCapture` and retries, like `screen_capture`; the handle reports `captureSource` |
| a workspace path | `image.readImage(path)` | large files are decoded at a power-of-two sample size (D45); the handle reports `sourceWidth` / `sourceHeight` / `scale` |
| afterwards | `image.recycle(handle)` | in a `finally`; a failure is not reported (the session close releases the handle at the latest) |

`ImageHandle` converts in both directions: `region` to decoded pixels (`{x, y, width, height}` for OCR and barcode, `[x, y, width, height]`
for the OpenCV methods), and results back to the image's own pixels. A region beyond the image is rejected once its size is known (after the
handle is taken; the handle is still recycled). Every result reports `width` / `height` (the image's own size), `scale`, `decodedWidth` /
`decodedHeight` when the host downscaled, and `captureSource` for the screen. A host `resource-limit` answer while taking a handle (the
session keeps at most twice its concurrent call limit, 8 for MCP) is `LIMIT_EXCEEDED` with a hint that names the handle limit; the plugin's
own concurrency limit (4) keeps a session at 8 handles at most, so this only shows when handles leak. `ocr_recognize` uses the same helper for
its file path; its screen path stays on `accessibility.readScreenText`.

## barcode_detect (D23 / D24 / D38)

`BarcodeTools`, group `barcode`: the image handle goes to `barcode.detectAll(handle, {formats?, maxResults, region?})`.

Arguments: `image` (required), `formats` (1 - 13 of `AZTEC`, `CODABAR`, `CODE_39`, `CODE_93`, `CODE_128`, `DATA_MATRIX`, `EAN_8`, `EAN_13`, `ITF`,
`PDF_417`, `QR_CODE`, `UPC_A`, `UPC_E`, the names the host reports; omitted = all; duplicates are dropped), `region`, `maxResults` (default 10,
1 .. 128, the host's cap), `timeoutMs` (default 30 s, 1 s .. 300 s).

Result: one content line per code, `FORMAT: rawValue` (`(no barcode found)` when empty); structured content
`{image, source, count, items[{rawValue, displayValue, format, valueType, bounds?, cornerPoints?, structured?}], formats?, region?, width, height, scale, decodedWidth?, decodedHeight?, captureSource?, durationMs}`.
`rawValue` / `displayValue` may be null (binary payloads); `structured` is the plugin's parsed form (URL, Wi-Fi, contact, ...) when it gives one.
A missing or disabled barcode plugin comes back as the host's `unavailable` category with module `barcode` and is mapped to `PLUGIN_UNAVAILABLE`.

## opencv_find_image, opencv_find_color, opencv_find_multi_colors (D23 / D24 / D38)

`OpenCvTools`, group `opencv`: the OpenCV methods of the host's `image` module, which run on the AutoJs6 OpenCV plugin inside the host process.

| Tool | Host call | Result |
|---|---|---|
| `opencv_find_image` (`image`, `template` path, `threshold` 0..1 default 0.9, `region`, `maxResults` default 5 max 100) | `image.matchTemplate(image, template, {threshold, limit, region?})` | `matches[{bounds, center, score}]`, best first, no overlaps; `templateWidth` / `templateHeight` |
| `opencv_find_color` (`image`, `color`, `threshold` 0..255 default 4, `region`) | `image.findColor(image, color, {threshold, region?})` | `found`, `point?`: the first pixel in row order |
| `opencv_find_multi_colors` (`image`, `color`, `points[{x, y, color}]` 1 - 64, `threshold`, `region`) | `image.findMultiColors(image, color, [[x, y, color], ...], {threshold, region?})` | `found`, `point?` |

Colors are `#RRGGBB` or `#RGB` and are sent upper case as `#RRGGBB`. `threshold` is the host's: a per-channel box for the first color and the
average difference for the neighbors. Every tool has `timeoutMs` (default 30 s, 1 s .. 300 s).

Scale rules (D45): the template is read as a file too. When the host decodes the image and the template at different sample sizes (in practice:
an image longer than 4096 px with an ordinary template), template matching would compare two scales and find nothing, so the call is refused with
`INVALID_ARGUMENTS` before `matchTemplate`; a template larger than the search area is refused the same way. Points of a downscaled file are
reported at the file pixel under the decoded pixel's center; neighbor offsets of `opencv_find_multi_colors` are scaled to decoded pixels and
rounded. The screen is never downscaled. An OpenCV plugin that disappears between the probe and the call surfaces as the host's
`provider-failed` (`HOST_ERROR`); the next `tools/list` hides the rows.

Tests: `BarcodeToolsTest` and `OpenCvToolsTest` (JVM, fake caller: argument mapping, region conversion, scale mapping, recycle on every path,
consent retry, handle-limit hint, PLUGIN_UNAVAILABLE, argument checks), `McpServerHostSessionTest.pluginFamilyToolsFollowTheHostReportAndRunThroughTheHost`
(instrumentation, fake broker: the rows appear with the report, the bridge envelopes, both handles of a template match recycled, the rows leave when
the plugins go away). Device scenario: `tools/evidence/p11_image_tools.py`.

## pinyin_convert, pinyin4j_convert (D38)

`PinyinTools`, groups `pinyin` and `pinyin4j`: plain-text tools, one host call each, only the common options.

| Tool | Host call | Notes |
|---|---|---|
| `pinyin_convert` (`text`, `style` tone / tone2 / to3ne / normal / initials / first_letter, `mode` normal / surname / place_name, `segment` default true, `heteronym` default false) | `pinyin.convert(text, {style, mode, segment, heteronym})` with the host's upper-case names | the Pinyin plugin reads whole words from its phrase dictionary; segmentation (on here, off in the script API) splits longer text into words first. On a device "银行行长" read `yín háng háng cháng` with and without it, so the tool description makes no promise about names and rare words |
| `pinyin4j_convert` (`text`, `romanization` hanyu / tongyong / wade_giles / mps2 / yale / gwoyeu, `tone` mark / number / none and `case` lower / upper for hanyu only, `separator` default a space, `allReadings` default false) | `pinyin4j.readings(text, {romanization, tone?, case?})` | one row per code point (the code point itself when it has no reading); the text joins the first reading of each row with the separator, which is exactly what `pinyin4j.of` returns, so one host method covers both forms. `tone` or `case` with another romanization is refused before the host call (the host would refuse it too) |

`text` is at most 8 KiB of UTF-8, well inside the host's 32 KiB argument limit (`NodeCapabilityBrokerPolicy`). Results: `pinyin_convert`
`{pinyin, rows, style, mode, segment, heteronym}`, `pinyin4j_convert` `{text, rows, romanization, tone?, case?, allReadings}`; the content text joins rows with
spaces (pinyin) or the separator (pinyin4j) and alternatives with `|`. A Pinyin plugin that fails surfaces as the host's `provider-failed`
(`HOST_ERROR`); the probe hides the rows while a family is not available.

## mediainfo_read (D38)

`MediaInfoTools`, group `mediainfo`: `mediainfo.read(path, {includeInform: report, includeSections: true})`, permissions `media` + `media.metadata`.

Arguments: `path` (workspace path; the shared storage switch of D26 widens it later), `report` (default false), `timeoutMs` (default 60 s, the plugin gives up a
parse after 30 s). The host's default snapshot (`autojs6-node-mediainfo-snapshot-v1`) groups the fields of MediaInfo's text report by track kind, one object per
track with camelCase keys (`overallBitRate`, `samplingRate`, `channels`, ...) and MediaInfo's display strings ("8 000 Hz", "1 channel").

Result: one content line per track (`audio #2: format=AAC LC, channels=1 channel, ...`, chosen keys per kind in `MediaInfoTools.SUMMARY_KEYS`, the first six fields
when none of them is present), then the report when asked; structured content `{path, fileName?, sizeBytes?, trackCounts, sections, report?, reportTruncated?, durationMs}`
with the report cut at 64 Ki characters. A missing plugin is the host's `unavailable` category with module `mediainfo` and maps to `PLUGIN_UNAVAILABLE`; a missing
file is `INVALID_ARGUMENTS` ("target does not exist").

Tests: `PinyinToolsTest` and `MediaInfoToolsTest` (JVM), the plugin family case of `McpServerHostSessionTest` (fake broker). Device scenario:
`tools/evidence/p11_text_tools.py` (a one-second WAV fixture written through `script_run`; needs "All files access" for the media checks).

## Shared screenshot window (D39)

Every tool with an `image` argument counts a `"screen"` call against the per-client window of 30 screenshots per minute that
`screen_capture` uses; files are not counted. The rate limiter decides this from the JSON-RPC body before the call reaches the tool
gate (`RateLimiter.countsAsScreenshot`); details and evidence in `docs/dev/p6-rate-limits.md` (P11 addendum).

## Shared storage, read-only (D26)

The settings page has an "Allow reading shared storage" switch (off by default). It is stored as `allowSharedStorageRead` in
`tool_groups.json`, next to `allowShellRoot`, and `PathAccess` reads it again for every call (the settings page writes from the
main process, the listener runs in `:mcp_server`).

| Path | Switch off | Switch on |
|---|---|---|
| relative (`pics/a.png`, `.`) | the working directory, as before | the same |
| absolute in shared storage (`/storage/emulated/0/...` or the `/sdcard/...` alias) | `CAPABILITY_DENIED` with a hint to the switch, before any host call | sent as given (trailing `/` dropped), declaring `files.shared_read` |
| `Android/data`, `Android/obb` (any letter case) or deeper | refused | `INVALID_ARGUMENTS`, never readable |
| any other absolute path, `.` / `..` / empty segments, backslashes, control characters | refused | `INVALID_ARGUMENTS` |

Tools that take such paths: the `image` of `ocr_recognize`, `barcode_detect` and the three opencv tools, the `template` of
`opencv_find_image`, the `path` of `mediainfo_read`, `files_list` and `files_stat`. Every other file tool stays in the working
directory whatever the switch says. Only a request that targets shared storage declares `files.shared_read`, so a relative
`files_list` on a host whose grant lacks the token keeps working. AutoJs6 checks again (`NodeBridgeModelPathPolicy`: canonical
path, the token, its own all-files access, `Android/data` / `Android/obb`); a host without all-files access answers
`permission-denied` (`CAPABILITY_DENIED`) with its reason.

A shared storage listing or stat comes back with the shared storage `root` (the host's canonical root, for example
`/storage/emulated/0`), absolute `path` values for the target and every entry, and `sharedStorage: true`; a workspace answer keeps
its old shape (no `root`). The roots the plugin accepts are `Environment.getExternalStorageDirectory()` and `/sdcard`.

Tests: `PathAccessTest`, the shared cases of `WorkspaceToolsTest`, `BarcodeToolsTest`, `MediaInfoToolsTest`, `ToolPermissionsTest`
(JVM); the workspace case of `McpServerHostSessionTest` (switch off -> refused without a host call, on -> token, absolute entries,
`Android/data` refused, `files_read` refused). Device scenario: `tools/evidence/p11_shared_storage.py` (the switch is set through
`EvidenceHelperTest#setSharedStorageRead` and restored afterwards).

## device_info plugin families (D27)

`device_info` became a flow (`DeviceTools`, `DeviceInfo`): `device.info` as before, then `app.pluginCapabilities` (5 s, token `app.query`).
When the host answers with the capabilities schema, the result and the resource `autojs6://device/info` gain `plugins`:

```json
{"families": [{"id": "barcode", "name": "ML Kit Barcode", "status": "not_installed", "toolGroup": "barcode", "toolGroupEnabled": true,
               "tools": ["barcode_detect"], "plugins": [], "detail": "...", "hint": "install the ML Kit Barcode plugin from the AutoJs6 plugin center"}],
 "hint": "plugins are installed and enabled in AutoJs6 > Plugin center; the tool list follows within seconds"}
```

One entry per family in the host's order (`ocr`, `barcode`, `opencv`, `pinyin`, `pinyin4j`, `mediainfo`, `docs`, and any family a newer
host adds, shown by its id); `tools` are the catalog rows of the family, `docs` lists the `autojs6://docs/{+path}` resources instead,
`plugins` keeps package, version, enabled, compatible and the required host build. A `hint` appears when a family brings no tools:
install, enable, update (with the build an incompatible plugin needs), or switch the tool group on in the MCP Server settings. A host
without the method, a grant without it, a failure or an unknown answer leaves `plugins` out; the device part is unchanged, and the
`outputSchema` stays an open object with `plugins` described but not required.

Tests: `DeviceInfoTest` (JVM: section shape and hints, the section only with a valid report, the flow and the output schema), the
basic and plugin-family cases of `McpServerHostSessionTest` (no section without the grant; families, tools and the not-installed hint
with the fake report). Device scenario: `tools/evidence/p11_device_info.py`.

## Registered scripts (D42)

Two built-in rows of the `script` group expose the scripts a user registered for models (an `@agent` JSDoc header, or an `agent`
object in `project.json`; host `docs/dev/agent-script-manifest-v1.md`), so the tool list does not grow with the number of scripts:

| Tool | Host calls | Permissions |
|---|---|---|
| `script_catalog` (`query?`, `limit` 1..500, default 100) | `agent.listScripts({query?, limit})` | `agent` |
| `script_registered_run` (`id` or `path`, `arguments`, `timeoutMs?`) | `agent.listScripts` (only for an id) -> `agent.readManifest(path)` -> `agent.execRegistered(path, arguments, {expectedManifest, timeoutMs, captureConsole: true})` | `agent` for the first two, `agent`, `agent.exec`, `engines`, `engines.exec` for the run |

Refinements decided 2026-10-15:

- `risk` and `confirm` are passed through. The catalog lists them (the host normalizes `risk: sensitive` to `confirm: before-run`),
  both descriptions tell the model to ask the user before such a script, and the run row carries the same annotations as `script_run`
  (destructive, open world). The phone shows no extra prompt; an MCP client confirms destructive tools itself.
- A run names exactly one of `id` or `path`. An id is resolved through `listScripts({query: id, limit: 500})` filtered by exact id;
  none is `INVALID_ARGUMENTS`, and several (two directories may register the same id) is `INVALID_ARGUMENTS` listing every path. The
  read manifest goes back as `expectedManifest`, so a registration that changed in between is refused.
- The host's default scan scope: the working directory to depth 4 plus its `agent/` subtree. MCP sessions configure no extra roots.

The plugin checks only what it can without the manifest: `..` or NUL in `path`, argument values that are not strings, numbers or
booleans, and more than 16 KiB of argument JSON. AutoJs6 validates the arguments against the registered schema (types, enumerations,
bounds, required names, no extra names, defaults filled in) and stops the script at `min(timeoutMs, registered timeout)`; the bridge
waits 5 s longer. The host answers catalog failures with the bare code as the message; `SCRIPT_NOT_REGISTERED` and
`TOOL_ARGUMENTS_INVALID` become `INVALID_ARGUMENTS`, a path outside the scanned directories stays `CAPABILITY_DENIED`, each with a
hint to `script_catalog`. The run result:

```json
{"id": "echo", "path": "/storage/emulated/0/Scripts/echo.js", "kind": "file", "risk": "readonly", "confirm": "never",
 "status": "finished", "outcome": "success", "executionId": 12, "resultReported": true, "result": {"text": "abab", "times": 2},
 "console": ["echo abab"], "durationMs": 1737}
```

`status` maps the host outcome onto the statuses of the script group (`success` -> `finished`, `exception` -> `error`, `stopped`,
`timeout`); `error` keeps the host's `{code, message}`; `result` appears only when the script called `ai.agent.result`.

### Grant-gated built-in rows

Older AutoJs6 builds do not grant the `agent.*` methods to MCP sessions (host commit `972e5dba6` adds them with the `agent` and
`agent.exec` tokens). `ToolSpec.gatedByGrant` marks a built-in row that is listed only while the open session's grant covers all its
`hostMethods`; without a session, or when the host reports an empty grant, it is listed like any other built-in row.
`PluginToolAvailability.unavailableFailure` reuses the family check (`missingGrant`), so a direct call on an older host answers
`CAPABILITY_DENIED` naming the first missing method instead of failing inside the host. The catalog snapshot shows
`"gatedByGrant": true`.

Tests: `RegisteredScriptToolsTest` (JVM: catalog options and text, id resolution with none / one / several matches, argument checks,
the exec envelope, timeouts and failure mapping), `ToolRegistryTest` (grant-gated rows hidden on a grant without the agent methods),
`ToolCatalogTest`, and `McpServerHostSessionTest#registeredScriptToolsListAndRunThroughTheHost` (listing, the
listScripts -> readManifest -> execRegistered sequence, permissions and `expectedManifest`). Device scenario:
`tools/evidence/p11_registered_scripts.py` (`--expect-hidden` for an older host).
