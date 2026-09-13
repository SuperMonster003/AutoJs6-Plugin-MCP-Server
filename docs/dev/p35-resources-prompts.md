# P3.5: Resources and prompts

The existing 37 tools remain unchanged. Resources use the same request gate, Bearer authentication, pairing gate, host bridge, and persistent group switches as tools. Discovery requires a valid token. resources/read and prompts/get additionally require pairing. The plugin holds no workspace or console content cache.

| Resource | Representation | Host call | Group |
|---|---|---|---|
| autojs6://workspace/{+path} | Text or base64 blob, maximum 1 MiB | FileTools -> files.read | files |
| autojs6://samples/ | JSON directory index with child URIs | app.listSamples | files |
| autojs6://samples/{+path} | Text or base64 blob; a trailing slash selects a directory | app.readSample / app.listSamples | files |
| autojs6://device/info | Current device JSON | device.info | device |
| autojs6://console/tail | Latest 100 console entries, without the internal schema tag | console.tail | script |

The two path templates use RFC 6570 reserved expansion. Nested path separators remain literal slashes; individual path segments are percent-encoded. URI parsing decodes exactly once and preserves a literal plus. It rejects credentials, ports, queries, fragments, malformed escapes, invalid UTF-8, encoded separators, control characters, dot segments, absolute paths, and drive prefixes. Workspace reads also pass through FileTools validation and host canonical path checks.

Known text extensions return TextResourceContents. PNG, JPEG, WebP, and other binary files return BlobResourceContents. Per-content _meta carries bytes, totalBytes when provided by the host, encoding, and truncated. Consumers must inspect truncated before using a source file. Files without a known text extension are binary. Queries such as ?encoding=base64 are not accepted; the files_read tool offers explicit encoding selection.

resources/list includes device, console, a sample root index, and built-in sample file metadata, filtered by enabled groups. It does not enumerate workspace filenames. Pages contain at most 100 entries, with a cursor tied to the current ordered URI set. A changed catalog invalidates old cursors. The host sample scan is bounded to 2000 entries and reports sampleCatalogTruncated. An unavailable sample provider leaves fixed metadata discoverable and reports sampleCatalogStatus. Each read rechecks its group before dispatch and before returning content.

