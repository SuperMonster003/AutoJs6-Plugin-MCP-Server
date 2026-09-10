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
