# AutoJs6 MCP Server 插件 Roadmap

本文是 `AutoJs6-Plugin-MCP-Server` (手机作为 MCP 服务器, 供 PC 端 AI 代理控制设备) 的可执行状态表.
以 2026-09-07 的宿主本地代码快照 (`AutoJs6 master@5166d088b`, `VERSION_NAME=6.8.0`, `VERSION_BUILD=5278`),
MCP 规范修订版 `2026-07-28`, 官方 Kotlin SDK `0.15.0` (Ktor `3.5.1`), 平台版本插件 `1.7.4` 为起点,
每个条目均可独立 Check 并落地, 后续会话按阶段逐步推进.

使用方式:

1. 每次会话开始时, 从 "阶段总览" 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号 / 客户端名称与版本), 证据等级见附录 D.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(PC)` PC 端桥接程序, `(测试)`, `(文档)`, `(发布)`.
4. 涉及宿主公开契约或脚本 API 的条目, 完成后必须同步宿主 `docs/dev/`, 宿主 `.changelog` (10 语言) 与本仓库 `.changelog`.
5. 附录 C 的 "待决事项" 在进入对应阶段前由维护者拍板, 拍板结果回填到 "固定决策".
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 在 P0 落地时按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 并将该文件复制为本仓库 `AGENTS.md` 后裁剪.

---

## 1. 固定决策

以下决策已由维护者确认 (D1-D8 于 2026-09-07, 附录 C 的 Q1-Q7 同日拍板并回填为 D10 / D15 / D16 / D19-D22), 后续阶段不再重新讨论; D9-D18 为据此派生的技术决策, 其中未被 Q1-Q7 覆盖的部分进入 P0 前可推翻, 之后视同固定.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | Server 先行, Client 预留 | 本 Roadmap 只交付 MCP Server 插件. MCP Client (脚本调用外部 MCP 服务器的工具) 作为第二个关联插件 `AutoJs6-Plugin-MCP-Client` 在附录 E 预留仓库名, 契约边界与接入点, 不排期. |
| D2 | 插件进程运行监听, 宿主下发能力代理 | HTTP 监听运行在插件自己的进程 (`:mcp_server`, 前台服务). 宿主像对待 Node / Python 插件一样发起绑定, 通过 AIDL 把 "宿主能力代理" Binder 交给插件; 插件把 MCP 工具映射到宿主 `NodeBridgeProtocol` 的模块 / 方法上, 不在插件内重新实现设备能力. 插件永远拿不到宿主 `Context`. |
| D3 | 第一期工具面为四组 | 脚本执行与日志, 无障碍 UI 观察与操作, 截图, 文件 / 应用 / 设备. 工具清单与参数草案见附录 A. |
| D4 | 协议实现选官方 Kotlin SDK | `io.modelcontextprotocol:kotlin-sdk-server` 0.15.0 + Ktor 3.5.1 `CIO` 引擎. P0 先做 Android `minSdk 24` 可行性 spike; 不可行则退回自研最小实现 (退路见附录 D.2), 由 P0 末尾的决策点决定, 不拖到 P2. **P0.2 结论 (2026-09-08)**: 成立. Kotlin 2.3.20 直接编译 SDK 0.15.0 (kotlin-stdlib 2.4.0 元数据), API 24 / 28 / 35 运行正常, release 仅需两条 `-dontwarn java.lang.management.*`, APK 增量 0.90 MiB; 证据见 `docs/dev/p0-spike-evidence.md`. |
| D5 | 四条连接路径全部纳入 | USB (`adb forward` 到 `127.0.0.1`, 默认) 与局域网 Wi-Fi 直连 (显式开启) 在 P5 交付; PC 端 stdio 桥接程序为 P5 后段; 公网隧道 + OAuth 2.1 为可选的 P8, 不阻塞 1.0.0 发布. |
| D6 | 令牌 + 首次配对确认 + 工具组开关 | 自动生成可轮换的 Bearer 令牌; 持有效令牌的新客户端首次 `tools/call` 前必须在手机上确认配对 (元数据类请求如 `tools/list` 不需要); 已配对客户端持久化并可撤销; 每个工具组可单独启用 / 禁用; `shell`, 文件删除, 坐标手势 (`ui_swipe` / `ui_gesture`) 默认关闭; 局域网监听默认关闭. |
| D7 | 宿主抽屉开关 + 插件设置页 | 宿主抽屉新增 "MCP 服务器" 开关 (与 "客户端模式 / 服务端模式" 同形; 未安装时引导安装, 未激活时引导激活); 插件自带设置 Activity 承载端口 / 监听范围 / 令牌 / 已配对客户端 / 工具组开关 / 客户端配置片段 / 发行历史; 宿主插件中心与抽屉均可跳转到插件设置页. |
| D8 | 路线图与仓库 | 本文件位于 `D:/idea-projects/AutoJs6-Plugin-MCP-Server/ROADMAP.md` (文件名沿用插件仓库多数约定的大写). 本次会话只落盘路线图, 仓库骨架与 `git init` 在 P0 生成. Client 仓库名预留 `AutoJs6-Plugin-MCP-Client`. |
| D9 | 协议版本双模 | 单端点 `/mcp` 同时服务 `2026-07-28` 无状态模型 (无 `initialize`, 每请求携带 `MCP-Protocol-Version` / `Mcp-Method` / `Mcp-Name` 头与 `_meta` 版本字段, 结果带 `resultType`) 与 `2025-06-18` / `2025-11-25` 有状态模型 (`initialize` 握手 + `Mcp-Session-Id`), 按请求头与方法分派. 当前主流客户端 (Claude Code / Cursor / Inspector) 仍以有状态模型为主, 因此有状态路径是 1.0.0 的验收主线, 无状态路径以 SDK 的 `mcpStatelessStreamableHttp` 为准. 若 P0 证明 SDK 不支持单端点双模, 则 `/mcp` 走有状态, 无状态模型另设路径并写入配置片段. 已废弃的 HTTP+SSE 传输不支持. **P0.2 实测 (2026-09-08)**: SDK 0.15.0 只声明 2024-11-05 / 2025-03-26 / 2025-06-18 / 2025-11-25, 任何携带 `MCP-Protocol-Version: 2026-07-28` 的请求 (含 `initialize`, `server/discover`, `Mcp-Method` / `Mcp-Name` 头) 一律 400 Unsupported protocol version; `mcpStreamableHttp` 对无会话请求回 400 Server not initialized, `mcpStatelessStreamableHttp` 是无会话的 2025 版语义 (无需 `initialize`, GET / DELETE 405), 两者是互斥的路由挂载, 不存在单端点双模. 定稿: `/mcp` 只挂 `mcpStreamableHttp` (有状态, 1.0.0 验收主线); 无状态模型按本决策的退路另设路径 (P2.1), 其中 SDK 的无会话挂载可直接复用, 2026-07-28 的 `server/discover` / `_meta` / `HeaderMismatch` 语义需自研分派或等待 SDK 支持, 是否纳入 1.0.0 由维护者在 P2.1 前拍板. **维护者拍板 (2026-09-10)**: 有状态优先; `/mcp/stateless` 与 2026-07-28 无状态模型不纳入 1.0.0, 等待 SDK 支持后再考虑. P2.1 据此只挂 `mcpStreamableHttp` 并移除 `EXTRA_STATELESS` 评估开关; SDK 内建 DNS rebinding 校验由插件自己的请求门取代 (允许列表随局域网地址变化). |
| D10 | 宿主契约模块与信封 | 宿主新增 `plugin-api/mcp-server-api` (包 `org.autojs.plugin.mcp.server.api`). 控制面 (打开 / 停止服务器, 状态回调) 与数据面 (能力代理) 均采用 Node 家族的 `Bundle` + 字符串常量 + JSON 信封形态, 因为数据面负载本来就是 `NodeBridgeRequest` / `NodeBridgeResponse` JSON, 宿主侧可直接复用 `NodeJsHostCapabilityBroker` 的分派逻辑; 大负载 (截图位图, 文件正文) 走 `ParcelFileDescriptor`. 契约版本用 `McpServerContract.CONTRACT_VERSION` + `MIN/MAX` 区间协商, 不做异常嗅探. (TaggedWire 二进制信封的备选已由 Q1 否决, 2026-09-07.) |
| D11 | 工具命名与描述来源 | 工具名为无前缀 snake_case, 形如 `<组>_<动作>` (`script_run`, `ui_dump`, `screen_capture`, `files_read`); 客户端会按服务器名自行加命名空间 (如 Claude Code 的 `mcp__autojs6__ui_dump`). 工具名, 描述, JSON Schema, 所属权限组, 默认开关, 映射的 bridge 模块 / 方法全部以数据表 (`ToolCatalog`) 定义, 既驱动 `tools/list`, 也生成 README 的工具清单与 JVM 快照测试. |
| D12 | 节点树紧凑表示与节点引用 | `ui_dump` 默认返回宿主 `NodeDump` 的 TEXT 格式经插件二次压缩的紧凑文本 (每节点一行, 只列非空属性, 含 `#n<序号>` 引用, 中心点与边界), 并返回 `snapshotId`; 动作类工具接受 `nodeRef` (上一次快照的 `#n12`), `selector` (`BridgeSelector` 方言的 JSON) 或坐标三者之一. 节点引用按指纹 (类名 / 文本 / 描述 / id / 边界) 在动作时重新定位, 失效时返回 `NODE_REF_STALE` 并提示重新 `ui_dump`. 草案见附录 B. |
| D13 | 选择器方言复用 `BridgeSelector` | 选择器 JSON 键与宿主 `core/automator/bridge/BridgeSelector` 完全一致 (`text` / `textContains` / `textMatches` / `desc*` / `id*` / `className*` / `clickable` / `enabled` / `scrollable` / `depth` / `boundsInside` / `boundsContains`), 不引入第三种方言 (Python Host 的方言差异记入宿主 a11y Roadmap 的后续项). |
| D14 | 截图路径 | 优先 `A11yScreenshotter` (API 30+, 免授权, 有限频重试); API < 30 或失败时回退 MediaProjection (`ScreenCapturer` + 宿主 `ScreenCapturerForegroundService`, 需一次用户授权). 返回 MCP `image` 内容 (base64), 默认缩放到最长边 1280 px, JPEG 质量 70, 可用参数调整; 位图经 `ParcelFileDescriptor` 跨进程, 不进 Bundle. |
| D15 | 前台服务与生命周期归属 | 插件自己的 `McpServerForegroundService` (`foregroundServiceType="specialUse"`, 通知常驻显示端点与 "停止" 动作) 持有监听器. 宿主进程死亡时插件保持监听但工具返回 `HOST_UNAVAILABLE`; 宿主重启且开关未被用户关闭时自动重绑并恢复. 插件进程死亡时宿主的绑定租约收到 death 通知, 有界退避重绑, 失败则开关归位并提示. 两侧都不做开机自启 (Q4 已确认不提供, 2026-09-07). |
| D16 | 默认端口 9637, 只监听回环 | 端口由维护者指定 (Q2, 2026-09-07); 用户可改. 默认绑定 `127.0.0.1`; 局域网模式绑定 `0.0.0.0` 并要求令牌与配对. SDK 默认开启的 DNS rebinding 保护 (只允许 localhost Host 头) 在局域网模式下需把设备 IP 加入允许列表. |
| D17 | 宿主对代理施加固定能力上限 | 宿主为每个 MCP 会话构造带上限的能力授予 (grant): 允许的 bridge 模块 / 方法集合, `NodeBridgePermissionManifest` 令牌子集, 速率与体积上限. 请求声明的 `permissions` 必须是 grant 的子集, 否则 `capability-denied`. 即使插件被替换, 也无法越过宿主授予的边界. |
| D18 | 插件默认关闭且需官方 / 受信签名 | 宿主侧 `AidlPluginHost(defaultEnabled = false)`; 抽屉开关首次打开时要求插件处于 `OFFICIAL` 或 `TRUSTED` 授权态 (`PluginTrustManager`), `USER_GRANTED` 需额外确认对话框. 与向外部 LLM 暴露设备控制能力的风险等级匹配. |
| D19 | PC 端桥接程序为独立仓库 | 仓库 `D:/idea-projects/AutoJs6-MCP-Bridge`, npm 包名 `autojs6-mcp-bridge`, 版本独立于插件并在两侧 README 记录兼容矩阵; 本仓库不含 `bridge/` 目录. (Q3, 2026-09-07) |
| D20 | 示例与离线文档资源的来源 | 示例脚本经宿主 bridge 新增只读方法 `app.listSamples` / `app.readSample` 暴露 (P1.3 顺带); 离线文档资源为可选项: 安装了 `AutoJs6-Plugin-Offline-Docs` 时由宿主转读, 未安装时 `resources/list` 不列出 `autojs6://docs/`. (Q5, 2026-09-07) |
| D21 | 不实现 MCP Tasks 扩展 | 长脚本用同步等待 + 进度通知 + `script_stop` 覆盖; Tasks 扩展待客户端普遍支持后再评估. (Q6, 2026-09-07) |
| D22 | 坐标点击归入 `ui_gesture` 组 | `ui_click` / `ui_long_click` 的 `nodeRef` / `selector` 形式属 `ui` 组 (默认开); 坐标形式在 `ui_gesture` 组关闭时返回 `TOOL_DISABLED` 并提示改用节点引用. 默认配置下模型只能点击可定位的节点. (Q7, 2026-09-07) |

由 D2 / D10 派生的硬约束:

