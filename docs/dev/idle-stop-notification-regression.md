# Idle stop with and without notification permission

`McpServerIdleStopTest` now checks the notification outcome against Android's actual notification
permission. It does not grant permission or skip the idle-stop assertions. A fresh API 33+ install
without POST_NOTIFICATIONS must still stop the listener and record `idle_timeout`; no idle notice
is expected. When notifications are enabled, the idle notice is required. Both paths require
`userStopped == false` and removal of the foreground notification. HTTP setup has bounded connect
and read timeouts so a stalled request cannot hang the test indefinitely.

## Device evidence, 2026-09-16

- API 37 x86_64 emulator, fresh plugin install without notification permission: passed.
- Same emulator after granting POST_NOTIFICATIONS: passed.
- Both runs measured 60 seconds from the authenticated request to idle stop.
- The full test runs took 66.811 and 62.923 seconds respectively.
- The notification grant used for the second run was revoked afterward.
- Full JVM suite: 233/233 passed, with no skips.
- Debug/instrumentation assembly and `lintDebug` passed. Runtime implementation and public API
  are unchanged.

## Reproduce on a test device

Build and install matching debug and instrumentation APKs first. Run the following command once
with notifications denied and once with notifications allowed:

```powershell
adb -s <serial> shell am instrument -w -r `
  -e class io.github.supermonster003.autojs6.plugin.mcp.server.McpServerIdleStopTest `
  io.github.supermonster003.autojs6.plugin.mcp.server.test/androidx.test.runner.AndroidJUnitRunner
```

On API 33+, configure the test device before each invocation with `adb -s <serial> shell pm grant`
or `pm revoke`, followed by `io.github.supermonster003.autojs6.plugin.mcp.server` and
`android.permission.POST_NOTIFICATIONS`. Permission revocation must happen outside the running
instrumentation process. Restore the device's original permission after testing.

The test prints `MCP_IDLE_STOP notificationsEnabled=<boolean> elapsedSeconds=<seconds>` to logcat.
Local logs are under `build/verification/plugin-test-repair/` and remain ignored.
