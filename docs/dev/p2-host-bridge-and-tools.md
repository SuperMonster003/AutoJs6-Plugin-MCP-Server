# P2.3 host bridge and tools: developer notes

Roadmap P2.3 connects the listener of P2.1 / P2.2 to AutoJs6: the plugin implements the real
`IMcpServerPlugin` Binder, tool calls travel through the host capability broker, and a tool
catalog with group switches replaces the single hard-wired `device_ping`. This note records the
design decisions that are not obvious from the roadmap and the evidence of the first delivery.

## Process model

Everything of the server lives in the `:mcp_server` process:

- `McpServerPluginService` is the Binder the host binds to (action `org.autojs.plugin.MCP_SERVER`).
  `getInfo` / `getCapabilities` are metadata and answer any caller the manifest permission admits;
  `openServer` additionally runs `HostCallerVerifier` (calling UID is the installed host's UID,
  host version >= 5279, identical signer sets) and hands the call to the runtime.
- `McpServerRuntime` is the process singleton that owns the `McpHttpServer`, the `ToolRegistry`,
  the open host session, and the reference to the foreground service.
- `McpServerService` is the foreground service that keeps the process alive while the listener
  runs. It no longer owns the listener: `ACTION_START` / `ACTION_STOP` (adb, settings page) call
  the runtime, and the runtime brings the service up with `ACTION_KEEP_ALIVE` when the host opens
  a session. A keep-alive that arrives after the listener already failed stops the service again.

## Binder surface

`openServer(config, broker, callback)`:

1. `SessionConfig.fromValues` validates the Bundle: port within `1024..65535`, bind scope
   `loopback` / `lan`, protocol mode must be `stateful` (D9), host label <= 64 characters. A
   rejected value throws `IllegalArgumentException("invalid_config: ...")`, an unsupported
   `contractVersion` throws `unsupported_contract`; Binder carries both to the host unchanged.
2. `BinderBridgeTransport` reads `getBrokerInfo()` once and `HostBridgeClient` links to the
   broker's death.
3. A previous session whose host is still alive (bridge available and broker answers `pingBinder`)
   is refused with `IllegalStateException("already_open: ...")`; a session whose host died or that
   was closed is replaced silently.
4. The session Binder returns at once. On the lifecycle thread the runtime starts the listener with
   the running listener's `ServerConfig` (else the stored one) plus the host's port and scope
   (restarting a running listener whose port or scope differs, reason `config_changed`; an
   adb-started developer mode survives the attach), publishes the status through the callback, and
   runs one `device.info` probe through the broker (`McpServerRuntime: Host probe ok ...` in the
   log). That probe is the P1 acceptance round trip `openServer -> dispatch -> onResponse`.

`IMcpServerSession`:

- `getStatus()` returns the `KEY_STATUS_*` Bundle built from `McpServerRuntime.statusSnapshot()`:
  the listener state, every endpoint URL (loopback first, then LAN addresses in LAN scope), the
  port and scope, paired and active session counts, `startedAt`, `hostAvailable`, and the last
  error code (a bind failure, or the last stop reason other than `user_request`).
- `updateConfig(config)` records the new port / scope; a running listener restarts when they
  changed, a stopped one stays stopped (there is no `start` in the contract; the host opens again).
- `stop(reason)` stops the listener and releases the foreground service; the session and the
  bridge stay attached, so the next status still says `hostAvailable = true`.
- `close()` ends the session and stops the listener as well; the host is going away on purpose.
- Every method verifies that the caller is still the UID that opened the session.

Events reach the host through the oneway callback: `pairing_requested` and `client_paired` (with
the client name), `tool_call` (tool and client name, never arguments), and `warning` (a denied
pairing, a failed host probe, a foreground service the system refused to start).

## Host death

When AutoJs6 dies, `HostBridgeClient` fails every in-flight call with `HOST_UNAVAILABLE`, marks
itself `degraded`, and the listener keeps running: `device_ping` still works, host-backed tools
answer `HOST_UNAVAILABLE` with the hint to start AutoJs6 again, and the notification switches to
"Waiting for AutoJs6". The host's `McpServerSessionController` rebinds after 1 / 3 / 9 s and its
new `openServer` replaces the degraded session without touching the listener.

## Bridge client

`HostBridgeClient` is pure Kotlin over a `BridgeTransport` (Binder in production, a fake in the
JVM tests):

- Request ids are `mcp-1`, `mcp-2`, ... per client; the envelope is the host's `NodeBridgeRequest`
  JSON (`id`, `module`, `method`, `args`, `timeoutMs`, `permissions`) plus the MCP client name in
  the Bundle for the host's audit log.
- The per-call timeout is clamped to the grant's `maxTimeoutMs`; the client waits that long plus a
  2 s grace period before answering `TIMEOUT` on its own.
- At most `grantMaxConcurrentCalls` (<= 4) calls run at once; a further call waits up to 10 s (or
  its own timeout, whichever is shorter) for a slot and then fails with `LIMIT_EXCEEDED`.
