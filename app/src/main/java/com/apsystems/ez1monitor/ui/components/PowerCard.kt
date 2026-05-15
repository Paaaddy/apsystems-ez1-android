package com.apsystems.ez1monitor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apsystems.ez1monitor.data.api.models.OutputData

@Composable
fun PowerCard(
    outputData: OutputData?,
    isOn: Boolean?,
    lastUpdatedText: String,
    isStale: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current Output",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Main power number — large, primary focus
            val totalPower = outputData?.pTotal ?: 0f
            val powerText = "%.0f W".format(totalPower)

            Text(
                text = powerText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = "Total power output: $powerText"
                }
            )

            // Stale data indicator directly under main number
            Text(
                text = if (isStale) "$lastUpdatedText · stale" else lastUpdatedText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isStale) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Inverter status near the power display — clarifies "0W off" vs "0W night"
            if (isOn != null) {
                Spacer(Modifier.height(4.dp))
                InverterStatusBadge(isOn = isOn)
            }

            if (outputData != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    ChannelPower(
                        label = "Channel 1",
                        watts = outputData.p1,
                        modifier = Modifier.weight(1f)
                    )
                    ChannelPower(
                        label = "Channel 2",
                        watts = outputData.p2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelPower(label: String, watts: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%.1f W".format(watts),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
