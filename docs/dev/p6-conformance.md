# P6 MCP conformance: the official suite against the stateful path, and the stateless path that has no route

Date: 2026-09-15. Plugin 1.0.1 (build 42 tree plus the Origin change below, committed as build
43), host AutoJs6 6.8.0 (5280), MCP Kotlin SDK 0.15.0 on Ktor 3.5.1 CIO. Harness:
`@modelcontextprotocol/conformance` 0.1.16 (the npm `latest`, published 2026-08-07) on Node.js
24.15.0. Devices: Sony XQ-DQ72 (API 33, `QV770340J7`) and Xiaomi Pad 23046RP50C (API 35,
HyperOS, `968e9f18`), both with the host attached through the drawer switch. The two runs
produced the same 32 verdicts.

## What the roadmap item asked, and which suite

"Run the official conformance suite (the conformance tests that ship with kotlin-sdk 0.15.0 or
`modelcontextprotocol/conformance`) once against the stateful and once against the stateless
path; record the failures and their reasons."

- kotlin-sdk 0.15.0 ships no suite that can be pointed at a third-party server (its
  conformance module tests the SDK's own client and server), so the run uses the npm harness,
  which is what the MCP organisation runs against the official SDKs.
- 0.1.16 has 32 server scenarios, all for the stateful model (`initialize` handshake,
  `Mcp-Session-Id`), tagged `2025-06-18` and / or `2025-11-25`. The `server-stateless` scenario
  for the `2026-07-28` model exists only on the repository's main branch and is not in any
  published version, so the stateless model could not have been exercised by the harness even
  if the plugin mounted it.
- The plugin mounts only the stateful transport (decision D9 as amended on 2026-09-10; the
  stateless model waits for SDK support). The stateless round is therefore four probes that
  document the answers a stateless client gets today (last section).

## Method

`build/tools/p6_conformance.py <serial> [--local-port N] [--proxy-port N] [--only a,b]` (git-ignored
evidence tooling, on top of the P4 / P6 helpers):

1. Listener on through the AutoJs6 drawer (host attached), token read from the settings page
   through the instrumentation helper, `adb forward` to a per-device PC port.
2. The harness identifies itself as `conformance-test-client` 1.0.0 and starts a new session
   per scenario, so that identity is paired once through the real prompt before the run (the
   dialog on the Sony, the expanded notification action on the pad). Every scenario then
   passes the pairing gate. The `dns-rebinding-protection` scenario uses a second identity
   (`conformance-dns-rebinding-test`) that only sends `initialize`, which is not gated.
3. The harness has no option for request headers, and the plugin refuses everything without
   the Bearer token. `build/tools/p6_conformance_proxy.mjs` is a Node loopback proxy that adds
   `Authorization: Bearer <token>` to every request, rewrites only the port of the `Host`
   header (its own port to the device port; the hostname stays, so the rebinding scenario's
   `evil.example.com` reaches the plugin unchanged), passes `Origin` and every other header
   through, pipes SSE bodies as they arrive and logs both directions with the token redacted
   (`build/p6c/proxy-<serial>.log`).
4. One harness process per scenario with a 1.5 s pause: `server --url ... --scenario <name>
   -o <dir>`. The first attempt ran all scenarios in one process (`--suite all`) and the
   harness fired them within about a second: from the eighth scenario on the plugin answered
   `429` / `-32004 RATE_LIMITED` (20 requests per second per client, roadmap P6) and 28 of 32
   scenarios failed for that reason alone. The limiter is the intended behaviour for a client
   that bursts; the pacing keeps the harness under it.
5. `checks.json` per scenario is the record (`build/p6c/results-<serial>/<scenario>/`), plus
   the harness's own text output and the proxy log. On Windows every scenario process ends
   with a libuv assertion (`!(handle->flags & UV_HANDLE_CLOSING)`, exit code `0xC0000409`)
   after it has written its results, so the process exit code is not usable there; the
   `checks.json` files are.

## Results (both devices)

11 checks pass in 9 scenarios, 23 checks fail in 23 scenarios, and the SSE polling scenario
adds 4 informational and 2 warning entries. The failures fall into two groups that have
nothing to do with the transport:

- Fixture-bound (18 scenarios): the harness calls the reference server's fixtures (tools
  `test_simple_text`, `test_image_content`, `test_audio_content`, `test_embedded_resource`,
  `test_multiple_content_types`, `test_tool_with_progress`, `test_error_handling`,
  `test_sampling`, `test_elicitation`, `test_elicitation_sep1034_defaults`,
  `test_elicitation_sep1330_enums`, `json_schema_2020_12_tool`, `test_reconnection`; prompts
  `test_simple_prompt`, `test_prompt_with_arguments`, `test_prompt_with_embedded_resource`,
  `test_prompt_with_image`; resources `test://static-text`, `test://static-binary`,
  `test://template/123/data`, `test://watched-resource`). The plugin's catalog is the
  AutoJs6 tool set, so it answers an unknown tool with a tool result (`isError: true`, "Tool
  X not found"), an unknown prompt with `-32602 Unknown prompt`, and a foreign URI with
  `-32602 Invalid AutoJs6 resource URI`. Two scenarios pass on those answers alone
  (`tools-call-simple-text` wants text content, `tools-call-error` wants an error result), which
  says nothing about the plugin's real tools.
- Capability-bound (5 scenarios): the plugin declares `tools` (`listChanged`), `resources`
  (`subscribe: false`, `listChanged: false`) and `prompts`; it declares neither `logging` nor
  `completions`, and it never issues `sampling/createMessage` or `elicitation/create`. The
  SDK answers `logging/setLevel`, `completion/complete` and `resources/subscribe` /
  `unsubscribe` with `-32601 Server does not support ...`, which is the specified answer for an
  undeclared capability; the harness counts it as a failure because its reference server
  declares them all.

| Scenario | Spec | Result | Group | What the plugin answered |
| --- | --- | --- | --- | --- |
| `server-initialize` | 06-18, 11-25 | pass | protocol | `2025-11-25` negotiated, `serverInfo` `autojs6-mcp-server` 1.0.1, capabilities as above |
| `logging-set-level` | 06-18, 11-25 | fail | capability | `-32601 Server does not support logging/setLevel` |
| `ping` | 06-18, 11-25 | pass | protocol | empty result |
| `completion-complete` | 06-18, 11-25 | fail | capability | `-32601 Server does not support completion/complete` (ref `test_prompt_with_arguments`) |
| `tools-list` | 06-18, 11-25 | pass | protocol | 33 tools with the host attached |
| `tools-call-simple-text` | 06-18, 11-25 | pass (vacuous) | fixture | `test_simple_text` unknown: `isError` result with text content, which is all the check wants |
| `tools-call-image` | 06-18, 11-25 | fail | fixture | `test_image_content` unknown: "No image content found" |
| `tools-call-audio` | 06-18, 11-25 | fail | fixture | `test_audio_content` unknown: "No audio content found" |
| `tools-call-embedded-resource` | 06-18, 11-25 | fail | fixture | `test_embedded_resource` unknown: "No resource content found" |
| `tools-call-mixed-content` | 06-18, 11-25 | fail | fixture | `test_multiple_content_types` unknown |
| `tools-call-with-logging` | 06-18, 11-25 | fail | capability | `logging/setLevel` first, `-32601` |
| `tools-call-error` | 06-18, 11-25 | pass (vacuous) | fixture | `test_error_handling` unknown: `isError` result, which is what the check wants |
| `tools-call-with-progress` | 06-18, 11-25 | fail | fixture | `test_tool_with_progress` unknown: no progress notifications (the plugin sends them for its own long-running tools, e.g. `script_run`) |
| `tools-call-sampling` | 06-18, 11-25 | fail | fixture | `test_sampling` unknown; the plugin never requests sampling |
| `tools-call-elicitation` | 06-18, 11-25 | fail | fixture | `test_elicitation` unknown; the plugin never requests elicitation |
| `json-schema-2020-12` | 11-25 | fail | fixture | `json_schema_2020_12_tool` not in the catalog (the check lists the plugin's tools) |
| `elicitation-sep1034-defaults` | 11-25 | fail | fixture | `test_elicitation_sep1034_defaults` unknown |
| `server-sse-polling` | 11-25 | fail + 2 warnings | fixture, SHOULD | `test_reconnection` unknown (fail); warnings: no priming event with an id and no `retry` field on the POST SSE stream (SEP-1699 resumability, both SHOULD) |
| `server-sse-multiple-streams` | 11-25 | pass 2 / 2 | protocol | concurrent POST SSE streams accepted and functional |
| `elicitation-sep1330-enums` | 11-25 | fail | fixture | `test_elicitation_sep1330_enums` unknown |
| `resources-list` | 06-18, 11-25 | pass | protocol | the AutoJs6 resource list |
| `resources-read-text` | 06-18, 11-25 | fail | fixture | `test://static-text`: `-32602 Invalid AutoJs6 resource URI` |
| `resources-read-binary` | 06-18, 11-25 | fail | fixture | `test://static-binary`: `-32602` |
| `resources-templates-read` | 06-18, 11-25 | fail | fixture | `test://template/123/data`: `-32602` |
| `resources-subscribe` | 06-18, 11-25 | fail | capability | `-32601 Server does not support resources/subscribe` (`subscribe: false` declared) |
| `resources-unsubscribe` | 06-18, 11-25 | fail | capability | same |
| `prompts-list` | 06-18, 11-25 | pass | protocol | `write_autojs6_script`, `automate_task`, `debug_selector` |
| `prompts-get-simple` | 06-18, 11-25 | fail | fixture | `test_simple_prompt`: `-32602 Unknown prompt` |
| `prompts-get-with-args` | 06-18, 11-25 | fail | fixture | `test_prompt_with_arguments`: `-32602` |
| `prompts-get-embedded-resource` | 06-18, 11-25 | fail | fixture | `test_prompt_with_embedded_resource`: `-32602` |
| `prompts-get-with-image` | 06-18, 11-25 | fail | fixture | `test_prompt_with_image`: `-32602` |
| `dns-rebinding-protection` | 11-25 | pass 2 / 2 | protocol | `Host: evil.example.com` + matching `Origin`: `403 Host not allowed`; loopback `Host` + `Origin`: `200` (see the change below; 1 / 2 before it) |

Every JSON-RPC message the plugin sent passed the harness's wire-schema validation (no
`wire-schema-valid` failure in any scenario). Proxy-level statistics of the Sony run: 94
responses `200`, 31 `202` (notifications), 1 `403` (the rebinding probe), 1 `400` (the
script's own empty-body probe); the listener stayed `running` with the host attached, same
pid, notification intact, and answered `401` to the unauthenticated probe afterwards.

## Findings and changes

1. The rate limiter stops a one-process run of the suite (above). Nothing changed: 20
   requests per second per client is the P6 design for a burst, and real clients issue a
   handful of requests at start-up. Anyone repeating the run must pace the scenarios (the
   script does) or raise the limit for the measurement.
2. Loopback `Origin` headers were refused outside developer mode. The P2.1 rule was "any
   `Origin` is refused unless developer mode admits the Inspector's loopback origin through
   CORS", and the suite's `localhost-host-valid-accepted` check (a loopback `Host` with a
   matching loopback `Origin`, expecting `2xx`) got `403 Origin not allowed`. The specification
   asks servers to validate `Origin` against DNS rebinding, not to refuse loopback origins, so
   `RequestGate` now accepts a loopback `Origin` in every mode and still refuses any other
   origin with `403`. CORS did not change: `Access-Control-Allow-Origin` / `Expose-Headers`
   are only emitted in developer mode, and an `OPTIONS` preflight with an `Origin` outside
   developer mode is refused (`403 Cross-origin access needs developer mode`), so a browser page
   still cannot complete a cross-origin call by default (its preflight fails, and an
   `EventSource` cannot carry the Bearer token). MCP clients on the PC send no `Origin` and are
   unaffected. JVM: `RequestGateTest` and `McpTransportTest` updated (233 tests, 0 failures;
   lint 0 issues).
3. SEP-1699 resumability (SHOULD): the plugin's SSE mount sends no event ids, no priming
   event and no `retry` field, so a client cannot resume a stream with `Last-Event-ID`. SDK
   0.15.0 has an `EventStore` interface for this; the plugin's mount (`mcpStreamableSse`,
   P3.1) does not use it. Not changed here: sessions are short-lived and a client that loses
   its stream re-initializes; recorded as a follow-up candidate for the SSE mount.