- 插件不复制宿主 `PluginInfo` 或 AIDL 伪实现; `mcp-server-api` 的 AAR 复制到本仓库 `libs/` 并以 SHA-256 锁定 (`locks/host-api-aars.lock`), 或在宿主发布前以受控源码模块形式临时引入, 发布前切换为锁定 AAR.
- 已发布 AIDL 演进只在末尾追加方法并通过 `CONTRACT_VERSION` 协商; 破坏性变更同步升级宿主与插件.
- 插件工具的一切设备能力都经宿主代理; 插件进程自身只申请 `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `org.autojs.permission.PLUGIN`, 不申请存储 / 无障碍 / 悬浮窗等权限.

---

## 2. 范围与非目标

范围内:

- 本仓库: 插件 APK (Binder 服务, 前台服务, Ktor + MCP SDK 服务器, 工具目录, 鉴权与配对, 设置页, 10 语言资源, README / changelog 生成, 单元与 instrumentation 测试, CI).
- 宿主 `D:/idea-projects/AutoJs6`: `plugin-api/mcp-server-api` 契约模块; `core/plugin/mcp/` 宿主客户端与能力代理; bridge 新增方法 (`accessibility.dump` / `accessibility.explain` / `accessibility.screenshot`, `engines.list` / `engines.stop` 的宿主进程语义, `files` 模块, `console` 模块); 抽屉开关与偏好; 插件中心注册; `docs/dev/mcp-server-protocol-v1.md`; changelog.
- PC 端: 独立仓库 `AutoJs6-MCP-Bridge` 的 stdio 桥接程序 (Node.js, npm 包 `autojs6-mcp-bridge`), 客户端配置片段与接入文档 (D19).

非目标 (本 Roadmap 不处理, 但会预留接口):

- MCP Client 能力 (脚本调用外部 MCP 服务器), 见附录 E.
- 宿主内置 MCP 服务器 (不做插件的方案), 已被 D2 否决.
- 无障碍核心 (`core/accessibility`, `core/automator`) 的重构或新语义; 本 Roadmap 只消费宿主 a11y Roadmap 已交付的 `BridgeSelector` / `NodeDump` / `A11yScreenshotter` / `Toolkit`.
- MCP `sampling` / `roots` / `logging` 三项已在 `2026-07-28` 标记废弃的特性, 不实现; 日志走 `notifications/message` 的按请求 `io.modelcontextprotocol/logLevel` 语义 (无状态) 或请求级进度通知.
- 已废弃的 HTTP+SSE 传输 (`2024-11-05`) 与 WebSocket 传输.
- MCP Apps (UI 扩展) 与 Tasks 扩展 (`io.modelcontextprotocol/tasks`); 长耗时脚本用同步等待 + 进度通知 + `script_stop` 覆盖, Tasks 留待附录 C Q6.
- 对 `app/src/main/java/com/stardust/**` 兼容包的任何改动.

---

## 3. 现状诊断

以下是 2026-09-07 探查得到的事实, 是各阶段条目的直接依据. 行号以宿主快照 `5166d088b` 为准.

### 3.1 可直接复用的宿主能力

| 事实 | 锚点 |
| --- | --- |
| `AndroidNodeBridgeCapabilityProvider` 以 `NodeBridgeRequest{id, module, method, args, timeoutMs, permissions}` / `NodeBridgeResponse{id, ok, result, error}` JSON 信封提供 33 个模块 (`accessibility`, `engines`, `rhino`, `image(s)`, `media_projection`, `app`, `clipboard`, `device`, `shell`, `package_manager`, ...); 错误分类 10 种 (`unavailable`, `permission-denied`, `capability-denied`, `provider-failed`, `invalid-request`, `resource-limit`, `rate-limited`, `timeout`, `process-dead`, `runtime-error`) | `engine/NodeBridgeProtocol.kt:120, 235, 281, 312, 590-749, 777` |
| Node 插件的宿主能力代理 AIDL 与宿主实现: `INodeJsHostCapabilityBroker { getBrokerInfo(); dispatch(Bundle, callback); getNativeDiagnostics(); destroy(Bundle) }`, 宿主 `NodeJsHostCapabilityBroker` 把 `KEY_BRIDGE_REQUEST_JSON` 解码为 `NodeBridgeRequest` 后分派 | `plugin-api/nodejs-api/.../INodeJsHostCapabilityBroker.aidl`, `engine/NodeJsHostCapabilityBroker.kt:12`, `NodeJsRuntimeContract.kt:5-14` |
| Rhino-free 的选择器 / 节点 / 动作协议 (Node 与 Python 已共用): `BridgeSelector` (16 个条件), `BridgeNode` (7 字段), `BridgeNodeActions.click/perform/setText`; 根节点获取与搜索惯用法 `WindowRootProvider.default.activeRoot(...)` + `DFS.search(root, filter, limit, NodeLifecycle.default)` | `core/automator/bridge/{BridgeSelector,BridgeNode,BridgeNodeActions}.kt`, `NodeBridgeProtocol.kt:1755-1794, 5110-5113` |
| 节点树导出与诊断: `NodeDump.dump(root, DumpOptions(format TEXT/JSON/XML, maxDepth, properties, visibleOnly, maxNodes))` 返回 `Result(text, nodes, truncated)`; `SelectorExplainer.explain(...)` 给出逐过滤器命中数; `A11yStats` | `core/automator/diagnostics/{NodeDump,SelectorExplainer,A11yStats}.kt` |
| 免授权截图: `A11yScreenshotter.take / takeWithRetry(displayId, mutable, maxAttempts=60, retryDelayMillis=50)` (API 30+), 已处理硬件缓冲转软件位图与限频; MediaProjection 路径 `ScreenCapturer` + `ScreenCapturerForegroundService` | `core/accessibility/A11yScreenshotter.kt:31-78`, `core/image/capture/ScreenCapturer.java:35` |
| 脚本执行: `Scripts.run(...)`, `ScriptEngineService.execute(source, listener, config)`, `StringScriptSource`, `ScriptLaunchSourceFactory`; `ScriptExecution.getId()`, `getEngine().forceStop()`, `getScriptExecutions()`, `stopAll()`, `registerGlobalScriptExecutionListener(...)` | `model/script/Scripts.kt:128-250`, `engine/ScriptEngineService.java:164-249` |
| 宿主自有的 "请求经 Intent + ResultReceiver 异步应答" 模板: `NodeBridgeEngineDispatchService` / `NodeBridgeRhinoExecutionService` | `engine/NodeBridgeEngineDispatchService.kt:22`, `engine/NodeBridgeRhinoExecutionService.kt:42` |
| 通用 AIDL 客户端 `AidlPluginHost` (发现, 探测, 签名与版本校验, 池化绑定 30 s 空闲自动解绑, `linkToDeath`, 一次自动重试) 与长连接专用的 `callWithDedicatedBindingLease` | `core/plugin/AidlPluginHost.kt:35-44, 68-135, 387-409` |
| 插件中心注册三件套: `InstalledPluginRepository.queryDeclaredPluginServices` 的 `specs`, `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE`, `PluginCenterFragment` 探测分支; 信任与授权态 `PluginTrustManager` / `PluginAuthorizationStore`; 默认关闭策略 `PluginDefaultEnabledPolicy` | `core/plugin/center/InstalledPluginRepository.kt:190-205`, `PluginCenterViewModel.kt:1022-1039`, `PluginTrustManager.kt:52-60` |
| 抽屉开关模式: `JsonSocketServerTool` (`connect / disconnect / isNormallyClosed`), `SocketItemHelper` / `ServiceItemHelper`, 偏好 `key_$_server_socket_normally_closed`, `DrawerFragment` 绑定 | `app/tool/JsonSocketServerTool.kt`, `ui/main/drawer/DrawerFragment.kt:310-380`, `res/values/strings_donottranslate.xml:248-249` |
| 长时前台服务先例 (持久停止动作 + 有界心跳租约): `PythonLongRunningForegroundService`; 通知构造 `ForegroundServiceCreator.Builder` | `external/foreground/PythonLongRunningForegroundService.kt`, `AndroidManifest.xml:590-598`, `tool/ForegroundServiceCreator` |
| 新插件家族的宿主接入模板 (提交 `8e25c22e5` 屏幕取色器, `b07a09890` Bun 运行时): 契约模块 + `settings.gradle.kts` 列表 + `app/build.gradle.kts` 依赖 + Manifest `<queries>` + 宿主客户端 + 插件中心三处注册 + 11 语言字符串 + 10 语言 changelog + 测试 | `git show 8e25c22e5 --stat`, `git show b07a09890 --stat` |
| 官方插件只读设置快照 (主题色 / 夜间模式 / 语言), 插件设置页跟随宿主外观 | `plugin-api/common-plugin-api/.../AutoJs6HostSettingsContract.kt`, `docs/dev/official-plugin-settings-contract-v1.md` |

### 3.2 缺口 (需要新建或修改)

| 缺口 | 处理阶段 |
| --- | --- |
| 宿主与任何插件都没有 HTTP / SSE 服务器, 没有 WebSocket 服务端; 唯一的监听是 7347 端口的自定义 8 字节帧 TCP (VSCode 通道), 且无鉴权 | P0 / P2 (插件内 Ktor), 不改宿主 |
| 没有 `mcp-server-api` 契约, 没有 MCP 家族的宿主客户端与能力代理 | P1 |
| bridge `accessibility` 模块没有 `dump` / `explain` / `screenshot` 方法; `NodeDump` 与 `A11yScreenshotter` 只被 Rhino / Python Host 消费 | P1 |
| bridge `engines.all` 只返回调用方自身, `engines.stopAll` 固定返回 `"0"` (插件进程视角); 缺少宿主进程视角的枚举 / 停止 | P1 |
| bridge 没有文件模块 (只有 `storage` 键值与 `app.viewFile/editFile`); Python Host 的 `PythonHostFileScope` 是路径限制的正确先例 | P1 |
| `ConsoleImpl.addLogListener` 为 `private` (仅 `setConsoleView` 可达), `logEntries` 无上限, 没有 "按执行 ID 归集输出" 的钩子; `ScriptExecutionListener` 只有 start / success / exception | P1 (公开有界监听 + 按执行 ID 归集) |
| `NodeBridgeModules.supportedMethodsByModule` 只有名字, 没有参数 Schema 与描述; MCP `tools/list` 需要三者 | P2 (`ToolCatalog` 数据表, 插件侧) |
| 两种选择器方言 (`BridgeSelector` 与 `PythonHostSelectorPolicy`) 并存 | D13 复用前者; 合并记入宿主 a11y Roadmap |
| 插件生态没有开过 TCP 端口的先例, 没有端口 / 令牌 / 配对 / 前台服务的设置 UI 可复制; 三个播放器插件的前台服务 Manifest 形态可参考 | P2 / P4 |
| 没有 `docs/dev/mcp-*.md`, 没有 `test-apps/mcp-server-conformance` | P1 / P6 |

### 3.3 外部事实

| 事实 | 依据 |
| --- | --- |
| MCP `2026-07-28`: 移除协议级会话与 `initialize` 握手, 每请求携带版本与能力 (`_meta`), 新增 `server/discover` (服务器 MUST 实现), `subscriptions/listen` 取代 GET 流与 `resources/subscribe`, MRTR 取代服务器发起的请求, 结果必带 `resultType`, 列表结果必带 `ttlMs` / `cacheScope`, 请求头 `Mcp-Method` / `Mcp-Name` / `MCP-Protocol-Version` 必需且须与正文一致 (`HeaderMismatch -32020`), 资源未找到改为 `-32602`; Roots / Sampling / Logging 废弃 (至少 12 个月窗口) | `modelcontextprotocol.io/specification/2026-07-28/changelog` |
| Kotlin SDK `0.15.0` (2026-07-28): `mcpStreamableHttp` (有状态) 与 `mcpStatelessStreamableHttp` (无状态) Ktor 扩展, `Mcp-Method` / `Mcp-Name` 头, elicitation schema 校验, 握手后并发分派; DNS rebinding 保护默认开启 (0.13.0 起); 重复 `addTool` 名抛异常; Kotlin 2.4.0 / Ktor 3.5.1; README 只声明 JVM / Native / JS / Wasm 目标, 未提 Android, 需 spike | `github.com/modelcontextprotocol/kotlin-sdk/releases` |
| 客户端接入形态: Claude Code `claude mcp add --transport http <name> <url> --header "Authorization: Bearer <token>"` (2026 年有若干 "头部在部分路径未发送" 的 bug 报告, 需真实验证); Cursor `mcp.json` 的 `url` + `headers` (支持 `${env:VAR}`); Claude.ai / Claude Desktop 自定义连接器要求公网 HTTPS 且以 OAuth 为主 (从 Anthropic 云端发起连接, 局域网不可达); 本地 stdio 服务器走 `claude_desktop_config.json` | 各客户端官方文档 (2026-09) |
| 同类设备端 MCP 服务器 (设计参照, 非依赖): `danielealbano/android-remote-control-mcp` (57 工具, 每工具 / 每参数开关, 令牌或内置 OAuth 双模, 前台服务, 隐私模式脱敏), `zerotap` (代理委托模型, `mcp-remote` 接入) | GitHub README (2026-09) |

---

## 4. 目标架构

### 4.1 数据流

```
PC 端 MCP 客户端 (Claude Code / Cursor / Codex CLI / Inspector / stdio 桥接程序)
    |  Streamable HTTP  http://127.0.0.1:9637/mcp (adb forward)  或  http://<手机IP>:9637/mcp (局域网)
    |  Authorization: Bearer <token>
    v
插件进程 :mcp_server (前台服务)
    Ktor CIO  ->  AuthInterceptor (令牌, 常量时间比较)  ->  PairingGate (首次配对确认)
              ->  MCP Server (kotlin-sdk-server; 有状态 + 无状态双模)
              ->  ToolCatalog (名称 / 描述 / Schema / 权限组 / 默认开关 / 映射)  ->  ToolPermissionStore
              ->  HostBridgeClient (JSON 请求, 超时, 宿主死亡处理, PFD 大负载)
    |  AIDL  IMcpHostCapabilityBroker.dispatch(Bundle{bridgeRequestJson}, callback)
    v
宿主进程 (AutoJs6)
    McpHostCapabilityBroker (会话级 grant 校验)  ->  AndroidNodeBridgeCapabilityProvider (33 模块 + 本 Roadmap 新增方法)
    ->  ScriptEngineService / A11yScreenshotter / NodeDump / BridgeSelector / GlobalConsole / 工作目录
```

反向: 宿主 `McpServerPluginHost` (基于 `AidlPluginHost.callWithDedicatedBindingLease`) 绑定插件 `McpServerPluginService`, 调用 `openServer(config, broker, callback)` 得到 `IMcpServerSession`; 状态经 `IMcpServerCallback.onStatus` 回到抽屉开关.

### 4.2 目标包结构

插件 (`io.github.supermonster003.autojs6.plugin.mcp.server`):

```
service/    McpServerPluginService (Binder), McpPluginInfoService (INFO), WakeActivity, McpServerForegroundService
server/     McpHttpServer (Ktor + SDK 装配), ProtocolModeRouter (有状态 / 无状态), AuthInterceptor, PairingGate, RequestLimits
catalog/    ToolCatalog (数据表), ToolSpec, ToolGroup, ToolHandlers/*, ResourceCatalog, PromptCatalog
host/       HostBridgeClient, HostBridgeRequest/Response 编解码, HostAvailability (死亡 / 重绑状态机), BitmapDescriptorReader
ui/         McpServerSettingsActivity, PairingConfirmActivity, ClientSnippets (Claude Code / Cursor / Codex / 通用), ReleaseHistoryActivity
store/      ServerConfigStore, TokenStore (Keystore 支持), PairedClientStore, ToolPermissionStore
nodes/      CompactNodeText (紧凑格式), NodeRefRegistry (快照与指纹重定位)
```

宿主新增 (`org.autojs.autojs.core.plugin.mcp`):

```
McpServerPluginHost            (AidlPluginHost 封装, 专用绑定租约, 有界重绑)
McpHostCapabilityBroker        (IMcpHostCapabilityBroker.Stub, 复用 NodeJsHostCapabilityBroker 的分派核心, 施加 grant)
McpCapabilityGrant             (允许的模块 / 方法 / 权限令牌 / 速率 / 体积上限, 纯 Kotlin 可测)
McpServerTool                  (app/tool, 抽屉开关, key_$_mcp_server_normally_closed)
```

宿主契约 (`plugin-api/mcp-server-api`, 包 `org.autojs.plugin.mcp.server.api`):

```
IMcpServerPlugin.aidl          PluginInfo getInfo(); Bundle getCapabilities(); IMcpServerSession openServer(in Bundle config, IMcpHostCapabilityBroker broker, IMcpServerCallback callback);
IMcpServerSession.aidl         Bundle getStatus(); void updateConfig(in Bundle config); void stop(in Bundle reason); void close();
IMcpServerCallback.aidl        oneway: void onStatus(in Bundle status); void onEvent(in Bundle event);
IMcpHostCapabilityBroker.aidl  Bundle getBrokerInfo(); void dispatch(in Bundle request, IMcpHostCapabilityCallback callback); void destroy(in Bundle reason);
IMcpHostCapabilityCallback.aidl oneway: void onResponse(in Bundle response);
McpServerContract.kt           CONTRACT_VERSION / MIN / MAX, KEY_* 常量, 上限常量 (见附录 A.7)
McpServerActions.kt            SERVICE_ACTION = "org.autojs.plugin.MCP_SERVER", CATEGORY = "mcp-server"
McpServerIds.kt                PLUGIN_ID = "mcp-server", ENGINE = "mcp-server", VARIANT = "default", DEFAULT_PACKAGE_NAME
McpServerCapabilityKeys.kt     REQUIRES_HOST_VERSION, CONTRACT_VERSION, TOOL_GROUPS, PROTOCOL_VERSIONS
```

设计原则:

1. 单一事实来源: 工具的名称 / 描述 / Schema / 权限组 / 映射只存在于 `ToolCatalog`; `tools/list`, README 工具清单, 快照测试都从它派生.
2. 纯 Kotlin 可测: `ToolCatalog`, `CompactNodeText`, `NodeRefRegistry`, `PairingGate` 状态机, `McpCapabilityGrant`, 配置片段生成器都不依赖 Android / Ktor / Binder, 用 JUnit4 直接测试.
3. 失败闭合: 令牌缺失或错误 401; 未配对客户端的 `tools/call` 返回配对提示错误而非阻塞; grant 之外的能力 `capability-denied`; 宿主不可用 `HOST_UNAVAILABLE` 且附带恢复提示; 一切上限超出返回明确错误码.
4. 宿主改动最小且可退化: 插件未安装时抽屉开关显示引导; 未启用 / 不兼容 / 调用失败四态分开提示; 插件禁用或卸载后宿主不保留任何 MCP 服务能力.

---

## 5. 阶段总览

| 阶段 | 目标 | 主要落点 | 前置 |
| --- | --- | --- | --- |
| P0 | 仓库骨架 + SDK 可行性 spike + 决策点 | 插件 | 无 |
| P1 | 宿主契约模块, 能力代理, bridge 新增方法, 插件中心注册 | 宿主 | P0 骨架 (可并行) |
| P2 | MCP 服务器核心: 传输双模, 鉴权, 配对, 工具目录框架, 前台服务, 最小工具集 | 插件 | P0 决策点; 真实代理前可用假代理 |
| P3 | 四组工具面全量落地, 资源与提示 | 插件 + 宿主 (小) | P1, P2 |
| P4 | 宿主抽屉开关, 插件设置页, 配对确认 UI, 通知 | 宿主 + 插件 | P2 |
| P5 | 连接路径与客户端兼容矩阵, PC 端 stdio 桥接程序 | PC + 插件 + 文档 | P3, P4 |
| P6 | 健壮性, 安全, 性能, 一致性测试 | 全部 | P3 |
| P7 | 文档, changelog, 发布 gate, 官方索引 | 文档 + 发布 | P5, P6 |
| P8 (可选) | 公网隧道 + OAuth 2.1 (Claude.ai / Desktop 连接器) | 插件 + 文档 | P7 |

建议会话切分: P0 一次会话; P1 一到两次 (契约 + 代理为一次, bridge 新方法 + 注册为一次); P2 两次 (传输与鉴权; 目录与前台服务); P3 按工具组各一次; P4 一次; P5 两次 (路径与矩阵; 桥接程序); P6 一到两次; P7 一次.

---

## P0: 仓库骨架与可行性 spike

目标: 让 `AutoJs6-Plugin-MCP-Server` 成为一个可构建, 可安装, 能在真机上用 MCP Inspector 列出一个工具的最小 APK, 并在阶段末决定 D4 是否成立.

### P0.1 仓库骨架

- [x] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 第 2 节确定标识并全仓库一致: `{PROJECT_NAME}=AutoJs6-Plugin-MCP-Server`, `{ROOT_PROJECT_NAME}=autojs6-plugin-mcp-server`, `{APP_NAME}=MCP Server`, `{APPLICATION_ID}=io.github.supermonster003.autojs6.plugin.mcp.server`, `{PLUGIN_ID}=mcp-server`, `{PLUGIN_ENGINE}=mcp-server`, `{PLUGIN_VARIANT}=default`, `{PLUGIN_SERVICE}=McpServerPluginService`, `{CAPABILITY_API}=mcp-server-api`, `{SERVICE_ACTION}=org.autojs.plugin.MCP_SERVER`, `{SERVICE_CATEGORY}=mcp-server`, `{REQUIRES_HOST_VERSION}=` P1 交付契约的宿主 `versionCode` (待定, > 5278), `{PLATFORM_VERSIONS_PLUGIN_VERSION}=1.7.4` (已确认 `AutoJs6-Gradle-Platform-Versions/version.properties`, 落地前再确认公共仓库可解析). (SOURCE: McpServerPlugin.kt 常量与 Manifest / resValue / 文档三方一致, ManifestContractTest 守卫; REQUIRES_HOST_VERSION 自 P1.4 起为 5279 (P1 交付契约的宿主构建); 平台插件 1.7.4 已由 Gradle 从 plugins.gradle.org 解析; commit 95f7079)
- [x] (插件) 以 `AutoJs6-Plugin-Three-Stone-AI` (最精简的现代参照) 与 `AutoJs6-Plugin-Python-Runtime` (`libs/README.md` 与 `locks/host-api-aars.lock` 的 AAR 锁定策略) 为模板生成: `settings.gradle.kts` (平台插件位于 `includeBuild("build-logic")` 之前, 无 `mavenLocal()`), 根 `build.gradle.kts` (`apply false` 版本声明), `build-logic/` (`org.autojs.build.{utils,versions,signs,jvm-convention}`), `app/build.gradle.kts` (`globalApplicationId`, `resValue` 的 `plugin_id / plugin_engine / plugin_variant / plugin_author / plugin_version_date`, `buildFeatures { aidl = true; resValues = true }`, `appendDigestToReleasedFiles`, 不启用 ABI 拆分并在 `getInfo()` 显式 `supportedAbis = emptyArray()`), `version.properties` (`VERSION_NAME=1.0.0`, `COMPILE/TARGET_SDK=36`, `MIN_SDK=24`, `OVERRIDDEN_*=NONE`), `gradle.properties`, wrapper. 不复制 `AutoJs6-Plugin-NodeJs-Runtime` 的 `settings.gradle.kts` (仍用旧本地平台插件与 `mavenLocal()`). (ANDROID_BUILD: assembleDebug / testDebugUnitTest / assembleDebugAndroidTest / lintDebug / assembleRelease 通过, AGP 9.3.2 + Kotlin 2.3.20 + Gradle 9.5.0 由平台插件自动选择且仅一段版本决策; debug APK 1.08 MB, release APK 169 KB; 宿主 AAR 由 locks/host-api-aars.lock 哈希锁定并在配置期校验; commit 95f7079)
- [x] (插件) 从宿主复制 `.gitignore`, `sign.properties`, `app/sm003.jks` 到相同相对路径, `git check-ignore` 确认后两者不入库. (SOURCE: git check-ignore 命中 sign.properties / app/sm003.jks / local.properties, 均未入库; commit 95f7079)
- [x] (插件) Manifest 骨架: `org.autojs.permission.PLUGIN`, `<queries><package android:name="org.autojs.autojs6"/></queries>`, `org.autojs.plugin.WAKE_ACTIVITY` + `WakeActivity`, `org.autojs.plugin.info.AUTHOR`, `INFO` 服务 (`org.autojs.plugin.INFO` + category `mcp-server` + `requiresHostVersion` meta-data), `McpServerPluginService` (action `org.autojs.plugin.MCP_SERVER`, category `mcp-server`, `android:process=":mcp_server"`, PLUGIN 权限), `allowBackup=false`, `usesCleartextTraffic="true"` 仅限本插件的回环 / 局域网 HTTP (在 Manifest 注释说明原因), `localeConfig`, `supportsRtl`. (JVM: ManifestContractTest 3 用例; BINDER: McpServerPluginContractTest 3 用例于 emulator-5554 / API 24, Xiaomi 23046RP50C / API 35, Sony G8441 / API 28, 2026-09-08; commit fb54f4e)
- [x] (插件) 资源骨架: 10 语言 `strings.xml` (`plugin_description` 各语言, 句尾无点号, 不写 "适用于 AutoJs6"), `strings_donottranslate.xml` (`app_name=MCP Server`), 按 `name` 升序, ASCII 标点; `mipmap/ic_launcher.png` (体现 MCP 语义的专属图标, 不沿用其它插件图案). (SOURCE: 11 个 strings.xml + strings_donottranslate.xml, ApplicationTextPunctuationTest 守卫 ASCII 标点; 图标由 .python/generate_launcher_icons.py 生成 (圆角框 + MCP 字样 + 三个端口点, 含夜间, 前景与单色变体); commit fb54f4e / 73bb204)
- [x] (插件) `.readme/` + `.changelog/` (10 个 `lang_*.json` + 模板) + `.python/generate_markdown.py` (写入与 `--check`) + `.bat` 入口; 根 `README.md` 标明简体中文并与 `README-zh-Hans.md` 同源; `LICENSE` (与宿主插件生态一致, 徽章同名); `THIRD_PARTY_NOTICES.md` (kotlin-sdk Apache-2.0 / Ktor Apache-2.0 等). (DOCS: generate_markdown.py 写入 36 个产物, --check 通过; THIRD_PARTY_NOTICES.md 暂列 common-plugin-api 与 kotlin-stdlib, SDK 与 Ktor 条目随 P0.2 引入时补齐; commit 73bb204)
- [x] (插件) 复制 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 为本仓库 `AGENTS.md` 并裁剪不适用条款 (无原生库, 无 ABI 拆分, 有独立设置页与发行历史, 有联网监听). (DOCS: AGENTS.md 已裁剪为本仓库约定, 含身份表, 无 ABI 拆分说明, 联网监听与安全约束; commit 73bb204)
- [x] (插件) `git init`; 按 "身份与构建骨架 / 契约 / 资源与文档 / 测试与 CI" 拆分初始提交, 每次提交前把 `VERSION_BUILD` 写为 "当前提交数 + 1", 最后校验 `VERSION_BUILD == git rev-list --count HEAD` 且工作树干净. (SOURCE: 95f7079 骨架 / fb54f4e 契约 / 73bb204 资源与文档 / 6eaa7a4 测试与 CI, 本条随第 5 个提交关闭; 每次提交前 VERSION_BUILD 写为提交数 + 1, 收尾时与 git rev-list --count HEAD 一致且工作树干净)
- [x] (测试) `PluginInfo` 纯数据映射 JVM 测试; Manifest 契约与服务发现 instrumentation 测试 (Wake Activity, INFO 服务, `McpServerPluginService` 的 exported / permission / action / category 唯一命中). (JVM: McpServerPluginRuntimeInfoTest 2 + ManifestContractTest 3 + ApplicationTextPunctuationTest 1; BINDER: McpServerPluginContractTest 3 用例 x 3 台设备 (API 24 / 28 / 35), 2026-09-08; commit 6eaa7a4)
- [x] (插件) CI: `.github/workflows/build.yml` (单元测试 + debug / androidTest APK), `markdown.yml` (Windows 运行 `.python/check_markdown.bat`). (SOURCE: build.yml 含单元测试, debug / androidTest / release APK, lint 与 API 24 x86 + API 35 x86_64 模拟器契约测试矩阵, markdown.yml 在 Windows 上运行 check_markdown.bat; 仓库尚未推送, GitHub 侧尚未实际运行; commit 6eaa7a4)

### P0.2 SDK 可行性 spike

- [x] (插件) 引入 `io.modelcontextprotocol:kotlin-sdk-server:0.15.0` + `io.ktor:ktor-server-cio` (Ktor BOM 3.5.1) + `kotlinx-serialization-json`; 在 `:mcp_server` 进程用 `embeddedServer(CIO, host = "127.0.0.1", port = 9637) { mcpStreamableHttp { server } }` 挂载一个 `device_ping` 工具 (返回插件版本与时间戳); 前台服务临时以最简形式启动. (SOURCE: McpHttpServer / DevicePingTool / McpServerService, 目录 ktor 3.5.1 BOM + mcp-kotlin-sdk 0.15.0; ANDROID_BUILD: assembleDebug / assembleRelease / lintDebug 通过, Kotlin 2.3.20 直接读取 SDK 的 2.4.0 元数据; 前台服务以 DUMP 权限守卫导出供 adb 启停; commit 873c7f2)
- [x] (插件) 记录: `minSdk 24` 上的启动与运行 (AVD API 24 x86 + 一台 API 33+ 实体机), release 构建的 R8 规则 (Ktor / kotlinx-serialization / SDK 反射点), APK 体积增量, 首个请求延迟, 空闲内存; 16 KB 页无关 (无原生库) 记为不适用. (DOCS: docs/dev/p0-spike-evidence.md; DEVICE: AVD API 24 x86, Sony G8441 API 28 (release 包), Xiaomi 23046RP50C API 35, 2026-09-08; release +0.90 MiB, R8 仅需两条 -dontwarn java.lang.management.*, PC 端首请求 197 ~ 428 ms, 空闲 PSS 14.7 MB (release) / 26.4 MB (debug AVD); 16 KB 页不适用, release 无 lib/)
- [x] (测试) `adb forward tcp:9637 tcp:9637` 后, PC 端: `npx @modelcontextprotocol/inspector` 连接 `http://127.0.0.1:9637/mcp` 完成 `initialize` + `tools/list` + `tools/call device_ping`; `curl` 分别以有状态 (`initialize`) 与无状态 (`MCP-Protocol-Version: 2026-07-28` + `Mcp-Method` / `Mcp-Name` 头, `server/discover`) 两种形态各打一轮, 记录 SDK 对单端点双模的真实行为 (D9 的分派方式据此定稿). (CLIENT_E2E: MCP Inspector 2.5.0 --cli 经 adb forward 完成 tools/list + tools/call device_ping, 2026-09-08; HTTP 探针 (Python urllib 代替 curl) 有状态全链路与 2026-07-28 无状态形态各一轮: SDK 0.15.0 仅声明 2024-11-05 ~ 2025-11-25, 无会话请求一律 400, mcpStatelessStreamableHttp 为 2025 版无会话语义, D9 据此定稿; BINDER: McpServerSpikeTest 2 用例 x 3 台设备)
- [x] (测试) Claude Code: `claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer test"` (令牌此时仅回显) 后 `/mcp` 显示 connected 并能调用 `device_ping`; 记录客户端版本. (CLIENT_E2E: Claude Code 2.1.257 print 模式 --mcp-config (http 传输 + Authorization: Bearer test 头) 调用 device_ping 并原样返回 JSON 载荷, 2026-09-08; 以临时 --mcp-config 代替 claude mcp add 以免改写维护者全局配置, 交互式 /mcp 状态页未验证)
- [x] (文档) 决策点: 以上全部通过则 D4 成立并关闭本节; 任一硬性失败 (无法在 API 24 运行, 或 R8 后不可用, 或体积增量 > 6 MiB 且无法裁剪) 则启用附录 D.2 退路并回填 D4. (DOCS: D4 成立, 附录 D.2 退路不启用; D9 定稿见决策表与 docs/dev/p0-spike-evidence.md 第 6.2 节)

验收条件: `:app:assembleDebug` / `testDebugUnitTest` / `assembleDebugAndroidTest` 通过; Inspector 与 Claude Code 各完成一次真实工具调用 (`CLIENT_E2E`); 证据 (设备, API, 客户端版本, APK 体积) 写入本节与 `docs/dev/p0-spike-evidence.md`. 已满足 (2026-09-08): 构建与测试见 P0.1 / P0.2 条目, Inspector 与 Claude Code 各一次真实 `device_ping` 调用, 证据文件已提交.

---

## P1: 宿主契约与能力代理

目标: 宿主具备发现, 授权, 绑定 MCP 插件并向其下发受 grant 约束的能力代理的全部能力; bridge 补齐 MCP 工具面需要的方法. 全部改动在宿主仓库, 按 `8e25c22e5` / `b07a09890` 的模板逐项落地.

### P1.1 契约模块 `plugin-api/mcp-server-api`

- [x] (宿主) 新建模块 (以 `plugin-api/nodejs-api` 与 `screen-color-picker-api` 为模板): `build.gradle.kts` (`aidl = true`, `api(project(":plugin-api:common-plugin-api"))`), `consumer-rules.pro`; 加入 `settings.gradle.kts` 的 `pluginApi` 列表与 `app/build.gradle.kts` 依赖 (带 `// Plugin API: MCP Server` 注释). (SOURCE: `plugin-api/mcp-server-api/build.gradle.kts` (`aidl = true`, `api(project(":plugin-api:common-plugin-api"))`), `consumer-rules.pro`; `settings.gradle.kts` 的 `pluginApi` 列表与 `app/build.gradle.kts` 的 `// Plugin API: MCP Server API` 依赖; 宿主 commit b63cca493)
- [x] (宿主) AIDL 五件 (4.2 节): `IMcpServerPlugin`, `IMcpServerSession`, `IMcpServerCallback`, `IMcpHostCapabilityBroker`, `IMcpHostCapabilityCallback`; 事务顺序即冻结顺序, 后续只追加. (SOURCE: 5 个 AIDL 文件, `McpServerAidlOrderTest` 解析源文件守卫方法顺序与 oneway 标记; 宿主 commit b63cca493)
- [x] (宿主) 常量: `McpServerContract` (`CONTRACT_VERSION=1`, `MIN/MAX`, `KEY_CONTRACT_VERSION`, `KEY_SERVER_CONFIG_*` (端口, 绑定范围, 协议模式), `KEY_STATUS_*` (state / endpoints / pairedClientCount / lastError), `KEY_BRIDGE_REQUEST_JSON` / `KEY_BRIDGE_RESPONSE_JSON` (与 Node 家族同名同义), `KEY_GRANT_*`, 上限常量见附录 A.7), `McpServerActions`, `McpServerIds`, `McpServerCapabilityKeys`; 全部 KDoc 说明 nullability / 上限 / 线程 / 所有权. (SOURCE: `McpServerContract` / `McpServerActions` / `McpServerIds` / `McpServerCapabilityKeys`, 全部 KDoc; 与附录 A.7 的差异: `MAX_FILE_WRITE_BYTES` 降为 1 MiB 以不超过 `MAX_REQUEST_BODY_BYTES`, 新增 `MAX_BRIDGE_INLINE_JSON_BYTES` 512 KiB 与 `MAX_BRIDGE_PAYLOAD_BYTES` 8 MiB (描述符传输), 以及 `MAX_ERROR_MESSAGE_BYTES` / `MAX_EVENT_TEXT_BYTES` / `MAX_CLIENT_NAME_BYTES`; `REQUIRED_HOST_VERSION_CODE` 自 P1.4 起为 5279; 宿主 commit b63cca493)
- [x] (测试) 契约常量与版本区间谓词的 JVM 测试 (`supportsContractVersion`, 上限值非零且有序). (SOURCE: `McpServerContractTest` 6 项 + `McpServerAidlOrderTest` 1 项, `:plugin-api:mcp-server-api:testDebugUnitTest` 通过; 宿主 commit b63cca493)

### P1.2 宿主客户端与能力代理

- [x] (宿主) `core/plugin/mcp/McpServerPluginHost.kt`: 以 `AidlPluginHost(action = SERVICE_ACTION, category = CATEGORY, defaultEnabled = false, ...)` 封装; `openServer(config)` 使用 `callWithDedicatedBindingLease` 并持有租约至会话关闭; 租约 death 时有界退避重绑 (3 次, 1 / 3 / 9 s), 失败则发出 `STOPPED(reason=PLUGIN_DIED)`. (SOURCE: `McpServerPluginHost` (`defaultEnabled = false`, 严格 `validatePluginInfo` / `validateService`, `openServer` 走 `callWithDedicatedBindingLease(retryOnRemoteException = false)`) 与 `McpServerSessionController` (在会话 Binder 上 `linkToDeath`, 死亡后释放租约与代理并按 `McpRebindPolicy` 1 / 3 / 9 s 重绑 3 次, 用户关闭抑制重绑, 耗尽后 `STOPPED(reason = plugin_died)`); 宿主 commit 423678165)
- [x] (宿主) `McpCapabilityGrant` (纯 Kotlin): 允许的 `module.method` 集合 (附录 A.6 的映射表全集), `NodeBridgePermissionManifest` 令牌子集 (`accessibility`, `accessibility.gesture`, `engines`, `engines.exec`, `screen_capture`, `image`, `app`, `app.launch`, `app.query`, `clipboard`, `device`, `shell`, `toast`, `package_manager`), 速率 (沿用 `NodeBridgeLimits.accessibilityQueriesPerSecond`), 每请求体积与并发上限; 请求 `permissions` 不是 grant 子集时返回 `capability-denied`. (SOURCE: `McpCapabilityGrant`: `DEFAULT_METHODS` 为附录 A.6 全集, 令牌子集在本条列表之上补 `app.activity` (`app.editFile`) 与 `device.power` (`device.isIgnoringBatteryOptimizations`), 并预置 P1.3 令牌 `files` / `files.write` / `files.delete` / `console`; 未列方法 -> `permission-denied(category = capability-denied)`, 超集请求 -> `capabilityNotDeclared` (`capability-denied`), 体积超限 -> `resource-limit`, 超时钳制到 30 s / 300 s; 速率沿用 `NodeBridgeLimits.defaults.accessibilityQueriesPerSecond`; 宿主 commit 423678165)
- [x] (宿主) `McpHostCapabilityBroker : IMcpHostCapabilityBroker.Stub`: 抽取 `NodeJsHostCapabilityBroker` 的分派核心为可复用的 `HostCapabilityBrokerCore` (或直接组合), 为 MCP 会话构造 `NodeBridgeEngineInfo(engineName = "mcp", sourceName = 客户端名)`, 每次 `dispatch` 先过 grant 再交给 `AndroidNodeBridgeCapabilityProvider`; `destroy` 释放 provider; 每个 `openServer` 一个代理实例. (SOURCE: 未抽取 `HostCapabilityBrokerCore`, 改为给 `AndroidNodeBridgeCapabilityProvider` 增加可注入的 `permissionManifest` 构造参数 (grant 本身作为 enforced manifest, 缺省仍读工作目录 manifest, Node 路径行为不变); `NodeBridgeEngineInfo.copy(engineName = "mcp", backend = "plugin")`, 每个 `openServer` 一个代理, `destroy` 释放 provider, 内联 JSON 超过 512 KiB 的请求与结果均以 `resource-limit` 拒绝; 宿主 commit 423678165)
- [x] (宿主) 插件中心注册三处 + Manifest `<queries>` 的 `<intent>` (action + category) + `InstalledPluginRepository` 的 `ServiceQuery` + `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE` + `PluginCenterFragment` 探测分支; 抽屉与设置的字符串 11 个 `values*/strings.xml` (本阶段只加插件中心可见文案, 抽屉开关文案在 P4). (SOURCE: `InstalledPluginRepository` 发现分支 + `ServiceQuery`, `PluginCenterViewModel.SERVICE_ACTION_BY_ENGINE`, `PluginCenterFragment.probeAidlPlugin`, Manifest `<queries>` 的 `<package>` 与 `<intent>` (action + category); 本阶段无需新增字符串资源: 插件中心直接渲染插件自身的 `PluginInfo`, 抽屉文案留待 P4; 宿主 changelog 10 语言 `feature` 条目 (v6.8.0, 2026/09/08) 已随提交生成; 宿主 commit 423678165)
- [x] (测试) `McpCapabilityGrantTest` (子集 / 超集 / 未知令牌 / 速率), `McpServerPluginHostTest` (发现, 版本不兼容, 重绑退避; 以假 Binder 驱动), 插件中心注册测试 (仿 `PluginCenterScreenColorPickerRegistrationTest`). (SOURCE: `McpCapabilityGrantTest` 8 项 (子集 / 超集 / 未知模块 / 体积 / 超时钳制 / 上限校验 / 与 `requiredCapabilitiesFor` 一致性), `McpServerConfigTest` 7 项 (配置校验, `dual` 保留, 状态助手, 重绑退避 1 / 3 / 9 s 且第 4 次为空), `PluginCenterMcpServerRegistrationTest` 2 项; `McpServerPluginHostTest` 未做: 宿主 JVM 测试为纯 JUnit4 且 Binder / Bundle 为 android.jar stub, 无法以假 Binder 驱动, 发现 / 版本不兼容 / 重绑改由 P1 验收的 Binder 往返 (`BINDER`) 与 P2 真实插件覆盖; `:app:compileAppDebugKotlin` 与定向 `:app:testAppDebugUnitTest` 通过; 宿主 commit 423678165)

### P1.3 bridge 新增方法 (供 MCP, Node, Python 共同受益)

- [x] (宿主) `accessibility.dump(options)`: 参数 `{format: "text"|"json"|"xml", maxDepth, maxNodes, visibleOnly, properties[], window: "active"|"all"}` -> `NodeDump.dump(...)`, 结果含 `text|nodes`, `truncated`, `packageName`, `activityName` (当前窗口), 节点条目携带 `BridgeNode` 七字段 + `depth` + `index`; 上限 `maxNodes <= 400`, `maxDepth <= 32`, 文本 `<= 256 KiB`. (SOURCE: `NodeBridgeAccessibilityDiagnostics.dump`: JSON 形式为 Android-free 的 `BridgeNodeDump` 前序走查 (节点 7 字段 + `root` / `depth` / `index` / `childCount` / `visible`), text / xml 经 `NodeDump`, schema `autojs6-bridge-accessibility-dump-v1`, 结果含 `packageName` / `activityName` / `roots` / `nodeCount` / `truncated` / `textTruncated`; 上限 `BridgeAccessibilityLimits` 400 / 32 / 256 KiB; `window` 为 all 时用后释放根节点; `BridgeNodeDumpTest` 7 项; 宿主 commit f034dc895)
- [x] (宿主) `accessibility.explain(selector)` -> `SelectorExplainer.explain(...)` 的逐过滤器命中数与渲染文本. (SOURCE: `SelectorExplainer.explain(roots, selector.toFilters(), ...)` (`BridgeSelector.toFilters()` 新增), schema `autojs6-bridge-accessibility-explain-v1`: `steps[]` (filter / matched / cumulative), `matches` / `nearMisses` / `count` / `found` / `failingIndex` / `failingFilter` 与 `render()` 文本, 选项 `window` / `maxNodes` (<= 2000) / `keep` (<= 20); `BridgeSelectorFiltersTest` 3 项; 宿主 commit f034dc895)
- [x] (宿主) `accessibility.screenshot(options)`: `A11yScreenshotter.takeWithRetry` (API 30+) -> 按 `{scale|maxWidth, format, quality, region}` 缩放 / 裁剪 / 编码 -> 写入宿主 cache 的一次性文件并以 `ParcelFileDescriptor` 返回 (Bundle 中只放元数据: 宽高, 格式, 字节数, 截图耗时); API < 30 或失败时返回 `unavailable` 并携带 `fallback: "media_projection"` 提示; 权限令牌 `screen_capture`. (SOURCE: `A11yScreenshotter.takeWithRetry` (API 30+), 编码在单线程执行器 `AutoJs6BridgeScreenshot`, 选项 `format` jpeg / png / webp, `quality` 1..100 (默认 80), `scale` 与 `maxWidth` 二选一, `region`, `savePath` (工作目录内); 写入 `cacheDir/bridge-payloads` 的一次性文件 (10 min 清扫) 或工作目录文件, JSON 只带元数据 (`width` / `height` / `format` / `mime` / `bytes` / `elapsedMs` / `payload`); `McpHostCapabilityBroker.extractOneShotPayload` 把 `payload {kind: file, oneShot: true}` 转成 `ParcelFileDescriptor` (`KEY_BRIDGE_PAYLOAD_FD/BYTES/MIME`, JSON `payload.kind = descriptor`, 文件随即 unlink, > 8 MiB 拒绝); 编码超过 4 MiB -> `resource-limit`; API < 30 -> `permission-denied(category = unavailable)` 且消息含 `fallback: media_projection`; 令牌 `accessibility` + `screen_capture`; 宿主 commit f034dc895)
- [x] (宿主) `engines.list()` / `engines.stop(id)` / `engines.stopAll()` 的宿主进程语义 (`ScriptEngineService.getScriptExecutions / getScriptExecution(id).engine.forceStop / stopAll`), 与既有插件进程视角的 `engines.all` / `stopAll` 并存, 不改变后者. (SOURCE: `NodeBridgeHostEnginesBridge`: `list` (schema `autojs6-bridge-engines-list-v1`, `ScriptEngineService.getScriptExecutions()`, `state` starting / running / finished, `startedAt` / `uptimeMs`), `stop(id)` (`getScriptExecution(id).engine.forceStop()`, schema `-stop-v1`), `stopAll` 仅在传入 `{scope: "host"}` 时调用 `ScriptEngineService.stopAll()` (schema `-stop-all-v1`), 无 scope 或 `scope: plugin` 保持既有插件进程答复 `0`, `engines.all` 不变; 偏差: 宿主语义以 scope 选项区分而非新方法名, 便于 MCP 与 Node 共用同一方法集; 令牌 `engines`; 宿主 commit ee8bd83de)
- [x] (宿主) `files` 模块 (以 `PythonHostFileScope` 为路径限制先例): `list(path, recursive, maxEntries)`, `stat(path)`, `read(path, encoding, maxBytes)`, `write(path, content, createDirs, overwrite)`, `delete(path)`, `mkdir(path)`, `rename(from, to)`; 根为 `WorkingDirectoryUtils.path`, 拒绝 `..` / 符号链接逃逸 / 绝对路径; 写入后 `Explorers.workspace().refreshAll()`; 新令牌 `files` / `files.write` / `files.delete` 加入 `NodeBridgePermissionManifest.definedCapabilities`. (SOURCE: `NodeBridgeFileScope` (Android-free: 规范化相对路径, canonical 包含检查, 符号链接不可逃逸, 读写 1 MiB, UTF-8 / base64, 列表 2000 项, 截断读取丢弃不完整的 UTF-8 尾部) + `NodeBridgeFilesBridge` (schema `autojs6-bridge-files-<method>-v1`, 未知选项拒绝, 变更后 `Explorers.workspace().refreshAll()`); 签名为 `list(path?, {recursive, maxEntries})` / `stat(path)` / `read(path, {encoding, maxBytes})` / `write(path, content, {encoding, createDirs, overwrite, append})` / `mkdir(path)` / `rename(from, to, {overwrite})` / `delete(path, {recursive})`; 令牌 `files` / `files.write` (write / mkdir / rename) / `files.delete` 加入 `definedCapabilities` 且不由 `files` 隐含; `NodeCapabilityBrokerPolicy` 给 `files.write` 2 MiB args 上限, 请求上限取 `max(maxRequestBytes, maxArgsBytes + 16 KiB)`; `NodeBridgeFileScopeTest` 5 项; 宿主 commit 3ee2abebe)
- [x] (宿主) `console` 模块: `tail(lines, sinceId, level)` 读取 `GlobalConsole` 的有界尾部 (新增公开的 `ConsoleImpl.addLogListener / removeLogListener` 与 `logEntries` 的软上限, 如 4096 条, 超出丢最旧; 记 changelog `improvement`); `engines.execScript` / `execScriptFile` 增加 `captureConsole` 选项: 以 `ScriptExecutionListener` + 该执行的 `runtime.console` 代理归集本次执行的输出 (JS 引擎), 非 JS 引擎退化为时间窗过滤并在结果中标明 `captureMode`. (SOURCE: `NodeBridgeConsoleTail` (Android-free 尾部策略) + `NodeBridgeConsoleBridge` (`console.tail({lines <= 500, sinceId, level, maxBytes <= 256 KiB})`, schema `autojs6-bridge-console-tail-v1`, 返回 `firstId` / `lastId` / `nextSinceId` / `latestId` / `total` / `capacity` / `truncated` / `evicted`); `ConsoleImpl` 公开 `addLogListener` / `removeLogListener` (`CopyOnWriteArrayList`), `MAX_LOG_ENTRIES = 4096` 软上限 (丢最旧), 新增 `latestLogId` / `snapshotLogEntries`, `ConsoleView.refreshLog` 改为按条目 id 增量刷新以兼容软上限; `engines.execScript` / `execScriptFile` 新增 `captureConsole` 与 `waitMs` (0..300000, 归集时默认为 `timeoutMs`): 以全局 `ScriptExecutionListener` 等待完成 (不漏掉 `execute` 返回前结束的脚本), 结果加 `finished` / `outcome` (running / success / exception) / `error` / `waitedMs` 与 `console` 窗口; 偏差: 所有运行时共用同一个 `GlobalConsole` (`ScriptRuntime.console` 即 `AutoJs.globalConsole`), 无法按执行代理, 因此 JS 引擎同样退化为共享控制台上的 id 窗口并标明 `captureMode = "global-window"`; changelog `improvement` 已记; `NodeBridgeConsoleTailTest` 6 项; 宿主 commit 7464f70c3)
- [x] (宿主) `app.currentWindow()`: 当前活动窗口的包名 / 活动名 / 窗口列表 (来自 `A11yWindowFacts`); `device.info()` 补齐屏幕尺寸 / 密度 / 方向 / 电量 / 宿主版本 / 无障碍服务状态. (SOURCE: `NodeBridgeAccessibilityDiagnostics.currentWindow` (schema `autojs6-bridge-app-current-window-v1`: `packageName` / `activityName` 自 `ActivityInfoProvider`, `windows[]` 自 `A11yWindowFacts` 含 id / type / typeName / packageName / title / active / focused / layer / displayId / bounds; 宿主 commit f034dc895); `device.info()` 为 `NodeBridgeDeviceInfo` (schema `autojs6-bridge-device-info-v1`: `build` / `screen` (`ScreenMetrics`: 宽高 / densityDpi / orientation / rotation) / `battery` (粘性 `ACTION_BATTERY_CHANGED`: percent / status / plugged / temperatureCelsius) / `host` (packageName / versionName / versionCode / processName / pid) / `accessibility` (serviceRunning / operational) / `screenOn` / `locale` / `timeZone` / `uptimeMs`; 不含 IMEI / 序列号 / Android ID; 宿主 commit ee8bd83de)
- [x] (宿主) `app.listSamples(path, maxEntries)` / `app.readSample(path, maxBytes)`: 宿主内置示例脚本 (assets `sample/`) 的只读枚举与读取, 路径限制在示例根内 (D20). (SOURCE: `NodeBridgeSampleCatalog` (Android-free: 注入 `AssetManager.list` / `open`, 路径复用 `NodeBridgeFileScope.normalizeRelative` 且限制在 `sample/` 内, 点开头条目隐藏, 2000 项 / 1 MiB, UTF-8 / base64) + `NodeBridgeSamplesBridge` (`listSamples(path?, {recursive, maxEntries})` schema `autojs6-bridge-app-list-samples-v1`, `readSample(path, {encoding, maxBytes})` schema `autojs6-bridge-app-read-sample-v1`, 在主线程之外执行, `inrt` 风味无示例时 `available: false`); 令牌 `app.query`; `NodeBridgeSampleCatalogTest` 6 项; 宿主 commit ee8bd83de)
- [x] (测试) 每个新方法的 JVM 测试 (参数校验, 上限, 路径逃逸, 输出归集) + Node Bridge 既有测试回归; `docs/dev/accessibility-architecture.md` 的消费方表增加 MCP 一行. (SOURCE: 新增 JVM 测试 27 项: `BridgeNodeDumpTest` 7, `BridgeSelectorFiltersTest` 3, `NodeBridgeFileScopeTest` 5, `NodeBridgeConsoleTailTest` 6, `NodeBridgeSampleCatalogTest` 6 (均为 Android-free 策略层; 与宿主单例耦合的 `engines.list` / `device.info` / `screenshot` JSON 层无 JVM 测试, 由 P1 验收的 Binder 往返与 P2 真实插件覆盖); 回归: `org.autojs.autojs.engine.*` + `core.plugin.mcp.*` + `core.automator.bridge.*` 共 23 个套件通过, 宿主全量 JVM 测试见 P1.4; `NodeBridgeConformanceInstrumentationTest.stableDispatchMethods` 镜像已同步 (未在真机运行); `docs/dev/accessibility-architecture.md` 消费方表增加 MCP 一行 (宿主 commit c02f4ed6d); 宿主 changelog 10 语言 4 条 `improvement`; 宿主 commits f034dc895 / 3ee2abebe / 7464f70c3 / ee8bd83de)

### P1.4 协议文档与 changelog

- [x] (文档) `docs/dev/mcp-server-protocol-v1.md` (按 `python-runtime-protocol-v1.md` 的骨架: Decision / Binder Surface / Bundle 键表 / 状态机 / Grant 与上限 / 生命周期与死亡处理 / R0 scope): 冻结 AIDL 顺序, Bundle 键, 状态枚举, 错误分类, 上限. (SOURCE: 按 `python-runtime-protocol-v1.md` 骨架: Decision / V1 Binder Surface / Bundle Keys (能力, 配置, 状态, 原因, 事件, 代理信息, bridge 请求 / 响应 7 张键表) / State Machine / Grant And Ceilings / Bridge Methods Added For MCP / Lifecycle And Death Handling / Payload Descriptor Convention / R0 Scope And Non-Goals; 宿主 commit c02f4ed6d)
- [x] (宿主) 宿主 `.changelog` 10 语言: `feature` (MCP 插件契约与能力代理), `improvement` (bridge 新方法, 控制台监听公开) ; 运行生成脚本. (SOURCE: `feature` 条目随 P1.2 (宿主 commit 423678165), 4 条 `improvement` 随 P1.3 四个提交 (accessibility 诊断与 currentWindow, files, console 与 captureConsole, engines / device / samples), 每次均运行 `.python/generate_markdown.py`; P1.4 无需新增条目)
- [x] (宿主) 提交并记录交付契约的宿主 `versionCode`, 回填本文件 P0.1 的 `{REQUIRES_HOST_VERSION}` 与插件 `requiresHostVersion`. (SOURCE: 宿主 `version.properties` `VERSION_BUILD=5279`, `McpServerIds.REQUIRED_HOST_VERSION_CODE = 5279L` (宿主 commit c02f4ed6d); 插件 `McpServerPlugin.REQUIRED_HOST_VERSION = 5279L`, Manifest 两处 `requiresHostVersion` meta-data, AGENTS.md 与本文件 P0.1 / P1.1 记录同步 (本提交); README 由生成脚本重新生成)

验收条件: 宿主全量 JVM 测试通过, `assembleAppDebug` 通过; 用 `test-apps` 风格的假插件 (或 P2 的真实插件) 完成一次 `openServer` -> `dispatch(accessibility.dump)` -> `onResponse` 的 Binder 往返 (`BINDER`); 协议文档与 changelog 已提交. -> 2026-09-10 以 P2.3 的真实插件完成: 宿主 `McpServerPluginRoundTripTest` (`app/src/androidTest/java/org/autojs/autojs/core/plugin/mcp/`) 经 `McpServerSessionController.open` 打开会话, 插件附着后经 broker `dispatch(device.info)` -> `onResponse` (`McpServerRuntime: Host probe ok`), 状态 `running` + `hostAvailable`, `stop` / `close` 收尾: 宿主 `McpServerPluginRoundTripTest` 1/1 于同 4 台设备 (API 28 / 31 / 33 / 35): `open` -> running (绑定 879 / 253 / 136 / 272 ms) + hostAvailable, 插件日志 `Host probe ok` 115 / 39 / 43 / 48 ms (broker `dispatch(device.info)` -> `onResponse`), `refreshStatus` -> `stop` (stopped, port 0) -> `close`; 宿主 debug APK 6.8.0 / 5279 由 HEAD a37bbd2ae + 控制器修复 (94f65d301) 构建; 偏差: 往返方法为 `device.info` 而非 `accessibility.dump` (后者随 P3.1 `ui_dump` 到来, 桥接路径相同).

---

## P2: MCP 服务器核心

目标: 插件在有状态 Streamable HTTP 模型下 (无状态模型按 D9 的 2026-09-10 修订推迟) 能通过鉴权与配对完成工具调用; 工具目录框架, 前台服务与宿主代理接入就绪; 用最小工具集 (`device_info`, `device_ping`, `script_run`) 打通端到端.

### P2.1 传输与协议模式

- [x] (插件) `McpHttpServer`: Ktor CIO, 绑定地址与端口来自 `ServerConfigStore`; 请求体上限 (SDK `maxRequestBodySize`) 1 MiB; 空闲连接与请求超时; 启动失败 (端口占用, 权限) 转为 `STOPPED(reason=BIND_FAILED)` 状态并附提示. (SOURCE: `server/McpHttpServer` (`embeddedServer(CIO)` + `connector { host; port }` 取自 `ServerConfig` (`store/ServerConfig` + `store/ServerConfigStore`, SharedPreferences `mcp_server_config`, 字符串编码 + 宽容解码, 端口 1024..65535, `bindScope` loopback `127.0.0.1` / lan `0.0.0.0`, `developerMode`, `extraAllowedHosts` <= 8); 请求体上限 1 MiB 由请求门实现 (SDK 公开的 `mcpStreamableHttp` 不暴露 `maxRequestBodySize`, 见偏差); `connectionIdleTimeoutSeconds = 60`, 停止 grace 250 ms / timeout 1 s; 请求级超时留给 P2.3 的工具超时; `start()` 不抛异常: `BindFailures.classify` 把 EADDRINUSE / `BindException` 归为 `port_in_use`, EACCES / EADDRNOTAVAIL / 其它归为 `bind_failed` 并附提示, 非法配置为 `invalid_config`, 状态经 `ServerStatus` (`stopped` / `starting` / `running` / `stopping` / `failed`, 与 `McpServerContract.STATE_*` 同名) 推给监听器; `McpServerService` 读取存储配置, `EXTRA_PORT` / `EXTRA_DEVELOPER_MODE` 仅覆盖本次运行, 失败时记录并 `stopSelf()`; 插件 commit 0f2d112)
- [x] (插件) 传输挂载 (按 D9 的 P0.2 定稿): `/mcp` 只挂 `mcpStreamableHttp` (有状态); 无状态路径另设 `/mcp/stateless`, 先复用 SDK 的 `mcpStatelessStreamableHttp` (2025 版无会话语义), 2026-07-28 模型 (`server/discover`, 每结果 `_meta` 携带 `io.modelcontextprotocol/serverInfo`, 列表结果携带 `ttlMs` / `cacheScope: "private"`, 头与正文版本不一致返回 `HeaderMismatch (-32020)`; 有状态路径实现 `initialize` 协商 (声明 `2025-06-18` 与 `2025-11-25`), `Mcp-Session-Id` 与 `DELETE` 关闭. (SOURCE: `server/McpTransport.mcpServerModule`: 请求门 + `mcpStreamableHttp(path, enableDnsRebindingProtection = false)`; `initialize` 协商 (`2025-06-18`, `2025-11-25`), `Mcp-Session-Id` 与 `DELETE` 关闭为 SDK 行为, JVM 测试确认 `initialize` 200 + 会话头, `notifications/initialized` 202, `DELETE` 200 后旧会话 404; 偏差: 按维护者 2026-09-10 拍板 (D9 修订) 无状态路径 `/mcp/stateless` 与 2026-07-28 模型不纳入 1.0.0, 等待 SDK 支持后再评估, `EXTRA_STATELESS` 评估开关已移除; 插件 commit 0f2d112)
- [x] (插件) DNS rebinding 保护: 回环模式允许 `localhost` / `127.0.0.1`; 局域网模式把当前 IPv4 (及可选主机名) 加入允许 Host 列表, IP 变化时刷新; CORS 默认关闭, 仅 "开发者模式" 开关允许 Inspector 的浏览器来源. (SOURCE: `server/HostHeaders` (Host / Origin 解析: 去端口, IPv6 方括号, 小写, 畸形拒绝) + `server/RequestGate` (`GatePolicy.loopback` = `localhost` / `127.0.0.1` / `[::1]`, `GatePolicy.lan` 追加当前 IPv4 与 `extraAllowedHosts`; 校验顺序 Host -> Origin -> 预检 -> 声明长度; 拒绝为 403 / 413 + JSON-RPC `-32600` 错误体); `server/LanAddressWatcher` 用 `ConnectivityManager.registerNetworkCallback` 在网络变化时重读 IPv4 并重建策略 (无需重启监听器, JVM 测试 `lanAddressesAreHonouredAfterARefreshWithoutRestarting`); CORS 默认关闭: 任何 `Origin` 都被拒绝, 仅 `developerMode` 放行回环来源 (Inspector `http://localhost:<port>`) 并回 `Access-Control-Allow-Origin` / `Expose-Headers` 与 OPTIONS 预检 204; 插件 commit 0f2d112)
- [x] (插件) 服务器信息: `Implementation(name = "autojs6-mcp-server", version = 插件 versionName)`; 能力声明 `tools(listChanged = true)`, `resources`, `prompts`; 工具列表顺序确定 (按目录顺序) 以利客户端缓存. (SOURCE: `server/McpServerFactory.create(versionName)`: `Implementation(name = "autojs6-mcp-server", version = 插件 versionName)`, `ServerCapabilities(tools(listChanged = true), resources(subscribe = false, listChanged = false), prompts(listChanged = false))`; SDK `FeatureRegistry` 为插入有序的 `persistentMapOf`, 工具按注册顺序列出 (JVM 测试注册 `zeta_tool` / `alpha_tool` 后 `tools/list` 保持该顺序); 插件 commit 0f2d112)
- [x] (测试) JVM: 路由分派与头校验的纯逻辑测试; instrumentation: 在设备上启动服务器后用 Ktor 客户端跑有状态 / 无状态两套最小会话. (SOURCE: JVM 新增 28 项: `ServerConfigTest` 7, `HostHeadersTest` 4, `RequestGateTest` 7, `BindFailuresTest` 5, `McpTransportTest` 5 (Ktor `testApplication` 上跑真实 SDK 传输: 有状态最小会话 + 工具顺序 + `DELETE`, Host 拒绝, LAN 刷新, 开发者模式 CORS 与预检, 1 MiB 上限的声明长度与流式两种路径均 413); `testDebugUnitTest` 35 项全部通过; instrumentation `McpServerSpikeTest` 改为 P2.1 断言 (`serverInfo.name`, 能力声明) 并新增伪造 Host 403, 同端口 `port_in_use`, 非法端口 `invalid_config` 三项: `McpServerSpikeTest` 5 项 + `McpServerPluginContractTest` 3 项在 4 台设备全部通过 (Sony G8441 API 28, Sony XQ-AT72 API 31, AVD sdk_gphone64_x86_64 API 33, Xiaomi 23046RP50C API 35; `device_ping` 往返 19..88 ms; 日志 build/p21-sony28.log / p21-sony.log / p21-avd33.log / p21-pad.log); 首轮设备运行暴露并修复两处真实缺陷: CIO accept 协程的 `BindException` 在 Android 上杀死进程 (现由 `serverConfig { parentCoroutineContext = CoroutineExceptionHandler }` 接住), 以及 API 31+ 上 `stopService()` 紧跟 `startForegroundService()` 触发 `ForegroundServiceDidNotStartInTimeException` (测试与 KDoc 改为经 `ACTION_STOP` 停止, `onDestroy` 同步停止监听器); 偏差: 设备侧用 `HttpURLConnection` / 原始 socket 而非 Ktor 客户端 (无需新增 androidTest 依赖), 无状态会话按 D9 修订不再运行; `testDebugUnitTest` 35 项通过, `lintDebug` 0 错误 (6 条既有的依赖版本 / 图标提示), `assembleDebug` / `assembleDebugAndroidTest` / `assembleRelease` (R8) 通过, `generate_markdown.py --check` 通过; 插件 commit 0f2d112)

