package day.azimuth.observer.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import day.azimuth.observer.data.local.NtripConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtkSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RtkSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val status by viewModel.status.collectAsState()

    var providerName by remember { mutableStateOf(uiState.providerName) }
    var casterUrl by remember { mutableStateOf(uiState.casterUrl) }
    var casterPort by remember { mutableStateOf(uiState.casterPort.toString()) }
    var mountpoint by remember { mutableStateOf(uiState.mountpoint) }
    var username by remember { mutableStateOf(uiState.username) }
    var password by remember { mutableStateOf(uiState.password) }

    var ephEnabled by remember { mutableStateOf(uiState.ephEnabled) }
    var ephCasterUrl by remember { mutableStateOf(uiState.ephCasterUrl) }
    var ephCasterPort by remember { mutableStateOf(uiState.ephCasterPort.toString()) }
    var ephMountpoint by remember { mutableStateOf(uiState.ephMountpoint) }
    var ephUsername by remember { mutableStateOf(uiState.ephUsername) }
    var ephPassword by remember { mutableStateOf(uiState.ephPassword) }
    var expandEphemeris by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("RTK Provider (Experimental)") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Experimental warning banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠ EXPERIMENTAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RTK integration is in early testing. Expect bugs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Configuration section
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = providerName,
                onValueChange = { providerName = it },
                label = { Text("Provider Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = casterUrl,
                onValueChange = { casterUrl = it },
                label = { Text("Caster URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = casterPort,
                onValueChange = { casterPort = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mountpoint,
                onValueChange = { mountpoint = it },
                label = { Text("Mountpoint") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced: Ephemeris Mount section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandEphemeris = !expandEphemeris }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Advanced: Dedicated Ephemeris Mount",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (expandEphemeris) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle ephemeris mount",
                        )
                    }

                    if (expandEphemeris) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = ephEnabled,
                                    onCheckedChange = {
                                        ephEnabled = it
                                        viewModel.toggleEphemerisMount(it)
                                    },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Use dedicated ephemeris mount",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            if (ephEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = ephCasterUrl,
                                    onValueChange = { ephCasterUrl = it },
                                    label = { Text("Caster URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = ephCasterPort,
                                    onValueChange = { ephCasterPort = it },
                                    label = { Text("Port") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = ephMountpoint,
                                    onValueChange = { ephMountpoint = it },
                                    label = { Text("Mountpoint") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = ephUsername,
                                    onValueChange = { ephUsername = it },
                                    label = { Text("Username") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = ephPassword,
                                    onValueChange = { ephPassword = it },
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection status
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val statusColor = when (status.state) {
                NtripConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outlineVariant
                NtripConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
                NtripConnectionState.CONNECTED, NtripConnectionState.STREAMING -> MaterialTheme.colorScheme.tertiary
                NtripConnectionState.ERROR -> MaterialTheme.colorScheme.error
            }

            val statusText = when (status.state) {
                NtripConnectionState.DISCONNECTED -> "Disconnected"
                NtripConnectionState.CONNECTING -> "Connecting..."
                NtripConnectionState.CONNECTED -> "Connected"
                NtripConnectionState.STREAMING -> "Streaming"
                NtripConnectionState.ERROR -> "Error: ${status.errorMessage}"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    modifier = Modifier.weight(1f)
                )
            }

            if (status.state == NtripConnectionState.STREAMING) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Messages: ${status.messagesDecoded} | Bytes: ${status.bytesReceived}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Button(
                onClick = {
                    val port = casterPort.toIntOrNull() ?: 2101
                    viewModel.testConnection(
                        providerName = providerName,
                        casterUrl = casterUrl,
                        casterPort = port,
                        mountpoint = mountpoint,
                        username = username,
                        password = password,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Test Connection")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isEnabled) {
                OutlinedButton(
                    onClick = viewModel::disconnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Disconnect")
                }
            } else {
                Button(
                    onClick = {
                        val port = casterPort.toIntOrNull() ?: 2101
                        val ephPort = ephCasterPort.toIntOrNull() ?: 2101
                        viewModel.saveAndEnable(
                            providerName = providerName,
                            casterUrl = casterUrl,
                            casterPort = port,
                            mountpoint = mountpoint,
                            username = username,
                            password = password,
                            ephEnabled = ephEnabled,
                            ephCasterUrl = ephCasterUrl,
                            ephCasterPort = ephPort,
                            ephMountpoint = ephMountpoint,
                            ephUsername = ephUsername,
                            ephPassword = ephPassword,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save & Enable")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info section
            Text(
                text = "About RTK",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect your RTK correction provider for improved GPS accuracy. When enabled, RTK corrections provide 1.5x points for observations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
