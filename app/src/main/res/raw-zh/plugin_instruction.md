MCP Server 让运行 AutoJs6 的 Android 设备成为一台 [Model Context Protocol](https://modelcontextprotocol.io) 服务器. 电脑上的 AI 代理 (如 Claude Code, Cursor 或 MCP Inspector) 通过 USB 或 Wi-Fi 连接手机, 借助工具运行脚本, 读取日志, 查看无障碍节点树, 点击与输入, 截取屏幕, 以及操作文件与应用.

版本 1.0.1: 37 个工具 (默认启用 33 个), MCP 资源与提示, AutoJs6 抽屉开关和插件设置页. 需要 AutoJs6 6.8.0 (构建 5279) 或更高版本; 可选的 autojs6://docs/ 资源还需要 AutoJs6 离线文档插件以及带转读方法的宿主. 进度与证据记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### 使用方法

1. 在安装了 AutoJs6 构建 5279 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下载并安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `MCP Server` 已被识别并启用. 官方发布包会自动通过签名校验.
3. 在 AutoJs6 抽屉中打开 MCP 服务器. 长按条目标题或点击插件中心的设置入口, 进入设置页并复制电脑客户端所需配置.
4. 在电脑上执行 `adb forward tcp:9637 tcp:9637`, 并让 MCP 客户端连接 `http://127.0.0.1:9637/mcp`, 以令牌作为 Bearer 凭据.
5. 首次连接时在手机上确认配对请求. 使用完毕后可从抽屉, 设置页或通知中停止服务.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).
