# P6 battery and residency: idle cost, foreground survival, idle auto-stop

Date: 2026-09-15. Plugin 1.0.0 (build 40 tree, committed as build 41), host AutoJs6 6.8.0
(5280, master with the lifecycle merge), MCP Kotlin SDK 0.15.0 on Ktor 3.5.1 CIO. Devices:
Sony XQ-AT72 (API 31, `QV710AF65F`), Xiaomi Pad 23046RP50C (API 35, HyperOS, `968e9f18`),
Redmi 22120RN86C (API 33, MIUI, `bek749scrwv4wo8h`). The Sony XQ-DQ72 (API 33) had no host
build installed any more when this session started and was left alone; the Sony G8441 (API 28)
cannot be driven through `uiautomator` (see the lifecycle matrix).

## What the roadmap item asked

"Idle CPU near zero (Ktor does not poll), the notification stays, an optional 'stop after N
idle minutes' (off by default), and the battery increment of one idle hour." Three of the four
are not what was assumed, which is the point of measuring:

1. Ktor CIO does poll. The listener's only periodic work is the selector loop of
   `ktor-network` 3.5.1 (`ActorSelectorManager.select`: `selector.select(500L)` followed by a
   coroutine `yield()`), which wakes twice a second for as long as the accepting socket is
   registered, whether or not a client is connected. The plugin adds no timer of its own while
   idle: the LAN reminder (24 h) and the pairing timeout (60 s while a request is pending) are
   delayed main-thread messages, the LAN address watcher is a connectivity callback, the rate
   limiter and the pairing gate keep state without timers, and the idle auto-stop below is one
   delayed message that never wakes the device.
