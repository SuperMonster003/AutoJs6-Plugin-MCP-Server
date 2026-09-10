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

專案處於骨架階段: 本版本向 AutoJs6 外掛中心註冊外掛, 並準備好建置, 文件與測試基礎設施. MCP 端點及其工具尚未可用. 進度在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) 中逐項追蹤.

******

### 規劃功能

******

路線圖分階段交付以下能力:

- 指令碼執行: 在 AutoJs6 內執行文字或檔案形式的 JavaScript, 列出與停止引擎, 讀取最近的主控台輸出.
- 無障礙介面: 以精簡文字格式匯出節點樹, 用 AutoJs6 選擇器語法尋找節點, 點擊, 長按, 捲動, 設定文字, 以及觸發返回和主畫面等全域按鍵.
- 截圖: 以 PNG 或 JPEG 擷取螢幕, 並限制尺寸以配合多模態模型.
- 檔案, 應用程式與裝置: 讀寫 AutoJs6 工作目錄下的檔案, 啟動應用程式, 查詢前景視窗, 回報裝置資訊.
- 連線方式: 透過 `adb forward` 的 USB 連線, 須明確開啟的區域網路連線, 電腦端 stdio 橋接程式, 以及可選的公網通道與 OAuth 2.1.
- 安全: 可輪換的 Bearer 權杖, 手機端首次配對確認, 依分組的工具開關; 伺服器預設只監聽回送介面.

******

### 使用方式

******

1. 在安裝了 AutoJs6 組建 5279 (6.8.0) 或更新版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下載並安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `MCP Server` 已被辨識並啟用. 官方發行套件會自動通過簽章驗證.
3. 在 AutoJs6 側邊欄或外掛設定頁開啟 MCP 伺服器; 手機上會顯示端點位址與配對權杖.
4. 在電腦上執行 `adb forward tcp:9637 tcp:9637`, 並讓 MCP 用戶端連線 `http://127.0.0.1:9637/mcp`, 以權杖作為 Bearer 憑證.

> 第 3 步與第 4 步描述的是規劃中的流程, 將在對應路線圖階段完成後可用. 外掛支援 Android 7.0 (API 24) 及以上版本.

******

### 用戶端設定

******

Claude Code 只需一道命令即可註冊伺服器; 其他用戶端在各自的 MCP 設定中使用相同的 URL 與請求標頭:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

請將權杖替換為手機上顯示的值. 該命令只有在伺服器可以啟動後才會生效 (見 `目前狀態`).

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

`McpServerPluginService` 回應 `org.autojs.plugin.MCP_SERVER` action (category `mcp-server`), 在 `:mcp_server` 處理程序中執行. AIDL 契約 `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` 由主程式在 `mcp-server-api` 模組中定義, 隨路線圖 P1 階段落地; 在此之前服務只公開契約描述元. `McpServerPluginInfoService` 以標準 `PluginInfo` 回應 `org.autojs.plugin.INFO`, `WakeActivity` 則讓主程式能在會保持新裝應用程式停止狀態的裝置上喚醒外掛處理程序.

******

### 路線圖

******

外掛的規劃與進度以可勾選清單的形式維護在 ROADMAP.md 中, 依階段組織並附有驗收條件與證據等級. 未勾選條目表達的是意圖而非目前能力; 歡迎透過 Issues 討論.

- [檢視 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### 發行歷史

******

#### v1.0.0

_2026/09/10_

- `提示` 開發預覽版: 外掛已可在 AutoJs6 外掛中心註冊, 但 MCP 端點及其工具尚未可用
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
- `相依性` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 與 Ktor 3.5.1 CIO 引擎
- `相依性` 附加 Ktor 3.5.1 `ktor-server-test-host` 用於 JVM 傳輸測試 (僅測試範圍)
- `相依性` 附加 `mcp-server-api.aar` (AutoJs6 模組 `plugin-api/mcp-server-api`, 宿主建置 6.8.0 / 5279, MPL 2.0) 作為 AutoJs6 與外掛之間的 Binder 契約, 並在 `locks/host-api-aars.lock` 中鎖定雜湊

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
