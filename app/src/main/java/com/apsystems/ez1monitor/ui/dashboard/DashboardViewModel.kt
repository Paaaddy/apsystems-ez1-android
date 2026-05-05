package com.apsystems.ez1monitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apsystems.ez1monitor.data.api.models.AlarmInfo
import com.apsystems.ez1monitor.data.api.models.DeviceInfo
import com.apsystems.ez1monitor.data.api.models.OutputData
import com.apsystems.ez1monitor.data.prefs.AppPreferences
import com.apsystems.ez1monitor.data.repository.EZ1Repository
import com.apsystems.ez1monitor.data.repository.EZ1Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val deviceInfo: DeviceInfo? = null,
    val outputData: OutputData? = null,
    val isOn: Boolean? = null,           // null = unknown until first poll
    val currentMaxPower: Int = 0,
    val pendingMaxPower: Int = 0,
    val alarms: AlarmInfo? = null,
    val error: String? = null,
    val lastUpdatedMs: Long = 0L,
    val isCommandInFlight: Boolean = false,
    val snackbarMessage: String? = null
)

class DashboardViewModel(
    private val prefs: AppPreferences,
    private val repository: EZ1Repository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var consecutiveFailures = 0

    init {
        startPolling()
    }

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val ip = prefs.ipAddress.first()
            val port = prefs.port.first()
            val intervalSecs = prefs.pollIntervalSecs.first()

            if (ip.isBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "No inverter configured")
                return@launch
            }

            // First poll immediately, then schedule recurring
            poll(ip, port)

            while (true) {
                val backoffSecs = when {
                    consecutiveFailures == 0 -> intervalSecs.toLong()
                    consecutiveFailures == 1 -> 60L
                    else -> 120L
                }
                delay(backoffSecs * 1000L)
                poll(ip, port)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val ip = prefs.ipAddress.first()
            val port = prefs.port.first()
            consecutiveFailures = 0
            poll(ip, port)
            // Restart polling loop with reset backoff
            startPolling()
        }
    }

    private suspend fun poll(ip: String, port: Int) {
        Timber.d("Polling %s:%d", ip, port)

        // Fetch all data in sequence — mutex inside repository serializes these
        val deviceInfoResult = if (_state.value.deviceInfo == null) {
            repository.getDeviceInfo(ip, port)
        } else null

        val outputResult = repository.getOutputData(ip, port)
        val onOffResult = repository.getOnOff(ip, port)
        val maxPowerResult = repository.getMaxPower(ip, port)
        val alarmResult = repository.getAlarm(ip, port)

        val allSucceeded = outputResult is EZ1Result.Success

        if (allSucceeded) {
            consecutiveFailures = 0
            val current = _state.value
            _state.value = current.copy(
                isLoading = false,
                isConnected = true,
                error = null,
                lastUpdatedMs = System.currentTimeMillis(),
                deviceInfo = (deviceInfoResult as? EZ1Result.Success)?.value ?: current.deviceInfo,
                outputData = (outputResult as EZ1Result.Success).value,
                isOn = (onOffResult as? EZ1Result.Success)?.value ?: current.isOn,
                currentMaxPower = (maxPowerResult as? EZ1Result.Success)?.value ?: current.currentMaxPower,
                pendingMaxPower = if (current.pendingMaxPower == 0)
                    (maxPowerResult as? EZ1Result.Success)?.value ?: current.pendingMaxPower
                else current.pendingMaxPower,
                alarms = (alarmResult as? EZ1Result.Success)?.value ?: current.alarms
            )
        } else {
            consecutiveFailures++
            val errorMsg = (outputResult as? EZ1Result.Failure)?.message
                ?: "Connection lost"
            _state.value = _state.value.copy(
                isConnected = false,
                isLoading = false,
                error = errorMsg
            )
        }
    }

    fun toggleOnOff() {
        val current = _state.value
        if (current.isCommandInFlight || current.isOn == null) return

        val newState = !current.isOn
        viewModelScope.launch {
            val ip = prefs.ipAddress.first()
            val port = prefs.port.first()

            _state.value = _state.value.copy(isCommandInFlight = true)

            val result = repository.setOnOff(ip, port, newState)

            when (result) {
                is EZ1Result.Success -> {
                    _state.value = _state.value.copy(
                        isOn = result.value,
                        isCommandInFlight = false,
                        snackbarMessage = if (result.value) "Inverter turned on" else "Inverter turned off"
                    )
                }
                is EZ1Result.Failure -> {
                    _state.value = _state.value.copy(
                        isCommandInFlight = false,
                        snackbarMessage = "Command failed — try again"
                    )
                }
            }
        }
    }

    fun onPendingMaxPowerChanged(watts: Int) {
        _state.value = _state.value.copy(pendingMaxPower = watts)
    }

    fun confirmSetMaxPower() {
        val current = _state.value
        if (current.isCommandInFlight) return
        val deviceInfo = current.deviceInfo ?: return

        val target = current.pendingMaxPower

        viewModelScope.launch {
            val ip = prefs.ipAddress.first()
            val port = prefs.port.first()

            _state.value = _state.value.copy(isCommandInFlight = true)

            val result = repository.setMaxPower(
                ip, port, target,
                min = deviceInfo.minPower,
                max = deviceInfo.maxPower
            )

            when (result) {
                is EZ1Result.Success -> {
                    _state.value = _state.value.copy(
                        currentMaxPower = result.value,
                        pendingMaxPower = result.value,
                        isCommandInFlight = false,
                        snackbarMessage = "Max power set to ${result.value} W"
                    )
                }
                is EZ1Result.Failure -> {
                    // Revert slider to last confirmed value
                    _state.value = _state.value.copy(
                        pendingMaxPower = current.currentMaxPower,
                        isCommandInFlight = false,
                        snackbarMessage = "Failed to set max power — try again"
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
