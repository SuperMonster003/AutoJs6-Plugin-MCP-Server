# AutoJs6-Plugin-MCP-Server AGENTS.md

本文件是本仓库的工程约定, 由 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` (AutoJs6 新插件仓库参考规范) 裁剪而来, 只保留对本仓库真实有效的条款. 路线图与阶段性决策见 `ROADMAP.md`; 本文件描述的是 "怎样改仓库", 路线图描述的是 "改什么".

## 1. 规则等级与本仓库的适用范围

- `MUST`: 必须遵循. `SHOULD`: 默认遵循, 偏离时在仓库文档中说明原因. `CONDITIONAL`: 仅在对应能力落地后适用.
- 用户在当前任务中的明确要求优先于本文件.
- 本仓库不包含原生库, 模型, 上游源码快照或 ABI 拆分, 参考规范中对应的 CONDITIONAL 条款不适用 (见第 5.4 节的省略理由).
- 本仓库会拥有联网监听 (MCP 端点), 独立设置页与内置发行历史 (路线图 P4), 这些条款以 CONDITIONAL 形式保留在第 9 节与第 13 节.

## 2. 仓库身份

下列值在 Gradle, Manifest, Kotlin 常量 (`McpServerPlugin`), 资源, 文档, 测试和宿主注册信息中 MUST 完全一致. 修改任一值时同步修改全部位置, 并运行 `ManifestContractTest` 与 `McpServerPluginRuntimeInfoTest`.

| 项目 | 值 |
|---|---|
| 仓库与目录名 | `AutoJs6-Plugin-MCP-Server` |
| `rootProject.name` | `autojs6-plugin-mcp-server` |
| 应用标题 (不可翻译) | `MCP Server` |
| `applicationId` / namespace | `io.github.supermonster003.autojs6.plugin.mcp.server` |
| 插件 ID / engine / variant | `mcp-server` / `mcp-server` / `default` |
| Binder 服务类 | `McpServerPluginService` (进程 `:mcp_server`) |
| 服务发现 action / category | `org.autojs.plugin.MCP_SERVER` / `mcp-server` |
| INFO 服务 | `McpServerPluginInfoService`, action `org.autojs.plugin.INFO`, category `mcp-server` |
| 专用 API | `mcp-server-api` (宿主 `plugin-api/mcp-server-api`, AIDL 包 `org.autojs.plugin.mcp.server.api`, 路线图 P1.1 落地后以 AAR 形式进入 `libs/`) |
| 最低宿主 versionCode | `McpServerPlugin.REQUIRED_HOST_VERSION` (当前 5279, 即交付 MCP 契约模块与能力代理的宿主构建, 路线图 P1.4) |
| 默认端点 | `http://127.0.0.1:9637/mcp` (仅回环; 端口为路线图决策 D16) |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.7.4 |
| 发布文件名 | `autojs6-plugin-mcp-server-v{VERSION_NAME}-{CRC32}.apk` (单 APK) |

## 3. 工作区与提交

### 3.1 会话开始

- MUST 运行 `git status --short`, 检查当前分支, 最近提交和相关文件差异.
- MUST 将已有未提交内容视为用户工作. 不覆盖, 不回滚, 不擅自整理与当前任务无关的改动.
- 禁止使用 `git reset --hard`, `git checkout -- <path>` 或其他可能丢失用户内容的命令, 除非用户明确授权.
- 先阅读 `ROADMAP.md` 的 "阶段总览" 与最后一条 "会话记录", 从路线图建议的起点开始.

### 3.2 开发过程

