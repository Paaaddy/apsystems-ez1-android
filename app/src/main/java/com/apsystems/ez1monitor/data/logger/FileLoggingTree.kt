package com.apsystems.ez1monitor.data.logger

import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FileLoggingTree(private val logDirProvider: () -> File?) : Timber.Tree() {

    private val lock = Any()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val dir = logDirProvider() ?: return
        synchronized(lock) {
            try {
                val now = Date()
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(now)
                val fileName = "ez1-debug-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)}.log"
                val line = "$timestamp [${priorityToChar(priority)}] ${tag ?: "App"}: $message\n"
                val file = File(dir, fileName)
                if (file.length() < MAX_FILE_SIZE) {
                    file.appendText(line)
                }
            } catch (_: Exception) {
                // Never crash the app because of a logging failure
            }
        }
    }

    fun cleanupOldLogs() {
        val dir = logDirProvider() ?: return
        val cutoff = System.currentTimeMillis() - KEEP_DAYS_MS
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("ez1-debug-") && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    private fun priorityToChar(priority: Int): Char = when (priority) {
        android.util.Log.VERBOSE -> 'V'
        android.util.Log.DEBUG -> 'D'
        android.util.Log.INFO -> 'I'
        android.util.Log.WARN -> 'W'
        android.util.Log.ERROR -> 'E'
        else -> '?'
    }

    companion object {
        private const val MAX_FILE_SIZE = 2 * 1024 * 1024L
        private const val KEEP_DAYS_MS = 3 * 24 * 60 * 60 * 1000L
    }
}
