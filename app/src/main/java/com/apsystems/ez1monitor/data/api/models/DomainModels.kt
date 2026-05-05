package com.apsystems.ez1monitor.data.api.models

data class DeviceInfo(
    val deviceId: String,
    val devVer: String,
    val ssid: String,
    val ipAddr: String,
    val maxPower: Int,
    val minPower: Int
)

data class OutputData(
    val p1: Float,
    val p2: Float,
    val e1: Float,
    val e2: Float,
    val te1: Float,
    val te2: Float
) {
    val pTotal: Float get() = p1 + p2
    val eTotal: Float get() = e1 + e2
    val teTotal: Float get() = te1 + te2
}

data class AlarmInfo(
    val offGrid: Boolean,
    val shortCircuit1: Boolean,
    val shortCircuit2: Boolean,
    val outputFault: Boolean
) {
    val hasAlarm: Boolean get() = offGrid || shortCircuit1 || shortCircuit2 || outputFault
}

/** true = inverter on, false = inverter off */
fun Int.toInverterOn(): Boolean = this == 0

/** true (on) → status=0, false (off) → status=1 */
fun Boolean.toStatusParam(): Int = if (this) 0 else 1
