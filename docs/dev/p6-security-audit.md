# P6 security audit: the checklist, where each rule lives, and how it was verified

Date: 2026-09-16. Plugin 1.0.1 (build 44 tree plus the cleartext scope change below, committed
as build 45), host AutoJs6 6.8.0 (5280). Devices: Sony XQ-DQ72 (API 33, `QV770340J7`) and
Xiaomi Pad 23046RP50C (API 35, HyperOS, `968e9f18`), host attached through the drawer switch,
debug build with the instrumentation APK (the token read and `run-as` need it). Evidence
tooling (git-ignored): `build/tools/p6_security.py <serial> [--local-port N]`, which writes
`build/p6s/security-<serial>.log` / `.json`, the redacted `logcat-<serial>.txt` and screenshots,
and `aapt2 dump xmltree` of the built APK's manifest (`build/p6s/manifest-debug.txt`).

The roadmap item lists seven rules. Each row below names the rule, where it is implemented,
how it is verified (JVM tests, the compiled manifest, or the device run) and what was found.
The README "Permissions and Security" section (`.readme/lang_*.json`, `security_points`) now
states the same boundaries for users in ten languages.

## Threat model in one paragraph

The endpoint is a local HTTP server that can drive the phone through AutoJs6. Everything a
client can do runs through the host's capability broker under the host's own permissions, so
the plugin's job is to make sure that only a client the user admitted can reach that broker,
that nothing secret leaks through logs, backups or the UI, and that the dangerous tool groups
are off until the user turns them on. A PC with an authorised adb connection is trusted: it
can already read the screen, install apps and, on a debug build, read the plugin's private
files; the DUMP-protected start / stop switch and the developer-mode token dump exist for that
operator on purpose. A third-party app on the phone is not trusted and cannot reach any
component, the token or the endpoint's tools without pairing.

## Checklist

