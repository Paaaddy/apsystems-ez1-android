package com.apsystems.ez1monitor.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ez1_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_IP = stringPreferencesKey("inverter_ip")
        private val KEY_PORT = intPreferencesKey("inverter_port")
        private val KEY_INTERVAL = intPreferencesKey("poll_interval_secs")

        const val DEFAULT_PORT = 8050
        const val DEFAULT_INTERVAL = 30
    }

    val ipAddress: Flow<String> = context.dataStore.data.map { it[KEY_IP] ?: "" }
    val port: Flow<Int> = context.dataStore.data.map { it[KEY_PORT] ?: DEFAULT_PORT }
    val pollIntervalSecs: Flow<Int> = context.dataStore.data.map { it[KEY_INTERVAL] ?: DEFAULT_INTERVAL }

    suspend fun saveConnection(ip: String, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IP] = ip
            prefs[KEY_PORT] = port
        }
    }

    suspend fun savePollInterval(secs: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INTERVAL] = secs
        }
    }

    suspend fun clearConnection() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_IP)
        }
    }
}
