# P0.2 SDK 可行性 spike 证据

日期: 2026-09-08. 对应 `ROADMAP.md` P0.2 与决策 D4 / D9. 本文只记录实测数据与结论, 步骤与命令可在 AGENTS.md 第 9 / 15.2 / 17 节找到.

## 1. 结论

- D4 成立: 官方 Kotlin SDK (`io.modelcontextprotocol:kotlin-sdk-server` 0.15.0) + Ktor 3.5.1 CIO 在 `minSdk 24` 上可构建, 可运行, R8 后可用, 体积增量 0.90 MiB (阈值 6 MiB), 附录 D.2 退路不启用.
- D9 定稿: SDK 0.15.0 的 `mcpStreamableHttp` (有状态) 与 `mcpStatelessStreamableHttp` (无会话) 是两种互斥的路由挂载, 都只接受协议版本 2024-11-05 / 2025-03-26 / 2025-06-18 / 2025-11-25, 不支持 2026-07-28 无状态模型. `/mcp` 只挂有状态传输, 这是 1.0.0 的验收主线.
- 三种 PC 客户端各完成一次真实 `device_ping` 调用: HTTP 探针 (Python urllib), MCP Inspector 2.5.0 (`--cli`), Claude Code 2.1.257 (print 模式 + `--mcp-config`).

## 2. 构建

| 项目 | 结果 |
|---|---|
| 工具链 | Gradle 9.5.0, AGP 9.3.2, Kotlin 2.3.20, R8 8.13.19 (平台版本插件 1.7.4 自动选择), JDK 21 |
| SDK 传递依赖 | `kotlin-sdk-core` 0.15.0, Ktor 3.5.1 (`server-core` / `server-sse` / `server-content-negotiation` / `server-websockets` / `serialization-kotlinx-json`), `kotlinx-serialization-json` 1.11.0, `kotlinx-coroutines-core` 1.11.0, `kotlinx-io-core` 0.9.1, `kotlinx-collections-immutable` 0.5.1, `kotlin-logging` 8.0.4 (Android 变体), `slf4j-api` 2.0.18 (无绑定) |
| Kotlin 元数据 | SDK 以 Kotlin 2.4.0 编译, `kotlin-stdlib` 解析为 2.4.0; 2.3.20 编译器直接读取, 无需 `-Xskip-metadata-version-check` |
| debug APK | 1,080,031 -> 5,150,613 字节 |
| release APK (R8 + 资源收缩) | 169,209 -> 1,115,117 字节 (+0.90 MiB), `classes.dex` 2,018,628 字节 |
| R8 规则 | 只需 `-dontwarn java.lang.management.ManagementFactory` 与 `-dontwarn java.lang.management.RuntimeMXBean` (Ktor 启动耗时统计的受保护调用点); 缺少时 `minifyReleaseWithR8` 以 "Missing class" 失败, 不会产出过期 APK 之外的任何提示, 因此 AGENTS.md 第 17 节把 `assembleRelease` 列为依赖变更后的必做验证 |
| 16 KB 页 | 不适用: release APK 无 `lib/` 目录 |
| Lint | 5 个 warning (GradleDependency x2, NewerVersionAvailable, IconDuplicatesConfig x2), 0 error |

## 3. 设备矩阵

| 设备 | API | 构建 | 启动方式 | 绑定耗时 | 结果 |
|---|---|---|---|---|---|
| AVD `Android SDK built for x86` (emulator-5554) | 24 (7.0) | debug | instrumentation / `am startservice` | 227 ms / 671 ms (冷进程) | 通过 |
| Sony G8441 | 28 (9) | debug + release | instrumentation / `am start-foreground-service` | 894 ms (debug 冷) / 183 ms (release) | 通过 |
| Xiaomi 23046RP50C (HyperOS) | 35 (15) | debug | instrumentation / `am start-foreground-service` | 1562 ms (冷) / 259 ms (二次) | 通过, 见 6.1 |

instrumentation `McpServerSpikeTest` (2 用例) 在三台设备上全部通过; 进程内 `tools/call device_ping` 往返 16 / 99 / 84 ms (API 24 / 28 / 35).

## 4. PC 端延迟 (经 `adb forward`, 本机回环)

| 设备 | `initialize` (首个请求) | `tools/call device_ping` (稳定后) |
|---|---|---|
| AVD API 24 (debug) | 232 ms | 15 ~ 23 ms |
| Sony API 28 (release) | 197 ms | 33 ~ 39 ms |
| Xiaomi API 35 (debug) | 428 ms | 65 ~ 74 ms |

## 5. 内存 (`dumpsys meminfo <pkg>:mcp_server`, 服务器空闲)

| 设备 | 构建 | TOTAL PSS | Dalvik Heap |
|---|---|---|---|
| AVD API 24 | debug | 26.4 MB | 9.4 MB |
| Sony API 28 | release | 14.7 MB | 5.6 MB |
| Xiaomi API 35 | debug | 58.7 MB | (HyperOS 大堆) |

