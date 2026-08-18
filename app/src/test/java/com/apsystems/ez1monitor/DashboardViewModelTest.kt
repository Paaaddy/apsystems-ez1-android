package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.prefs.AppPrefsSource
import com.apsystems.ez1monitor.data.repository.EZ1DataSource
import com.apsystems.ez1monitor.data.repository.EZ1Result
import com.apsystems.ez1monitor.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    private val createdViewModels = mutableListOf<DashboardViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        cancelAllViewModels()
        Dispatchers.resetMain()
    }

    private fun createVm(
        prefs: AppPrefsSource,
        dataSource: EZ1DataSource
    ): DashboardViewModel = DashboardViewModel(prefs, dataSource).also { createdViewModels.add(it) }

    // DashboardViewModel starts an infinite polling loop (viewModelScope.launch { while (true)
    // { delay(...); poll() } }) in init. runTest's *internal* end-of-test-body advanceUntilIdle
    // runs before the test lambda returns — i.e. before @After — and spins forever trying to
    // drain a scheduler that always has more (delayed) work queued if that job is still alive.
    // So every test must cancel it itself, as the last thing it does; tearDown() is only a
    // backstop for the (rarer) case where the test throws before reaching its last line.
    private fun cancelAllViewModels() {
        createdViewModels.forEach { it.onCleared() }
        createdViewModels.clear()
    }

    @Test
    fun `startPolling in demo mode with blank IP shows data, not error`() = runTest {
        val prefs = FakeAppPrefs(ip = "", demoMode = true)
        val dataSource = FakeEZ1DataSource()
        val vm = createVm(prefs, dataSource)

        runCurrent()

        val state = vm.state.value
        assertNull(state.error)
        assertNotNull(state.outputData)
        assertTrue(state.isDemoMode)
        cancelAllViewModels()
    }

    @Test
    fun `blank IP without demo mode shows error`() = runTest {
        val prefs = FakeAppPrefs(ip = "", demoMode = false)
        val vm = createVm(prefs, FakeEZ1DataSource())

        runCurrent()

        assertEquals("No inverter configured", vm.state.value.error)
        cancelAllViewModels()
    }

    @Test
    fun `consecutive getOutputData failures trigger 60s backoff after first fail`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1", interval = 30)
        val dataSource = FakeEZ1DataSource()
        dataSource.defaultOutputData = EZ1Result.Failure("timeout")
        val vm = createVm(prefs, dataSource)

        runCurrent() // first poll — fails → consecutiveFailures=1
        val callsAfterFirst = dataSource.outputDataCallCount

        // Advance 59s — no new poll expected
        advanceTimeBy(59_000L)
        assertEquals(callsAfterFirst, dataSource.outputDataCallCount)

        // Advance 1 more second → 60s elapsed — second poll fires
        advanceTimeBy(1_001L)
        assertTrue(dataSource.outputDataCallCount > callsAfterFirst)
        cancelAllViewModels()
    }

    @Test
    fun `two consecutive failures trigger 120s backoff`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1", interval = 30)
        val dataSource = FakeEZ1DataSource()
        dataSource.defaultOutputData = EZ1Result.Failure("timeout")
        val vm = createVm(prefs, dataSource)

        // First poll + 60s backoff = second poll
        runCurrent()
        advanceTimeBy(61_000L)

        val callsAfterSecond = dataSource.outputDataCallCount

        // Next backoff is 120s from t=60s → fires at t=180s. We're at t=61s, advance 118s to t=179s.
        advanceTimeBy(118_000L)
        assertEquals(callsAfterSecond, dataSource.outputDataCallCount)

        advanceTimeBy(2_000L)
        assertTrue(dataSource.outputDataCallCount > callsAfterSecond)
        cancelAllViewModels()
    }

    @Test
    fun `refresh resets consecutiveFailures, next delay is normal interval`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1", interval = 30)
        val dataSource = FakeEZ1DataSource()
        dataSource.defaultOutputData = EZ1Result.Failure("timeout")
        val vm = createVm(prefs, dataSource)

        runCurrent() // first poll fails

        dataSource.defaultOutputData = EZ1Result.Success(testOutputData)
        vm.refresh()
        runCurrent() // refresh poll succeeds

        val callsAfterRefresh = dataSource.outputDataCallCount

        // Next poll should come after 30s (normal interval), not 60s
        advanceTimeBy(29_000L)
        assertEquals(callsAfterRefresh, dataSource.outputDataCallCount)

        advanceTimeBy(2_000L)
        assertTrue(dataSource.outputDataCallCount > callsAfterRefresh)
        cancelAllViewModels()
    }

    @Test
    fun `toggleOnOff sends setOnOff with correct value`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1")
        val dataSource = FakeEZ1DataSource()
        val vm = createVm(prefs, dataSource)

        runCurrent() // first poll sets isOn = true

        vm.toggleOnOff()
        runCurrent()

        assertEquals(1, dataSource.setOnOffCallCount)
        assertEquals(false, dataSource.lastSetOnOffValue)
        cancelAllViewModels()
    }

    @Test
    fun `confirmSetMaxPower reverts slider on failure`() = runTest {
        val prefs = FakeAppPrefs(ip = "10.0.0.1")
        val failingDs = object : FakeEZ1DataSource() {
            override suspend fun setMaxPower(ip: String, port: Int, watts: Int, min: Int, max: Int): EZ1Result<Int> =
                EZ1Result.Failure("Device error")
        }
        val vm = createVm(prefs, failingDs)
        runCurrent()

        val currentBefore = vm.state.value.currentMaxPower
        vm.onPendingMaxPowerChanged(400)
        vm.confirmSetMaxPower()
        runCurrent()

        assertEquals(currentBefore, vm.state.value.pendingMaxPower)
        cancelAllViewModels()
    }
}
