package com.apsystems.ez1monitor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun MaxPowerControl(
    currentMaxPower: Int,
    pendingMaxPower: Int,
    minPower: Int,
    maxPower: Int,
    isCommandInFlight: Boolean,
    onPendingChanged: (Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDirty = pendingMaxPower != currentMaxPower && pendingMaxPower > 0

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Max Power Limit",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${pendingMaxPower.coerceAtLeast(minPower)} W",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "/ $maxPower W max",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Slider(
                value = pendingMaxPower.coerceIn(minPower, maxPower).toFloat(),
                onValueChange = { onPendingChanged(it.toInt()) },
                valueRange = minPower.toFloat()..maxPower.toFloat(),
                steps = 0,
                enabled = !isCommandInFlight && maxPower > minPower,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Max power slider, current value ${pendingMaxPower} watts, range $minPower to $maxPower"
                    }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$minPower W",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$maxPower W",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDirty || isCommandInFlight) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onConfirm,
                        enabled = !isCommandInFlight && isDirty
                    ) {
                        Text("Set")
                    }
                    if (isCommandInFlight) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
