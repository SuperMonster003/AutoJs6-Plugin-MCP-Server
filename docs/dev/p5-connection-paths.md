# P5.1 connection paths: USB and local network evidence

Date: 2026-09-15. Plugin: 1.0.0, APK built from the build 30 tree with the
P5.1 sources (committed as build 31). Host: AutoJs6 `master` `9c3ba2e52`
(the P4 paired build). Device: Sony XQ-DQ72 (API 33) on 5 GHz Wi-Fi
(5320 MHz, RSSI -40 dBm, link 300 Mbps) with a VPN tunnel (`tun0`) and
cellular data also up; PC on Ethernet in the same /24 as the phone.

## Delivered behavior

- Network card of the settings page: with local network access on, the page
  lists the current local network addresses of the phone, a same-network /
  firewall hint and a "Daily reminder while listening on the local network"
  switch. The address list comes from the interfaces while the listener is
  down and from the listener otherwise; the connectivity callback of
  `LanAddressWatcher` refreshes it when Wi-Fi changes. The reminder switch is
  enabled only while local network access is on.
- `LanAddressWatcher` now considers only interfaces that are up, not
  loopback, not point-to-point and multicast-capable. VPN tunnels and
  cellular data links are therefore excluded from both the address list and
  the Host allow list. This was found during this run: before the change the
  page listed the `tun0` address and, with Wi-Fi off, the cellular address as
  "local network addresses".
- Pairing from a local network address: the dialog is titled "Local network
  client wants to pair" and starts with a warning paragraph; the notification
  is titled "Local network MCP client wants to pair" with the warning in its
  expanded text. Loopback pairing keeps the previous title.
- Daily reminder: while a LAN listener runs and the switch is on, the
  foreground service posts one dismissable notification every 24 h on its
  own channel ("Local network reminders") with a Stop action; tapping it
  opens the settings page. The switch only changes `lan_reminder` in
  `config.json`; `ServerConfig.sameListener` keeps the listener running
  across reminder-only changes.
- README (10 languages): a "Connection Paths" section documents the USB
  path (`adb forward`, `-s <serial>` with several devices, port change when
  taken, emulators, the copyable command on the Connection card) and the
  local network path.

## Verification

- JVM: 206/206 (`ServerConfigTest`: reminder default, persistence, invalid
  value, `sameListener`; `LanReminderTest`: `wanted` and `endpoint`).
- `lintDebug`: 0 errors, 8 warnings (unchanged set).
- Device run: the git-ignored script `build/tools/p5_lan_latency.py`
  (built on the P4 helpers and a stdlib Streamable HTTP client,
  `build/tools/mcp_client.py`) drove the phone through `uiautomator` dumps.
  The log never contains the token.

| Step | Outcome |
| --- | --- |
| Listener started from the host drawer (loopback) | Drawer subtitle `http://127.0.0.1:9637/mcp`; unauthenticated probe over `adb forward` 401, over Wi-Fi connection refused |
| Token from the settings page clipboard (instrumentation, loopback scope) | `OK (1 test)`; the cache copy was deleted |
| Local network switch on | Confirmation dialog shown and accepted; runtime log `Listener configuration changed (9637/loopback -> 9637/lan); restarting`; probes 401 over USB and Wi-Fi |
| Network card after the switch | Address list `http://192.168.3.213:9637/mcp` only (VPN tunnel excluded), hint text, reminder switch checked and enabled; endpoint text lists loopback and the LAN URL |
| Reminder switch off, then on | `config.json` `lan_reminder` followed the switch; no `restarting` line in the runtime log; probes stayed 401 on both paths |
| Pairing over USB (`p5-usb`) | `PAIRING_REQUIRED` (-32002), dialog "Pair MCP client", accepted; `device_info` answered |
| Pairing over Wi-Fi (`p5-wifi`) | `PAIRING_REQUIRED` (-32002), dialog "Local network client wants to pair" with the warning paragraph and the heads-up notification "Local network MCP client wants to pair"; accepted; `device_info` answered |
| Wi-Fi off, then on | Address list changed to "No local network address yet. Connect the phone to Wi-Fi." and returned to the Wi-Fi URL after reconnection; Wi-Fi probe 401 again |
| Cleanup | Local network switched off (listener restarted as loopback), server stopped from the drawer, pairing records and `config.json` removed (absent before), `adb forward` removed, phone accessibility list restored |

## Latency baseline

Same PC, same phone, same listener; 5 timed calls per tool after one warm-up
call; client-side wall clock including HTTP and JSON handling. `ui_dump` used
`maxNodes = 200` on the host main activity (200 nodes, tree truncated,
about 13.7 KB of text). `screen_capture` used the defaults (JPEG quality 70,
longest edge 1280): 548 x 1280 pixels, about 60.8 KB of base64.

| Path | initialize | ui_dump min / median / max | screen_capture min / median / max |
| --- | --- | --- | --- |
| USB (`adb forward tcp:9637 tcp:9637`) | 17 ms | 50 / 61 / 68 ms | 117 / 367 / 382 ms |
| Wi-Fi (`http://192.168.3.213:9637/mcp`) | 140 ms | 331 / 393 / 418 ms | 479 / 480 / 638 ms |

ICMP round trips from the PC to the phone during the run were 65 to 209 ms
(average 144 ms), so the Wi-Fi path is dominated by the power-save latency
of the phone's Wi-Fi radio, not by the plugin; the USB numbers are the host
work itself.

## Observations and limits

- The 24 h reminder notification itself was not observed (it needs a day of
  listening); the JVM tests cover the policy and the device run covers the
  switch, the stored flag and the absence of a listener restart.
- `ui_dump` needs the host accessibility service. The first attempt returned
  `A11Y_SERVICE_NOT_RUNNING` because the service was not enabled on this
  phone; `device_ensure_accessibility` enabled it in 276 ms (the host holds
  `WRITE_SECURE_SETTINGS`), which also exercised that tool's secure-settings
  strategy. The previous accessibility list was restored afterwards.
- The settings page follows the host's Chinese UI language, while the pairing
  dialog and notification (plugin process) use the system locale (en-US), as
  in the P4 evidence.
- The Wi-Fi numbers are one phone on one router; they are a baseline, not a
  specification.

## Screenshots

| Capture | File |
| --- | --- |
| Local network confirmation dialog | [settings-lan-confirm-api33](images/p5/settings-lan-confirm-api33.png) |
| Network card with the address, hint and reminder switch | [settings-network-lan-api33](images/p5/settings-network-lan-api33.png) |
| Network card with Wi-Fi off | [settings-lan-wifi-off-api33](images/p5/settings-lan-wifi-off-api33.png) |
| Local network pairing dialog and notification | [pairing-lan-api33](images/p5/pairing-lan-api33.png) |
