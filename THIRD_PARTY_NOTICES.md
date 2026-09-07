# Third-party notices

This file records third-party components shipped with or consumed by the MCP Server plugin. The
plugin itself is licensed under the Mozilla Public License 2.0; the components below retain their
own licenses. Runtime dependencies are added to this list in the same commit that introduces them.

## AutoJs6 common plugin API

- Component: `common-plugin-api.aar` (Binder contract shared by AutoJs6 and its plugins)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`), host 6.8.0 release distribution
- SHA-256: `c526f4fd0adbf38b36a7bf54f9931385e6cc20050d6ea8fb741c60496af635d5` (pinned in `locks/host-api-aars.lock`)
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