2. The notification does not stay on HyperOS / MIUI by default. Both restrict the battery usage
   of sideloaded apps (`RUN_ANY_IN_BACKGROUND` = ignore, `ActivityManager.isBackgroundRestricted()`
   true), and Android stops the services of a background-restricted app, foreground service
   included, about a minute after its uid goes idle: `Stopping service due to app idle:
   u0a1024 -1m12s984ms .../.McpServerService` appeared on the pad 73 s after the screen went
   dark on (simulated) battery, the listener stopped with the `internal` reason ("the foreground
   service was destroyed") and the host closed its session. With the app's battery policy set
   to unrestricted the same listener survived the whole hour below.
3. The idle auto-stop exists now (settings page, off by default, 5 / 15 / 30 / 60 / 120 min).
4. The battery increment is recorded below as the `batterystats` estimate plus process CPU
   seconds, because a phone on a USB cable cannot show a real level drop.

## Idle auto-stop (roadmap P6, `ServerConfig.idleStopMinutes`)

- Setting: `idle_stop_minutes` in `config.json` (0 = off, at most 1440; the dialog offers 5,
  15, 30, 60 and 120). Like the LAN reminder it never restarts a running listener
  (`sameListener` ignores it); the runtime re-arms the check when the setting is applied.
- Activity: `installActivityMarker` sits behind the bearer check, so refused requests never
  count. An authenticated `POST` counts from its arrival to the end of its response (a
  `script_run` that streams progress for minutes is never idle), any other authenticated request
  (`GET` stream, `DELETE`) counts when it arrives. A client that keeps its `GET` stream open
  without sending requests, or a PC that went to sleep without closing it, therefore does not
  keep the listener alive; it gets a connection error on its next call and the user turns the
  switch on again.
- Mechanism: `IdleStopMonitor` (per listener, monotonic clock) plus one delayed main-thread
  message in `McpServerRuntime` armed after every start / configuration apply. When it fires
  with activity still inside the window it re-arms for the remainder; otherwise the lifecycle
  thread re-checks and stops the listener with the plugin-only reason `idle_timeout`
  (`ServerStatus.REASON_IDLE_TIMEOUT`, not a contract code). A delayed message does not wake
  a sleeping device, so a deadline that falls into deep sleep is honoured at the next wake-up.
- Effects: `userStopped` stays false (the plugin stopped itself, the user did not), so a host
  that starts later reopens the server as usual; a host that is attached receives `stopped`
  and closes its session like after the notification's Stop action (drawer switch off). One
  auto-cancelling notification (id `0x4D45`, the service channel) says "MCP Server stopped
  after N min without requests"; the settings page reads "Stopped automatically after being
  idle" while `status.json` carries the reason.
- Background restriction: the settings page shows a warning with a "Battery settings" button
  (app details page) when `isBackgroundRestricted()` is true, and the listener logs the warning
  and sends the host an `EVENT_WARNING` on every start under restriction.
- Fixed on the way: the stop sequence posted a "stopped" rendering of the foreground
  notification right before `stopForeground(STOP_FOREGROUND_REMOVE)`; on MIUI the queued
  notify landed after the removal and the notification stayed in the shade as a plain ongoing
  one. `refreshNotification` no longer renders stopping / stopped states and `finishForeground`
  cancels the id explicitly.

Tests: JVM `IdleStopMonitorTest` (3), `IdleStopInterceptorTest` (3), `ServerConfigTest`
(+4 assertions); device `McpServerIdleStopTest` (1 minute setting through the config store:
stop 61 s after the last request, `idle_timeout` in `status.json`, `userStopped` false, idle
notification present, foreground notification gone) and `McpServerSettingsTest` (2) on the
Redmi; the UI path below.

## Method

`build/tools/p6_battery.py <serial> [--minutes 60] [--assume-running] [--local-port N]`
(git-ignored evidence tooling): turns the listener on through the drawer (or takes a running
listener as is), records the pids, then `dumpsys battery unplug` (the battery service
believes the device is discharging while the cable stays in, so `batterystats` collects
discharge statistics), `dumpsys batterystats --reset`, screen off, and 60 minutes without any
request. Every 10 minutes it reads `/proc/<pid>/stat` (utime + stime) of the listener and the
host processes and the Doze state; at the end `dumpsys batterystats --checkin` for the plugin
uid, the per-package estimate, `dumpsys cpuinfo`, the foreground notification, `status.json`
and one unauthenticated probe (`401` = still listening). `dumpsys battery reset` restores the
charger state. Per-thread CPU (`/proc/<pid>/task/*/stat`, `build/p6/threads.sh`) over 90 s
showed where the idle time goes.

USB stays attached during the measurement, so the SoC never suspends and the 2 Hz selector
timer ticks the whole hour; on a phone on battery with the SoC suspended those wake-ups do not
happen until the next resume. The figures are therefore an upper bound for the listener's own
cost.

## Results

| | Sony XQ-AT72 (API 31) | Xiaomi Pad (API 35) |
| --- | --- | --- |
| Setup | drawer switch, host attached, 0 paired clients, no client connected, battery policy unrestricted | listener started from adb after the host had closed its session (the pad was locked with a pattern by then), host not attached, 1 paired client, no client connected, `RUN_ANY_IN_BACKGROUND` set to allow for the hour |
| Doze during the hour | light Doze from the first sample on (IDLE / IDLE_MAINTENANCE), deep Doze reached IDLE_PENDING after 40 min | deep Doze IDLE from the first sample on (one IDLE_MAINTENANCE window), light OVERRIDE |
| Listener CPU seconds (60 min) | 17.3 s (`/proc`), 16.8 s per `batterystats` (u 12.1 s + s 4.8 s); 4.3 s in the first 10 min (warm-up), then 2.5 to 2.8 s per 10 min, about 0.45% of one core | 38.3 s (`/proc` and `batterystats` agree: u 28.8 s + s 9.5 s); 6.2 to 6.9 s per 10 min throughout, about 1.05% of one core, 97% of it at the little cluster's second-lowest step (556.8 MHz) |
| Host CPU seconds (60 min) | 3.5 s (attached, idle) | 6.4 s (running in the background, not attached) |
| `batterystats` estimate for the plugin uid | `UID u0a5118: 0.0349 ( cpu=0.0349 )` mAh in 60 min, on a 3308 mAh capacity: about 0.001% of the battery per hour, all of it CPU; no wakelocks, alarms or jobs attributed to the uid | HyperOS prints no per-uid estimate in the package dump; the uid section shows the foreground service running for the whole hour and no wakelock, alarm or job entries |
| Listener alive after 60 min | yes, same pid (30479), host pid unchanged | yes, same pid (9981), with the battery policy set to unrestricted for the hour (with the default policy it was stopped after 73 s, see above) |
| Notification and endpoint after 60 min | notification "Listening on http://127.0.0.1:9637/mcp / AutoJs6 connected, Paired clients: 0", `status.json` running, probe `401` | notification "Listening on ... / Waiting for AutoJs6, Paired clients: 1", `status.json` running with `hostAvailable` false, probe `401` |

Per-thread view (API 31, 90 s, listener idle with the host attached): 43 ticks in total, all
on five `DefaultDispatcher-worker` threads (6 to 9 ticks each) plus 3 ticks of ART's profile
saver; the main thread, `mcp-server-lifecycle`, the Binder threads and everything else stayed
at 0. That is the selector loop above: `select(500 ms)` returns 0, `yield()` re-dispatches the
coroutine to another worker, and the loop selects again.

Listener CPU ticks (1 tick = 10 ms) per 10-minute sample:

| Minute | API 31 listener | API 31 host | Pad listener | Pad host |
| --- | --- | --- | --- | --- |
| 10 | 432 | 66 | 694 | 100 |
| 20 | 283 | 39 | 645 | 129 |
| 30 | 265 | 73 | 622 | 94 |
| 40 | 247 | 51 | 624 | 113 |
| 50 | 245 | 62 | 630 | 90 |
| 60 | 257 | 58 | 610 | 111 |

`dumpsys cpuinfo` at the end: 0.4% (API 31) and 1% (pad) for the listener process over the
last minutes, 0% and 0.1% for the host.

## The idle stop through the real UI (Redmi, API 33, MIUI)

`build/tools/p6_idle_ui.py bek749scrwv4wo8h` on the Redmi (host 5280, plugin build 40 with
the fixes above, English locale):

| Step | Observation |
| --- | --- |
| Settings page | "Stop automatically when idle: off" under the Stop button; the choice dialog offers Off / 5 / 15 / 30 / 60 / 120 minutes; after choosing 5 the button reads "Stop automatically when idle: after 5 min without requests". The battery row was hidden because `RUN_ANY_IN_BACKGROUND` had flipped back to the default (allow) after the reinstall; during the instrumentation run earlier the same device had it at ignore and the listener logged the warning |
| Drawer switch on | endpoint shown, listener pid 27065, `hostAvailable` true, notification "Listening on ... AutoJs6 connected, Paired clients: 0" |
| No request at all | `status.json` reached `stopped` with `idle_timeout` 303.7 s after the switch (polled every 15 s); the listener process stayed alive without its service; the foreground notification was gone and one notification read "MCP Server stopped after 5 min without requests. Turn the switch on in AutoJs6 when you need it again." |
| Host | the drawer row read "Stopped" with the switch off (the host closed its session on the `stopped` status, as after the notification's Stop action) |
| Settings page | state text "Stopped automatically after being idle"; the option was set back to off at the end |

| Image | File |
| --- | --- |
| The choice dialog (Redmi, API 33) | [dialog-idle-stop-choices-api33](images/p6/dialog-idle-stop-choices-api33.png) |
| Connection card after the idle stop (Redmi, API 33) | [settings-stopped-idle-api33](images/p6/settings-stopped-idle-api33.png) |
| Drawer row after the idle stop (Redmi, API 33) | [drawer-after-idle-stop-api33](images/p6/drawer-after-idle-stop-api33.png) |

## Notes

- `dumpsys battery unplug` on the pad made the uid idle within about a minute and, with the
  default HyperOS policy, cost the foreground service (the `Stopping service due to app idle`
  line above); this is the ordinary "screen off on battery" situation for a HyperOS user, not
  a test artefact. The remedy is the app's battery policy ("No restrictions"), which the
  settings page now points at. `appops set <pkg> RUN_ANY_IN_BACKGROUND allow` was used for the
  measurement and reverted to `ignore` afterwards.
- The `Long monitor contention ... McpHttpServer.getActiveConfiguration()` warnings in the
  Redmi log come from the main thread rendering the notification while the lifecycle thread
  binds or stops the listener under the same lock (about 1.2 s on that device); pre-existing,
  not changed here.
- The pad got locked (pattern) by the first, aborted measurement attempt (screen off); the
  drawer could not be driven afterwards, hence the adb-started listener without a host session.
