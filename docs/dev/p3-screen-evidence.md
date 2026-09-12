# P3.3 screen evidence

Date: 2026-09-13. Plugin: 1.0.0 / 21 development build. Host: AutoJs6 6.8.0 / 5280,
with this session's encoded `image.captureScreen(options)` change. MCP client: the local
Python 3 stdlib Streamable HTTP driver `build/p33_device_evidence.py`, clientInfo
`p33-device-evidence/1.0`, protocol `2025-11-25`. Images were base64-decoded and checked
with Pillow; neither token nor image content was written to the evidence logs.

## Automated checks

- Plugin JVM: 157 tests, zero failures/errors. New `ScreenToolsTest` has 13 cases;
  `HostBridgeClientTest` adds descriptor ownership/length cases. Manifest identity,
  runtime info, punctuation, catalog snapshot, and the existing groups remain covered.
- Plugin `connectedDebugAndroidTest` passed on five physical devices. Final APKs were then
  installed and all 16 tests run through `adb am instrument` on AVD x86 API 24 (12.035 s),
  Sony G8441 API 28 (17.704 s), Sony XQ-AT72 API 31 (12.334 s), Sony XQ-DQ72 API 33
  (9.491 s), Xiaomi 22120RN86C API 33 (18.338 s), Xiaomi 23046RP50C API 35 (12.486 s).
  Logs: `build/p33-final-device-<serial>.log`. The screen test exercises HTTP image content with real PFD reads,
  both capture paths, permission declarations, denial guidance, screen state and group gating.
- Host `NodeBridgeScreenshotEncodingTest`: final 4 tests on API 24 / 28 / 31 / 33 / 35. Checks
  JPEG/PNG/WebP decoded dimensions after crop/scale, bitmap release, deterministic noisy PNG
  rejection above 4 MiB, invalid options, permission revocation, recovery from missing frames,
  and the retry ceiling. Logs: `build/p33-encoder-final-<serial>.log` in the plugin checkout.
- Debug and androidTest assemblies pass; lint reports 0 errors / 6 existing warnings;
  release assembly with R8 and native alignment verification passes.
- Documentation: `generate_markdown.py --check` reports 10 languages and 36 artifacts.

## Real host and device paths

The driver opens the host's real `McpServerPluginRoundTripTest` with its bounded hold mode,
reads the token through developer-mode `dumpsys`, initializes an HTTP session, pairs the test
client, and calls the normal tool API. The host test then stops/closes its session. Original
accessibility service lists, rotation settings, developer-mode config and pairing state are
restored after the run. Each device uses a separate adb forward port.

| Device | Path | Evidence |
| --- | --- | --- |
| AVD Android SDK built for x86, API 24, `emulator-5554` | MediaProjection | Consent approved once; first JPEG 720x1280 took 22,213 ms including manual confirmation. Two following JPEGs took 1,384 / 123 ms without another prompt. WebP crop 250x250 took 1,363 ms. Rotated state was 2560x1440, landscape, 90 degrees; PNG 1000x563 took 123 ms and default JPEG 1280x720 took 121 ms. |
| Sony G8441, API 28, `BH900ASK9E` | MediaProjection | Final host with the retry fix: phone consent approved once; first JPEG 720x1280 took 23,622 ms including manual confirmation. Two following JPEGs took 185 / 183 ms without another prompt. WebP crop 250x250 took 197 ms. Rotated PNG 1000x563 took 456 ms and default JPEG 1280x720 took 157 ms. |
| Sony XQ-AT72, API 31, `QV710AF65F` | Accessibility | Three consecutive portrait JPEGs 548x1280 took 236 / 117 / 247 ms. Final rotated state 2560x1096, landscape, 90 degrees; JPEG 1280x548 took 218 / 87 / 292 ms. WebP crop 250x250 took 298 ms; PNG 1000x428 took 105 ms. |
| Xiaomi 23046RP50C, API 35, `968e9f18` | Accessibility | Three consecutive portrait JPEGs 800x1280 took 183 / 106 / 179 ms. WebP crop 250x250 took 248 ms; rotated PNG 1000x625 took 130 ms. |
| Xiaomi 23046RP50C, API 35 | Size reduction | A seeded noisy UI fixture at 2880x1800 requested PNG at scale 1. The real host/plugin path reduced it over 4 capture attempts to 988x617, 1,817,197 compressed bytes / 2,422,932 base64 bytes in 2,621 ms; `adjusted=true`. The script was stopped by execution id afterward. |

Real host round trips on API 24 / 28 / 31 / 35 each finished with `OK (1 test)` and stopped
the listener. Raw summaries are `build/p33-evidence-<serial>.log`,
`build/p33-noise-968e9f18.log`, and `build/p33-roundtrip-<serial>.log` (ignored files).
The measurements are correctness evidence, not a performance benchmark.

## Findings and corrections

1. The accessibility screenshot permission envelope initially declared only `accessibility`.
   The actual host answered `CAPABILITY_DENIED`, naming the missing `screen_capture` token.
   The flow now declares both and the instrumented envelope assertion covers this regression.
2. Rotation reproduced on API 28 / 31 / 35: the host's dimensions already followed display
   rotation while its cached Configuration still said portrait. For example, the tablet had
   rotation 1 and produced a 1000x625 PNG, while the initial screen-state shaping reversed the
   dimensions to 1800x2880. The plugin now preserves the dimensions and derives orientation
   from their aspect ratio. A recorded-shape JVM regression failed before this correction and
   passed afterward; the final API 31 run reports 2560x1096 and produces 1280x548 images.
   An IDE logpoint run was attempted but ended with zero hits and no retained output; the
   diagnosis relies on the real HTTP values and the failing/passing regression, not a debugger hit.
3. MIUI on API 35 refused the adb foreground-service start from the background even though
   component resolution succeeded. Preparing developer config while stopped and letting the
   authorized host open the session exercised the existing production entry without relaxing
   component permissions. No true cold-install activation acceptance is claimed here.
4. One API 28 instrumentation launch crashed in the system ART `ADB-JDWP Connection` thread
   before application initialization. Re-running completed the real projection path and
   round trip. The system crash is not attributed to the screenshot encoder.
5. API 24 came online during final verification. The first real run returned the first image
   but the second failed with `No screen frame is available yet` (preserved in
   `build/p33-api24-before-retry.log`). `NodeScreenCaptureSession` called the underlying
   capturer only once, while the established `Images.captureScreen` path retries after the
   capturer refreshes its display on a 1.2 s frame timeout. The bridge now retries up to six
   times with 40 ms between attempts, within its 15 s budget, and never returns a cached
   bitmap. The deterministic regression covers missing/missing/frame, six missing frames,
   and immediate exception propagation. Real API 24 then returned all six images, including
   a second JPEG at 1,384 ms; API 28 also passed again. The IDE refused host test locations
   outside its current plugin project, so this finding uses the observed HTTP failure,
   comparison with the host implementation, and the successful device regression.

## Scope left for later

API 24 and API 28 both cover the pre-30 fallback. Inspector/Claude visual rendering was not re-tested for the new image
content; the protocol driver decoded every returned image, and P5 retains the client matrix.
Optional node annotations remain in P6. No release was published and no public tunnel was used.
