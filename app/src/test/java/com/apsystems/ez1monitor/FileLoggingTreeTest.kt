package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.logger.FileLoggingTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FileLoggingTreeTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val todayFile: String
        get() = "ez1-debug-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.log"

    @Test
    fun `log with null externalFilesDir does not crash`() {
        val tree = FileLoggingTree { null }
        // Should not throw
        tree.log(android.util.Log.DEBUG, "Tag", "Test message", null)
    }

    @Test
    fun `log writes timestamped line to file`() {
        val dir = tmpFolder.newFolder("logs")
        val tree = FileLoggingTree { dir }

        tree.log(android.util.Log.DEBUG, "TestTag", "Hello world", null)

        val logFile = File(dir, todayFile)
        assertTrue(logFile.exists())
        val content = logFile.readText()
        assertTrue(content.contains("TestTag"))
        assertTrue(content.contains("Hello world"))
        assertTrue(content.contains("[D]"))
    }

    @Test
    fun `log from two concurrent threads produces two lines without corruption`() {
        val dir = tmpFolder.newFolder("logs2")
        val tree = FileLoggingTree { dir }
        val executor = Executors.newFixedThreadPool(2)

        val f1 = executor.submit { tree.log(android.util.Log.DEBUG, "T1", "Message from thread 1", null) }
        val f2 = executor.submit { tree.log(android.util.Log.DEBUG, "T2", "Message from thread 2", null) }
        f1.get(5, TimeUnit.SECONDS)
        f2.get(5, TimeUnit.SECONDS)
        executor.shutdown()

        val logFile = File(dir, todayFile)
        val lines = logFile.readLines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        assertTrue(lines.any { it.contains("thread 1") })
        assertTrue(lines.any { it.contains("thread 2") })
    }

    @Test
    fun `cleanupOldLogs deletes files older than 3 days`() {
        val dir = tmpFolder.newFolder("logs3")
        val oldFile = File(dir, "ez1-debug-2020-01-01.log")
        oldFile.writeText("old log")
        oldFile.setLastModified(System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000L)

        val tree = FileLoggingTree { dir }
        tree.cleanupOldLogs()

        assertFalse(oldFile.exists())
    }

    @Test
    fun `cleanupOldLogs keeps files from today`() {
        val dir = tmpFolder.newFolder("logs4")
        val todayLogFile = File(dir, todayFile)
        todayLogFile.writeText("today log")

        val tree = FileLoggingTree { dir }
        tree.cleanupOldLogs()

        assertTrue(todayLogFile.exists())
    }
}
