-dontwarn kotlinx.parcelize.Parcelize

-keep class io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPluginInfoService { *; }
-keep class io.github.supermonster003.autojs6.plugin.mcp.server.McpServerPluginService { *; }
-keep class io.github.supermonster003.autojs6.plugin.mcp.server.McpServerService { *; }
-keep class io.github.supermonster003.autojs6.plugin.mcp.server.WakeActivity { *; }
-keep class io.github.supermonster003.autojs6.plugin.mcp.server.ui.PairingConfirmActivity { *; }
-keep class io.github.supermonster003.autojs6.plugin.mcp.server.ui.PairingDecisionReceiver { *; }

-keep class org.autojs.plugin.common.api.** { *; }
-keep class org.autojs.plugin.mcp.server.api.** { *; }

# Ktor reports JVM start-up time through java.lang.management when it exists; Android has no such API
# and the call sites are guarded, so R8 only needs to stop treating the references as errors.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
