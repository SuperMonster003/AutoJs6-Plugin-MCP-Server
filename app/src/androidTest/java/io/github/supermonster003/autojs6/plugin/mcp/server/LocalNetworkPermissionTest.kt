package io.github.supermonster003.autojs6.plugin.mcp.server

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.mcp.server.server.McpHttpServer
import io.github.supermonster003.autojs6.plugin.mcp.server.store.BindScope
import io.github.supermonster003.autojs6.plugin.mcp.server.store.ServerConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalNetworkPermissionTest {
    @Test fun lanRequiresThePluginGrantAndLoopbackRemainsAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val server = McpHttpServer(context)
        try {
            val lan = server.start(ServerConfig(port = 19637, bindScope = BindScope.LAN))
            if (LocalNetworkAccess.isGranted(context)) assertTrue(lan.toString(), lan.isRunning)
            else {
                assertFalse(lan.isRunning)
                assertTrue(lan.toString(), lan.toString().contains("local_network_permission_required"))
            }
            server.stop()
            val loopback = server.start(ServerConfig(port = 19637))
            assertTrue(loopback.toString(), loopback.isRunning)
        } finally { server.stop() }
    }
}
