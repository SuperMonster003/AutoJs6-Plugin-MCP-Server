******

### 發行歷史

******

# v1.0.0

###### 2026/09/07

* `提示` 開發預覽版: 外掛已可在 AutoJs6 外掛中心註冊, 但 MCP 端點及其工具尚未可用
* `新增` 外掛識別碼 `mcp-server`, 含 INFO 服務, Wake Activity 以及供主程式探索的 `org.autojs.plugin.MCP_SERVER` 服務骨架
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌
* `新增` 位於 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端點及 `device_ping` 工具, 由可經 adb 或宿主啟停的前台服務承載 (開發預覽)
* `相依性` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 與 Ktor 3.5.1 CIO 引擎