## 6. 运行时行为

### 6.1 前台服务与 adb 控制面

- `McpServerService` 以 `android.permission.DUMP` 守卫导出, adb shell 可启停; `am startservice` (API 24) 与 `am start-foreground-service` (API 26+) 均验证通过, `STOP_SERVER` 后 `dumpsys activity services` 无残留记录, 端口释放.
- HyperOS (Xiaomi, API 35): 新安装应用的 `RUN_ANY_IN_BACKGROUND` app-op 默认为 `ignore`, 此时来自 shell 的前台服务启动被 `ActivityManager` 以 "Background start not allowed" 拒绝 (`startFg?=true` 也不豁免); `cmd appops set <pkg> RUN_ANY_IN_BACKGROUND allow` 后正常. 这是设备策略而非插件缺陷, P2.4 需检测该状态并在 P4.2 的设置页给出跳转提示; 测试后已把该 app-op 恢复为 `ignore`.
- API 34+ 首次启动打印 "Foreground service started from background can not have location/camera/microphone access", 与 `specialUse` 类型无关, 可忽略.
- instrumentation 进程内 `startForegroundService` 在三台设备上都不受后台限制影响.

### 6.2 SDK 0.15.0 协议行为 (HTTP 探针)

`mcpStreamableHttp` (有状态, `/mcp` 的正式挂载):

- `initialize` (2025-06-18) -> 200 `application/json`, 返回 `Mcp-Session-Id` (UUID), `serverInfo.name = mcp-server`, `capabilities.tools.listChanged = false`.
- `notifications/initialized` -> 202 无正文; `tools/list` / `tools/call` -> 200 `application/json` (单响应不走 SSE). P3.1 起插件改用自有挂载 (`server/SseStreamableMount`), 带请求的 POST 一律以 `text/event-stream` 应答, 见 `docs/dev/p3-tools.md`.
- 带会话的 GET -> 200 `text/event-stream` (独立通知流); DELETE -> 200, 之后同一会话 -> 404 `-32000 Session not found`.
- 无会话的任何请求 (含 `server/discover`, 含 `Mcp-Method` / `Mcp-Name` 头) -> 400 `-32000 Bad Request: Server not initialized`; 无会话 GET -> 200 `text/event-stream` (SDK 行为, 与规范期望的 400 不同, P2.1 鉴权层会先于它拦截).
- `MCP-Protocol-Version: 2026-07-28` 的 `initialize` -> 400 `Unsupported protocol version (supported versions: 2025-11-25, 2025-06-18, 2025-03-26, 2024-11-05)`.

`mcpStatelessStreamableHttp` (无会话, 仅用于本次评估, 通过 `--ez stateless true` 挂载):

- 不发 `Mcp-Session-Id`; `initialize` 可选, 无会话直接 `tools/list` / `tools/call` -> 200; GET / DELETE -> 405 `Method not allowed`.
- 任何携带 `MCP-Protocol-Version: 2026-07-28` 的请求 -> 400 Unsupported protocol version; `server/discover` 亦然. 即它是 2025 版规范的 "无会话" 形态, 不是 2026-07-28 无状态模型.

### 6.3 客户端

| 客户端 | 版本 | 路径 | 结果 |
|---|---|---|---|
| HTTP 探针 (Python urllib) | Python 3.13 | `adb forward tcp:9637 tcp:9637` | 有状态全链路 + 无状态形态各一轮, 见 6.2 |
| MCP Inspector | 2.5.0 (`npx -y @modelcontextprotocol/inspector --cli --transport http`) | 同上 | `tools/list` 返回 `device_ping`, `tools/call device_ping` 返回载荷 |
| Claude Code | 2.1.257 (`claude -p --mcp-config <json> --strict-mcp-config --allowedTools mcp__autojs6__device_ping`, http 传输 + `Authorization: Bearer test` 头) | 同上 | 2 轮内调用 `device_ping` 并原样返回 JSON 载荷 (API 7.1 s) |

Claude Code 未使用 `claude mcp add` (会写入维护者的全局配置), 以临时 `--mcp-config` 文件等价验证; 交互式 `/mcp` 状态页未验证.

## 7. 遗留与后续

- 无会话 GET 返回 200 事件流以及 `mcpStreamableHttp` 的会话上限 / 空闲回收策略需在 P2.1 用鉴权层与会话管理覆盖. P2.1 (2026-09-10) 结论: 请求门只做 Host / Origin / 请求体校验, 无会话 GET 与会话上限留给 P2.2 的鉴权层.
- HyperOS 后台限制的用户提示 (P2.4 / P4.2).
- `EXTRA_STATELESS` 仅为评估开关, P2.1 定稿后移除或改为内部测试专用. P2.1 (2026-09-10) 已移除: `/mcp` 只挂有状态传输, 无状态模型等待 SDK 支持 (D9 修订).
