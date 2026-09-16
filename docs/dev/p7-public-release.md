# P7 first public release

Date: 2026-09-16. The first public release is **v1.0.1**, versionCode **57**.
The unpublished v1.0.0 development history is included in its release notes.

## Published artifact and source

| Field | Value |
| --- | --- |
| Repository | [SuperMonster003/AutoJs6-Plugin-MCP-Server](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) |
| Release | [v1.0.1](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases/tag/v1.0.1) |
| Tagged source commit | `d2731e8ffed1efc2a222b57cc782f4b65e29adec` |
| Published at | 2026-09-16 10:00:27, GMT+08:00 |
| Asset name | `autojs6-plugin-mcp-server-v1.0.1-3b9fc1b0.apk` |
| Size | 1377442 bytes |
| CRC32 | `3b9fc1b0` |
| APK SHA-256 | `c8b5d521358fb196b746fc417797f5d283b15dcb287ebc0f5a71c22fbcd565b5` |
| Signer SHA-256 | `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213` |
| SDK | minSdk 24, targetSdk 36, compileSdk 37 |
| Required host versionCode | 5279 |

The release commit updates `released_date` to `2026/09/16` in all ten changelog
sources for v1.0.1 and the included, previously unpublished v1.0.0 history. Generated
documents were regenerated and checked. `:app:appendDigestToReleasedFiles` was run
again on the final tagged commit. The manifest generator received that exact APK,
`--tag v1.0.1` and `--commit d2731e8ffed1efc2a222b57cc782f4b65e29adec`.

The release contains one APK. Its uploaded asset name, size and GitHub SHA-256 digest
match `artifacts[0]` in the admission manifest. Downloading the published asset and
hashing it independently produced the same SHA-256.

## Official index

