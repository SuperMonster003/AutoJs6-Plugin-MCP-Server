# P3 tool groups: developer notes

Roadmap P3 fills the tool table of appendix A.2 group by group on top of the P2.3 framework
(`ToolCatalog`, `ToolArguments`, `CatalogToolExecutor`, `HostBridgeClient`; see
`p2-host-bridge-and-tools.md`). Each section below records one group: the host methods it maps
onto, the result shapes, and the deviations from the roadmap text.

## P3.1 script group

| Tool | Host method | Arguments | Result |
|---|---|---|---|
| `script_run` | `engines.execScript(name, source, options)` | `source`, `name?`, run options | run result |
| `script_run_file` | `engines.execScriptFile(path, options)` | `path`, run options | run result |
| `script_stop` | `engines.stop(executionId)` | `executionId` | `{executionId, name, state, stopped}` |
| `script_stop_all` | `engines.stopAll({scope: "host"})` | - | `{stopped}` |
| `script_list` | `engines.list()` | - | `{count, executions[{executionId, name, engine, path, workingDirectory, state, startedAt, uptimeMs}]}` |
| `console_tail` | `console.tail({lines, sinceId?, level?})` | `lines?=100`, `sinceId?`, `level?` | the host record without its `schema` tag: `entries[{id, level, levelName, time, text}]`, `count`, `nextSinceId`, `latestId`, `total`, `truncated`, `evicted`, ... |

Run options shared by the two run tools: `workingDirectory?` (POSIX path relative to the AutoJs6
working directory, the host refuses escapes), `arguments?` (primitive map exposed through
`engines.myEngine().execArgv`), `timeoutMs?=60000` (1 s .. 300 s), `waitForCompletion?=true`,
`captureConsole?=true`, `maxConsoleLines?=200` (1 .. 500).

### Run result

```json
{
  "executionId": 12,
  "name": "demo",
  "engine": "org.autojs.autojs.script.JavaScriptSource.Engine",
  "workingDirectory": "/storage/emulated/0/Scripts",
  "status": "finished",
  "waited": true,
  "waitMs": 60000,
  "durationMs": 812,
  "console": [{"id": 40, "level": 3, "levelName": "debug", "time": 1789003118084, "text": "09:18:38.084/D: hello"}],
  "consoleCount": 6,
  "consoleTruncated": false
}
```

