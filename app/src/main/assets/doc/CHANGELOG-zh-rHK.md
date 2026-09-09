******

### 發行歷史

******

# v1.0.0

###### 2026/09/10

* `提示` 開發預覽版: 外掛已可在 AutoJs6 外掛中心註冊, 但 MCP 端點及其工具尚未可用
* `新增` 外掛識別碼 `mcp-server`, 含 INFO 服務, Wake Activity 以及供主程式發現的 `org.autojs.plugin.MCP_SERVER` 服務骨架
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌
* `新增` 位於 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端點及 `device_ping` 工具, 由可經 adb 或宿主啟停的前台服務承載 (開發預覽)
* `新增` `/mcp` 端點的傳輸加固: 綁定地址與端口來自伺服器設定儲存, 請求主體上限 1 MiB, 閒置連線 60 秒後關閉, 端口被佔用或綁定被拒絕時以 `port_in_use` / `bind_failed` 狀態附帶提示結束而非崩潰
* `新增` SDK 傳輸前置的 DNS rebinding 保護: 迴環模式只接受 `localhost` / `127.0.0.1` / `[::1]` 作為 `Host`, 區域網絡模式加入裝置目前的 IPv4 地址與可選的額外主機名稱並在網絡變化時刷新; 瀏覽器來源一律拒絕, 僅 "開發者模式" 開關經 CORS 放行 Inspector 的迴環來源
* `新增` 伺服器識別 `autojs6-mcp-server` 攜帶插件版本, 並宣告 tools (`listChanged`), resources 與 prompts 能力; `tools/list` 保持註冊順序以便客戶端緩存
* `新增` 每個 `/mcp` 請求的 Bearer 令牌鑑權: 首次啟動時生成 32 位元組令牌, 以 Android Keystore 的 AES-GCM 密鑰包裹後存放在插件私有且不參與備份的儲存中; `Authorization` 標頭缺失或錯誤時經常數時間比較後以 `401` + `WWW-Authenticate: Bearer` 與 JSON-RPC `-32001` 錯誤拒絕; 令牌不寫入日誌
* `新增` 傳輸前置的首次配對: 未配對客戶端可以 `initialize` 並列出 tools, resources 與 prompts, 但首次 `tools/call`, `resources/read`, `resources/subscribe` 或 `prompts/get` 返回 `PAIRING_REQUIRED` (`-32002`), 直到 60 秒內在手機上確認; 拒絕或逾時後 30 秒內返回 `PAIRING_DENIED` (`-32003`); 客戶端按 `clientInfo` 名稱 (缺失時用 `User-Agent`) 加地址類別 (迴環 / 區域網絡) 識別, 因此令牌輪換不影響既有配對, 最多可配對 32 個客戶端
* `新增` 手機上的配對確認走雙通道: 帶允許 / 拒絕動作的高優先級通知, 以及屏幕解鎖時彈出的對話框; 伺服器設定, 令牌與已配對客戶端保存在原子替換的檔案中, 伺服器進程與設定頁共享且不會讀到過期緩存
* `依賴` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 與 Ktor 3.5.1 CIO 引擎
* `依賴` 附加 Ktor 3.5.1 `ktor-server-test-host` 用於 JVM 傳輸測試 (僅測試範圍)
