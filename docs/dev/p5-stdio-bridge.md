# P5.3 stdio bridge: autojs6-mcp-bridge evidence

Date: 2026-09-15. Bridge `autojs6-mcp-bridge` 0.1.0 (separate repository
`AutoJs6-MCP-Bridge`, TypeScript, Node.js 18 or newer, MPL-2.0) built and
tested with Node.js 24.15.0 and `@modelcontextprotocol/sdk` 1.30.0. Plugin
1.0.0 (build 32 tree), host AutoJs6 `master` `9c3ba2e52`, phone Sony XQ-DQ72
(API 33) over USB, PC on Windows 11 with Claude Code 2.1.257. The git-ignored
script `build/tools/p5_bridge_e2e.py` drove the acceptance run; the probes
`p5_bridge_probe*.py` isolated the fresh-forward reset described below.

## Delivered behavior

- `src/options.ts`: `--url` (default `http://127.0.0.1:9637/mcp`), `--serial`
  / `--forward`, `--adb` (or `AUTOJS6_MCP_ADB`), `--protocol`, `--token`;
  the token is read from `AUTOJS6_MCP_TOKEN` first and `--token` prints a
  warning because process arguments are visible to other users. `--serial`
  and `--forward` are rejected with a non-loopback URL. Exit codes: 0 normal,
  1 unexpected error, 2 invalid options, 3 `adb forward` failure.
- `src/forward.ts`: `adb [-s serial] forward tcp:<port> tcp:<port>` before
  the first request, `adb forward --remove tcp:<port>` exactly once on stdin
  end, SIGINT or SIGTERM; the port is taken from the endpoint URL.
- `src/bridge.ts`: the SDK `StdioServerTransport` faces the client, the SDK
  `StreamableHTTPClientTransport` faces the phone with the Bearer header,
  `Mcp-Session-Id` and the negotiated `MCP-Protocol-Version`. Messages pass
  through unchanged except an optional rewrite of `initialize`'s
  `protocolVersion` (`--protocol`). Requests that cannot be delivered are
  answered with JSON-RPC error -32000 and one readable sentence; the phone's
  own errors (for example `PAIRING_REQUIRED`) pass through as they are.
  `initialize` is repeated up to 4 times (500 ms apart) on connection-level
  failures (`ECONNREFUSED`, `ECONNRESET`, `UND_ERR_SOCKET`, `EPIPE`); other
  requests are never repeated. When stdin ends, requests still in flight get
  up to 10 s before the bridge closes (the SDK transport itself does not
  watch for the end of stdin).
- `src/errors.ts`: HTTP 401 (token rejected), 403 (allow list), 404 (wrong
  path), 421 (Host header), 429, 503 and any other status, plus
  `ECONNREFUSED`, `ETIMEDOUT` / `EHOSTUNREACH` / `ENETUNREACH`, `ENOTFOUND` /
  `EAI_AGAIN`, `ECONNRESET` / `UND_ERR_SOCKET` found anywhere in the fetch
  cause chain, and the SDK's `UnauthorizedError`, each map to one sentence
  that names the action to take.
- README (English and Simplified Chinese) with the install command, the
  option table, Claude Desktop and Claude Code snippets, the error table and
  the compatibility table that the plugin README mirrors; CHANGELOG 0.1.0.

## Unit tests and package audit

`npm test` builds `dist/` and runs `node --test`: 23 tests pass.

| File | Covers |
| --- | --- |
| `test/options.test.mjs` | defaults, `AUTOJS6_MCP_TOKEN` precedence over `--token`, invalid URL / missing token / `--serial` with a Wi-Fi URL rejected, `--help` and `--version` |
| `test/forward.test.mjs` | forward and single removal for `--serial` and `--forward`, `adb` failure reported with its stderr, custom `--adb` path |
| `test/errors.test.mjs` | HTTP status and socket error code mapping, `UnauthorizedError`, unknown errors |
| `test/bridge.test.mjs` | against a local fake phone: protocol passthrough and `--protocol` rewrite, Bearer / session id / protocol headers, 401 and connection refused answered as JSON-RPC errors, session termination on close, `initialize` retried after a reset while later requests are not |

