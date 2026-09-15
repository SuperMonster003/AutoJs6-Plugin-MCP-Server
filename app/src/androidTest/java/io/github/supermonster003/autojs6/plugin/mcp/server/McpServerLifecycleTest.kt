package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Process-level helpers for the lifecycle matrix (roadmap P6): the evidence script runs one of
 * them through `am instrument -e <argument> true` while it drives the host, the drawer and a PC
 * client from adb. They are skipped in an ordinary test run.
 *
 * `run-as <pkg> kill` does not end a process on every device (Sony builds return success without
 * a signal) and `am crash` leaves a "keeps stopping" dialog behind, so the listener process is
 * killed from inside the app's own UID, which Linux always permits.
 */
@RunWith(AndroidJUnit4::class)
class McpServerLifecycleTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun killListenerProcess() {
        Assume.assumeTrue("run with -e mcpKillListener true", InstrumentationRegistry.getArguments().getString("mcpKillListener") == "true")
        val pid = listenerPid()
        assertNotNull("the :mcp_server process must be running", pid)
        Process.killProcess(pid!!)
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline && listenerPid() == pid) Thread.sleep(100)
        Log.i(TAG, "killed :mcp_server pid $pid; now ${listenerPid()}")
        assertTrue("the listener process $pid must be gone", listenerPid() != pid)
    }

    private fun listenerPid(): Int? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.runningAppProcesses.orEmpty().firstOrNull { it.processName == context.packageName + ":mcp_server" }?.pid
    }

    private companion object {
        const val TAG = "McpServerLifecycleTest"
    }
}
