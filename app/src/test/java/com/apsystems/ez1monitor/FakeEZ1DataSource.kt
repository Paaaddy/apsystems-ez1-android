package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.api.models.AlarmInfo
import com.apsystems.ez1monitor.data.api.models.DeviceInfo
import com.apsystems.ez1monitor.data.api.models.OutputData
import com.apsystems.ez1monitor.data.repository.EZ1DataSource
import com.apsystems.ez1monitor.data.repository.EZ1Result

val testOutputData = OutputData(p1 = 210f, p2 = 195f, e1 = 1.24f, e2 = 1.18f, te1 = 48.7f, te2 = 46.3f)
val testDeviceInfo = DeviceInfo(
    deviceId = "EZ1-TEST-001",
    devVer = "v2.0.0",
    ssid = "TestWifi",
    ipAddr = "10.0.0.1",
    maxPower = 800,
    minPower = 30
)
val testAlarmInfo = AlarmInfo(offGrid = false, shortCircuit1 = false, shortCircuit2 = false, outputFault = false)

class FakeEZ1DataSource : EZ1DataSource {

    val outputDataResults = ArrayDeque<EZ1Result<OutputData>>()
    val deviceInfoResults = ArrayDeque<EZ1Result<DeviceInfo>>()

    var defaultOutputData: EZ1Result<OutputData> = EZ1Result.Success(testOutputData)
    var defaultDeviceInfo: EZ1Result<DeviceInfo> = EZ1Result.Success(testDeviceInfo)

    var outputDataCallCount = 0
    var setOnOffCallCount = 0
    var lastSetOnOffValue: Boolean? = null

    var responseDelayMs: Long = 0L

    private var currentOnOff = true
    private var currentMaxPower = 800

    override suspend fun getDeviceInfo(ip: String, port: Int): EZ1Result<DeviceInfo> =
        deviceInfoResults.removeFirstOrNull() ?: defaultDeviceInfo

    override suspend fun getOutputData(ip: String, port: Int): EZ1Result<OutputData> {
        outputDataCallCount++
        return outputDataResults.removeFirstOrNull() ?: defaultOutputData
    }

    override suspend fun getOnOff(ip: String, port: Int): EZ1Result<Boolean> =
        EZ1Result.Success(currentOnOff)

    override suspend fun setOnOff(ip: String, port: Int, on: Boolean): EZ1Result<Boolean> {
        setOnOffCallCount++
        lastSetOnOffValue = on
        currentOnOff = on
        return EZ1Result.Success(currentOnOff)
    }

    override suspend fun getMaxPower(ip: String, port: Int): EZ1Result<Int> =
        EZ1Result.Success(currentMaxPower)

    override suspend fun setMaxPower(ip: String, port: Int, watts: Int, min: Int, max: Int): EZ1Result<Int> {
        currentMaxPower = watts.coerceIn(min, max)
        return EZ1Result.Success(currentMaxPower)
    }

    override suspend fun getAlarm(ip: String, port: Int): EZ1Result<AlarmInfo> =
        EZ1Result.Success(testAlarmInfo)
}
