package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.repository.EZ1Result
import com.apsystems.ez1monitor.ui.setup.SetupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

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
    fun `connect with blank IP sets error, does not call repository`() = runTest {
        val prefs = FakeAppPrefs(ip = "")
        val dataSource = FakeEZ1DataSource()
        val vm = SetupViewModel(prefs, dataSource)

        vm.connect(onSuccess = {})
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertEquals(0, dataSource.outputDataCallCount)
    }

    @Test
    fun `connect with invalid port sets error`() = runTest {
        val prefs = FakeAppPrefs(ip = "192.168.1.1")
        val dataSource = FakeEZ1DataSource()
        val vm = SetupViewModel(prefs, dataSource)
        vm.onIpChanged("192.168.1.1")
        vm.onPortChanged("99999")

        vm.connect(onSuccess = {})
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `connect clears demoMode pref on success`() = runTest {
        val prefs = FakeAppPrefs(ip = "192.168.1.1", demoMode = true)
        val dataSource = FakeEZ1DataSource()
        var callbackInvoked = false
        val vm = SetupViewModel(prefs, dataSource)
        vm.onIpChanged("192.168.1.1")
        vm.onPortChanged("8050")

        vm.connect(onSuccess = { callbackInvoked = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackInvoked)
        val demoMode = prefs.isDemoMode.first()
        assertTrue(!demoMode)
    }

    @Test
    fun `enterDemoMode sets demoMode pref and calls onConnected`() = runTest {
        val prefs = FakeAppPrefs()
        val vm = SetupViewModel(prefs, FakeEZ1DataSource())
        var callbackInvoked = false

        vm.enterDemoMode(onSuccess = { callbackInvoked = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertTrue(prefs.isDemoMode.first())
    }
}