### P2.2 鉴权与配对

- [x] (插件) `TokenStore`: 32 字节随机令牌 (base64url), Android Keystore 包裹的私有存储, 轮换 API, 设置页显示 / 复制 / 轮换; `AuthInterceptor` 常量时间比较, 缺失或错误返回 401 + `WWW-Authenticate: Bearer`; 令牌不写日志. (SOURCE: `server/BearerTokens` (32 字节 `SecureRandom` -> base64url 无填充 43 字符, `parseAuthorization` 只接受 `Bearer <token>` 且 scheme 不区分大小写, `constantTimeEquals` 在双方 SHA-256 摘要上做异或累加, 期望值缺失或空白时失败闭合, `fingerprint` 16 位十六进制, `tail` 尾 4 位) + `store/TokenStore` (Keystore 别名 `mcp_server_token` 的 AES-256-GCM 包裹, 文档 `noBackupFilesDir/mcp_server/token.json`, Keystore 不可用时未包裹存放并告警, `rotate()`, 按文档文本缓存解包结果, 每次访问重读以便他进程轮换即时生效) + `server/AuthInterceptor` (请求门之后: 401 + `WWW-Authenticate: Bearer realm="autojs6-mcp-server"` + JSON-RPC `-32001`, 日志只含指纹); 偏差: 设置页显示 / 复制 / 轮换推迟到 P4.2, 落地前开发者模式下 `dumpsys activity service .McpServerService` 是 adb 控制面读取令牌的唯一途径 (`McpServerService.dump`, 非开发者模式只打印指纹); 插件 commit a88dd7c)
- [x] (插件) `PairingGate` (纯 Kotlin 状态机): 客户端指纹 = 令牌指纹 + 客户端标识 (`initialize.clientInfo` 或 `_meta` 的 `io.modelcontextprotocol/clientInfo`, 缺失时用 `User-Agent`) + 远端地址类别 (回环 / 局域网); 未配对客户端允许 `initialize` / `server/discover` / `tools|resources|prompts/list`, 首次 `tools/call` 或 `resources/read` 返回错误 `PAIRING_REQUIRED` (含 60 s 内在手机上确认的提示) 并触发配对请求; 确认后 60 s 内的重试成功; 拒绝或超时进入 30 s 冷却. (SOURCE: `server/PairingGate`: `check(identity, method)` / `approve` / `deny` / `revoke` / `pendingRequests`, 60 s 窗口, 30 s 冷却 (`denied` / `timeout` / `limit`), 最多 32 个客户端, 已配对客户端的 `lastSeenAt` 每分钟至多触碰一次, `Listener` 回调 (`onPairingRequested` / `onPaired` / `onDenied`); `server/PairingInterceptor` 在鉴权之后解析 POST 正文的 JSON-RPC 方法, 门控 `tools/call` / `resources/read` / `resources/subscribe` / `prompts/get`, 身份取 SDK 会话的 `clientVersion` (经传输的 `Mcp-Session-Id` 定位 `ServerSession`) 否则 `User-Agent`, 加 `AddressClass.ofRemoteHost`; `PAIRING_REQUIRED` (`-32002`, data: fingerprint / client / addressClass / expiresAt / firstRequest) 与 `PAIRING_DENIED` (`-32003`, data: reason / retryAt) 按批次内每个请求 id 各回一份; 偏差: 指纹 = SHA-256(规范化客户端名 + 地址类别) 前 24 位, 不含令牌指纹, 否则与下一条 "令牌轮换不清除配对" 矛盾 (对话框显示令牌尾 4 位以示区分); `_meta` 的 `io.modelcontextprotocol/clientInfo` 未实现 (SDK 会话已携带 `clientInfo`); `server/discover` 不是 MCP 方法, 未门控的方法一律放行; 插件 commit a88dd7c)
- [x] (插件) `PairedClientStore`: 已配对客户端 (指纹, 名称, 首次 / 最近连接时间, 地址类别), 可撤销; 令牌轮换不清除配对. (SOURCE: `store/PairedClientStore` (实现 `PairedClientRepository`, `all` / `put` / `remove` / `clear`) + `store/PairedClientCodec` (`format` 1, 宽容解码, 按指纹去重) + `store/ProcessSharedFile` (`noBackupFilesDir/mcp_server/paired_clients.json`, 每次访问重读, 临时文件 + rename 原子替换, 锁文件跨进程 / 跨线程串行化读改写); `ServerConfigStore` 同步改用该文件存储 (`config.json`), 因为 `SharedPreferences` 按进程缓存, 常驻的 `:mcp_server` 进程读不到设置页写入; 令牌轮换不触碰该文件 (指纹不含令牌); 插件 commit a88dd7c)
- [x] (插件) 配对确认 UI: `PairingConfirmActivity` (全屏对话框样式, 显示客户端名称, 来源地址, 令牌尾 4 位, 允许 / 拒绝) + 高优先级通知 (带允许 / 拒绝动作) 双通道; 屏幕锁定时仅通知. (SOURCE: `server/PairingCoordinator` (每个监听器一个, `PairingCoordinator.instance`; 高优先级通道 `mcp_pairing` 的通知带允许 / 拒绝动作 (广播到非导出的 `ui/PairingDecisionReceiver`) 与内容意图; 键盘锁未锁定时再启动 `ui/PairingConfirmActivity`; 请求被决定 / 过期 / 监听器停止时撤销通知并经 `addSettledListener` 关闭对话框) + `ui/PairingConfirmActivity` (`Theme.Translucent.NoTitleBar` + `AlertDialog`, 显示客户端名称, 来源 (USB / 回环 或 局域网), 令牌尾 4 位, 允许 / 拒绝, 过期自动关闭, `singleTop` 复用时替换对话框) + 11 个 locale 的 `pairing_*` 字符串; 偏差: 不申请 `USE_FULL_SCREEN_INTENT` (权限清单不变), Android 10+ 可能阻止服务启动 Activity, 此时通知是可靠通道; 插件 commit a88dd7c)
- [x] (测试) JVM: `PairingGateTest` (状态迁移, 超时, 冷却, 撤销), `AuthInterceptorTest` (常量时间比较, 空令牌失败闭合); instrumentation: 401 / 配对错误 / 配对后成功三段. (SOURCE: JVM 新增 25 项: `BearerTokensTest` 5, `PairingGateTest` 10 (列出免配对, 首次门控请求, 等待中重复, 批准, 拒绝 -> 冷却 -> 新请求, 超时不可批准, 撤销, 32 上限, `lastSeen` 触碰, 身份 / 地址类别 / 门控方法), `PairedClientCodecTest` 2, `AuthInterceptorTest` 4 (401 + 挑战头 + `-32001`, 正确令牌与 scheme 大小写, 空令牌失败闭合, 请求门先于鉴权), `PairingInterceptorTest` 4 (Ktor `testApplication` 上的真实 SDK 传输: 配对前可 list, `tools/call` -> `-32002` 且 `data.client` 为 `clientInfo.name`, 批准后成功, 撤销后再次要求, 拒绝后 `-32003`; 无会话按 `User-Agent` 命名; 批次逐 id 回错; `JsonRpcCalls.parse` 宽容); `testDebugUnitTest` 60 项全部通过; instrumentation `McpServerSpikeTest` 7 项: 全部请求携带 `TokenStore(context).current()`, 新增无令牌 401 + `WWW-Authenticate`, 配对三段 (`-32002` -> 经 `PairingDecisionReceiver` 广播允许 -> `device_ping` 成功) 与拒绝 -> `-32003`: `McpServerSpikeTest` 7 项在 4 台设备全部通过 (Sony G8441 API 28, Sony XQ-AT72 API 31, Sony XQ-DQ72 API 33, Xiaomi 23046RP50C API 35), 最终构建上 API 28 / 31 / 35 再跑 7 项 + `McpServerPluginContractTest` 3 项通过; 配对后 `device_ping` 往返 7..62 ms; 开发者模式 `dumpsys activity service` 在 API 31 打印状态 / 指纹 / 令牌 / 已配对数, 非开发者模式与停止后分别只打印指纹 / 无服务; 日志 build/p22-inst-*.log; `testDebugUnitTest` 60 项通过, `lintDebug` 0 错误 (6 条既有的依赖版本 / 图标提示; `java.util.Base64` 的 NewApi 错误已改为自写 base64url), `assembleDebug` / `assembleDebugAndroidTest` / `assembleRelease` (R8) 通过, `generate_markdown.py --check` 通过; 插件 commit a88dd7c)

