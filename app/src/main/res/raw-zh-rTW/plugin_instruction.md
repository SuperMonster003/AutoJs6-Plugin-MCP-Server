MCP Server 讓執行 AutoJs6 的 Android 裝置成為一台 [Model Context Protocol](https://modelcontextprotocol.io) 伺服器. 電腦上的 AI 代理 (例如 Claude Code, Cursor 或 MCP Inspector) 透過 USB 或 Wi-Fi 連線手機, 藉由工具執行指令碼, 讀取日誌, 檢視無障礙節點樹, 點擊與輸入, 擷取螢幕, 以及操作檔案與應用程式.

開發預覽已推進至 P3.3: 帶驗證與配對的 MCP 端點, 腳本工具, UI 工具和螢幕擷取工具均已實作. screen_capture 傳回 JPEG, PNG 或 WebP 圖片, screen_state 傳回螢幕尺寸與方向. 抽屜開關和設定頁仍計劃在 P4 實作. 進度與裝置證據見 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### 使用方式

1. 在安裝了 AutoJs6 組建 5279 (6.8.0) 或更新版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下載並安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `MCP Server` 已被辨識並啟用. 官方發行套件會自動通過簽章驗證.
3. 本開發預覽透過開發文件中的 adb 控制面與主程式測試工作階段連線; 抽屜開關和外掛設定頁計劃在 P4 實作.
4. 在電腦上執行 `adb forward tcp:9637 tcp:9637`, 並讓 MCP 用戶端連線 `http://127.0.0.1:9637/mcp`, 以權杖作為 Bearer 憑證.

連線指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).
