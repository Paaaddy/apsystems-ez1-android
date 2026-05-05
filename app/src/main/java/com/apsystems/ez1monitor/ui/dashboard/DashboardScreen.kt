package com.apsystems.ez1monitor.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apsystems.ez1monitor.ui.components.AlarmSection
import com.apsystems.ez1monitor.ui.components.ConnectionIndicator
import com.apsystems.ez1monitor.ui.components.EnergyCard
import com.apsystems.ez1monitor.ui.components.MaxPowerControl
import com.apsystems.ez1monitor.ui.components.OnOffControl
import com.apsystems.ez1monitor.ui.components.PowerCard
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar messages from commands
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EZ1 Monitor") },
                actions = {
                    ConnectionIndicator(isConnected = state.isConnected)
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (state.isLoading && state.outputData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading && state.outputData != null,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Error banner — shown but doesn't hide last known data
                if (state.error != null) {
                    ErrorBanner(message = state.error!!)
                }

                // Alarm section — shown only when faults present
                state.alarms?.let { alarms ->
                    if (alarms.hasAlarm) {
                        AlarmSection(alarms = alarms)
                    }
                }

                // Power display with stale data timestamp
                PowerCard(
                    outputData = state.outputData,
                    isOn = state.isOn,
                    lastUpdatedText = formatLastUpdated(state.lastUpdatedMs)
                )

                EnergyCard(outputData = state.outputData)

                // Controls section — only shown once device info is known
                state.deviceInfo?.let { deviceInfo ->
                    Text(
                        text = "Controls",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    OnOffControl(
                        isOn = state.isOn,
                        isCommandInFlight = state.isCommandInFlight,
                        onToggle = viewModel::toggleOnOff
                    )

                    MaxPowerControl(
                        currentMaxPower = state.currentMaxPower,
                        pendingMaxPower = if (state.pendingMaxPower > 0) state.pendingMaxPower
                            else state.currentMaxPower,
                        minPower = deviceInfo.minPower,
                        maxPower = deviceInfo.maxPower,
                        isCommandInFlight = state.isCommandInFlight,
                        onPendingChanged = viewModel::onPendingMaxPowerChanged,
                        onConfirm = viewModel::confirmSetMaxPower
                    )
                }

                // Device info footer
                state.deviceInfo?.let { info ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${info.deviceId} · v${info.devVer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatLastUpdated(timestampMs: Long): String {
    if (timestampMs == 0L) return "Loading…"
    val diffMs = System.currentTimeMillis() - timestampMs
    return when {
        diffMs < 60_000 -> "Updated just now"
        diffMs < 3_600_000 -> "Updated ${TimeUnit.MILLISECONDS.toMinutes(diffMs)} min ago"
        else -> "Updated ${TimeUnit.MILLISECONDS.toHours(diffMs)} h ago"
    }
}
