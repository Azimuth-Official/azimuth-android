package day.azimuth.observer.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import day.azimuth.observer.BuildConfig

@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    onNavigateToRtk: () -> Unit = {},
    onNavigateToNodes: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.LoggedOut -> onLogout()
            }
        }
    }

    if (uiState.showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelLogout,
            title = { Text("Log out") },
            text = { Text("This will clear your local session and return you to setup. Your observation history will remain on this device.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Log out")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::cancelLogout) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Account card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::requestLogout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Log out")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Name card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Display Name",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = uiState.displayNameInput,
                        onValueChange = viewModel::onDisplayNameChange,
                        placeholder = { Text("Set display name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = viewModel::saveDisplayName,
                        enabled = uiState.displayNameInput.isNotBlank() &&
                                uiState.displayNameInput != (uiState.displayName ?: "") &&
                                !uiState.displayNameSaving,
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manage Nodes card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToNodes() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Manage Nodes",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keep screen on",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Collection card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Collection",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Observation collection",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Automatically collects signal data in the background",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.collectionEnabled,
                        onCheckedChange = viewModel::setCollectionEnabled,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RTK Provider card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "RTK Provider (Experimental)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onNavigateToRtk,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Configure RTK")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions card
        PermissionsCard(context = context)

        Spacer(modifier = Modifier.height(32.dp))

        // App version info at bottom
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionsCard(context: android.content.Context) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionStates by remember {
        mutableStateOf(checkPermissions(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStates = checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val permissions = listOf(
                "Location" to Manifest.permission.ACCESS_FINE_LOCATION,
                "Background Location" to Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                "Phone State" to Manifest.permission.READ_PHONE_STATE,
                "Wi-Fi Devices" to Manifest.permission.NEARBY_WIFI_DEVICES,
                "Notifications" to Manifest.permission.POST_NOTIFICATIONS,
            )

            permissions.forEachIndexed { index, (name, permission) ->
                val isGranted = permissionStates[permission] ?: false
                val statusColor = if (isGranted) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                }
                val statusText = if (isGranted) "Granted" else "Denied"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                    )
                }

                if (index < permissions.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun checkPermissions(context: android.content.Context): Map<String, Boolean> {
    return mapOf(
        Manifest.permission.ACCESS_FINE_LOCATION to (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ),
        Manifest.permission.ACCESS_BACKGROUND_LOCATION to (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ),
        Manifest.permission.READ_PHONE_STATE to (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ),
        Manifest.permission.NEARBY_WIFI_DEVICES to (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ),
        Manifest.permission.POST_NOTIFICATIONS to (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ),
    )
}
