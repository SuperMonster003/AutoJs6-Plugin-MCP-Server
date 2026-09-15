# P4 settings and host drawer evidence

Date: 2026-09-13, follow-up 2026-09-15. Plugin: 1.0.0 / build 25 (follow-up:
build 28 sources). Paired host: 6.8.0 / build 5280, branch `feat/mcp-p4`, commit
`e1aa3f813`, merged into the host `master` as `9c3ba2e52` on 2026-09-15 (see the
follow-up section). The paired API source revision is recorded in
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

Updated 2026-09-15. The four installation / compatibility guides, the host-level
plugin-disabled guide, drawer / notification synchronization, keyboard
navigation, screen-reader labels, process death recovery and the Claude Code
paste-and-connect check are verified below. Still open: a Cursor paste check
(Cursor is not installed in the current command-line environment, so only the
JSON snapshot test covers that format), a gesture-driven TalkBack traversal
(see the TalkBack notes below), and the notification Stop action on the two Sony
devices whose notification shade publishes no accessibility hierarchy to
`uiautomator`. These remain listed for P5 client compatibility work.

No host release build was repeated: this phase changes the host's UI and
optional pure contract keys, and introduces no host runtime dependency. The
plugin's release / R8 build was executed. No publication or push was performed.

## 2026-09-15 acceptance follow-up

Plugin 1.0.0 / build 28 sources plus the keyboard fix below; host 6.8.0 /
build 5280 built from the merge commit `9c3ba2e52`.

### Host integration

`feat/mcp-p4` (`e1aa3f813`) was merged into the host `master` as `9c3ba2e52`
("merge: integrate the MCP Server P4.1 drawer control with the SDK 37
mainline"). Conflicts were limited to the ten changelog JSON files (both entry
sets kept, generated README / CHANGELOG regenerated with the host generator)
and `DrawerFragment.kt`, where the MCP Server item stays next to the JSON socket
server item ahead of the new local network permission item. The merged host
passed its MCP JVM tests (19/19: grant 9, config 7, UI policy 3) and the debug /
androidTest assembly (5m 34s). The merged host debug build was installed on all
six devices below. Host master later received unrelated SDK 37 commits from
other sessions; they are not part of this evidence.

### Keyboard navigation and screen-reader labels

`McpServerSettingsAccessibilityTest` (3 tests) was added to the plugin
instrumentation suite:

- Every visible clickable / editable view carries a text or content
  description; card titles are accessibility headings (API 28+); and, through
  `UiAutomation`, every actionable node in the published accessibility tree has
  a name. This is what a screen reader receives.
- Tab reaches every enabled control and the toolbar back button; Enter opens
  the About dialog from a focused button and toggles a focused tool-group switch,
  with the change persisted for the listener process and shown again.
- Copying the Claude Code snippet places the generated command on the
  clipboard, marked sensitive on API 33+ (`EXTRA_IS_SENSITIVE`); the clip is
  cleared afterwards unless the opt-in `mcpClipboardEvidence` argument asks the
  test to leave the text in the app cache for the paste check below.

One defect was found and fixed: the Material toolbar style blocks keyboard focus
on touch screens and forms its own keyboard navigation cluster, so Tab never
reached the back button; on Android 7 the focus order is sorted by screen
position, so scrolled content also won the wrap-around. `SettingsPageActivity`
now clears `touchscreenBlocksFocus` and the cluster flag and links the last
content focusable back to the toolbar button. The class passed 3/3 on API 24
(AVD), 28, 31, 33, 33 and 35.

### Real-device drawer state matrix

The evidence script (git-ignored `build/tools/p4_states.py`) drives the host
drawer through `uiautomator` dumps: baseline, switch on, notification Stop,
switch off, `pm uninstall -k --user 0` (not installed guide), fresh install
(activation guide, accepted), `pm disable-user` (application disabled guide),
a throwaway build whose manifest requires host build 999999 (incompatible
guide), and the restored build. Every guide dialog and the drawer subtitle
after it were captured; the listener was probed through a per-device adb
forward (401 without a token = listening).

| Device | API | Four guides + restored | Notification Stop -> drawer off | Notes |
| --- | --- | --- | --- | --- |
| AVD x86, SwiftShader | 24 | Passed | Passed | Host restart needed before the drawer saw the fresh install |
| Sony G8441 | 28 | Passed | Not verified (shade hierarchy empty) | Dialog windows publish an empty `uiautomator` hierarchy; buttons located from the screenshot and the window frame. Host-level plugin-disabled guide ("Enable plugin") observed and accepted |
| Sony XQ-AT72 | 31 | Passed | Not verified (shade hierarchy empty) | Multi-user device: installs need `--user 0`; host restart needed before the drawer saw the fresh install |
| Sony XQ-DQ72 | 33 | Passed | Passed | Notification permission requested from the settings page first |
| Xiaomi 22120RN86C | 33 | Passed | Passed | MIUI permission dialog labels the grant "Allow all the time"; host restart needed after the fresh install |
| Xiaomi 23046RP50C | 35 | Passed | Passed | HyperOS permission dialog "始终允许" |

