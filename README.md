<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>通过 Model Context Protocol 向 AI 代理开放设备自动化能力</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### 简介

******

MCP Server 让运行 AutoJs6 的 Android 设备成为一台 [Model Context Protocol](https://modelcontextprotocol.io) 服务器. 电脑上的 AI 代理 (如 Claude Code, Cursor 或 MCP Inspector) 通过 USB 或 Wi-Fi 连接手机, 借助工具运行脚本, 读取日志, 查看无障碍节点树, 点击与输入, 截取屏幕, 以及操作文件与应用.

服务器运行在插件自己的进程中, 通过单一的 Streamable HTTP 端点对外提供服务. AutoJs6 通过 Binder 向插件下发能力代理, 每次工具调用都由宿主以其既有的权限, 引擎和无障碍服务执行; 插件不会复制宿主的任何功能.

******

### 当前状态

******

项目处于骨架阶段: 本版本向 AutoJs6 插件中心注册插件, 并准备好构建, 文档与测试基础设施. MCP 端点及其工具尚不可用. 进度在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) 中逐项跟踪.

******

### 规划功能

******

路线图分阶段交付以下能力:

- 脚本执行: 在 AutoJs6 内运行文本或文件形式的 JavaScript, 列出与停止引擎, 读取最近的控制台输出.
- 无障碍界面: 以紧凑文本格式导出节点树, 用 AutoJs6 选择器语法查找节点, 点击, 长按, 滚动, 设置文本, 以及触发返回和主屏幕等全局按键.
- 截图: 以 PNG 或 JPEG 截取屏幕, 并限制尺寸以适配多模态模型.
- 文件, 应用与设备: 读写 AutoJs6 工作目录下的文件, 启动应用, 查询前台窗口, 报告设备信息.
- 连接方式: 通过 `adb forward` 的 USB 连接, 需显式开启的局域网连接, 电脑端 stdio 桥接程序, 以及可选的公网隧道与 OAuth 2.1.
- 安全: 可轮换的 Bearer 令牌, 手机端首次配对确认, 按分组的工具开关; 服务器默认只监听回环接口.

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5279 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下载并安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `MCP Server` 已被识别并启用. 官方发布包会自动通过签名校验.
3. 在 AutoJs6 抽屉或插件设置页开启 MCP 服务器; 手机上会显示端点地址与配对令牌.
4. 在电脑上执行 `adb forward tcp:9637 tcp:9637`, 并让 MCP 客户端连接 `http://127.0.0.1:9637/mcp`, 以令牌作为 Bearer 凭据.

> 第 3 步与第 4 步描述的是规划中的流程, 将在对应路线图阶段完成后可用. 插件支持 Android 7.0 (API 24) 及以上版本.

******

### 客户端配置

******

Claude Code 只需一条命令即可注册服务器; 其他客户端在各自的 MCP 配置中使用相同的 URL 与请求头:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

请将令牌替换为手机上显示的值. 该命令只有在服务器可以启动后才会生效 (见 `当前状态`).

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够绑定.
- INTERNET 权限仅用于插件自身的 HTTP 监听; 插件不发起任何出站请求, 也不收集数据.
- 工具调用经由 AutoJs6 能力代理执行, 永远不会超出宿主自身的权限范围; Shell 命令与文件删除等危险分组默认关闭, 直到用户手动开启.
- 已禁用备份, 令牌只保存在插件的私有存储中.

请只从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 页面或 AutoJs6 插件中心获取插件. 来源不明的安装包即使版本号相同, 也可能无法通过宿主校验或带来风险.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者; 宿主使用这些标识发现插件并协商兼容性:

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

`McpServerPluginService` 响应 `org.autojs.plugin.MCP_SERVER` action (category `mcp-server`), 运行在 `:mcp_server` 进程中. AIDL 契约 `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` 由宿主在 `mcp-server-api` 模块中定义, 随路线图 P1 阶段落地; 在此之前服务只暴露契约描述符. `McpServerPluginInfoService` 以标准 `PluginInfo` 响应 `org.autojs.plugin.INFO`, `WakeActivity` 则让宿主能在会保持新装应用停止状态的设备上唤醒插件进程.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/09/07_

- `提示` 开发预览版: 插件已可在 AutoJs6 插件中心注册, 但 MCP 端点及其工具尚不可用
- `新增` 插件标识 `mcp-server`, 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.MCP_SERVER` 服务骨架
- `新增` 10 种语言的 README, 插件中心说明与更新日志
- `新增` 位于 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端点及 `device_ping` 工具, 由可经 adb 或宿主启停的前台服务承载 (开发预览)
- `依赖` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 与 Ktor 3.5.1 CIO 引擎

##### 更多发行历史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建与验证

******

本节面向希望从源码构建插件的开发者; 普通用户直接安装 Releases 页面的预构建 APK 即可.

构建 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

运行 JVM 单元测试并构建 instrumentation 测试 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

构建 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集发布产物并在文件名后追加版本与 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

校验多语言文档源与生成产物是否同步 (CI 同样执行此检查):

```powershell
py .python\generate_markdown.py --check
```

构建需要 JDK 21 或更高版本以及 Android SDK 36; Gradle 与插件版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 统一管理.

******

### 本地化与文档生成

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

`.readme/` 与 `.changelog/` 下的语言 JSON 文件是 README, 插件中心说明与更新日志的唯一文案源. 请始终修改这些 JSON 源文件并重新运行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 与更新日志产物不得手工编辑. 运行 `py .python/generate_markdown.py --check` 可校验全部生成产物.

******

### 许可证

******

项目代码基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE) 授权. 第三方组件及其许可证列于 [第三方声明](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- Model Context Protocol 规范: https://modelcontextprotocol.io
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md
