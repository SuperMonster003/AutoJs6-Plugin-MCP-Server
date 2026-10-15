# Device evidence tools

Committed replacements for the git-ignored `build/tools/*.py` scripts that earlier roadmap
evidence referred to and that were lost in the 2026-10-10 workspace cleanup (roadmap P9). New
evidence should only depend on these files.

| File | Purpose |
| --- | --- |
| `mcp_client.py` | Streamable HTTP MCP client (standard library only): `initialize`, requests answered as JSON or SSE, `Mcp-Session-Id`, pagination, streams read event by event, resumption with `Last-Event-ID`, `DELETE` on close |
| `device.py` | adb helpers: plugin `dumpsys` state, `adb forward` with automatic cleanup, token reading, pre-pairing and revocation of the evidence client, the shared storage switch, a held host session, package enable / disable |
| `session.py` | The common frame of a scenario: token, evidence pairing, held host session, forward, MCP session, revocation |
| `docs_resources.py` | Scenario: offline docs resources on a device (roadmap P3.5 optional item) |
| `sse_resume.py` | Scenario: SSE resumption with `Last-Event-ID` mid-run and after the end, unknown ids (roadmap P9, SEP-1699) |
| `ui_chain.py` | Scenario: the UI tools on the system Settings app, dump -> find -> click -> wait -> set_text -> scroll -> back (roadmap P3.2 / P9) |
| `screenshot_rate.py` | Scenario: 31 captures in a row, the 31st `RATE_LIMITED` with `retryAfterMs`, then a capture after the wait (roadmap P6 / P9) |
| `token_rotation.py` | Scenario: lifecycle S5, the token rotated on the settings page while a session is live (roadmap P6 / P9) |
| `lifecycle.py` | Scenario: the P6 lifecycle matrix S1 - S6 with kills from adb and `ACTION:` prompts for the on-screen steps (roadmap P6 / P9) |
| `privileged.py` | Scenario: `device_ensure_accessibility` while the AutoJs6 accessibility service is off (the host's root, secure settings or Shizuku strategy), then `shell_exec id` without and with `root: true`; reports only the uid names (roadmap P3.4 / P9) |
| `contract_v2.py` | Scenario: the host contract version the installed host chose (`openServerV2` from host build 5283 on, `openServer` before), read from `dumpsys` and the plugin log, then tools through that broker, a descriptor payload included when the accessibility service runs (roadmap P10) |
| `bridge_reopen.py` | Scenario: the stdio bridge (`autojs6-mcp-bridge`) across a listener restart; each `--bridge` command runs once, so an old and a new bridge can be compared (roadmap P9) |
| `p11_image_tools.py` | Scenario: the P11 barcode and OpenCV tools on a real host: fixtures written through `script_run`, `barcode_detect` / `opencv_find_color` / `opencv_find_multi_colors` / `opencv_find_image` on them at normal size and at 5000 px (decoded at half size, D45), a template at another scale refused; then on the screen of the Settings app with a label cut by `screen_capture` as template, and optionally an EAN-13 opened in an image viewer (`--show-barcode-with`) (roadmap P11) |
| `p11_text_tools.py` | Scenario: the P11 text and media tools on a real host: `pinyin_convert` (word readings, heteronyms, styles), `pinyin4j_convert` (tone marks, Wade-Giles, all readings, tone refused outside Hanyu) and `mediainfo_read` on a WAV fixture written through `script_run`, with and without the report, and a missing file (roadmap P11) |
| `p11_shared_storage.py` | Scenario: the "Allow reading shared storage" switch (roadmap P11 D26): with it off, absolute paths are refused before the host; with it on, fixtures pushed into `/sdcard/Download` are listed with absolute paths and read by `files_stat`, `barcode_detect`, `opencv_find_color` and `mediainfo_read`, while `Android/data`, `files_read` and paths outside shared storage stay refused; the switch is restored afterwards |
| `p11_device_info.py` | Scenario: the `plugins` section of `device_info` and `autojs6://device/info` (roadmap P11 D27): seven families with known statuses, a hint for every family without tools, the available families matching the listed plugin-family tools; `--expect-no-plugins` for an older host |
| `p11_registered_scripts.py` | Scenario: the registered-script tools (roadmap P11 D42): five `@agent` scripts written through `files_write`, `script_catalog` with risk / confirm and parameter defaults, an id two scripts share refused with both paths, a run by path with defaults and the reported result, extra arguments refused, a sensitive script run without a phone prompt, a throwing and a timed-out script; `--expect-hidden` for an older host (tools not listed, direct call `CAPABILITY_DENIED`) |
| `p11_screen_quota.py` | Scenario: the screenshot window shared by `screen_capture` and the recognition tools with `image: "screen"` (roadmap P11 D39): 30 interleaved calls succeed, the 31st recognition and a capture are `RATE_LIMITED`, a file call is not counted, recovery after `retryAfterMs` |
| `tool_calls.py` | `tools/call`, `script_run`, chunked file writes through the host and the accessibility check with rebinding, shared by the P11 scenarios |
| `workspace_missing.py` | Scenario: the reasons AutoJs6 gives while its working directory does not exist (host follow-up of P11): `files_write`, `files_list` and `script_run` name the missing directory and the way out, none blames the grant; `--create` creates it through adb shell and checks both tools again. Needs a host whose working directory is missing, for example a read-only emulator with a newly installed host |
| `image_fixtures.py` | Standard-library PNG encoding / decoding, the EAN-13 symbol and the fixtures of `p11_image_tools.py` |
| `test_evidence.py` | Offline checks of the parsing and redaction logic and of the image fixtures |

## Prerequisites

- Python 3.10 or newer and `adb` on the PATH (or `ADB=<path>`); several devices may be connected,
  every command takes `--serial`.
- Token and pairing helpers: the plugin debug build and its androidTest APK
  (`:app:assembleDebug :app:assembleDebugAndroidTest`) from the same build installed on the device; they run the opt-in
  `EvidenceHelperTest` (`reportToken`, `pairClient`, `revokePairing`). `am instrument` force-stops
  the whole plugin package when it starts and ends, so the scenarios call these helpers before a
  host session is held, and a pending pairing prompt cannot be answered from there: the evidence
  client is recorded as paired beforehand instead and revoked afterwards. The prompt flow itself is
  covered by the pairing evidence of roadmap P2.2 and P5. Without the androidTest APK, switch
  developer mode on in the plugin settings (the token is then read from `dumpsys`) or set
  `AUTOJS6_MCP_TOKEN`, and allow the pairing on the phone.
  The helpers check that the installed plugin is debuggable before starting instrumentation.
  A leftover androidTest APK beside a release plugin is not a compatible pair. Automatic token
  selection and session setup skip that runner and use the release-compatible paths above;
  scenarios that explicitly require a helper still need both debug APKs.
- Host session: if the listener is not already running with the host attached, the host
  androidTest APK holds one through `McpServerPluginRoundTripTest` and
  `setprop debug.autojs6.mcp.hold <seconds>`. That test enables the plugin for the run and restores
  its previous enable state; the scenario waits until the hold ends and the test has stopped and
  closed the session. Several devices can run at the same time: each run asks adb for a free local
  port.

## Rules

- The token stays in memory. It is never printed, logged or written to a file; adb output that may
  carry it is scrubbed before it appears in an error, and `dumpsys` parsing drops the token line.
- Reports under `build/evidence/` (git-ignored) contain sizes, hashes, counts, timings and error
  codes only, never file content, page content or the token.
- Every change to the device is undone in `finally` blocks: forwards are removed, the hold property
  is reset, a disabled package is enabled again and the evidence client's pairing is revoked unless
  `--keep-pairing` is given.

## Debug test APK against a release app

On 2026-10-11, Sony XQ-DQ72 (API 33, `QV770340J7`) recorded three crashes of MCP Server
1.0.3 / 168 at 14:52:33, 14:52:47 and 14:52:59 (UTC+08:00). Every stack started in
`AppComponentFactoryRegistry.instantiateApplication`, called by `MonitoringInstrumentation`
and `AndroidJUnitRunner`, and failed to resolve `kotlin.jvm.internal.Intrinsics` before any
MCP application code ran.

Read-only inspection on 2026-10-14 found:

- The release app was last updated at 14:51:32 on 2026-10-11. Its APK matched the local
  `autojs6-plugin-mcp-server-v1.0.3-4f5b5fd7.apk` byte for byte (SHA-256
  `bd444519a88c0b5a83e8bb1db1a1a0a2110108de85defd954d1fa9e77cae0949`).
- The separate debug test APK was last updated at 12:55:36 that day and still registered
  `AndroidJUnitRunner` for this package. Its SHA-256 was
  `3f61f8f8d31ed19cf2130554409e936bcfb9e70295f84e0eaaedd6f7ec5f6f20`.
- The test DEX referenced `kotlin.jvm.internal.Intrinsics.checkNotNullParameter` without
  defining `Intrinsics`. The release DEX neither defined nor referenced that original name;
  R8 had optimized the application independently. The current debug APK defines that name.
- The current `releaseRuntimeClasspath` resolves `kotlin-stdlib:2.4.0`; the Kotlin/SDK/Ktor
  dependency declarations and R8 settings have not changed since `v1.0.3`.

This is an incompatible instrumentation/app pair, not evidence that the release app omitted
its Kotlin runtime. This debug test APK expects unoptimized application classes and cannot be
reused against this independently minified release APK. Adding a stdlib dependency or a blanket
Kotlin keep rule to the release build does not correct the mismatched test setup.

Install the debug app and its matching androidTest APK together when using these helpers.
For release acceptance, use the public MCP endpoint with an environment or developer-mode
token and confirm pairing on the phone. A test APK that is no longer needed can be removed
separately by uninstalling `io.github.supermonster003.autojs6.plugin.mcp.server.test`.
The crash report identifies the runner startup failure but does not identify which command
or scenario invoked it. The preflight added after this investigation prevents the evidence
helpers from starting that incompatible runner; the affected phone was verified with read-only
ADB calls and its installed packages and app data were preserved.

References: [Android instrumentation build types](https://developer.android.com/studio/test/advanced-test-setup#change_the_test_build_type),
[R8 optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization).

## Usage

```shell
py -m unittest discover -s tools/evidence
py tools/evidence/docs_resources.py --serial <serial>
py tools/evidence/sse_resume.py --serial <serial>
py tools/evidence/bridge_reopen.py --serial <serial> --bridge "node ../AutoJs6-MCP-Bridge/dist/cli.js"
py tools/evidence/ui_chain.py --serial <serial>
py tools/evidence/screenshot_rate.py --serial <serial>
py tools/evidence/token_rotation.py --serial <serial>   # then rotate the token on the settings page
py tools/evidence/lifecycle.py --serial <serial> [--only S1 S2 ...]   # follow the ACTION lines on stderr
py tools/evidence/privileged.py --serial <serial>   # switch the Shell group and "Allow root" on when asked
```

`docs_resources.py` options: `--hold-seconds` (host session hold, default 240),
`--token-source auto|env|instrumentation|dumpsys`, `--no-toggle-docs` (skip disabling the Offline
Docs plugin), `--keep-pairing`, `--report-dir`.

`bridge_reopen.py` needs Node.js, a listener that the AutoJs6 drawer switched on (a session held by
the host instrumentation does not survive the restart) and the plugin androidTest APK: the restart
is an `am instrument` run of `EvidenceHelperTest#pairClient`, which restarts the plugin package like
an update. Do not restart it with `am force-stop`: the host treats that as the user's decision and
leaves the server off until the drawer switch is turned on again. Write `--bridge` commands with
forward slashes; the token reaches the bridge only through `AUTOJS6_MCP_TOKEN` in its environment.

`ui_chain.py` and every scenario that needs the AutoJs6 accessibility service need a listener the
drawer switched on: holding a host session runs the host instrumentation, which restarts the
AutoJs6 process and stops its accessibility service until it is switched on again. On MIUI the
host took more than a minute to rebind the plugin after the helpers, hence the 180 s
`rebind_timeout` of `evidence_session`.

`privileged.py` needs a listener the drawer switched on and restores
`enabled_accessibility_services` afterwards (`--keep-accessibility` leaves it). The host tries its
strategies in a fixed order (root, secure settings, Shizuku), each behind its own host setting; to
prove the Shizuku path on a phone that also has root, switch the other two off in the AutoJs6
settings for the run and back on afterwards.
