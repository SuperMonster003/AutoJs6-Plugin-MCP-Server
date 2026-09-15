# P6 lifecycle matrix: expected states and recovery paths

Date: 2026-09-15. Plugin 1.0.0 (build 35 tree, committed as build 36); host
AutoJs6 6.8.0 (5280) built from the host branch `mcp-p6-lifecycle` (commit
89347f22b, the force-stop fix below), MCP Kotlin SDK 0.15.0 on Ktor 3.5.1 CIO. Devices: Sony
XQ-DQ72 (API 33, `QV770340J7`), Sony XQ-AT72 (API 31, `QV710AF65F`, installed
with `--user 0`), Xiaomi Pad 23046RP50C (API 35, `968e9f18`). Sony G8441 (API
28) could not be driven: its `uiautomator dump` returned "null root node" for
the whole session (two third-party accessibility services are enabled there),
so the drawer could not be operated from adb.

## Merge into host master

On 2026-09-15, host `master` merged `mcp-p6-lifecycle` with merge commit
`7c31269cc` (parents `541d15ffb` and `89347f22b`). The merge had no conflicts.
All ten changelog sources retain the later master changes and append exactly
the lifecycle fix from the branch; regenerating the host README and changelog
files produced no further changes.

Validation on the merged tree:

- `:app:testAppDebugUnitTest` filtered to the MCP package and
  `PluginCenterMcpServerRegistrationTest`: 21 tests, no failures or skips.
- `:app:assembleAppDebug`: passed with AutoJs6 6.8.0 (5280).
- `:app:connectedAppDebugAndroidTest` filtered to
  `McpServerPluginRoundTripTest` on Sony XQ-DQ72 (API 33): 1 test, no failures
  or skips; the real Binder session reached running with the host attached,
  then stopped and closed cleanly.
- `git diff --check`: passed. The branch is an ancestor of `master`, and the
  host working tree is clean after the merge commit.

The full lifecycle matrix below is the earlier branch validation; it was not
repeated for this conflict-free merge.

## Method

`build/tools/p6_lifecycle.py <serial>` (git-ignored evidence tooling, on top
of the P4 / P5 helpers) runs the six scenarios in one session per device:

- Setup: the drawer switch turns the listener on, the token comes from the
  settings page (the clipboard evidence test of P4), a PC client
  (`mcp_client.py`, `clientInfo.name` `p6-lifecycle`) initializes, pairs
  through the phone prompt and calls `device_ping` (answered in the plugin)
  and `device_info` (answered through the host broker).
- Observation after every step: the `:mcp_server` and host pids, an
  unauthenticated POST to the endpoint (`401` means listening), `status.json`
  through `run-as` (`statusState`, `processId`, `statusHostAvailable`), the
  foreground notification text and the pairing notifications from `dumpsys
  notification`, the drawer subtitle, and the two tool calls on the client's
  current session.