Three devices kept "Plugin not installed" in the drawer after a fresh install
until the host process was restarted (the two Sony API 33 / Xiaomi API 35
devices refreshed without it). This is recorded as a host follow-up: the drawer
state should refresh on package changes without a host restart.

Screenshots (English UI, Xiaomi API 33 unless noted):

| Scenario | Image |
| --- | --- |
| Not installed guide | [Plugin Center guidance](images/p4/dialog-not-installed-api33.png) |
| Fresh install, activation guide | [Activate](images/p4/dialog-activation-api33.png) |
| Application disabled guide | [System settings guidance](images/p4/dialog-app-disabled-api33.png) |
| Incompatible build guide | [Required host build](images/p4/dialog-incompatible-api33.png) |
| Incompatible build, drawer subtitle | [Drawer state](images/p4/drawer-incompatible-api33.png) |
| Host-level plugin disabled guide, API 28 | [Enable plugin](images/p4/dialog-plugin-disabled-api28.png) |

### Notification, process death and settings recovery

- Sony API 33, Xiaomi API 33 / 35 and the AVD: the Stop action in the
  foreground notification stopped the listener, the drawer switch returned to
  off with the stopped subtitle, and the notification disappeared; switching on
  and off from the drawer showed and removed the notification.
- Xiaomi API 35 process death matrix: `kill -9` of the `:mcp_server` process
  through `run-as` was followed by a new listener process while the drawer kept
  showing the endpoint and the probe kept answering 401; `am force-stop` of the
  host left the listener running with `statusHostAvailable = false` in
  `status.json`, and the reopened host showed the endpoint again with the same
  listener process; after `am kill` of the plugin's UI process, the settings
  page reopened with the identical text content. On Sony API 33 the same host
  kill and settings recovery passed; there the `run-as kill` signal returned
  success without ending the process, so listener-death recovery is evidenced on
  API 35 only.

| Scenario | Image |
| --- | --- |
| Listener process killed, drawer still connected (API 35) | [Drawer](images/p4/drawer-after-listener-kill-api35.png) |
| Settings page after its process was killed (API 33) | [Settings](images/p4/settings-after-process-death-api33.png) |

### TalkBack

TalkBack was enabled through `settings put secure` with the settings page in
front on Sony API 33 and Xiaomi API 35; the accessibility focus frame rendered
on the toolbar navigation button, and the previous accessibility services were
restored afterwards. Gesture-driven traversal could not be scripted: touch
events injected by `adb shell input` bypass TalkBack touch exploration on the
Sony device (they click the control) and are ignored as gestures on the Xiaomi
device, whose first TalkBack start also opens the TalkBack tutorial; injected
Alt+arrow key combinations are not honored as TalkBack keymap commands. The
screen-reader content check therefore rests on the `UiAutomation` tree audit in
`McpServerSettingsAccessibilityTest`, which is the same node information
TalkBack announces.

| Scenario | Image |
| --- | --- |
| TalkBack enabled on the settings page, API 33 | [Focus frame on the back button](images/p4/settings-talkback-api33.png) |

### Claude Code paste-and-connect

Sony API 33, Claude Code 2.1.257 (the ACP-bundled CLI), the maintainer's gateway
environment, an isolated temporary `CLAUDE_CONFIG_DIR`, and `adb forward
tcp:9637 tcp:9637`:

1. The settings page copied the Claude Code command (instrumentation with
   `mcpClipboardEvidence`); the text was pulled from the app cache and deleted
   there.
2. The text was executed unchanged in Git Bash: `claude mcp add` reported the
   HTTP server with a redacted Authorization header.
3. `claude mcp list` reported `autojs6: http://127.0.0.1:9637/mcp (HTTP) - Connected`.
4. `claude -p` with `--allowedTools mcp__autojs6__device_info` triggered the
   pairing prompt on the phone (dialog in one run, notification action in
   another); after Allow, the model session finished in 4 turns / 33 s with
   `is_error = false` and answered "Model: Sony XQ-DQ72, Android API level: 33"
   from the tool result. The paired client `claude-code` appeared in
   `paired_clients.json`.
5. The temporary configuration, the adb forward and the pairing record were
   removed; the listener was stopped from the drawer. The evidence log redacts
   the token and gateway credential.

Cursor remains unverified because it is not installed here; its JSON format is
covered by the snapshot test only.
