package com.apsystems.ez1monitor.data.repository

import com.apsystems.ez1monitor.data.api.models.AlarmInfo
import com.apsystems.ez1monitor.data.api.models.DeviceInfo
import com.apsystems.ez1monitor.data.api.models.OutputData

interface EZ1DataSource {
    suspend fun getDeviceInfo(ip: String, port: Int): EZ1Result<DeviceInfo>
    suspend fun getOutputData(ip: String, port: Int): EZ1Result<OutputData>
    suspend fun getOnOff(ip: String, port: Int): EZ1Result<Boolean>
    suspend fun setOnOff(ip: String, port: Int, on: Boolean): EZ1Result<Boolean>
    suspend fun getMaxPower(ip: String, port: Int): EZ1Result<Int>
    suspend fun setMaxPower(ip: String, port: Int, watts: Int, min: Int, max: Int): EZ1Result<Int>
    suspend fun getAlarm(ip: String, port: Int): EZ1Result<AlarmInfo>
}