`npm pack --dry-run`: 17 files, 20.4 kB packed, 62.5 kB unpacked (`dist/`,
both READMEs, CHANGELOG, LICENSE). `npm ls --omit=dev --all` lists 95
packages under the single runtime dependency `@modelcontextprotocol/sdk`;
`npm audit --omit=dev` reports 0 vulnerabilities.

## CLIENT_E2E: Claude Code over stdio through the bridge

Run 4 of `p5_bridge_e2e.py QV770340J7` (17:28-17:30):

1. Listener switched on from the AutoJs6 drawer (loopback), token copied by
   the clipboard instrumentation of the settings page, all adb forwards
   removed first.
2. `claude mcp add autojs6 -e AUTOJS6_MCP_TOKEN=<token> -- node
   <bridge>/dist/cli.js --serial QV770340J7` in an isolated
   `CLAUDE_CONFIG_DIR`; the token is only in the server's environment block.
3. `claude mcp list`: `autojs6 ... Connected`. Claude Code's MCP log shows
   the bridge's stderr (`adb forward tcp:9637 active for QV770340J7`,
   `bridging stdio to http://127.0.0.1:9637/mcp`), "Successfully connected
   (transport: stdio) in 355ms" and the plugin's capabilities; after the
   probe the forward list was empty again.
4. `claude -p "Call the autojs6 device_info tool ..." --allowedTools
   mcp__autojs6__device_info --output-format json --max-turns 6` with a
   debug log: connection in 333 ms; first `device_info` answered by the
   phone with `MCP error -32002: PAIRING_REQUIRED: confirm "claude-code" on
   the phone within 59 s, then retry` after 49 ms; the phone showed the
   pairing dialog "claude-code wants to control this device through the MCP
   server (from this computer over USB (adb), token ending in ...)" and the
   heads-up notification, accepted by the script; the second `device_info`
   completed in 75 ms; the model replied "Model: XQ-DQ72 (Sony). Android API
   level: 33." after 4 turns and 28 s. The bridge process closed cleanly
   when Claude Code ended the session and the adb forward was gone.
5. Cleanup: the `claude-code` pairing record was removed again, the listener
   stopped from the drawer, the forward list left empty.

The phone sees the bridged client exactly as it sees a direct HTTP client:
same `clientInfo.name`, USB (loopback) address class, same pairing record.
The pairing screenshot shows personal file names of the phone and is not
copied into the repository.

## Findings during the run

- Fresh-forward reset: the first `initialize` through a forward the bridge
  had just created was answered with a connection reset in roughly one of
  three attempts (`p5_bridge_probe3.py`: reset, then 417 ms and 364 ms
  successes; a pre-existing forward always succeeded in about 330 ms), while
  ten immediate Python `urllib` probes after `adb forward` all received the
  expected 401. The bridge therefore repeats `initialize` on connection-level
  failures; with the retry, `claude mcp list` connected on every run.
- Run 3 of the acceptance script stalled for 300 s in `claude -p` with no MCP
  activity after the connection was established (no tool call reached the
  bridge or the phone) and the script's own timeout ended it; runs 2 and 4
  completed the same prompt in 27-28 s. The stall belongs to the model
  gateway, not to the bridge; the script now treats a timeout as a retryable
  attempt and keeps the Claude Code debug log of every attempt.
- The SDK stdio server transport keeps running after the client closes
  stdin; without the bridge's own `end` / `close` handling the process (and
  the adb forward) would outlive the client.

## Not verified here

- Claude Desktop is not installed on this PC (only a leftover
  `claude_desktop_config.json`); the `claude_desktop_config.json` snippet in
  the bridge README follows its documented stdio server format and was not
  exercised.
- The package is prepared (`build/autojs6-mcp-bridge-0.1.0.tgz` in the bridge
  repository, git-ignored) but not yet published to npm; publishing needs the
  maintainer's npm account, and the plugin README already names
  `npm install -g autojs6-mcp-bridge` as the install command.
- Node.js 18, 20 and 22 were not run; the package declares `engines.node
  >=18` and uses only APIs available since Node 18 (`fetch` through the SDK,
  `node:test` only for the test suite).
