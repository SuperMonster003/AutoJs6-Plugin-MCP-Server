# P5.2 client compatibility matrix

Date: 2026-09-15. Plugin 1.0.0 (build 31 sources, build 32 commit), host
AutoJs6 `master` `9c3ba2e52`, phone Sony XQ-DQ72 (API 33) over
`adb forward tcp:9637 tcp:9637`, PC on Windows 11. Every verified row used the
token copied from the settings page, triggered the phone pairing prompt on
the first tool call and finished a `device_info` call after Allow. The
git-ignored script `build/tools/p5_clients.py` drove the Inspector and Codex
runs; the Claude Code row comes from the P4 evidence
(`p4-settings-and-drawer.md`, "Claude Code paste-and-connect").

| Client | Version | Transport | Authentication | Result | Known issues / notes |
| --- | --- | --- | --- | --- | --- |
| Claude Code | 2.1.257 (ACP-bundled CLI) | Streamable HTTP (`claude mcp add --transport http`) | `--header "Authorization: Bearer <token>"` | Passed: `claude mcp list` Connected; `claude -p` called `device_info` after the pairing prompt (4 turns, 33 s) | The "header not sent on some paths" bug was not observed in this version; no stdio fallback needed |
| Claude Code through `autojs6-mcp-bridge` | 2.1.257 with bridge 0.1.0 on Node.js 24.15 | stdio (`claude mcp add autojs6 -e AUTOJS6_MCP_TOKEN=<token> -- autojs6-mcp-bridge --serial <serial>`) | `AUTOJS6_MCP_TOKEN` in the server's environment block; the bridge adds the Bearer header | Passed: `claude mcp list` Connected (355 ms); `claude -p` called `device_info` (`PAIRING_REQUIRED` after 49 ms, Allow on the phone, second call 75 ms) and answered in 4 turns, 28 s | The phone sees the same `claude-code` client name and USB address class as with HTTP; the bridge removed its adb forward when the session ended. Evidence: `p5-stdio-bridge.md` |
| Codex CLI | codex-cli 0.154.0-alpha.6.2 (Codex desktop bundle) | Streamable HTTP (`codex mcp add --url`) | `bearer_token_env_var = "AUTOJS6_MCP_TOKEN"` (token only in the environment; `config.toml` holds the variable name) | Passed: `codex exec --json` made two `mcp_tool_call` items (first `PAIRING_REQUIRED`, second answered) and replied "XQ-DQ72, Android API level 33" in 49 s | Run with an isolated `CODEX_HOME`; Codex warns that it cannot create PATH aliases under a temporary home (harmless). Client name shown on the phone: `codex-mcp-client` |
| MCP Inspector CLI | 2.6.0 (`npx @modelcontextprotocol/inspector --cli`) | Streamable HTTP (`--transport http`) | `--header "Authorization: Bearer <token>"` | Passed: `tools/list` returned 33 tools; `tools/call device_info` reported `PAIRING_REQUIRED` in the JSON error, then succeeded after Allow (client name `inspector-cli`) | `--catalog` cannot be combined with an ad-hoc URL. `--strict` reported 2 portability warnings (array-valued `type` in the `arguments` map of `script_run` / `script_run_file`), fixed in this commit by an `anyOf` of single-type branches |
| MCP Inspector web UI | 2.6.0 | HTTP through the Inspector's Node proxy | header set in the UI | Not exercised | The proxy makes the request from Node, so Developer mode (CORS) is only needed when a browser page calls the endpoint directly |
| Cursor | not installed on this PC (still not installed on 2026-10-10; moved to roadmap P9 as optional) | `mcp.json` url + headers | `headers.Authorization` | Not verified | Snippet from the settings page documented in the README |
| VS Code Copilot Chat | VS Code 1.138.0 (built-in Copilot Chat, agent mode) | Streamable HTTP (user `mcp.json`: `servers.autojs6` with `type: "http"`, `url`, `headers`) | `Authorization: Bearer ${input:autojs6-token}` (password prompt, the token is not written to the file) | Passed 2026-10-10: `initialize` with protocol `2025-11-25`, pairing dialog for `Visual Studio Code` allowed on the phone, `tools/call device_info` answered and the chat reported the model, Android 12, screen, battery and host 6.8.0 | The settings page generic JSON uses `mcpServers`, VS Code needs `servers`, so the snippet cannot be pasted as is. A wrong token makes VS Code try OAuth discovery and dynamic client registration ("User did not provide client details") before it reports 401; cancel and enter the token again. Cline and Gemini CLI were not exercised |
| Gemini CLI | not installed (only a stale `~/.gemini/settings.json`) | `settings.json` `httpUrl` + headers | header | Not verified | Same URL and header |
| Claude Desktop | 2.9939.4.0 (MSIX) with bridge 0.1.0 (global npm install) on Node.js 24.15 | stdio through `autojs6-mcp-bridge` (`claude_desktop_config.json`: `command: "node"`, `args: ["<npm prefix>\\node_modules\\autojs6-mcp-bridge\\dist\\cli.js"]`, no `--serial`, manual `adb forward`) | `AUTOJS6_MCP_TOKEN` and `AUTOJS6_MCP_ADB` in the `env` block | Passed 2026-10-10: developer settings show `Running`; a call from a Claude Desktop Code session (client name `local-agent-mode-autojs6`) got `PAIRING_REQUIRED`, was allowed on the phone and the retry returned `device_info` | When the adb forward disappeared (adb server restart), the app showed the bridge's readable `Cannot connect to http://127.0.0.1:9637/mcp (connection refused) ...` notice; recreating the forward and restarting the server fixed it. `--serial` would recreate the forward on every start but removes it when the bridge exits. The regular chat of the app was not exercised separately |

