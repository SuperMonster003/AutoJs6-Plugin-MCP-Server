MCP Server 让运行 AutoJs6 的 Android 设备成为一台 [Model Context Protocol](https://modelcontextprotocol.io) 服务器. 电脑上的 AI 代理 (如 Claude Code, Cursor 或 MCP Inspector) 通过 USB 或 Wi-Fi 连接手机, 借助工具运行脚本, 读取日志, 查看无障碍节点树, 点击与输入, 截取屏幕, 以及操作文件与应用.

P3.5 开发预览: 工具目录共 37 项, 默认启用 33 项. 文件操作, 编辑器定位, 应用查询, 剪贴板, 无障碍自动启用和限制输出的 Shell 已接入, 与脚本, UI 和截图工具配合使用. 抽屉开关和设置页仍计划在 P4 实现. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### 使用方法

1. 在安装了 AutoJs6 构建 5279 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下载并安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `MCP Server` 已被识别并启用. 官方发布包会自动通过签名校验.
3. 本开发预览通过开发文档中的 adb 控制面与宿主测试会话连接; 抽屉开关和插件设置页计划在 P4 实现.
4. 在电脑上执行 `adb forward tcp:9637 tcp:9637`, 并让 MCP 客户端连接 `http://127.0.0.1:9637/mcp`, 以令牌作为 Bearer 凭据.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).
