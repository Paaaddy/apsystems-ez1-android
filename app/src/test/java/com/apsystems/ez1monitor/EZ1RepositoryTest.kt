package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.repository.EZ1Repository
import com.apsystems.ez1monitor.data.repository.EZ1Result
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EZ1RepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: EZ1Repository
    private lateinit var ip: String
    private var port: Int = 0

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        ip = server.hostName
        port = server.port
        repo = EZ1Repository()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // --- getDeviceInfo ---

    @Test
    fun `getDeviceInfo returns Success on valid response`() = runBlocking {
        server.enqueue(MockResponse().setBody("""
            {"data":{"deviceId":"SN123","devVer":"V1.0","ssid":"Home","ipAddr":"192.168.1.1","maxPower":800,"minPower":30},"message":"SUCCESS"}
        """.trimIndent()))

        val result = repo.getDeviceInfo(ip, port)
        assertTrue(result is EZ1Result.Success)
        val info = (result as EZ1Result.Success).value
        assertEquals("SN123", info.deviceId)
        assertEquals(800, info.maxPower)
        assertEquals(30, info.minPower)
    }

    @Test
    fun `getDeviceInfo returns Failure when device not EZ1`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{},"message":"SUCCESS"}"""))

        val result = repo.getDeviceInfo(ip, port)
        assertTrue(result is EZ1Result.Failure)
    }

    @Test
    fun `getDeviceInfo returns Failure on timeout`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))

        val result = repo.getDeviceInfo(ip, port)
        assertTrue(result is EZ1Result.Failure)
    }

    // --- getOutputData ---

    @Test
    fun `getOutputData maps p1 p2 e1 e2 te1 te2 correctly`() = runBlocking {
        server.enqueue(MockResponse().setBody("""
            {"data":{"p1":125.5,"p2":118.3,"e1":1.23,"e2":1.15,"te1":234.56,"te2":220.11},"message":"SUCCESS"}
        """.trimIndent()))

        val result = repo.getOutputData(ip, port)
        assertTrue(result is EZ1Result.Success)
        val data = (result as EZ1Result.Success).value
        assertEquals(125.5f, data.p1, 0.01f)
        assertEquals(118.3f, data.p2, 0.01f)
        assertEquals(243.8f, data.pTotal, 0.1f) // p1 + p2
    }

    // --- setOnOff direction ---

    @Test
    fun `setOnOff(true) sends status=0 (EZ1 ON convention)`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"status":0},"message":"SUCCESS"}"""))

        repo.setOnOff(ip, port, on = true)

        val request = server.takeRequest()
        assertTrue(request.path?.contains("status=0") == true)
    }

    @Test
    fun `setOnOff(false) sends status=1 (EZ1 OFF convention)`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"status":1},"message":"SUCCESS"}"""))

        repo.setOnOff(ip, port, on = false)

        val request = server.takeRequest()
        assertTrue(request.path?.contains("status=1") == true)
    }

    @Test
    fun `setOnOff returns correct inverter state from response`() = runBlocking {
        // Response says status=0 → inverter is ON
        server.enqueue(MockResponse().setBody("""{"data":{"status":0},"message":"SUCCESS"}"""))

        val result = repo.setOnOff(ip, port, on = true)
        assertTrue(result is EZ1Result.Success)
        assertTrue((result as EZ1Result.Success).value) // true = on
    }

    // --- setMaxPower clamping ---

    @Test
    fun `setMaxPower clamps value to min-max range`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"maxPower":800},"message":"SUCCESS"}"""))

        // Attempt to set 2000W on a device with max 800W
        repo.setMaxPower(ip, port, watts = 2000, min = 30, max = 800)

        val request = server.takeRequest()
        assertTrue(request.path?.contains("p=800") == true)
    }

    @Test
    fun `setMaxPower clamps value to min`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"maxPower":30},"message":"SUCCESS"}"""))

        repo.setMaxPower(ip, port, watts = 5, min = 30, max = 800)

        val request = server.takeRequest()
        assertTrue(request.path?.contains("p=30") == true)
    }

    // --- getOnOff ---

    @Test
    fun `getOnOff status=0 returns true (inverter on)`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"status":0},"message":"SUCCESS"}"""))

        val result = repo.getOnOff(ip, port)
        assertTrue(result is EZ1Result.Success)
        assertTrue((result as EZ1Result.Success).value)
    }

    @Test
    fun `getOnOff status=1 returns false (inverter off)`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":{"status":1},"message":"SUCCESS"}"""))

        val result = repo.getOnOff(ip, port)
        assertTrue(result is EZ1Result.Success)
        assertFalse((result as EZ1Result.Success).value)
    }
}
