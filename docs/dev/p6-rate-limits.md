# P6 rate limits: per-client request and screenshot windows

Date: 2026-09-15. Plugin 1.0.0 (build 34 tree, committed as build 35).
Devices as in `p6-hostile-input.md` (API 28 / 31 / 33 / 35).

## Delivered behavior

- `server/RateLimiter.kt`: sliding windows per client key, `RateLimits`
  defaults of 20 requests per 1 s and 30 screenshots per 60 s, at most 256
  tracked clients (least recently seen dropped first), clients silent for
  5 min forgotten. A refusal reports the exact `retryAfterMs` (oldest stamp +
  window - now) and the `Retry-After` header value in whole seconds.
- `server/RateLimitInterceptor.kt`: installed after the bearer check and
  before the pairing gate, so only authenticated traffic is counted and a
  flood never reaches the pairing prompts or the host. The client key is the
  identity the pairing gate uses (the `clientInfo.name` of the session, or
  the `User-Agent` before `initialize`) plus the remote address, so an
  invented `Mcp-Session-Id` does not open a fresh window.
  - Over the per-second window: HTTP `429`, `Retry-After: <s>`, JSON-RPC
    error `-32004` "RATE_LIMITED: at most 20 requests per 1 s per client;
    retry after N ms" with `data` = `{code, retryAfterMs, limit, windowMs}`,
    one document per request id of the body (null id for GET / DELETE).
  - A `tools/call` of `screen_capture` over the per-minute window: HTTP
    `200` with an `isError` tool result "RATE_LIMITED: screen_capture is
    limited to 30 calls per 60 s per client (wait N ms before calling it
    again)" and `structuredContent.error.retryAfterMs`, so the model reads
    the wait instead of losing the transport. `ToolFailure` gained the
    optional `retryAfterMs` field; the `screen_capture` description names the
    limit.
- The host grant's `accessibilityQueriesPerSecond` stays in force behind the
  plugin's windows (`RATE_LIMITED` from the host maps through
  `ToolFailure.fromBridge`, without `retryAfterMs`).
- Every listener owns one limiter (`McpHttpServer.rateLimiter`), dropped on
  stop.

## Device runs

`McpServerAdversarialTest.aRequestFloodIsRateLimitedWithRetryAfterAndRecovers`:
one session, 40 `tools/list` with unique ids on 8 threads, then a wait of
`retryAfterMs` + 150 ms and one more request.

| Device | Flood | Statuses | First `429` | Afterwards |
| --- | --- | --- | --- | --- |
| Sony G8441, API 28 | 524 ms | 18 x 200, 22 x 429 | `Retry-After: 1`, `retryAfterMs` 530 | `200` |
| Sony XQ-AT72, API 31 | 603 ms | 18 x 200, 22 x 429 | `Retry-After: 1`, `retryAfterMs` 549 | `200` |
| Sony XQ-DQ72, API 33 | 128 ms | 18 x 200, 22 x 429 | `Retry-After: 1`, `retryAfterMs` 879 | `200` |
| Xiaomi Pad, API 35 | 2429 ms | 40 x 200 | none: 16.5 requests per second is below the limit; the test skips with an assumption message | `200` |

The 18 (not 20) accepted requests: the test sends the same name as
`User-Agent` and as `clientInfo.name`, so its `initialize` and `initialized`
already hold two stamps of the window when the flood starts.

JVM: `RateLimiterTest` (5: window and exact retry, independent clients and
screenshot window, `Retry-After` rounding, idle forgetting and the tracking
ceiling, defaults) and `RateLimitInterceptorTest` (3, Ktor test engine with
the real SDK transport: `429` with header and body, screenshot tool result
with `retryAfterMs` and recovery, forged session ids sharing the window). The
screenshot window was not exercised on the device: 31 real `screen_capture`
calls would need the host's capture consent and would take real screenshots.

## Notes

- Requests before `initialize` and after it fall into two windows of the
  same client (User-Agent versus `clientInfo.name`); both are bounded, and a
  client with a fixed name cannot evade the limit by rotating session ids.
- The MCP Inspector, Claude Code, Codex and the bridge send at most a few
  requests per second in normal use; 20 per second leaves headroom for
  `ui_wait_for` style polling while keeping a runaway loop from starving the
  host. The bridge's error table already names the `429` case.

## P11 addendum: the shared screenshot window (D39, 2026-10-15)

The plugin-family recognition tools take a screenshot inside AutoJs6 when their `image` argument is
`"screen"` (`ocr_recognize`, `barcode_detect`, `opencv_find_image`, `opencv_find_color`,
`opencv_find_multi_colors`; `RateLimiter.SCREEN_IMAGE_TOOLS` is every plugin-family row with an `image`
property). Such a call draws from the same per-client window of 30 per 60 s as `screen_capture`
(`RateLimiter.countsAsScreenshot`, which the interceptor now asks instead of matching tool names), so
recognizing the screen cannot watch it faster than capturing it. An image file is not counted, nor is
`"SCREEN"` or a path that merely contains the word. A refused recognition answers `RATE_LIMITED` with
`retryAfterMs`, the message "<tool> with image "screen" takes a screenshot, and screenshots are limited
to 30 per 60 s per client together with screen_capture" and the hint to wait or pass an image file;
`screen_capture` keeps its own wording. The five tool descriptions end with the same note.

Tests: `RateLimiterTest.screenReadingRecognitionCallsCountAsScreenshotsButFilesDoNot`,
`RateLimitInterceptorTest.recognitionOfTheScreenSharesTheScreenshotWindowAndFilesDoNotCount`. Device:
`tools/evidence/p11_screen_quota.py` (30 interleaved `screen_capture` / `opencv_find_color` /
`barcode_detect` screen calls, the 31st refused, a file call not counted, recovery after the wait).