- Kills: the host with `am force-stop org.autojs.autojs6`; the listener
  process from inside the plugin's own UID through the instrumentation helper
  `McpServerLifecycleTest#killListenerProcess` (`am instrument -e
  mcpKillListener true`), because `run-as <pkg> kill` returns success without
  a signal on Sony builds and `am crash` leaves a "keeps stopping" dialog
  behind; the plugin package with `am force-stop <pkg>` (what the system
  settings' Force stop button does).
- Outputs: `build/p6/lifecycle-<serial>.log` / `.json` (token redacted) and
  screenshots.

## Matrix

| # | Scenario | Expected state | Recovery path | Verified |
| --- | --- | --- | --- | --- |
| S1 | Host killed while a client session is live | Listener keeps running with `hostAvailable = false`; notification "Waiting for AutoJs6"; `device_ping` answers, host-backed tools answer `HOST_UNAVAILABLE`; the drawer is gone with the host | The host starts, `connectIfNotNormallyClosed` reopens the server, the plugin replaces the degraded session (same listener process, same client session, no re-initialize) | API 33, 31, 35 |
| S2 | Listener process killed while the host is attached | The host's death recipient fires; rebind after 1 s (then 3 s, 9 s); a new listener process serves the same endpoint; the drawer keeps showing the endpoint; the paired clients are unchanged | The client's next request on its old session id gets `404` "Session not found" and initializes again; no pairing prompt (the fix below) | API 33, 31, 35 |
| S3 | Host and listener killed at once | Nothing runs and nothing restarts on its own (no boot receiver, no sticky service); the endpoint refuses connections | The host starts and reopens the server as in S1; the client initializes again | API 33, 31, 35 |
| S4 | The user force-stops the plugin in the system settings | The listener stays dead; the host does not rebind (the fix below); the drawer switch turns off and its subtitle reads "Activation required" | The user turns the switch on: the activation guide starts the plugin explicitly, the listener comes back, the client initializes again | API 33, 31, 35 |
| S5 | Token rotated from the settings page while a client session is live | The listener restarts with the new token; the old token gets `401` / `UNAUTHORIZED`; the old session id gets `404` with the new token; pairing survives (it is keyed by client name and address class, not by the token) | The client updates its token and initializes again; no pairing prompt (the fix below) | API 33, 35 (API 31: the settings page could not be driven, see below) |
| S6a | Pairing pending, then the host is killed | The pending request lives in the listener process and survives; the prompt can still be answered | Allow pairs the client; `device_ping` works at once, host-backed tools answer `HOST_UNAVAILABLE` until the host is back | API 33, 31, 35 |
| S6b | Pairing pending, then the listener process is killed | The pending request dies with the process; its notification is cleared when the new listener starts (the fix below); the client is not paired | The client initializes again, the prompt appears again, Allow pairs it | API 33, 31, 35 |

## Findings and fixes

1. Stale session ids raised pairing prompts (S2, S5). The pairing gate
   resolved the identity of a request whose `Mcp-Session-Id` belonged to the
   dead listener from the `User-Agent` header (there was no session to read
   `clientInfo` from), so an already paired client got `PAIRING_REQUIRED` for
   "p6-lifecycle/1.0" and a pairing notification appeared on the phone,
   instead of the `404` that tells the client to initialize again.
   `installPairingGate` now leaves a request with an unknown session id to
   the transport (`Server.hasSession`), which answers `404` "Session not
   found"; JVM `PairingInterceptorTest` covers it.
2. Pairing notifications outlived the listener process (S6b). The requests
   die with the process but their notifications stay in the shade with
   actions that no longer reach anything. `PairingCoordinator` now cancels
   the notifications of its tag when it is created.
3. The host resurrected a force-stopped plugin (S4). `am force-stop` sets the
   package's stopped state before it kills the processes; the host's death
   recipient then rebound after 1 s and an explicit `bindService` starts a
   stopped package, so the plugin was back within about 1.2 s against the
   user's decision. `McpServerSessionController` (host) now inspects the
   plugin before a rebind and ends the session (`stopped`, reason
   `plugin_died`) when it is no longer available; `McpServerOwner` shows the
   inspected availability afterwards, so the drawer reads "Activation
   required" like a fresh install and the switch reopens the plugin through
   the existing activation guide. Host branch `mcp-p6-lifecycle`.

## Evidence, Sony XQ-DQ72 (API 33)

| Step | Observation |
| --- | --- |
| S1 host killed | `statusHostAvailable` false 56 ms after `am force-stop`; notification "Listening on http://127.0.0.1:9637/mcp / Waiting for AutoJs6, Paired clients: 1"; `device_ping` ok, `device_info` "HOST_UNAVAILABLE: AutoJs6 stopped; the MCP server is waiting for it to come back"; after the host started, `hostAvailable` true 73 ms after the drawer opened; same listener pid, same client session, both tools ok |
| S2 listener killed | killed by the instrumentation helper; endpoint answering from a new pid after 1192 ms, host reattached 208 ms later (2076 ms in total); old session: `-32000` "Session not found"; re-initialize 138 ms, both tools ok; no pairing notification; `paired_clients.json` unchanged; drawer still shows the endpoint |
| S3 both killed | 6 s later: no host, no listener, endpoint down (connection refused), calls fail at the socket; after `am start` of the host the endpoint answered with the host attached 154 ms after the drawer opened; re-initialize ok |
| S4 force-stop | listener absent in all 19 samples of the 20 s window; drawer subtitle "需要激活" (Activation required), switch off; switching on showed the guide "MCP Server 已安装, 但尚未被 Android 激活 ..." and reached the endpoint 24.7 s later (automation time included); listener answering with the host attached 158 ms after that; re-initialize ok |
| S5 token rotated | token fingerprint changed; old token `-32001` "UNAUTHORIZED: the bearer token is not accepted"; old session id with the new token `-32000` "Session not found"; new session initialize 94 ms, both tools ok; no pairing notification |
| S6a pending, host killed | 1 pairing notification before and after the host kill; Allow with the host dead: paired, `device_ping` ok, `device_info` HOST_UNAVAILABLE; after the host returned both ok |
| S6b pending, listener killed | 1 pairing notification before the kill, 0 after the new listener came up (1145 ms); initialize again + Allow: paired, both tools ok |

Before the fixes (same device, build 35 without them): S2 and S5 answered the
old session with `PAIRING_REQUIRED: confirm "p6-lifecycle/1.0"` and left a
pairing notification for 60 s; S6b showed 2 stale pairing notifications after
the restart; S4 had the listener back after 1.2 s with the drawer still
showing the endpoint.

## Evidence, other devices

| Step | Sony XQ-AT72 (API 31) | Xiaomi Pad (API 35) |
| --- | --- | --- |
| Setup pairing | confirmation dialog | notification action, after expanding the grouped card and the child notification (HyperOS hides the action row until then; the dialog is blocked as a background activity start) |
| S1 host killed | `hostAvailable` false after 69 ms; `device_ping` ok, `device_info` HOST_UNAVAILABLE; host back: reattached 79 ms after the drawer opened; same listener pid and client session, both tools ok | 95 ms; same answers; reattached 68 ms; same session |
| S2 listener killed | new listener answering 736 ms after the kill, host reattached 274 ms later; old session `404` "Session not found"; re-initialize 275 ms, both tools ok; no pairing notification; paired file unchanged | 1693 ms and 219 ms; `404`; re-initialize 185 ms; no pairing notification; paired file unchanged |
| S3 both killed | nothing back within 6 s, endpoint refused; after the host start: endpoint with the host attached 289 ms after the drawer opened; re-initialize ok | 204 ms; re-initialize ok |
| S4 force-stop | listener absent in all 19 samples of 20 s; drawer "Activation required", switch off; switching on showed the activation guide ("CANCEL" / "ACTIVATE") and reached the endpoint after 40.2 s (automation time, slowed by the dump failures below); listener with the host 318 ms later; re-initialize ok | 19 samples, absent; "需要激活"; guide accepted, endpoint after 29.0 s; 215 ms; re-initialize ok |
| S5 token rotated | not driven: `uiautomator dump` answered "could not get idle state" for the rest of the session (the status bar's traffic indicator keeps repainting), so the harness could not open the settings page; the plugin-side answers are the same code path as on API 33 / 35 | fingerprint changed; old token `401` / `-32001` UNAUTHORIZED; old session id with the new token `404`; new session initialize 201 ms, both tools ok; no pairing notification |
| S6a pending, host killed | 1 pairing notification before and after the host kill; Allow (dialog) with the host dead: paired, `device_ping` ok, `device_info` HOST_UNAVAILABLE; both ok after the host returned | same, Allow through the expanded notification action |
| S6b pending, listener killed | 0 stale notifications after the new listener (702 ms); initialize again + Allow: paired, both tools ok | 0 stale notifications (828 ms); paired again after 20.8 s of notification handling |

## Notes

- `status.json` records the writer's pid; the settings process treats a file
  whose pid is gone as `stopped`, so the "running" snapshot a killed listener
  leaves behind never shows as running (S3, S4).
- A token rotation restarts the listener, so every session is gone; clients
  see `401` until they use the new token and `404` until they initialize
  again. Pairing is not touched by the rotation.
- The host's rebind schedule (1 s, 3 s, 9 s, then `stopped` with reason
  `plugin_died`) applies to crashes and kills; a package that is stopped,
  disabled or uninstalled after the death ends the session at once.
- The pending pairing request is process state: it survives a host kill (S6a)
  and is lost with the listener (S6b); the 60 s window is not persisted on
  purpose (a restart should not carry an unanswered request from an earlier
  process).
- Sony API 31 (and API 28 on that day) could not be driven through
  `uiautomator dump` for long stretches ("could not get idle state" / "null
  root node"); the harness now discards a failed dump instead of reusing the
  previous hierarchy, which had made it act on a stale screen.
- The pad (API 35) blocks the confirmation activity started from the service
  (`Background activity launch blocked`); the notification is the way in, and
  MIUI keeps the action row hidden until the notification is expanded, so the
  harness taps the notification body (its content intent opens the dialog).

| Image | File |
| --- | --- |
| Drawer after the plugin was force-stopped (API 33) | [drawer-force-stopped-api33](images/p6/drawer-force-stopped-api33.png) |
| Activation guide when the switch is turned on again (API 33) | [dialog-activate-after-force-stop-api33](images/p6/dialog-activate-after-force-stop-api33.png) |
| Drawer after the listener process was killed and rebound (API 33) | [drawer-after-listener-kill-api33](images/p6/drawer-after-listener-kill-api33.png) |
