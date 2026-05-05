package com.apsystems.ez1monitor.data.api

import com.apsystems.ez1monitor.data.api.models.EZ1AlarmResponse
import com.apsystems.ez1monitor.data.api.models.EZ1DeviceInfoResponse
import com.apsystems.ez1monitor.data.api.models.EZ1MaxPowerResponse
import com.apsystems.ez1monitor.data.api.models.EZ1OnOffResponse
import com.apsystems.ez1monitor.data.api.models.EZ1OutputDataResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface EZ1ApiService {
    @GET("getDeviceInfo")
    suspend fun getDeviceInfo(): EZ1DeviceInfoResponse

    @GET("getOutputData")
    suspend fun getOutputData(): EZ1OutputDataResponse

    @GET("getMaxPower")
    suspend fun getMaxPower(): EZ1MaxPowerResponse

    @GET("setMaxPower")
    suspend fun setMaxPower(@Query("p") watts: Int): EZ1MaxPowerResponse

    @GET("getOnOff")
    suspend fun getOnOff(): EZ1OnOffResponse

    /**
     * status=0 → inverter ON, status=1 → inverter OFF
     * This is EZ1's own convention — inverted from the natural reading.
     */
    @GET("setOnOff")
    suspend fun setOnOff(@Query("status") status: Int): EZ1OnOffResponse

    @GET("getAlarm")
    suspend fun getAlarm(): EZ1AlarmResponse
}
