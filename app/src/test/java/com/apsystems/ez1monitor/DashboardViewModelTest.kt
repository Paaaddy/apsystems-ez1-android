package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.repository.EZ1Result
import com.apsystems.ez1monitor.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startPolling in demo mode with blank IP shows data, not error`() = runTest {
        val prefs = FakeAppPrefs(ip = "", demoMode = true)
        val dataSource = FakeEZ1DataSource()
        val vm = DashboardViewModel(prefs, dataSource)

        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.error)
        assertNotNull(state.outputData)
        assertTrue(state.isDemoMode)
    }

    @Test
    fun `blank IP without demo mode shows error`() = runTest {
        val prefs = FakeAppPrefs(ip = "", demoMode = false)
        val vm = DashboardViewModel(prefs, FakeEZ1DataSource())

        advanceUntilIdle()

        assertEquals("No inverter configured", vm.state.value.error)
    }

    @Test
    fun `consecutive getOutputData failures trigger 60s backoff after first fail`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1", interval = 30)
        val dataSource = FakeEZ1DataSource()
        dataSource.defaultOutputData = EZ1Result.Failure("timeout")
        val vm = DashboardViewModel(prefs, dataSource)

        advanceUntilIdle() // first poll — fails → consecutiveFailures=1
        val callsAfterFirst = dataSource.outputDataCallCount

        // Advance 59s — no new poll expected
        advanceTimeBy(59_000L)
        assertEquals(callsAfterFirst, dataSource.outputDataCallCount)

        // Advance 1 more second → 60s elapsed — second poll fires
        advanceTimeBy(1_001L)
        assertTrue(dataSource.outputDataCallCount > callsAfterFirst)
    }

    @Test
    fun `two consecutive failures trigger 120s backoff`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1", interval = 30)
        val dataSource = FakeEZ1DataSource()
        dataSource.defaultOutputData = EZ1Result.Failure("timeout")
        val vm = DashboardViewModel(prefs, dataSource)

        // First poll + 60s backoff = second poll
        advanceUntilIdle()
        advanceTimeBy(61_000L)

        val callsAfterSecond = dataSource.outputDataCallCount

        // Next backoff should be 120s
        advanceTimeBy(119_000L)
        assertEquals(callsAfterSecond, dataSource.outputDataCallCount)

        advanceTimeBy(2_000L)
        assertTrue(dataSource.outputDataCallCount > callsAfterSecond)
    }

    @Test
    fun `refresh resets consecutiveFailures, next delay is normal interval`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1", interval = 30)
        val dataSource = FakeEZ1DataSource()
        dataSource.defaultOutputData = EZ1Result.Failure("timeout")
        val vm = DashboardViewModel(prefs, dataSource)

        advanceUntilIdle() // first poll fails

        dataSource.defaultOutputData = EZ1Result.Success(testOutputData)
        vm.refresh()
        advanceUntilIdle() // refresh poll succeeds

        val callsAfterRefresh = dataSource.outputDataCallCount

        // Next poll should come after 30s (normal interval), not 60s
        advanceTimeBy(29_000L)
        assertEquals(callsAfterRefresh, dataSource.outputDataCallCount)

        advanceTimeBy(2_000L)
        assertTrue(dataSource.outputDataCallCount > callsAfterRefresh)
    }

    @Test
    fun `toggleOnOff sends setOnOff with correct value`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1")
        val dataSource = FakeEZ1DataSource()
        val vm = DashboardViewModel(prefs, dataSource)

        advanceUntilIdle() // first poll sets isOn = true

        vm.toggleOnOff()
        advanceUntilIdle()

        assertEquals(1, dataSource.setOnOffCallCount)
        assertEquals(false, dataSource.lastSetOnOffValue)
    }

    @Test
    fun `confirmSetMaxPower reverts slider on failure`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1")
        val failingDs = object : FakeEZ1DataSource() {
            override suspend fun setMaxPower(ip: String, port: Int, watts: Int, min: Int, max: Int): EZ1Result<Int> =
                EZ1Result.Failure("Device error")
        }
        val vm = DashboardViewModel(prefs, failingDs)
        advanceUntilIdle()

        val currentBefore = vm.state.value.currentMaxPower
        vm.onPendingMaxPowerChanged(400)
        vm.confirmSetMaxPower()
        advanceUntilIdle()

        assertEquals(currentBefore, vm.state.value.pendingMaxPower)
    }
}
