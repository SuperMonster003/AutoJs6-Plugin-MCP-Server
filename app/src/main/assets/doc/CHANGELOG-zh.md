******

### 发行历史

******

# v1.0.0

###### 2026/09/13

* `提示` P4 开发预览: 37 个工具, 默认启用 33 个, 提供 AutoJs6 抽屉开关和插件设置页. 需要配套的 P4 AutoJs6 构建. ROADMAP.md.
* `新增` 手机设置页提供服务状态, USB 转发, 端口和局域网访问, 令牌显示/复制/轮换, 配对撤销, 工具分组和 root 权限, 开发者模式, 可复制的 Claude Code / Cursor / Codex / 通用 HTTP 配置及发行历史, 并跟随 AutoJs6 外观. 网络设置会重启运行中的监听器, 令牌和权限变更立即生效. 敏感信息窗口禁止截图.
* `新增` MCP 资源 (P3.5) 提供只读工作目录文件, 可浏览的宿主示例, 设备信息和最近控制台输出, 遵守配对与分组开关. 文本和二进制读取报告截断状态. write_autojs6_script, automate_task 和 debug_selector 提示提供中英文指导, 其他手机语言回退英语.
* `新增` 工作目录工具 (P3.4): files_list / stat / read / write / mkdir / rename / delete, 使用从 1 开始的行列号的 editor_open, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast 和 shell_exec. 二进制读取使用 base64, 原始数据最多 1 MiB. 写入还受宿主请求预算约束 (通常为包含 JSON 转义的 96 KiB). 文件删除和 Shell 默认关闭; root 另需 allowShellRoot 开关与宿主 shell.root 授权. 这些能力需要匹配的 P3.4 宿主构建.
* `新增` 插件标识 `mcp-server`, 含 INFO 服务, Wake Activity 以及供宿主发现的 `org.autojs.plugin.MCP_SERVER` 服务骨架
* `新增` 10 种语言的 README, 插件中心说明与更新日志
* `新增` 位于 `http://127.0.0.1:9637/mcp` 的 Streamable HTTP 端点及 `device_ping` 工具, 由可经 adb 或宿主启停的前台服务承载 (开发预览)
* `新增` `/mcp` 端点的传输加固: 绑定地址与端口来自服务器配置存储, 请求体上限 1 MiB, 空闲连接 60 秒后关闭, 端口被占用或绑定被拒绝时以 `port_in_use` / `bind_failed` 状态附带提示结束而非崩溃
* `新增` SDK 传输前置的 DNS rebinding 保护: 回环模式只接受 `localhost` / `127.0.0.1` / `[::1]` 作为 `Host`, 局域网模式加入设备当前 IPv4 地址与可选的额外主机名并在网络变化时刷新; 浏览器来源一律拒绝, 仅 "开发者模式" 开关经 CORS 放行 Inspector 的回环来源
* `新增` 服务器标识 `autojs6-mcp-server` 携带插件版本, 并声明 tools (`listChanged`), resources 与 prompts 能力; `tools/list` 保持注册顺序以便客户端缓存
* `新增` 每个 `/mcp` 请求的 Bearer 令牌鉴权: 首次启动时生成 32 字节令牌, 以 Android Keystore 的 AES-GCM 密钥包裹后存放在插件私有且不参与备份的存储中; `Authorization` 头缺失或错误时经常量时间比较后以 `401` + `WWW-Authenticate: Bearer` 与 JSON-RPC `-32001` 错误拒绝; 令牌不写入日志
* `新增` 传输前置的首次配对: 未配对客户端可以 `initialize` 并列出 tools, resources 与 prompts, 但首次 `tools/call`, `resources/read`, `resources/subscribe` 或 `prompts/get` 返回 `PAIRING_REQUIRED` (`-32002`), 直到 60 秒内在手机上确认; 拒绝或超时后 30 秒内返回 `PAIRING_DENIED` (`-32003`); 客户端按 `clientInfo` 名称 (缺失时用 `User-Agent`) 加地址类别 (回环 / 局域网) 识别, 因此令牌轮换不影响已有配对, 最多可配对 32 个客户端
* `新增` 手机上的配对确认走双通道: 带允许 / 拒绝动作的高优先级通知, 以及屏幕解锁时弹出的对话框; 服务器配置, 令牌与已配对客户端保存在原子替换的文件中, 服务器进程与设置页共享且不会读到过期缓存
* `新增` 带分组开关的工具目录 (决策 D6): `device_ping` (插件本地), `device_info` (AutoJs6 `device.info`) 与 `script_run` (AutoJs6 `engines.execScript`: 运行 JavaScript, 最多等待 `timeoutMs` 直到脚本结束, 返回结果与最新的控制台行, 运行期间发送进度通知); 每个工具声明封闭的 JSON Schema (`additionalProperties: false`), 参数在到达 AutoJs6 之前完成校验; 分组开关 `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` 存放于 `tool_groups.json`, 关闭的分组自下一次请求起从 `tools/list` 消失, 其工具回答 `TOOL_DISABLED`
* `新增` 宿主桥接: `org.autojs.plugin.MCP_SERVER` 服务实现真实的 `IMcpServerPlugin` Binder (`getInfo` / `getCapabilities` 报告契约版本 1, 工具分组, MCP 协议版本与 SDK 版本; `openServer` 只接受已安装且同签名的 AutoJs6, 返回带 `getStatus` / `updateConfig` / `stop` / `close` 的 `IMcpServerSession`); 工具调用经宿主能力代理传递, 带单调递增的请求 id, 每次调用的超时, 4 路并发上限, 宿主错误类别映射为 `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; AutoJs6 退出时监听器继续运行, 依赖宿主的工具回答 `HOST_UNAVAILABLE` 直到宿主重新连接; 状态与事件 (`pairing_requested`, `client_paired`, `tool_call`, `warning`) 经回调送达宿主
* `新增` 前台服务通知显示端点, AutoJs6 连接状态与已配对客户端数, 并提供停止动作; 通知被禁用时以 toast 提示端点; `dumpsys activity service` 额外打印宿主会话, 工具分组开关与已注册工具
* `新增` 补全脚本分组: `script_run_file` 运行设备上的脚本文件, `script_stop` / `script_stop_all` 停止一个或全部 AutoJs6 执行, `script_list` 列出运行中的执行, `console_tail` 返回最新的控制台行并带 `nextSinceId` 游标与级别过滤; `script_run` 与 `script_run_file` 现在返回 `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, 含行号的异常与最新的控制台行, 等待期间每 2 s 发送一条携带最新控制台行的进度通知
* `新增` MCP 端点的响应改为以服务器发送事件 (SSE) 流式返回 (不再使用 SDK 的 JSON 响应模式), 属于某个请求的通知 (如运行中脚本的进度心跳) 会随该请求自身的响应送达客户端
* `新增` 新增 UI 分组 (roadmap P3.2): `ui_dump` 以带 `#n` 引用的紧凑节点树返回当前窗口 (`format` 为 text / json / xml, `maxNodes` 最多 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` 轮询选择器, `ui_current_window` 与 `ui_explain_selector` 报告窗口与选择器失败的原因, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` 作用于 `nodeRef` (按指纹重定位, 节点消失时返回 `NODE_REF_STALE`) 或 `selector`, `ui_press_key` 按下 back / home / recents / notifications / quick_settings / power_dialog / lock_screen, 默认关闭的 `ui_gesture` 分组增加 `ui_swipe`, `ui_gesture` 与点击工具的坐标形式 (分组关闭时返回 `TOOL_DISABLED`); 工具目录快照增至 20 个工具; 坐标手势需要 2026-09-11 或之后构建的 AutoJs6 宿主 (更早的宿主会随机以 "the system cancelled ..." 应答)
* `新增` 截图分组 (P3.3): screen_capture 返回 MCP 图片, 支持裁剪, scale 或 maxWidth, JPEG / PNG / WebP 与质量参数. 默认 JPEG 质量 70, 最长边 1280 px. base64 超过 4 MiB 时降低质量或尺寸重试, 元数据说明调整情况. screen_state 返回亮屏状态, 尺寸, 方向和密度. 工具目录现有 22 项. MediaProjection 回退需要 2026-09-13 或之后构建的 AutoJs6 宿主及手机端授权, 宿主会话复用该授权.
* `修复` IDE rebuild 不再为 JVM 单元测试查找 APK. APK 校验任务会自动组装所需产物, 可直接从 clean 后执行.
* `修复` 插件中心亮暗模式下图标比例不一致及自适应图标留白不足的问题; 夜间同样使用自适应图标, 调整图层尺寸以保留 ic_launcher_round.png 的完整图形和留白, 仅切换背景色
* `优化` 构建阶段阻止意外引入原生依赖, 并输出 JSON 校验报告
* `依赖` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) 与 Ktor 3.5.1 CIO 引擎
* `依赖` 附加 Ktor 3.5.1 `ktor-server-test-host` 用于 JVM 传输测试 (仅测试范围)
* `依赖` 附加 `mcp-server-api.aar` (AutoJs6 模块 `plugin-api/mcp-server-api`, 宿主构建 6.8.0 / 5279, MPL 2.0) 作为 AutoJs6 与插件之间的 Binder 契约, 并在 `locks/host-api-aars.lock` 中锁定哈希
* `依赖` 更新来自 P4 宿主构建的 common-plugin-api 和 mcp-server-api 配套 AAR: 可选设置扩展 v1, AIDL transaction 顺序不变, SHA-256 锁定, 保留 SDK 36 消费兼容性.
