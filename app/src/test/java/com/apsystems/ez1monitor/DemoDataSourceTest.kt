package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.repository.EZ1Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// DemoDataSource is in the debug source set, so these tests run only in debug builds.
// The class is available from src/debug when running ./gradlew test (debug variant).
@OptIn(ExperimentalCoroutinesApi::class)
class DemoDataSourceTest {

    private fun makeSource() = com.apsystems.ez1monitor.data.repository.DemoDataSource()

    @Test
    fun `getOutputData returns realistic values`() = runTest {
        val ds = makeSource()
        val result = ds.getOutputData("", 0)
        assertTrue(result is EZ1Result.Success)
        val data = (result as EZ1Result.Success).value
        assertTrue(data.p1 >= 0f)
        assertTrue(data.p2 >= 0f)
        assertTrue(data.e1 > 0f)
    }

    @Test
    fun `setOnOff false causes getOnOff to return false`() = runTest {
        val ds = makeSource()
        ds.setOnOff("", 0, false)
        val result = ds.getOnOff("", 0)
        assertTrue(result is EZ1Result.Success)
        assertFalse((result as EZ1Result.Success).value)
    }

    @Test
    fun `setMaxPower 400 causes getMaxPower to return 400`() = runTest {
        val ds = makeSource()
        ds.setMaxPower("", 0, 400, 30, 800)
        val result = ds.getMaxPower("", 0)
        assertTrue(result is EZ1Result.Success)
        assertEquals(400, (result as EZ1Result.Success).value)
    }

    @Test
    fun `concurrent setOnOff and getOnOff do not race`() = runTest {
        val ds = makeSource()
        val errors = mutableListOf<Throwable>()

        val j1 = launch {
            repeat(50) {
                try {
                    ds.setOnOff("", 0, it % 2 == 0)
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }
        val j2 = launch {
            repeat(50) {
                try {
                    ds.getOnOff("", 0)
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }
        j1.join()
        j2.join()

        assertTrue("Concurrency errors: $errors", errors.isEmpty())
    }

    @Test
    fun `getDeviceInfo always returns EZ1-DEMO-001 with maxPower 800`() = runTest {
        val ds = makeSource()
        val result = ds.getDeviceInfo("", 0)
        assertTrue(result is EZ1Result.Success)
        val info = (result as EZ1Result.Success).value
        assertEquals("EZ1-DEMO-001", info.deviceId)
        assertEquals(800, info.maxPower)
    }
}
