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

P3.4 development preview: 37 tools are available, with 33 enabled by default. File operations, editor positioning, application queries, clipboard, accessibility activation, and bounded Shell execution join the script, UI, and screenshot tools. The drawer switch and settings page remain planned in P4. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Planned Features

******

The roadmap delivers the following capabilities in stages:

- Script execution: run JavaScript from text or from a file inside AutoJs6, list and stop engines, and read recent console output.
- Accessibility UI: dump the node tree in a compact text format, find nodes with the AutoJs6 selector syntax, click, long-press, scroll, set text, and press global keys such as Back and Home.
- Screenshot group (P3.3): screen_capture returns MCP images with crop, scale or maxWidth, JPEG / PNG / WebP, and quality controls. Defaults are JPEG quality 70 and longest edge 1280 px. Images above 4 MiB of base64 are retried at lower quality or smaller dimensions, with metadata reporting adjustments. screen_state reports power, dimensions, orientation and density. The catalog now has 37 tools. MediaProjection fallback requires an AutoJs6 host built on 2026-09-13 or later and consent on the phone; the host session reuses that consent.
- Workspace tools (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open with one-based line and column, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast, and shell_exec. Binary reads use base64, up to 1 MiB of raw data. Writes also obey the host request budget (normally 96 KiB including JSON escaping). File deletion and Shell are off by default; root additionally requires allowShellRoot and a shell.root host grant. These additions require the matching P3.4 host build.
- Connection paths: USB through `adb forward`, local network with an explicit opt-in, a PC-side stdio bridge, and an optional public tunnel with OAuth 2.1.
- Security: a rotating bearer token, first-use pairing confirmation on the phone, and per-group tool switches; the server listens only on the loopback interface by default.

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) on a device with AutoJs6 build 5279 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `MCP Server` is recognized, and enable it. Official release packages pass signature verification automatically.
3. For this development preview, use the adb control plane and a host test session described in the developer notes; the drawer switch and plugin settings page are planned in P4.
4. On the PC, run `adb forward tcp:9637 tcp:9637` and point the MCP client at `http://127.0.0.1:9637/mcp` with the token as a bearer credential.

> For this development preview, use the adb control plane and a host test session described in the developer notes; the drawer switch and plugin settings page are planned in P4.

******

### Client Configuration

******

Claude Code registers the server with one command; other clients use the same URL and header in their MCP configuration:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

In this preview, read the token through the developer-mode adb control plane described in the developer notes. The token display in the settings page is planned in P4.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can bind to them.
- The INTERNET permission is used only for the plugin's own HTTP listener; the plugin makes no outbound requests and collects no data.
- Tool calls run through the AutoJs6 capability broker and never exceed what the host itself is allowed to do; dangerous groups such as shell commands and file deletion stay off until the user enables them.
- Backups are disabled, and tokens are stored only in the plugin's private storage.

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

#### v1.0.0

_2026/09/13_

- `Hint` P3.4 development preview: 37 tools are available, with 33 enabled by default. File operations, editor positioning, application queries, clipboard, accessibility activation, and bounded Shell execution join the script, UI, and screenshot tools. The drawer switch and settings page remain planned in P4. ROADMAP.md.
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
- `Improvement` Build verification rejects accidental native dependencies and produces a JSON report
- `Dependency` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) on the Ktor 3.5.1 CIO engine
- `Dependency` Ktor 3.5.1 `ktor-server-test-host` added for the JVM transport tests (test scope only)
- `Dependency` Added `mcp-server-api.aar` (AutoJs6 module `plugin-api/mcp-server-api`, host build 6.8.0 / 5279, MPL 2.0) as the Binder contract between AutoJs6 and the plugin, hash-locked in `locks/host-api-aars.lock`

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

Building requires JDK 21 or later and Android SDK 36; Gradle and plugin versions are managed centrally by `version.properties` and `io.github.supermonster003.autojs6-platform-versions`.

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