- 每个行为改动应同时考虑实现, 测试, 10 语言资源, README, changelog, 宿主入口和公共契约.
- 不提交本地缓存, IDE 状态, 调试输出或无意生成的二进制文件.
- Gradle 自动修改 `BUILD_TIME` 时, 在确认来源后与相关变更一并处理. 若 Gradle 修改 `VERSION_BUILD`, 必须按第 3.4 节的提交计数规则校正; `VERSION_NAME` 只按语义化版本规则调整.
- 修改第三方依赖时同步记录版本, 来源, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`), 并在 changelog 的 `dependency` 分类记录.
- 路线图条目完成后在 `ROADMAP.md` 勾选并写入证据 (设备, API, 客户端版本, 度量值), 不勾选没有证据的条目.

### 3.3 提交

- 除非用户明确要求本次会话不要提交, 会话结束前 MUST 将本次范围内的全部文件按逻辑提交, 一个路线图子项一个提交.
- 使用 Conventional Commits 风格: `feat:`, `fix:`, `docs:`, `build:`, `test:`, `ci:`, `chore:`, 可加作用域, 例如 `feat(server): ...`.
- 一个提交表达一个完整意图; 行为实现, 对应测试和对应 changelog 通常放在同一提交.
- 提交前 MUST 审阅 `git diff --check`, `git diff --cached`, `git status --short`, 确认没有密钥, 本地路径, 临时 APK 或无关改动.
- 会话结束时最终 `git status --short` 无输出; 若发现无法纳入本次提交的用户改动, 停止自动提交并向用户说明.

### 3.4 提交计数

- `VERSION_BUILD` MUST 与当前分支 `HEAD` 可达的 Git 提交数一致.
- 每次准备新提交时, 先用当前提交数加 1 得到即将产生的 build number, 写入 `version.properties`, 再把该文件与本次逻辑改动一并提交. 不要先写成当前提交数再提交.

```bash
next=$(( $(git rev-list --count HEAD 2>/dev/null || echo 0) + 1 ))
sed -i "s/^VERSION_BUILD=.*/VERSION_BUILD=$next/" version.properties
```

最后一笔提交完成后 MUST 验证 `VERSION_BUILD == git rev-list --count HEAD` 且 `git status --short` 无输出. 若发现不一致, 将 `VERSION_BUILD` 设置为 "当前提交数 + 1" 并创建一笔有明确含义的校正提交.

### 3.5 版本名称

- `VERSION_NAME` 从 1.0.0 开始, 按语义化版本管理, 与提交数量不绑定.
- 修改 `VERSION_NAME` 时同步更新全部 changelog JSON 的版本 key, README, 发布文件名断言与测试夹具, 再运行文档生成器.

## 4. 仓库结构

```text
AutoJs6-Plugin-MCP-Server/
|-- .changelog/                 lang_*.json x 10 + template_changelog.md (文案源)
|-- .github/workflows/          build.yml, markdown.yml
|-- .python/                    generate_markdown.py (+ .bat), check_markdown.bat, generate_launcher_icons.py
|-- .readme/                    common.json, lang_*.json x 10, template_readme.md, template_plugin_instruction.md, README-*.md (生成)
|-- app/
|   |-- sm003.jks               本地签名密钥, Git 忽略
|   `-- src/{main,test,androidTest}
|-- build-logic/                org.autojs.build.{utils,versions,signs,jvm-convention,...} 约定插件
|-- docs/dev/                   阶段证据与开发笔记 (按需)
|-- gradle/                     libs.versions.toml, wrapper/
|-- libs/                       宿主 API AAR (哈希锁定, 见 libs/README.md)
|-- locks/                      host-api-aars.lock
|-- AGENTS.md, ROADMAP.md, README.md (生成, 简体中文), LICENSE (MPL-2.0), THIRD_PARTY_NOTICES.md
|-- build.gradle.kts, settings.gradle.kts, gradle.properties, version.properties
`-- sign.properties             本地签名配置, Git 忽略
```

不要仅为目录整齐创建空模块. 桥接程序 (路线图 P5.3) 位于独立仓库 `AutoJs6-MCP-Bridge`, 不放入本仓库.

## 5. Gradle 与版本平台

### 5.1 在线平台版本插件

- MUST 使用在线仓库中的 `io.github.supermonster003.autojs6-platform-versions` (当前 1.7.4). 升级时先确认新版本已能从公共仓库解析, 并与其他官方插件仓库统一升级.
- 禁止使用 `mavenLocal()`, 禁止本地平台版本实现, 禁止提交 `gradle/data` 消费端覆盖.
- 平台插件只在根 `settings.gradle.kts` 应用一次, 且整个 `plugins` 块位于 `includeBuild("build-logic")` 之前; `build-logic/settings.gradle.kts` 不应用它.
- 根 `build.gradle.kts` 用 `System.getProperty("gradle.agp.version")` 等属性声明模块实际使用的插件并 `apply false`; 模块只应用插件, 不硬编码版本. 版本逃生门只用 `version.properties` 的 `OVERRIDDEN_*`, 常规构建保持 `NONE`.
- `app` 模块从 `version.properties` 和 `org.autojs.build.versions` 读取 compileSdk, minSdk, targetSdk, versionCode, versionName.
- 不声明 `org.jetbrains.kotlin.android`; Kotlin 支持由 AGP 内置能力与约定插件提供.

验收命令 (模拟 GitHub Actions 的 Temurin 环境, 日志 MUST 只有一段 `Version information for IDE platform and Gradle plugins`):

```powershell
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' :app:assembleDebug :app:testDebugUnitTest
```

### 5.2 仓库边界

- Gradle 构建 MUST 自包含. 禁止引用仓库外部的 JAR, AAR, `flatDir` 或兄弟项目路径 (例如 `../AutoJs6/...`).
- 宿主 API AAR MUST 复制到 `libs/` 并在 `locks/host-api-aars.lock` 记录小写 SHA-256; `app/build.gradle.kts` 在配置期校验文件存在, 非 debug 命名, 哈希匹配, 锁文件键集合精确. 更新 AAR 时同步更新锁文件, `THIRD_PARTY_NOTICES.md` 与契约测试.
- 宿主与插件需要同步更新时分别修改各仓库 (宿主 `D:/idea-projects/AutoJs6`), 不通过跨仓库相对路径制造隐式耦合.

### 5.3 签名与发布构建

- `sign.properties` 与 `app/sm003.jks` 从宿主复制到相同相对路径, MUST 保持被 Git 忽略 (`git check-ignore` 验证). 仓库中不得出现密码, token, 私钥或开发者绝对路径.
- 保留 `org.autojs.build.signs`, `signingConfigs` 与 release 签名选择逻辑.
- `appendDigestToReleasedFiles` 任务 MUST 保留该名称, 依赖 `assembleRelease`, 在签名缺失时失败, 校验实际 APK 集合恰为 `autojs6-plugin-mcp-server-v{VERSION_NAME}.apk`, 并追加 CRC32 生成 `autojs6-plugin-mcp-server-v{VERSION_NAME}-{CRC32}.apk` 到 `releases/` (不入库).

### 5.4 不启用 ABI 拆分的理由

插件完全由 Kotlin 字节码与普通资源构成 (MCP SDK, Ktor 与 kotlinx-serialization 均为纯 JVM 库), 拆分包内容实质相同, 不会带来下载或兼容性收益. 因此:

- 不配置 `splits.abi`, 不配置 `ndk.abiFilters`, 每次发布只有一个 APK.
- `getInfo()` MUST 显式写有 `supportedAbis = emptyArray()`, 测试断言其为显式空数组.
- 16 KB page size 检查不适用; 若未来引入含原生库的依赖, 本节作废并需补齐 ABI 与 16 KB 验证.

## 6. Manifest 与激活协议

- Manifest MUST 声明 `org.autojs.permission.PLUGIN`, `<queries>` 宿主包名, `org.autojs.plugin.WAKE_ACTIVITY` 与 `org.autojs.plugin.info.AUTHOR` meta-data.
- `WakeActivity` MUST 为 `exported=true`, `Theme.NoDisplay`, `excludeFromRecents`, `finishOnTaskLaunch`, 受 PLUGIN 权限保护, 响应 `org.autojs.plugin.action.WAKE` + DEFAULT category, 启动后立即结束, 不做任何副作用.
- `McpServerPluginInfoService` 与 `McpServerPluginService` MUST `exported=true`, 受 PLUGIN 权限保护, 声明 `requiresHostVersion` meta-data (与 `McpServerPlugin.REQUIRED_HOST_VERSION` 一致); 后者固定运行在 `:mcp_server` 进程, HTTP 监听与前台服务都放在该进程.
- 所有对外组件逐项审查 `android:exported`; 除契约入口外不得导出其他组件. 独立设置页 (P4) 若需被宿主打开, 使用 PLUGIN 权限保护的显式 action.
- `android:usesCleartextTraffic="true"` 只服务于插件自己的回环 / 局域网 HTTP 监听, Manifest 注释 MUST 保留该说明; 插件不得发起任何出站明文请求.
- 权限清单只包含 PLUGIN, INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, POST_NOTIFICATIONS; 新增权限必须在 README 安全章节与 changelog 说明理由.
- 在 ColorOS 等会保持新装应用停止状态的设备上 SHOULD 做真实激活验收; 未执行时在路线图如实记录 `未执行真实设备激活验证`.

## 7. PluginInfo 与能力协商

- `McpServerPluginRuntimeInfo` 是纯数据映射, `McpServerPluginInfo.kt` 负责 Android 侧读取 (包版本, 本地化描述, `@raw/plugin_instruction`, 构建日期), 二者的分离 MUST 保持, 以便 JVM 测试覆盖映射.
- `name` 与不可翻译的 `app_name` 一致; `description` 来自当前 locale 的 `plugin_description`; `versionName` / `versionCode` 来自 `PackageInfo`; `versionDate` 来自 `plugin_version_date` (`MMM d, yyyy`, `GMT+08:00`); `id` / `engine` / `variant` 与第 2 节一致.
- `capabilities` 至少包含 `PluginCapabilityKeys.REQUIRES_HOST_VERSION` (Long). 路线图 P1 起追加契约版本与可选能力集合, 宿主先读取能力再调用新方法, 不通过捕获异常猜测协议版本.

## 8. Binder 与公共 API

- 公共常量, option key, capability key, ID, action 和 category MUST 集中在宿主 `mcp-server-api` 契约模块 (AAR) 与 `McpServerPlugin` 中, 禁止散落字符串字面量.
- 宿主到插件的能力代理使用 Bundle + JSON 请求 / 响应 (路线图 D10); 所有 Binder 输入 MUST 做边界校验 (长度, 大小, key, 枚举, 索引), 上限常量集中定义并与路线图附录 A 一致.
- 已发布 AIDL 演进时保持旧 transaction 顺序, 末尾追加, 通过契约版本协商; 破坏性重设计同步升级宿主与插件.
- 不在 Binder 主路径执行无界网络访问或不可取消的长耗时初始化; 服务被回收, 首次绑定, 重复绑定和并发调用都应保持确定行为.

## 9. MCP 服务器专属约束 (CONDITIONAL, 随路线图 P2 起生效)

- 默认只监听 `127.0.0.1:9637`; 局域网监听 MUST 是显式的用户开关, 默认关闭, 并在界面上显示实际地址.
- 每次请求 MUST 校验 Bearer 令牌; 令牌只存于插件私有存储, 不写入日志, 通知正文, 截图或 Bundle 之外的任何地方; 首次配对需在手机上确认.
- 工具按分组提供开关; Shell, 文件删除, 坐标手势等危险分组默认关闭. 工具实现只能通过宿主能力代理执行, 不得在插件内复制宿主功能.
- 不记录脚本正文, 文件内容, 节点树或截图到普通日志; 诊断日志只保留大小, 耗时和错误分类.
- 服务器只在用户开启时运行 (前台服务 + 通知), 不自动启动 (路线图 D15), 关闭后释放端口与 Binder 引用.
- 请求门 (P2.1 起): `/mcp` 之前的 `RequestGate` 负责 Host / Origin 校验与 1 MiB 请求体上限, SDK 内建的 DNS rebinding 校验关闭 (允许列表在运行期随局域网地址变化). 回环模式只接受 `localhost` / `127.0.0.1` / `[::1]`, 局域网模式加入当前 IPv4 与 `ServerConfig.extraAllowedHosts`; 浏览器来源一律拒绝, 只有开发者模式经 CORS 放行回环来源. MUST NOT 放宽为任意 Host, 默认开启 CORS, 或绕过请求门挂载其它路由.
- 鉴权与配对 (P2.2 起): `AuthInterceptor` 挂在请求门之后, `PairingInterceptor` 挂在鉴权之后, 二者都在 SDK 传输之前; 未配对客户端只能 `initialize` / `ping` / 各类 `list`, `tools/call`, `resources/read`, `resources/subscribe`, `prompts/get` 受 `PairingGate` 门控 (60 s 确认窗口, 30 s 冷却, 最多 32 个客户端), 客户端指纹 = 规范化客户端名 + 地址类别 (不含令牌, 轮换令牌不清除配对). 令牌, 配置与已配对客户端存放在 `noBackupFilesDir/mcp_server/` 下经 `ProcessSharedFile` 原子替换并跨进程加锁的 JSON 文件中, MUST NOT 改回 `SharedPreferences` (按进程缓存, 常驻的 `:mcp_server` 进程读不到设置页的写入). 令牌只经 `TokenStore` 读取, 通知与对话框最多显示尾 4 位; P4.2 设置页落地前, 开发者模式下的 `dumpsys activity service .McpServerService` 是 adb 控制面读取令牌的唯一途径, 非开发者模式只打印指纹.
- 开发期 adb 控制面 (P0.2 起生效): `McpServerService` 以 `android.permission.DUMP` 守卫导出, 只有 adb shell 与系统能经 `am start-foreground-service` (API 24 / 25 用 `am startservice`) 携带 `action.START_SERVER` / `action.STOP_SERVER` 启停它; MUST NOT 为其它调用方放宽该权限或改为无守卫导出. 宿主与插件自身界面运行在同一 UID, 无需该权限.

## 10. 主项目职责

若改动同时需要修改 `D:/idea-projects/AutoJs6`, MUST 遵循:

- 宿主只保留入口 (抽屉开关, 插件中心注册, 能力代理), 插件拥有服务器的真实实现; 插件未安装或被禁用时宿主不得提供重复的完整实现.
- 宿主先区分 `未安装`, `已安装但未激活或禁用`, `版本不兼容`, `调用失败`, `可用`, 各状态有对应提示与引导 (安装来源, 激活按钮, 所需版本).
- 更新包名, action, category, ID 或 API 时同步检查宿主注册表, ProGuard/R8, 安装 URL, 启用状态缓存和测试夹具.
- 涉及公开脚本 API 时再同步 `AutoJs6-Documentation`, `AutoJs6-TypeScript-Declarations`, `AutoJs6-Plugin-Offline-Docs`, `AutoJs6-Plugin-Ace-Editor`.

## 11. 应用标题与字符串资源

- 用户可见字符串 MUST 覆盖 `values`, `values-en`, `values-ar`, `values-es`, `values-fr`, `values-ja`, `values-ko`, `values-ru`, `values-zh`, `values-zh-rHK`, `values-zh-rTW`; `values` 与 `values-en` 共有条目内容一致, 各语言占位符与转义一致.
- `app_name` 位于 `strings_donottranslate.xml` 且 `translatable="false"`; `plugin_author`, `plugin_id`, `plugin_engine`, `plugin_variant`, `plugin_version_date` 由 Gradle `resValue` 生成.
- 每个 locale MUST 有 `plugin_description`: 简洁说明能力, 句尾不加终止标点, 不写 "文件管理器插件" 前缀, 不写 "适用于 AutoJs6" 等限定表述.
- `<string>` 按 `name` 升序; plurals 与数组放入各自文件.
- 所有资源与文档字符串使用 ASCII 标点 (`, . : ; ! ? ( ) [ ] / -`), 省略号用 `...` 并加 `tools:ignore="TypographyEllipsis"`; 禁止全角标点, 顿号, 弯引号. `ApplicationTextPunctuationTest` 会扫描 `app/src/main`, `.readme`, `.changelog`, `README.md` 与 `ROADMAP.md`.

### 11.1 启动器图标

- `app/src/main/res/mipmap/ic_launcher.png` 与 `mipmap-night/` 变体, adaptive 图层由 `.python/generate_launcher_icons.py` 确定性生成; 修改图标时修改脚本并重新生成, 不手工改 PNG.
- 图标语义为 MCP 端点 (带端口的节点框 + `MCP` 字样), 不沿用其他插件的图案或颜色身份; 背景色与 `values*/ic_launcher_background.xml` 保持一致.

## 12. README 与多语言生成

- README, 插件中心说明 (`raw*/plugin_instruction.md`) 与 changelog MUST 由 `.readme/*.json`, `.changelog/*.json` 与模板通过 `.python/generate_markdown.py` 生成; 生成产物不得手工编辑.
- 修改 JSON 或模板后先运行 `py .python/generate_markdown.py`, 再运行 `py .python/generate_markdown.py --check` (CI `markdown.yml` 也会执行). 生成器校验语言集合, JSON 键与列表形状, 全角符号, 未替换占位符, 版本对齐, 孤儿产物与漂移.
- 根 `README.md` 是简体中文版本, 与 `.readme/README-zh-Hans.md` 同源; 语言导航必须出现 `简体中文`.
- README 先说明用户能完成什么, 再说明安装与连接; 不写 Android Studio 或 IntelliJ IDEA 版本信息, 不向普通用户解释 `supportedAbis`, 签名过程等实现细节. 服务器尚不可用时 README 与插件说明 MUST 如实标明当前状态.
- README 链接必须指向本仓库的真实 release, issue, license 与生成 changelog.

## 13. Changelog

- `.changelog/` 只存放 10 个 `lang_*.json` 与模板; 生成的多语言 changelog 位于 `app/src/main/assets/doc/`.
- 涉及 `feature`, `fix`, `improvement`, `dependency` 的提交 MUST 更新当前 `VERSION_NAME` 对应 `vX.Y.Z` 条目的全部语言 JSON, `released_date` 更新为当日 `YYYY/MM/DD`.
- 分类 key 只用 `hint`, `feature`, `fix`, `improvement`, `dependency`; 标签沿用既有固定翻译 (简体中文 `提示`, `新增`, `修复`, `优化`, `依赖`; 英文 `Hint`, `Feature`, `Fix`, `Improvement`, `Dependency`; 其他语言见现有 JSON).
- `feature` 条目不以 `新增` 开头, `fix` 条目不以 `修复` 开头; `dependency` 只记录 Gradle 依赖变化, 使用 `附加`, `升级`, `移除` 等固定动作词.
- 与 AutoJs6 GitHub Issue 有关时按既有格式写明 Issue 引用.

## 14. 独立界面, 设置与发行历史 (CONDITIONAL, 路线图 P4)

- 设置页 SHOULD 跟随宿主的语言, 夜间模式和主题色 (`AutoJs6HostSettingsContract`), 宿主配置不可用时安全回退.
- 设置页 MUST 提供独立的 `发行历史` 入口, 按当前 locale 读取 `doc/CHANGELOG-{LANGUAGE_TAG}.md`, 找不到时回退英语.
- 所有界面覆盖无障碍标签, RTL, 大字体, 夜间模式与进程恢复; 令牌展示界面提供复制与轮换, 不在截图或最近任务缩略图中泄露令牌.

## 15. 测试要求

### 15.1 JVM 单元测试 (`app/src/test`)

- `McpServerPluginRuntimeInfoTest`: PluginInfo 纯数据映射与身份常量.
- `ManifestContractTest`: Manifest 与 `McpServerPlugin` 常量一致 (权限, queries, Wake Activity, 两个服务的 action / category / process / requiresHostVersion).
- `ApplicationTextPunctuationTest`: 打包与生成文本只使用 ASCII 标点.
- 路线图 P2 起补充: 工具目录, JSON Schema, 令牌与配对状态机, 节点树紧凑格式, 上限与错误映射.

### 15.2 Android instrumentation (`app/src/androidTest`)

- `McpServerPluginContractTest` MUST 覆盖: Wake Activity 契约, INFO 服务发现与真实 `getInfo()` 往返 (包版本, 本地化描述, ID / engine / variant, 显式空 `supportedAbis`, `REQUIRES_HOST_VERSION`), `McpServerPluginService` 发现, `:mcp_server` 进程, 显式绑定与 Binder descriptor.
- `McpServerSpikeTest` (P0.2 起) MUST 覆盖: 启动 `McpServerService` 后在回环地址完成 `initialize` -> `notifications/initialized` -> `tools/list` -> `tools/call device_ping` 的有状态全链路 (会话头, `serverInfo`, 工具清单, `device_ping` 载荷与 `:mcp_server` 进程名), 并把无会话请求的状态码记录到 logcat 而不断言 SDK 的具体语义. P2.1 起还 MUST 覆盖: 伪造的非回环 `Host` 头被请求门以 403 拒绝, 同端口第二个监听器得到 `port_in_use`, 非法端口得到 `invalid_config` 而不绑定. P2.2 起还 MUST 覆盖: 所有请求携带 `TokenStore(context).current()` 的令牌, 无令牌请求得到 401 + `WWW-Authenticate`, 未配对客户端的 `tools/call` 得到 `PAIRING_REQUIRED` 并在经 `PairingDecisionReceiver` 广播允许后重试成功, 拒绝后得到 `PAIRING_DENIED`.
- 路线图 P2 起补充: 鉴权失败, 大小上限与错误传播, 绑定 / 解绑 / 进程重建不泄漏.
- 有设备或模拟器时执行 `:app:connectedDebugAndroidTest`; 性能度量与正确性测试分开.

## 16. CI 基线

- `build.yml`: push, pull request 与手动触发; `contents: read`; JDK 21 Temurin; 运行单元测试, 组装 debug / androidTest / release APK 与 lint, 上传产物; 在 API 24 (x86) 与 API 35 (x86_64) 模拟器上执行 instrumentation 契约测试.
- `markdown.yml`: Windows 环境运行 `.python\check_markdown.bat`, 阻止生成文档漂移.
- CI action 使用固定大版本并定期更新; timeout 与真实构建时长匹配.

## 17. 验证顺序

按变更范围执行最小但充分的验证:

```powershell
py .python/generate_markdown.py --check
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleRelease             # 引入或升级运行时依赖后 MUST 执行: R8 缺失类只会在这里以构建失败暴露
.\gradlew.bat :app:connectedDebugAndroidTest   # 有设备或模拟器时
```

Release 前额外执行 `.\gradlew.bat :app:appendDigestToReleasedFiles`, 检查 `releases/` 中只出现预期的已签名单 APK 且 CRC32 与文件内容一致. 任何未执行的验证都在最终说明中明确列出原因; 构建耗时较长时给予足够时间, 不用过短 timeout 误判失败.

## 18. 许可证, 安全, 隐私与第三方内容

- 根目录 `LICENSE` 为 Mozilla Public License 2.0, README 徽章与源码头保持一致.
- `android:allowBackup="false"` 与 `data_extraction_rules.xml` 全量排除保持不变.
- 不记录用户脚本正文, 文件内容, 令牌, 私有 URI 到普通日志; 联网行为限定为插件自身的监听端点, 超时, 拒绝策略与错误分类在文档中明确.
- 第三方代码与 AAR 必须记录来源, 版本, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`); 引入 MCP Kotlin SDK, Ktor, kotlinx-serialization 等依赖时同一提交更新该文件.

## 19. 完成检查清单

- [ ] 第 2 节身份值在 Gradle, Manifest, Kotlin 常量, 资源, 文档与测试中一致; `ManifestContractTest` 通过.
- [ ] 平台插件只在根 settings 应用一次, 无 `mavenLocal()`, 无外部路径引用, 无 `gradle/data`.
- [ ] `libs/` AAR 与 `locks/host-api-aars.lock` 哈希匹配, `THIRD_PARTY_NOTICES.md` 已更新.
- [ ] `sign.properties` 与 `app/sm003.jks` 被 Git 忽略; `appendDigestToReleasedFiles` 可用.
- [ ] Wake Activity, INFO 服务, `MCP_SERVER` 服务契约完整; `getInfo()` 显式 `supportedAbis = emptyArray()`.
- [ ] 10 语言资源与文档完整, ASCII 标点, `plugin_description` 无句尾点号; 图标由脚本生成.
- [ ] JSON 文案源已生成产物且 `--check` 通过; 当前版本全部语言 changelog 已更新.
- [ ] 单元测试, assemble, lint 通过; 有设备时 instrumentation 通过, 否则明确记录.
- [ ] `ROADMAP.md` 已勾选完成条目并写入证据; `VERSION_BUILD` 与提交数一致; `git status --short` 无输出.

## 20. 参考项目路由

只读取完成当前任务所需的参考, 不复制项目专属内容:

- 构建平台, Wake 激活, PluginInfo, 多语言生成和 README 样式: `D:/idea-projects/AutoJs6-Plugin-OpenCC`
- 纯 JVM PluginInfo 与真实 Binder 测试: `D:/idea-projects/AutoJs6-Plugin-Pinyin4j`
- 宿主 AAR 哈希锁定与发布身份锁: `D:/idea-projects/AutoJs6-Plugin-Python-Runtime`
- 宿主下发能力代理与 Bundle + JSON 桥接 (Node 插件家族): `D:/idea-projects/AutoJs6` 的 `engine/NodeBridgeProtocol.kt` 与 `core/plugin/nodejs/`
- 独立设置页, 跟随宿主主题与内置发行历史: `D:/idea-projects/AutoJs6-Plugin-Three-Stone-AI`
- 宿主入口, 插件发现, 安装和启用引导: `D:/idea-projects/AutoJs6`

参考时以这些仓库的当前代码为准, 不以历史 README 或旧 release 中已经淘汰的写法为准.
