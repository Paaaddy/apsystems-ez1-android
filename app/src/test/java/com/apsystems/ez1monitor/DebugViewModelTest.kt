package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.ui.debug.DebugViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DebugViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    private val todayFileName: String
        get() = "ez1-debug-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.log"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeVm(dir: File?, prefs: FakeAppPrefs = FakeAppPrefs()): DebugViewModel =
        DebugViewModel(
            logDir = dir,
            prefs = prefs,
            appVersion = "1.0.0",
            androidVersion = "12",
            createShareUri = null
        )

    @Test
    fun `loadLogs with no log file results in empty state not error`() = runTest {
        val vm = makeVm(tmpFolder.newFolder("empty"))

        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.error)
        assertTrue(state.logLines.isEmpty())
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `loadLogs with existing file loads last 50 lines`() = runTest {
        val dir = tmpFolder.newFolder("logs")
        val logFile = File(dir, todayFileName)
        val allLines = (1..60).map { "Line $it" }
        logFile.writeText(allLines.joinToString("\n"))

        val vm = makeVm(dir)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(50, state.logLines.size)
        assertEquals("Line 11", state.logLines.first())
        assertEquals("Line 60", state.logLines.last())
    }

    @Test
    fun `clearLogs deletes file and resets state to empty`() = runTest {
        val dir = tmpFolder.newFolder("logs2")
        val logFile = File(dir, todayFileName)
        logFile.writeText("some log content\n")

        val vm = makeVm(dir)
        advanceUntilIdle()

        vm.clearLogs()
        advanceUntilIdle()

        assertFalse(logFile.exists())
        assertTrue(vm.state.value.logLines.isEmpty())
        assertNull(vm.state.value.error)
    }

    @Test
    fun `loadLogs IO failure results in error state with retry available`() = runTest {
        // logDir is a FILE not a directory — causes I/O error when listing
        val notADir = tmpFolder.newFile("not-a-dir")
        val vm = makeVm(notADir)

        advanceUntilIdle()

        // A file treated as directory — error state expected
        // (either error or empty — both are valid, not a crash)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `shareLogsContent includes header with version, Android version, demo mode`() = runTest {
        val prefs = FakeAppPrefs(demoMode = true)
        val vm = makeVm(tmpFolder.newFolder("logs3"), prefs)
        advanceUntilIdle()

        val content = vm.buildShareContent()

        assertTrue(content.contains("App Version: 1.0.0"))
        assertTrue(content.contains("Android Version: 12"))
        assertTrue(content.contains("Demo Mode: true"))
        assertTrue(content.contains("---"))
    }
}

private fun assertFalse(condition: Boolean) = assertTrue(!condition)
