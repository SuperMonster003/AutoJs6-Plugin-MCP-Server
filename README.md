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

版本 1.0.2: 37 个工具 (默认启用 33 个), MCP 资源与提示, AutoJs6 抽屉开关和插件设置页. 需要 AutoJs6 6.8.0 (构建 5279) 或更高版本; 可选的 autojs6://docs/ 资源还需要 AutoJs6 离线文档插件以及带转读方法的宿主. 进度与证据记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### 功能

******

插件提供以下能力:

- 手机设置页提供服务状态, USB 转发, 端口和局域网访问, 令牌显示/复制/轮换, 配对撤销, 工具分组和 root 权限, 开发者模式, 可复制的 Claude Code / Cursor / Codex / 通用 HTTP 配置及发行历史, 并跟随 AutoJs6 外观. 网络设置会重启运行中的监听器, 令牌和权限变更立即生效. 敏感信息窗口禁止截图.
- 脚本执行: 在 AutoJs6 内运行文本或文件形式的 JavaScript, 列出与停止引擎, 读取最近的控制台输出.
- 无障碍界面: 以紧凑文本格式导出节点树, 用 AutoJs6 选择器语法查找节点, 点击, 长按, 滚动, 设置文本, 以及触发返回和主屏幕等全局按键.
- 截图分组 (P3.3): screen_capture 返回 MCP 图片, 支持裁剪, scale 或 maxWidth, JPEG / PNG / WebP 与质量参数. 默认 JPEG 质量 70, 最长边 1280 px. base64 超过 4 MiB 时降低质量或尺寸重试, 元数据说明调整情况. screen_state 返回亮屏状态, 尺寸, 方向和密度. 工具目录现有 37 项. MediaProjection 回退需要 2026-09-13 或之后构建的 AutoJs6 宿主及手机端授权, 宿主会话复用该授权.
- 工作目录工具 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 使用从 1 开始的行列号的 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast 和 shell_exec. 二进制读取使用 base64, 原始数据最多 1 MiB. 写入还受宿主请求预算约束 (通常为包含 JSON 转义的 96 KiB). 文件删除和 Shell 默认关闭; root 另需 allowShellRoot 开关与宿主 shell.root 授权. 这些能力需要匹配的 P3.4 宿主构建.
- MCP 资源 (P3.5) 提供只读工作目录文件, 可浏览的宿主示例, 安装 AutoJs6 离线文档插件后的离线文档, 设备信息和最近控制台输出, 遵守配对与分组开关. 文本和二进制读取报告截断状态. write_autojs6_script, automate_task 和 debug_selector 提示提供中英文指导, 其他手机语言回退英语.
- 连接方式: 通过 `adb forward` 的 USB 连接, 需显式开启的局域网连接, 以及面向无 HTTP 传输客户端的电脑端 stdio 桥接程序.
- 安全: 可轮换的 Bearer 令牌, 手机端首次配对确认, 按分组的工具开关; 服务器默认只监听回环接口.

******

### 工具清单

******

下表由插件的工具目录快照 (`app/src/test/resources/tool-catalog.snapshot.json`) 生成; 描述为客户端收到的英文原文, 每个分组都可在设置页关闭:

| 工具 | 分组 | 默认 | 描述 |
|---|---|---|---|
| `device_ping` | `device` | 开 | Confirms that the AutoJs6 MCP Server plugin is reachable and returns its version, the device model, the Android API level, and the device time. |
| `device_info` | `device` | 开 | Returns the device build, screen, battery, memory, AutoJs6 host version and process, accessibility service state, screen state, locale, and time zone as AutoJs6 reports them (schema autojs6-bridge-device-info-v1). No hardware identifiers. |
| `script_run` | `script` | 开 | Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API) and by default waits for it to finish. Use it for automation steps: toasts, UI actions, file work, app launches. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_run_file` | `script` | 开 | Runs a script file that already exists on the device (AutoJs6 picks the engine from the suffix) and by default waits for it to finish. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_stop` | `script` | 开 | Stops one running AutoJs6 script by the executionId that script_run, script_run_file, or script_list reported. |
| `script_stop_all` | `script` | 开 | Stops every script AutoJs6 is running, including ones started on the phone, and returns how many were stopped. |
| `script_list` | `script` | 开 | Lists the scripts AutoJs6 is running or starting, with executionId, name, path, working directory, state, and uptime. |
| `console_tail` | `script` | 开 | Returns the newest lines of the AutoJs6 console, which every script shares; optionally only entries after sinceId or at least a level. nextSinceId in the result continues from where this call ended. |
| `ui_dump` | `ui` | 开 | Dumps the accessibility node tree of the active window as compact text: one node per line with a #n reference, an indent per depth, the short class name, the state markers that apply (clickable, long_clickable, checkable, checked, scrollable, editable, focused, selected, !enabled, hidden), the text in quotes, desc=, id= (name part only), and the position (bounds [l,t][r,b] for a node with children, c=(x,y) for a leaf). Pass a #n reference as nodeRef to ui_click, ui_long_click, ui_set_text, or ui_scroll; references stay valid until the next ui_dump or for 60 s. Call it before acting and again after the screen changed. format json returns the nodes as objects with every flag; format xml returns the uiautomator-style export. |
| `ui_find` | `ui` | 开 | Finds the nodes of the active window that match every condition of the selector, optionally waiting up to timeoutMs for the first match, and returns up to limit of them with #n references, bounds, and center. An empty count is not an error; ui_explain_selector tells which condition fails. |
| `ui_current_window` | `ui` | 开 | Returns the package and activity in the foreground, whether the AutoJs6 accessibility service is available, and the accessibility windows with their type, title, bounds, and focus. |
| `ui_explain_selector` | `ui` | 开 | Explains why a selector matches or not: evaluates its conditions one by one over the active window and reports how many nodes pass each step cumulatively, the first failing condition, the matches, and the near misses. Use it when ui_find returns nothing. |
| `ui_wait_for` | `ui` | 开 | Waits until a node matching the selector appears (default) or disappears, polling the active window every 0.5 s for up to timeoutMs, and answers TIMEOUT when the state is not reached. Use it after an action that opens a screen or dismisses a dialog. |
| `ui_click` | `ui` | 开 | Clicks a node given by nodeRef (a #n reference from the last ui_dump), by selector (the first match in pre-order), or by x and y (a coordinate tap, allowed only while the ui_gesture group is enabled). The accessibility click climbs to the nearest clickable ancestor when the node itself is not clickable. Returns the node it acted on. Give nodeRef or selector, not both. |
| `ui_long_click` | `ui` | 开 | Long-presses a node given by nodeRef or selector (the accessibility long click climbs to the nearest node that accepts it), or by x and y as a 700 ms press at that point (allowed only while the ui_gesture group is enabled). Give nodeRef or selector, not both. |
| `ui_set_text` | `ui` | 开 | Sets the text of an editable node (an EditText, marked editable by ui_dump) given by nodeRef or selector; append adds to the current text instead of replacing it. Works without focus or the keyboard; ACTION_FAILED means the node is not editable or not enabled. Give nodeRef or selector, not both. |
| `ui_scroll` | `ui` | 开 | Scrolls a node given by nodeRef or selector, or the first scrollable node of the window when neither is given: forward, down, and right move towards the end, backward, up, and left towards the start; times repeats the step. performed counts the steps the node accepted, fewer than requested means it reached the end. Give nodeRef or selector, not both. |
| `ui_press_key` | `ui` | 开 | Presses a global key through the accessibility service: back, home, recents, notifications (opens the notification shade), quick_settings, power_dialog, or lock_screen (Android 9 or later). |
| `ui_swipe` | `ui_gesture` | 关 | Swipes one finger from (x1, y1) to (x2, y2) in device pixels over durationMs; take the coordinates from ui_dump bounds or a screenshot. Part of the ui_gesture group, which is off by default. |
| `ui_gesture` | `ui_gesture` | 关 | Performs a free-path one-finger gesture through the given points over durationMs (at most 10 s): the first point is the touch down, the last the lift. Part of the ui_gesture group, which is off by default. |
| `screen_capture` | `screen` | 开 | Capture the phone screen as an MCP image with dimensions, size, duration and capture source. Uses accessibility on Android 11+ and falls back to MediaProjection, which requires consent on the phone the first time. Defaults to JPEG quality 70 and a longest edge of 1280 pixels. Choose scale or maxWidth to override the size. Images above the 4 MiB base64 limit are retried at lower quality or smaller dimensions; metadata reports adjustments. At most 30 captures per minute per client; a RATE_LIMITED result names the wait in retryAfterMs. |
| `screen_state` | `screen` | 开 | Read whether the screen is on, its current width and height, orientation, rotation, and density. Does not request screen capture consent. |
| `files_list` | `files` | 开 | Lists workspace files with metadata. Results are bounded and report truncation. |
| `files_stat` | `files` | 开 | Returns existence, type, size, and modification time of a workspace path. |
| `files_read` | `files` | 开 | Reads up to 1 MiB. Use encoding base64 for binary data; encoding, bytes, totalBytes, and truncated identify the representation and limit. |
| `files_write` | `files` | 开 | Writes UTF-8 text and refreshes the host explorer. Content is limited to 1 MiB and the negotiated Binder request budget (normally 96 KiB including JSON escaping); oversized calls fail before writing. |
| `files_mkdir` | `files` | 开 | Creates a workspace directory and missing parents, then refreshes the host explorer. |
| `files_rename` | `files` | 开 | Moves a workspace file or directory to another workspace path and refreshes the host explorer. |
| `files_delete` | `files_delete` | 关 | Deletes a workspace entry. The separate files_delete group is off by default. The workspace root cannot be deleted. |
| `editor_open` | `files` | 开 | Opens a workspace file in the AutoJs6 editor at a one-based line and column. Lines outside the file are ignored by the editor. |
| `app_launch` | `device` | 开 | Opens an installed Android application. Provide exactly one of packageName or appName. |
| `app_list` | `device` | 开 | Lists up to 1000 Android applications visible to AutoJs6, optionally matching a package name or label. Android package visibility restrictions apply. |
| `clipboard_get` | `device` | 开 | Reads clipboard text (up to 64 KiB). Android may restrict clipboard access while AutoJs6 is in the background. |
| `clipboard_set` | `device` | 开 | Replaces clipboard text, including an empty string to clear it. |
| `device_ensure_accessibility` | `device` | 开 | Asks AutoJs6 to enable its accessibility service using its configured secure-settings, root, or Shizuku strategy. Waits up to 10 s for an operational service; failure includes manual activation guidance. |
| `toast` | `device` | 开 | Shows a short Android toast on the phone. |
| `shell_exec` | `shell` | 关 | Runs an Android shell command in the host workspace. The shell group is off by default; root also requires the separate allow root switch and a shell.root host grant. Reports exit code, timeout, stdout, stderr, and truncation. maxOutputBytes bounds stdout and stderr together. |

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5279 (6.8.0) 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) 下载并安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `MCP Server` 已被识别并启用. 官方发布包会自动通过签名校验.
3. 在 AutoJs6 抽屉中打开 MCP 服务器. 长按条目标题或点击插件中心的设置入口, 进入设置页并复制电脑客户端所需配置.
4. 在电脑上执行 `adb forward tcp:9637 tcp:9637`, 并让 MCP 客户端连接 `http://127.0.0.1:9637/mcp`, 以令牌作为 Bearer 凭据.
5. 首次连接时在手机上确认配对请求. 使用完毕后可从抽屉, 设置页或通知中停止服务.