- `status` is `finished` (the script completed), `error` (it threw; `exception.message` is the
  host's error text and `exception.line` the Rhino line taken from its `(<file>#<line>)` suffix),
  or `running` (not waited for, or still running when the wait ended; `hint` names `script_stop`
  with the id, `script_list`, and `console_tail`). A script that outlives the wait is never killed.
- `durationMs` is the host's `waitedMs`: the time between the start and the completion, or the
  time waited when the script is still running.
- `console` holds the newest `maxConsoleLines` entries of the console window the host captured
  for this run (`captureMode = global-window`: every runtime prints into the one GlobalConsole, so
  lines of other scripts running at the same time appear as well); `consoleCount` is the size of
  the whole window and `consoleTruncated` says lines were dropped, by the host or here.
- The wait is `min(timeoutMs, 290 s)` so the bridge timeout (the wait plus 10 s) stays inside the
  contract ceiling of 300 s; the host's own start timeout is `min(timeoutMs, 60 s)`.

### Deviations

- The roadmap lists a `timeout` status; a wait that expires answers `running` (the roadmap's own
  sentence: the script is not killed and `script_stop` is suggested), and a bridge timeout is the
  `TIMEOUT` error. So the status set is `finished` / `error` / `running`.
- `exception` carries the message and the line only: the host reports the throwable's message,
  and Rhino puts the line but not the column into it.
- `engines.stop` and `engines.list` have the host-process meaning on their own; only
  `engines.stopAll` needs `{scope: "host"}` (the plain call keeps the Node plugin's own-process
  meaning). `stopAll` counts every execution the host tracks, including scripts started on the
  phone, hence `destructiveHint`.
- Progress: while a run tool waits and the client sent `_meta.progressToken`, a
  `notifications/progress` goes out every 2 s with the elapsed time and, when the grant allows
  `console.tail`, the newest console line (`"waiting for AutoJs6 (4 s); last output: ..."`). The
  line is the global console's newest entry, not necessarily this script's. Other host-backed
  tools keep the 5 s heartbeat without a line. The notification is sent with the related
  request id, and the endpoint answers every POST that carries requests on a
  `text/event-stream` body (`server/SseStreamableMount`), so the heartbeat precedes the result
  on the request's own stream and a client that never opened the standalone GET stream still
  receives it. The SDK's own `mcpStreamableHttp` mount answers in JSON and routes such a
  notification to the GET stream, where it is dropped when none is open (observed on a device
  before the switch). A POST of notifications only is still answered with 202, and the gate,
  bearer, and pairing rejections stay JSON bodies.
- A pending run holds one of the four concurrent host calls for its whole wait; `script_stop`
  still fits beside it, but four parallel long runs would make a fifth call wait 10 s and fail
  with `LIMIT_EXCEEDED`.
- `script_run_file` runs whatever engine the host picks from the suffix (the P1.3 bridge refuses
  nested Node.js executions); the run result shape is the same.

## P3.2 ui and ui_gesture groups

| Tool | Host method | Arguments | Result |
|---|---|---|---|
| `ui_dump` | `accessibility.dump({format, maxDepth, maxNodes, visibleOnly, window})` | `format?=text` (`text` / `json` / `xml`), `maxDepth?=32`, `maxNodes?=200` (1 .. 400), `visibleOnly?=true`, `window?=active` (`active` / `all`) | text: the compact tree (appendix B) as the content text and `{snapshotId, format, window{packageName, activityName}, roots, nodeCount, truncated}` as structured content; json: the same plus `nodes[]` (full node records with `ref`); xml: the host XML as the content text |
| `ui_find` | `accessibility.findAll(selector)`, polled every 500 ms until `timeoutMs` | `selector`, `limit?=10` (1 .. 100), `timeoutMs?=0` (0 .. 60000) | `{snapshotId?, selector, count, returned, truncated, elapsedMs, nodes[], hint?}` |
| `ui_current_window` | `app.currentWindow()` | - | the host record without its `schema` tag |
| `ui_explain_selector` | `accessibility.explain(selector, {})` | `selector` | the host record without `schema`, `matches` / `nearMisses` as node records; the host text as the content text |
| `ui_wait_for` | `accessibility.findOne(selector)`, polled every 500 ms | `selector`, `state?=appear` (`appear` / `disappear`), `timeoutMs?=10000` (100 .. 120000) | `{state, satisfied: true, selector, elapsedMs, snapshotId?, node?}`; `TIMEOUT` when the state is not reached |
| `ui_click`, `ui_long_click` | `accessibility.click` / `longClick(selector)`; the coordinate form is `accessibility.swipe(x, y, x, y, 100 / 700)` | `nodeRef` or `selector`, or `x` + `y` | `{action, performed: true, via, target?}` or `{action, performed, via: "coordinates", x, y, durationMs}` |
| `ui_set_text` | `accessibility.setText(selector, text)` | `nodeRef` or `selector`, `text` (up to 16 KiB), `append?=false` | `{action: "set_text", performed, via, target, text, appended}` |
| `ui_scroll` | `accessibility.scrollForward` / `scrollBackward(selector)`, `times` times with 300 ms between | `nodeRef` or `selector` (default: the first scrollable node), `direction?=forward` (`forward` / `backward` / `down` / `up` / `right` / `left`), `times?=1` (1 .. 20) | `{action: "scroll", direction, requested, performed, atEnd, via, target, hint?}` |
| `ui_press_key` | `accessibility.back` / `home` / `recentApps`, `keys.notifications` / `quickSettings` / `powerDialog` / `lockScreen` | `key` | `{key, performed: true}` |
| `ui_swipe` | `accessibility.swipe(x1, y1, x2, y2, durationMs)` | `x1`, `y1`, `x2`, `y2`, `durationMs?=300` (1 .. 10000) | `{action: "swipe", performed, from, to, durationMs}` |
| `ui_gesture` | `accessibility.gesture(durationMs, points)` | `durationMs` (1 .. 10000), `points` (2 .. 64 `[x, y]` pairs) | `{action: "gesture", performed, points, durationMs}` |

`via` is `nodeRef`, `selector`, `scrollable` (the default target of `ui_scroll`), or `coordinates`;
`target` is the node record the tool acted on (`ref` when it came from a reference). The ten `ui`
tools need the host permission `accessibility` (`ui_current_window`: `app.query` as well,
`ui_press_key` with a `keys.*` key: `keys`); the two `ui_gesture` tools and the coordinate form of
the click tools need `accessibility.gesture` on top and follow the `ui_gesture` group switch
(decision D22: the coordinate form of a `ui` tool answers `TOOL_DISABLED` while the group is off).

### Selectors

The `selector` object is the BridgeSelector dialect (decision D13), one key per condition: `text`,
`textContains`, `textMatches`, `desc`, `descContains`, `descMatches`, `id`, `idMatches`,
`className`, `classNameMatches`, `clickable`, `enabled`, `scrollable`, `depth`, `boundsInside`,
`boundsContains` (`{left, top, right, bottom}`). Two conveniences on top: an `id` without `:`
matches the short form (`search` becomes `idMatches (?:.*:id/)?search`) and a `className` without
`.` matches the simple class name (`Button` becomes `classNameMatches (?:.*\.)?Button`). Regular
expressions are compiled on the plugin side, so a bad one is `INVALID_ARGUMENTS` naming the
condition, as are an empty selector, an unknown key, a wrong type, and a condition given twice.

### Node references

`ui_dump` replaces the snapshot (`s1`, `s2`, ...) and numbers its nodes `#n1`, `#n2`, ... in
pre-order; `ui_find` and `ui_wait_for` append their nodes to the current snapshot (and start one
when there is none). A reference lives 60 s. An action on a `nodeRef` makes two host calls: an
`accessibility.findAll` with the node's fingerprint (`className`, `id`, `text`, `desc` when
present, and `boundsInside` = the remembered bounds grown by up to 48 px on each side, at most
half the node's width / height), then the action with an exact selector (the fingerprint plus
`boundsInside` and `boundsContains` set to the relocated bounds). Of several matches the one
closest to the remembered bounds wins. No match, an unknown number, a number from an older
snapshot, or an expired reference is `NODE_REF_STALE` (`hint`: dump again or use a selector).