The retained 2025-11-25 / 2025-06-18 protocol has no top-level ttlMs field. ttlMs=0 and cacheScope=private appear in _meta, the extension space permitted by the [2025 resource schema](https://modelcontextprotocol.io/specification/2025-11-25/schema). These are advisory AutoJs6 metadata, not the postponed 2026 protocol. Subscribe and list-changed notifications remain unadvertised; clients refresh discovery instead. Public SDK per-session handlers provide list metadata, pagination, and reserved path matching through the existing SSE mount. Protocol errors keep private host details in response data, with a generic exception message because the SDK logs exceptions.

## Prompts

| Prompt | Arguments | Purpose |
|---|---|---|
| write_autojs6_script | goal optional, language optional | Read a host sample, explain script conventions, write and verify requested code |
| automate_task | goal required, language optional | Observe with ui_dump, act once, wait with a bound, and verify |
| debug_selector | selector required, language optional | Explain the MCP selector dialect and check predicates against current nodes |

Bodies live in assets/prompts/en and assets/prompts/zh. Phone Chinese locales select the Chinese body; other locales fall back to English. An explicit language argument can select English or Chinese. Required arguments reject blanks, unknown keys are rejected, and each argument has an 8192-byte UTF-8 ceiling. User arguments become separate user messages and never select asset paths or replace text inside a template. Obtaining a prompt executes no script or device action.

The host sample catalog originally used java.util.Base64, unavailable on API 24 / 25. Binary sample reads now use the existing Okio codec, matching the workspace file bridge. No dependency versions or locked AARs change. The optional Offline-Docs provider is not implemented, and no docs resource is advertised.

## Validation

On 2026-09-13, the plugin passed 193 JVM tests, debug/androidTest/release assembly, and lint (0 errors, 6 existing warnings). The Temurin-simulation build took 2m 8s and emitted one platform-version block. The IDE file build also succeeded, with an SDK XML version warning. Host NodeBridgeSampleCatalogTest passed 6 tests, and debug plus androidTest assembly took 49s. No runtime dependencies changed, so host release was not rebuilt in this phase.

| Device | API | Plugin instrumentation | Real MCP resource flow | Host sample device test |
|---|---|---|---|---|
| AVD x86, emulator-5554 | 24 | 18/18, 13.092 s | Passed, list 605 ms, image 43 ms | 1/1, 0.035 s |
| Sony G8441, BH900ASK9E | 28 | 18/18, 21.551 s | Not run | Not run |
| Sony XQ-AT72, QV710AF65F | 31 | 18/18, connected task 25 s | Not run | Not run |
| Sony XQ-DQ72, QV770340J7 | 33 | 18/18, 11.219 s | Passed, list 283 ms, image 22 ms | 1/1, 0.015 s |
| Xiaomi 22120RN86C, bek749scrwv4wo8h | 33 | 18/18, 24.286 s | Not run | Not run |
| Xiaomi Pad 23046RP50C, 968e9f18 | 35 | 18/18, 16.586 s | Passed, list 623 ms, image 129 ms | 1/1, 0.023 s |

The Python standard-library HTTP client identifies as p35-device-evidence 1.0 and negotiates 2025-11-25 through adb forward. All three real flows listed 203 resources over three pages, read sample directories and HelloWorld.js, and decoded OCR/test.png to the same 18949 bytes (SHA-256 d94fede2793566b4f3a9e227ce930496bc8071b8d4e551f2392c9153b46eba4c). Six prompt bodies, a Chinese workspace path, a workspace blob, script_run_file finishing, its console marker, and live group revocation passed. Tokens and resource contents are not written to evidence logs. Temporary workspace fixtures were deleted, configuration and pairing were restored, and each real host session passed stop/close after a 90 s observation window. Instrumentation also covers a 1 MiB JSON descriptor resource result.

### Claude Code client acceptance

The Claude Code CLIENT_E2E passed on 2026-09-13 using the maintainer's JetBrains ACP profile named Claude via AIGoCode [Fabel 5.1 (1M)]. Its environment was injected into a temporary Claude Code 2.1.257 child process; ANTHROPIC_MODEL was claude-fable-5-1[1m], and the response reported the same model identifier. The CLI connected to the configured AIGoCode gateway using ANTHROPIC_BASE_URL and ANTHROPIC_AUTH_TOKEN. Earlier auth status checks without this environment did not establish whether the gateway was usable. These variables are supported by the [Claude Code environment reference](https://code.claude.com/docs/en/env-vars).

The test used AVD x86 / API 24 / emulator-5554 with plugin 1.0.0 / build 23 (296d816) and host 6.8.0 / 5280. Claude Code connected over HTTP with a Bearer header and negotiated 2025-11-25. The first /mcp__autojs6__write_autojs6_script invocation reached prompts/get and received PAIRING_REQUIRED. The test confirmed the request on the phone through PairingDecisionReceiver, then invoked the command again with a single-token goal and language=en. This exercises the [MCP prompt command syntax](https://code.claude.com/docs/en/mcp#execute-mcp-prompts).

The successful model session took 23.35 s and 4 turns:

1. prompts/get returned the English write_autojs6_script body and the requested goal.
2. Claude Code called ReadMcpResourceTool for autojs6://samples/, then autojs6://samples/JavaScript/HelloWorld.js. The sample was 67 UTF-8 bytes, truncated=false, SHA-256 35bbd5eff48c045400bd9b94e4a30372cc1d5af44bb3636c81e17016218339a0.
3. The model generated a 77-byte script for the requested console.log/toast verification marker and called mcp__autojs6__script_run. The actual response had status=finished, durationMs=2525, no exception, and the marker in the captured console. Script SHA-256: e4f141a919455cd9270758e09befcd332139257bb46efabd6db7d6bd8a579fbb.
4. The CLI returned success with the marker in its answer and zero tool permission denials. The initial pairing refusal also produced a zero-turn CLI success result, so acceptance checks the actual MCP resource, prompt, and execution responses rather than the process exit code alone.

An ignored, local test harness used a temporary loopback HTTP observer ahead of adb forward. It passed the original JSON payload and authentication header through to the plugin's existing request gate and SSE transport. Evidence records only methods, public sample URIs, byte counts, SHA-256 values, and result status; it omits credentials and resource/script bodies. The gateway credential remained in the child environment; the temporary MCP configuration used an environment placeholder for the plugin token. The existing ACP configuration and global Claude settings were not edited. Local files, shell, and unrelated MCP tool calls were not authorized to the model.

The host McpServerPluginRoundTripTest passed 1/1 in 151.601 s, including a 150 s observation window followed by normal stop/close. The harness restored configuration, paired clients, and group switches, removed its adb forward, and verified that the plugin service had stopped. The Markdown check passed for all 10 languages / 36 artifacts, and ApplicationTextPunctuationTest passed 1/1. No provider credential was found in the temporary client files.

This is actual Claude Code CLI resource consumption and model-generated script execution through the configured gateway. The JetBrains ACP conversation UI was not separately tested. The optional Offline-Docs provider and OEM activation matrix remain outside the completed evidence. This follow-up changes documentation only; it reuses the previously tested APKs and does not repeat the full build/device matrix.

The host sample codec/test/doc patch is committed as cc1383376. SDK 37 Looper/Timer work was committed separately before this patch. The tested host APK was built from the shared checkout with those concurrent changes present; they are not included in the MCP commit. No public script API, grant, or locked AAR changes were needed.
