package com.apsystems.ez1monitor.data.api.models

import com.google.gson.annotations.SerializedName

data class EZ1DeviceInfoResponse(
    @SerializedName("data") val data: DeviceInfoData?,
    @SerializedName("message") val message: String?,
    @SerializedName("deviceSn") val deviceSn: String?
)

data class DeviceInfoData(
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("devVer") val devVer: String?,
    @SerializedName("ssid") val ssid: String?,
    @SerializedName("ipAddr") val ipAddr: String?,
    @SerializedName("maxPower") val maxPower: Int?,
    @SerializedName("minPower") val minPower: Int?
)

data class EZ1OutputDataResponse(
    @SerializedName("data") val data: OutputDataData?,
    @SerializedName("message") val message: String?,
    @SerializedName("deviceSn") val deviceSn: String?
)

data class OutputDataData(
    @SerializedName("p1") val p1: Float?,
    @SerializedName("p2") val p2: Float?,
    @SerializedName("e1") val e1: Float?,
    @SerializedName("e2") val e2: Float?,
    @SerializedName("te1") val te1: Float?,
    @SerializedName("te2") val te2: Float?
)

data class EZ1OnOffResponse(
    @SerializedName("data") val data: OnOffData?,
    @SerializedName("message") val message: String?,
    @SerializedName("deviceSn") val deviceSn: String?
)

data class OnOffData(
    // status=0 means ON, status=1 means OFF (EZ1 API convention — inverted from intuition)
    @SerializedName("status") val status: Int?
)

data class EZ1MaxPowerResponse(
    @SerializedName("data") val data: MaxPowerData?,
    @SerializedName("message") val message: String?,
    @SerializedName("deviceSn") val deviceSn: String?
)

data class MaxPowerData(
    @SerializedName("maxPower") val maxPower: Int?
)

data class EZ1AlarmResponse(
    @SerializedName("data") val data: AlarmData?,
    @SerializedName("message") val message: String?,
    @SerializedName("deviceSn") val deviceSn: String?
)

data class AlarmData(
    @SerializedName("offGrid") val offGrid: String?,
    @SerializedName("shortCircuitError_1") val shortCircuitError1: String?,
    @SerializedName("shortCircuitError_2") val shortCircuitError2: String?,
    @SerializedName("outputFault") val outputFault: String?
)