### P2.3 工具目录框架与宿主代理接入

- [x] (插件) `ToolCatalog` 数据表: `ToolSpec(name, title, description, inputSchema(JSON Schema 2020-12), outputSchema?, group: ToolGroup, defaultEnabled, bridge = module.method 或 composite, permissions[], timeoutMs, annotations{readOnlyHint, destructiveHint, idempotentHint})`; `ToolGroup` = `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell`; `ToolPermissionStore` 持久化各组开关 (默认值见 D6); 被禁用的工具不出现在 `tools/list`, 调用时返回 `TOOL_DISABLED`. (SOURCE: `tools/ToolSpec` (`name` / `title` / `description` / `inputSchema` / `outputSchema?` / `group` / `bridge: BridgeMethod?` / `permissions` / `timeoutMs` / `maxTimeoutMs` / `hints`, `toSdkTool()` 生成 SDK `Tool` + `ToolAnnotations`, `toSnapshotJson()`) + `tools/ToolGroup` (D6 八组与默认值) + `tools/ToolCatalog` (`device_ping` / `device_info` / `script_run`, `tools/JsonSchemas` 生成 2020-12 封闭对象 Schema) + `tools/ToolPermissions` (纯编解码 `{format, groups}`, 文档未提及的组保持默认) + `tools/ToolPermissionStore` (`noBackupFilesDir/mcp_server/tool_groups.json`, 经 `ProcessSharedFile`) + `tools/ToolRegistry` (只向 SDK 注册已启用工具, `refresh()` 增删并逐会话 `sendToolListChanged`, 实现 `ToolGate`) + `server/ToolGateInterceptor` (配对门之后, `tools/list` / `tools/call` 前刷新开关, 已知但关闭的工具回 `isError` + `TOOL_DISABLED`) + `tools/ToolArguments` (必填 / 未知键 / 类型 / 整数范围 / 长度 / 枚举 -> `INVALID_ARGUMENTS`); 偏差: `defaultEnabled` 由 `ToolGroup` 推导而非每工具字段, composite bridge 留到 P3 需要时再加, 暂无工具声明 `outputSchema`; SDK 0.15.0 的 `ToolSchema` 只序列化 type / properties / required / $defs, `tools/list` 里看不到 `additionalProperties: false`, 未知参数由 `ToolArguments` 在插件侧拒绝; 插件 commit d6d9a10)
- [x] (插件) `HostBridgeClient`: 把 `ToolSpec.bridge` + 参数编码为 `NodeBridgeRequest` JSON 放入 Bundle 经 `IMcpHostCapabilityBroker.dispatch`; 请求 id 单调递增; 每请求超时 (默认 30 s, `script_run` 最长 300 s); 并发上限 4; 宿主错误分类到 MCP 错误 (`isError` 工具结果 + 结构化 `error.code/category/hint`); `linkToDeath` 代理死亡 -> `HostAvailability.DEGRADED`, 所有工具返回 `HOST_UNAVAILABLE` 直至新代理到达. (SOURCE: `bridge/BridgeEnvelope` (`BridgeRequest.toJson()` = `NodeBridgeRequest` 形状, `BridgeResponse.parse` 以 Bundle 的 ok / errorMessage 兜底, `BridgeError.fromJson` 宽容) + `bridge/BridgeTransport` (`BrokerInfo` 读 grant, `BridgeReply`, `BridgePayload` 到达即关闭) + `bridge/BinderBridgeTransport` (请求 Bundle 带 `bridgeRequestJson` / `bridgeClientName`, 每请求一个 `IMcpHostCapabilityCallback.Stub` 强引用直到回应, `linkToDeath`) + `bridge/HostBridgeClient` (`mcp-N` 单调 id, 超时 = min(请求超时, grant 上限) + 2 s 宽限 -> `TIMEOUT`, 并发上限 = min(grant, 4) 且排队最多 10 s -> `LIMIT_EXCEEDED`, 未授权方法直接 `CAPABILITY_DENIED`, 死亡 -> 在途调用 `HOST_UNAVAILABLE` + `HostAvailability.DEGRADED`, `detach()` -> `DETACHED`) + `bridge/ToolFailure` (附录 A.5 映射: process-dead / unavailable -> `HOST_UNAVAILABLE` (accessibility 模块 -> `A11Y_SERVICE_NOT_RUNNING`), capability-denied / permission-denied -> `CAPABILITY_DENIED`, resource-limit -> `LIMIT_EXCEEDED`, rate-limited -> `RATE_LIMITED`, timeout -> `TIMEOUT`, invalid-request -> `INVALID_ARGUMENTS` (新增码), 其余 -> `HOST_ERROR`; 文本 `code: message (hint)`, `structuredContent.error` 含 code / message / hint / category / hostCode / module / method); 插件 commit d6d9a10)
- [x] (插件) `McpServerPluginService` (Binder): `getInfo()` (含 `requiresHostVersion`, `contractVersion`, `toolGroups`, `protocolVersions`), `openServer(config, broker, callback)` 启动前台服务与 HTTP 服务器并返回 `IMcpServerSession`; 校验宿主调用方 (UID / 包名 / 签名, 仿 Python 插件 `HostCallerVerifier`); 重复 `openServer` 复用运行中实例并换新代理. (SOURCE: 真实 `IMcpServerPlugin.Stub`: `getInfo()` / `getCapabilities()` 经 `McpServerPluginRuntimeInfo.capabilitiesBundle()` 报告 `requiresHostVersion` 5279, `mcpServerContractVersion` 1, `mcpServerToolGroups` [script, device], `mcpServerProtocolVersions` (SDK `SUPPORTED_PROTOCOL_VERSIONS` 降序), `mcpServerSdkVersion` 0.15.0; `openServer` 先经 `host/HostCallerVerifier` (调用 UID = 宿主 UID, 宿主 versionCode >= 5279, 签名集合相同) 再交给 `host/McpServerRuntime.openSession` (`host/SessionConfig` 校验端口 / bindScope / 仅 stateful / hostLabel <= 64, 契约版本不支持 -> `unsupported_contract`; 附着 `BinderBridgeTransport` + `HostBridgeClient`; 生命周期线程以运行中的配置 (否则存储配置, adb 开启的 developer mode 在宿主附着后保留) + 宿主端口 / 范围启动监听器 (端口或范围不同则重启, reason `config_changed`), 回调发布状态, 再经 broker 做一次 `device.info` 探测); `host/HostSession` (`getStatus` / `updateConfig` / `stop` / `close`, 每次校验会话所有者 UID, `publishStatus` / `publishEvent` 经 oneway 回调; `stop` 保留会话, `close` 连监听器一起停); 偏差: 重复 `openServer` 在前一宿主仍存活 (bridge 可用且 broker `pingBinder`) 时回 `IllegalStateException("already_open: ...")`, 宿主已死或会话已关闭时才换新代理并复用运行中的监听器 (宿主 `McpServerSessionController` 本就只开一个会话); `getInfo` / `getCapabilities` 与 INFO 服务一样只受清单权限保护; 插件 commit d6d9a10)
- [x] (插件) `McpServerForegroundService`: `specialUse` 子类型说明, 常驻通知 (状态, 端点, 已配对数, "停止" 与 "打开设置" 动作), 停止时回调 `onStatus(STOPPED, reason=USER)`; Android 13+ 请求 `POST_NOTIFICATIONS`; 通知被禁用时降级为 toast 提示. (SOURCE: `McpServerService` 改为 `host/McpServerRuntime` 的前台壳: `ACTION_START` / `ACTION_STOP` (adb / 设置页) 调用运行时, 运行时在宿主开启会话后以 `ACTION_KEEP_ALIVE` 拉起前台 (监听器已不在运行则自行停止), 启动被拒 (API 31+ `ForegroundServiceStartNotAllowedException`) 时记录并发 `warning` 事件; 停止 / 启动竞态: `finishForeground()` 在仍有 start 任务排队时推迟, 以 `stopSelfResult(lastStartId)` 让系统已受理的新 start 否决停止并重回前台, `onDestroy` 只在非运行时主动结束 (`finishing == false`) 时才停监听器 (设备链首轮 spike 测试的 stop / start 对曾因此丢失新监听器); 通知显示端点 / AutoJs6 连接状态 / 已配对数与 "停止" 动作 (`PendingIntent` 到 `ACTION_STOP`), 监听器状态 / 宿主会话开关 / 宿主死亡 / 配对时重绘, 通知被禁用时 toast 提示端点一次; 停止后运行时经回调发布 `stopped` 状态 (`statusLastErrorCode` 只在非 `user_request` 时携带); `dump` 增加 `activeSessions` / `hostSession` / `toolGroups` / `tools`; 11 个 locale 的 `server_*` 字符串; 偏差: 未加 "打开设置" 动作 (设置页在 P4.2), `specialUse` 子类型说明与 `POST_NOTIFICATIONS` 声明沿用 P0.2, 运行期通知权限请求留给 P4.2 设置页; 插件 commit d6d9a10)
- [x] (插件) 最小工具集: `device_ping`, `device_info` (bridge `device.info`), `script_run` (`engines.execScript` + `captureConsole`, 同步等待 + 进度通知 `notifications/progress`); 三者跑通 P0 的 Inspector / Claude Code 路径但改为经宿主代理. (SOURCE: `tools/CatalogToolExecutor` (校验参数 -> `tool_call` 事件 (只含工具名与客户端名) -> 本地工具或经 `HostBridgeClient` 调用, 带 `_meta.progressToken` 时每 5 s 一条 `notifications/progress`) + `tools/ToolResults` (成功: JSON 文本 + `structuredContent`; 失败: `isError` + `code: message (hint)`) + `tools/ScriptRunTool` (`engines.execScript(name, source, options)`: 名称去掉 `.js` 后缀 (宿主 `StringScriptSource` 固定为 JavaScript 并自行补 `.js` 显示, 首轮手工验证曾得到 `demo.js.js`), `options.timeoutMs` = min(timeoutMs, 60 s), `waitMs` = timeoutMs 上限 290 s (`waitForCompletion=false` 为 0), bridge 超时 = max(两者) + 10 s, `cwd` / `arguments` 仅非空时传, 控制台按 `maxConsoleLines` 截尾并标 `returned` / `truncated`); `device_info` 直通 `device.info` 结果; 客户端路径: API 31 `QV710AF65F` 经 `adb forward tcp:9637` 的 PC stdlib 客户端 (`build/tools/mcp_client_e2e.py`, 未入库): initialize 296 ms -> tools/list 82 ms [device_ping, device_info, script_run] -> device_info 回 PAIRING_REQUIRED 轮询, 手机通知的 Allow 动作确认 (`Client paired`) -> device_info ok 91-112 ms (host 6.8.0 / 5279, XQ-AT72, a11y 未运行) -> script_run(toast + console.log) outcome=success, finished, waited 147-802 ms, 控制台 6 行含 `hello from mcp`, 任务名 `$engine/p23_e2e.js` -> script_run(throw) outcome=exception, 控制台含 `Error: boom`; 杀宿主 (`am force-stop org.autojs.autojs6`) -> `dumpsys` hostSession host=degraded, device_info -> `HOST_UNAVAILABLE` 37 ms 附提示; 宿主重开会话 -> 新会话替换旧会话 (host=available), device_info ok 80 ms; developer mode 下 token 只经 `dumpsys` 打印到 adb shell; 插件 commit d6d9a10)
- [x] (测试) JVM: `ToolCatalogTest` (名称唯一, Schema 合法, 默认开关, 快照文件 `tool-catalog.snapshot.json` 漂移守卫), `HostBridgeClientTest` (以假代理: 超时, 死亡, 错误映射, 并发上限); instrumentation: 真实宿主 (P1 后) 或假代理 APK 的端到端. (SOURCE: JVM 新增 45 项 (共 105 项通过): `ToolCatalogTest` 8 (名称 snake_case 唯一, 封闭 Schema, D6 默认, 启用过滤与顺序, `script_run` 行, `device_*` 行, SDK 视图, 快照 `app/src/test/resources/tool-catalog.snapshot.json` 漂移守卫), `HostBridgeClientTest` 14 (信封, 单调 id 与超时钳制, 超时, 死亡, 链接失败, 错误映射表, 并发上限, 未授权, 空 grant, detach, 畸形回应, dispatch 异常, 描述符关闭, grant 查询), `BridgeEnvelopeTest` 5, `ToolPermissionsTest` 3, `ScriptRunToolTest` 6, `ToolArgumentsTest` 3, `SessionConfigTest` 4, `McpServerPluginRuntimeInfoTest` +2 (能力键, SDK 版本与 `libs.versions.toml` 一致); instrumentation: `McpServerHostSessionTest` 4 (假 broker: `openServer` -> running / 端点 / 契约版本 / hostAvailable -> 全目录 -> 配对 (含 `pairing_requested` / `client_paired` 事件) -> `device_info` 与 `script_run` 信封与结果整形 -> 宿主失败 `HOST_ERROR` -> `INVALID_ARGUMENTS` -> `tool_call` 事件 -> 分组关闭隐藏 + `TOOL_DISABLED` -> `stop` / `close`; `already_open`; `invalid_config`; 无会话 `HOST_UNAVAILABLE`), `McpServerPluginContractTest` 3 (真实 `IMcpServerPlugin` 的 `getInfo` / `getCapabilities` 能力键, 非宿主 `openServer` -> `SecurityException`), `McpServerSpikeTest` 7 (工具清单改为全目录): 2026-09-10 插件 instrumentation 14/14 (`McpServerPluginContractTest` 3 + `McpServerHostSessionTest` 4 + `McpServerSpikeTest` 7) 于 Sony G8441 API 28 (`BH900ASK9E`, 10.9 s), Sony XQ-AT72 API 31 (`QV710AF65F`, 7.3 s), Sony XQ-DQ72 API 33 (`QV770340J7`, 6.2 s), Xiaomi Pad 23046RP50C API 35 (`968e9f18`, 7.6 s), 每台先 `adb install -r -t` 插件 debug + androidTest APK 再按类运行; 宿主仓库 `McpServerPluginRoundTripTest` (真实宿主 `McpServerSessionController.open` -> running + hostAvailable -> `refreshStatus` -> stop -> close): 宿主 `McpServerPluginRoundTripTest` 1/1 于同 4 台设备 (API 28 / 31 / 33 / 35): `open` -> running (绑定 879 / 253 / 136 / 272 ms) + hostAvailable, 插件日志 `Host probe ok` 115 / 39 / 43 / 48 ms (broker `dispatch(device.info)` -> `onResponse`), `refreshStatus` -> `stop` (stopped, port 0) -> `close`; 宿主 debug APK 6.8.0 / 5279 由 HEAD a37bbd2ae + 控制器修复 (94f65d301) 构建; JVM `:app:testDebugUnitTest` 105/105; `generate_markdown.py --check` MARKDOWN_OK (10 languages, 36 artifacts); `:app:lintDebug` 0 errors / 6 warnings (既有 GradleDependency / IconDuplicatesConfig / NewerVersionAvailable); `:app:assembleDebug` + `:app:assembleDebugAndroidTest` + `:app:assembleRelease` 通过; 插件 commit d6d9a10)

