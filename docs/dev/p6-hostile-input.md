# P6 hostile input: bounded failures on the listener

Date: 2026-09-15. Plugin 1.0.0 (build 33 tree, committed as build 34), MCP
Kotlin SDK 0.15.0 on Ktor 3.5.1 CIO. Devices: Sony G8441 (API 28,
`BH900ASK9E`), Sony XQ-AT72 (API 31, `QV710AF65F`, installed with `--user 0`),
Sony XQ-DQ72 (API 33, `QV770340J7`), Xiaomi Pad 23046RP50C (API 35,
`968e9f18`). The host AutoJs6 was not needed: every case uses the transport,
`tools/list` and the plugin-local `device_ping`.

## Delivered behavior

- `server/RequestBodyChecks.kt`: a one-pass scan of the buffered POST body
  counts the nesting of `[` / `{` outside string literals (escapes honoured)
  and stops at 65; a body deeper than 64 levels is refused with `400` and
  JSON-RPC `-32600` "Request body nests deeper than 64 levels" before
  `kotlinx.serialization` (which recurses per level) or the SDK sees it. A
  body whose request objects repeat an id is refused the same way ("Request
  ids within one body must be unique"). The request gate keeps the parsed
  request list in a call attribute so the pairing gate and the tool gate do
  not parse the body again.
- `server/SseStreamableMount.kt`: the SDK transport keys the response streams
  of a session by request id, so a second POST that reuses an id still in
  flight took over the mapping and left the first POST without an answer (its
  event stream stayed open until the client gave up). The mount now tracks
  the in-flight ids per session and refuses such a POST with `400` "a request
  with this id is still in flight on this session"; ids are released when the
  POST's body was written out, and the set is dropped with the session. A GET
  without `Mcp-Session-Id` or with an unknown one is refused with `400` /
  `404` in the route interceptor, before Ktor's SSE route commits a `200`
  event stream (previously the rejection was written into an empty stream).

## Cases and answers

| Case | Answer (same on all four devices) |
| --- | --- |
| POST body of 1 MiB + envelope | `413`, `-32600` "Request body exceeds 1048576 bytes" |
| 100 000 nested `[` | `400`, `-32600` "Request body nests deeper than 64 levels" |
| 64 nested levels | reaches the parser, answered as a JSON-RPC error |
| batch with two requests of id 1 | `400`, `-32600` "Request ids within one body must be unique" |
| bytes `FF FE 7B C3 28 22 00 7D` (invalid UTF-8) | `400`, `-32700` Parse error (replacement characters in the message) |
| truncated JSON | `400`, `-32700` |
| unknown method `no/such/method` on a session | `200`, `-32601` |
| `tools/call` with `name` as an array and `arguments` as a number | `200`, answered as an error, not dropped |
| `Accept: application/json` only | `406` |
| `Content-Type: text/plain` | `415` |
| `MCP-Protocol-Version: not-a-version` | `400` |
| GET without a session / with an unknown session | `400` / `404` (JSON body, no event stream) |
| DELETE without a session / with an unknown session | `400` / `404` |
| POST with an unknown or 4096-character session id | `404` |
| 640 000 base64 characters to an unknown tool | `200`, `isError` "Tool no_such_tool not found" |
| 1 080 000 base64 characters | `413` |
| two POSTs with id 77 at once on one session | one `200`, one `400` (in flight); id 77 usable afterwards |
| 64 concurrent sessions, each initialize + initialized + tools/list + device_ping (distinct client names) | all `200 202 200 200`, `isError=false` |

After every case the port is still open and a fresh `initialize` is answered
with a new `Mcp-Session-Id`.

## Device runs

`McpServerAdversarialTest` (androidTest, `HttpURLConnection` on 64 threads for
the concurrency case; Ktor's client was not added to the test APK, the
established `HttpURLConnection` harness of the other device tests was used;
the runs included the rate-limit flood case that lands with the next commit):

| Device | Result | 64 concurrent sessions |
| --- | --- | --- |
| Sony G8441, API 28 | 6 tests OK, 15.8 s | 3825 ms |
| Sony XQ-AT72, API 31 | 6 tests OK, 9.0 s | 1235 ms |
| Sony XQ-DQ72, API 33 | 6 tests OK, 6.2 s | 864 ms |
| Xiaomi Pad, API 35 | 5 tests OK, 1 assumption skipped (the rate-limit flood case, recorded in `docs/dev/p6-rate-limits.md`), 22.4 s | 9746 ms |

The Xiaomi Pad answers each request several times slower than the Sony
phones (about 15 requests per second from the instrumentation process); its
numbers are recorded for the P6 performance baseline rather than treated as
a failure.

JVM: `RequestBodyChecksTest` (4) and `McpTransportHardeningTest` (7) on
Ktor's test engine with the real SDK transport: over-deep, duplicate-id and
in-flight-id bodies, invalid UTF-8, truncated JSON, unknown methods, header
combinations, base64 sizes, 64 concurrent sessions.

## Findings

- Before the in-flight check, a flood of 40 `tools/list` that reused one
  request id took about 30 s on every device: each overlapping POST orphaned
  the previous one until the client's 30 s read timeout. With unique ids the
  same flood completes in 128-603 ms on the Sony phones.
- Ktor's client and `HttpURLConnection` both send `Accept: */*` when none is
  set, which the transport accepts; the `406` needs an explicit `Accept`
  without `text/event-stream`.
- The SDK answers a `tools/call` whose `name` is not a string with an error
  document rather than a transport failure; nothing to add on the plugin
  side.
