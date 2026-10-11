# Third-party notices

This file records third-party components shipped with or consumed by the MCP Server plugin. The
plugin itself is licensed under the Mozilla Public License 2.0; the components below retain their
own licenses. Runtime dependencies are added to this list in the same commit that introduces them.

## AutoJs6 common plugin API

- Component: `common-plugin-api.aar` (Binder contract shared by AutoJs6 and its plugins)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`), host build 5329 (6.8.0), commit `03b37ef76a53dee59601515b3576e05417794369`, release build staged with `host-capability-api.aar` and `mcp-server-api.aar`
- SHA-256: `ed7a71942ffb67255ebad8d8a2c912b01c4a3cdd8aad497baba0687b4072032a` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## AutoJs6 host capability API

- Component: `host-capability-api.aar` (shared host capability data plane: `IHostCapabilityBroker`, `IHostCapabilityCallback`, and `HostCapabilityContract`; used by MCP contract version 2)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/host-capability-api`), host build 5329 (6.8.0), commit `03b37ef76a53dee59601515b3576e05417794369`, data-plane contract version 1
- SHA-256: `23024fd981b7846936ef59f595f4da2208adf38d7c2d35594c20c7dfc1a86bd1` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## AutoJs6 MCP Server plugin API

- Component: `mcp-server-api.aar` (Binder contract between AutoJs6 and this plugin: `IMcpServerPlugin`, `IMcpServerSession`, `IMcpServerCallback`, `IMcpHostCapabilityBroker`, `IMcpHostCapabilityCallback`, and `McpServerContract`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/mcp-server-api`), host build 5329 (6.8.0), commit `03b37ef76a53dee59601515b3576e05417794369`, contract version 2 (`openServerV2` appended; version 1 transactions unchanged)
- SHA-256: `296d47416ce3787b3cf0ba49f464842ac376478558d77e36b4cdcf8d553f21f2` (pinned in `locks/host-api-aars.lock`)
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

## ZXing

- Component: `com.google.zxing:core` 3.5.4 (QR code encoder for the client configuration snippets on the settings page; no camera or decoder use at run time)
- Source: <https://github.com/zxing/zxing>, Maven Central
- SHA-256: `71de5d89341b5fcf5dd89da7f44e84d825d0e084cdf3ec77c9abe26b0f0ceb13` (`core-3.5.4.jar`; SHA-1 `955fcd6bcd0723ddfb8ee6ed502d5fdf0e9676a9` matches Maven Central)
- License: Apache License 2.0
- The JVM and instrumentation tests also use its decoder (`QRCodeReader`) to read the generated codes back

## Test-only dependencies

These libraries are used by the JVM and instrumentation test source sets only and are not shipped
in the APK.

- JUnit 4 (`junit:junit`): Eclipse Public License 1.0
- AndroidX Test (`androidx.test:runner`, `androidx.test:rules`, `androidx.test.ext:junit`): Apache License 2.0
