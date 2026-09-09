******

### Release History

******

# v1.0.0

###### 2026/09/10

* `Hint` Development preview: the plugin registers with the AutoJs6 plugin center, but the MCP endpoint and its tools are not available yet
* `Feature` Plugin identity `mcp-server` with the INFO service, the Wake Activity, and the `org.autojs.plugin.MCP_SERVER` service skeleton for host discovery
* `Feature` README, plugin-center instructions, and changelog in 10 languages
* `Feature` Streamable HTTP endpoint at `http://127.0.0.1:9637/mcp` with the `device_ping` tool, hosted by a foreground service that adb or the host can switch on and off (development preview)
* `Feature` Transport hardening for the `/mcp` endpoint: the bind address and port come from the server config store, request bodies are capped at 1 MiB, idle connections close after 60 s, and a port that is already taken or a refused bind ends in a `port_in_use` / `bind_failed` status with a hint instead of a crash
* `Feature` DNS rebinding protection in front of the SDK transport: loopback mode accepts only `localhost` / `127.0.0.1` / `[::1]` as `Host`, LAN mode adds the device's current IPv4 addresses and optional extra host names and refreshes them when the network changes; browser origins are refused unless the developer-mode switch admits the Inspector's loopback origin through CORS
* `Feature` Server identity `autojs6-mcp-server` with the plugin version and the tools (`listChanged`), resources, and prompts capabilities; `tools/list` keeps the registration order so clients can cache it
* `Dependency` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) on the Ktor 3.5.1 CIO engine
* `Dependency` Ktor 3.5.1 `ktor-server-test-host` added for the JVM transport tests (test scope only)
