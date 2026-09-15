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
| Codex CLI | codex-cli 0.154.0-alpha.6.2 (Codex desktop bundle) | Streamable HTTP (`codex mcp add --url`) | `bearer_token_env_var = "AUTOJS6_MCP_TOKEN"` (token only in the environment; `config.toml` holds the variable name) | Passed: `codex exec --json` made two `mcp_tool_call` items (first `PAIRING_REQUIRED`, second answered) and replied "XQ-DQ72, Android API level 33" in 49 s | Run with an isolated `CODEX_HOME`; Codex warns that it cannot create PATH aliases under a temporary home (harmless). Client name shown on the phone: `codex-mcp-client` |
| MCP Inspector CLI | 2.6.0 (`npx @modelcontextprotocol/inspector --cli`) | Streamable HTTP (`--transport http`) | `--header "Authorization: Bearer <token>"` | Passed: `tools/list` returned 33 tools; `tools/call device_info` reported `PAIRING_REQUIRED` in the JSON error, then succeeded after Allow (client name `inspector-cli`) | `--catalog` cannot be combined with an ad-hoc URL. `--strict` reported 2 portability warnings (array-valued `type` in the `arguments` map of `script_run` / `script_run_file`), fixed in this commit by an `anyOf` of single-type branches |
| MCP Inspector web UI | 2.6.0 | HTTP through the Inspector's Node proxy | header set in the UI | Not exercised | The proxy makes the request from Node, so Developer mode (CORS) is only needed when a browser page calls the endpoint directly |
| Cursor | not installed on this PC | `mcp.json` url + headers | `headers.Authorization` | Not verified | Snippet from the settings page documented in the README |
| VS Code Copilot Chat / Cline | VS Code 1.137.0 without either extension | `mcp.json` / `cline_mcp_settings.json` | header | Not verified | Generic JSON snippet (`"type": "http"`) documented in the README |
| Gemini CLI | not installed (only a stale `~/.gemini/settings.json`) | `settings.json` `httpUrl` + headers | header | Not verified | Same URL and header |
| Claude Desktop | not installed (only a leftover `claude_desktop_config.json`) | stdio only | via the bridge program | Not verified | Requires the P5.3 bridge; remote connectors are P8 |

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
