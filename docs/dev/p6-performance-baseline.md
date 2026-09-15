# P6 performance baseline: ui_dump, screen_capture, script_run and four concurrent requests

Date: 2026-09-16. Plugin 1.0.1 (build 45), host AutoJs6 6.8.0 (5280). Record only: these figures
are a baseline for later comparison, not thresholds, and no test asserts them.

Devices, one set each:

| Set | Device | API | ABI | Screen | Notes |
| --- | --- | --- | --- | --- | --- |
| AVD | `AVD_API_24`, headless emulator (`emulator-5554`) | 24 | x86 | 1440 x 2560 | `screen_capture` through MediaProjection (consent dialog tapped by the harness) |
| Phone | Sony XQ-DQ72 (`QV770340J7`) | 33 | arm64-v8a | 1096 x 2560 | `screen_capture` through the accessibility path |
| Pad | Xiaomi Pad 23046RP50C (`968e9f18`, HyperOS) | 35 | arm64-v8a | 1800 x 2880 | `screen_capture` through the accessibility path; pairing through the expanded notification action |

Tooling (git-ignored): `build/tools/p6_perf.py <serial> [--local-port N] [--runs 5]`, which writes
`build/p6p/perf-<serial>.log` / `.json` and `summary-<serial>.md` plus screenshots. The harness
starts the listener through the drawer, reads the token with the instrumentation helper, forwards
a PC port to 9637 over adb, pairs `p6-perf` through the real prompt and leaves the device as found
(listener off, paired-client file and accessibility setting restored, the synthetic UI stopped).

## Method

- Wall time is measured on the PC around one `tools/call` POST (`urllib`, one connection per call,
  through `adb forward`), so it includes the USB or emulator loopback hop, the Ktor request, the
  host bridge and the JSON answer. Five runs per measurement with 0.3 s pauses; the table shows
  the median and the min-max range in milliseconds. The plugin's own timing is listed where a
  tool reports it (`durationMs`: the wait inside `script_run`, the capture inside `screen_capture`).
- `ui_dump`: a script started with `script_run` (`waitForCompletion: false`) shows a scroll view
  with 450 text nodes of 8 sp. `ui_dump` is called with `visibleOnly: false`, `format: text` and
  `maxNodes` 50 / 200 / 400 (400 is the schema maximum); all three answer `truncated: true`, so the
  tree exceeds 400 nodes on every device. The defaults (`visibleOnly: true`, 200) are listed too.
  `uiautomator dump` on the same screen counts only the visible nodes (71 / 94 / 110).
- `screen_capture`: JPEG quality 70 at `maxWidth` 360, at the default size (longest edge 1280) and
  at `scale: 1` (full resolution), then PNG at the default size. The first call is listed on its
  own because on API 24 it includes the MediaProjection consent.
- Round trips: `device_ping` (answered by the plugin, no host call), `device_info` (one host
  bridge call), `script_run` of `1 + 1;` and of a loop printing 20 console lines (`captureConsole`
  on).
- Concurrency: four threads post one request each at the same instant on the same session, for
  `device_info`, `ui_dump` (50 nodes) and `script_run` (`1 + 1;`); five rounds with 0.5 s pauses;
  the wall time is the slowest of the four. The host grant for this session was 59 methods and
  4 concurrent calls, so four is the broker's limit. Four requests per round stay below the
  20 requests / s rate limit; no `429` occurred.

## Results (median ms, min-max in brackets)

