package com.apsystems.ez1monitor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apsystems.ez1monitor.data.api.models.OutputData

@Composable
fun EnergyCard(outputData: OutputData?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Energy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                EnergyItem(
                    label = "Today",
                    kwh = outputData?.eTotal ?: 0f,
                    modifier = Modifier.weight(1f)
                )
                EnergyItem(
                    label = "Lifetime",
                    kwh = outputData?.teTotal ?: 0f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EnergyItem(label: String, kwh: Float, modifier: Modifier = Modifier) {
    val kwhText = "%.2f kWh".format(kwh)
    Column(
        modifier = modifier.semantics {
            contentDescription = "$label energy: $kwhText"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = kwhText,
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
