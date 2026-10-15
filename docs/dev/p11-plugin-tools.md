# P11 plugin capability tools: developer notes

Roadmap P11 turns the capabilities of other AutoJs6 plugins (OCR, barcode, OpenCV image search,
Pinyin / Pinyin4j, MediaInfo) into MCP tools. The host side (AutoJs6 6.8.0, decisions D23 - D28
and D45) landed first: OCR engine auto-selection, `accessibility.captureScreenImage`, downsampled
`image.readImage`, the model-session path policy with `files.shared_read`, `app.pluginCapabilities`,
and the wider MCP default grant. This page records the plugin side as it lands.

## Soft probing and the dynamic catalog (D27 / D28)

The plugin never binds a plugin process itself; it asks AutoJs6. Two layers decide whether a
plugin-family tool is listed:

1. **The grant is the ceiling.** `getBrokerInfo()` of the open session lists the methods the host
   allows (`BrokerInfo.methods`, read once when the session opens). A tool row declares every host
   method it may call in `ToolSpec.hostMethods` (default: its `bridge` method; flows list all of
   them). A plugin-family row is listed only while the grant covers all of them. An old host whose
   grant lacks `app.pluginCapabilities` lists none of them, so the 37 built-in tools are unaffected.
2. **The host decides availability.** `app.pluginCapabilities` (schema
   `autojs6-bridge-app-plugin-capabilities-v1`) reports every family with `status` =
   `available` / `not_installed` / `disabled` / `incompatible`, its methods, the plugins behind it
   and a `detail`. `PluginFamilyProbe` calls it with a 5 s timeout and keeps the answer for 5 s
   (`PluginFamilyProbe.TTL_MS`); the cache is dropped on package changes, when a session opens or
   closes, and when the host dies. A host that stops answering (`HOST_UNAVAILABLE`, `TIMEOUT`) keeps
   the last report, marked `stale`, so a short host restart does not make the tools flap; any other
   failure, an unknown answer shape, or a missing method hides the rows and records why.

`ToolRegistry.refresh()` reads both layers together with the group switches, re-registers the
rows in catalog order, and sends `notifications/tools/list_changed` when the registration changed.
The tool gate runs it before every `tools/list` and `tools/call`; the runtime also runs it after a
session opens or closes, after the host dies, and after a package broadcast, so clients learn about
a newly installed plugin without asking. `install()` starts with `PluginToolAvailability.NONE`:
plugin-family rows join on the first refresh.

### Package changes

`PackageChangeWatcher` registers a runtime receiver for `ACTION_PACKAGE_ADDED` / `REMOVED` /
`CHANGED` / `REPLACED` (data scheme `package`) while the listener runs, debounced 500 ms because
one install fires several of them. Each change invalidates the probe and refreshes the registry.
Enabling or disabling a plugin inside the AutoJs6 plugin center sends no broadcast; the 5 s cache
and the refresh before every request cover it.

### Groups and the settings page

`ToolGroup` gained six groups with a `family`: `ocr`, `barcode`, `opencv`, `pinyin`, `pinyin4j`,
`mediainfo`, all on by default (read-only tools, the same risk class as `screen`). The settings
page lists them after the built-in groups under a hint that they need an AutoJs6 plugin; the group
switch wins over the host state (`TOOL_DISABLED` first). `ToolCatalog.groups` and the
`mcpServerToolGroups` capability list a group only once it has rows, so the capability keeps its
eight entries until the tool items land.

### Errors while a row is hidden

A `tools/call` for a hidden plugin-family tool goes through the tool gate like a switched-off tool
and answers an `isError` result instead of the SDK's "tool not found":

| Code | When |
|---|---|
| `TOOL_DISABLED` | the group switch is off |
| `HOST_UNAVAILABLE` | no host session is attached |
| `CAPABILITY_DENIED` | the session grant lacks one of the tool's `hostMethods` (`module` / `method` name the first) |
| `PLUGIN_UNAVAILABLE` | the host reports the family as `not_installed`, `disabled` or `incompatible`, does not know it, or cannot report plugin capabilities at all; the message carries the host's `detail`, the hint points at the AutoJs6 plugin center and `device_info` |

`dumpsys activity service` prints `pluginFamilies: session=open stale=false families={ocr=available,...} probes=N`.

### Tests

- JVM: `PluginFamiliesTest` (report parsing, the availability rule, the probe's cache / invalidation /
  session change / stale handling / failure handling), `ToolRegistryTest` (plugin-family rows appear
  and vanish with the grant and the report, the gate reasons), `ToolCatalogTest` and
  `ToolPermissionsTest` (the new groups).
- Instrumentation and devices: recorded with the tool items and the P11 test item in `ROADMAP.md`.
