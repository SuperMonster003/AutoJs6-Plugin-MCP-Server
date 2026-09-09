******

### 發行歷史

******

# v1.0.0

###### 2026/09/10

* `提示` 開發預覽版: 外掛已可在 AutoJs6 外掛中心註冊, 但 MCP 端點及其工具尚未可用
* `新增` 外掛識別碼 `mcp-server`, 含 INFO 服務, Wake Activity 以及供主程式探索的 `org.autojs.plugin.MCP_SERVER` 服務骨架
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌
* `新增` 位於 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端點及 `device_ping` 工具, 由可經 adb 或宿主啟停的前台服務承載 (開發預覽)
* `新增` `/mcp` 端點的傳輸加固: 綁定位址與連接埠來自伺服器設定存放區, 請求本體上限 1 MiB, 閒置連線 60 秒後關閉, 連接埠被佔用或綁定被拒絕時以 `port_in_use` / `bind_failed` 狀態附帶提示結束而非當機
* `新增` SDK 傳輸前置的 DNS rebinding 保護: 迴環模式只接受 `localhost` / `127.0.0.1` / `[::1]` 作為 `Host`, 區域網路模式加入裝置目前的 IPv4 位址與可選的額外主機名稱並在網路變化時重新整理; 瀏覽器來源一律拒絕, 僅 "開發者模式" 開關經 CORS 放行 Inspector 的迴環來源
* `新增` 伺服器識別 `autojs6-mcp-server` 攜帶外掛程式版本, 並宣告 tools (`listChanged`), resources 與 prompts 能力; `tools/list` 保持註冊順序以便用戶端快取
* `相依性` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 與 Ktor 3.5.1 CIO 引擎
* `相依性` 附加 Ktor 3.5.1 `ktor-server-test-host` 用於 JVM 傳輸測試 (僅測試範圍)
