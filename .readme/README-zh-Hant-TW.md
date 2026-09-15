<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>透過 Model Context Protocol 向 AI 代理開放裝置自動化能力</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 語言

******

目前 README.md 支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### 簡介

******

MCP Server 讓執行 AutoJs6 的 Android 裝置成為一台 [Model Context Protocol](https://modelcontextprotocol.io) 伺服器. 電腦上的 AI 代理 (例如 Claude Code, Cursor 或 MCP Inspector) 透過 USB 或 Wi-Fi 連線手機, 藉由工具執行指令碼, 讀取日誌, 檢視無障礙節點樹, 點擊與輸入, 擷取螢幕, 以及操作檔案與應用程式.

伺服器在外掛自己的處理程序中執行, 透過單一的 Streamable HTTP 端點對外提供服務. AutoJs6 透過 Binder 向外掛下發能力代理, 每次工具呼叫都由主程式以其既有的權限, 引擎和無障礙服務執行; 外掛不會複製主程式的任何功能.

******

### 目前狀態

******

P4 開發預覽: 37 個工具, 預設啟用 33 個, 提供 AutoJs6 抽屜開關與外掛程式設定頁. 需要配套的 P4 AutoJs6 組建. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### 規劃功能

******

路線圖分階段交付以下能力:

- 手機設定頁提供服務狀態, USB 轉發, 連接埠與區域網路存取, 權杖顯示/複製/輪換, 配對撤銷, 工具群組與 root 權限, 開發者模式, 可複製的 Claude Code / Cursor / Codex / 通用 HTTP 設定及發行歷史, 並跟隨 AutoJs6 外觀. 網路設定會重新啟動執行中的監聽器, 權杖與權限變更立即生效. 敏感資訊視窗禁止截圖.
- 指令碼執行: 在 AutoJs6 內執行文字或檔案形式的 JavaScript, 列出與停止引擎, 讀取最近的主控台輸出.
- 無障礙介面: 以精簡文字格式匯出節點樹, 用 AutoJs6 選擇器語法尋找節點, 點擊, 長按, 捲動, 設定文字, 以及觸發返回和主畫面等全域按鍵.
- 螢幕擷取群組 (P3.3): screen_capture 傳回 MCP 圖片, 支援裁切, scale 或 maxWidth, JPEG / PNG / WebP 與品質參數. 預設 JPEG 品質 70, 最長邊 1280 px. base64 超過 4 MiB 時降低品質或尺寸重試, 中繼資料說明調整情況. screen_state 傳回亮屏狀態, 尺寸, 方向和密度. 工具目錄現有 37 項. MediaProjection 備援需要 2026-09-13 或之後建置的 AutoJs6 主程式及手機端授權, 主程式工作階段重用該授權.
- 工作目錄工具 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 使用從 1 開始的行列號的 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast 和 shell_exec. 二進位讀取使用 base64, 原始資料最多 1 MiB. 寫入亦受宿主請求預算約束 (通常為包含 JSON 跳脫的 96 KiB). 檔案刪除和 Shell 預設關閉; root 另需 allowShellRoot 開關與宿主 shell.root 授權. 這些能力需要相符的 P3.4 宿主建置.
- MCP 資源 (P3.5) 提供唯讀工作目錄檔案, 可瀏覽的宿主範例, 裝置資訊和最近主控台輸出, 遵守配對與分組開關. 文字和二進位讀取報告截斷狀態. write_autojs6_script, automate_task 和 debug_selector 提示提供中英文指引, 其他手機語言回退英語.
- 連線方式: 透過 `adb forward` 的 USB 連線, 須明確開啟的區域網路連線, 電腦端 stdio 橋接程式, 以及可選的公網通道與 OAuth 2.1.
- 安全: 可輪換的 Bearer 權杖, 手機端首次配對確認, 依分組的工具開關; 伺服器預設只監聽回送介面.

******

### 使用方式

******

1. 在安裝了 AutoJs6 組建 5279 (6.8.0) 或更新版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下載並安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `MCP Server` 已被辨識並啟用. 官方發行套件會自動通過簽章驗證.
3. 在 AutoJs6 抽屜中開啟 MCP 伺服器. 長按項目標題或點選外掛程式中心的設定入口, 進入設定頁並複製電腦用戶端所需設定.
4. 在電腦上執行 `adb forward tcp:9637 tcp:9637`, 並讓 MCP 用戶端連線 `http://127.0.0.1:9637/mcp`, 以權杖作為 Bearer 憑證.
5. 首次連線時在手機上確認配對要求. 使用完畢後可從抽屜, 設定頁或通知中停止服務.

> MCP 伺服器抽屜開關提供安裝, 啟用, 授權與相容性引導, 與通知停止操作同步, 重新連線時保留外掛程式設定, 抽屜及外掛程式中心透過權限檢查開啟同一設定頁. AutoJs6 開啟時恢復先前啟用的伺服器, 但遵循宿主離線期間的使用者停止操作, 不隨裝置開機自啟.

******

### 用戶端設定

******

Claude Code 只需一道命令即可註冊伺服器; 其他用戶端在各自的 MCP 設定中使用相同的 URL 與請求標頭:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

在 AutoJs6 抽屜中開啟 MCP 伺服器. 長按項目標題或點選外掛程式中心的設定入口, 進入設定頁並複製電腦用戶端所需設定. 首次連線時在手機上確認配對要求. 使用完畢後可從抽屜, 設定頁或通知中停止服務.

******

### 連線路徑

******

USB: `adb forward tcp:9637 tcp:9637` 將手機連接埠對應到 PC; 多台裝置時加上 `-s <serial>` (透過 `adb devices` 查看), 模擬器同樣適用. 任一側連接埠被占用時, 在設定頁修改連接埠並轉送新連接埠. 連線卡片提供可直接複製的轉送命令.

區域網路: 在設定頁開啟 "允許區域網路連線". 設定頁隨後列出手機目前位址 (隨 Wi-Fi 變化重新整理) 並提示用戶端需處於同一網路; 訪客網路, AP 隔離和 PC 防火牆是常見阻礙. 來自區域網路的配對要求會被顯著標註; 伺服器持續可從網路存取期間每日通知提醒, 提醒可關閉.

兩條路徑使用相同的權杖和相同的手機側配對. 沒有 HTTP 傳輸的用戶端使用 "接入" 中介紹的 stdio 橋接程式.

******

### 接入

******

設定頁會為下列每個用戶端複製帶真實權杖的現成設定; 此處的片段以 `<token>` 作為占位. 所有用戶端都透過帶 Authorization 標頭的 Streamable HTTP 通訊, 新用戶端的首次呼叫需在手機上確認. 已實測: Claude Code, Codex CLI 與 MCP Inspector; 其餘用戶端使用相同的 URL 與標頭, 但維護者尚未測試.

Claude Code: 執行 "用戶端設定" 中的命令 (設定頁複製的版本已帶權杖); 隨後 `claude mcp list` 會將 `autojs6` 顯示為 Connected.

Cursor: 將下面的條目加入 `mcp.json`:

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

Codex CLI: 將權杖放入環境變數 `AUTOJS6_MCP_TOKEN` (設定頁可複製對應的 PowerShell 命令), 再把伺服器加入 `config.toml`, 或執行 `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN`:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: CLI 模式無需額外設定, Web 介面透過自帶的 Node 代理存取手機. 僅當瀏覽器頁面直接連線端點時, 才需在設定頁開啟開發者模式:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline, VS Code Copilot Chat, Gemini CLI 等用戶端: 在各自的 MCP 設定中使用相同的 URL 與標頭; 設定頁提供帶 `"type": "http"` 的通用 JSON 片段.

Claude Desktop 及其他只支援 stdio 的用戶端: 用 `npm install -g autojs6-mcp-bridge` 安裝橋接程式, 將 `autojs6-mcp-bridge --serial <serial>` 註冊為 stdio 伺服器並把 `AUTOJS6_MCP_TOKEN` 放入其環境變數區塊 (Claude Desktop 與 Claude Code 的片段見[橋接程式 README](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge)). 橋接程式 0.1.0 對應外掛 1.0.0, 透傳用戶端的協定版本; 已用 Claude Code 2.1.257 經 stdio 驗證.

******

### 常見問題

******

- 401 Unauthorized: 權杖缺失, 輸錯或已輪換. 從設定頁重新複製設定; 輪換權杖後每個用戶端都需要新值.
- 配對逾時: 新用戶端的首次呼叫會等待約一分鐘, 直到手機上點選允許. 解鎖手機, 接受對話方塊或通知動作, 然後重複呼叫. 拒絕會進入短暫冷卻, 之後下一次呼叫會再次詢問.
- HOST_UNAVAILABLE: AutoJs6 未執行或外掛工作階段已關閉. 開啟 AutoJs6, 保持抽屜開關開啟, 並在設定頁查看連線狀態.
- 無障礙未啟用: `ui_*` 工具與截圖需要 AutoJs6 無障礙服務. 呼叫 `device_ensure_accessibility`, 或在系統無障礙設定中啟用該服務.
- 連接埠被占用: 抽屜會回報 `port_in_use`. 在設定頁修改連接埠, 並用 adb 轉送新連接埠.
- 區域網路不可達: 開啟區域網路存取, 使用設定頁列出的位址, 讓 PC 與手機處於同一網路且無訪客隔離, 並在 PC 防火牆放行該連接埠. 手機 Wi-Fi 省電會使每次呼叫增加幾百毫秒.
- 熄屏後伺服器消失: 限制了本應用程式電池用量的手機 (HyperOS 與 MIUI 預設對側載應用程式如此) 會在使用電池熄屏約一分鐘後停止前景服務. 此時設定頁顯示警告和 "電池設定" 按鈕, 請在其中為 MCP Server 選擇 "無限制". 可選的 "閒置時自動停止" (預設關閉) 也會在所選分鐘數內無請求時停止伺服器, 並留下一條說明通知.

******

### 權限與安全

******

外掛遵循明確的邊界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 能夠繫結.
- INTERNET 權限僅用於外掛自身的 HTTP 監聽; 外掛不發起任何對外請求, 也不蒐集資料.
- 工具呼叫經由 AutoJs6 能力代理執行, 永遠不會超出主程式自身的權限範圍; Shell 命令與檔案刪除等危險分組預設關閉, 直到使用者手動開啟.
- 已停用備份, 權杖只儲存在外掛的私有儲存空間中.

請只從官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 頁面或 AutoJs6 外掛中心取得外掛. 來源不明的安裝套件即使版本號相同, 也可能無法通過主程式驗證或帶來風險.

******

### 外掛介面

******

以下資訊面向 AutoJs6 主程式與外掛開發者; 主程式使用這些識別碼探索外掛並協商相容性:

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

`McpServerPluginService` 在 `:mcp_server` 程序中實作宿主 mcp-server-api 契約 `org.autojs.plugin.mcp.server.api.IMcpServerPlugin`, 回應 `org.autojs.plugin.MCP_SERVER` (category `mcp-server`). `McpServerPluginInfoService` 以 PluginInfo 回應 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主啟動外掛.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 依階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/15_

- `提示` P4 開發預覽: 37 個工具, 預設啟用 33 個, 提供 AutoJs6 抽屜開關與外掛程式設定頁. 需要配套的 P4 AutoJs6 組建. ROADMAP.md.
- `新增` 手機設定頁提供服務狀態, USB 轉發, 連接埠與區域網路存取, 權杖顯示/複製/輪換, 配對撤銷, 工具群組與 root 權限, 開發者模式, 可複製的 Claude Code / Cursor / Codex / 通用 HTTP 設定及發行歷史, 並跟隨 AutoJs6 外觀. 網路設定會重新啟動執行中的監聽器, 權杖與權限變更立即生效. 敏感資訊視窗禁止截圖.
- `新增` MCP 資源 (P3.5) 提供唯讀工作目錄檔案, 可瀏覽的宿主範例, 裝置資訊和最近主控台輸出, 遵守配對與分組開關. 文字和二進位讀取報告截斷狀態. write_autojs6_script, automate_task 和 debug_selector 提示提供中英文指引, 其他手機語言回退英語.
- `新增` 工作目錄工具 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 使用從 1 開始的行列號的 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast 和 shell_exec. 二進位讀取使用 base64, 原始資料最多 1 MiB. 寫入亦受宿主請求預算約束 (通常為包含 JSON 跳脫的 96 KiB). 檔案刪除和 Shell 預設關閉; root 另需 allowShellRoot 開關與宿主 shell.root 授權. 這些能力需要相符的 P3.4 宿主建置.
- `新增` 外掛識別碼 `mcp-server`, 含 INFO 服務, Wake Activity 以及供主程式探索的 `org.autojs.plugin.MCP_SERVER` 服務骨架
- `新增` 10 種語言的 README, 外掛中心說明與更新日誌
- `新增` 位於 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端點及 `device_ping` 工具, 由可經 adb 或宿主啟停的前台服務承載 (開發預覽)
- `新增` `/mcp` 端點的傳輸加固: 綁定位址與連接埠來自伺服器設定存放區, 請求本體上限 1 MiB, 閒置連線 60 秒後關閉, 連接埠被佔用或綁定被拒絕時以 `port_in_use` / `bind_failed` 狀態附帶提示結束而非當機
- `新增` SDK 傳輸前置的 DNS rebinding 保護: 迴環模式只接受 `localhost` / `127.0.0.1` / `[::1]` 作為 `Host`, 區域網路模式加入裝置目前的 IPv4 位址與可選的額外主機名稱並在網路變化時重新整理; 瀏覽器來源一律拒絕, 僅 "開發者模式" 開關經 CORS 放行 Inspector 的迴環來源
- `新增` 伺服器識別 `autojs6-mcp-server` 攜帶外掛程式版本, 並宣告 tools (`listChanged`), resources 與 prompts 能力; `tools/list` 保持註冊順序以便用戶端快取
- `新增` 每個 `/mcp` 請求的 Bearer 權杖鑑權: 首次啟動時產生 32 位元組權杖, 以 Android Keystore 的 AES-GCM 金鑰包裹後存放在外掛程式私有且不參與備份的儲存空間; `Authorization` 標頭缺失或錯誤時經常數時間比較後以 `401` + `WWW-Authenticate: Bearer` 與 JSON-RPC `-32001` 錯誤拒絕; 權杖不寫入日誌
- `新增` 傳輸前置的首次配對: 未配對用戶端可以 `initialize` 並列出 tools, resources 與 prompts, 但首次 `tools/call`, `resources/read`, `resources/subscribe` 或 `prompts/get` 回傳 `PAIRING_REQUIRED` (`-32002`), 直到 60 秒內在手機上確認; 拒絕或逾時後 30 秒內回傳 `PAIRING_DENIED` (`-32003`); 用戶端按 `clientInfo` 名稱 (缺失時用 `User-Agent`) 加位址類別 (迴環 / 區域網路) 識別, 因此權杖輪換不影響既有配對, 最多可配對 32 個用戶端
- `新增` 手機上的配對確認走雙通道: 帶允許 / 拒絕動作的高優先級通知, 以及螢幕解鎖時彈出的對話框; 伺服器設定, 權杖與已配對用戶端保存在原子替換的檔案中, 伺服器程序與設定頁共享且不會讀到過期快取
- `新增` 帶分組開關的工具目錄 (決策 D6): `device_ping` (外掛本機), `device_info` (AutoJs6 `device.info`) 與 `script_run` (AutoJs6 `engines.execScript`: 執行 JavaScript, 最多等待 `timeoutMs` 直到指令碼結束, 回傳結果與最新的主控台行, 執行期間送出進度通知); 每個工具宣告封閉的 JSON Schema (`additionalProperties: false`), 參數在送達 AutoJs6 之前完成驗證; 分組開關 `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` 存放於 `tool_groups.json`, 關閉的分組自下一次請求起從 `tools/list` 消失, 其工具回應 `TOOL_DISABLED`
- `新增` 宿主橋接: `org.autojs.plugin.MCP_SERVER` 服務實作真實的 `IMcpServerPlugin` Binder (`getInfo` / `getCapabilities` 回報契約版本 1, 工具分組, MCP 協定版本與 SDK 版本; `openServer` 只接受已安裝且同簽章的 AutoJs6, 回傳帶 `getStatus` / `updateConfig` / `stop` / `close` 的 `IMcpServerSession`); 工具呼叫經宿主能力代理傳遞, 帶單調遞增的請求 id, 每次呼叫的逾時, 4 路並行上限, 宿主錯誤類別映射為 `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; AutoJs6 結束時監聽器繼續執行, 依賴宿主的工具回應 `HOST_UNAVAILABLE` 直到宿主重新連線; 狀態與事件 (`pairing_requested`, `client_paired`, `tool_call`, `warning`) 經回呼送達宿主
- `新增` 前景服務通知顯示端點, AutoJs6 連線狀態與已配對用戶端數, 並提供停止動作; 通知被停用時以 toast 提示端點; `dumpsys activity service` 額外列印宿主工作階段, 工具分組開關與已註冊工具
- `新增` 補全指令碼分組: `script_run_file` 執行裝置上的指令碼檔案, `script_stop` / `script_stop_all` 停止一個或全部 AutoJs6 執行, `script_list` 列出執行中的執行, `console_tail` 回傳最新的主控台行並帶 `nextSinceId` 游標與層級篩選; `script_run` 與 `script_run_file` 現在回傳 `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, 含行號的例外與最新的主控台行, 等待期間每 2 s 送出一則攜帶最新主控台行的進度通知
- `新增` MCP 端點的回應改為以伺服器傳送事件 (SSE) 串流回傳 (不再使用 SDK 的 JSON 回應模式), 屬於某個請求的通知 (如執行中指令碼的進度心跳) 會隨該請求自身的回應送達用戶端
- `新增` 新增 UI 分組 (roadmap P3.2): `ui_dump` 以帶 `#n` 參照的緊湊節點樹回傳目前視窗 (`format` 為 text / json / xml, `maxNodes` 最多 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` 輪詢選擇器, `ui_current_window` 與 `ui_explain_selector` 回報視窗與選擇器失敗的原因, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` 作用於 `nodeRef` (依指紋重新定位, 節點消失時回傳 `NODE_REF_STALE`) 或 `selector`, `ui_press_key` 按下 back / home / recents / notifications / quick_settings / power_dialog / lock_screen, 預設關閉的 `ui_gesture` 分組新增 `ui_swipe`, `ui_gesture` 與點擊工具的座標形式 (分組關閉時回傳 `TOOL_DISABLED`); 工具目錄快照增至 20 個工具; 座標手勢需要 2026-09-11 或之後建置的 AutoJs6 宿主 (更早的宿主會隨機以 "the system cancelled ..." 回應)
- `新增` 螢幕擷取群組 (P3.3): screen_capture 傳回 MCP 圖片, 支援裁切, scale 或 maxWidth, JPEG / PNG / WebP 與品質參數. 預設 JPEG 品質 70, 最長邊 1280 px. base64 超過 4 MiB 時降低品質或尺寸重試, 中繼資料說明調整情況. screen_state 傳回亮屏狀態, 尺寸, 方向和密度. 工具目錄現有 22 項. MediaProjection 備援需要 2026-09-13 或之後建置的 AutoJs6 主程式及手機端授權, 主程式工作階段重用該授權.
- `新增` 區域網路路徑 (P5.1): 開啟區域網路存取後, 設定頁列出手機目前位址 (Wi-Fi 變化時重新整理) 並提示同網段與防火牆; 來自區域網路的配對要求在對話方塊和通知中顯著標註; 每日提醒伺服器仍可從區域網路存取, 可關閉且不重啟監聽. README 記錄 USB 與區域網路路徑.
- `新增` 每用戶端速率限制 (P6): 每用戶端每秒最多 20 個請求, 每分鐘最多 30 次 screen_capture. 超限請求回傳 HTTP 429, 帶 Retry-After 標頭和含 retryAfterMs 的 JSON-RPC RATE_LIMITED 錯誤; 超限截圖以帶 retryAfterMs 的 RATE_LIMITED 工具結果回應, 便於模型等待. 宿主 grant 的無障礙查詢速率在此之上仍然生效.
- `新增` 閒置自動停止與電量 (P6): 設定頁新增 "閒置時自動停止" (預設關閉; 5 / 15 / 30 / 60 / 120 分鐘). 監聽器在所選時間內沒有用戶端請求時自行停止, 記錄外掛程式內部的 idle_timeout 原因, 留下一條可自動消失的通知, 且不算作使用者停止; 處理中的請求 (正在執行的工具呼叫) 不算閒置, 而僅保持連線卻不發請求的用戶端不會讓伺服器繼續執行. 當 Android 限制了本應用程式的電池用量 (HyperOS 與 MIUI 預設對側載應用程式如此, 熄屏並使用電池約一分鐘後會停止前景服務) 時, 設定頁顯示警告和 "電池設定" 按鈕, AutoJs6 收到一條 warning 事件. 閒置 CPU 時間與一小時閒置的 batterystats 估算記錄於 docs/dev/p6-battery-and-residency.md.
- `修復` IDE rebuild 不再為 JVM 單元測試尋找 APK. APK 驗證工作會自動組建所需產物, 可直接從 clean 後執行.
- `修復` 外掛程式中心明暗模式下圖示比例不一致及自適應圖示留白不足的問題; 夜間同樣使用自適應圖示, 調整圖層尺寸以保留 ic_launcher_round.png 的完整圖形和留白, 僅切換背景色
- `修復` 設定頁的鍵盤 Tab 導覽會略過工具列返回按鈕; 現在 Tab 循環涵蓋返回按鈕和全部控制項 (含 Android 7). 裝置測試檢查螢幕閱讀器標籤與鍵盤操作.
- `修復` script_run 與 script_run_file 的 arguments 對應以 JSON Schema 陣列宣告值型別, 部分 MCP 用戶端會拒絕或弱化; 現改為單型別 anyOf 分支. README 新增 "接入" 與 "常見問題" 章節及實測用戶端矩陣.
- `修復` 敵意請求體在任何解析器遞迴之前就被拒絕: 巢狀超過 64 層的 JSON, 重複請求 id 的請求體, 以及同一工作階段上仍在途的請求 id (原本會讓先到的請求得不到回應) 都回傳 400 與 JSON-RPC 錯誤; 沒有有效工作階段的 GET 串流回傳 400 / 404 而非空事件串流. JVM 測試與裝置測試 (API 28 / 31 / 33 / 35) 涵蓋超長, 超深, 非法 UTF-8, 未知方法, 標頭組合, 超大 base64 與 64 並行工作階段.
- `修復` 生命週期矩陣 (P6): 帶有上一個監聽處理程序工作階段 id 的請求 (監聽處理程序被終止, 或因權杖輪換而重新啟動之後) 交由傳輸層回 404, 讓用戶端重新 initialize, 而不再以請求的 User-Agent 為已配對的用戶端發起多餘的配對提示; 監聽處理程序死亡後殘留在通知列的配對通知在下一個監聽器啟動時清除. 宿主被終止, 監聽處理程序被終止, 兩者同時被終止, 在系統設定中強制停止, 活動工作階段中的權杖輪換, 以及跨越宿主被終止與監聽處理程序被終止的待確認配對, 各自的期望狀態與恢復路徑記錄於 docs/dev/lifecycle-matrix.md 並在實機上驗證.
- `優化` 建置階段阻止意外引入原生相依套件, 並輸出 JSON 校驗報告
- `相依性` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 與 Ktor 3.5.1 CIO 引擎
- `相依性` 附加 Ktor 3.5.1 `ktor-server-test-host` 用於 JVM 傳輸測試 (僅測試範圍)
- `相依性` 附加 `mcp-server-api.aar` (AutoJs6 模組 `plugin-api/mcp-server-api`, 宿主建置 6.8.0 / 5279, MPL 2.0) 作為 AutoJs6 與外掛之間的 Binder 契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊
- `相依性` 更新來自 P4 宿主組建的 common-plugin-api 與 mcp-server-api 配套 AAR: 可選設定擴充 v1, AIDL transaction 順序不變, SHA-256 鎖定, 保留 SDK 36 消費相容性.

##### 更多發行歷史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 建置與驗證

******

本節面向希望從原始碼建置外掛的開發者; 一般使用者直接安裝 Releases 頁面的預建 APK 即可.

建置 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

執行 JVM 單元測試並建置 instrumentation 測試 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

建置 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集發行產物並在檔案名稱後附加版本與 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件來源與產生的產物是否同步 (CI 同樣執行此檢查):

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 或更新版本以及 Android SDK 36; Gradle 與外掛版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 統一管理.

******

### 在地化與文件產生

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

`.readme/` 與 `.changelog/` 下的語言 JSON 檔案是 README, 外掛中心說明與更新日誌的唯一文案來源. 請始終修改這些 JSON 來源檔案並重新執行 `py .python/generate_markdown.py`; 產生的 README, `plugin_instruction.md` 與更新日誌產物不得手動編輯. 執行 `py .python/generate_markdown.py --check` 可驗證全部產生的產物.

******

### 授權條款

******

專案程式碼基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE) 授權. 第三方元件及其授權條款列於 [第三方聲明](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- Model Context Protocol 規範: https://modelcontextprotocol.io
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
