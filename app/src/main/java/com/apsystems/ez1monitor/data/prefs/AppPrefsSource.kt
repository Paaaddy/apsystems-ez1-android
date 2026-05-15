package com.apsystems.ez1monitor.data.prefs

import kotlinx.coroutines.flow.Flow

interface AppPrefsSource {
    val ipAddress: Flow<String>
    val port: Flow<Int>
    val pollIntervalSecs: Flow<Int>
    val isDemoMode: Flow<Boolean>
    val savedDeviceId: Flow<String>
    suspend fun saveConnection(ip: String, port: Int)
    suspend fun savePollInterval(secs: Int)
    suspend fun clearConnection()
    suspend fun setDemoMode(enabled: Boolean)
    suspend fun saveDeviceId(id: String)
}
