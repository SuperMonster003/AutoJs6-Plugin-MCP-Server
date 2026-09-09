package io.github.supermonster003.autojs6.plugin.mcp.server.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.BindException
import java.net.SocketException

class BindFailuresTest {

    @Test
    fun addressInUseBecomesPortInUse() {
        val failure = BindFailures.classify(BindException("bind failed: EADDRINUSE (Address already in use)"), "127.0.0.1", 9637)
        assertEquals(BindFailures.CODE_PORT_IN_USE, failure.code)
        assertTrue(failure.message, failure.message.contains("9637") && failure.message.contains("already in use"))
    }

    @Test
    fun bindExceptionWithoutDetailsStillMeansPortInUse() {
        val failure = BindFailures.classify(IOException("engine start failed", BindException()), "0.0.0.0", 9637)
        assertEquals(BindFailures.CODE_PORT_IN_USE, failure.code)
    }

    @Test
    fun permissionProblemsBecomeBindFailedWithAHint() {
        val failure = BindFailures.classify(BindException("bind failed: EACCES (Permission denied)"), "127.0.0.1", 80)
        assertEquals(BindFailures.CODE_BIND_FAILED, failure.code)
        assertTrue(failure.message, failure.message.contains("denied") && failure.message.contains("1024"))
    }

    @Test
    fun unavailableAddressesBecomeBindFailed() {
        val failure = BindFailures.classify(SocketException("bind failed: EADDRNOTAVAIL (Cannot assign requested address)"), "10.0.0.9", 9637)
        assertEquals(BindFailures.CODE_BIND_FAILED, failure.code)
        assertTrue(failure.message, failure.message.contains("10.0.0.9") && failure.message.contains("not available"))
    }

    @Test
    fun unknownErrorsKeepTheirTypeAndMessage() {
        val failure = BindFailures.classify(IllegalStateException("engine failed"), "127.0.0.1", 9637)
        assertEquals(BindFailures.CODE_BIND_FAILED, failure.code)
        assertTrue(failure.message, failure.message.contains("IllegalStateException") && failure.message.contains("engine failed"))
        val silent = BindFailures.classify(RuntimeException(), "127.0.0.1", 9637)
        assertTrue(silent.message, silent.message.contains("no message"))
    }
}
