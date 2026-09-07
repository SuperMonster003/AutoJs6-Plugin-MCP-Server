******

### 发行历史

******

# v1.0.0

###### 2026/09/07

* `提示` 开发预览版: 插件已可在 AutoJs6 插件中心注册, 但 MCP 端点及其工具尚不可用
* `新增` 插件标识 `mcp-server`, 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.MCP_SERVER` 服务骨架
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `新增` 位于 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端点及 `device_ping` 工具, 由可经 adb 或宿主启停的前台服务承载 (开发预览)
* `依赖` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 与 Ktor 3.5.1 CIO 引擎
