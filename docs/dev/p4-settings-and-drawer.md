# P4 settings and host drawer evidence

Date: 2026-09-13. Plugin: 1.0.0 / build 25. Paired host: 6.8.0 / build 5280,
branch `feat/mcp-p4`, commit `e1aa3f813`. The host work is isolated from existing SDK 37 edits in
the original host worktree. That worktree received further SDK 37 commits during
this session; P4 has not been merged into its master branch. The paired API
source revision is recorded in
`THIRD_PARTY_NOTICES.md`.

## Delivered behavior

- The AutoJs6 drawer owns the MCP Server switch. It inspects installation,
  activation, application / plugin enablement, signature trust and version
  compatibility before opening a Binder session. D18 confirmation is pinned
  to the currently authorized signer. The owner survives drawer recreation.
- Drawer long-press and Plugin Center Settings resolve the same explicit,
  PLUGIN-permission-protected settings activity. The foreground notification
  opens that activity and retains its Stop action.
- Settings expose current status and endpoints, adb forwarding, port and LAN
  scope, token reveal / copy / rotation, paired-client revocation, all eight
  tool groups and the root sub-switch, developer mode, four client snippets,
  release history and version information. LAN, dangerous groups, root and
  developer mode require confirmation. Native controls retain Android labels,
  focus behavior and layout direction. Eleven Android resource directories
  and ten generated document languages are updated together.
- Port / scope changes restart an active listener. Changes while stopped
  remain saved without starting it. Token rotation immediately rejects the
  old token and retains pairing; revocation requires a fresh confirmation.
  Tool changes emit `notifications/tools/list_changed`, and `tools/list`
  carries `_meta.ttlMs = 0` with the updated catalog.
- Settings and listener exchange atomic, locked private JSON files and an
  unexported receiver. Status snapshots include the listener process ID so
  a dead process cannot leave a false running indication. User-stop intent
  is retained separately for host relaunch. No boot receiver is added.
- Full tokens and client snippets use a secure dialog window, are dismissed
  when the activity pauses, and are excluded from saved view state, autofill
  and content capture. Normal pages and notifications show at most the last
  four token characters. Screenshot evidence covers non-secret pages only.

## Optional contract extension

The existing AIDL order and core contract version are unchanged. Optional
`mcpServerSettingsVersion = 1` is negotiated before a host sets
`serverConfigUseSavedSettings = true`. Legacy explicit port / scope callers
retain their behavior. Dynamic `mcpServerUserStopped` defaults to true when
its file is absent or invalid; successful start clears it and user stop sets
it. This also respects a notification stop issued while the host was dead.

Both release API AARs come from the same host build:

| Artifact | SHA-256 |
| --- | --- |
| `common-plugin-api.aar` | `59706b9fdb76a2312d295d9ad48b3bfe96d1da899cf76434e96ef646a673f400` |
| `mcp-server-api.aar` | `6ee668b2d883be7bc5c2be88b1b7787da0f7e1072410b0d5d2efca1df9a43400` |

Their `minCompileSdk = 36` metadata allows the compileSdk 36 plugin to consume
the contracts built by the compileSdk 37 host. The contracts use no SDK 37
APIs. Kotlin SDK / Ktor / serialization runtime dependency versions are unchanged.

## Verification

| Check | Result |
| --- | --- |
| Plugin JVM tests | 202/202 |
| Host MCP JVM tests | 19/19: grant 9, config 7, UI policy 3 |
| Host API release assembly and MCP API unit tests | Passed |
| Plugin debug, androidTest and release assembly | Passed, including R8 |
| Plugin lint | 0 errors; existing dependency / icon warnings retained |
| Signed release with CRC32 filename | `autojs6-plugin-mcp-server-v1.0.0-e9042e6d.apk`, 1356610 bytes, CRC32 `e9042e6d` verified |
| Host debug and androidTest assembly | Passed |
| Host full app lint | Canceled after about 45 minutes without a result; not a pass |
| IntelliJ project build | Passed; existing SDK XML / nullability warnings |
| Markdown generation | Ten languages / 36 generated artifacts; drift check passed |

The final Temurin-simulated plugin run passed in 5m 20s and printed exactly one
platform version-information section. `appendDigestToReleasedFiles` produced
the signed APK above; `releases/` contains that single current artifact.

The plugin uses AGP 9.3.2 with compileSdk 36; the host uses AGP 9.4.0 with
compileSdk 37. Host lint was still analyzing project code after about 45 minutes.
Two thread samples showed lint detector / Kotlin UAST resolution work, without
a reported lint result. Only the lint invocation started for this task was
canceled. No host lint failure or successful completion is inferred.

The full plugin device suite contains 20 tests, including the two new settings
tests. Device files and existing pairing / token state were backed up and
restored around each run.

| Device | API | Plugin instrumentation | Host Binder round trip |
| --- | --- | --- | --- |
| AVD_API_24, x86, SwiftShader | 24 | 20/20 | 1/1 |
| Sony G8441 | 28 | 20/20 | Not repeated in P4 |
| Sony XQ-AT72 | 31 | 20/20 | Not repeated in P4 |
| Sony XQ-DQ72 | 33 | 20/20 | Not repeated in P4 |
| Xiaomi, secondary API 33 device | 33 | 20/20 | Not repeated in P4 |
| Xiaomi 23046RP50C | 35 | 20/20 | 1/1 |

