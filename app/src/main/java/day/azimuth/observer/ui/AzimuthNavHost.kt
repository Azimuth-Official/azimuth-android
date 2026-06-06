package day.azimuth.observer.ui

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import day.azimuth.observer.MainActivity
import day.azimuth.observer.ui.screens.dashboard.DashboardScreen
import day.azimuth.observer.ui.screens.map.MapScreen
import day.azimuth.observer.ui.screens.observations.ObservationsScreen
import day.azimuth.observer.ui.screens.onboarding.OnboardingScreen
import day.azimuth.observer.ui.screens.onboarding.PermissionOnboardingScreen
import day.azimuth.observer.ui.screens.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object PermissionOnboardingRoute
@Serializable object DashboardRoute
@Serializable object ObservationsRoute
@Serializable object SettingsRoute
@Serializable object MapRoute

data class BottomNavItem(
    val label: String,
    val icon: @Composable () -> Unit,
    val route: Any,
)

@Composable
fun AzimuthNavHost(viewModel: AzimuthNavViewModel = hiltViewModel()) {
    val isRegistered by viewModel.isRegistered.collectAsState(initial = false)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    val items = listOf(
        BottomNavItem("Dashboard", { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") }, DashboardRoute),
        BottomNavItem("Observations", { Icon(Icons.Default.List, contentDescription = "Observations") }, ObservationsRoute),
        BottomNavItem("Map", { Icon(Icons.Default.Place, contentDescription = "Map") }, MapRoute),
        BottomNavItem("Settings", { Icon(Icons.Default.Settings, contentDescription = "Settings") }, SettingsRoute),
    )

    val showBottomBar = isRegistered && currentDestination?.hierarchy?.any {
        it.hasRoute(OnboardingRoute::class) || it.hasRoute(PermissionOnboardingRoute::class)
    } != true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(item.route::class)
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(DashboardRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isRegistered) DashboardRoute else OnboardingRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onRegistrationComplete = {
                        navController.navigate(PermissionOnboardingRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    },
                )
            }
            composable<PermissionOnboardingRoute> {
                PermissionOnboardingScreen(
                    onPermissionsGranted = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(PermissionOnboardingRoute) { inclusive = true }
                        }
                    },
                    onLogout = {
                        navController.navigate(OnboardingRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable<DashboardRoute> { DashboardScreen() }
            composable<ObservationsRoute> { ObservationsScreen() }
            composable<MapRoute> { MapScreen() }
            composable<SettingsRoute> {
                SettingsScreen(
                    onLogout = {
                        // Restart activity to clear all Compose state and re-evaluate startDestination
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    },
                )
            }
        }
    }
}