4. Harness quirks: the Windows exit-code crash above; the SSE polling scenario negotiates
   `2025-03-26` instead of `2025-11-25`; the `list --server` output is the only scenario
   catalog (no documentation of the fixture names, which had to be read from the proxy log).

## The stateless path

Probes through the same forward with the Bearer token (identical on both devices):

| Probe | Answer |
| --- | --- |
| `initialize` with `protocolVersion` `2026-07-28` and the `MCP-Protocol-Version: 2026-07-28` header | `400`, `-32000 Bad Request: Unsupported protocol version (supported versions: 2025-11-25, 2025-06-18, 2025-03-26, 2024-11-05)` |
| `server/discover` without a session, with `Mcp-Method` / `Mcp-Name` headers | `400`, `-32000 Bad Request: Server not initialized` |
| `tools/list` without `initialize` (the SDK's session-less 2025 semantics) | `400`, `-32000 Bad Request: Server not initialized` |
| `POST /mcp/stateless` | `404` |

This is decision D9 as recorded in P0.2 and amended on 2026-09-10: SDK 0.15.0 declares
`2024-11-05` to `2025-11-25`, its session-less mount is the 2025 semantics rather than the
`2026-07-28` model, and the maintainer deferred both the session-less mount and the
`2026-07-28` model until the SDK supports them. The 0.1.16 harness has no scenario for either,
so the first stateless run will need both a plugin route and a harness version that ships
`server-stateless`.

## Notes

- Devices: the Sony phone showed the pairing prompt as the host dialog; the pad showed it as
  a grouped notification whose action row needed two expansions (HyperOS), as in the
  lifecycle matrix. The pad run took 211 s against 139 s on the phone, all of it in the
  harness's per-process start-up and its pauses, not in the plugin.
- Both devices had the debug build 43 and the instrumentation APK installed for the token
  read; the paired-client file was restored (pad) or removed (phone, none before) afterwards
  and the listener was switched off through the drawer.
- The harness's client name `conformance-test-client` is a third paired identity while the
  run lasts; the notification on the pad read "Paired clients: 2" because one client was
  paired before the run.