验收条件: AVD API 24 与一台实体机上, Claude Code 与 Inspector 经令牌 + 配对完成 `device_info` 与 `script_run("toast('hi')")` (`CLIENT_E2E`); 杀死宿主进程后工具返回 `HOST_UNAVAILABLE`, 宿主重启并重新 `openServer` 后自动恢复 (`DEVICE`); 关闭通知动作停止服务器且宿主开关归位. 已满足 (2026-09-10, 插件 commit f314ec5 的 debug 构建, SSE 响应): CLIENT_E2E: AVD API 24 (`emulator-5554`) 与实体机 API 31 (XQ-AT72) 上, Inspector CLI (`npx @modelcontextprotocol/inspector --cli --transport http`, `Authorization: Bearer` 头) 与 Claude Code (`claude -p --mcp-config`, http 传输) 均经令牌 + 配对 (AVD 直接弹 `PairingConfirmActivity` 对话框, API 31 走通知的 ALLOW 动作) 完成 `device_info` (返回宿主 6.8.0 / 5279 与机型) 与 `script_run("toast('hi'); console.log(...)")` (`status finished`, 控制台含 `hi from inspector` / `hi from claude`); DEVICE: AVD 上 `am force-stop` 宿主 -> 会话 `host=degraded`, `device_info` 返回 `HOST_UNAVAILABLE`; 宿主重开会话 (测试进程 `openServer`) 后同一监听器 `device_info` 立即成功 (build/tools/run_p23_killhost.sh, 此前已在 API 31 验证); 通知 STOP 动作 (API 31): 点按后插件服务消失 (`dumpsys` No services match), 宿主回调 `stopping` -> `stopped` 且无错误码 (运行时日志 `Stopping the listener (user_request)`). 偏差: "宿主开关归位" 依赖 P4 的宿主设置开关, 本次以宿主回调 `stopped` 代替; 宿主进程崩溃后的自动重绑仍由 `McpServerSessionController` 的重试覆盖 (P4.1 前不改); API 24 无 `am start-foreground-service`, 证据脚本按 API 级别改用 `startservice`.

---

## P3: 工具面全量落地

目标: 附录 A 的工具清单全部可用, 每个工具有 Schema, 描述, 错误提示, JVM 测试与真机证据; 资源与提示上线.

### P3.1 脚本执行与日志 (`script` 组)

- [x] (插件) `script_run` 完整参数 (`source`, `name`, `workingDirectory`, `arguments`, `timeoutMs`, `waitForCompletion`, `captureConsole`, `maxConsoleLines`); 返回 `executionId`, `status` (`finished` / `error` / `running` / `timeout`), `exception` (含行列), `console[]`, `durationMs`; 超时不杀脚本, 返回 `running` 并提示用 `script_stop`. (SOURCE: tools/ScriptRunTool (`Prepared` / `Options` / `shape` / `exceptionOf`) 与 tools/ToolCatalog.ScriptRun (常量与 Schema); `status` 只有 `finished` / `error` / `running`: 等待到期或 `waitForCompletion=false` 时报 `running` 并附 `hint` (指向 `script_stop` / `script_list` / `console_tail`), 不设 `timeout` 状态 (见 docs/dev/p3-tools.md); `exception` = `{message, line?}` (从宿主错误文本 `<message> (<file>#<line>)` 取行号, Rhino 不给列号); `durationMs` = 宿主 `waitedMs`; `console[]` 取最新 `maxConsoleLines` 行并附 `consoleCount` / `consoleTruncated`. 证据 (API 31 XQ-AT72, 插件 commit f314ec5 的 debug 构建, build/tools/run_p31_evidence.sh): 语法错误 `var x = ;` -> `status: error`, `exception.line 1`, message `syntax error ($engine/p31_syntax.js#1)`; `null.foo` -> `error`, `TypeError: Cannot read property "foo" from null (...#1)`; 死循环 `timeoutMs 3000` -> `running`, `durationMs 3000`, `console` 最新 2 行 (`maxConsoleLines 2`); 5 s 脚本 -> `finished`, `durationMs 5115`)
- [x] (插件) `script_run_file` (`path` 相对工作目录, `arguments`), `script_stop(executionId)`, `script_stop_all`, `script_list` (`engines.list`), `console_tail(lines, sinceId, level)`. (SOURCE: tools/ToolCatalog 新增 `scriptRunFile` / `scriptStop` / `scriptStopAll` / `scriptList` / `consoleTail` 五行 (目录 8 行, 快照 `tool-catalog.snapshot.json`), tools/ScriptTools 做参数编码与结果整形 (`script_stop` -> `{executionId, name, state, stopped}`, `script_stop_all` -> `{stopped}` 且宿主 `engines.stopAll` 带 `{scope: "host"}`, `script_list` -> `{count, executions[{executionId, name, engine, path, workingDirectory, state, startedAt, uptimeMs}]}`, `console_tail` = 宿主记录去掉 `schema`); `script_run_file` 的 `path` 相对宿主工作目录, `arguments` 经 `options.arguments` 进入 `engines.myEngine().execArgv`. 证据 (API 31): `script_run` 写入 `mcp_p31_file.js` 后 `script_run_file {path, arguments: {who: "mcp"}}` -> `finished`, 控制台 `from file mcp`; `script_list` 列出运行中的死循环 (`state running`, `uptimeMs`); `script_stop(2)` -> `stopped true` 且列表变空, 对已结束 id 再次 `script_stop` -> `INVALID_ARGUMENTS: engines.stop: no running execution with id`; `script_stop_all` -> `{stopped: N}` (UI 模式脚本的停止为异步, 紧随其后的 `script_list` 仍可能列出它); `console_tail {lines 3, level warn}` 只含 warn 以上, `{sinceId: nextSinceId}` 只返回其后的新行 (无新行时 `count 0`); 非法参数 (`executionId -1`, `lines 0`, 空白 `path`, 未知参数) 均为 `INVALID_ARGUMENTS`)
- [x] (插件) 长脚本的进度通知: 有状态路径每 2 s 一条 `notifications/progress` (含最近一行输出); 无状态路径以响应 SSE 流承载. (SOURCE: tools/CatalogToolExecutor.withHeartbeat: 运行工具每 2 s (`SCRIPT_HEARTBEAT_MS`) 一条 `notifications/progress`, `message` = `waiting for AutoJs6 (N s); last output: <console.tail 最新一行, 截 200 字符>` (授权含 `console.tail` 时), 其他宿主工具保持 5 s 无行; 通知带关联请求 id (`RequestHandlerExtra.requestId`). 实测发现 SDK 0.15.0 的 `mcpStreamableHttp` 固定 JSON 响应模式 (`enableJsonResponse = true`), 该模式把与请求关联的通知改投独立 GET 流, 无 GET 流时直接丢弃 (设备上客户端收不到任何心跳), 故新增 `server/SseStreamableMount.mcpStreamableSse`: 带请求的 POST 以 `text/event-stream` 应答 (自实现 `ServerSSESession`: 首个事件才开始响应体, 空事件写成 SSE 注释, 由传输层 `close` 结束响应体; 传输层异步派发请求且把流簿记绑在 call 生命周期上, 路由等到响应体写完才返回), 只含通知的 POST 仍 202, 网关 / 鉴权 / 配对拒绝仍为 JSON. 证据 (API 31): 5 s 脚本带 `progressToken` -> 客户端在同一响应流先后收到 `progress: 2001/25000 waiting for AutoJs6 (2 s); last output: ... step 2` 与 4 s 一条, 随后 `finished`; Inspector CLI 与 Claude Code 在 SSE 响应下完成 `tools/list` / `device_info` / `script_run`)
- [x] (测试) JVM: 参数校验与结果映射; DEVICE: 语法错误脚本, 异常脚本, 死循环脚本 + `script_stop`, UI 模式脚本各一例. (SOURCE: JVM 115 项全过: 新增 `ScriptRunToolTest` 9 (参数校验, 文件路径, 结果 / 异常 / 运行中形状), `ScriptToolsTest` 5, `ToolCatalogTest` +1 (脚本分组行映射到宿主方法), `McpTransportTest` +1 (`postResponsesStreamAsEventsWithTheirRelatedNotificationsFirst`: 延迟 300 ms 的工具, 关联通知先于结果出现在 POST 事件流); `McpServerHostSessionTest` 增加假宿主 2.6 s 慢脚本 (同一响应流 1 条心跳, 含 `last output: e1`, 随后 `finished`) 及 throw / run_file / list / tail / stop / stop_all 各形状. DEVICE (build/tools/run_p31_evidence.sh + run_p31_evidence_stop.sh, API 31): 语法错误脚本, 异常脚本, 死循环 + `script_stop`, UI 模式脚本 (`waitForCompletion=false`, 截图 `hello from mcp`), `script_run_file`, 进度心跳; 插件 14 项 instrumentation + 宿主 `McpServerPluginRoundTripTest` 在 API 24 (AVD) / 28 / 31 / 33 / 35 全过 (最终构建); lint 0 错误 6 警告, release 构建通过)

### P3.2 无障碍 UI 观察与操作 (`ui` / `ui_gesture` 组)