> MCP 服务器抽屉开关提供安装, 激活, 启用, 授权与兼容性引导, 与通知停止操作同步, 重连时保留插件配置, 抽屉及插件中心通过权限校验打开同一设置页. AutoJs6 打开时恢复此前启用的服务器, 但尊重宿主离线期间的用户停止操作, 不随设备开机自启.

<p align="center">
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/drawer-zh.png?raw=true" alt="AutoJs6 抽屉中的 MCP Server 开关" width="300" />
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/settings-zh.png?raw=true" alt="MCP Server 设置页" width="300" />
</p>

******

### 客户端配置

******

Claude Code 只需一条命令即可注册服务器; 其他客户端在各自的 MCP 配置中使用相同的 URL 与请求头:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

在 AutoJs6 抽屉中打开 MCP 服务器. 长按条目标题或点击插件中心的设置入口, 进入设置页并复制电脑客户端所需配置. 首次连接时在手机上确认配对请求. 使用完毕后可从抽屉, 设置页或通知中停止服务.

******

### 连接路径

******

USB: `adb forward tcp:9637 tcp:9637` 将手机端口映射到 PC; 多台设备时加上 `-s <serial>` (通过 `adb devices` 查看), 模拟器同样适用. 任一侧端口被占用时, 在设置页修改端口并转发新端口. 连接卡片提供可直接复制的转发命令.

