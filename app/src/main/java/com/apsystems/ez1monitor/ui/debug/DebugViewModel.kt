package com.apsystems.ez1monitor.ui.debug

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apsystems.ez1monitor.data.prefs.AppPrefsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DebugUiState(
    val isLoading: Boolean = true,
    val logLines: List<String> = emptyList(),
    val error: String? = null
)

class DebugViewModel(
    private val logDir: File?,
    private val prefs: AppPrefsSource,
    val appVersion: String,
    val androidVersion: String,
    private val createShareUri: ((File) -> Uri?)? = null
) : ViewModel() {

    private val _state = MutableStateFlow(DebugUiState())
    val state: StateFlow<DebugUiState> = _state.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = currentLogFile()
                if (file == null || !file.exists()) {
                    _state.value = DebugUiState(isLoading = false, logLines = emptyList())
                    return@launch
                }
                val lines = file.readLines().takeLast(50)
                _state.value = DebugUiState(isLoading = false, logLines = lines)
            } catch (e: Exception) {
                Timber.e(e, "loadLogs failed")
                _state.value = DebugUiState(isLoading = false, error = "Failed to load logs: ${e.message}")
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                currentLogFile()?.delete()
                _state.value = DebugUiState(isLoading = false, logLines = emptyList())
            } catch (e: Exception) {
                Timber.e(e, "clearLogs failed")
            }
        }
    }

    suspend fun buildShareContent(): String {
        val isDemoMode = prefs.isDemoMode.first()
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val header = buildString {
            appendLine("App Version: $appVersion")
            appendLine("Android Version: $androidVersion")
            appendLine("Demo Mode: $isDemoMode")
            appendLine("Generated: $timestamp")
            appendLine("---")
        }
        val logContent = currentLogFile()?.takeIf { it.exists() }?.readText() ?: "(no log file)"
        return header + logContent
    }

    fun buildShareUri(): Uri? {
        val file = currentLogFile()?.takeIf { it.exists() } ?: return null
        return createShareUri?.invoke(file)
    }

    private fun currentLogFile(): File? {
        val dir = logDir ?: return null
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return File(dir, "ez1-debug-$today.log")
    }
}