- [x] (插件) `ui_dump(format, maxDepth, maxNodes, visibleOnly, window)`: 经 `accessibility.dump` 取 JSON 节点 -> `CompactNodeText` 生成附录 B 格式 -> `NodeRefRegistry` 登记 `snapshotId` 与指纹; 返回窗口包名 / 活动名, 节点数, 是否截断; `format="json"` 返回原始节点数组供程序化消费. (SOURCE: tools/UiTools.dump + tools/CompactNodeText + tools/NodeRefRegistry + tools/UiNodes (`UiNodeInfo` / `Bounds`): `accessibility.dump({format, maxDepth, maxNodes, visibleOnly, window})` 的 JSON 节点 -> 附录 B 紧凑文本 (行 = `#nK` + 深度缩进 + 短类名 + 标记 (clickable / long_clickable / checkable / checked / scrollable / editable / focused / selected / !enabled / hidden) + `"text"` + `desc=` + `id=<短 id>` + 有子节点给 bounds, 叶子给 `c=(x,y)`; 文本转义并截 40 字符; 头行 `window: <包名>/<活动短名>  bounds=[l,t][r,b]  nodes=N` (宿主截断时加 `truncated=true`); 尾行说明有子节点未展示的原因 (`visibleOnly`, `maxDepth`, 或 `maxNodes` 预算) 与可调参数); 结构化内容 `{snapshotId, format, window{packageName, activityName}, roots, nodeCount, truncated}`; `format=json` 另带 `nodes[]` (完整节点记录 + `ref`), `format=xml` 直接返回宿主 XML; `maxNodes` 1..400 (默认 200), `maxDepth` 默认 32, `visibleOnly` 默认 true, `window` active / all; 每次 `ui_dump` 替换快照 (`s1`, `s2`, ...), 引用 `#n1`, `#n2`, ... 按前序编号, 存活 60 s, 监听器停止时清空, `dumpsys` 打印 `nodeRefs`. 证据 (API 28 G8441 / 31 XQ-AT72 / 33 XQ-DQ72 / 35 23046RP50C, 插件 commit 811781d 的 debug 构建 + 宿主 3507afdda, build/tools/run_p32_evidence.sh): AutoJs6 UI 模式脚本窗口 `nodes=24` (API 28 为 23) 49 ms, 尾行 `(2 listed nodes have children that are not shown (visibleOnly, maxDepth=32, or the budget); raise maxNodes (up to 400), lower maxDepth, or use ui_find)`; Settings 首页 `nodes=72` (API 35 80, API 33 67, API 28 49) 202 ms; `format=json, maxNodes 3` -> `nodeCount 3, truncated true`; `format=xml` 头两行 `<?xml` / `<hierarchy>`; `dumpsys` 末尾 `nodeRefs: s4: 75 refs`)
- [x] (插件) `ui_find(selector, limit, timeoutMs)` (`accessibility.findAll` + 轮询), `ui_current_window`, `ui_explain_selector(selector)`, `ui_wait_for(selector, state: appear|disappear, timeoutMs)`. (SOURCE: tools/UiTools.find / currentWindow / explainSelector / waitFor 与 tools/UiSelectors (选择器对象 -> BridgeSelector 方言 (D13) 的 16 个键; `id` 无 `:` 时转 `idMatches (?:.*:id/)?<id>`, `className` 无 `.` 时转 `classNameMatches (?:.*\.)?<name>`; 正则在插件侧编译; 空对象 / 未知键 / 类型错 / 重复键均为 `INVALID_ARGUMENTS` 并点名条件); `ui_find` 每 500 ms 轮询 `accessibility.findAll` 直到 `timeoutMs` (0..60000, 默认 0), `limit` 1..100 (默认 10), 结果 `{snapshotId?, selector, count, returned, truncated, elapsedMs, nodes[], hint?}` 并把节点追加进当前快照 (无快照时新开); `ui_current_window` = `app.currentWindow` 去掉 `schema`; `ui_explain_selector` = `accessibility.explain` (宿主文本为内容, `matches` / `nearMisses` 为节点记录); `ui_wait_for` 每 500 ms 轮询 `findOne`, `state` appear / disappear, `timeoutMs` 100..120000 (默认 10000), 满足返回 `{state, satisfied, selector, elapsedMs, snapshotId?, node?}`, 到期 `TIMEOUT` 附提示. 证据 (API 31): `ui_wait_for {textMatches "(?i)press me", timeoutMs 15000}` 在脚本启动后 1043 ms 命中 (`#n1`), `{text "clicked"}` 6 ms; `ui_find {textContains "hello"}` count 1 (`hello from mcp +1`), `{text "nothing here", timeoutMs 1200}` count 0 + hint (1547 ms); `ui_explain_selector {text "Press me", className "TextView"}` 文本 `text("Press me") matched 0, cumulative 0 <- nothing left` / `classNameMatches(/(?:.*\.)?\QTextView\E/) matched 42`; Settings `ui_find {clickable true, limit 2}` count 21, returned 2, truncated true; `ui_current_window` 报 `org.autojs.autojs6/org.autojs.autojs.execution.ScriptExecuteActivity`, `com.android.settings/.homepage.SettingsHomepageActivity`, `SubSettings` 与 `accessibilityAvailable: true`; `ui_wait_for {text "nothing here", timeoutMs 800}` -> `TIMEOUT: no node matching text="nothing here" appeared within 800 ms (...)`)
- [x] (插件) 动作: `ui_click(nodeRef|selector|x,y)`, `ui_long_click`, `ui_set_text(nodeRef|selector, text, append)`, `ui_scroll(nodeRef|selector?, direction, times)`, `ui_press_key(key)` (`back` / `home` / `recents` / `notifications` / `quick_settings` / `power_dialog` / `lock_screen`); `nodeRef` 由 `NodeRefRegistry` 重定位后转为 `boundsInside` + 文本 / 类名选择器交给 bridge; 失效返回 `NODE_REF_STALE`; 坐标形式按 D22 受 `ui_gesture` 组开关约束, 组关闭时返回 `TOOL_DISABLED` 并提示改用节点引用. (SOURCE: tools/UiTools.click / longClick / setText / scroll / pressKey 与 `resolveTarget`: nodeRef -> `NodeRefRegistry.resolve` -> `accessibility.findAll(指纹)` (className / id / text / desc + `boundsInside` = 记忆 bounds 每边扩张 min(48 px, 宽或高的一半)) -> 多命中取离记忆 bounds 最近者 -> 精确选择器 (指纹 + `boundsInside` + `boundsContains` = 重定位后的 bounds) 交给 `accessibility.click / longClick / setText / scrollForward / scrollBackward`; selector 形式先 `findOne` 再动作; 未知编号 / 旧快照 / 过期 / 重定位无命中 -> `NODE_REF_STALE` (hint: 重新 dump 或改用 selector); 宿主答 `false` -> `ACTION_FAILED` (附录 A.5 之外, 见 docs/dev/p3-tools.md), `null` -> `NODE_NOT_FOUND`; `ui_set_text(text <= 16 KiB, append)`; `ui_scroll(direction forward / backward / down / up / right / left, times 1..20, 默认目标首个 scrollable 节点, 每次间隔 300 ms, 到底时 `performed < requested` + `atEnd` + hint 而非错误)`; `ui_press_key`: back / home / recents -> `accessibility.*`, notifications / quick_settings / power_dialog / lock_screen -> `keys.*` (权限 `keys`, 宿主 0a453a487 起在 MCP 授权内); 坐标形式 (`x`, `y`) 按 D22 检查 `ui_gesture` 组, 关闭时 `TOOL_DISABLED` 并提示改用 nodeRef / selector, 开启时以零长度 `accessibility.swipe(x, y, x, y, 100 / 700)` 派发 (宿主按压). 证据 (API 28 / 31 / 33 / 35 各一遍): `ui_click {nodeRef "#n9"}` (Button) -> `performed true, via nodeRef, target{ref, className, bounds, center}` 24..160 ms, 随后 `ui_wait_for {text "clicked"}` 6 ms 命中; `ui_long_click #n9` 在 API 28 `performed true`, 在 API 31 / 33 / 35 为 `ACTION_FAILED` 但 `"long clicked"` 已出现 (Android 只在监听器消费事件时报告成功, hint 已说明); `ui_set_text {selector {className "EditText"}, "hello from mcp"}` 与 `{" +1", append true}` -> `ui_find textContains hello` 见 `hello from mcp +1`; `ui_scroll {selector ScrollView, down, 2}` performed 2 / `{up, 5}` (默认 scrollable 目标) performed 2, atEnd true; Settings: `ui_click #n19` (`"Network & internet"` 行的可点击祖先, build/tools/p32_pick_ref.py 取 `id=title` 行) -> `SubSettings`, `ui_press_key back`, `ui_scroll down 2` / `up 8` (RecyclerView / ScrollView, `atEnd true`); 旧快照的 `#n9` -> `NODE_REF_STALE: #n9 is not in the current snapshot s3 (3 nodes) (call ui_dump again ...)`; `nodeRef` 与 `selector` 同给 / 未知键 `textStartsWith` -> `INVALID_ARGUMENTS`; 组关闭时 `ui_click {x, y}` -> `TOOL_DISABLED: ui_click with x and y is a coordinate gesture and the ui_gesture group is switched off (...)`; 组开启后 `ui_click {x, y}` 100 ms (连续两次) / `ui_long_click` 700 ms 均 `performed true, via coordinates`; `ui_press_key` back / home / notifications 均 `performed true`)
- [x] (插件) 手势组 (默认关闭): `ui_swipe(x1,y1,x2,y2,durationMs)`, `ui_gesture(durationMs, points[][])`; 参数在插件侧先按 `MAX_ACCESSIBILITY_GESTURE_DURATION_MS` 与屏幕尺寸校验. (SOURCE: tools/ToolCatalog.uiSwipe / uiGesture (分组 `ui_gesture`, 权限 `accessibility` + `accessibility.gesture`) 与 tools/UiTools.swipe / gesture: `durationMs` 1..10000 (`MAX_GESTURE_DURATION_MS`, 与宿主 `MAX_ACCESSIBILITY_GESTURE_DURATION_MS` 一致, swipe 默认 300), 坐标 0..65535 且经 `device.info` 的 `screen.width` / `screen.height` (缓存 60 s, 宿主未报则不查) 校验在屏内, `points` 2..64 个 `[x, y]`; 宿主答 `false` -> `ACTION_FAILED: the system cancelled the ...`; 组开关经 `ToolPermissionStore` (`tool_groups.json`), `ToolRegistry` 在组变化时整体重注册以保持 `tools/list` 的目录顺序 (`ToolRegistryTest`). 证据 (API 28 / 31 / 33 / 35): 组关闭时 `ui_swipe` -> `TOOL_DISABLED: tool ui_swipe is switched off by the ui_gesture group (...)`; 写入 `{"format":1,"groups":{"ui_gesture":true}}` 后 `ui_swipe` (屏高 3/5 -> 1/5, 400 ms) `performed true` 435..514 ms, `ui_gesture` 3 点 500 ms `performed true` 520..551 ms, `ui_click {x 60000, y 1}` -> `INVALID_ARGUMENTS: ui_click: (60000, 1) is outside the 1096x2560 screen` (API 28 为 720x1280, API 35 为 1800x2880), 单点 `points` -> `INVALID_ARGUMENTS: ui_gesture: points needs at least 2 [x, y] pairs`; 删除开关文件后 `ui_swipe` 立即回到 `TOOL_DISABLED`; 探针 build/tools/p32_tap_probe.sh (API 31): 宿主修复前 8 次手势调用只有新线程上的 200 px swipe 与 700 ms 长按完成, 其余在 30 ms 内 "cancelled" 且 logcat 每次随后出现 `MessageQueue: sending message to a Handler on a dead thread`, 而脚本内 `press` / `click` / `swipe` / `gesture` 全为 true; 宿主 3507afdda 后 8/8 完成 (含两个相同点与 1 px 的 `ui_gesture`, 脚本运行后的再次点击))
- [x] (插件) 无障碍服务未启用时的统一错误 `A11Y_SERVICE_NOT_RUNNING`, 附带提示 (可先调用 `device_ensure_accessibility` 尝试 WSS / root / Shizuku 自动启用, 或提示用户手动开启); Android 13+ 受限设置提示文案. (SOURCE: bridge/ToolFailure.fromBridge: 宿主 `unavailable` 类别在 `accessibility` / `keys` 模块映射为 `A11Y_SERVICE_NOT_RUNNING`, hint = `enable the AutoJs6 accessibility service on the phone (Settings > Accessibility); on Android 13+ open the AutoJs6 app info page and allow restricted settings first`; 宿主 0a453a487 起 `keys.*` 无服务时同样答 `unavailable`; `device_ensure_accessibility` 属 P3.4, 尚不存在, 提示中未提及 (偏差, 见 docs/dev/p3-tools.md). 证据 (4 台设备): `settings put secure enabled_accessibility_services none` 后 `ui_dump {maxNodes 5}` -> `A11Y_SERVICE_NOT_RUNNING: AutoJs6 accessibility bridge requires an enabled accessibility capability provider in the current process. (enable the AutoJs6 accessibility service ...)` 18..92 ms, `ui_press_key notifications` -> 同码 (`AutoJs6 keys bridge requires ...`); 重新开启后 `ui_current_window` 报 `accessibilityAvailable: true`)
- [x] (测试) JVM: `CompactNodeTextTest` (行格式, 截断标记, 转义), `NodeRefRegistryTest` (指纹命中 / 漂移 / 过期), 选择器参数到 `BridgeSelector` JSON 的映射; DEVICE: 设置应用与一个第三方应用上的 dump -> click -> set_text -> scroll -> back 全链路. (SOURCE: JVM 141 项全过 (新增 26): `CompactNodeTextTest` 3 (头行 / 标记顺序 / 转义与 40 字符截断 / 尾行), `NodeRefRegistryTest` 4 (编号与快照替换, 追加, 旧快照与未知编号的 stale 说明, 过期), `UiSelectorsTest` 5 (短 id / 类名转换, 正则与重复键校验, 指纹与精确选择器, bounds 距离与扩张), `UiToolsTest` 12 (dump 文本与引用, nodeRef 重定位取最近者, 失效引用 -> NODE_REF_STALE, 选择器无命中 -> NODE_NOT_FOUND, 动作 false -> ACTION_FAILED, set_text append, scroll 方向 / 次数 / 到底, wait_for appear / disappear / TIMEOUT, press_key 映射含 keys.*, 坐标点击受组开关 -> TOOL_DISABLED, swipe / gesture 校验), `ToolRegistryTest` 1 (组恢复后 `tools/list` 保持目录顺序), `ToolCatalogTest` +1 (12 行 ui 工具的宿主方法 / 权限 / Schema 上限), 目录快照 `tool-catalog.snapshot.json` 20 工具; instrumentation 15 项 (+1 `McpServerHostSessionTest.uiToolsReachTheBrokerWithNodeReferencesAndTheGestureSwitch`: 假 broker 上 dump 信封, nodeRef 重定位 + 精确选择器信封, 失效引用, set_text, scroll, press_key -> `keys.notifications`, 坐标点击 TOOL_DISABLED -> 经 `ToolPermissionStore` 开启 UI_GESTURE 后放行; `McpServerPluginContractTest` / `McpServerSpikeTest` 改为 4 个分组与按开关过滤的工具名) 在 API 28 (G8441 17.2 s) / 31 (XQ-AT72 11.9 s) / 33 (XQ-DQ72 9.6 s, 22120RN86C 18.0 s) / 35 (23046RP50C 11.9 s) 全过 (build/p32-device3-*.log, 最终构建); 宿主 `McpServerPluginRoundTripTest` (hold 420 s, 证据脚本借它保持会话) 在 API 28 / 31 / 33 / 35 均 `OK (1 test)` (build/p32-host-hold-<serial>.log). DEVICE (build/tools/run_p32_evidence.sh <serial> [hold] [local-port], 4 台: API 28 / 31 / 33 / 35, 宿主 3507afdda 的 debug 构建): Settings 应用 + AutoJs6 UI 模式脚本窗口 (build/tools/p32_ui_app.js: 确定性的 EditText / Button / ScrollView, 代替第三方应用) 上 dump -> click(nodeRef) -> set_text -> scroll -> back 全链路, 日志 build/p32-evidence-<serial>.log; lint 0 错误 6 警告 (既有), release 构建通过, `generate_markdown.py --check` MARKDOWN_OK)

### P3.3 截图 (`screen` 组)

- [ ] (插件) `screen_capture(scale|maxWidth, format: jpeg|png|webp, quality, region)`: 经 `accessibility.screenshot` 取 PFD -> 读入 -> base64 -> MCP `image` 内容 + 文本元数据 (尺寸, 字节数, 耗时, 捕获路径); 编码后上限 4 MiB, 超出自动降质并在元数据注明.
- [ ] (插件) MediaProjection 回退: 宿主返回 `fallback: "media_projection"` 时调用 `media_projection.requestScreenCapture` -> `image.captureScreen` (首次需用户授权, 错误提示说明), 成功后缓存授权态.
- [ ] (插件) `screen_state` (`device.isScreenOn` + 尺寸 / 方向 / 密度), 可选 `screen_capture` 的 `annotate: true` (在图上叠加 `#n` 节点编号, 与最近一次 `ui_dump` 对应; 放 P6 视工作量).
- [ ] (测试) DEVICE: API 30+ 免授权路径 (三次连续截图含限频), API 24 / 28 回退路径; 横竖屏; 体积上限触发降质.

### P3.4 文件, 应用与设备 (`files` / `files_delete` / `device` / `shell` 组)

- [ ] (插件) `files_list(path, recursive, maxEntries)`, `files_stat`, `files_read(path, encoding, maxBytes)`, `files_write(path, content, createDirs, overwrite)`, `files_mkdir`, `files_rename`; `files_delete` 单独成组默认关闭; 二进制文件读取返回 base64 并标注.
- [ ] (插件) `editor_open(path, line, column)` (`app.editFile` -> 宿主 `EditActivity.editFileAt`), `app_launch(packageName|appName)`, `app_list(query)` (`package_manager`), `clipboard_get` / `clipboard_set`, `device_info`, `device_ensure_accessibility`, `toast(text)`.
- [ ] (插件) `shell_exec(cmd, root, timeoutMs, maxOutputBytes)` (默认关闭组; `root=true` 需 `shell.root` 令牌且组开关另有 "允许 root" 子开关).
- [ ] (测试) JVM: 路径规范化与逃逸拒绝 (在插件侧也做一遍, 双重防线), 体积上限; DEVICE: 写入后宿主文件管理器刷新可见, 编辑器打开到指定行.

### P3.5 资源与提示

- [ ] (插件) MCP resources: `autojs6://workspace/<相对路径>` (资源模板, 只读, 走 `files_read`), `autojs6://samples/<路径>` (宿主 `sample/` 目录, 经 P1.3 的 `app.listSamples` / `app.readSample`, D20), `autojs6://device/info`, `autojs6://console/tail`; `resources/list` 带 `ttlMs`; 可选 `autojs6://docs/<路径>`: 安装了 `AutoJs6-Plugin-Offline-Docs` 时经宿主转读其 `OfflineDocsPluginContract` 资产, 未安装时不列出 (D20).
- [ ] (插件) MCP prompts: `write_autojs6_script` (AutoJs6 脚本约定速查: 选择器 / 无障碍 / `toast` / `console.log`, 以及 "先 `ui_dump` 再操作" 的推荐流程), `automate_task` (给定目标 -> 观察 / 操作 / 校验循环模板), `debug_selector` (用 `ui_explain_selector` 排查); 提示文本以资源文件维护, 10 语言中至少中英两版.
- [ ] (测试) JVM: 资源 URI 解析与模板匹配; CLIENT_E2E: Claude Code 读取一个样例资源并按 `write_autojs6_script` 提示写出可运行脚本.

验收条件: 附录 A 清单全部工具在 `tools/list` 中出现 (禁用组除外) 且各有一条真机证据; 工具目录快照测试通过; 宿主与插件 changelog 同步.

---

## P4: 宿主开关, 插件设置页与通知

目标: 用户可在宿主抽屉一键开关 MCP 服务器, 在插件设置页完成全部配置, 并能一键复制客户端配置片段.

- [ ] (宿主) `app/tool/McpServerTool.kt` (仿 `JsonSocketServerTool`): `connect` -> 插件状态四分 (未安装: 引导到插件中心 / 官方索引; 已安装未激活或禁用: Wake 与启用引导; 版本不兼容: 显示所需版本; 可用: `openServer`), `disconnect`, `isNormallyClosed` (偏好 `key_$_mcp_server_normally_closed`, 放 `strings_donottranslate.xml` 并排序), 启动时 `connectIfNotNormallyClosed`.
- [ ] (宿主) 抽屉条目 "MCP 服务器": 开关 + 副标题显示端点或状态 (来自 `onStatus`), 长按或齿轮跳转插件设置页 (显式组件, 校验 Activity 受 PLUGIN 权限保护, 仿 `AiPluginSettingsLauncher`); 首次打开时 D18 的信任确认; 字符串 11 语言.
- [ ] (宿主) 插件中心条目的 "设置" 入口指向同一设置页; `PluginCenterItem` 显示运行状态 (可选).
- [ ] (插件) `McpServerSettingsActivity` (Three-Stone-AI 的 `AppSettingsActivity` 样式, 跟随宿主主题 / 夜间 / 语言快照): 状态卡 (运行 / 已停止 / 宿主不可用, 端点列表含 `adb forward` 命令), 端口, 监听范围 (仅本机 / 局域网, 局域网开启时二次确认), 令牌 (显示 / 复制 / 轮换), 已配对客户端 (列表 / 撤销 / 全部撤销), 工具组开关 (含 root 子开关), 开发者模式 (CORS for Inspector, 详细日志), 客户端配置片段 (Claude Code 命令, Cursor `mcp.json`, Codex `config.toml`, 通用 JSON; 一键复制; 二维码可选), 发行历史 (`ReleaseHistoryActivity` 读 `assets/doc/CHANGELOG-{tag}.md`), 关于.
- [ ] (插件) 配置变更热应用: 端口 / 监听范围变更时重启监听并回调状态; 令牌轮换即时生效; 工具组开关触发 `notifications/tools/list_changed` (有状态) 与下次 `tools/list` 的 `ttlMs` 归零.
- [ ] (插件) 无障碍标签, 键盘导航, RTL, 大字体, 夜间模式, 进程恢复 (设置页在进程被杀后重建不丢状态).
- [ ] (测试) JVM: 配置片段生成器快照测试 (三种客户端 + 通用), 状态文案映射; DEVICE: 抽屉开关四态各一次截图证据; 设置页在 API 24 与 API 35 上的显示.

验收条件: 从未安装到可用的四态引导在真机上逐一验证; 抽屉开关与通知 "停止" 双向同步; 配置片段粘贴到 Claude Code / Cursor 后无需修改即可连接.

---

## P5: 连接路径与客户端兼容矩阵

目标: D5 的前三条路径可用并有文档; 主流客户端逐一实测并记录版本与已知问题.

### P5.1 USB 与局域网

- [ ] (文档) USB 路径文档: `adb forward tcp:9637 tcp:9637` (多设备时 `-s <serial>`), 端口占用时改端口, 模拟器同样适用; 插件设置页的端点卡直接给出命令.
- [ ] (插件) 局域网路径: 开启时显示当前 IPv4 (Wi-Fi 变化时刷新), 提示同网段与防火墙; 连接来源为局域网时配对对话框显著标注; 局域网模式下每 24 h 提醒一次 "仍在监听局域网" 的通知 (可关闭).
- [ ] (测试) DEVICE + CLIENT_E2E: 同一台 PC 分别经 USB 与 Wi-Fi 连接同一部手机完成 `ui_dump` + `screen_capture`; 记录延迟 (`ui_dump` 200 节点, `screen_capture` 1280 px JPEG) 作为性能基线.

### P5.2 客户端兼容矩阵

- [ ] (测试) 逐一实测并记录 (客户端, 版本, 传输, 鉴权方式, 结果, 已知问题, 规避): Claude Code (http + header; 若遇到 "头部未在部分路径发送" 的已知 bug, 记录版本并以 stdio 桥接规避), Cursor (`mcp.json` url + headers), Codex CLI (`config.toml` 的 streamable HTTP + bearer 环境变量), MCP Inspector (开发者模式 CORS), VS Code Copilot / Cline / Gemini CLI (至少一个), Claude Desktop (仅 stdio 桥接; 远程连接器留 P8).
- [ ] (文档) README "接入" 章节: 每个客户端一段, 配置片段与设置页一致; "常见问题": 401 / 配对超时 / 宿主不可用 / 无障碍未启用 / 端口占用 / 局域网不可达.

### P5.3 PC 端 stdio 桥接程序

- [ ] (PC) 独立仓库 `D:/idea-projects/AutoJs6-MCP-Bridge` (TypeScript, Node 18+), npm 包 `autojs6-mcp-bridge` (D19): 以 stdio 对接客户端, 以 Streamable HTTP 对接手机; 参数 / 环境变量: `--url`, `--token` / `AUTOJS6_MCP_TOKEN`, `--serial` (自动执行 `adb forward`, 退出时清理), `--protocol` (透传或降级); 令牌不出现在进程参数时优先环境变量; 错误信息可读.
- [ ] (PC) 单元测试 (协议透传, forward 生命周期, 错误映射), `npm pack` 产物体积与依赖审计, README (与插件 README 互链), 版本与插件解耦但两侧 README 记录兼容矩阵.
- [ ] (测试) CLIENT_E2E: Claude Desktop 经桥接程序完成一次工具调用; Claude Code 以 stdio 方式接入作为 http 路径的备选.

验收条件: 兼容矩阵至少 5 个客户端有真实结果; 桥接程序发布到 npm (或以 GitHub Release 附件形式) 并在 README 记录安装命令.

---

## P6: 健壮性, 安全, 性能与一致性

目标: 敌意输入, 资源上限, 进程生命周期与规范一致性都有自动化测试与真机证据; 性能有基线不设阈值.

- [ ] (插件) 敌意输入: 超长 / 嵌套 JSON, 非法 UTF-8, 未知方法, 错误头组合, 重复请求 id, 超大 base64, 并发 64 连接; 全部有界失败且服务器不崩溃 (instrumentation 用 Ktor 客户端压测).
- [ ] (插件) 速率限制: 每客户端每秒请求数与每分钟截图数上限 (`rate-limited` 错误, 含 `retryAfterMs`); 与宿主 grant 的速率双重生效.
- [ ] (插件 + 宿主) 生命周期矩阵: 宿主被杀 / 插件被杀 / 两者同时 / 用户在系统设置强制停止插件 / 令牌轮换中 / 配对进行中 各一次, 期望状态与恢复路径写入 `docs/dev/lifecycle-matrix.md` 并逐项真机验证.
- [ ] (插件) 电量与常驻: 空闲时 CPU 近零 (Ktor 无轮询), 通知常驻; 提供 "空闲 N 分钟自动停止" 可选项 (默认关闭); 记录 1 小时空闲的电量增量.
- [ ] (测试) MCP 一致性: 用官方 conformance 套件 (kotlin-sdk 0.15.0 自带的 conformance 测试或 `modelcontextprotocol/conformance`) 对有状态与无状态两条路径各跑一轮, 记录未通过项与原因.
- [ ] (宿主) `test-apps/mcp-server-conformance` (可选, 与 AI / DEX 家族同形): 确定性假插件 + 恶意 Binder 用例 (错误契约版本, 超限 Bundle, 重复回调, 死亡回调), 加入宿主 `pluginConformanceTestApps`.
- [ ] (插件) 安全审计清单: 令牌存储 / 日志脱敏 (令牌, 文件正文, 截图不进日志) / 导出组件最小化 / `usesCleartextTraffic` 范围说明 / 局域网默认关闭 / 配对撤销 / 工具默认关闭项; 记入 README "安全" 章节.
- [ ] (测试) 性能基线 (只记录不设阈值): `ui_dump` (50 / 200 / 400 节点), `screen_capture` (三档尺寸), `script_run` 往返, 并发 4 请求; 设备 API 24 AVD 与实体机各一组.

验收条件: 敌意输入与生命周期矩阵全部通过; 一致性测试报告与性能基线提交到 `docs/dev/`.

---

## P7: 文档, changelog 与发布 gate

目标: 1.0.0 发布并进入官方插件索引; 宿主随版本发布契约与开关.

- [ ] (文档) README 10 语言 (由 `.readme/*.json` 生成): 简介, 功能 (工具清单从 `ToolCatalog` 生成的表), 安装 (宿主版本要求), 快速接入 (USB / 局域网 / 桥接), 客户端矩阵, 安全, 常见问题, 发行历史, 许可证, 第三方声明; 截图真实有效.
- [ ] (文档) 插件中心说明 (`@raw/plugin_instruction`, 10 语言) 与 `plugin_description`.
- [ ] (插件) `.changelog` 10 语言 `v1.0.0` 条目 (`feature` / `dependency`), 生成 `assets/doc/CHANGELOG-*.md`; `VERSION_NAME=1.0.0` 与文件名断言一致.
- [ ] (宿主) 宿主 changelog 10 语言 (抽屉开关 `feature`), 宿主 `docs/dev/mcp-server-protocol-v1.md` 定稿; 若涉及脚本公开 API (本 Roadmap 默认不涉及) 才同步文档 / d.ts / Ace 仓库.
- [ ] (发布) 验证顺序: `py .python/generate_markdown.py --check`, `:app:testDebugUnitTest`, `:app:assembleDebug :app:assembleDebugAndroidTest`, `:app:lintDebug`, instrumentation (AVD), `:app:appendDigestToReleasedFiles` (签名齐全, 单 APK `autojs6-plugin-mcp-server-v1.0.0-<CRC32>.apk`).
- [ ] (发布) GitHub Release (tag `v1.0.0`, 签名 APK, changelog 文本); `AutoJs6-Official-Plugins-Index` 经 `tools/generate_official_plugin_index.py` 重新生成 (不手改 JSON), 追加 `release-manifests/<packageName>/<versionCode>.json` (真实 sha256 / size / signer); 宿主插件中心可发现并安装.
- [ ] (测试) ColorOS 或同类设备的安装后激活实测 (插件中心出现 "激活", 点击后可用, 重启后仍可发现); 无设备时如实记录 "未执行".
- [ ] (发布) 发布后回填本文件 "当前结论" 表与附录 D 证据; 更新维护者记忆 / 宿主 a11y Roadmap 的跨运行时消费方列表.

