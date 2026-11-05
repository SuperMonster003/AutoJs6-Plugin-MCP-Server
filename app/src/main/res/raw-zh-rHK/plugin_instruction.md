MCP Server 讓運行 AutoJs6 的 Android 裝置成為一台 [Model Context Protocol](https://modelcontextprotocol.io) 伺服器. 電腦上的 AI 代理 (例如 Claude Code, Cursor 或 MCP Inspector) 透過 USB 或 Wi-Fi 連接手機, 借助工具執行腳本, 讀取日誌, 檢視無障礙節點樹, 點擊與輸入, 擷取螢幕, 以及操作檔案與應用程式. 安裝相應的 AutoJs6 外掛後, 還能識別螢幕或圖片中的文字與條碼, 找圖找色, 轉換拼音以及讀取媒體信息. 插件也可以自行聲明工具 (例如中文繁簡轉換), 由 MCP Server 按插件分組提供.

版本 1.3.0: 39 個內建工具 (預設啟用 35 個) 與 8 個外掛能力工具, MCP 資源與提示, AutoJs6 抽屜開關與外掛程式設定頁. 需要 AutoJs6 6.8.0 (組建 5298) 或更高版本; 外掛能力工具只在 AutoJs6 中安裝並啟用對應外掛時出現, 外掛能力工具與已登記腳本工具在宿主不支援時自動隱藏, 可選的 autojs6://docs/ 資源還需要 AutoJs6 離線文件外掛以及帶轉讀方法的宿主. AutoJs6 插件自行聲明的工具 (如 3-Hanzi CC 的 opencc_convert) 按插件分組另外列出, 需要 AutoJs6 6.8.0 及以上. 進度與證據記錄在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### 使用方法

1. 在安裝了 AutoJs6 組建 5298 (6.8.0) 或更高版本的裝置上, 從 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下載並安裝外掛 APK.
2. 開啟 AutoJs6 外掛中心, 確認 `MCP Server` 已被識別並啟用. 官方發佈套件會自動通過簽章驗證.
3. 在 AutoJs6 抽屜中開啟 MCP 伺服器. 長按項目標題或點選外掛程式中心的設定入口, 進入設定頁並複製電腦用戶端所需設定.
4. 在電腦上執行 `adb forward tcp:9637 tcp:9637`, 並讓 MCP 用戶端連接 `http://127.0.0.1:9637/mcp`, 以權杖作為 Bearer 憑證.
5. 首次連線時在手機上確認配對要求. 使用完畢後可從抽屜, 設定頁或通知中停止服務.

連接指南與目前進度請參閱 [專案 README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) 與 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).
