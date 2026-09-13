# Third-party notices

This file records third-party components shipped with or consumed by the MCP Server plugin. The
plugin itself is licensed under the Mozilla Public License 2.0; the components below retain their
own licenses. Runtime dependencies are added to this list in the same commit that introduces them.

## AutoJs6 common plugin API

- Component: `common-plugin-api.aar` (Binder contract shared by AutoJs6 and its plugins)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`), host build 5280 (6.8.0), commit `e1aa3f813dc1415eca9a7e89781f5d26be26cb53`, release build paired with `mcp-server-api.aar`
- SHA-256: `59706b9fdb76a2312d295d9ad48b3bfe96d1da899cf76434e96ef646a673f400` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## AutoJs6 MCP Server plugin API

- Component: `mcp-server-api.aar` (Binder contract between AutoJs6 and this plugin: `IMcpServerPlugin`, `IMcpServerSession`, `IMcpServerCallback`, `IMcpHostCapabilityBroker`, `IMcpHostCapabilityCallback`, and `McpServerContract`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/mcp-server-api`), host build 5280 (6.8.0), commit `e1aa3f813dc1415eca9a7e89781f5d26be26cb53`, P4 settings extension (optional version 1, unchanged AIDL order)
- SHA-256: `6ee668b2d883be7bc5c2be88b1b7787da0f7e1072410b0d5d2efca1df9a43400` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## Kotlin standard library

- Component: `org.jetbrains.kotlin:kotlin-stdlib` (provided through the Android Gradle Plugin built-in Kotlin support)
- Source: <https://github.com/JetBrains/kotlin>
- License: Apache License 2.0

## Model Context Protocol Kotlin SDK

- Component: `io.modelcontextprotocol:kotlin-sdk-server` 0.15.0 with `kotlin-sdk-core` 0.15.0 (the MCP protocol implementation behind the `/mcp` endpoint)
- Source: <https://github.com/modelcontextprotocol/kotlin-sdk>
- License: Apache License 2.0 (portions under the MIT License, see the upstream LICENSE)

## Ktor

- Component: `io.ktor:ktor-server-cio` 3.5.1 (HTTP engine) and the server modules the SDK requires (`ktor-server-core`, `ktor-server-sse`, `ktor-server-content-negotiation`, `ktor-server-websockets`, `ktor-serialization-kotlinx-json`) with their `ktor-http`, `ktor-io`, `ktor-network`, `ktor-utils`, and `ktor-events` runtime
- Source: <https://github.com/ktorio/ktor>
- License: Apache License 2.0
- Test scope only (not shipped in the APK): `io.ktor:ktor-server-test-host` 3.5.1 with its `ktor-client-core` and `kotlinx-coroutines-test` runtime, used by the JVM transport tests

## kotlinx libraries

- Component: `kotlinx-coroutines-core` 1.11.0, `kotlinx-serialization-json` 1.11.0 (with `kotlinx-serialization-core` and `kotlinx-serialization-json-io`), `kotlinx-io-core` 0.9.1, `kotlinx-collections-immutable` 0.5.1 (transitive through the SDK and Ktor)
- Source: <https://github.com/Kotlin>
- License: Apache License 2.0

## kotlin-logging

- Component: `io.github.oshai:kotlin-logging` 8.0.4, resolved to the `kotlin-logging-android` variant that writes through `android.util.Log` (transitive through the SDK)
- Source: <https://github.com/oshai/kotlin-logging>
- License: Apache License 2.0

## SLF4J

- Component: `org.slf4j:slf4j-api` 2.0.18 (transitive through Ktor; no binding is shipped, so its output is discarded)
- Source: <https://www.slf4j.org>
- License: MIT License

## Test-only dependencies

These libraries are used by the JVM and instrumentation test source sets only and are not shipped
in the APK.

- JUnit 4 (`junit:junit`): Eclipse Public License 1.0
- AndroidX Test (`androidx.test:runner`, `androidx.test:rules`, `androidx.test.ext:junit`): Apache License 2.0
