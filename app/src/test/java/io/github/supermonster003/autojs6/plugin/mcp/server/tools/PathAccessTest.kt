package io.github.supermonster003.autojs6.plugin.mcp.server.tools

import io.github.supermonster003.autojs6.plugin.mcp.server.bridge.ToolErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PathAccessTest {

    private var switchOn = true
    private val access = PathAccess(listOf("/storage/emulated/0", "/sdcard")) { switchOn }

    private fun refused(raw: String, allowRoot: Boolean = false): Pair<String, String> = try {
        access.readable("tool", "path", raw, allowRoot)
        fail("$raw should be refused")
        throw AssertionError()
    } catch (e: ToolArgumentException) {
        e.failure.code to e.failure.message
    }

    @Test
    fun `relative paths stay in the working directory whatever the switch says`() {
        assertEquals(ReadTarget("pics/a.png", shared = false), access.readable("tool", "path", "./pics/a.png"))
        switchOn = false
        assertEquals(ReadTarget("pics/a.png", shared = false), access.readable("tool", "path", "pics/a.png"))
        assertEquals(ReadTarget(".", shared = false), access.readable("tool", "path", ".", allowRoot = true))
        assertTrue(refused("../a.png").second.contains("inside the AutoJs6 working directory"))
        assertEquals(listOf("files"), ReadTarget("a", shared = false).permissions(listOf("files")))
    }

    @Test
    fun `absolute paths need the switch, which is read for every call`() {
        switchOn = false
        val (code, message) = refused("/storage/emulated/0/DCIM/a.jpg")
        assertEquals(ToolErrorCodes.CAPABILITY_DENIED, code)
        assertTrue(message, message.startsWith("tool: path is an absolute path, and reading shared storage is switched off"))
        switchOn = true
        val target = access.readable("tool", "path", "/storage/emulated/0/DCIM/a.jpg")
        assertEquals(ReadTarget("/storage/emulated/0/DCIM/a.jpg", shared = true), target)
        assertEquals(listOf("image", "files.shared_read"), target.permissions(listOf("image")))
        assertTrue(access.sharedReadEnabled)
    }

    @Test
    fun `shared paths must lie in shared storage and never in Android data or obb`() {
        assertEquals("/sdcard/Download", access.readable("tool", "path", "/sdcard/Download/").path)
        assertEquals("/storage/emulated/0", access.readable("tool", "path", "/storage/emulated/0", allowRoot = true).path)
        assertEquals("/storage/emulated/0/Android/media/x.mp3", access.readable("tool", "path", "/storage/emulated/0/Android/media/x.mp3").path)
        assertEquals("/sdcard/Androidx/data/a", access.readable("tool", "path", "/sdcard/Androidx/data/a").path)
        listOf(
            "/data/data/org.autojs.autojs6/a" to "must lie in shared storage",
            "/storage/emulated/10/a" to "must lie in shared storage",
            "/storage/emulated/01/a" to "must lie in shared storage",
            "/storage/1234-5678/a" to "must lie in shared storage",
            "/" to "must not contain empty",
            "/sdcard//a" to "must not contain empty",
            "/sdcard/../data" to "must not contain empty",
            "/sdcard/./a" to "must not contain empty",
            "/sdcard/a\\b" to "without backslashes",
            "/sdcard/a\u0000b" to "without backslashes",
            "/storage/emulated/0/Android/data/com.example/a" to "Android/data, which is never readable",
            "/sdcard/android/OBB" to "Android/obb, which is never readable",
            "/sdcard/" + "a".repeat(5000) to "without backslashes",
        ).forEach { (raw, expected) ->
            val (code, message) = refused(raw)
            assertEquals(raw, ToolErrorCodes.INVALID_ARGUMENTS, code)
            assertTrue("$raw -> $message", message.contains(expected))
        }
        assertTrue(refused("/sdcard/Android/data").second.contains("never readable"))
    }

    @Test
    fun `without shared roots every absolute path is refused`() {
        val none = PathAccess.WORKSPACE_ONLY
        try {
            none.readable("tool", "path", "/sdcard/a")
            fail()
        } catch (e: ToolArgumentException) {
            assertEquals(ToolErrorCodes.CAPABILITY_DENIED, e.failure.code)
            assertEquals(PathAccess.HINT_SWITCH, e.failure.hint)
        }
        assertFalse(none.sharedReadEnabled)
    }
}
