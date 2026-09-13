# P3.4: Workspace, applications, device, and Shell

The catalog contains 37 tools across all 8 groups. The default set contains 33 tools: `ui_swipe`, `ui_gesture`, `files_delete`, and `shell_exec` stay disabled. `toast` keeps the single-word name explicitly specified by P3.4; it is the sole exception to the group_action naming convention.

## Tool behavior

| Surface | Host implementation | Limits and results |
|---|---|---|
| `files_list`, `files_stat`, `files_read` | `NodeBridgeFilesBridge` / `NodeBridgeFileScope` | Relative workspace paths. Listing defaults to 500 entries, maximum 2000. Reads default to 1 MiB; encoding=base64 represents binary content, with raw byte counts and truncation metadata. |
| `files_write`, `files_mkdir`, `files_rename` | File bridge, followed by explorer refresh | UTF-8 writes support createDirs and overwrite. Rename uses path and to, defaulting to no overwrite. Root mutations are refused. |
| `files_delete` | File bridge, separate permission token and plugin group | Disabled by default. Recursive deletion unlinks nested symbolic links without traversing their targets. |
| `editor_open` | app.editFile(path, {line, column}) -> EditActivity.editFileAt | Public positions start at 1; the host subtracts 1. Existing single-argument editFile calls retain their external-editor behavior. Out-of-range lines are ignored by the editor. |
| `app_list`, `app_launch` | package_manager.listApps, app.launchPackage / launchApp | Up to 1000 Android-visible applications, with case-insensitive package or label filtering. Exactly one launch target. The existing package manager list still lists script dependencies. |
| `clipboard_get`, `clipboard_set`, `toast` | Existing clipboard and toast bridge | Clipboard text up to 64 KiB. Android may restrict background clipboard access. Toasts do not request host logging. |
| `device_ensure_accessibility` | Existing AccessibilityTool controller | Uses configured root, secure-settings, and Shizuku strategies, waiting up to 10 s for operational state. Failure includes manual activation and Android 13 restricted-settings guidance. |
| `shell_exec` | Existing controlled Shell execution | Default timeout 15 s, maximum 120 s; combined stdout/stderr defaults to 64 KiB, maximum 256 KiB. Both sides truncate at UTF-8 boundaries. Exit code 124 and timedOut identify timeout. |

The plugin reloads tool_groups.json at execution time. allowShellRoot is a separate boolean, defaults to false, and survives other group changes. A root request requires the shell group, allowShellRoot, and shell.root in the host grant. The default host grant now includes this permission ceiling; both phone-side switches still default off. P4 will provide the settings interface.

## Transport and compatibility

A file content limit of 1 MiB is an upper bound. The current host grant also caps the entire encoded request at 96 KiB, including JSON escaping and envelope fields. HostBridgeClient checks that budget before Binder dispatch, so an oversized write makes no partial changes. No chunking, staging-file deletion, or wider host grant is hidden inside files_write.

For results above 64 KiB, the host writes JSON to a private temporary file, opens a read-only descriptor, unlinks the file, and returns only a payload wrapper with MIME application/json. HostBridgeClient consumes the descriptor, parses the JSON, and closes it on success, malformed input, cancellation, or late delivery. The existing 8 MiB payload contract remains the ceiling. Screenshot payloads retain their image MIME and metadata. AIDL transaction order and the locked API AARs are unchanged.

This phase requires the matching P3.4 host. The minimum discovery version remains 5279; older hosts may reject new methods. In particular, old hosts do not provide listApps, editor positioning, automatic ensureEnabled, or large JSON payloads.

API 24 testing exposed File.toPath() in the earlier file bridge. Android 24 / 25 now use File.renameTo after the same scoped path and overwrite checks, without pre-deleting the destination. Symbolic-link checks use canonical entries on these systems, and recursive deletion explicitly avoids descending into links. Base64 uses the host's existing Okio codec. Shell uses bounded exitValue polling and legacy process destruction on API 24 / 25. The newer process methods and File.toPath were added in API 26: [Android File reference](https://developer.android.com/reference/java/io/File#toPath()) and [Android Process reference](https://developer.android.com/reference/java/lang/Process#waitFor(long,%20java.util.concurrent.TimeUnit)). No dependency versions changed.

