<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Exposes device automation to AI agents over the Model Context Protocol</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### Introduction

******

MCP Server turns an Android device running AutoJs6 into a [Model Context Protocol](https://modelcontextprotocol.io) server. AI agents on a PC, such as Claude Code, Cursor, or the MCP Inspector, connect to the phone over USB or Wi-Fi and use tools to run scripts, read logs, inspect the accessibility node tree, tap and type, take screenshots, and work with files and apps.

The server runs inside the plugin's own process and is reached through a single Streamable HTTP endpoint. AutoJs6 hands the plugin a capability broker over Binder, so every tool call is executed by the host with its existing permissions, engines, and accessibility service; the plugin never duplicates host functionality.

******

### Status

******

Version 1.0.1: 37 tools (33 enabled by default), MCP resources and prompts, an AutoJs6 drawer switch and a plugin settings page. Requires AutoJs6 6.8.0 (build 5279) or later; the optional autojs6://docs/ resources also need the AutoJs6 Offline Docs plugin and a host with its relay methods. Progress and evidence are tracked in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Features

******

The plugin provides the following capabilities:

- Phone settings for server status, USB forwarding, port and LAN access, token display/copy/rotation, pairing revocation, tool groups and root permission, developer mode, copyable Claude Code / Cursor / Codex / generic HTTP configurations, release history, and appearance following AutoJs6. Network settings apply by restarting an active listener; token and permission changes take effect immediately. Secret dialogs block screenshots.
- Script execution: run JavaScript from text or from a file inside AutoJs6, list and stop engines, and read recent console output.
- Accessibility UI: dump the node tree in a compact text format, find nodes with the AutoJs6 selector syntax, click, long-press, scroll, set text, and press global keys such as Back and Home.
- Screenshot group (P3.3): screen_capture returns MCP images with crop, scale or maxWidth, JPEG / PNG / WebP, and quality controls. Defaults are JPEG quality 70 and longest edge 1280 px. Images above 4 MiB of base64 are retried at lower quality or smaller dimensions, with metadata reporting adjustments. screen_state reports power, dimensions, orientation and density. The catalog now has 37 tools. MediaProjection fallback requires an AutoJs6 host built on 2026-09-13 or later and consent on the phone; the host session reuses that consent.
- Workspace tools (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open with one-based line and column, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast, and shell_exec. Binary reads use base64, up to 1 MiB of raw data. Writes also obey the host request budget (normally 96 KiB including JSON escaping). File deletion and Shell are off by default; root additionally requires allowShellRoot and a shell.root host grant. These additions require the matching P3.4 host build.
- MCP resources (P3.5) expose read-only workspace files, browsable host samples, the offline documentation when the AutoJs6 Offline Docs plugin is installed, device information, and recent console output, respecting pairing and group switches. Text and binary reads report truncation. The write_autojs6_script, automate_task, and debug_selector prompts provide English and Chinese guidance, with English fallback for other phone languages.
- Connection paths: USB through `adb forward`, local network with an explicit opt-in, and a PC-side stdio bridge for clients without an HTTP transport.
- Security: a rotating bearer token, first-use pairing confirmation on the phone, and per-group tool switches; the server listens only on the loopback interface by default.

******

### Tools

******

The table below is generated from the plugin's tool catalog snapshot (`app/src/test/resources/tool-catalog.snapshot.json`); descriptions are the English texts that clients receive, and each group can be switched off on the settings page:

| Tool | Group | Default | Description |
|---|---|---|---|
| `device_ping` | `device` | on | Confirms that the AutoJs6 MCP Server plugin is reachable and returns its version, the device model, the Android API level, and the device time. |
| `device_info` | `device` | on | Returns the device build, screen, battery, memory, AutoJs6 host version and process, accessibility service state, screen state, locale, and time zone as AutoJs6 reports them (schema autojs6-bridge-device-info-v1). No hardware identifiers. |
| `script_run` | `script` | on | Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API) and by default waits for it to finish. Use it for automation steps: toasts, UI actions, file work, app launches. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_run_file` | `script` | on | Runs a script file that already exists on the device (AutoJs6 picks the engine from the suffix) and by default waits for it to finish. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_stop` | `script` | on | Stops one running AutoJs6 script by the executionId that script_run, script_run_file, or script_list reported. |
| `script_stop_all` | `script` | on | Stops every script AutoJs6 is running, including ones started on the phone, and returns how many were stopped. |
| `script_list` | `script` | on | Lists the scripts AutoJs6 is running or starting, with executionId, name, path, working directory, state, and uptime. |
| `console_tail` | `script` | on | Returns the newest lines of the AutoJs6 console, which every script shares; optionally only entries after sinceId or at least a level. nextSinceId in the result continues from where this call ended. |
| `ui_dump` | `ui` | on | Dumps the accessibility node tree of the active window as compact text: one node per line with a #n reference, an indent per depth, the short class name, the state markers that apply (clickable, long_clickable, checkable, checked, scrollable, editable, focused, selected, !enabled, hidden), the text in quotes, desc=, id= (name part only), and the position (bounds [l,t][r,b] for a node with children, c=(x,y) for a leaf). Pass a #n reference as nodeRef to ui_click, ui_long_click, ui_set_text, or ui_scroll; references stay valid until the next ui_dump or for 60 s. Call it before acting and again after the screen changed. format json returns the nodes as objects with every flag; format xml returns the uiautomator-style export. |
| `ui_find` | `ui` | on | Finds the nodes of the active window that match every condition of the selector, optionally waiting up to timeoutMs for the first match, and returns up to limit of them with #n references, bounds, and center. An empty count is not an error; ui_explain_selector tells which condition fails. |
| `ui_current_window` | `ui` | on | Returns the package and activity in the foreground, whether the AutoJs6 accessibility service is available, and the accessibility windows with their type, title, bounds, and focus. |
| `ui_explain_selector` | `ui` | on | Explains why a selector matches or not: evaluates its conditions one by one over the active window and reports how many nodes pass each step cumulatively, the first failing condition, the matches, and the near misses. Use it when ui_find returns nothing. |
| `ui_wait_for` | `ui` | on | Waits until a node matching the selector appears (default) or disappears, polling the active window every 0.5 s for up to timeoutMs, and answers TIMEOUT when the state is not reached. Use it after an action that opens a screen or dismisses a dialog. |
| `ui_click` | `ui` | on | Clicks a node given by nodeRef (a #n reference from the last ui_dump), by selector (the first match in pre-order), or by x and y (a coordinate tap, allowed only while the ui_gesture group is enabled). The accessibility click climbs to the nearest clickable ancestor when the node itself is not clickable. Returns the node it acted on. Give nodeRef or selector, not both. |
| `ui_long_click` | `ui` | on | Long-presses a node given by nodeRef or selector (the accessibility long click climbs to the nearest node that accepts it), or by x and y as a 700 ms press at that point (allowed only while the ui_gesture group is enabled). Give nodeRef or selector, not both. |
| `ui_set_text` | `ui` | on | Sets the text of an editable node (an EditText, marked editable by ui_dump) given by nodeRef or selector; append adds to the current text instead of replacing it. Works without focus or the keyboard; ACTION_FAILED means the node is not editable or not enabled. Give nodeRef or selector, not both. |
| `ui_scroll` | `ui` | on | Scrolls a node given by nodeRef or selector, or the first scrollable node of the window when neither is given: forward, down, and right move towards the end, backward, up, and left towards the start; times repeats the step. performed counts the steps the node accepted, fewer than requested means it reached the end. Give nodeRef or selector, not both. |
| `ui_press_key` | `ui` | on | Presses a global key through the accessibility service: back, home, recents, notifications (opens the notification shade), quick_settings, power_dialog, or lock_screen (Android 9 or later). |
| `ui_swipe` | `ui_gesture` | off | Swipes one finger from (x1, y1) to (x2, y2) in device pixels over durationMs; take the coordinates from ui_dump bounds or a screenshot. Part of the ui_gesture group, which is off by default. |
| `ui_gesture` | `ui_gesture` | off | Performs a free-path one-finger gesture through the given points over durationMs (at most 10 s): the first point is the touch down, the last the lift. Part of the ui_gesture group, which is off by default. |
| `screen_capture` | `screen` | on | Capture the phone screen as an MCP image with dimensions, size, duration and capture source. Uses accessibility on Android 11+ and falls back to MediaProjection, which requires consent on the phone the first time. Defaults to JPEG quality 70 and a longest edge of 1280 pixels. Choose scale or maxWidth to override the size. Images above the 4 MiB base64 limit are retried at lower quality or smaller dimensions; metadata reports adjustments. At most 30 captures per minute per client; a RATE_LIMITED result names the wait in retryAfterMs. |
| `screen_state` | `screen` | on | Read whether the screen is on, its current width and height, orientation, rotation, and density. Does not request screen capture consent. |
| `files_list` | `files` | on | Lists workspace files with metadata. Results are bounded and report truncation. |
| `files_stat` | `files` | on | Returns existence, type, size, and modification time of a workspace path. |
| `files_read` | `files` | on | Reads up to 1 MiB. Use encoding base64 for binary data; encoding, bytes, totalBytes, and truncated identify the representation and limit. |
| `files_write` | `files` | on | Writes UTF-8 text and refreshes the host explorer. Content is limited to 1 MiB and the negotiated Binder request budget (normally 96 KiB including JSON escaping); oversized calls fail before writing. |
| `files_mkdir` | `files` | on | Creates a workspace directory and missing parents, then refreshes the host explorer. |
| `files_rename` | `files` | on | Moves a workspace file or directory to another workspace path and refreshes the host explorer. |
| `files_delete` | `files_delete` | off | Deletes a workspace entry. The separate files_delete group is off by default. The workspace root cannot be deleted. |
| `editor_open` | `files` | on | Opens a workspace file in the AutoJs6 editor at a one-based line and column. Lines outside the file are ignored by the editor. |
| `app_launch` | `device` | on | Opens an installed Android application. Provide exactly one of packageName or appName. |
| `app_list` | `device` | on | Lists up to 1000 Android applications visible to AutoJs6, optionally matching a package name or label. Android package visibility restrictions apply. |
| `clipboard_get` | `device` | on | Reads clipboard text (up to 64 KiB). Android may restrict clipboard access while AutoJs6 is in the background. |
| `clipboard_set` | `device` | on | Replaces clipboard text, including an empty string to clear it. |
| `device_ensure_accessibility` | `device` | on | Asks AutoJs6 to enable its accessibility service using its configured secure-settings, root, or Shizuku strategy. Waits up to 10 s for an operational service; failure includes manual activation guidance. |
| `toast` | `device` | on | Shows a short Android toast on the phone. |
| `shell_exec` | `shell` | off | Runs an Android shell command in the host workspace. The shell group is off by default; root also requires the separate allow root switch and a shell.root host grant. Reports exit code, timeout, stdout, stderr, and truncation. maxOutputBytes bounds stdout and stderr together. |

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) on a device with AutoJs6 build 5279 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `MCP Server` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Turn on MCP Server in the AutoJs6 drawer. Long-press its title to open settings, or use Settings in its Plugin Center entry. Copy the configuration for your PC client.
4. On the PC, run `adb forward tcp:9637 tcp:9637` and point the MCP client at `http://127.0.0.1:9637/mcp` with the token as a bearer credential.
5. Confirm the first pairing request on the phone. Stop the server from the drawer, settings, or the notification when finished.

> MCP Server drawer switch with installation, activation, enablement, authorization, and compatibility guidance; notification Stop synchronization; saved plugin settings retained on reconnect; permission-checked settings entry from the drawer and Plugin Center. Restores a previously enabled server when AutoJs6 opens, unless the user stopped it while the host was absent; no boot startup.

<p align="center">
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/drawer-en.png?raw=true" alt="AutoJs6 drawer with the MCP Server switch" width="300" />
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/settings-en.png?raw=true" alt="MCP Server settings page" width="300" />
</p>

******

### Client Configuration

******

Claude Code registers the server with one command; other clients use the same URL and header in their MCP configuration:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

Turn on MCP Server in the AutoJs6 drawer. Long-press its title to open settings, or use Settings in its Plugin Center entry. Copy the configuration for your PC client. Confirm the first pairing request on the phone. Stop the server from the drawer, settings, or the notification when finished.

******

### Connection Paths

******

USB: `adb forward tcp:9637 tcp:9637` maps the phone port to the PC; with several devices add `-s <serial>` (find it with `adb devices`), and emulators work the same way. If the port is taken on either side, change it on the settings page and forward the new one. The Connection card offers the exact forward command for copying.

Local network: turn on Allow local network connections on the settings page. The page then lists the current addresses of the phone (they follow Wi-Fi changes) and reminds you that the client must join the same network; guest networks, AP isolation and the PC firewall are the usual blockers. Pairing requests from the local network are marked as such, and a daily notification reminds you while the server stays reachable from the network; the reminder can be turned off.

Both paths keep the same token and the same phone-side pairing. Clients without an HTTP transport use the stdio bridge described under Clients.

******

### Clients

******

The settings page copies a ready configuration with the real token for each client below; the snippets here use `<token>` as a placeholder. Every client speaks Streamable HTTP with an Authorization header, and the first call of a new client is confirmed on the phone. Verified: Claude Code, Codex CLI and MCP Inspector; the other clients use the same URL and header but have not been tested by the maintainer yet.

Claude Code: run the command shown under Client Configuration (the settings page copies it with the token); `claude mcp list` then reports `autojs6` as Connected.

Cursor: add the entry below to `mcp.json`:

```json
{
  "mcpServers": {
    "autojs6": {
      "url": "http://127.0.0.1:9637/mcp",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

Codex CLI: put the token into the `AUTOJS6_MCP_TOKEN` environment variable (the settings page copies a PowerShell command for it) and add the server to `config.toml`, or run `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN`:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: the CLI mode needs no extra setup, and the web UI reaches the phone through its own Node proxy. Turn on Developer mode on the settings page only when a browser page connects to the endpoint directly:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline, VS Code Copilot Chat, Gemini CLI and similar clients: use the same URL and header in their MCP configuration; the settings page offers a generic JSON snippet with `"type": "http"`.

Claude Desktop and other stdio-only clients: install the bridge with `npm install -g autojs6-mcp-bridge` and register `autojs6-mcp-bridge --serial <serial>` as a stdio server with `AUTOJS6_MCP_TOKEN` in its environment block (see the [bridge README](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge) for the Claude Desktop and Claude Code snippets). Bridge 0.1.0 pairs with plugin 1.0.0 and passes the client's protocol version through; it was verified with Claude Code 2.1.257 over stdio.

******

### FAQ

******

- 401 Unauthorized: the token is missing, mistyped or rotated. Copy the configuration again from the settings page; after Rotate token every client needs the new value.
- Pairing timeout: the first call of a new client waits about a minute for Allow on the phone. Unlock the phone, accept the dialog or the notification action, then repeat the call. Deny starts a short cooldown, after which the next call asks again.
- HOST_UNAVAILABLE: AutoJs6 is not running or its plugin session is closed. Open AutoJs6, keep the drawer switch on, and check the connection state on the settings page.
- Accessibility off: the `ui_*` tools and screen capture need the AutoJs6 accessibility service. Call `device_ensure_accessibility` or enable the service in the system accessibility settings.
- Port in use: the drawer reports `port_in_use`. Change the port on the settings page and forward the new port with adb.
- Local network unreachable: turn on local network access, use an address listed on the settings page, keep the PC and the phone on the same network without guest isolation, and allow the port through the PC firewall. Wi-Fi power saving on the phone adds a few hundred milliseconds per call.
- Server gone after the screen turned off: phones that restrict the app's battery usage (HyperOS and MIUI do so for sideloaded apps by default) stop the foreground service about a minute after the screen turns off on battery. The settings page then shows a warning with a Battery settings button; choose Unrestricted for MCP Server there. The optional Stop automatically when idle setting (off by default) also stops the server after the chosen minutes without requests and leaves a notification saying so.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder entry points and the settings page are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the pairing dialog, its receiver and the release history page are not exported. Only the foreground service that hosts the listener accepts adb (`android.permission.DUMP`), which is the developer's start / stop switch.
- The INTERNET permission serves only the plugin's own HTTP listener; the plugin makes no outbound requests and collects no data. Cleartext HTTP is permitted only towards loopback addresses through the network security configuration.
- The server listens on 127.0.0.1 by default. Local network access stays off until you turn it on; the token, the pairing confirmation, the Host allow list and the rate limits still apply on the local network, and a daily notification reminds you while it is on.
- The access token comes from a secure random source, is wrapped with an AES-GCM key from the Android Keystore and lives in the plugin's private, never-backed-up storage; backups and device transfers are disabled. The settings page shows only its last 4 characters, the full-token dialogs block screenshots, and copies are marked sensitive for the clipboard.
- Logs never contain the token, request bodies, file contents or screenshots; the plugin logs tool names, client names and token fingerprints only. This was verified with logcat on two devices during real file and screenshot calls (docs/dev/p6-security-audit.md).
- Tool calls run through the AutoJs6 capability broker and never exceed what the host itself is allowed to do; shell commands, file deletion and gestures stay off until you enable their groups, and a root shell additionally needs its own switch and a host grant.
- Pairings can be revoked one by one or all at once on the settings page; a revoked client must be confirmed on the phone again before its next tool call. Rotating the token keeps the pairings but cuts off every client that still uses the old token.

Only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

******

### Plugin Interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate compatibility:

```text
application id: io.github.supermonster003.autojs6.plugin.mcp.server
plugin id: mcp-server
engine: mcp-server
variant: default
service action: org.autojs.plugin.MCP_SERVER
service category: mcp-server
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.mcp.server.api.IMcpServerPlugin
minimum host build: 5279 (6.8.0)
default endpoint: http://127.0.0.1:9637/mcp
```

`McpServerPluginService` implements the host mcp-server-api contract `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` in the `:mcp_server` process and answers `org.autojs.plugin.MCP_SERVER` (category `mcp-server`). `McpServerPluginInfoService` answers `org.autojs.plugin.INFO` with PluginInfo. `WakeActivity` lets the host activate the plugin.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.1

_2026/09/16_

- `Feature` Optional offline documentation resources: when the AutoJs6 Offline Docs plugin is installed and the host relays it through app.listDocs / app.readDoc, resources/list adds autojs6://docs/ (an index with child URIs) and one autojs6://docs/{+path} resource per documentation page, and resources/templates/list adds the docs template; without the plugin, or on a host without these methods, nothing is listed and _meta.docsCatalogStatus tells why
- `Improvement` Raise compileSdk to 37 (Android 17); targetSdk stays at 36 until the behavior that depends on the target is verified
- `Improvement` MCP conformance (P6): the official @modelcontextprotocol/conformance suite 0.1.16 was run against the stateful /mcp path on two devices. 9 of its 32 server scenarios pass (initialize, ping, tools/list, text and error tool results, resources/list, prompts/list, concurrent SSE streams, DNS rebinding protection); 18 call the suite's own reference fixtures (test_* tools, prompts and test:// resources, which this server answers with an unknown-tool result, -32602 or isError) and 5 need capabilities the server does not declare (logging, completions, resource subscriptions). A loopback Origin header is now accepted in every mode, as the suite expects; CORS headers and preflight answers stay limited to developer mode. The stateless 2026-07-28 model has no route (Roadmap D9). Details in docs/dev/p6-conformance.md.
- `Improvement` Security audit (P6): the seven checklist items (token storage, log redaction, exported components, cleartext scope, local network off by default, pairing revocation, default-off tool groups) are verified in the code and on two devices in docs/dev/p6-security-audit.md, and the README security section now states these boundaries. Cleartext HTTP is limited to loopback addresses by a network security configuration instead of the app-wide usesCleartextTraffic flag; the plugin opens no client connections and the listener does not need the flag.
- `Improvement` Performance baseline (P6): ui_dump at 50 / 200 / 400 nodes, screen_capture at three sizes, script_run round trips and four concurrent requests were timed on an API 24 emulator, a Sony phone (API 33) and a Xiaomi Pad (API 35) and recorded in docs/dev/p6-performance-baseline.md as a reference without thresholds. A plugin-only call answers in about 20 ms on the emulator and the phone, ui_dump grows by about 0.05 ms per node, the accessibility screenshot path answers within 100 ms while the MediaProjection path on API 24 takes about 1.35 s per capture, and four concurrent requests complete in 1.0-1.8 times one round trip within the host's limit of four concurrent calls.

#### v1.0.0

_2026/09/16_

- `Hint` P4 development preview: 37 tools, 33 enabled by default, with an AutoJs6 drawer switch and a plugin settings page. Requires the matching P4 AutoJs6 build. ROADMAP.md.
- `Feature` Phone settings for server status, USB forwarding, port and LAN access, token display/copy/rotation, pairing revocation, tool groups and root permission, developer mode, copyable Claude Code / Cursor / Codex / generic HTTP configurations, release history, and appearance following AutoJs6. Network settings apply by restarting an active listener; token and permission changes take effect immediately. Secret dialogs block screenshots.
- `Feature` MCP resources (P3.5) expose read-only workspace files, browsable host samples, device information, and recent console output, respecting pairing and group switches. Text and binary reads report truncation. The write_autojs6_script, automate_task, and debug_selector prompts provide English and Chinese guidance, with English fallback for other phone languages.
- `Feature` Workspace tools (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open with one-based line and column, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast, and shell_exec. Binary reads use base64, up to 1 MiB of raw data. Writes also obey the host request budget (normally 96 KiB including JSON escaping). File deletion and Shell are off by default; root additionally requires allowShellRoot and a shell.root host grant. These additions require the matching P3.4 host build.
- `Feature` Plugin identity `mcp-server` with the INFO service, the Wake Activity, and the `org.autojs.plugin.MCP_SERVER` service skeleton for host discovery
- `Feature` README, plugin-center instructions, and changelog in 10 languages
- `Feature` Streamable HTTP endpoint at `http://127.0.0.1:9637/mcp` with the `device_ping` tool, hosted by a foreground service that adb or the host can switch on and off (development preview)
- `Feature` Transport hardening for the `/mcp` endpoint: the bind address and port come from the server config store, request bodies are capped at 1 MiB, idle connections close after 60 s, and a port that is already taken or a refused bind ends in a `port_in_use` / `bind_failed` status with a hint instead of a crash
- `Feature` DNS rebinding protection in front of the SDK transport: loopback mode accepts only `localhost` / `127.0.0.1` / `[::1]` as `Host`, LAN mode adds the device's current IPv4 addresses and optional extra host names and refreshes them when the network changes; browser origins are refused unless the developer-mode switch admits the Inspector's loopback origin through CORS
- `Feature` Server identity `autojs6-mcp-server` with the plugin version and the tools (`listChanged`), resources, and prompts capabilities; `tools/list` keeps the registration order so clients can cache it
- `Feature` Bearer token authentication for every `/mcp` request: a 32-byte token generated on first start, wrapped with an Android Keystore AES-GCM key and kept in the plugin's private, never-backed-up storage; a missing or wrong `Authorization` header is refused after a constant-time comparison with `401` + `WWW-Authenticate: Bearer` and a JSON-RPC `-32001` error; the token never reaches the log
- `Feature` First-use pairing in front of the transport: an unpaired client may `initialize` and list tools, resources, and prompts, but its first `tools/call`, `resources/read`, `resources/subscribe`, or `prompts/get` answers `PAIRING_REQUIRED` (`-32002`) until the request is confirmed on the phone within 60 s; a denial or a timeout answers `PAIRING_DENIED` (`-32003`) for 30 s; a client is identified by its `clientInfo` name (or `User-Agent`) plus the address class (loopback / LAN), so a token rotation keeps existing pairings, and up to 32 clients can be paired
- `Feature` Pairing confirmation on the phone through two channels: a high-priority notification with Allow / Deny actions, plus a dialog while the screen is unlocked; the server configuration, the token, and the paired clients live in atomically replaced files that the server process and the settings page share without stale caches
- `Feature` Tool catalog with group switches (decision D6): `device_ping` (plugin-local), `device_info` (AutoJs6 `device.info`), and `script_run` (AutoJs6 `engines.execScript`: runs JavaScript, waits up to `timeoutMs` for it to finish, and returns the outcome plus the newest console lines, with progress notifications while it runs); every tool declares a closed JSON Schema (`additionalProperties: false`) and its arguments are validated before anything reaches AutoJs6; the group switches `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` are stored in `tool_groups.json`, a switched-off group disappears from `tools/list` on the next request and its tools answer `TOOL_DISABLED`
- `Feature` Host bridge: the `org.autojs.plugin.MCP_SERVER` service implements the real `IMcpServerPlugin` Binder (`getInfo` / `getCapabilities` report contract version 1, the tool groups, the MCP protocol versions, and the SDK version; `openServer` admits only the installed same-signer AutoJs6 and returns an `IMcpServerSession` with `getStatus` / `updateConfig` / `stop` / `close`); tool calls travel through the host capability broker with monotonic request ids, per-call timeouts, the 4-call concurrency ceiling, and host error categories mapped onto `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; when AutoJs6 dies the listener keeps running and host-backed tools answer `HOST_UNAVAILABLE` until the host reconnects; status and events (`pairing_requested`, `client_paired`, `tool_call`, `warning`) reach the host through its callback
- `Feature` Foreground service notification with the endpoint, the AutoJs6 connection state, and the paired client count plus a Stop action; a toast names the endpoint when notifications are blocked; `dumpsys activity service` additionally prints the host session, the tool group switches, and the registered tools
- `Feature` Script group completed: `script_run_file` runs a script file on the device, `script_stop` / `script_stop_all` stop one or every AutoJs6 execution, `script_list` lists the running ones, and `console_tail` returns the newest console lines with a `nextSinceId` cursor and a level filter; `script_run` and `script_run_file` now report `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, the exception with its line, and the newest console lines, and while they wait a progress notification every 2 s carries the newest console line
- `Feature` Responses of the MCP endpoint stream as server-sent events (the SDK's JSON response mode is not used), so a notification that belongs to a request, such as the progress heartbeat of a running script, reaches the client on the response of that request
- `Feature` UI group added (roadmap P3.2): `ui_dump` returns the active window as a compact node tree with `#n` references (`format` text / json / xml, `maxNodes` up to 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` poll a selector, `ui_current_window` and `ui_explain_selector` report the window and why a selector fails, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` act on a `nodeRef` (relocated by its fingerprint, `NODE_REF_STALE` once it is gone) or a `selector`, `ui_press_key` presses back / home / recents / notifications / quick_settings / power_dialog / lock_screen, and the `ui_gesture` group (off by default) adds `ui_swipe`, `ui_gesture`, and the coordinate form of the click tools (`TOOL_DISABLED` while the group is off); the tool catalog snapshot grows to 20 tools; the coordinate gestures need an AutoJs6 host built on 2026-09-11 or later (an older host answers them at random with "the system cancelled ...")
- `Feature` Screenshot group (P3.3): screen_capture returns MCP images with crop, scale or maxWidth, JPEG / PNG / WebP, and quality controls. Defaults are JPEG quality 70 and longest edge 1280 px. Images above 4 MiB of base64 are retried at lower quality or smaller dimensions, with metadata reporting adjustments. screen_state reports power, dimensions, orientation and density. The catalog now has 22 tools. MediaProjection fallback requires an AutoJs6 host built on 2026-09-13 or later and consent on the phone; the host session reuses that consent.
- `Feature` Local network path (P5.1): with local network access on, the settings page lists the current addresses of the phone (refreshed when Wi-Fi changes) with same-network and firewall hints; a pairing request from the local network is marked in the dialog and the notification; a daily reminder says the server is still reachable from the local network and can be turned off without restarting the listener. README documents the USB and local network paths.
- `Feature` Per-client rate limits (P6): at most 20 requests per second and 30 screen_capture calls per minute per client. A request over the limit gets HTTP 429 with a Retry-After header and a JSON-RPC RATE_LIMITED error carrying retryAfterMs; a screenshot over the limit is answered as a RATE_LIMITED tool result with retryAfterMs so the model can wait. The host grant keeps its own accessibility query rate on top.
- `Feature` Idle auto-stop and battery (P6): the settings page offers "Stop automatically when idle" (off by default; 5 / 15 / 30 / 60 / 120 minutes). The listener stops itself after the chosen time without client requests, records the plugin-only idle_timeout reason, leaves one auto-cancelling notification and does not count as a user stop; a request in flight (a running tool call) is never idle, while a client that keeps its stream open without sending requests does not keep the server running. When Android restricts the app's battery usage (HyperOS and MIUI do so for sideloaded apps by default and stop the foreground service about a minute after the screen turns off on battery), the settings page shows a warning with a Battery settings button and AutoJs6 receives a warning event. Idle CPU time and the batterystats estimate of one idle hour are recorded in docs/dev/p6-battery-and-residency.md.
- `Fix` IDE rebuild no longer looks for an APK for JVM unit tests. APK verification tasks automatically assemble their inputs and work from a clean build.
- `Fix` Plugin Center icon proportions differed between light and dark modes; night mode now also uses the adaptive icon, with layer sizing adjusted to preserve the complete ic_launcher_round.png artwork and margins while changing only the background color
- `Fix` Keyboard Tab navigation on the settings page skipped the toolbar back button; the Tab cycle now covers the back button and every control, including on Android 7. Device tests check screen reader labels and keyboard operation.
- `Fix` The arguments map of script_run and script_run_file declared its value type as a JSON Schema array, which some MCP clients reject or weaken; the schema now uses single-type anyOf branches. README gains Clients and FAQ sections with the tested client matrix.
- `Fix` Hostile request bodies are refused before any parser recurses into them: JSON nested deeper than 64 levels, a body that repeats a request id, and a request id still in flight on the same session (which would have left the earlier request without an answer) get a 400 with a JSON-RPC error; a GET stream without a live session gets 400 / 404 instead of an empty event stream. JVM tests and device tests (API 28 / 31 / 33 / 35) cover oversized, over-deep, invalid UTF-8, unknown method, header combination, large base64 and 64 concurrent session cases.
- `Fix` Lifecycle matrix (P6): a request that names the session of a previous listener process (after the listener was killed, or restarted by a token rotation) is left to the transport's 404 so the client initializes again, instead of raising a pairing prompt under the request's User-Agent for a client that is already paired; pairing notifications a dead listener process left in the shade are cleared when the next listener starts. Host kill, listener kill, both at once, force-stop from the system settings, token rotation under a live session and a pending pairing across a host kill and a listener kill are recorded with their expected states and recovery paths in docs/dev/lifecycle-matrix.md and verified on devices.
- `Improvement` Build verification rejects accidental native dependencies and produces a JSON report
- `Dependency` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) on the Ktor 3.5.1 CIO engine
- `Dependency` Ktor 3.5.1 `ktor-server-test-host` added for the JVM transport tests (test scope only)
- `Dependency` Added `mcp-server-api.aar` (AutoJs6 module `plugin-api/mcp-server-api`, host build 6.8.0 / 5279, MPL 2.0) as the Binder contract between AutoJs6 and the plugin, hash-locked in `locks/host-api-aars.lock`
- `Dependency` Update the paired common-plugin-api and mcp-server-api AARs from the P4 host build: optional settings extension v1, unchanged AIDL transaction order, SHA-256 locks, and SDK 36 consumer compatibility.

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build and Verification

******

This section targets developers who want to build the plugin from source; regular users can simply install the prebuilt APK from the Releases page.

Build a debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run JVM unit tests and build the instrumentation test APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Build the release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collect the release artifact and append the version and CRC32 digest to its file name:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verify that the multilingual documentation sources and generated artifacts are in sync (also enforced by CI):

```powershell
py .python\generate_markdown.py --check
```

Building requires JDK 21 or later and Android SDK 37; Gradle and plugin versions are managed centrally by `version.properties` and `io.github.supermonster003.autojs6-platform-versions`.

******

### Localization and Docs Generation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

The language JSON files under `.readme/` and `.changelog/` are the single source for the README, the plugin-center instructions, and the changelog. Always edit those JSON sources and rerun `py .python/generate_markdown.py`; generated README, `plugin_instruction.md`, and changelog artifacts are never edited by hand. Run `py .python/generate_markdown.py --check` to verify all generated artifacts.

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE). Third-party components and their licenses are listed in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- Model Context Protocol specification: https://modelcontextprotocol.io
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