局域网: 在设置页开启 "允许局域网连接". 设置页随后列出手机当前地址 (随 Wi-Fi 变化刷新) 并提示客户端需处于同一网络; 访客网络, AP 隔离和 PC 防火墙是常见阻碍. 来自局域网的配对请求会被显著标注; 服务器持续可从网络访问期间每日通知提醒, 提醒可关闭. 在 Android 17 及以上, 在 AutoJs6 插件中心开启此插件前需允许访问附近的设备. 也可在此插件的设置页面中管理本地网络权限. 未获授权时插件保持关闭, 自动启动将静默跳过. 此权限属于插件自身, 与 AutoJs6 的授权相互独立.

两条路径使用相同的令牌和相同的手机侧配对. 没有 HTTP 传输的客户端使用 "接入" 中介绍的 stdio 桥接程序.

******

### 接入

******

设置页会为下列每个客户端复制带真实令牌的现成配置; 此处的片段以 `<token>` 作为占位. 所有客户端都通过带 Authorization 头的 Streamable HTTP 通信, 新客户端的首次调用需在手机上确认. 已实测: Claude Code, Codex CLI 与 MCP Inspector; 其余客户端使用相同的 URL 与头部, 但维护者尚未测试.

Claude Code: 执行 "客户端配置" 中的命令 (设置页复制的版本已带令牌); 随后 `claude mcp list` 会将 `autojs6` 显示为 Connected.

Cursor: 将下面的条目加入 `mcp.json`:

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

Codex CLI: 将令牌放入环境变量 `AUTOJS6_MCP_TOKEN` (设置页可复制对应的 PowerShell 命令), 再把服务器加入 `config.toml`, 或执行 `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN`:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: CLI 模式无需额外设置, Web 界面通过自带的 Node 代理访问手机. 仅当浏览器页面直接连接端点时, 才需在设置页开启开发者模式:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline, VS Code Copilot Chat, Gemini CLI 等客户端: 在各自的 MCP 配置中使用相同的 URL 与头部; 设置页提供带 `"type": "http"` 的通用 JSON 片段.

Claude Desktop 及其他仅支持 stdio 的客户端: 用 `npm install -g autojs6-mcp-bridge` 安装桥接程序, 将 `autojs6-mcp-bridge --serial <serial>` 注册为 stdio 服务器并把 `AUTOJS6_MCP_TOKEN` 放入其环境变量块 (Claude Desktop 与 Claude Code 的片段见[桥接程序 README](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge)). 桥接程序 0.1.0 对应插件 1.0.0, 透传客户端的协议版本; 已用 Claude Code 2.1.257 经 stdio 验证.

******

### 常见问题

******

- 401 Unauthorized: 令牌缺失, 输错或已轮换. 从设置页重新复制配置; 轮换令牌后每个客户端都需要新值.
- 配对超时: 新客户端的首次调用会等待约一分钟, 直到手机上点击允许. 解锁手机, 接受对话框或通知动作, 然后重复调用. 拒绝会进入短暂冷却, 之后下一次调用会再次询问.
- HOST_UNAVAILABLE: AutoJs6 未运行或插件会话已关闭. 打开 AutoJs6, 保持抽屉开关开启, 并在设置页查看连接状态.
- 无障碍未启用: `ui_*` 工具与截屏需要 AutoJs6 无障碍服务. 调用 `device_ensure_accessibility`, 或在系统无障碍设置中启用该服务.
- 端口占用: 抽屉会报告 `port_in_use`. 在设置页修改端口, 并用 adb 转发新端口.
- 局域网不可达: 开启局域网访问, 使用设置页列出的地址, 让 PC 与手机处于同一网络且无访客隔离, 并在 PC 防火墙放行该端口. 手机 Wi-Fi 省电会使每次调用增加几百毫秒.
- 息屏后服务器消失: 限制了本应用电池用量的手机 (HyperOS 与 MIUI 默认对侧载应用如此) 会在使用电池息屏约一分钟后停止前台服务. 此时设置页显示警告和 "电池设置" 按钮, 请在其中为 MCP Server 选择 "无限制". 可选的 "空闲时自动停止" (默认关闭) 也会在所选分钟数内无请求时停止服务器, 并留下一条说明通知.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口与设置页受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能够访问; 配对对话框, 其接收器与发行历史页均未导出. 只有承载监听器的前台服务接受 adb (`android.permission.DUMP`), 作为开发者的启停开关.
- INTERNET 权限仅用于插件自身的 HTTP 监听器; 插件不发起任何出站请求, 也不收集数据. 通过网络安全配置, 明文 HTTP 仅允许指向回环地址.
- 服务器默认只监听 127.0.0.1. 局域网访问保持关闭直到你手动开启; 开启后令牌, 配对确认, Host 允许列表与速率限制在局域网上仍然生效, 并有每日一次的通知提醒.
- 在 Android 17 及以上, 在 AutoJs6 插件中心开启此插件前需允许访问附近的设备. 也可在此插件的设置页面中管理本地网络权限. 未获授权时插件保持关闭, 自动启动将静默跳过. 此权限属于插件自身, 与 AutoJs6 的授权相互独立.
- 访问令牌来自安全随机源, 以 Android Keystore 中的 AES-GCM 密钥封装后保存在插件私有且永不备份的存储中; 备份与设备迁移均已禁用. 设置页只显示令牌末 4 位, 完整令牌对话框禁止截屏, 复制到剪贴板时标记为敏感内容.
- 日志从不包含令牌, 请求正文, 文件内容或截图; 插件只记录工具名, 客户端名与令牌指纹. 已在两台设备上于真实的文件与截图调用期间用 logcat 验证 (docs/dev/p6-security-audit.md).
- 工具调用经由 AutoJs6 的能力代理执行, 永远不会超出宿主自身的权限范围; Shell 命令, 文件删除与手势保持关闭直到你启用对应分组, root Shell 还需要单独的开关与宿主授权.
- 可在设置页逐个或一次性撤销配对; 被撤销的客户端在下次工具调用前必须重新在手机上确认. 轮换令牌会保留配对, 但仍使用旧令牌的客户端会立即失效.

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