The admission manifest is committed as
[`release-manifests/io.github.supermonster003.autojs6.plugin.mcp.server/57.json`](https://github.com/SuperMonster003/AutoJs6-Official-Plugins-Index/blob/0084f13973b1a06fd06a7dabb7e25e1692accc29/release-manifests/io.github.supermonster003.autojs6.plugin.mcp.server/57.json).
Index commit: `0084f13973b1a06fd06a7dabb7e25e1692accc29`, pushed to `main`.

The full generator produced 37 official entries. The one MCP entry binds version
1.0.1 / 57, its single asset, required host version, certificate and tagged source.
The generated index was not edited manually. All 33 index unit tests pass.

The first full generation exposed a published ImGui manifest placeholder. The
generator now resolves a bounded set of static Gradle placeholder assignments and
the referenced host-version constant from source at the same release tag. Unknown
expressions, duplicate assignments, missing or ambiguous constants, and paths
outside the permitted source roots fail closed. Five tests cover this addition.
The generator does not execute Gradle or evaluate arbitrary source code.

## Host integration and workspace

Host `master` merged `mcp-p35-docs` through
`d4e05a81dc159472a017715412ed1cc858b20a37`. Its parents are
`8cec3694ec0ab8128c7a4815d111b7d6f4f82200` and
`04c9c16b7a3ab2cccbd82afba37f667a039aba40`. The merged tree passed 32 JVM tests
covering documentation and sample catalogs, bridge permissions and the MCP package,
and assembled the host debug and instrumentation APKs. The worktree registration
and `AutoJs6-mcp-p35-docs` directory, including its ignored signing copies, were
removed. The source branch remains available. The host merge was not pushed.
Unrelated host workspace changes were preserved.

The plugin workspace already contained work for targetSdk 37 and version 1.0.2.
It was preserved, restored after publishing v1.0.1, and committed separately as
`e56e7ef`. It is not part of the v1.0.1 APK. The idle-stop instrumentation test fix
is separate commit `5e67a73`: a brief failed socket connection alone no longer
counts as evidence that the idle timer stopped the listener.

## Validation

| Scope | Result |
| --- | --- |
| Release source JVM tests | 235 passed |
| Debug and instrumentation APK assembly | Passed |
| Release source lint | 0 errors, 8 warnings |
| Signed release / R8 / digest | Passed at the tagged source commit |
| API 33 instrumentation | `OK (35 tests)`, 34 passed and one opt-in process-kill assumption skipped, 98.192 s |
| Tag Build integrity | [Passed](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/actions/runs/35045522886), including API 24 and API 35 jobs |
| Tag Markdown integrity | [Passed](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/actions/runs/35045522868) |
| Index generator unit tests | 33 passed |
| Future 1.0.2 work | 235 JVM tests, debug assemblies, lint with 0 errors / 13 warnings, API 33 local-network compatibility test passed |

The API 33 instrumentation run used the release debug APK with the corrected
test APK from `5e67a73`. The private read-only emulator instance was closed after
verification. On the later 1.0.2 commit, one CI API 24 run failed in the workspace
tool test with `No value for result`; a retry of the failed job passed without a
source change. The API 35 and unit/build jobs passed on their first attempt.

## Installation and client acceptance

Both real devices installed through AutoJs6's Plugin Center, updating their
existing development installations. This is an update-installation test, not
evidence for a fresh ColorOS installation. Reading each installed base APK back
and hashing it produced the published SHA-256 above.

| Device | API | Installed release | Installed APK hash |
| --- | --- | --- | --- |
| Sony XQ-DQ72, `QV770340J7` | 33 | 1.0.1 / 57 | Matches published APK |
| Xiaomi Pad 23046RP50C, `968e9f18` | 35 | 1.0.1 / 57 | Matches published APK |

Both hosts remain AutoJs6 6.8.0 / 5280 from their existing installations. Their
builds do not yet contain the newly merged `app.listDocs` / `app.readDoc` bridge.
Real-device documentation-resource evidence is explicitly deferred to the next
session, together with P7 item 8, the current-conclusions table, appendix D and
maintainer memory updates.

Sony passed missing and wrong Bearer token checks (401 plus `WWW-Authenticate`),
forged Host rejection (403), initialization reporting 1.0.1, 33 enabled tools,
first-client pairing, `device_info` and `script_run("toast('hi'); console.log(...)")`.
MCP Inspector CLI 2.6.0 completed both tools. Killing the host left the listener
reachable with `HOST_UNAVAILABLE`; reopening the host restored calls. The actual
notification Stop action stopped the listener and cleared the host drawer switch.

Xiaomi passed the same token, Host, initialization, enabled-tool list, paired
tool-call, Inspector 2.6.0, host-death and recovery checks. HyperOS required the
notification Allow action for pairing. The initial attempt expired because the
automation waited for a foreground dialog; the later run used the notification
action and passed. Its actual notification Stop action also made the listener
unreachable and cleared the host drawer switch.

The tablet initially had Wi-Fi disabled, and its existing VPN could not resolve
GitHub after Wi-Fi was enabled. Downloading through Plugin Center used a temporary
USB CONNECT tunnel that only allowed GitHub hostnames and retained end-to-end TLS.
The proxy and reverse mapping were removed after downloading. The devices' source
installation permission and plugin developer mode, and the tablet's Wi-Fi setting,
were restored to their original disabled states. Temporary P7 client pairings and
ADB forwards were removed. Inspector client pairings were retained.

Claude Code 2.1.270 is present, but no usable login was available in the checked
local configurations. A request for the maintainer's existing configured login
remains unanswered. No Claude Code model-driven tool call on the published APK
is claimed. Consequently the full P2 client acceptance gate, and P7 item 6, remain
open until that client evidence is added.

Local, ignored evidence is under `build/p7/`: the manifest, downloaded and
device-read APKs, client verdict JSON, and CI reports. Build logs are
`build/p7-release-verification.log`, `build/p7-final-tag-digest.log`,
`build/p7-instrumentation-confirmed-stop.log` and
`build/p7-sdk37-verification.log`. These artifacts do not contain bearer tokens.
