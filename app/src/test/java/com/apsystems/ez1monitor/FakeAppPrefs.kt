package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.prefs.AppPrefsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppPrefs(
    ip: String = "",
    port: Int = 8050,
    interval: Int = 30,
    demoMode: Boolean = false,
    deviceId: String = ""
) : AppPrefsSource {

    private val _ip = MutableStateFlow(ip)
    private val _port = MutableStateFlow(port)
    private val _interval = MutableStateFlow(interval)
    private val _demoMode = MutableStateFlow(demoMode)
    private val _deviceId = MutableStateFlow(deviceId)

    override val ipAddress: Flow<String> = _ip
    override val port: Flow<Int> = _port
    override val pollIntervalSecs: Flow<Int> = _interval
    override val isDemoMode: Flow<Boolean> = _demoMode
    override val savedDeviceId: Flow<String> = _deviceId

    override suspend fun saveConnection(ip: String, port: Int) {
        _ip.value = ip
        _port.value = port
    }

    override suspend fun savePollInterval(secs: Int) {
        _interval.value = secs
    }

    override suspend fun clearConnection() {
        _ip.value = ""
    }

    override suspend fun setDemoMode(enabled: Boolean) {
        _demoMode.value = enabled
    }

    override suspend fun saveDeviceId(id: String) {
        _deviceId.value = id
    }
}
