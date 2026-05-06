package com.apsystems.ez1monitor.data.repository

import com.apsystems.ez1monitor.data.api.models.AlarmInfo
import com.apsystems.ez1monitor.data.api.models.DeviceInfo
import com.apsystems.ez1monitor.data.api.models.OutputData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sin

class DemoDataSource : EZ1DataSource {

    private val mutex = Mutex()
    private var isOn = true
    private var maxPower = 800
    private var pollCount = 0

    private val deviceInfo = DeviceInfo(
        deviceId = "EZ1-DEMO-001",
        devVer = "v2.1.0",
        ssid = "HomeWiFi",
        ipAddr = "192.168.1.42",
        maxPower = 800,
        minPower = 30
    )

    override suspend fun getDeviceInfo(ip: String, port: Int): EZ1Result<DeviceInfo> =
        EZ1Result.Success(deviceInfo)

    override suspend fun getOutputData(ip: String, port: Int): EZ1Result<OutputData> = mutex.withLock {
        val t = pollCount++ * 0.3
        val cap = maxPower.toFloat()
        val p1 = if (isOn) (210f + sin(t).toFloat() * 30f).coerceIn(0f, cap) else 0f
        val p2 = if (isOn) (195f + sin(t + 1.0).toFloat() * 25f).coerceIn(0f, cap) else 0f
        EZ1Result.Success(
            OutputData(p1 = p1, p2 = p2, e1 = 1.24f, e2 = 1.18f, te1 = 48.7f, te2 = 46.3f)
        )
    }

    override suspend fun getOnOff(ip: String, port: Int): EZ1Result<Boolean> = mutex.withLock {
        EZ1Result.Success(isOn)
    }

    override suspend fun setOnOff(ip: String, port: Int, on: Boolean): EZ1Result<Boolean> = mutex.withLock {
        isOn = on
        EZ1Result.Success(isOn)
    }

    override suspend fun getMaxPower(ip: String, port: Int): EZ1Result<Int> = mutex.withLock {
        EZ1Result.Success(maxPower)
    }

    override suspend fun setMaxPower(ip: String, port: Int, watts: Int, min: Int, max: Int): EZ1Result<Int> = mutex.withLock {
        maxPower = watts.coerceIn(min, max)
        EZ1Result.Success(maxPower)
    }

    override suspend fun getAlarm(ip: String, port: Int): EZ1Result<AlarmInfo> =
        EZ1Result.Success(
            AlarmInfo(offGrid = false, shortCircuit1 = false, shortCircuit2 = false, outputFault = false)
        )
}
