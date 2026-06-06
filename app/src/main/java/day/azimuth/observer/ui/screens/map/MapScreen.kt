package day.azimuth.observer.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import day.azimuth.observer.data.local.HexCoverage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Azimuth Coverage Map",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Coarse hex units where you have collected data. Local only. No raw locations shown.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Hexes Covered",
                value = uiState.totalHexes.toString(),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Today",
                value = uiState.hexesToday.toString(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Cell",
                value = uiState.cellTotal.toString(),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "GNSS",
                value = uiState.gnssTotal.toString(),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Wi-Fi",
                value = uiState.wifiTotal.toString(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Pending (approx from local inserts)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${uiState.pendingApprox} (see Dashboard for authoritative pending count)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Covered units (most recent first)", style = MaterialTheme.typography.titleMedium)

        if (uiState.coveredHexes.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No coverage yet. Start collection to populate coarse hex units from your observations. Data is local-first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.height(400.dp)) {
                items(uiState.coveredHexes) { hex ->
                    HexRow(hex)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Legend: Units are coarse (~res 8 or grid approx). Pending may be approximate after uploads. Dashboard pending is authoritative.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HexRow(hex: HexCoverage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val short = if (hex.h3Index.startsWith("grid")) hex.h3Index else hex.h3Index.take(10) + "..."
            Text("Unit: $short  (res ${hex.resolution})", style = MaterialTheme.typography.bodyMedium)
            Text("Last seen: ${formatTime(hex.lastSeen)}", style = MaterialTheme.typography.bodySmall)
            Text("Obs: ${hex.observationCount}  Cell:${hex.cellCount} GNSS:${hex.gnssCount} WiFi:${hex.wifiCount}", style = MaterialTheme.typography.bodySmall)
            if (hex.pendingCount > 0) {
                Text("Pending in unit: ${hex.pendingCount} (approx)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

private fun formatTime(ts: Long): String {
    return try {
        SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(Date(ts))
    } catch (e: Exception) {
        "recent"
    }
}
