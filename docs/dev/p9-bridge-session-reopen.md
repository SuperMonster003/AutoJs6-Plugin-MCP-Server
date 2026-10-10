# P9: the stdio bridge across a listener restart

Date: 2026-10-11. Bridge `autojs6-mcp-bridge` 0.1.1 (local, commit `fac564f` in AutoJs6-MCP-Bridge,
not yet on npm), plugin 1.0.3 (build 84), host AutoJs6 6.8.0 / 5328.

## Problem

The phone's MCP session ends whenever its listener restarts: the drawer switch, a port or network
change, a plugin update, or the plugin process being restarted. The plugin then answers
`404 Session not found` for the old session id, as the Streamable HTTP transport specifies, and a
client is expected to start a new session with `initialize`. A stdio client such as Claude Desktop
initializes once per server start and knows nothing about the HTTP session, so the bridge has to
do it. Bridge 0.1.0 kept the old session id and answered every later request with a JSON-RPC
error that blamed the endpoint path (`404 Not Found from ...: the endpoint path is wrong; the plugin
serves /mcp.`), until the server was restarted in the client. This was first observed while the
SSE resumption change was verified (docs/dev/p9-sse-resumption.md).

## Change (bridge 0.1.1)

- When a message that carried a session id gets `404`, the bridge opens a new session on a fresh
  transport: it repeats the client's `initialize` (with the protocol it requested, `--protocol`
  applied) under an internal request id, keeps the answer from the client, records the negotiated
  protocol for the `MCP-Protocol-Version` header, sends `notifications/initialized` if the client
  had sent it, and then sends the refused message again. The phone had refused that message
  without acting on it. Answers to server requests of the ended session are dropped.
- Concurrent failures share one reopen; a failed reopen is answered with
  `The phone's MCP session ended (its listener restarted or was switched off) and the bridge could
  not open a new one: <reason>`, and the next request tries again.
- The new session carries the same client name, so the pairing on the phone still applies. The
  client receives `notifications/tools/list_changed` (and the prompts / resources variants the
  phone declares), since the phone may have been updated while it restarted.
- A `404` with `Session not found` in its body is described as an ended session; any other `404`
  mentions both a wrong path and an ended session. The old transport is closed, so its event
  stream stops reconnecting.
- Limit: a request that was still running when the listener stopped is lost with it; the client's
  own timeout applies.

Tests: `npm test` 26 / 26 (new: a fake phone that forgets its sessions; the reopen with the
client's protocol, `clientInfo`, `notifications/initialized`, one reopen for two concurrent
requests, the new session id and protocol header on the retried request, the list-changed
notifications, nothing internal forwarded to the client, `DELETE` of the new session on close; a
reopen refused with `503` and the next request succeeding; the error texts).

## Device evidence

`py tools/evidence/bridge_reopen.py --serial QV710AF65F --bridge "node <0.1.0 build>/dist/cli.js"
--bridge "node <bridge repo>/dist/cli.js"` on Sony XQ-AT72 (Android 12, API 31) with the listener
switched on in the AutoJs6 drawer, protocol 2025-11-25, the bridge reached over `adb forward`. The
0.1.0 build was compiled from the bridge repository's previous `HEAD` (the published 0.1.0 sources).
The listener restart is an `am instrument` run of `EvidenceHelperTest#pairClient`, which restarts
the plugin package the way an update does; the host rebinds and a new listener process starts
(about 4.1 s).

| Step | Bridge 0.1.0 | Bridge 0.1.1 |
| --- | --- | --- |
| `initialize` | ok, 2025-11-25, server 1.0.3, `tools.listChanged` | same |
| `device_ping` before the restart | ok (77 ms) | ok (55 ms) |
| First `device_ping` after the restart | error -32000 blaming the endpoint path (140 ms) | ok (339 ms, including the new session); `notifications/tools/list_changed` before the answer |
| Second `device_ping` after the restart | error -32000 blaming the endpoint path | ok (60 ms) |
| Bridge log | `404 Not Found ...: the endpoint path is wrong` | `the phone no longer knows session <id> ...`, then `opened session <id> with the client's initialize` |

The report (`build/evidence/`, git-ignored) holds statuses, timings, error classes and what the log
mentions, never the token, session ids or log lines. The evidence pairing was revoked and the
listener came back with the host attached.

Found on the way: a plain `am force-stop` of the plugin leaves the server off, because the host
treats it as the user's decision (`McpServerSessionController.stopForUnavailablePluginLocked`, P6
lifecycle matrix); the drawer switch had to be turned on again. `run-as <plugin> kill` is refused
by SELinux on Android 12. The scenario therefore uses the instrumentation restart.

A rerun with the build of `90005cc` (`@modelcontextprotocol/sdk` 1.32.1, Node.js 22 required) gave
the same result: the first `device_ping` after the restart succeeded in 354 ms including the new
session, with `notifications/tools/list_changed` before the answer.

## Remaining

Pushing the bridge commits, the first CI run on GitHub and publishing 0.1.1 to npm need the
maintainer (npm two-factor authentication); until then
Claude Desktop setups that run `npx autojs6-mcp-bridge` keep 0.1.0 and need the server restarted in
the client after a listener restart.
