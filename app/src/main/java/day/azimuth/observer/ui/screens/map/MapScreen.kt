package day.azimuth.observer.ui.screens.map

import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import day.azimuth.observer.data.local.HexCoverage
import day.azimuth.observer.data.local.CoverageTier
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.coveredHexes.isEmpty()) {
        EmptyMapState()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Interactive map (full bleed)
            OsmdroidMapView(
                hexes = uiState.coveredHexes,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Privacy banner (top, semi-transparent)
            PrivacyBanner(modifier = Modifier.align(Alignment.TopCenter))

            // Layer 3: Stats overlay card (bottom)
            CoverageStatsCard(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OsmdroidMapView(
    hexes: List<HexCoverage>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName

            MapView(ctx).apply {
                setTileSource(
                    XYTileSource(
                        "CartoDB-DarkMatter",
                        0,
                        19,
                        256,
                        ".png",
                        arrayOf(
                            "https://a.basemaps.cartocdn.com/dark_all/",
                            "https://b.basemaps.cartocdn.com/dark_all/",
                            "https://c.basemaps.cartocdn.com/dark_all/"
                        )
                    )
                )
                setMultiTouchControls(true)
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                minZoomLevel = 4.0
                maxZoomLevel = 18.0
                mapViewRef.value = this
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            val h3Core = try {
                com.uber.h3core.H3Core.newInstance()
            } catch (_: Throwable) {
                null
            }

            val allPoints = mutableListOf<GeoPoint>()

            for (hex in hexes) {
                val tier = hex.getTier()
                if (tier == CoverageTier.UNMAPPED) continue

                val vertices: List<GeoPoint> = when {
                    !hex.h3Index.startsWith("grid") && h3Core != null -> {
                        try {
                            h3Core.cellToBoundary(hex.h3Index).map { latLng ->
                                GeoPoint(latLng.lat, latLng.lng)
                            }
                        } catch (_: Exception) {
                            continue
                        }
                    }
                    hex.h3Index.startsWith("grid8:") -> {
                        try {
                            val parts = hex.h3Index.split(":")
                            val latBucket = parts[1].toInt()
                            val lonBucket = parts[2].toInt()
                            val scale = 0.00417
                            val centerLat = latBucket * scale + scale / 2.0
                            val centerLon = lonBucket * scale + scale / 2.0
                            val radius = scale / 2.0

                            (0 until 6).map { i ->
                                val angle = Math.toRadians(60.0 * i)
                                GeoPoint(
                                    centerLat + radius * Math.cos(angle),
                                    centerLon + radius * Math.sin(angle)
                                )
                            }
                        } catch (_: Exception) {
                            continue
                        }
                    }
                    else -> continue
                }

                allPoints.addAll(vertices)

                val fillColor = when (tier) {
                    CoverageTier.OWN -> AndroidColor.argb(153, 245, 158, 11)
                    CoverageTier.OTHER -> AndroidColor.argb(102, 6, 182, 212)
                    CoverageTier.UNMAPPED -> continue
                }

                val polygon = Polygon(mapView).apply {
                    points = vertices.toMutableList()
                    fillPaint.color = fillColor
                    outlinePaint.color = AndroidColor.argb(200, 255, 255, 255)
                    outlinePaint.strokeWidth = 2f
                }
                mapView.overlays.add(polygon)
            }

            if (allPoints.isNotEmpty()) {
                val centerLat = allPoints.map { it.latitude }.average()
                val centerLon = allPoints.map { it.longitude }.average()
                mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
                mapView.controller.setZoom(14.0)
            }

            mapView.invalidate()
        },
        modifier = modifier
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
        }
    }
}

@Composable
private fun PrivacyBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Approximate coverage areas from your local observations. Exact locations and raw identifiers are not shown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CoverageStatsCard(
    uiState: MapUiState,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatBadge(
                    label = "Coverage areas",
                    value = uiState.totalHexes.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Today",
                    value = uiState.hexesToday.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatBadge(
                    label = "Cell",
                    value = uiState.cellTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "GNSS",
                    value = uiState.gnssTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Wi-Fi",
                    value = uiState.wifiTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pending: ${uiState.pendingApprox}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyMapState() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "No coverage found",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No coverage yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start observing to light up your first hex on the map. All data stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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
