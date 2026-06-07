package day.azimuth.observer.ui.screens.map

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
            text = "Coverage Map",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Approximate coverage areas from your local observations. Exact locations and raw identifiers are not shown.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Coverage areas",
                value = uiState.totalHexes.toString(),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Today",
                value = uiState.hexesToday.toString(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stats row 2
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
                Text(
                    "Pending uploads",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${uiState.pendingApprox} approximate (Dashboard has the authoritative count)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Coverage sketch",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Schematic view — not a precise map.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.schematicRowCount > 0) {
            SchematicGrid(uiState.schematicCells)
        } else {
            Text(
                text = "New coverage areas will appear here as schematic cells.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recent coverage",
            style = MaterialTheme.typography.titleMedium,
        )

        if (uiState.coveredHexes.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No coverage found",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No coverage yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start collecting observations and your approximate coverage areas will appear here. Data stays on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.height(400.dp)) {
                itemsIndexed(uiState.coveredHexes) { index, hex ->
                    HexRow(index = index, hex = hex)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Coverage areas are approximate summaries of collected data. Exact locations and raw identifiers are not displayed.",
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
private fun HexRow(index: Int, hex: HexCoverage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Coverage area ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusLabel(hex = hex)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last seen: ${formatTime(hex.lastSeen)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Observations: ${hex.observationCount}  Cell: ${hex.cellCount}  GNSS: ${hex.gnssCount}  Wi-Fi: ${hex.wifiCount}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (hex.pendingCount > 0) {
                Text(
                    text = "Pending uploads: ${hex.pendingCount} (approx)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun StatusLabel(hex: HexCoverage) {
    val label = if (hex.pendingCount > 0) "Pending upload" else "Synced"
    val color = if (hex.pendingCount > 0) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@Composable
private fun SchematicGrid(cells: List<SchematicCell>) {
    val gridSize = 10
    // Create a lookup map: Pair(x, y) -> SchematicCell for fast access
    val cellMap = cells.associateBy { Pair(it.gridX, it.gridY) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (y in 0 until gridSize) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (x in 0 until gridSize) {
                    val cell = cellMap[Pair(x, y)]
                    val bgColor = when {
                        cell != null && cell.pending -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                        cell != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cell != null) {
                                // Simple filled indicator
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("•", style = MaterialTheme.typography.labelSmall, color = bgColor)
                                }
                            }
                        }
                    }
                }
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
