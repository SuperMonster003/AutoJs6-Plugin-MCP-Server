# Device evidence tools

Committed replacements for the git-ignored `build/tools/*.py` scripts that earlier roadmap
evidence referred to and that were lost in the 2026-10-10 workspace cleanup (roadmap P9). New
evidence should only depend on these files.

| File | Purpose |
| --- | --- |
| `mcp_client.py` | Streamable HTTP MCP client (standard library only): `initialize`, requests answered as JSON or SSE, `Mcp-Session-Id`, pagination, streams read event by event, resumption with `Last-Event-ID`, `DELETE` on close |
| `device.py` | adb helpers: plugin `dumpsys` state, `adb forward` with automatic cleanup, token reading, pre-pairing and revocation of the evidence client, a held host session, package enable / disable |
| `session.py` | The common frame of a scenario: token, evidence pairing, held host session, forward, MCP session, revocation |
| `docs_resources.py` | Scenario: offline docs resources on a device (roadmap P3.5 optional item) |
| `sse_resume.py` | Scenario: SSE resumption with `Last-Event-ID` mid-run and after the end, unknown ids (roadmap P9, SEP-1699) |
| `ui_chain.py` | Scenario: the UI tools on the system Settings app, dump -> find -> click -> wait -> set_text -> scroll -> back (roadmap P3.2 / P9) |
| `screenshot_rate.py` | Scenario: 31 captures in a row, the 31st `RATE_LIMITED` with `retryAfterMs`, then a capture after the wait (roadmap P6 / P9) |
| `token_rotation.py` | Scenario: lifecycle S5, the token rotated on the settings page while a session is live (roadmap P6 / P9) |
| `lifecycle.py` | Scenario: the P6 lifecycle matrix S1 - S6 with kills from adb and `ACTION:` prompts for the on-screen steps (roadmap P6 / P9) |
| `bridge_reopen.py` | Scenario: the stdio bridge (`autojs6-mcp-bridge`) across a listener restart; each `--bridge` command runs once, so an old and a new bridge can be compared (roadmap P9) |
| `test_evidence.py` | Offline checks of the parsing and redaction logic |

## Prerequisites

- Python 3.10 or newer and `adb` on the PATH (or `ADB=<path>`); several devices may be connected,
  every command takes `--serial`.
- Token and pairing helpers: the plugin debug build and its androidTest APK
  (`:app:assembleDebug :app:assembleDebugAndroidTest`) installed on the device; they run the opt-in
  `EvidenceHelperTest` (`reportToken`, `pairClient`, `revokePairing`). `am instrument` force-stops
  the whole plugin package when it starts and ends, so the scenarios call these helpers before a
  host session is held, and a pending pairing prompt cannot be answered from there: the evidence
  client is recorded as paired beforehand instead and revoked afterwards. The prompt flow itself is
  covered by the pairing evidence of roadmap P2.2 and P5. Without the androidTest APK, switch
  developer mode on in the plugin settings (the token is then read from `dumpsys`) or set
  `AUTOJS6_MCP_TOKEN`, and allow the pairing on the phone.
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
