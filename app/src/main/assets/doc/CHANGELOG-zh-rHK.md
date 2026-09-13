******

### 發行歷史

******

# v1.0.0

###### 2026/09/13

* `提示` P3.5 開發預覽: 工具目錄共 37 項, 預設啟用 33 項. 檔案操作, 編輯器定位, 應用查詢, 剪貼簿, 無障礙自動啟用和限制輸出的 Shell 已接入, 與指令碼, UI 和截圖工具配合使用. 抽屜開關和設定頁仍計劃在 P4 實現. ROADMAP.md.
* `新增` MCP 資源 (P3.5) 提供唯讀工作目錄檔案, 可瀏覽的宿主範例, 裝置資訊和最近主控台輸出, 遵守配對與分組開關. 文字和二進制讀取報告截斷狀態. write_autojs6_script, automate_task 和 debug_selector 提示提供中英文指引, 其他手機語言回退英語.
* `新增` 工作目錄工具 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 使用從 1 開始的行列號的 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast 和 shell_exec. 二進制讀取使用 base64, 原始資料最多 1 MiB. 寫入亦受宿主請求預算約束 (通常為包含 JSON 轉義的 96 KiB). 檔案刪除和 Shell 預設關閉; root 另需 allowShellRoot 開關與宿主 shell.root 授權. 這些能力需要匹配的 P3.4 宿主構建.
* `新增` 外掛識別碼 `mcp-server`, 含 INFO 服務, Wake Activity 以及供主程式發現的 `org.autojs.plugin.MCP_SERVER` 服務骨架
* `新增` 10 種語言的 README, 外掛中心說明與更新日誌
* `新增` 位於 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端點及 `device_ping` 工具, 由可經 adb 或宿主啟停的前台服務承載 (開發預覽)
* `新增` `/mcp` 端點的傳輸加固: 綁定地址與端口來自伺服器設定儲存, 請求主體上限 1 MiB, 閒置連線 60 秒後關閉, 端口被佔用或綁定被拒絕時以 `port_in_use` / `bind_failed` 狀態附帶提示結束而非崩潰
* `新增` SDK 傳輸前置的 DNS rebinding 保護: 迴環模式只接受 `localhost` / `127.0.0.1` / `[::1]` 作為 `Host`, 區域網絡模式加入裝置目前的 IPv4 地址與可選的額外主機名稱並在網絡變化時刷新; 瀏覽器來源一律拒絕, 僅 "開發者模式" 開關經 CORS 放行 Inspector 的迴環來源
* `新增` 伺服器識別 `autojs6-mcp-server` 攜帶插件版本, 並宣告 tools (`listChanged`), resources 與 prompts 能力; `tools/list` 保持註冊順序以便客戶端緩存
* `新增` 每個 `/mcp` 請求的 Bearer 令牌鑑權: 首次啟動時生成 32 位元組令牌, 以 Android Keystore 的 AES-GCM 密鑰包裹後存放在插件私有且不參與備份的儲存中; `Authorization` 標頭缺失或錯誤時經常數時間比較後以 `401` + `WWW-Authenticate: Bearer` 與 JSON-RPC `-32001` 錯誤拒絕; 令牌不寫入日誌
* `新增` 傳輸前置的首次配對: 未配對客戶端可以 `initialize` 並列出 tools, resources 與 prompts, 但首次 `tools/call`, `resources/read`, `resources/subscribe` 或 `prompts/get` 返回 `PAIRING_REQUIRED` (`-32002`), 直到 60 秒內在手機上確認; 拒絕或逾時後 30 秒內返回 `PAIRING_DENIED` (`-32003`); 客戶端按 `clientInfo` 名稱 (缺失時用 `User-Agent`) 加地址類別 (迴環 / 區域網絡) 識別, 因此令牌輪換不影響既有配對, 最多可配對 32 個客戶端
* `新增` 手機上的配對確認走雙通道: 帶允許 / 拒絕動作的高優先級通知, 以及屏幕解鎖時彈出的對話框; 伺服器設定, 令牌與已配對客戶端保存在原子替換的檔案中, 伺服器進程與設定頁共享且不會讀到過期緩存
* `新增` 帶分組開關的工具目錄 (決策 D6): `device_ping` (插件本機), `device_info` (AutoJs6 `device.info`) 與 `script_run` (AutoJs6 `engines.execScript`: 執行 JavaScript, 最多等待 `timeoutMs` 直到腳本結束, 回傳結果與最新的控制台行, 執行期間發送進度通知); 每個工具聲明封閉的 JSON Schema (`additionalProperties: false`), 參數在送達 AutoJs6 之前完成校驗; 分組開關 `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` 存放於 `tool_groups.json`, 關閉的分組自下一次請求起從 `tools/list` 消失, 其工具回應 `TOOL_DISABLED`
* `新增` 宿主橋接: `org.autojs.plugin.MCP_SERVER` 服務實現真實的 `IMcpServerPlugin` Binder (`getInfo` / `getCapabilities` 報告契約版本 1, 工具分組, MCP 協議版本與 SDK 版本; `openServer` 只接受已安裝且同簽名的 AutoJs6, 回傳帶 `getStatus` / `updateConfig` / `stop` / `close` 的 `IMcpServerSession`); 工具呼叫經宿主能力代理傳遞, 帶單調遞增的請求 id, 每次呼叫的逾時, 4 路並發上限, 宿主錯誤類別映射為 `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; AutoJs6 退出時監聽器繼續執行, 依賴宿主的工具回應 `HOST_UNAVAILABLE` 直到宿主重新連接; 狀態與事件 (`pairing_requested`, `client_paired`, `tool_call`, `warning`) 經回調送達宿主
* `新增` 前台服務通知顯示端點, AutoJs6 連接狀態與已配對客戶端數, 並提供停止動作; 通知被停用時以 toast 提示端點; `dumpsys activity service` 額外打印宿主會話, 工具分組開關與已註冊工具
* `新增` 補全腳本分組: `script_run_file` 運行裝置上的腳本檔案, `script_stop` / `script_stop_all` 停止一個或全部 AutoJs6 執行, `script_list` 列出運行中的執行, `console_tail` 回傳最新的控制台行並帶 `nextSinceId` 游標與級別過濾; `script_run` 與 `script_run_file` 現在回傳 `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, 含行號的異常與最新的控制台行, 等待期間每 2 s 發送一條攜帶最新控制台行的進度通知
* `新增` MCP 端點的回應改為以伺服器發送事件 (SSE) 串流返回 (不再使用 SDK 的 JSON 回應模式), 屬於某個請求的通知 (如運行中腳本的進度心跳) 會隨該請求自身的回應送達客戶端
* `新增` 新增 UI 分組 (roadmap P3.2): `ui_dump` 以帶 `#n` 引用的緊湊節點樹回傳當前視窗 (`format` 為 text / json / xml, `maxNodes` 最多 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` 輪詢選擇器, `ui_current_window` 與 `ui_explain_selector` 報告視窗與選擇器失敗的原因, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` 作用於 `nodeRef` (按指紋重新定位, 節點消失時回傳 `NODE_REF_STALE`) 或 `selector`, `ui_press_key` 按下 back / home / recents / notifications / quick_settings / power_dialog / lock_screen, 預設關閉的 `ui_gesture` 分組新增 `ui_swipe`, `ui_gesture` 與點擊工具的座標形式 (分組關閉時回傳 `TOOL_DISABLED`); 工具目錄快照增至 20 個工具; 座標手勢需要 2026-09-11 或之後構建的 AutoJs6 宿主 (更早的宿主會隨機以 "the system cancelled ..." 回應)
* `新增` 截圖分組 (P3.3): screen_capture 傳回 MCP 圖片, 支援裁剪, scale 或 maxWidth, JPEG / PNG / WebP 與品質參數. 預設 JPEG 品質 70, 最長邊 1280 px. base64 超過 4 MiB 時降低品質或尺寸重試, 中繼資料說明調整情況. screen_state 傳回亮屏狀態, 尺寸, 方向和密度. 工具目錄現有 22 項. MediaProjection 回退需要 2026-09-13 或之後建置的 AutoJs6 主程式及手機端授權, 主程式工作階段重用該授權.
* `優化` 建置階段阻止意外引入原生相依套件, 並輸出 JSON 校驗報告
* `依賴` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 與 Ktor 3.5.1 CIO 引擎
* `依賴` 附加 Ktor 3.5.1 `ktor-server-test-host` 用於 JVM 傳輸測試 (僅測試範圍)
* `依賴` 附加 `mcp-server-api.aar` (AutoJs6 模組 `plugin-api/mcp-server-api`, 宿主構建 6.8.0 / 5279, MPL 2.0) 作為 AutoJs6 與插件之間的 Binder 契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