`McpServerSettingsTest` verifies invalid port rejection, persistence across
activity recreation, the secret window's `FLAG_SECURE`, release-history content,
and actual cross-process HTTP effects of group changes, token rotation, port
restart, pairing revocation and Stop. The complete suite also retains the
original synchronous stop assertion in `McpServerHostSessionTest`.

API 24 interactive evidence confirms activation guidance, activation followed
by drawer start and endpoint display, drawer long-press into settings, Plugin
Center into the same page, and notification Stop returning both the listener
and drawer to stopped. The settings page correctly reports a stopped server
after returning from Plugin Center.

API 35 also passed the two settings tests with Arabic RTL, night mode and
`fontScale = 1.5` together. Host preferences and font scale were restored, and
the normal-theme settings tests passed again. The last visual contrast change
was rerun on API 24 and API 35; the four other devices passed the full suite
before that display-only adjustment. The final build 25 debug APK was then
installed on all six listed devices without clearing app data.

The API 24 AVD initially suffered native graphics failures during optional
UI evidence capture: SurfaceFlinger crashed in `GraphicBuffer.flatten`, and a
later app run aborted in RenderThread with `GL errors! CanvasContext.cpp:505`.
An adb reboot did not clear the rendering failures. A cold start with
`-gpu swiftshader -no-snapshot-load -no-snapshot-save` preserved AVD user data
and allowed the complete 20-test suite to pass in 5m 27s. A fresh, fully loaded
settings screenshot was then captured. The AVD configuration file was not
changed, and no other emulator was restarted.

Non-secret UI evidence:

| Scenario | Image |
| --- | --- |
| API 24 activation guidance | [Drawer activation](images/p4/drawer-activation-api24.png) |
| API 24 active drawer | [Endpoint and enabled switch](images/p4/drawer-running-api24.png) |
| API 24 notification | [Running notification](images/p4/notification-running-api24.png) |
| API 24 after notification Stop | [Stopped drawer](images/p4/drawer-notification-stop-api24.png) |
| API 24, software rendering | [Settings in English](images/p4/settings-emulator-5554.png) |
| API 35, normal theme | [Settings in Chinese](images/p4/settings-968e9f18.png) |
| API 35, RTL / night / large font | [Settings in Arabic](images/p4/settings-rtl-dark-large-968e9f18.png) |

API 35 images use a temporary test port; device settings were restored afterward.

Three issues were found and corrected during validation:

1. Asynchronous Binder stop returned before the listener stopped. Stop and
   close now wait on the lifecycle queue shared with startup, retaining the
   existing contract and avoiding a queued startup after close.
2. An Android ActivityMonitor could return the previous settings instance
   during API 24 recreation. The test now selects the new resumed instance
   through AndroidX lifecycle monitoring. Secret-window foreground guards
   remain intact. The startup test also waits for the persisted start marker.
3. A light host theme made white toolbar text and primary-colored button text
   hard to read. Toolbar / system-bar icon contrast now follows background
   luminance; button text uses the page foreground, and the back arrow mirrors
   in RTL. API 24 keeps a dark navigation bar for its light system icons.

## Client snippet formats

The JVM snapshot uses an explicitly synthetic token. It covers Claude Code
HTTP commands, Cursor JSON, Codex TOML plus its PowerShell environment command,
and generic HTTP JSON, including IPv6 and malformed endpoint rejection.

The formats follow the current primary documentation: [Claude Code MCP](https://code.claude.com/docs/en/mcp),
[Cursor MCP](https://cursor.com/docs/mcp), and [Codex MCP](https://learn.chatgpt.com/docs/extend/mcp?surface=cli).
Codex stores the environment-variable name in TOML; its token is copied by a
separate environment command. The previous P3.5 Claude Code / AIGoCode result
establishes working gateway credentials, but does not substitute for P4's
new copy-and-paste client acceptance.

A P4 command-format smoke check used Claude Code 2.1.257 with the same ACP
environment, an isolated temporary `CLAUDE_CONFIG_DIR`, and API 24 over adb
forwarding. The snapshot command was populated with the current endpoint and
token, then passed to `claude mcp add`; `claude mcp list` reported `Connected`.
The user's global configuration hash was unchanged. No model request was made,
no credential was logged, and temporary configuration / forwarding was removed.
The API 35 attempt stopped before CLI execution because adb could not resolve
the foreground service; its configuration was restored. This command check is
not evidence for the phone clipboard-to-client paste workflow.


## Remaining acceptance

P4's implementation is available for testing. Its complete acceptance is not
yet claimed: all four installation / compatibility guides still need a full
real-device screenshot matrix, including a genuinely incompatible build;
OEM fresh-install activation, TalkBack / keyboard navigation, and the complete
process-death matrix remain to be verified. The new snippets still require
unmodified paste-and-connect checks in Claude Code and Cursor. Cursor was not
available through the current command-line environment. These checks remain
unchecked in `ROADMAP.md`; P5 client compatibility work must retain them.

No host release build was repeated: this phase changes the host's UI and
optional pure contract keys, and introduces no host runtime dependency. The
plugin's release / R8 build was executed. No publication or push was performed.