`McpServerPluginService` 在 `:mcp_server` 进程中实现宿主 mcp-server-api 契约 `org.autojs.plugin.mcp.server.api.IMcpServerPlugin`, 响应 `org.autojs.plugin.MCP_SERVER` (category `mcp-server`). `McpServerPluginInfoService` 以 PluginInfo 响应 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主激活插件.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.2

_2026/09/17_

- `修复` 独立设置页的按钮, 开关轨道与滑块, 输入框, 单选项和弹窗按钮完整跟随宿主主题色, 并改善浅色与深色模式下的对比度
- `优化` Android 17 本地网络授权统一移至插件中心启用流程和插件设置, 不再提供启动器授权页面; 未获授权时保持关闭并静默跳过自动启动
- `优化` 适配 Android 17 (SDK 37), 提供插件独立的本地网络权限控制及恢复引导

#### v1.0.1

_2026/09/16_

- `新增` 可选的离线文档资源: 安装 AutoJs6 离线文档插件且宿主通过 app.listDocs / app.readDoc 转读时, resources/list 增加 autojs6://docs/ (带子 URI 的索引) 与每个文档页面对应的 autojs6://docs/{+path} 资源, resources/templates/list 增加 docs 模板; 未安装插件或宿主不含这两个方法时不列出任何条目, 并由 _meta.docsCatalogStatus 说明原因
- `优化` 将 compileSdk 提升到 37 (Android 17), targetSdk 保持 36, 待依赖目标版本的行为验证后再提升
- `优化` MCP 一致性 (P6): 在两台设备上以官方 @modelcontextprotocol/conformance 套件 0.1.16 测试有状态的 /mcp 路径. 32 个服务器场景中 9 个通过 (initialize, ping, tools/list, 文本与错误工具结果, resources/list, prompts/list, 并发 SSE 流, DNS rebinding 保护); 18 个调用套件自带的参考夹具 (test_* 工具, 提示与 test:// 资源, 本服务器以未知工具结果, -32602 或 isError 应答), 5 个依赖本服务器未声明的能力 (logging, completions, 资源订阅). 回环 Origin 头现在在任何模式下都被接受 (套件如此要求); CORS 头与预检应答仍仅限开发者模式. 2026-07-28 无状态模型没有路由 (Roadmap D9). 详见 docs/dev/p6-conformance.md.
- `优化` 安全审计 (P6): 七项清单 (令牌存储, 日志脱敏, 导出组件, 明文范围, 局域网默认关闭, 配对撤销, 工具分组默认关闭) 已在代码与两台设备上逐项核对, 记录于 docs/dev/p6-security-audit.md, README 安全章节改为说明这些边界. 明文 HTTP 由网络安全配置限定为回环地址, 取代应用级 usesCleartextTraffic 标志; 插件不发起客户端连接, 监听器也不需要该标志.
- `优化` 性能基线 (P6): 在 API 24 模拟器, Sony 手机 (API 33) 与小米平板 (API 35) 上测量 ui_dump 50 / 200 / 400 节点, screen_capture 三档尺寸, script_run 往返与 4 路并发请求, 记录于 docs/dev/p6-performance-baseline.md, 仅作参考不设阈值. 仅由插件应答的调用在模拟器与手机上约 20 ms, ui_dump 每节点约增加 0.05 ms, 无障碍截图路径 100 ms 内应答而 API 24 的 MediaProjection 路径每次约 1.35 s, 4 路并发请求在宿主 4 路并发上限内以单次往返的 1.0-1.8 倍完成.

#### v1.0.0

_2026/09/16_

- `提示` P4 开发预览: 37 个工具, 默认启用 33 个, 提供 AutoJs6 抽屉开关和插件设置页. 需要配套的 P4 AutoJs6 构建. ROADMAP.md.
- `新增` 手机设置页提供服务状态, USB 转发, 端口和局域网访问, 令牌显示/复制/轮换, 配对撤销, 工具分组和 root 权限, 开发者模式, 可复制的 Claude Code / Cursor / Codex / 通用 HTTP 配置及发行历史, 并跟随 AutoJs6 外观. 网络设置会重启运行中的监听器, 令牌和权限变更立即生效. 敏感信息窗口禁止截图.
- `新增` MCP 资源 (P3.5) 提供只读工作目录文件, 可浏览的宿主示例, 设备信息和最近控制台输出, 遵守配对与分组开关. 文本和二进制读取报告截断状态. write_autojs6_script, automate_task 和 debug_selector 提示提供中英文指导, 其他手机语言回退英语.
- `新增` 工作目录工具 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 使用从 1 开始的行列号的 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast 和 shell_exec. 二进制读取使用 base64, 原始数据最多 1 MiB. 写入还受宿主请求预算约束 (通常为包含 JSON 转义的 96 KiB). 文件删除和 Shell 默认关闭; root 另需 allowShellRoot 开关与宿主 shell.root 授权. 这些能力需要匹配的 P3.4 宿主构建.
- `新增` 插件标识 `mcp-server`, 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.MCP_SERVER` 服务骨架
- `新增` 10 种语言的 README, 插件中心说明与更新日志
- `新增` 位于 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端点及 `device_ping` 工具, 由可经 adb 或宿主启停的前台服务承载 (开发预览)
- `新增` `/mcp` 端点的传输加固: 绑定地址与端口来自服务器配置存储, 请求体上限 1 MiB, 空闲连接 60 秒后关闭, 端口被占用或绑定被拒绝时以 `port_in_use` / `bind_failed` 状态附带提示结束而非崩溃
- `新增` SDK 传输前置的 DNS rebinding 保护: 回环模式只接受 `localhost` / `127.0.0.1` / `[::1]` 作为 `Host`, 局域网模式加入设备当前 IPv4 地址与可选的额外主机名并在网络变化时刷新; 浏览器来源一律拒绝, 仅 "开发者模式" 开关经 CORS 放行 Inspector 的回环来源
- `新增` 服务器标识 `autojs6-mcp-server` 携带插件版本, 并声明 tools (`listChanged`), resources 与 prompts 能力; `tools/list` 保持注册顺序以便客户端缓存
- `新增` 每个 `/mcp` 请求的 Bearer 令牌鉴权: 首次启动时生成 32 字节令牌, 以 Android Keystore 的 AES-GCM 密钥包裹后存放在插件私有且不参与备份的存储中; `Authorization` 头缺失或错误时经常量时间比较后以 `401` + `WWW-Authenticate: Bearer` 与 JSON-RPC `-32001` 错误拒绝; 令牌不写入日志
- `新增` 传输前置的首次配对: 未配对客户端可以 `initialize` 并列出 tools, resources 与 prompts, 但首次 `tools/call`, `resources/read`, `resources/subscribe` 或 `prompts/get` 返回 `PAIRING_REQUIRED` (`-32002`), 直到 60 秒内在手机上确认; 拒绝或超时后 30 秒内返回 `PAIRING_DENIED` (`-32003`); 客户端按 `clientInfo` 名称 (缺失时用 `User-Agent`) 加地址类别 (回环 / 局域网) 识别, 因此令牌轮换不影响已有配对, 最多可配对 32 个客户端
- `新增` 手机上的配对确认走双通道: 带允许 / 拒绝动作的高优先级通知, 以及屏幕解锁时弹出的对话框; 服务器配置, 令牌与已配对客户端保存在原子替换的文件中, 服务器进程与设置页共享且不会读到过期缓存
- `新增` 带分组开关的工具目录 (决策 D6): `device_ping` (插件本地), `device_info` (AutoJs6 `device.info`) 与 `script_run` (AutoJs6 `engines.execScript`: 运行 JavaScript, 最多等待 `timeoutMs` 直到脚本结束, 返回结果与最新的控制台行, 运行期间发送进度通知); 每个工具声明封闭的 JSON Schema (`additionalProperties: false`), 参数在到达 AutoJs6 之前完成校验; 分组开关 `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` 存放于 `tool_groups.json`, 关闭的分组自下一次请求起从 `tools/list` 消失, 其工具回答 `TOOL_DISABLED`
- `新增` 宿主桥接: `org.autojs.plugin.MCP_SERVER` 服务实现真实的 `IMcpServerPlugin` Binder (`getInfo` / `getCapabilities` 报告契约版本 1, 工具分组, MCP 协议版本与 SDK 版本; `openServer` 只接受已安装且同签名的 AutoJs6, 返回带 `getStatus` / `updateConfig` / `stop` / `close` 的 `IMcpServerSession`); 工具调用经宿主能力代理传递, 带单调递增的请求 id, 每次调用的超时, 4 路并发上限, 宿主错误类别映射为 `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; AutoJs6 退出时监听器继续运行, 依赖宿主的工具回答 `HOST_UNAVAILABLE` 直到宿主重新连接; 状态与事件 (`pairing_requested`, `client_paired`, `tool_call`, `warning`) 经回调送达宿主
- `新增` 前台服务通知显示端点, AutoJs6 连接状态与已配对客户端数, 并提供停止动作; 通知被禁用时以 toast 提示端点; `dumpsys activity service` 额外打印宿主会话, 工具分组开关与已注册工具
- `新增` 补全脚本分组: `script_run_file` 运行设备上的脚本文件, `script_stop` / `script_stop_all` 停止一个或全部 AutoJs6 执行, `script_list` 列出运行中的执行, `console_tail` 返回最新的控制台行并带 `nextSinceId` 游标与级别过滤; `script_run` 与 `script_run_file` 现在返回 `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, 含行号的异常与最新的控制台行, 等待期间每 2 s 发送一条携带最新控制台行的进度通知
- `新增` MCP 端点的响应改为以服务器发送事件 (SSE) 流式返回 (不再使用 SDK 的 JSON 响应模式), 属于某个请求的通知 (如运行中脚本的进度心跳) 会随该请求自身的响应送达客户端
- `新增` 新增 UI 分组 (roadmap P3.2): `ui_dump` 以带 `#n` 引用的紧凑节点树返回当前窗口 (`format` 为 text / json / xml, `maxNodes` 最多 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` 轮询选择器, `ui_current_window` 与 `ui_explain_selector` 报告窗口与选择器失败的原因, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` 作用于 `nodeRef` (按指纹重定位, 节点消失时返回 `NODE_REF_STALE`) 或 `selector`, `ui_press_key` 按下 back / home / recents / notifications / quick_settings / power_dialog / lock_screen, 默认关闭的 `ui_gesture` 分组增加 `ui_swipe`, `ui_gesture` 与点击工具的坐标形式 (分组关闭时返回 `TOOL_DISABLED`); 工具目录快照增至 20 个工具; 坐标手势需要 2026-09-11 或之后构建的 AutoJs6 宿主 (更早的宿主会随机以 "the system cancelled ..." 应答)
- `新增` 截图分组 (P3.3): screen_capture 返回 MCP 图片, 支持裁剪, scale 或 maxWidth, JPEG / PNG / WebP 与质量参数. 默认 JPEG 质量 70, 最长边 1280 px. base64 超过 4 MiB 时降低质量或尺寸重试, 元数据说明调整情况. screen_state 返回亮屏状态, 尺寸, 方向和密度. 工具目录现有 22 项. MediaProjection 回退需要 2026-09-13 或之后构建的 AutoJs6 宿主及手机端授权, 宿主会话复用该授权.
- `新增` 局域网路径 (P5.1): 开启局域网访问后, 设置页列出手机当前地址 (Wi-Fi 变化时刷新) 并提示同网段与防火墙; 来自局域网的配对请求在对话框和通知中显著标注; 每日提醒服务器仍可从局域网访问, 可关闭且不重启监听. README 记录 USB 与局域网路径.
- `新增` 每客户端速率限制 (P6): 每客户端每秒最多 20 个请求, 每分钟最多 30 次 screen_capture. 超限请求返回 HTTP 429, 带 Retry-After 头和含 retryAfterMs 的 JSON-RPC RATE_LIMITED 错误; 超限截图以带 retryAfterMs 的 RATE_LIMITED 工具结果应答, 便于模型等待. 宿主 grant 的无障碍查询速率在此之上仍然生效.
- `新增` 空闲自动停止与电量 (P6): 设置页新增 "空闲时自动停止" (默认关闭; 5 / 15 / 30 / 60 / 120 分钟). 监听器在所选时间内没有客户端请求时自行停止, 记录插件内部的 idle_timeout 原因, 留下一条可自动消失的通知, 且不算作用户停止; 处理中的请求 (正在运行的工具调用) 不算空闲, 而仅保持连接却不发请求的客户端不会让服务器继续运行. 当 Android 限制了本应用的电池用量 (HyperOS 与 MIUI 默认对侧载应用如此, 息屏并使用电池约一分钟后会停止前台服务) 时, 设置页显示警告和 "电池设置" 按钮, AutoJs6 收到一条 warning 事件. 空闲 CPU 时间与一小时空闲的 batterystats 估算记录于 docs/dev/p6-battery-and-residency.md.
- `修复` IDE rebuild 不再为 JVM 单元测试查找 APK. APK 校验任务会自动组装所需产物, 可直接从 clean 后执行.
- `修复` 插件中心亮暗模式下图标比例不一致及自适应图标留白不足的问题; 夜间同样使用自适应图标, 调整图层尺寸以保留 ic_launcher_round.png 的完整图形和留白, 仅切换背景色
- `修复` 设置页的键盘 Tab 导航会跳过工具栏返回按钮; 现在 Tab 循环覆盖返回按钮和全部控件 (含 Android 7). 设备测试检查读屏标签与键盘操作.
- `修复` script_run 与 script_run_file 的 arguments 映射以 JSON Schema 数组声明值类型, 部分 MCP 客户端会拒绝或弱化; 现改为单类型 anyOf 分支. README 新增 "接入" 与 "常见问题" 章节及实测客户端矩阵.
- `修复` 敌意请求体在任何解析器递归之前就被拒绝: 嵌套超过 64 层的 JSON, 重复请求 id 的请求体, 以及同一会话上仍在途的请求 id (原本会让先到的请求得不到应答) 都返回 400 与 JSON-RPC 错误; 没有有效会话的 GET 流返回 400 / 404 而非空事件流. JVM 测试与设备测试 (API 28 / 31 / 33 / 35) 覆盖超长, 超深, 非法 UTF-8, 未知方法, 头部组合, 超大 base64 与 64 并发会话.
- `修复` 生命周期矩阵 (P6): 带有上一个监听进程会话 id 的请求 (监听进程被杀, 或因令牌轮换而重启之后) 交由传输层回 404, 让客户端重新 initialize, 而不再以请求的 User-Agent 为已配对的客户端发起多余的配对提示; 监听进程死亡后残留在通知栏的配对通知在下一个监听器启动时清除. 宿主被杀, 监听进程被杀, 两者同时被杀, 在系统设置中强制停止, 活动会话中的令牌轮换, 以及跨越宿主被杀与监听进程被杀的待确认配对, 各自的期望状态与恢复路径记录于 docs/dev/lifecycle-matrix.md 并在真机上验证.
- `优化` 构建阶段阻止意外引入原生依赖, 并输出 JSON 校验报告
- `依赖` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 与 Ktor 3.5.1 CIO 引擎
- `依赖` 附加 Ktor 3.5.1 `ktor-server-test-host` 用于 JVM 传输测试 (仅测试范围)
- `依赖` 附加 `mcp-server-api.aar` (AutoJs6 模块 `plugin-api/mcp-server-api`, 宿主构建 6.8.0 / 5279, MPL 2.0) 作为 AutoJs6 与插件之间的 Binder 契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
- `依赖` 更新来自 P4 宿主构建的 common-plugin-api 和 mcp-server-api 配套 AAR: 可选设置扩展 v1, AIDL transaction 顺序不变, SHA-256 锁定, 保留 SDK 36 消费兼容性.

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

构建需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 与插件版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 统一管理.

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


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