验收条件: 官方索引条目可见, 从索引安装的 APK 在两台设备上完成 P2 验收的端到端; 工作树干净, `VERSION_BUILD` 与提交数一致.

---

## P8 (可选): 公网隧道与 OAuth 2.1

目标: 让 Claude.ai / Claude Desktop 自定义连接器 (从 Anthropic 云端发起, 要求公网 HTTPS 与 OAuth) 可用. 不阻塞 1.0.0; 进入前重新评估需求.

- [ ] (插件) 隧道引导: Cloudflare Quick Tunnel / ngrok 的设备端引导 (不内置二进制, 给出命令与自检); HTTPS 由隧道终止; 记录公网 URL 到端点卡.
- [ ] (插件) 内置授权服务器: OAuth 2.1 + PKCE S256, 受保护资源元数据 (`/.well-known/oauth-protected-resource`), Client ID Metadata Documents 优先 (DCR 作为兼容回退), 设备上 2 位数字确认码批准, 可撤销客户端, 令牌短期 + 刷新; 令牌与 Bearer 静态令牌双模并存 (仿 android-remote-control-mcp 的 `bearer_token_enabled` / `oauth_enabled` 双开关, 两者都关时明确警告).
- [ ] (测试) CLIENT_E2E: Claude.ai 自定义连接器完成一次工具调用; 安全审计 (重定向 URI 匹配, `iss` 校验, 状态参数).
- [ ] (文档) 风险说明: 公网暴露设备控制能力的后果, 仅在需要时开启, 用完即关.

验收条件: 至少一个云端客户端经隧道 + OAuth 完成工具调用; 默认关闭且有醒目警告.

---

## 附录 A: 工具清单与 Schema 草案

### A.1 命名与通用约定

- 名称: snake_case, `<组>_<动作>`; 描述用英文 (MCP 客户端与模型消费), 设置页文案 10 语言.
- 输入 Schema: JSON Schema 2020-12, `additionalProperties: false`, 必填最少化; 坐标为设备像素整数; 路径为相对工作目录的 POSIX 风格.
- 结果: 主体为 `text` 内容 (紧凑文本或 JSON 字符串), 需要时附 `structuredContent`; 错误用 `isError: true` + 文本 `code: message (hint)`; 错误码见 A.5.
- 注解: 只读工具 `readOnlyHint: true`; `files_delete` / `shell_exec` / `script_stop_all` 标 `destructiveHint: true`.

### A.2 工具表

| 组 (默认) | 工具 | 关键参数 | bridge 映射 |
| --- | --- | --- | --- |
| script (开) | `script_run` | `source`, `name?`, `workingDirectory?`, `arguments?`, `timeoutMs?=60000`, `waitForCompletion?=true`, `captureConsole?=true`, `maxConsoleLines?=200` | `engines.execScript` (+ captureConsole) |
| script | `script_run_file` | `path`, `arguments?`, 其余同上 | `engines.execScriptFile` |
| script | `script_stop` / `script_stop_all` | `executionId` | `engines.stop` / `engines.stopAll` (宿主进程语义) |
| script | `script_list` | - | `engines.list` |
| script | `console_tail` | `lines?=100`, `sinceId?`, `level?` | `console.tail` |
| ui (开) | `ui_dump` | `format?=text`, `maxDepth?=32`, `maxNodes?=200`, `visibleOnly?=true`, `window?=active` | `accessibility.dump` + 插件压缩 |
| ui | `ui_find` | `selector`, `limit?=10`, `timeoutMs?=0` | `accessibility.findAll` |
| ui | `ui_current_window` | - | `app.currentWindow` |
| ui | `ui_explain_selector` | `selector` | `accessibility.explain` |
| ui | `ui_wait_for` | `selector`, `state=appear|disappear`, `timeoutMs?=10000` | `accessibility.findOne` 轮询 |
| ui | `ui_click` / `ui_long_click` | `nodeRef?` / `selector?` / `x,y?` (三选一) | `accessibility.click` / `longClick` / 坐标点击 (`accessibility.gesture` 组) |
| ui | `ui_set_text` | `nodeRef?|selector?`, `text`, `append?=false` | `accessibility.setText` |
| ui | `ui_scroll` | `nodeRef?|selector?`, `direction=forward|backward|up|down|left|right`, `times?=1` | `accessibility.scrollForward/Backward` |
| ui | `ui_press_key` | `key=back|home|recents|notifications|quick_settings|power_dialog|lock_screen` | `accessibility.back/home/recentApps` 等 |
| ui_gesture (关) | `ui_swipe` | `x1,y1,x2,y2`, `durationMs?=300` | `accessibility.swipe` |
| ui_gesture | `ui_gesture` | `durationMs`, `points[[x,y],...]` | `accessibility.gesture` |
| screen (开) | `screen_capture` | `maxWidth?=1280`, `format?=jpeg`, `quality?=70`, `region?` | `accessibility.screenshot` (回退 `media_projection` + `image.captureScreen`) |
| screen | `screen_state` | - | `device.isScreenOn` + `device.info` |
| files (开) | `files_list` / `files_stat` / `files_read` / `files_write` / `files_mkdir` / `files_rename` | `path`, `recursive?`, `maxEntries?=500`, `encoding?=utf-8`, `maxBytes?=1 MiB`, `content`, `createDirs?`, `overwrite?` | `files.*` |
| files_delete (关) | `files_delete` | `path` | `files.delete` |
| files | `editor_open` | `path`, `line?`, `column?` | `app.editFile` |
| device (开) | `device_info` / `device_ping` / `device_ensure_accessibility` | - | `device.info` / 插件本地 / `accessibility.ensureEnabled` |
| device | `app_launch` / `app_list` | `packageName?|appName?` / `query?` | `app.launchPackage` / `app.launchApp` / `package_manager` |
| device | `clipboard_get` / `clipboard_set` / `toast` | `text` | `clipboard.*` / `toast` |
| shell (关) | `shell_exec` | `cmd`, `root?=false`, `timeoutMs?=15000`, `maxOutputBytes?=64 KiB` | `shell.exec` |

### A.3 资源

| URI | 内容 | 来源 |
| --- | --- | --- |
| `autojs6://device/info` | 设备与宿主信息 JSON | `device.info` |
| `autojs6://console/tail` | 最近 100 行控制台 | `console.tail` |
| `autojs6://workspace/{path}` (模板) | 工作目录文件 (只读) | `files.read` |
| `autojs6://samples/{path}` (模板) | 宿主内置示例脚本 | `app.listSamples` / `app.readSample` (D20) |
| `autojs6://docs/{path}` (模板, 可选) | 离线文档 (需 Offline-Docs 插件, 未安装时不列出) | 宿主转读 (D20) |

### A.4 提示

`write_autojs6_script`, `automate_task`, `debug_selector` (见 P3.5).

### A.5 错误码 (插件层, 与宿主 bridge 分类的映射)

| 插件错误码 | 含义 | 宿主分类 |
| --- | --- | --- |
| `UNAUTHORIZED` (HTTP 401) | 令牌缺失或错误 | - |
| `PAIRING_REQUIRED` / `PAIRING_DENIED` | 未配对 / 被拒绝或冷却中 | - |
| `TOOL_DISABLED` | 工具组被禁用 | - |
| `HOST_UNAVAILABLE` | 宿主未绑定或代理死亡 | `process-dead` / `unavailable` |
| `A11Y_SERVICE_NOT_RUNNING` | 无障碍服务未运行 | `unavailable` (accessibility) |
| `NODE_REF_STALE` / `NODE_NOT_FOUND` | 节点引用失效 / 选择器无命中 | `invalid-request` / 空结果 |
| `CAPABILITY_DENIED` | 超出 grant | `capability-denied` / `permission-denied` |
| `LIMIT_EXCEEDED` / `RATE_LIMITED` | 体积或速率上限 | `resource-limit` / `rate-limited` |
| `TIMEOUT` | 工具超时 | `timeout` |
| `HOST_ERROR` | 其它宿主错误 (透传 name / message) | `provider-failed` / `runtime-error` |

### A.6 grant 允许的 bridge 方法全集

`accessibility.{isEnabled, ensureEnabled, dump, explain, screenshot, findOne, findAll, findByText, click, longClick, setText, scrollForward, scrollBackward, swipe, gesture, back, home, recentApps}`, `engines.{execScript, execScriptFile, list, stop, stopAll}`, `console.tail`, `files.{list, stat, read, write, mkdir, rename, delete}`, `app.{launchPackage, launchApp, isInstalled, editFile, currentWindow, listSamples, readSample}`, `package_manager.{query 类只读方法}`, `clipboard.{getText, setText, hasText}`, `device.{info, isScreenOn, wakeUp, isIgnoringBatteryOptimizations}`, `media_projection.{requestScreenCapture, stop}`, `image.{captureScreen, recycle}`, `shell.exec`, `toast`. 不在此列的模块 (如 `rhino`, `java`, `websocket`, `fetch`, `ui.overlay`, `input_observer`) 一律 `capability-denied`.

### A.7 上限常量 (写入 `McpServerContract`)

| 常量 | 值 |
| --- | --- |
| `MAX_REQUEST_BODY_BYTES` | 1 MiB |
| `MAX_CONCURRENT_TOOL_CALLS` | 4 |
| `DEFAULT_TOOL_TIMEOUT_MS` / `MAX_TOOL_TIMEOUT_MS` | 30 000 / 300 000 |
| `MAX_DUMP_NODES` / `MAX_DUMP_DEPTH` / `MAX_DUMP_TEXT_BYTES` | 400 / 32 / 256 KiB |
| `MAX_SCREENSHOT_ENCODED_BYTES` | 4 MiB |
| `MAX_CONSOLE_TAIL_LINES` / `MAX_CONSOLE_TAIL_BYTES` | 500 / 256 KiB |
| `MAX_FILE_READ_BYTES` / `MAX_FILE_WRITE_BYTES` / `MAX_LIST_ENTRIES` | 1 MiB / 1 MiB (P1.1 由 4 MiB 降低, 不得超过 `MAX_REQUEST_BODY_BYTES`) / 2000 |
| `MAX_SHELL_OUTPUT_BYTES` | 256 KiB |
| `MAX_PAIRED_CLIENTS` | 32 |
| `PAIRING_WINDOW_MS` / `PAIRING_COOLDOWN_MS` | 60 000 / 30 000 |
| `MAX_BRIDGE_INLINE_JSON_BYTES` / `MAX_BRIDGE_PAYLOAD_BYTES` (P1.1 新增) | 512 KiB (Bundle 内联 JSON) / 8 MiB (`ParcelFileDescriptor` 负载) |
| `MAX_ERROR_MESSAGE_BYTES` / `MAX_EVENT_TEXT_BYTES` / `MAX_CLIENT_NAME_BYTES` (P1.1 新增) | 4 KiB / 2 KiB / 256 |

---

## 附录 B: 节点树紧凑格式草案 (D12)

```
window: com.android.settings/.Settings$WifiSettingsActivity  size=1080x2400  nodes=37/37
#n1  FrameLayout                                  [0,0][1080,2400]
#n2   RecyclerView scrollable                     [0,220][1080,2400]
#n3    LinearLayout clickable id=recycler_item    [0,220][1080,376]
#n4     TextView "Wi-Fi"                          c=(540,270)
#n5     Switch checked clickable id=switch_widget c=(980,298)
#n6    LinearLayout clickable                     [0,376][1080,532]
#n7     TextView "HomeNet-5G" desc="Connected"    c=(430,430)
... (truncated: 12 more nodes beyond maxNodes)
```

规则:

- 每节点一行: `#n<序号>` + 缩进 (深度) + 简短类名 (去包名) + 状态标记 (`clickable` / `checkable` / `checked` / `scrollable` / `editable` / `focused` / `!enabled`, 只列真值) + `"text"` + `desc="..."` + `id=<去包名的 id>` + 位置 (容器给 `[l,t][r,b]`, 叶子给中心点 `c=(x,y)`).
- 文本超过 40 字符截断加 `...`; 换行 / 引号转义; 不可见节点默认省略.
- 头行给出窗口包名 / 活动名, 屏幕尺寸, `已列/总计` 节点数; 截断时给出剩余数量与提示 (缩小 `maxDepth` 或用 `ui_find`).
- `snapshotId` 随结果返回 (文本头行不含, 放在结构化字段); 节点引用在下一次 `ui_dump` 或 30 s 后失效; 动作时以指纹 (类名 + 文本 + 描述 + id + 边界的容差匹配) 重定位, 命中多个时取边界最接近者.
- `format="json"` 返回 `{window, nodes:[{ref, depth, className, text, desc, id, bounds, clickable, ...}]}`, 供程序化消费; `format="xml"` 直接返回宿主 uiautomator 风格导出 (兼容既有工具链).

---

## 附录 C: 待决事项

进入对应阶段前由维护者拍板, 结果回填第 1 节. 已决事项 (2026-09-07): Q1 -> D10 (保持 Bundle + JSON), Q2 -> D16 (端口 9637), Q3 -> D19 (独立仓库 `AutoJs6-MCP-Bridge`, npm `autojs6-mcp-bridge`), Q4 -> D15 (不提供开机自启), Q5 -> D20, Q6 -> D21, Q7 -> D22 (均按建议). 已决事项 (2026-09-10): Q8 -> D9 修订 (有状态优先, 无状态等待 SDK). 当前无待决事项.

### Q1 (P1 前): 控制面信封是否改用 TaggedWire

背景. D10 选择 Bundle + JSON (与 Node 家族一致, 数据面负载即 bridge JSON). 宿主 AI / Python / Lua / DEX / YOLO 家族使用 `protocol-wire-api` 的 TaggedWire 二进制信封, 有 golden corpus 与 fail-closed 校验, 更适合安全敏感契约.

建议: 保持 Bundle + JSON. 理由: MCP 代理的每个请求都是现成的 `NodeBridgeRequest` JSON, 宿主侧零转换; 安全边界由 grant 与调用方校验承担而非信封; 控制面字段少 (端口 / 范围 / 状态). 若维护者希望与新家族统一为 TaggedWire, 则 P1.1 改为 `byte[]` + `SCHEMA_*` 并增加 codec 测试, 工作量约增加一个会话.

结论 (2026-09-07): 保持 Bundle + JSON -> D10.

### Q2 (P2 前): 默认端口是否为 8347

建议: 8347 (原 D16). 备选: 7348 (紧邻 VSCode 通道, 易混淆, 不推荐), 8080 (常被占用).

结论 (2026-09-07): 维护者指定 9637 -> D16.

### Q3 (P5.3 前): 桥接程序的包名与仓库位置

建议: 放在本仓库 `bridge/` 目录, npm 包名 `autojs6-mcp-bridge` (或 scope 形式 `@supermonster003/autojs6-mcp-bridge`); 版本独立于插件. 备选: 独立仓库 `AutoJs6-MCP-Bridge`.

结论 (2026-09-07): npm 包名 `autojs6-mcp-bridge`, 建立独立仓库 `AutoJs6-MCP-Bridge` -> D19.

### Q4 (P4 前): 是否提供开机自启

建议: 1.0.0 不做 (D15). 若做, 只在插件侧提供 "开机后自动启动服务器" 开关, 并要求宿主也在运行 (宿主未运行时进入 `HOST_UNAVAILABLE` 等待), 默认关闭.

结论 (2026-09-07): 不提供 -> D15.

### Q5 (P3.5 前): 示例脚本与离线文档资源的来源

背景. 宿主 `sample/` 目录以 assets 形式随宿主打包 (`ExplorerSamplePage`); 离线文档在 `AutoJs6-Plugin-Offline-Docs` (契约 `OfflineDocsPluginContract`, 资产根 `docs`, 清单 `offline-docs-inventory-v1.txt`).

建议: 示例经宿主 bridge 新增只读方法 `app.listSamples / app.readSample` 暴露 (P1.3 顺带); 离线文档资源作为可选项, 安装了 Offline-Docs 插件时由宿主转读 (宿主已有该插件的读取路径), 未安装时 `resources/list` 不列出 `autojs6://docs/`. 若维护者认为文档资源价值高 (模型据此写出正确脚本), 可提升为 P3.5 必做.

结论 (2026-09-07): 按建议 -> D20.

### Q6 (P6 后): 是否实现 MCP Tasks 扩展承载长脚本

建议: 暂不. 同步等待 + 进度通知 + `script_stop` 已覆盖; Tasks 扩展 (`io.modelcontextprotocol/tasks`, 轮询 `tasks/get`) 在客户端普遍支持后再评估.

结论 (2026-09-07): 按建议 -> D21.

### Q7 (P3.2 前): 坐标点击是否归入 `ui_gesture` 组

背景. 宿主 `NodeBridgePermissionManifest` 刻意把坐标手势 (`accessibility.gesture`) 与普通无障碍分开; `ui_click(x, y)` 底层是手势.

建议: `ui_click` 的 `nodeRef` / `selector` 形式属 `ui` 组 (默认开), 坐标形式在 `ui_gesture` 组关闭时返回 `TOOL_DISABLED` 并提示改用节点引用; 这样默认配置下模型只能点击可定位的节点, 减少误触.

结论 (2026-09-07): 按建议 -> D22.

### Q8 (P2.1 前): 2026-07-28 无状态模型是否纳入 1.0.0

背景. P0.2 实测 SDK 0.15.0 不支持 2026-07-28 模型, 也不存在单端点双模 (见 D9).

结论 (2026-09-10): 有状态优先; 无状态模型等待 SDK 支持后再考虑, 不纳入 1.0.0 -> D9 修订.

---

## 附录 D: 证据等级与退路

### D.1 证据等级

| 标签 | 可以证明 | 不能证明 |
| --- | --- | --- |
| `SOURCE` | 源码存在, 结构符合设计 | 编译或行为正确 |
| `JVM` | Android-free 逻辑的单元测试 (JUnit4, 无 Robolectric) | Binder / 网络 / 真机行为 |
| `ANDROID_BUILD` | `assembleDebug` / `testDebugUnitTest` / `lintDebug` 通过 | 真机行为 |
| `BINDER` | 指定设备上的 instrumentation: 发现, 绑定, 往返, 敌意输入 | 网络与客户端行为 |
| `DEVICE` | 指定设备与 API 级别上的服务器 + 宿主真机行为 (curl / Inspector) | 未列出设备 / API 级别 |
| `CLIENT_E2E` | 指定 PC 客户端 (名称 + 版本) 经指定路径完成真实工具调用 | 其它客户端 / 版本 |
| `DOCS` | README (10 语言), changelog, 协议文档, 索引已同步且版本号已更新 | - |
| `RELEASE` | 签名 APK, CRC32 文件名, GitHub Release, 官方索引 receipt | 未明确覆盖的设备 / 客户端 |

条目勾选时在其后追加证据, 格式示例: `[x] ... (JVM: PairingGateTest; DEVICE: Xiaomi 23046RP50C / API 35, 2026-09-xx; CLIENT_E2E: Claude Code 2.1.x via adb forward; commit abc1234)`.

### D.2 D4 退路: 自研最小实现

触发条件见 P0.2 决策点. 形态: `java.net.ServerSocket` 或 Ktor CIO 去掉 SDK, 手写 JSON-RPC 2.0 分派, Streamable HTTP 的 POST 单端点 (JSON 响应 + 按请求 SSE 流), 有状态 (`initialize` / `Mcp-Session-Id`) 与无状态 (`server/discover` / 头校验 / `resultType`) 两套最小方法集, 工具 / 资源 / 提示三类 `list` / `call` / `read` / `get`; 一致性用官方 conformance 套件把关. 体积最小, 但要自行跟进规范演进; `ToolCatalog` / `PairingGate` / `HostBridgeClient` 等纯逻辑组件不受影响.

---

## 附录 E: MCP Client 插件预留 (D1)

- 仓库名: `AutoJs6-Plugin-MCP-Client`; `applicationId` `io.github.supermonster003.autojs6.plugin.mcp.client`; 契约模块 `plugin-api/mcp-client-api`, action `org.autojs.plugin.MCP_CLIENT`.
- 能力: 脚本侧 `mcp.connect(url|command, options)` 得到客户端句柄, `tools()` / `call(name, args)` / `resources()` / `read(uri)` / `prompts()`; 与宿主 `ai.*` 的工具调用归一化对接 (`ai.chat` 的 tool-call 事件可路由到 MCP 服务器的工具并回填结果), 形成 "AutoJs6 脚本 = 代理编排层" 的用法.
- 边界: 客户端凭据 (Bearer / OAuth) 留在插件进程 (与 AI Provider V2 的 `PLUGIN_MANAGED` 凭据模式一致); 宿主只传目标标识与非敏感元数据; 出站网络策略 (允许的 origin 列表) 由插件声明并由宿主校验.
- 与本插件的关系: 复用 `AutoJs6-MCP-Bridge` 的协议实现经验与 `ToolCatalog` 的 Schema 处理; 两者可同时安装, 互不依赖.
- 涉及脚本公开 API (`runtime/api/augment/mcp/`), 届时必须同步文档 / d.ts / Ace LSP 三个关联仓库.

---

## 附录 F: 参考

- 宿主: `docs/dev/accessibility-automation-roadmap.md` (格式与 a11y 能力来源), `docs/dev/python-runtime-protocol-v1.md` (协议文档骨架), `docs/dev/plugin-protocol-wire-v1.md`, `docs/dev/official-plugin-settings-contract-v1.md`, `engine/NodeBridgeProtocol.kt`, `engine/NodeJsHostCapabilityBroker.kt`, `core/plugin/AidlPluginHost.kt`.
- 插件生态: `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md`, `AutoJs6-Plugin-Three-Stone-AI` (设置页 / 发行历史 / Roadmap 风格), `AutoJs6-Plugin-Python-Runtime` (AAR 锁定, 宿主调用方校验, 长时执行), `AutoJs6-Plugin-Three-Ember-Player` (前台服务 Manifest), `AutoJs6-Official-Plugins-Index` (索引生成器与 release manifest).
- MCP: 规范 `2026-07-28` 及其 changelog, Streamable HTTP 传输页, MRTR 模式页; Kotlin SDK `modelcontextprotocol/kotlin-sdk` (releases, README 的 `mcpStreamableHttp` 示例); 客户端文档 (Claude Code MCP, Cursor MCP, Codex CLI 配置).
- 同类项目 (设计参照): `danielealbano/android-remote-control-mcp`, `zerotap-app/android-mcp-server`, `mobile-next/mobile-mcp` (ADB 侧对照).

---

## 会话记录

### 2026-09-07

