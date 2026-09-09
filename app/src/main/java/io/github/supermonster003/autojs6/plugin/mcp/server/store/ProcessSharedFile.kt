package io.github.supermonster003.autojs6.plugin.mcp.server.store

import android.content.Context
import android.os.Process
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A small text document in the plugin's private storage that more than one process reads and
 * writes: the `:mcp_server` process owns the listener, while the settings page (roadmap P4.2)
 * and the instrumentation tests run in the main process. `SharedPreferences` cache their file
 * per process and never notice another process's writes, so this class reads the file on every
 * access, replaces it atomically (temporary file plus rename, so a reader never sees a torn
 * document), and serializes read-modify-write cycles with a lock file that works across
 * processes as well as across threads.
 *
 * The documents live in [Context.getNoBackupFilesDir]: the bearer token must not travel to
 * another device inside a backup, and a paired-client list is meaningless without it.
 */
internal class ProcessSharedFile(directory: File, name: String) {

    constructor(context: Context, name: String) : this(File(context.applicationContext.noBackupFilesDir, DIRECTORY), name)

    private val file = File(directory, name)
    private val lockFile = File(directory, "$name.lock")
    private val processLock = processLocks.getOrPut(file.absolutePath) { ReentrantLock() }

    /** The current document, or null when there is none (or it cannot be read). */
    fun read(): String? = try {
        if (file.isFile) file.readText(Charsets.UTF_8) else null
    } catch (e: IOException) {
        Log.w(TAG, "Cannot read ${file.name} (${e.javaClass.simpleName})")
        null
    }

    /**
     * Replaces the document with what [transform] returns for the current one (null deletes it)
     * while no other thread or process runs the same cycle; returns the new document.
     */
    @Throws(IOException::class)
    fun update(transform: (String?) -> String?): String? = locked {
        val current = read()
        val next = transform(current)
        if (next != current) {
            if (next == null) delete() else replace(next)
        }
        next
    }

    @Throws(IOException::class)
    fun write(text: String?): String? = update { text }

    private fun <T> locked(block: () -> T): T = processLock.withLock {
        if (processLock.holdCount > 1) return@withLock block()
        file.parentFile?.mkdirs()
        RandomAccessFile(lockFile, "rw").use { handle ->
            val lock = handle.channel.lock()
            try {
                block()
            } finally {
                lock.release()
            }
        }
    }

    private fun replace(text: String) {
        val temporary = File(file.parentFile, "${file.name}.${Process.myPid()}.${System.nanoTime()}.tmp")
        try {
            FileOutputStream(temporary).use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
                stream.fd.sync()
            }
            if (!temporary.renameTo(file)) throw IOException("cannot rename ${temporary.name} to ${file.name}")
        } catch (e: IOException) {
            temporary.delete()
            throw e
        }
    }

    private fun delete() {
        if (file.exists() && !file.delete()) Log.w(TAG, "Cannot delete ${file.name}")
    }

    companion object {

        const val DIRECTORY = "mcp_server"

        private const val TAG = "ProcessSharedFile"

        private val processLocks = ConcurrentHashMap<String, ReentrantLock>()
    }
}
