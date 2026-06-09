package day.azimuth.observer.ui.screens.nodes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import day.azimuth.observer.data.remote.NodeInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeManagementScreen(
    viewModel: NodeManagementViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedHardwareType by remember { mutableStateOf("tier0_mobile") }
    var customLabel by remember { mutableStateOf("") }
    var editingNodeId by remember { mutableStateOf<String?>(null) }
    var editingLabel by remember { mutableStateOf("") }

    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAddDialog,
            title = { Text("Add Node") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Hardware Type",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val hardwareOptions = listOf(
                        "tier0_mobile" to "Mobile",
                        "tier1_rtlsdr" to "RTL-SDR",
                        "tier2_gpsdisc" to "GPS Disc",
                        "tier3_kraken" to "Kraken",
                    )

                    Column {
                        hardwareOptions.forEach { (value, label) ->
                            Button(
                                onClick = { selectedHardwareType = value },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (selectedHardwareType == value) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                            ) {
                                Text(label)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Label (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNode(selectedHardwareType, customLabel)
                        selectedHardwareType = "tier0_mobile"
                        customLabel = ""
                    },
                    enabled = !uiState.addingNode,
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = viewModel::dismissAddDialog,
                    enabled = !uiState.addingNode,
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (editingNodeId != null) {
        AlertDialog(
            onDismissRequest = { editingNodeId = null },
            title = { Text("Edit Label") },
            text = {
                OutlinedTextField(
                    value = editingLabel,
                    onValueChange = { editingLabel = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingNodeId?.let {
                            viewModel.updateLabel(it, editingLabel)
                        }
                        editingNodeId = null
                        editingLabel = ""
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { editingNodeId = null },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Nodes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Default.Add, contentDescription = "Add node")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.nodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No nodes registered",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.nodes) { node ->
                        NodeCard(
                            node = node,
                            onEditLabel = {
                                editingNodeId = node.id
                                editingLabel = node.label ?: ""
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeCard(
    node: NodeInfo,
    onEditLabel: () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title: Animal name
            Text(
                text = node.animalName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle: Custom label or "No label"
            Text(
                text = node.label ?: "No label",
                style = MaterialTheme.typography.bodySmall,
                color = if (node.label != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tier badge + Status dot + Last seen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    // Tier badge
                    val (tierLabel, tierColor) = getTierInfo(node.hardwareType)
                    Card(
                        modifier = Modifier
                            .background(tierColor)
                            .padding(4.dp),
                    ) {
                        Text(
                            text = tierLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 2.dp,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status dot
                    val statusColor = getStatusColor(node.status)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, shape = MaterialTheme.shapes.small),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Last seen
                    Text(
                        text = getLastSeenText(node.lastSeenAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Edit button
                OutlinedButton(
                    onClick = onEditLabel,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("Edit")
                }
            }
        }
    }
}

private fun getTierInfo(hardwareType: String): Pair<String, Color> {
    return when (hardwareType) {
        "tier0_mobile" -> "Mobile" to Color(0xFFE5E7EB)
        "tier1_rtlsdr" -> "RTL-SDR" to Color(0xFF06B6D4)
        "tier2_gpsdisc" -> "GPS Disc" to Color(0xFFF59E0B)
        "tier3_kraken" -> "Kraken" to Color(0xFF8B5CF6)
        else -> "Unknown" to Color(0xFFE5E7EB)
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        "active" -> Color(0xFF22C55E)
        "registered" -> Color(0xFF3B82F6)
        else -> Color(0xFF9CA3AF)
    }
}

private fun getLastSeenText(lastSeenAt: String?): String {
    return if (lastSeenAt == null) {
        "Never"
    } else {
        // Simple relative time calculation (would be enhanced in production)
        "Active"
    }
}