| Measurement | AVD API 24 x86 | Sony API 33 | Xiaomi Pad API 35 |
| --- | --- | --- | --- |
| `device_ping` | 17.8 (15.1-33.4) | 20.2 (17.3-29.9) | 76.0 (69.0-93.0) |
| `device_info` | 20.9 (17.2-23.0) | 24.8 (21.6-26.5) | 128.0 (90.2-135.3) |
| `script_run` `1 + 1;` | 116.4 (105.5-130.6), waited 68 | 104.1 (82.9-279.7), waited 48 | 259.8 (206.1-875.7), waited 106 |
| `script_run` 20 console lines (6.2 KB answer) | 117.8 (115.2-118.6), waited 68 | 93.5 (92.2-100.7), waited 45 | 259.5 (233.8-269.8), waited 110 |
| `ui_dump` 50 nodes (2.9 KB) | 29.6 (27.8-43.6) | 27.9 (26.3-46.4) | 103.9 (94.9-166.6) |
| `ui_dump` 200 nodes (11 KB) | 38.7 (32.2-87.1) | 47.7 (32.7-112.0) | 144.4 (135.3-305.1) |
| `ui_dump` 400 nodes (22 KB) | 47.7 (43.7-128.9) | 45.5 (40.4-112.1) | 178.4 (163.2-310.6) |
| `ui_dump` defaults (visible only) | 33.1 (26.1-62.4), 69 nodes | 29.8 (25.9-51.2), 93 nodes | 164.1 (123.7-198.6), 109 nodes |
| `screen_capture` first call | 2249 (consent dialog tapped) | 68 | 188 |
| `screen_capture` JPEG `maxWidth` 360 | 1306 (114-1361), 360 x 640, 10.5 KB | 65.9 (55.3-72.2), 360 x 841, 13.8 KB, capture 34 | 168.6 (164.4-195.1), 360 x 576, 7.0 KB, capture 99 |
| `screen_capture` JPEG default | 1371 (1366-1381), 720 x 1280, 30 KB, capture 1351 | 72.5 (48.0-83.9), 548 x 1280, 25 KB, capture 49 | 219.3 (198.1-236.6), 800 x 1280, 25 KB, capture 107 |
| `screen_capture` JPEG `scale` 1 | 1394 (1387-1398), 1440 x 2560, 81 KB, capture 1365 | 83.3 (73.0-90.3), 1096 x 2560, 76 KB, capture 59 | 265.1 (228.5-291.1), 1800 x 2880, 90 KB, capture 131 |
| `screen_capture` PNG default | 1406 (1400-1415), 81 KB, capture 1390 | 92.2 (83.5-106.7), 73 KB, capture 76 | 277.2 (247.0-291.1), 72 KB, capture 146 |
| 4 x `device_info`, wall | 36.4 (34.7-39.7) | 27.6 (24.2-29.8) | 99.4 (63.6-108.1) |
| 4 x `ui_dump` 50 nodes, wall | 53.9 (47.5-69.1) | 36.5 (29.8-47.5) | 129.7 (115.6-169.0) |
| 4 x `script_run` `1 + 1;`, wall | 196.4 (188.2-211.1) | 105.5 (94.4-111.0) | 264.8 (236.1-295.9) |

Sizes are the encoded image (`bytes`) or the JSON answer; base64 adds a third (the largest image
answer was 120,520 base64 characters on the pad, far below the 4 MiB limit).

## Observations

1. Transport floor. `device_ping` needs no host call, so its 18-20 ms on the AVD and the phone is
   the cost of the adb hop plus the Ktor round trip; the host bridge adds 3-5 ms (`device_info`).
   The pad is three to five times slower on every row (76 ms ping, 128 ms `device_info`), which
   matches the throughput note from the battery run; the ranking of the rows is the same there.
2. `ui_dump` grows about 0.05-0.06 ms per node on the AVD and the phone (0.2 ms on the pad) and
   about 56 bytes per node in text form, so 400 nodes cost about 20 ms more than 50 and answer
   22 KB. The default visible-only dump of the same screen returns 69-109 nodes in 30-160 ms.
3. `screen_capture` on the accessibility path costs 34-76 ms inside the plugin on the phone and
   99-146 ms on the pad; the output size changes the total by 20-100 ms, not the capture. On
   API 24 the MediaProjection path costs about 1.35 s per call at every size (the capture itself,
   `durationMs` 1351-1390; the encode is negligible), and one run in five at 360 px answered in
   114 ms with a 95 ms capture, so the projection sometimes has a frame ready. The first call on
   API 24 took 2.2 s including the consent tap.
4. `script_run` round trips are 95-120 ms on the AVD and the phone and about 260 ms on the pad;
   the plugin waited 45-110 ms of that for the host's engine, and 20 console lines add nothing
   measurable. One run per device was an outlier (280-880 ms), the first script start after a
   pause.
5. Four concurrent requests finish in 1.0-1.8 times the single round trip (`script_run`: 196 ms
   for four on the AVD against 116 ms for one; 106 against 104 on the phone), so the session
   runs them in parallel up to the broker's four concurrent calls. The rate limiter did not
   trigger at four requests per round. The harness serialises only the JSON-RPC id counter of
   the shared client.

## Notes

- The x86 image has no accessibility screenshot API (added in API 30), which is why the AVD set
  exercises the MediaProjection fallback; the consent dialog reappears after every listener
  restart on API 24, so a client's first capture after a restart pays that again.
- These numbers were taken with the debug build over USB (phone, pad) or the emulator's loopback
  (AVD); a LAN client sees the network round trip instead of the adb hop (P5 LAN latency).
- Not measured here: `ui_find` and `ui_gesture` timings, sessions with more than four concurrent
  requests, and the rate limiter's `429` path (covered by the P6 rate-limit tests).