## Notes

- Client identity on the phone is the `clientInfo.name` of the MCP
  `initialize` request (`claude-code`, `codex-mcp-client`, `inspector-cli`);
  each one is paired separately.
- Codex CLI evidence: the config after `codex mcp add` was
  `[mcp_servers.autojs6]` with `url` and `bearer_token_env_var` and no token
  literal; `codex mcp list` showed the server enabled with a Bearer token from
  the environment. The isolated home held a temporary copy of the maintainer's
  `auth.json`, deleted afterwards; the run log redacts the plugin token.
- The pairing records (`paired_clients.json`) created by the runs were removed
  again, the listener was stopped from the drawer and the adb forward removed.
- The README "Clients" and "FAQ" sections (10 languages) describe the
  configuration of every row and the six common failures: 401, pairing
  timeout, `HOST_UNAVAILABLE`, accessibility off, `port_in_use`, local network
  unreachable.

## 2026-10-10 re-verification

Roadmap review of 2026-10-10 reopened P5.2 and P5.3 because Cursor, VS Code /
Cline / Gemini CLI and Claude Desktop had not been verified. Both newly
installed clients were then exercised against Sony XQ-AT72 (API 31) over
`adb forward tcp:9637 tcp:9637`, plugin 1.0.3 / 78 development build (only
icon and build plugin changes since 1.0.2), host AutoJs6 6.8.0 / 5328.

| Time (UTC+08:00) | Phone log (`logcat`) |
| --- | --- |
| 21:53:59 | VS Code with a wrong token: `401 UNAUTHORIZED: the bearer token is not accepted` (VS Code log), followed by its OAuth discovery attempts |
| 22:21:28 / 22:21:31 | `Pairing requested by "Visual Studio Code" (loopback)`, then `Client paired: "Visual Studio Code"` |
| 22:21:32 | `tool_call device_info by Visual Studio Code` |
| 22:22:47 / 22:23:38 | `Pairing requested by "local-agent-mode-autojs6" (loopback)`, then `Client paired` |
| 22:23:46 | `tool_call device_info by local-agent-mode-autojs6` |

With these two rows five clients have real results (Claude Code, Codex CLI,
MCP Inspector CLI, VS Code Copilot Chat, Claude Desktop), which meets the P5
acceptance condition. The first VS Code attempt with another model failed at
the model provider before any MCP call; it is unrelated to the plugin.
