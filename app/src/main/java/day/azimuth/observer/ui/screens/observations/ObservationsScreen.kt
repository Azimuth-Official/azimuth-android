package day.azimuth.observer.ui.screens.observations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ObservationsScreen(viewModel: ObservationsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Observations",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${uiState.totalCount} total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.observations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No observations yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.observations, key = { it.id }) { observation ->
                    ObservationCard(observation)
                }
            }
        }
    }
}

@Composable
private fun ObservationCard(observation: day.azimuth.observer.data.local.Observation) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Signal type icon
                Icon(
                    imageVector = when {
                        observation.signalType.startsWith("cell") -> Icons.Default.SignalCellularAlt
                        observation.signalType.startsWith("gnss") -> Icons.Default.Satellite
                        observation.signalType.startsWith("wifi") -> Icons.Default.Wifi
                        else -> Icons.Default.SignalCellularAlt
                    },
                    contentDescription = formatSignalType(observation.signalType),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatSignalType(observation.signalType),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = formatRelativeTime(observation.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Upload status badge
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (observation.uploaded) "Uploaded" else "Pending",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.height(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Accuracy indicator
            Text(
                text = "Accuracy: ${getAccuracyClass(observation.accuracy)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun getAccuracyClass(accuracy: Float): String {
    return when {
        accuracy < 10f -> "High (<10m)"
        accuracy < 50f -> "Medium (10-50m)"
        else -> "Low (>50m)"
    }
}

private fun formatSignalType(raw: String): String = when (raw) {
    "cell_lte" -> "Cell LTE"
    "cell_nr" -> "Cell NR"
    "gnss_raw" -> "GNSS"
    "wifi_survey" -> "Wi-Fi Survey"
    "wifi_rtt" -> "Wi-Fi RTT"
    else -> raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = abs(now - timestampMs)

    return when {
        diffMs < 60_000L -> "Just now"
        diffMs < 3_600_000L -> {
            val minutes = diffMs / 60_000L
            "$minutes minute${if (minutes != 1L) "s" else ""} ago"
        }
        diffMs < 86_400_000L -> {
            val hours = diffMs / 3_600_000L
            "$hours hour${if (hours != 1L) "s" else ""} ago"
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US)
            dateFormat.format(Date(timestampMs))
        }
    }
}
