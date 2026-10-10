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

版本 1.0.3: 37 个工具 (默认启用 33 个), MCP 资源与提示, AutoJs6 抽屉开关和插件设置页. 需要 AutoJs6 6.8.0 (构建 5279) 或更高版本; 可选的 autojs6://docs/ 资源还需要 AutoJs6 离线文档插件以及带转读方法的宿主. 进度与证据记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

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

Claude Desktop 及其他仅支持 stdio 的客户端: 用 `npm install -g autojs6-mcp-bridge` 安装桥接程序, 将 `autojs6-mcp-bridge --serial <serial>` 注册为 stdio 服务器并把 `AUTOJS6_MCP_TOKEN` 放入其环境变量块 (Claude Desktop 与 Claude Code 的片段见[桥接程序 README](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge)). 桥接程序 0.1.0 适用于插件 1.0.1 及以上 (发布前以 1.0.0 开发版验证, 此后 MCP 传输与鉴权未变), 透传客户端的协议版本; 已用 Claude Code 2.1.257 经 stdio 验证.

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

#### v1.0.3

_2026/10/05_

- `优化` 插件中心图标采用统一工作台调整后的尺寸, 位置, 亮暗图稿与圆形底色, 保留可重建原稿和参数
- `优化` Android 系统应用信息图标与图标工作台共用图稿和亮暗底色, 保留插件中心透明图稿及现有启动器选项

#### v1.0.2

_2026/09/19_

- `修复` 独立设置页部分控件未跟随宿主主题色及亮暗模式下对比度不足的问题
- `修复` AGP 9.1 构建时的 SDK XML v4 解析警告, 以及 JVM 单元测试误触发 APK 原生库对齐检查的问题 (共享构建插件 1.8.3)
- `优化` Android 17 本地网络授权移至插件中心启用流程及插件设置, 移除启动器授权页面 (未授权时保持关闭并跳过自动启动)
- `优化` 适配 Android 17 (SDK 37), 提供插件独立的本地网络权限控制及恢复引导

#### v1.0.1

_2026/09/16_

- `新增` MCP 资源支持浏览及读取 AutoJs6 离线文档, 提供文档索引与页面资源 (需要安装 Offline Docs 插件, 且宿主支持 app.listDocs/app.readDoc)
- `优化` compileSdk 升级至 37 (Android 17), targetSdk 保持 36
- `优化` 有状态 MCP 端点接受回环 Origin, CORS 及预检仍仅在开发者模式下开放 (一致性验证范围参阅 docs/dev/p6-conformance.md)
- `优化` 明文 HTTP 的客户端网络配置收窄至回环地址, 并补充令牌, 配对及工具权限说明 (参阅 docs/dev/p6-security-audit.md)
- `优化` 补充 UI 查询, 截图, 脚本执行及并发请求的性能基线, 区分无障碍截图与 MediaProjection 路径 (参阅 docs/dev/p6-performance-baseline.md, 结果仅供参考)

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