| # | Rule | Implementation | Verification | Result |
| --- | --- | --- | --- | --- |
| 1 | Token storage: generated with a secure random source, never stored in clear when the Keystore works, never backed up, never shown in full without intent | `BearerTokens.generate` (SecureRandom, 32 bytes, base64url), `TokenStore` (AES-GCM key in the Android Keystore, one JSON document in `no_backup/mcp_server/token.json` written through `ProcessSharedFile` with a lock and an atomic rename; Keystore failure falls back to the private file with `wrapped: false` and a log warning), manifest `allowBackup="false"`, `fullBackupContent="false"`, `dataExtractionRules` excluding every domain for cloud backup and device transfer; settings page shows `Hidden, ending in xxxx`, the full-token and client-snippet dialogs carry `FLAG_SECURE`, disable autofill and content capture and clear the text on dismiss, clipboard copies are marked `EXTRA_IS_SENSITIVE` on API 33+ | JVM `BearerTokensTest` / `TokenStoreTest`; device: `token.json` has keys `created_at, format, iv, token, wrapped` with `wrapped: true` and an 80-character base64 ciphertext on both devices (a 43-character token in clear would be recognisable), file mode `-rw-------` owned by the plugin uid; package flags on both devices contain no `ALLOW_BACKUP`; `aapt2` shows `allowBackup=false` | pass |
| 2 | Log redaction: the token, request bodies, file contents and screenshots never reach the log | Every `Log.*` call in the plugin was read (41 statements in 14 files): they log tool names, client names (normalised, capped), token fingerprints (`sha256` prefix, `BearerTokens.fingerprint`), endpoint, state and exception class names. The MCP SDK logs through kotlin-logging without a binding that prints bodies; Ktor call logging is not installed. `CatalogToolExecutor` logs a failed tool's exception with its stack trace: messages may name a path, never a file body | Device: `logcat -c`, then `device_info`, `device_ensure_accessibility`, `files_write` of a file containing a random marker, `files_read` (returned the marker), `screen_capture` (31,148 / 22,756 base64 characters returned), `shell_exec`, `files_delete`, `tools/list`; then the whole `logcat -d` (198 / 187 lines including the host's) scanned for the token (0), the marker (0), `Authorization` / `Bearer` (0), base64 runs of 200+ characters (0) and the probe file name (0). The plugin's own lines were five `tool_call <name> by p6-security` entries | pass |
| 3 | Exported components kept to the minimum | Manifest: `WakeActivity`, `McpServerSettingsActivity`, `McpServerPluginInfoService` and `McpServerPluginService` exported behind the host's signature permission `org.autojs.permission.PLUGIN`; `McpServerService` exported behind `android.permission.DUMP` (adb start / stop switch, see threat model); `PairingConfirmActivity`, `PairingDecisionReceiver`, `McpSettingsReceiver`, `ReleaseHistoryActivity` not exported; no content provider | Compiled manifest (`aapt2 dump xmltree`, listed below); device: explicit intents from the adb shell (uid 2000, which holds more permissions than any third-party app) to all nine components on both devices: eight `Permission Denial` lines in logcat (the four PLUGIN-protected components, the two receivers, the two activities that are not exported; `am` prints "Starting" before the system refuses), and only the `STOP_SERVER` intent to the listener service was delivered | pass |
| 4 | `usesCleartextTraffic` scope stated | The listener is plain HTTP on loopback or, after opt-in, on the LAN. The Android cleartext policy governs client libraries, not server sockets, so the flag was never needed for serving; the plugin opens no client connection (no `HttpURLConnection`, OkHttp or Ktor client in `main`). The app-wide `usesCleartextTraffic="true"` is replaced by `@xml/network_security_config`: `base-config` refuses cleartext, a `domain-config` permits it for `localhost` and `127.0.0.1` only, which is what the plugin's instrumentation tests use | Manifest and config in this commit; instrumentation test with `HttpURLConnection` to `127.0.0.1` re-run on the Sony after the change (see below); no `androidTest` connects to a non-loopback address | pass (scope narrowed) |
| 5 | Local network off by default | `ServerConfig.bindScope` defaults to `LOOPBACK` (`127.0.0.1`); `GatePolicy.loopback` admits only `localhost` / `127.0.0.1` / `[::1]` as `Host`; LAN mode adds the current IPv4 addresses (refreshed on network change) and optional extra host names, keeps the token, the pairing gate (LAN clients are a separate address class) and the rate limits, and posts a 24-hour reminder notification while it stays on | JVM `ServerConfigTest`, `RequestGateTest`, `McpTransportTest.lanAddressesAreHonouredAfterARefreshWithoutRestarting`; device: no `config.json` on the phone (`{}` on the pad), `ss -ltnp` shows one socket `[::ffff:127.0.0.1]:9637` on both, a PC probe to the phone's Wi-Fi address on port 9637 was unreachable | pass |
| 6 | Pairing revocation | Settings page "Revoke pairing" per client and "Revoke all pairings" (`PairedClientStore.remove` / `clear`, `PairingGate.revoke`), notification refresh through `McpSettingsReceiver`; a revoked client's next gated request answers `-32002 PAIRING_REQUIRED` and opens a new prompt; token rotation keeps pairings but cuts off every client that still sends the old token | JVM `PairingGateTest`, `PairingInterceptorTest`; device: after tapping "Revoke pairing for p6-security" and confirming, `paired_clients.json` no longer contains the client, `device_ping` answered `-32002` and one pairing prompt was pending; token rotation under a live session is in the lifecycle matrix (S5) | pass |
| 7 | Tool groups off by default | `ToolGroup`: `ui_gesture`, `files_delete` and `shell` default to off; `shell_exec` with `root: true` additionally needs `allowShellRoot` and the host's `shell.root` grant; disabled groups vanish from `tools/list` and answer `TOOL_DISABLED` | JVM `ToolPermissionsTest`, `ToolGateInterceptorTest`; device: `tools/list` returned 33 of 37 tools, `shell_exec` and `files_delete` answered `TOOL_DISABLED: tool ... is switched off by the ... group`, no `tool_groups.json` existed (defaults) | pass |

