package com.apsystems.ez1monitor.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apsystems.ez1monitor.data.prefs.AppPreferences
import com.apsystems.ez1monitor.data.repository.EZ1Repository
import com.apsystems.ez1monitor.data.repository.EZ1Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupUiState(
    val ip: String = "",
    val port: String = "8050",
    val intervalSecs: String = "30",
    val isConnecting: Boolean = false,
    val error: String? = null,
    val connected: Boolean = false
)

class SetupViewModel(
    private val prefs: AppPreferences,
    private val repository: EZ1Repository
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.ipAddress.collect { saved ->
                if (saved.isNotBlank()) {
                    _state.value = _state.value.copy(ip = saved)
                }
            }
        }
        viewModelScope.launch {
            prefs.port.collect { saved ->
                _state.value = _state.value.copy(port = saved.toString())
            }
        }
        viewModelScope.launch {
            prefs.pollIntervalSecs.collect { saved ->
                _state.value = _state.value.copy(intervalSecs = saved.toString())
            }
        }
    }

    fun onIpChanged(value: String) {
        _state.value = _state.value.copy(ip = value, error = null)
    }

    fun onPortChanged(value: String) {
        _state.value = _state.value.copy(port = value, error = null)
    }

    fun onIntervalChanged(value: String) {
        _state.value = _state.value.copy(intervalSecs = value, error = null)
    }

    fun connect(onSuccess: () -> Unit) {
        val current = _state.value
        val ip = current.ip.trim()
        val port = current.port.trim().toIntOrNull()
        val interval = current.intervalSecs.trim().toIntOrNull()

        if (ip.isBlank()) {
            _state.value = current.copy(error = "Enter an IP address")
            return
        }
        if (port == null || port !in 1..65535) {
            _state.value = current.copy(error = "Invalid port number")
            return
        }

        _state.value = current.copy(isConnecting = true, error = null)

        viewModelScope.launch {
            when (val result = repository.getDeviceInfo(ip, port)) {
                is EZ1Result.Success -> {
                    prefs.saveConnection(ip, port)
                    if (interval != null) prefs.savePollInterval(interval.coerceIn(5, 3600))
                    _state.value = _state.value.copy(isConnecting = false, connected = true)
                    onSuccess()
                }
                is EZ1Result.Failure -> {
                    _state.value = _state.value.copy(
                        isConnecting = false,
                        error = result.message
                    )
                }
            }
        }
    }
}