### Compact text

```
window: com.android.settings/.Settings  bounds=[0,0][1080,2400]  nodes=5
#n1 FrameLayout [0,0][1080,2400]
#n2  RecyclerView scrollable id=recycler_view [0,220][1080,2400]
#n3   LinearLayout clickable id=recycler_item [0,220][1080,376]
#n4    TextView "Wi-Fi" c=(540,270)
#n5    Switch clickable checkable checked id=switch_widget c=(980,298)
```

The header names the window (package and the activity's short form), the root bounds, the node
count, and `truncated=true` when the host cut the tree. Each line is the reference, one space per
depth, the simple class name, the markers (`clickable`, `long_clickable`, `checkable`, `checked`,
`scrollable`, `editable`, `focused`, `selected`, `!enabled`, `hidden`), `"text"`, `desc="..."`,
`id=<short id>`, and either the bounds (a node with children) or the centre `c=(x,y)` (a leaf).
Text is escaped and cut at 40 characters. A closing line says when listed nodes have children
that are not shown (`visibleOnly`, `maxDepth`, or the `maxNodes` budget) and names the knobs
(`maxNodes` up to 400, `maxDepth`, `ui_find`).

### Deviations

- `ACTION_FAILED` (outside appendix A.5): the node was found but the host answered `false`
  (not clickable, not editable, a cancelled gesture, a refused key). `NODE_NOT_FOUND` stays for
  "no node matches the selector" and for a node that disappeared between the lookup and the action.
  Android reports `ACTION_LONG_CLICK` as performed only when the node's long-click listener
  consumes the event, so `ui_long_click` can answer `ACTION_FAILED` after the press did take
  effect (seen on a UI-mode script button whose handler ran); the hint says so.
- References live 60 s (the roadmap draft said 30 s): a model needs the time to read a dump
  and decide.
- `ui_find` without a match is a normal result (`count: 0` and a `hint`); only `ui_wait_for`
  answers `TIMEOUT`, and `ui_scroll` at the end of a list answers `performed < requested` with
  `atEnd: true` and a `hint` rather than an error.
- The coordinate forms of `ui_click` / `ui_long_click` are a zero-length `accessibility.swipe`
  of 100 ms / 700 ms (the bridge has no tap method); the host dispatches identical end points as
  a single-point press. A host built before 2026-09-11 answered every bridge gesture after the
  first one on a reused broker dispatch thread with `false`: its gesture automator had no
  handler, prepared a Looper on that thread and quit it after the first gesture, so the first
  device runs showed `ACTION_FAILED` "the system cancelled ..." at random (a 200 px swipe fine,
  the next tap "cancelled" in 30 ms, the real result posted to a dead handler); the host fix
  hands the automator the main-looper handler. They and `ui_swipe` / `ui_gesture` refuse points
  beyond the screen size read from `device.info` (cached for a minute; no check when the host
  reports none).
- `A11Y_SERVICE_NOT_RUNNING` maps the host's `unavailable` category for the `accessibility` and
  `keys` modules; the hint names the accessibility settings and the Android 13+ restricted
  settings step. `device_ensure_accessibility` (roadmap P3.4) does not exist yet, so the hint
  does not mention it.
- Host side (AutoJs6 6.8.0 after 2026-09-10): `accessibility.dump` JSON nodes carry
  `scrollable`, `checkable`, `checked`, `editable`, `focused`, `selected`, `longClickable` (the
  compact markers); the MCP grant includes `keys.notifications` / `quickSettings` /
  `powerDialog` / `lockScreen`; a `keys.*` call without the accessibility service answers the
  `unavailable` category; since 2026-09-11 the bridge gesture automator takes the main-looper
  handler (see above). An older host answers `ui_press_key` with `notifications` and friends
  as `CAPABILITY_DENIED`, dumps without the extra markers, and fails coordinate gestures at
  random.
- `ui_press_key` with `lock_screen` needs Android 9 or later; `ui_current_window` reads the
  window through the accessibility service, so it needs the service like the other tools.
