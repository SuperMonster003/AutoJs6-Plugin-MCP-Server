******

### Release History

******

# v1.0.0

###### 2026/09/07

* `Hint` Development preview: the plugin registers with the AutoJs6 plugin center, but the MCP endpoint and its tools are not available yet
* `Feature` Plugin identity `mcp-server` with the INFO service, the Wake Activity, and the `org.autojs.plugin.MCP_SERVER` service skeleton for host discovery
* `Feature` README, plugin-center instructions, and changelog in 10 languages
* `Feature` Streamable HTTP endpoint at `http://127.0.0.1:9637/mcp` with the `device_ping` tool, hosted by a foreground service that adb or the host can switch on and off (development preview)
* `Dependency` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) on the Ktor 3.5.1 CIO engine