- 完成: 宿主插件架构 / 可复用自动化与网络表面 / 插件仓库约定三项探查; MCP 规范与 Kotlin SDK 现状核实; 维护者拍板 D1-D8 (范围, 架构, 工具面, SDK, 连接路径, 安全模型, 界面分工, 落盘); 派生 D9-D18; 本 Roadmap 初稿.
- 未做: 仓库骨架与 `git init` (P0.1), SDK spike (P0.2), 任何宿主改动.
- 下次会话建议起点: P0.1 全部条目 + P0.2 spike; spike 通过后同一会话可开始 P1.1 契约模块.

### 2026-09-08

- 完成: P0.1 全部条目 (仓库骨架, 宿主注册契约, 10 语言资源与文档生成, 测试与 CI, 共 5 个提交); P0.2 spike 全部条目 (SDK 0.15.0 + Ktor CIO 的 `device_ping` 端点与前台服务, API 24 / 28 / 35 三台设备, release R8 验证, Inspector 2.5.0 与 Claude Code 2.1.257 真实调用, `docs/dev/p0-spike-evidence.md`); D4 成立, D9 定稿为 `/mcp` 只走有状态; P0 关闭.
- 未做: 任何宿主改动 (P1); 交互式 Claude Code `/mcp` 状态页未验证; CI 工作流尚未在 GitHub 上运行 (仓库未推送).
- 待维护者拍板: 2026-07-28 无状态模型是否纳入 1.0.0 (见 D9 的 P0.2 实测段与 P2.1 首条).
- 下次会话建议起点: P1.1 契约模块 (宿主仓库), 参照 `8e25c22e5` / `b07a09890` 模板; P1.2 能力代理可与之同会话.
- (同日续) 完成: P1.1 契约模块 (宿主 commit b63cca493: 模块, 5 个 AIDL, 常量, 7 项 JVM 测试) 与 P1.2 (宿主 commit 423678165: `McpServerPluginHost`, `McpServerSessionController`, `McpCapabilityGrant`, `McpHostCapabilityBroker`, `McpServerConfig` / `McpServerBundles`, 插件中心三处注册与 Manifest `<queries>`, 17 项 JVM 测试, 宿主 changelog 10 语言 feature 条目); `AndroidNodeBridgeCapabilityProvider` 增加可注入 `permissionManifest`.
- 未做: P1.3 bridge 新方法与 P1.4 协议文档; `McpServerPluginHostTest` (假 Binder) 因宿主 JVM 测试约束不可行, 改由 P1 验收的 Binder 往返与 P2 真实插件覆盖.
- 下次会话建议起点: P1.3 (宿主仓库, bridge 新方法, 建议按 `accessibility.*` -> `files` -> `console` -> `engines` / `app` / `device` 的顺序逐项提交), 随后 P1.4.
- (同日续 2) 完成: P1.3 全部条目 (宿主 4 个提交: f034dc895 `accessibility.dump` / `explain` / `screenshot` + `app.currentWindow` + broker 的一次性负载描述符; 3ee2abebe `files` 模块; 7464f70c3 `console` 模块 + `captureConsole` / `waitMs` + 控制台 4096 条软上限; ee8bd83de `engines.list` / `stop` / `stopAll(scope host)` + `device.info` + `app.listSamples` / `readSample`; 新增 JVM 测试 27 项, changelog `improvement` 4 条) 与 P1.4 全部条目 (协议文档 `docs/dev/mcp-server-protocol-v1.md`, `accessibility-architecture.md` 的 MCP 行, 宿主 versionCode 5279 与 `REQUIRED_HOST_VERSION_CODE` / 插件 `REQUIRED_HOST_VERSION` 回填; 宿主 commit c02f4ed6d); 宿主 `assembleAppDebug` + 全量 `testAppDebugUnitTest`: 通过 (2478 项, 0 失败, 5 跳过; 5 个 debug APK 生成; 宿主 build/p14-full.log).
- 偏差: `engines.stopAll` 的宿主语义经 `{scope: "host"}` 选项区分 (未新增方法名); `captureConsole` 对所有引擎均为共享 `GlobalConsole` 上的 id 窗口 (`captureMode = "global-window"`), 因为 `ScriptRuntime.console` 就是 `AutoJs.globalConsole`, 不存在按执行的控制台代理; `files.write` / `rename` / `delete` 的开关参数收进 options 对象 (`encoding` / `createDirs` / `overwrite` / `append` / `recursive`); 新方法的 JSON 层无 JVM 测试, 由 Binder 往返与真实插件覆盖.
- 未做: P1 验收的 Binder 往返 (`openServer` -> `dispatch(accessibility.dump)` -> `onResponse`) 尚未进行, P1 验收保持打开, 留待 P2 真实插件 (或 `test-apps` 风格假插件) 完成; Node 插件仓库需运行 `verifyNodeHostCapabilityManifestAlignment` 并为 `files` / `console` 模块与新增的 engines / app / device 方法补 JS 导出与文档; `NodeBridgeConformanceInstrumentationTest` 未在真机运行; d.ts / 文档仓库暂无变化 (新方法仅在 bridge 层, 不涉及脚本公开 API).
- 下次会话建议起点: P2.1 (插件仓库: 接入宿主 `openServer` 会话与 grant, 把 MCP 工具映射到 bridge 方法), 首先用真实插件完成 P1 验收的 Binder 往返并回填本文件.

### 2026-09-10

- 维护者拍板: D9 修订为有状态优先, 无状态模型等待 SDK 支持后再考虑 (Q8); P1 验收的 Binder 往返改用 P2 真实插件完成.
- 完成: P2.1 全部条目 (插件 commit 0f2d112): `store/ServerConfig` + `ServerConfigStore`, `server/` 下的 `HostHeaders` / `RequestGate` / `McpTransport` / `McpServerFactory` / `BindFailures` / `ServerStatus` / `LanAddressWatcher` / `McpHttpServer`, `McpServerService` 改读存储配置并移除 `EXTRA_STATELESS`; JVM 测试 35 项通过 (新增 28 项, 含 Ktor `testApplication` 上的真实 SDK 传输测试); `testDebugUnitTest` 35 项通过, `lintDebug` 0 错误 (6 条既有的依赖版本 / 图标提示), `assembleDebug` / `assembleDebugAndroidTest` / `assembleRelease` (R8) 通过, `generate_markdown.py --check` 通过; 设备: `McpServerSpikeTest` 5 项 + `McpServerPluginContractTest` 3 项在 4 台设备全部通过 (Sony G8441 API 28, Sony XQ-AT72 API 31, AVD sdk_gphone64_x86_64 API 33, Xiaomi 23046RP50C API 35; `device_ping` 往返 19..88 ms; 日志 build/p21-sony28.log / p21-sony.log / p21-avd33.log / p21-pad.log); 首轮设备运行暴露并修复两处真实缺陷: CIO accept 协程的 `BindException` 在 Android 上杀死进程 (现由 `serverConfig { parentCoroutineContext = CoroutineExceptionHandler }` 接住), 以及 API 31+ 上 `stopService()` 紧跟 `startForegroundService()` 触发 `ForegroundServiceDidNotStartInTimeException` (测试与 KDoc 改为经 `ACTION_STOP` 停止, `onDestroy` 同步停止监听器).
- 偏差: 请求体上限由请求门实现 (先读声明长度, 再把 POST 正文按 1 MiB + 1 读入并经 receive pipeline 回放给 SDK), 因为 SDK 公开的 `mcpStreamableHttp` 不暴露 `maxRequestBodySize` 且 `RequestBodyTooLargeException` 为 internal; 请求级超时留给 P2.3 的工具超时 (CIO 只有连接空闲超时); 无状态路径与 2026-07-28 模型不再实现; instrumentation 用 `HttpURLConnection` / 原始 socket 而非 Ktor 客户端; 无会话 GET 与会话上限仍留给 P2.2 鉴权层.
- 未做: P2.2 (令牌 / 配对 / 鉴权层), P2.3 (工具目录, 宿主桥接, 真实 Binder, `device_info` / `script_run`) 与 P1 验收的 Binder 往返; `mcp-server-api` AAR 尚未入 `libs/`.
- 下次会话建议起点: P2.2 (TokenStore / AuthInterceptor / PairingGate / PairedClientStore / PairingConfirmActivity, 鉴权层挂在请求门之后), 随后 P2.3 并顺带完成 P1 验收.

### 2026-09-10 (续)

- 完成: P2.2 全部条目 (插件 commit a88dd7c): `server/` 下的 `BearerTokens` / `McpErrors` / `AuthInterceptor` / `PairingGate` / `PairingInterceptor` / `PairingCoordinator`, `store/` 下的 `ProcessSharedFile` / `TokenStore` / `PairedClientStore` / `PairedClientCodec` (`ServerConfigStore` 改用同一文件存储), `ui/` 下的 `PairingConfirmActivity` / `PairingDecisionReceiver`, `McpHttpServer` 挂载鉴权与配对并公开 `pairingGate`, `McpServerService.dump` 打印状态 (开发者模式下含令牌); 11 个 locale 的 `pairing_*` 字符串; JVM 测试 60 项通过 (新增 25 项); `testDebugUnitTest` 60 项通过, `lintDebug` 0 错误 (6 条既有的依赖版本 / 图标提示; `java.util.Base64` 的 NewApi 错误已改为自写 base64url), `assembleDebug` / `assembleDebugAndroidTest` / `assembleRelease` (R8) 通过, `generate_markdown.py --check` 通过; 设备: `McpServerSpikeTest` 7 项在 4 台设备全部通过 (Sony G8441 API 28, Sony XQ-AT72 API 31, Sony XQ-DQ72 API 33, Xiaomi 23046RP50C API 35), 最终构建上 API 28 / 31 / 35 再跑 7 项 + `McpServerPluginContractTest` 3 项通过; 配对后 `device_ping` 往返 7..62 ms; 开发者模式 `dumpsys activity service` 在 API 31 打印状态 / 指纹 / 令牌 / 已配对数, 非开发者模式与停止后分别只打印指纹 / 无服务; 日志 build/p22-inst-*.log.
- 偏差: 配对指纹不含令牌指纹 (与 "令牌轮换不清除配对" 一致, 对话框显示令牌尾 4 位); 设置页的令牌显示 / 复制 / 轮换推迟到 P4.2, 期间用开发者模式 + `dumpsys` 读取; 三个存储从 `SharedPreferences` 改为 `noBackupFilesDir/mcp_server/*.json` 的原子文件 (跨进程一致性, 首轮设备运行时发现测试进程与 `:mcp_server` 进程各自缓存); SDK 的 `Server.sessions` 以自身会话 id 为键而非 `Mcp-Session-Id`, 身份解析改为按传输的 `sessionId` 查找; 未申请 `USE_FULL_SCREEN_INTENT`.
- 未做: P2.3 (工具目录, 权限存储, 宿主桥接, 真实 `McpServerPluginService` Binder, `device_info` / `script_run`, `mcp-server-api` AAR 入 `libs/`) 与 P1 验收的 Binder 往返; 配对事件转发到宿主 (`PairingCoordinator.EventListener` 已预留).
- 下次会话建议起点: P2.3 (先把宿主 `:plugin-api:mcp-server-api` 的 AAR 复制进 `libs/` 并加锁, 再实现 `HostBridgeClient` 与真实 Binder, 用 `openServer -> dispatch(accessibility.dump) -> onResponse` 顺带完成 P1 验收并回填 P1 记录).

### 2026-09-10 (再续)

- 完成: P2.3 全部条目 (插件 commit d6d9a10): 宿主 `mcp-server-api` release AAR 入 `libs/` 并在 `locks/host-api-aars.lock` 加锁 (宿主 HEAD a37bbd2ae, 6.8.0 / 5279); `bridge/` (`BridgeEnvelope` / `BridgeTransport` / `BinderBridgeTransport` / `HostBridgeClient` / `ToolFailure`), `tools/` (`ToolGroup` / `ToolSpec` / `JsonSchemas` / `ToolCatalog` / `ToolPermissions` / `ToolPermissionStore` / `ToolArguments` / `ToolResults` / `ToolRegistry` / `CatalogToolExecutor` / `ScriptRunTool`), `host/` (`HostCallerVerifier` / `SessionConfig` / `HostBundles` / `HostSession` / `McpServerRuntime`), `server/ToolGateInterceptor`, 真实 `McpServerPluginService` Binder, `McpServerService` 改为运行时的前台壳 (通知含端点 / 宿主状态 / 已配对数 / 停止动作, toast 降级), `McpHttpServer` 增加工具安装器 / 工具门 / 端点列表 / 会话计数 / 客户端名查询, 能力 Bundle 增加契约版本 / 工具分组 / 协议版本 / SDK 版本; 11 个 locale 的 `server_*` 字符串; `docs/dev/p2-host-bridge-and-tools.md`; JVM 测试 105 项通过 (新增 45 项); JVM `:app:testDebugUnitTest` 105/105; `generate_markdown.py --check` MARKDOWN_OK (10 languages, 36 artifacts); `:app:lintDebug` 0 errors / 6 warnings (既有 GradleDependency / IconDuplicatesConfig / NewerVersionAvailable); `:app:assembleDebug` + `:app:assembleDebugAndroidTest` + `:app:assembleRelease` 通过; 设备: 2026-09-10 插件 instrumentation 14/14 (`McpServerPluginContractTest` 3 + `McpServerHostSessionTest` 4 + `McpServerSpikeTest` 7) 于 Sony G8441 API 28 (`BH900ASK9E`, 10.9 s), Sony XQ-AT72 API 31 (`QV710AF65F`, 7.3 s), Sony XQ-DQ72 API 33 (`QV770340J7`, 6.2 s), Xiaomi Pad 23046RP50C API 35 (`968e9f18`, 7.6 s), 每台先 `adb install -r -t` 插件 debug + androidTest APK 再按类运行; 宿主往返: 宿主 `McpServerPluginRoundTripTest` 1/1 于同 4 台设备 (API 28 / 31 / 33 / 35): `open` -> running (绑定 879 / 253 / 136 / 272 ms) + hostAvailable, 插件日志 `Host probe ok` 115 / 39 / 43 / 48 ms (broker `dispatch(device.info)` -> `onResponse`), `refreshStatus` -> `stop` (stopped, port 0) -> `close`; 宿主 debug APK 6.8.0 / 5279 由 HEAD a37bbd2ae + 控制器修复 (94f65d301) 构建; 客户端: API 31 `QV710AF65F` 经 `adb forward tcp:9637` 的 PC stdlib 客户端 (`build/tools/mcp_client_e2e.py`, 未入库): initialize 296 ms -> tools/list 82 ms [device_ping, device_info, script_run] -> device_info 回 PAIRING_REQUIRED 轮询, 手机通知的 Allow 动作确认 (`Client paired`) -> device_info ok 91-112 ms (host 6.8.0 / 5279, XQ-AT72, a11y 未运行) -> script_run(toast + console.log) outcome=success, finished, waited 147-802 ms, 控制台 6 行含 `hello from mcp`, 任务名 `$engine/p23_e2e.js` -> script_run(throw) outcome=exception, 控制台含 `Error: boom`; 杀宿主 (`am force-stop org.autojs.autojs6`) -> `dumpsys` hostSession host=degraded, device_info -> `HOST_UNAVAILABLE` 37 ms 附提示; 宿主重开会话 -> 新会话替换旧会话 (host=available), device_info ok 80 ms; developer mode 下 token 只经 `dumpsys` 打印到 adb shell.
- P1 验收回填: Binder 往返用 P2.3 真实插件完成 (宿主仓库新增 `McpServerPluginRoundTripTest`; 宿主 androidTest 源集在 HEAD 上另有两处与本次无关的控制台测试编译错误, 构建时临时移开后复原). 设备链顺带暴露并修复宿主 `McpServerSessionController` 的状态回退竞态: 插件在 `openServer` 返回前就经回调送达 `running`, 宿主随后用 `getStatus()` 的旧快照覆盖 (API 33 上 `first { isRunning }` 等满 30 s); 现在拉取的快照只在拉取期间没有回调状态到达时才发布 (宿主 commit 94f65d301).
- 偏差: 探测与工具走 `device.info` / `engines.execScript` 而非 `accessibility.dump`; `already_open` 只在前一宿主仍存活时返回, 宿主已死则换代理复用监听器; `invalid-request` 类别映射到新增的 `INVALID_ARGUMENTS` 码 (附录 A.5 之外); `script_run` 的等待上限 290 s (契约 300 s 减去 10 s bridge 宽限); 通知未加 "打开设置" 动作 (P4.2); 长脚本进度通知按 5 s 一条 (P3.1 再调到 2 s 并附最近一行输出).
- 未做: P3.1 剩余项 (`script_run_file` / `script_stop` / `script_stop_all` / `script_list` / `console_tail`, 进度通知节奏); P2 验收中 AVD API 24 的客户端路径 (杀宿主 -> `HOST_UNAVAILABLE` -> 宿主重开会话恢复已在 API 31 实体机验证, 见客户端证据; 宿主进程崩溃后的自动重绑由 `McpServerSessionController` 的 1 / 3 / 9 s 重试覆盖, 本次以测试进程重开会话代替).
- 下次会话建议起点: P2 验收补完 (AVD API 24 客户端路径), 然后 P3.1 (`script_run` 完整参数已在 P2.3 落地: source / name / workingDirectory / arguments / timeoutMs / waitForCompletion / captureConsole / maxConsoleLines).

### 2026-09-10 (三续)

- 完成: P3.1 全部条目 (插件 commit f314ec5): 脚本分组 (`script_run` 完整结果形状, `script_run_file`, `script_stop`, `script_stop_all`, `script_list`, `console_tail`), 2 s 进度心跳带最新控制台行, 以及为此新增的 SSE 响应挂载 `server/SseStreamableMount` (SDK 0.15.0 的 `mcpStreamableHttp` 固定 JSON 响应, 与请求关联的通知只能投到独立 GET 流, 无 GET 流即丢弃); P2 验收补完 (AVD API 24 与 API 31 的 Inspector / Claude Code 客户端路径, AVD 上的杀宿主恢复, 通知 STOP 动作). JVM 115 项, 插件 instrumentation 14 项 + 宿主往返 1 项在 API 24 / 28 / 31 / 33 / 35 全过, lint 0 错误 6 警告, release 构建通过.
- 偏差: 无 `timeout` 状态 (等待到期报 `running` + `hint`); `exception` 只有 message 与行号; `script_stop` 对已结束的 id 返回宿主的 `INVALID_ARGUMENTS` 而非 `stopped: false`; UI 模式脚本的停止为异步, 紧随其后的 `script_list` 仍可能列出它; 有状态挂载改为 SSE 响应后, `initialize` / `tools/*` 的 POST 响应为 `text/event-stream` (MCP 规范要求客户端同时接受 JSON 与事件流), 网关 / 鉴权 / 配对拒绝仍为 JSON; Ktor 测试引擎的 `respond` 在响应体写完前就返回, 路由因此显式等待写完 (CIO 上会阻塞到写完).
- 未做: P3.2 及以后; 宿主设置开关与 "宿主开关归位" (P4); 宿主崩溃后的自动重绑仍以测试进程重开会话代替 (P4.1).
- 下次会话建议起点: P3.2 (无障碍 UI 观察与操作, `ui` / `ui_gesture` 组).

### 2026-09-11

- 完成: P3.2 全部条目 (插件 commit 811781d; 宿主 commit 0a453a487 (维护者提交: `accessibility.dump` 状态标记 `scrollable` / `checkable` / `checked` / `editable` / `focused` / `selected` / `longClickable`, MCP 授权加入 `keys.notifications` / `quickSettings` / `powerDialog` / `lockScreen`, `keys.*` 无服务时答 `unavailable`, 起止点相同的 `accessibility.swipe` 按压派发) 与 3507afdda (bridge 手势结果改投主循环 handler)): `ui` 组 10 个工具与 `ui_gesture` 组 2 个工具 (目录 20 工具, 快照更新), `ToolFlow` 多次宿主调用的工具流 (经 `CatalogToolExecutor` 的进度心跳运行), `NodeRefRegistry` (每进程一份快照, 60 s, 指纹重定位), `UiSelectors` (D13 方言 + 短 id / 类名便利), `CompactNodeText` (附录 B), `ToolFailure` 新增 `ACTION_FAILED` / `NODE_REF_STALE` / `NODE_NOT_FOUND` / 坐标 `TOOL_DISABLED` 工厂; JVM 141 项, 插件 instrumentation 15 项在 API 28 / 31 / 33 / 33 / 35 全过, 真机全链路证据 4 台 (API 28 / 31 / 33 / 35), lint 0 错误 6 警告, release 构建通过; docs/dev/p3-tools.md 增加 P3.2 节; changelog 10 语言 feature 条目 (含宿主版本要求).
- 发现并修复: (1) 宿主 bridge 的 `GlobalActionAutomator` 无 handler 时在调用线程上 `Looper.prepare()` 并在首个手势后 `quit()`, 复用的 broker 派发线程上此后所有手势立即返回 `false`, 真正的回调投到死线程 (logcat `sending message to a Handler on a dead thread`); 首轮真机证据表现为坐标点击随机 `ACTION_FAILED`, 先误判为系统取消零长度手势 (宿主 0a453a487 内的 press 派发因此保留但归因改正), 用 build/tools/p32_tap_probe.sh 对比 8 种路径后定位, 宿主 3507afdda 改用主循环 handler (与 `PythonHostCapabilityBroker` 一致); (2) `ToolRegistry` 逐个增删工具使组恢复后 `tools/list` 顺序偏离目录 (真机合同测试暴露), 改为整体重注册; (3) 证据脚本: 并行设备共用 `tcp:9637` 转发互相覆盖 (改为每次运行一个本机端口), `:mcp_server` 冷启动超过 3 s 时 `dumpsys` 取不到 token 与宿主附着在 API 28 需 8 s (改为轮询), `am force-stop` 宿主后只写一次无障碍设置可能 "enabled but not running" (改为 off / on 并轮询 `accessibilityAvailable`), 手势坐标按 `wm size` 计算.
- 偏差: 引用存活 60 s (草案 30 s); `ACTION_FAILED` 在附录 A.5 之外; `ui_wait_for` 到期为 `TIMEOUT`, 而 `ui_find` 无命中与 `ui_scroll` 到底是正常结果; 坐标点击 = 零长度 `accessibility.swipe` -> 宿主按压 (bridge 无 tap 方法); `ui_long_click` 在 UI 模式脚本按钮上于 API 31 / 33 / 35 报 `ACTION_FAILED` 但处理器已执行 (Android 仅在监听器消费事件时报告成功, hint 说明); 第三方应用以 AutoJs6 UI 模式脚本窗口代替 (确定性), Settings 为系统应用; `device_ensure_accessibility` 属 P3.4, `A11Y_SERVICE_NOT_RUNNING` 的提示未提及它; 插件 `REQUIRED_HOST_VERSION` 仍为 5279, 旧宿主表现为 `ui_press_key` 的 `CAPABILITY_DENIED`, 无状态标记与随机 `ACTION_FAILED` (docs/dev/p3-tools.md 与 changelog 记录).
- 未做: P3.3 (`screen` 组) 及以后; `device_ensure_accessibility` (P3.4); 真正的第三方应用真机证据; 宿主 `UiObject.kt` 中另一处无 handler 的 `GlobalActionAutomator` (脚本线程, 未改).
- 下次会话建议起点: P3.3 (`screen` 组: `screen_capture` 经 `accessibility.screenshot` 的 PFD 路径 -> base64 `image` 内容, MediaProjection 回退, `screen_state`; 体积上限与降质).
