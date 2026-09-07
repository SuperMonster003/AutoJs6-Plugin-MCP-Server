MCP Server 让运行 AutoJs6 的 Android 设备成为一台 [Model Context Protocol](https://modelcontextprotocol.io) 服务器. 电脑上的 AI 代理 (如 Claude Code, Cursor 或 MCP Inspector) 通过 USB 或 Wi-Fi 连接手机, 借助工具运行脚本, 读取日志, 查看无障碍节点树, 点击与输入, 截取屏幕, 以及操作文件与应用.

项目处于骨架阶段: 本版本向 AutoJs6 插件中心注册插件, 并准备好构建, 文档与测试基础设施. MCP 端点及其工具尚不可用. 进度在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) 中逐项跟踪.

### 使用方法

1. 在安装了 AutoJs6 构建 5279 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下载并安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `MCP Server` 已被识别并启用. 官方发布包会自动通过签名校验.
3. 在 AutoJs6 抽屉或插件设置页开启 MCP 服务器; 手机上会显示端点地址与配对令牌.
4. 在电脑上执行 `adb forward tcp:9637 tcp:9637`, 并让 MCP 客户端连接 `http://127.0.0.1:9637/mcp`, 以令牌作为 Bearer 凭据.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).
