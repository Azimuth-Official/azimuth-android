package day.azimuth.observer.ui.screens.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import day.azimuth.observer.data.remote.LeaderboardEntry

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Title with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Leaderboard",
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Period tabs
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val periods = listOf("daily", "weekly", "alltime")
            val periodLabels = mapOf(
                "daily" to "Daily",
                "weekly" to "Weekly",
                "alltime" to "All Time",
            )
            periods.forEachIndexed { index, period ->
                SegmentedButton(
                    selected = period == uiState.period,
                    onClick = { viewModel.setPeriod(period) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = periods.size,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(periodLabels[period] ?: period)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (uiState.entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No activity yet for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.entries) { entry ->
                    LeaderboardEntryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(entry: LeaderboardEntry) {
    val isTopThree = entry.rank <= 3
    val rankBackgroundColor = when (entry.rank) {
        1 -> MaterialTheme.colorScheme.primaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer
        3 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isTopThree) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            // Animal name and observation count
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = entry.animalName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${entry.observationCount} observations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Points
            Text(
                text = "${entry.points}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = "pts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
