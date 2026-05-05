package com.apsystems.ez1monitor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun OnOffControl(
    isOn: Boolean?,
    isCommandInFlight: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmOff by remember { mutableStateOf(false) }

    if (showConfirmOff) {
        AlertDialog(
            onDismissRequest = { showConfirmOff = false },
            title = { Text("Turn inverter off?") },
            text = { Text("The inverter will stop producing power.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmOff = false
                    onToggle()
                }) {
                    Text("Turn Off", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmOff = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Inverter Power",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (isOn) {
                        true -> "On"
                        false -> "Off"
                        null -> "Unknown"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isCommandInFlight) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                Switch(
                    checked = isOn ?: false,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            // Turning off requires confirmation
                            showConfirmOff = true
                        } else {
                            onToggle()
                        }
                    },
                    enabled = isOn != null && !isCommandInFlight,
                    modifier = Modifier.semantics {
                        contentDescription = "Inverter power switch, currently ${
                            when (isOn) {
                                true -> "on"
                                false -> "off"
                                null -> "unknown"
                            }
                        }"
                    }
                )
            }
        }
    }
}