## Compiled manifest (debug build 43, `aapt2 dump xmltree`)

| Component | exported | permission | process |
| --- | --- | --- | --- |
| `WakeActivity` | true | `org.autojs.permission.PLUGIN` | main |
| `ui.McpServerSettingsActivity` | true | `org.autojs.permission.PLUGIN` | main |
| `ui.ReleaseHistoryActivity` | false | | main |
| `ui.McpSettingsReceiver` | false | | `:mcp_server` |
| `McpServerPluginInfoService` | true | `org.autojs.permission.PLUGIN` | main |
| `McpServerPluginService` | true | `org.autojs.permission.PLUGIN` | `:mcp_server` |
| `ui.PairingConfirmActivity` | false | | `:mcp_server` |
| `ui.PairingDecisionReceiver` | false | | `:mcp_server` |
| `McpServerService` | true | `android.permission.DUMP` | `:mcp_server` |

Application attributes: `allowBackup=false`, `debuggable=true` (debug build only; the release
build is minified and not debuggable, so `run-as` and the developer-mode token dump are not
available there), `usesCleartextTraffic=true` in build 43, replaced by the network security
configuration from build 45. Permissions requested: `org.autojs.permission.PLUGIN`, `INTERNET`,
`ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
`POST_NOTIFICATIONS`; `<queries>` names the host package only.

## Device run

Both runs followed the same script. Figures are Sony / pad.

- Package flags: `DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA`, no `ALLOW_BACKUP`.
- Component probes: 9 explicit intents, 8 refused in logcat, the `STOP_SERVER` intent delivered
  (the listener was off, nothing happened). On API 35 the receiver refusals read as the
  broadcast delivery state instead of `Permission Denial`.
- Listener on through the drawer, unauthenticated probe `401`; `token.json` shape as in row 1;
  `ss -ltnp`: `LISTEN [::ffff:127.0.0.1]:9637`; LAN probe: unreachable on the phone (`URLError`),
  no Wi-Fi address on the pad.
- Pairing of `p6-security` through the real prompt (dialog on the phone, expanded notification
  action on the pad).
- Calls and logcat scan as in row 2: all counters 0; the probe file was written to the host's
  `Scripts` directory and removed afterwards; `screen_capture` used the accessibility path after
  `device_ensure_accessibility` (the accessibility setting was restored at the end).
- `shell_exec` and `files_delete`: `TOOL_DISABLED`; `tools/list`: 33 tools.
- Revocation from the settings page: client gone from the file, `device_ping` `-32002`, one new
  prompt pending (left to time out: `PAIRING_DENIED` for 30 s afterwards).
- Cleanup: forward removed, drawer switch off, paired-client file restored (pad) or removed
  (phone), accessibility setting restored.

## Findings and notes

1. Cleartext scope narrowed (row 4). Before this commit the app-wide flag allowed cleartext to
   any host for any in-process client; nothing used it beyond loopback, so the configuration
   now says so and enforces it. LAN clients connect *to* the phone; the phone never connects to
   them, so the LAN mode is unaffected.
2. The `am` command reports "Starting: Intent" for a refused activity; the refusal is only
   visible in logcat (`ActivityTaskManager: Permission Denial`). Anyone reproducing the probe
   must read logcat, not the command's exit status.
3. Failure logging: `CatalogToolExecutor` logs the exception of a failed tool with its stack
   trace. Host-side failure messages can contain a workspace path (for example a missing file);
   they never contain file contents, and the scan found no path of the probe file in this run.
   Left as is.
4. The developer-mode token dump (`dumpsys activity service` prints the token when the
   listener was started with `developer_mode` on) and the DUMP-protected service are the adb
   operator's tools; they are the reason the README tells users to keep developer mode off
   unless a browser page needs the endpoint.
5. Not in scope here and unchanged: the rate limits (P6 rate limits), the request body caps
   and hostile-input handling (P6 hostile input), the conformance findings (P6 conformance).
