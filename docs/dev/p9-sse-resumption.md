# P9: SSE resumption (SEP-1699)

Date: 2026-10-11. Plugin 1.0.3 (build 84 sources), MCP Kotlin SDK 0.15.0, host AutoJs6 6.8.0 / 5328.

The P6 conformance run recorded that the stateful mount sent no event ids, no priming event and no
`retry` field, so a client could not resume a stream with `Last-Event-ID` (both SHOULD in the
2025-11-25 Streamable HTTP transport). This change implements resumability for the POST response
streams and the standalone GET stream.

## What the SDK provides and what it lacks

`StreamableHttpServerTransport.Configuration` accepts an `EventStore` and a `retryInterval`. With a
store, the transport stores every event it emits and sends it with that id, sends a priming event
(an id, the `retry` interval, empty data) on each new stream for clients whose protocol is
`2025-11-25` or newer, and treats a GET with `Last-Event-ID` as a resume. Three gaps remained:

- The SDK ships only the `EventStore` interface, no implementation.
- The transport stores and routes a stream's events only while the stream's call is alive. When a
  client loses its POST connection, the call ends with the failed write, and everything emitted
  afterwards, including the final response, is neither stored nor delivered.
- Its resume path replays the stored events and returns, so the GET closes at once even when the
  request is still running, and its conflict / unknown-id answers would be written after the event
  stream headers went out.

## Design

- `SessionEventStore` (one per MCP session) implements `EventStore`. Ids are decimal numbers from one
  increasing sequence per listener (`SseEventRetention`), so they are unique across all sessions and
  their order is the emission order. A session keeps its newest 512 events within 4 MiB; all
  sessions together keep at most 16 MiB (twice the host contract's descriptor payload limit), the
  oldest events going first. A single message above 4 MiB (a very large screenshot) gets an id but is
  not kept. The stream of the newest 4096 ids stays known after their messages were dropped. A
  session's events are released when it closes.
- `mcpStreamableSse` configures the transport with that store and `retry: 1000` ms.
- A POST whose client went away keeps its call until the transport closes the stream (at most six
  minutes, the longest tool budget plus slack), so the transport keeps storing its events; the
  response stream drops them instead of buffering.
- A GET with `Last-Event-ID` of a POST stream is handled by the mount: the stream sends the kept
  events after that id, then takes the GET as its reader, so later progress and the final response
  arrive on the GET, and the GET ends when the transport closes the stream. Because ids increase, an
  event that is both replayed and emitted afterwards is delivered once. A stream that already ended
  is replayed from the store and the GET ends. If the resuming GET is lost as well, the next resume
  takes over the same way.
- A GET with `Last-Event-ID` of the standalone stream goes through the SDK, and the mount keeps the
  connection open like a new standalone stream. The store reports `getStreamIdForEventId` as
  unsupported, so the SDK neither refuses a reconnecting standalone stream with `409` nor answers
  after the headers.
- An unknown or forgotten `Last-Event-ID` is refused with `400` (JSON-RPC body) before the event
  stream starts.

Limits: a client that never received an event with an id (a pre-2025-11-25 client whose stream broke
before the first message) cannot resume; events dropped by the budgets cannot be replayed; the SDK's
private standalone stream id `_GET_stream` is named in the mount and pinned by a test.

## Tests

- JVM `SseEventStoreTest` (7): ids across sessions, per-stream order without priming events, the
  per-session count and byte budgets, oversized messages, the shared budget across sessions, clearing
  a session, and the standalone replay through the SDK interface.
- JVM `SseResumptionTest` (7) on Ktor's test engine and on a real CIO listener: the priming event
  with `retry: 1000` and ids for 2025-11-25 clients, ids without priming for 2025-06-18, `400` for an
  unknown id, replay of an ended stream, the standalone stream resumed through the SDK and still
  receiving `notifications/tools/list_changed`, a GET that takes over a running POST stream (the
  tool waits on a gate until the GET attached; the POST body gets nothing after the takeover), and a
  POST dropped mid-stream whose progress and result arrive on the resuming GET. Three reruns passed.
- Whole suite: JVM 250 / 250, `lintDebug` 0 errors (11 existing warnings), debug, androidTest and
  release builds passed.
- Instrumentation `McpServerSpikeTest`, `McpServerHostSessionTest` and `McpServerAdversarialTest`
  (21 tests, protocol 2025-06-18 over the real listener) passed on Xiaomi 23046RP50C (API 35) and
  Sony G8441 (API 28).

## Device evidence

`py tools/evidence/sse_resume.py --serial <serial>` (committed tools only), both phones at the same
time, each with a held host session and protocol 2025-11-25:

| Case | Xiaomi 23046RP50C, API 35 | Sony G8441, API 28 |
| --- | --- | --- |
| Priming event of a `script_run` POST | id 3, `retry: 1000`, no message | same |
| Mid-run: POST dropped after the first progress (about 2.1 s), GET with that id 3 s later | resumed at 5156 ms with 200; 2 progress notifications and the result, ids after the resume point, increasing, no repeats; status `finished` (6887 ms); the GET ended after the result | resumed at 5293 ms; 3 progress notifications and the result; `finished` (8543 ms); GET ended |
| After the end: POST dropped after its priming event, GET 6 s later | 200; the kept progress notification and the result replayed; `finished` (3255 ms); the GET ended | same (3386 ms) |
| Unknown `Last-Event-ID` | `400`, `application/json`, `Bad Request: Last-Event-ID is not an event of this session` | same |

## Real client

Cursor CLI 2026.10.01 (WSL) against Sony XQ-AT72 (API 31) with build 84, through a local proxy that
recorded only request lines, protocol headers, statuses and the count of `id:` / `retry:` lines:
`initialize` used protocol 2025-11-25, every POST response stream (`initialize`, `tools/list`,
`tools/call`) began with a priming event with `retry: 1000` and carried event ids, and the
`device_info` call succeeded (the phone logged `tool_call device_info by Cursor`). The temporary
configuration holding the token was deleted afterwards.

Observed on the way: after the listener restarted for the update, the stdio bridge (`autojs6-mcp-bridge`
0.1.0) kept its old session id; the plugin answered `404 Session not found` as specified, and the
bridge reported it as a wrong endpoint path. Recorded as a bridge follow-up in roadmap P9.
