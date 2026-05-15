package com.apsystems.ez1monitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apsystems.ez1monitor.data.api.models.AlarmInfo
import com.apsystems.ez1monitor.data.api.models.DeviceInfo
import com.apsystems.ez1monitor.data.api.models.OutputData
import com.apsystems.ez1monitor.data.notifications.NotificationHelper
import com.apsystems.ez1monitor.data.prefs.AppPrefsSource
import com.apsystems.ez1monitor.data.repository.EZ1DataSource
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
    val isDemoMode: Boolean = false,
    val deviceInfo: DeviceInfo? = null,
    val outputData: OutputData? = null,
    val isOn: Boolean? = null,
    val currentMaxPower: Int = 0,
    val pendingMaxPower: Int = 0,
    val alarms: AlarmInfo? = null,
    val error: String? = null,
    val lastUpdatedMs: Long = 0L,
    val isCommandInFlight: Boolean = false,
    val snackbarMessage: String? = null,
    val deviceIdMismatch: Boolean = false
) {
    val isDataStale: Boolean get() = !isConnected && outputData != null && lastUpdatedMs > 0
}

private const val CONNECTION_LOST_THRESHOLD = 5
private const val POWER_PRODUCING_THRESHOLD_W = 10f

class DashboardViewModel(
    private val prefs: AppPrefsSource,
    private val dataSource: EZ1DataSource,
    private val notificationHelper: NotificationHelper? = null
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var consecutiveFailures = 0

    // Notification state — track transitions to avoid repeat-firing
    private var prevHadAlarm = false
    private var prevTotalWatts = -1f
    private var connectionLostNotified = false

    init {
        startPolling()
    }

    fun startPolling() {
        pollJob?.cancel()
        consecutiveFailures = 0
        pollJob = viewModelScope.launch {
            val isDemo = prefs.isDemoMode.first()
            val ip = prefs.ipAddress.first()
            val port = prefs.port.first()
            val intervalSecs = prefs.pollIntervalSecs.first()

            _state.value = _state.value.copy(isDemoMode = isDemo)

            if (!isDemo && ip.isBlank()) {
                _state.value = _state.value.copy(isLoading = false, error = "No inverter configured")
                return@launch
            }

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
            startPolling()
        }
    }

    private suspend fun poll(ip: String, port: Int) {
        Timber.d("Polling %s:%d", ip, port)

        val deviceInfoResult = if (_state.value.deviceInfo == null) {
            dataSource.getDeviceInfo(ip, port)
        } else null

        val outputResult = dataSource.getOutputData(ip, port)
        val onOffResult = dataSource.getOnOff(ip, port)
        val maxPowerResult = dataSource.getMaxPower(ip, port)
        val alarmResult = dataSource.getAlarm(ip, port)

        if (outputResult is EZ1Result.Success) {
            consecutiveFailures = 0
            connectionLostNotified = false
            notificationHelper?.cancelConnectionLost()

            val current = _state.value
            val newDeviceInfo = (deviceInfoResult as? EZ1Result.Success)?.value
            val newAlarms = (alarmResult as? EZ1Result.Success)?.value

            // Device fingerprint check — warn if stored ID doesn't match actual
            val mismatch = if (newDeviceInfo != null) {
                checkDeviceIdMismatch(newDeviceInfo.deviceId)
            } else current.deviceIdMismatch

            _state.value = current.copy(
                isLoading = false,
                isConnected = true,
                error = null,
                lastUpdatedMs = System.currentTimeMillis(),
                deviceInfo = newDeviceInfo ?: current.deviceInfo,
                outputData = outputResult.value,
                isOn = (onOffResult as? EZ1Result.Success)?.value ?: current.isOn,
                currentMaxPower = (maxPowerResult as? EZ1Result.Success)?.value ?: current.currentMaxPower,
                pendingMaxPower = if (current.pendingMaxPower == 0)
                    (maxPowerResult as? EZ1Result.Success)?.value ?: current.pendingMaxPower
                else current.pendingMaxPower,
                alarms = newAlarms ?: current.alarms,
                deviceIdMismatch = mismatch
            )

            fireTransitionNotifications(
                newAlarms = newAlarms ?: current.alarms,
                newTotalWatts = outputResult.value.pTotal,
                isInverterOn = (onOffResult as? EZ1Result.Success)?.value ?: current.isOn ?: true
            )
        } else {
            consecutiveFailures++
            val errorMsg = (outputResult as? EZ1Result.Failure)?.message ?: "Connection lost"
            _state.value = _state.value.copy(
                isConnected = false,
                isLoading = false,
                error = errorMsg
            )
            if (consecutiveFailures == CONNECTION_LOST_THRESHOLD && !connectionLostNotified) {
                notificationHelper?.postConnectionLostNotification()
                connectionLostNotified = true
            }
        }
    }

    private suspend fun checkDeviceIdMismatch(actualId: String): Boolean {
        val saved = prefs.savedDeviceId.first()
        return saved.isNotBlank() && actualId != saved
    }

    private fun fireTransitionNotifications(
        newAlarms: AlarmInfo?,
        newTotalWatts: Float,
        isInverterOn: Boolean
    ) {
        val nowHasAlarm = newAlarms?.hasAlarm ?: false
        if (nowHasAlarm && !prevHadAlarm) {
            val desc = newAlarms?.describe() ?: "Unknown alarm"
            notificationHelper?.postAlarmNotification(desc)
        } else if (!nowHasAlarm && prevHadAlarm) {
            notificationHelper?.cancelAlarm()
        }
        prevHadAlarm = nowHasAlarm

        if (prevTotalWatts >= POWER_PRODUCING_THRESHOLD_W && newTotalWatts < 1f && isInverterOn) {
            notificationHelper?.postPowerLostNotification()
        } else if (newTotalWatts >= POWER_PRODUCING_THRESHOLD_W) {
            notificationHelper?.cancelPowerLost()
        }
        prevTotalWatts = newTotalWatts
    }

    fun trustNewDevice() {
        val deviceId = _state.value.deviceInfo?.deviceId ?: return
        viewModelScope.launch {
            prefs.saveDeviceId(deviceId)
            _state.value = _state.value.copy(deviceIdMismatch = false)
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

            when (val result = dataSource.setOnOff(ip, port, newState)) {
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

            val result = dataSource.setMaxPower(
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

private fun AlarmInfo.describe(): String = buildList {
    if (offGrid) add("Off-grid")
    if (shortCircuit1) add("Short circuit Ch1")
    if (shortCircuit2) add("Short circuit Ch2")
    if (outputFault) add("Output fault")
}.joinToString(", ").ifEmpty { "Unknown alarm" }