- A method outside the grant's `grantMethods` is refused as `CAPABILITY_DENIED` without a dispatch.
- Payload descriptors (`bridgePayloadFd`) are closed on arrival; no P2.3 tool consumes them.

Host error categories map onto the plugin codes of appendix A.5:

| Host category | Plugin code |
|---|---|
| `process-dead`, `unavailable` | `HOST_UNAVAILABLE` (`A11Y_SERVICE_NOT_RUNNING` when the module is `accessibility`) |
| `capability-denied`, `permission-denied` | `CAPABILITY_DENIED` |
| `resource-limit` | `LIMIT_EXCEEDED` |
| `rate-limited` | `RATE_LIMITED` |
| `timeout` | `TIMEOUT` |
| `invalid-request` | `INVALID_ARGUMENTS` (a plugin-side code: the host rejected the request shape) |
| `provider-failed`, `runtime-error`, anything else | `HOST_ERROR` |

The text of a failed call is `CODE: message (hint)`; `structuredContent.error` repeats the code,
message, hint, host category, host code, module, and method.

## Tool catalog

`tools/ToolCatalog` is the single table of tools; `ToolCatalogTest` guards snake_case names,
closed schemas, D6 defaults, and a committed snapshot (`src/test/resources/tool-catalog.snapshot.json`).

| Tool | Group | Host method | Permissions | Timeout |
|---|---|---|---|---|
| `device_ping` | `device` | local | - | 5 s |
| `device_info` | `device` | `device.info` | `device` | 10 s |
| `script_run` | `script` | `engines.execScript` | `engines`, `engines.exec` | 60 s default, 300 s max |

- `ToolArguments.validate` checks required names, unknown names, primitive types, integer
  bounds, string length, and enumerations before anything reaches the host; violations answer
  `INVALID_ARGUMENTS`. The catalog schemas carry `additionalProperties: false`, but the SDK's
  `ToolSchema` (0.15.0) serializes only `type`, `properties`, `required`, and `$defs`, so the
  published `tools/list` schema lacks the keyword; unknown arguments are still refused here.
- A success carries the JSON result as text and as `structuredContent`.
- Group switches live in `tool_groups.json` (`ToolPermissionStore`, same atomic file mechanism as
  the other stores). `ToolRegistry` registers only the enabled rows on the SDK server; the
  `ToolGate` interceptor refreshes the registration before every `tools/list` / `tools/call`, so a
  toggle from the settings page takes effect on the next request and sends
  `notifications/tools/list_changed` to the open sessions. A call to a known but switched-off tool
  is answered by the gate with `isError` + `TOOL_DISABLED` before the SDK sees it.

`script_run` maps onto `engines.execScript(name, source, options)`; P3.1 reshaped its result and
added the rest of the script group (see `p3-tools.md`). What stays from P2.3:

- `name` is the task name AutoJs6 shows; the host's `StringScriptSource` is JavaScript by
  construction and appends `.js` itself, so a given `.js` suffix is dropped (the first manual run
  produced `demo.js.js`); default `mcp_script`.
- `options.timeoutMs` (the host's start timeout) is `min(timeoutMs, 60 s)`; `options.waitMs` is
  `timeoutMs` capped at 290 s when `waitForCompletion` (default), else 0; the bridge timeout is the
  larger of the two plus 10 s so a script that just finished still answers.
- `workingDirectory` becomes `options.cwd` unless it is `.`; `arguments` is forwarded when non-empty.
- With a `_meta.progressToken`, a progress notification goes out every 5 s while a host call is
  pending; the two run tools use 2 s and add the newest console line since P3.1.

## Foreground service

The notification shows the endpoint, whether AutoJs6 is attached, and the paired client count,
with a Stop action (a `PendingIntent` to `ACTION_STOP`); it is re-rendered when the listener
changes state, when a host session opens or closes, when the host dies, and when a client pairs.
When notifications are blocked, a toast names the endpoint once per service instance.

The runtime releases the service through `finishForeground()` once the listener is down. A start
command that arrives around that moment must win: one whose task is still queued or running
postpones the finish (the task finishes the service itself when the start fails), and one the
system accepted but has not delivered yet makes `stopSelfResult(lastStartId)` refuse the stop, in
which case the service re-enters the foreground for it. `onDestroy` stops a running listener only
when the runtime did not ask for the finish (`finishing == false`), which is the system killing
the service. Without these rules the spike tests' stop / start pairs lost the new listener to the
previous instance's `onDestroy`. `dumpsys activity service <pkg>/.McpServerService`
prints the status, the endpoints, the token fingerprint (the token only in developer mode), the
paired clients, the host session (id, UID, availability, broker id, granted method count, call
count), the tool group switches, and the registered tools.

## Evidence

See the roadmap session record of 2026-09-10 for the build and device evidence of P2.3 (JVM
tests, instrumentation on the Sony / Xiaomi devices, the host round trip through
`McpServerPluginRoundTripTest`, and the manual client checks).
