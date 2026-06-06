package day.azimuth.observer.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import day.azimuth.observer.service.CollectionController

@Composable
fun PermissionOnboardingScreen(
    onPermissionsGranted: () -> Unit,
    onLogout: () -> Unit,
    viewModel: PermissionOnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionsToRequest = mutableListOf<String>().apply {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.READ_PHONE_STATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        viewModel.onPermissionResult(results)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PermissionOnboardingEvent.AllGranted -> onPermissionsGranted()
                is PermissionOnboardingEvent.Skipped -> onPermissionsGranted()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Enable Permissions",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Azimuth Observer needs the following permissions to collect radio environment data:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionRationaleItem(
            title = "Location",
            description = "Required for GPS-tagged observations and to access Cell / WiFi APIs on Android.",
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionRationaleItem(
            title = "Phone State",
            description = "Required to read cellular tower information (LTE / NR cell info).",
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRationaleItem(
                title = "Nearby Wi-Fi Devices",
                description = "Required for WiFi survey and RTT ranging on Android 13+.",
            )
            Spacer(modifier = Modifier.height(12.dp))
            PermissionRationaleItem(
                title = "Notifications",
                description = "Required to show foreground service status while collecting.",
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        PermissionRationaleItem(
            title = "Foreground Service",
            description = "Keeps collection active while the app is in the background.",
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { launcher.launch(permissionsToRequest) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Grant Permissions")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.permanentlyDenied.isNotEmpty()) {
            Text(
                text = "Some permissions were permanently denied. Open Settings to enable them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::openAppSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = viewModel::skip,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Not now")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Log out")
        }
    }
}

@Composable
private fun PermissionRationaleItem(
    title: String,
    description: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
