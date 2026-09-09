# P2.2 auth and pairing: developer notes

Roadmap P2.2 puts two interceptors between the request gate (P2.1) and the SDK transport:

1. `server/AuthInterceptor`: every request must carry `Authorization: Bearer <token>`. A missing,
   malformed, or wrong token answers `401` with `WWW-Authenticate: Bearer realm="autojs6-mcp-server"`
   and a JSON-RPC error `-32001` (`UNAUTHORIZED`). The comparison runs over SHA-256 digests in
   constant time and fails closed when no token exists.
2. `server/PairingInterceptor`: a POST whose body contains a gated method (`tools/call`,
   `resources/read`, `resources/subscribe`, `prompts/get`) is checked against the `PairingGate`
   before it reaches the SDK. Everything else (`initialize`, `ping`, the `list` methods, notifications)
   passes through, so a client can discover the server before the user confirms it.

## Storage

All three documents live in `noBackupFilesDir/mcp_server/` and are handled by `store/ProcessSharedFile`:
read on every access, replaced through a temporary file plus rename, and serialized with a lock file.
`SharedPreferences` were dropped on purpose: they cache per process, and the `:mcp_server` process would
never see a value the settings page (main process) wrote while the listener was alive.

| File | Owner | Content |
| --- | --- | --- |
| `config.json` | `store/ServerConfigStore` | port, bind scope, developer mode, extra allowed hosts (strings) |
| `token.json` | `store/TokenStore` | `format`, `token`, `iv`, `wrapped`, `created_at`; the token is AES-GCM wrapped with the Keystore key `mcp_server_token` (unwrapped only when the Keystore fails, with a warning) |
| `paired_clients.json` | `store/PairedClientStore` | `format`, `clients[]` (`fingerprint`, `name`, `version`, `addressClass`, `firstPairedAt`, `lastSeenAt`) |

## Getting the token during development

The settings page (P4.2) will show, copy, and rotate the token. Until then the adb control plane is the
only way to read it, and only while the listener runs in developer mode:

```
adb shell am start-foreground-service -n io.github.supermonster003.autojs6.plugin.mcp.server/.McpServerService \
    -a io.github.supermonster003.autojs6.plugin.mcp.server.action.START_SERVER --ez developer_mode true
adb shell dumpsys activity service io.github.supermonster003.autojs6.plugin.mcp.server/.McpServerService
```

`dumpsys` prints the listener status, the paired clients, and (developer mode only) the full token. The
output goes to the adb shell alone; the token is never written to logcat, notifications, or the dialog
(which shows the last four characters so the user can tell which token the client presented).

## Pairing state machine

`server/PairingGate` is pure Kotlin (`PairingGateTest` covers it):

- Identity: `SHA-256(normalized client name + "\n" + address class)`, 24 hex characters. The name comes
  from the SDK session's `clientInfo` (found through the transport's `Mcp-Session-Id`), or from
  `User-Agent` when there is no session. The token is not part of the identity, so a rotation keeps the
  pairings; the address class (loopback / LAN) is, so a LAN client pairs separately from the USB one.
- Unpaired + gated method: a request is created (60 s window) and `PAIRING_REQUIRED` (`-32002`) is
  answered with `data.fingerprint`, `data.client`, `data.addressClass`, `data.expiresAt`,
  `data.firstRequest`. Repeated calls while the request is pending get the same answer with
  `firstRequest = false` and do not re-notify.
- Approve within the window: the client is stored, further gated calls pass; `lastSeenAt` is touched at
  most once a minute.
- Deny, timeout, or client limit (32): `PAIRING_DENIED` (`-32003`) with `data.reason`
  (`denied` / `timeout` / `limit`) and `data.retryAt` for 30 s, then a new request may be created.
- Revoke: the client is forgotten and the next gated call pairs again.
- Batches: when any request in a batch is gated, every request id in the batch gets the error document.

## Confirmation on the phone

`server/PairingCoordinator` (one per running listener, `PairingCoordinator.instance`) posts a
high-priority notification on channel `mcp_pairing` with Allow / Deny actions that broadcast to
`ui/PairingDecisionReceiver` (not exported), and starts `ui/PairingConfirmActivity` (translucent, dialog
themed, `:mcp_server` process) while the keyguard is not locked. Android 10+ may keep a service from
starting an activity; the notification is the reliable channel. Whatever settles a request (a decision,
the expiry, or the listener stopping) cancels the notification and closes the dialog.

Only the plugin itself can send the decision broadcast: `adb shell am broadcast` from the `shell` user is
refused because the receiver is not exported. The instrumentation test therefore approves and denies
through `context.sendBroadcast(PairingDecisionReceiver.intent(...))` from the app's own process.

## Evidence

- JVM: `BearerTokensTest`, `PairingGateTest`, `PairedClientCodecTest`, `AuthInterceptorTest`,
  `PairingInterceptorTest` (the last two run the interceptors on Ktor's test engine, the pairing one with
  the real SDK transport behind them).
- Device: `McpServerSpikeTest` adds `requestWithoutTokenIsUnauthorized`,
  `statefulSessionListsAndCallsDevicePingOncePaired` (required -> allow -> success),
  and `deniedPairingIsReportedWithACooldown`.
