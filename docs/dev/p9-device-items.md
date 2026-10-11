# P9: device items done on 2026-10-11

Plugin 1.0.3 (build 93, commit `52f3eaa`; the QR code build 94 does not change these paths), host
AutoJs6 6.8.0 debug build 5328 (commit `9530fda638`), protocol 2025-11-25. Every run used the
committed tools under `tools/evidence/`; reports are under `build/evidence/` (git-ignored) and hold
statuses, counts, timings and package names only.

## Screen annotation end to end

Redmi 22120RN86C (Android 13, API 33), AutoJs6 in front with its drawer open, listener switched on
in the drawer. `ui_dump` registered 200 references; `screen_capture(annotate: true)` captured
through accessibility at 559 x 1281 and drew 196 of them (4 skipped: invisible or outside the
image) in 270 ms against 194 ms without annotation (JPEG 143.7 KB against 46.7 KB). Viewed on the
PC, every outline sits on its control (switches, tabs, the search button, the floating button);
labels of nested containers overlap, as expected for a full dump; a `region` crop or a `ui_find`
narrows them.

## Third-party app UI chain (P3.2)

`tools/evidence/ui_chain.py` on the same Redmi in the system Settings app (`com.android.settings`):

| Step | Result |
| --- | --- |
| `ui_dump` | 99 nodes, 155 ms |
| `ui_find` | search entry by text (`Search`), then `clickable + boundsContains` for its innermost clickable ancestor (the hint is drawn by a non-clickable `android:id/input` inside the clickable `header_view`) |
| `ui_click` by `#n` reference | ok, 68 ms |
| `ui_wait_for` an `EditText` | appeared after 77 ms |
| `ui_set_text` | ok, 84 ms, the text reads back as set |
| `ui_wait_for` a scrollable node, then `ui_scroll down x2` | results list scrollable after 1.7 s; 2 of 2 steps performed |
| `ui_press_key back` | 3 presses left Settings |

The first runs showed why the scenario looks for the clickable ancestor and waits for a
scrollable list: `idMatches .*search.*` matched the large `search_result_ll` container (not
clickable, `ACTION_FAILED` with the hint to use a clickable ancestor), and a narrow query left the
results list unscrollable (`NODE_NOT_FOUND`).

## Screenshot rate limit (P6)

`tools/evidence/screenshot_rate.py` on the Redmi: after a quiet minute, 31 captures (maxWidth 64)
in 11.8 s; the first 30 returned images through accessibility, the 31st answered `RATE_LIMITED`
with `retryAfterMs` 48328; after that wait one more capture succeeded.

## ColorOS: new install, activation and reboot (P7)

OnePlus OPD2413 (ColorOS 15, Android 15, API 35). The host was updated in place to 5328 (its
accessibility service stayed bound), the plugin 1.0.2 was uninstalled (maintainer's consent; its
token and pairings were lost) and build 93 installed with `adb install`:

- ColorOS showed its "unknown source from computer" install page for the plugin and for its test
  APK; installing needed a tap on each.
- After the install the package was `stopped=true notLaunched=true`. The drawer listed MCP Server
  with the switch off and no subtitle; the first tap opened the activation guide ("MCP Server is
  installed but Android has not activated it yet") and the subtitle turned to
  "Activation required". ACTIVATE started the plugin: listener running with the host attached
  1.9 s after the tap, package `stopped=false`, `device_ping` and `device_info` 27 ms each.
- `adb reboot`: boot completed within seconds of adb coming back; the maintainer unlocked the
  tablet about 54 s later. The package stayed `stopped=false`; no listener ran before AutoJs6 was
  opened (no autostart, decision D15). Opening AutoJs6 restored the server that was on before the
  reboot (the switch on, `http://127.0.0.1:9637/mcp`), and `device_ping` / `device_info` answered
  in 46 / 27 ms.

Observation: before the first tap the drawer did not tell that the newly installed plugin needs
activation; the guide appears with the tap. A host follow-up could show "Activation required" for a
never-launched plugin ahead of the tap.

## Lifecycle S5 on API 31

`tools/evidence/token_rotation.py` on Sony XQ-AT72 (Android 12, API 31), listener switched on in
the drawer, maintainer's consent to rotate the token of this everyday device. A session was opened
with the current token (`device_ping` 66 ms); the token was then rotated on the settings page
(Access token > Rotate token > OK):

| Check | Result |
| --- | --- |
| Token fingerprint | `68150c0c7f222e63` -> `0c89baf26f5058ae` |
| Listener back with the host | 176 ms after the new fingerprint showed |
| Old token, old session id | `401`, JSON-RPC `-32001` "UNAUTHORIZED: the bearer token is not accepted" |
| New token, old session id | `404`, `-32000` "Session not found" |
| New session | initialize 178 ms (2025-11-25), `device_ping` and `device_info` ok, no pairing prompt |
| Pairing | the evidence client's pairing kept, then revoked |

This completes S5 on API 31, which the P6 run could not drive.

## Found on the way

- A held host session (the host's `McpServerPluginRoundTripTest`) restarts the AutoJs6 process, so
  its accessibility service ends up in "Crashed services" until it is switched off and on again
  (`adb shell settings put secure enabled_accessibility_services` without the AutoJs6 entry and then
  with it again restored it on the Redmi with the maintainer's consent). Scenarios that need accessibility now
  require a listener switched on in the drawer.
- On MIUI the host took more than a minute to rebind the plugin after the `am instrument`
  helpers; `evidence_session` waits up to 180 s.