## Validation on 2026-09-13

- Plugin: 175 JVM tests passed; debug, androidTest, release, and lintDebug passed. Lint reported 0 errors and 6 existing warnings. The Temurin-simulation build emitted one platform-version information block. VERSION_BUILD is 22 for the P3.4 commit.
- Host: commit 83f780852. All 19 focused JVM tests passed (file scope, device/Shell policy, MCP grant), and app debug plus androidTest APKs built with compileSdk 37 / targetSdk 36 / minSdk 24. Existing SDK 37 work was committed separately. The previous P3.3 host changes were also committed in this session as dd521ed5d.
- Plugin instrumentation: 17/17 on each of six devices. API 31 used connectedDebugAndroidTest; the other five used adb instrument with the built APKs. See the matrix below.
- Real client: Python standard-library HTTP over adb forward, protocol 2025-11-25, client p34-device-evidence 1.0, authenticated and paired through the existing phone decision receiver. Tokens, clipboard contents, and file contents are never written to evidence logs.
- API 24 x86 AVD, emulator-5554: full P3.4 flow passed. Secure-settings automatic accessibility activation succeeded, the new folder appeared in the host explorer, and the editor status reached 3:4. The premature position assertion initially saw -:- during loading; waiting for readiness resolved it without an editor change. Renaming, separate deletion/Shell/root gates, combined output truncation, and Shell timeout passed. Binary reading returned exactly 1048576 bytes from a 1049600-byte fixture with truncated=true (1174 ms in the first successful run). A 100000-character write failed with LIMIT_EXCEEDED and left no file.
- The debugger router accepted a non-suspending logpoint in the host editor, but could not launch its test because the file is outside the IDE project. The logpoint was removed, preserving five existing user breakpoints. Position evidence comes from real accessibility queries, not debugger hits.

Test fixtures use unique workspace subdirectories and private cache directories. Pairing state, plugin configuration, group overrides, clipboard text, accessibility settings, and any temporary secure-settings grant are restored after each client test. No root Shell was executed on a physical device, and Shizuku activation was not separately exercised. Client display matrices, OEM activation, resources/prompts, and the phone settings UI belong to later stages.

| Device | API | Plugin instrumentation | Real P3.4 MCP flow | Host filesystem test |
|---|---|---|---|---|
| AVD x86, emulator-5554 | 24 | 17/17, 12.213 s | Passed, 1 MiB binary read 1174 ms | 1/1, 0.070 s |
| Sony G8441, BH900ASK9E | 28 | 17/17, 20.446 s | Not run in this phase | Not run |
| Sony XQ-AT72, QV710AF65F | 31 | 17/17, connected task | Not run in this phase | Not run |
| Sony XQ-DQ72, QV770340J7 | 33 | 17/17, 10.605 s | Passed, 1 MiB binary read 397 ms | 1/1, 0.042 s |
| Xiaomi 22120RN86C, bek749scrwv4wo8h | 33 | 17/17, 22.336 s | Not run in this phase | Not run |
| Xiaomi Pad 23046RP50C, 968e9f18 | 35 | 17/17, 14.937 s | Passed, 1 MiB binary read 953 ms | 1/1, 0.101 s |

The real client runs also passed McpServerPluginRoundTripTest, one per device, including stop and close after the observation window. NodeBridgeFileScopeDeviceTest creates its symbolic link in a unique private cache fixture and proves that reading through the outward link is rejected and recursive deletion preserves the external target. Its first run exposed an old JUnit runtime without ThrowingRunnable; replacing the test assertion helper with explicit exception checks made the test compatible without changing dependencies.
