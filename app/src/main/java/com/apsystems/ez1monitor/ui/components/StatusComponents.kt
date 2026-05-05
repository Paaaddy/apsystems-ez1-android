package com.apsystems.ez1monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Connection indicator using both icon and color — accessible to color-blind users.
 */
@Composable
fun ConnectionIndicator(isConnected: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.semantics {
            contentDescription = if (isConnected) "Connected" else "Disconnected"
        }
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (isConnected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.bodySmall,
            color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Inverter on/off status badge shown near the power display.
 * Distinguishes "0W because inverter is off" from "0W because it's nighttime."
 */
@Composable
fun InverterStatusBadge(isOn: Boolean, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics {
            contentDescription = if (isOn) "Inverter is on" else "Inverter is off"
        }
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isOn) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isOn) "Inverter on" else "Inverter off",
            style = MaterialTheme.typography.bodySmall,
            color = if (isOn) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
