package day.azimuth.observer.ui.screens.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import day.azimuth.observer.data.remote.HexFreshnessData
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// ─── Boundary parsing ───────────────────────────────────────────────

/** Parse server boundary JSON "[[lat,lng],[lat,lng],...]" → GeoPoint list */
private fun parseBoundaryJson(json: String): List<GeoPoint> {
    // Format from h3-js cellToBoundary: [[lat, lng], [lat, lng], ...]
    // Stored as toString() of List<List<Double>>
    val cleaned = json.replace("[", "").replace("]", "")
    val numbers = cleaned.split(",").map { it.trim().toDouble() }
    return (numbers.indices step 2).map { i ->
        GeoPoint(numbers[i], numbers[i + 1])
    }
}

// ─── Freshness colors ───────────────────────────────────────────────

private fun freshnessColors(tier: String): Pair<Int, Int> = when (tier) {
    "stale_30d"    -> AndroidColor.argb(153, 245, 158, 11)  to AndroidColor.argb(204, 245, 158, 11)
    "aging_7d"     -> AndroidColor.argb(115, 251, 191, 36)  to AndroidColor.argb(166, 251, 191, 36)
    "baseline_24h" -> AndroidColor.argb(89, 6, 182, 212)    to AndroidColor.argb(140, 6, 182, 212)
    "recent"       -> AndroidColor.argb(46, 6, 182, 212)    to AndroidColor.argb(97, 6, 182, 212)
    "saturated"    -> AndroidColor.argb(31, 100, 116, 139)  to AndroidColor.argb(82, 100, 116, 139)
    else           -> AndroidColor.argb(31, 100, 116, 139)  to AndroidColor.argb(82, 100, 116, 139)
}

// ─── Custom location icons ──────────────────────────────────────────

private fun createLocationDot(density: Float): Bitmap {
    val sizePx = (24 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Outer glow ring
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x4006B6D4.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx / 2f, glowPaint)

    // Inner solid dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF06B6D4.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx / 3f, dotPaint)

    // White border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(cx, cy, sizePx / 3f, borderPaint)

    return bitmap
}

private fun createDirectionArrow(density: Float): Bitmap {
    val sizePx = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Outer glow ring (larger for arrow variant)
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x3006B6D4.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.42f, glowPaint)

    // Inner dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF06B6D4.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx / 4f, dotPaint)

    // White border on dot
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    canvas.drawCircle(cx, cy, sizePx / 4f, borderPaint)

    // Direction arrow (triangle pointing UP from center)
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF06B6D4.toInt()
        style = Paint.Style.FILL
    }
    val arrowPath = Path().apply {
        val arrowTip = cy - sizePx * 0.45f
        val arrowBase = cy - sizePx * 0.15f
        val arrowHalfWidth = sizePx * 0.12f
        moveTo(cx, arrowTip)
        lineTo(cx - arrowHalfWidth, arrowBase)
        lineTo(cx + arrowHalfWidth, arrowBase)
        close()
    }
    canvas.drawPath(arrowPath, arrowPaint)

    // Arrow white outline
    val arrowBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    canvas.drawPath(arrowPath, arrowBorder)

    return bitmap
}

// ─── Map screen ─────────────────────────────────────────────────────

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val locationOverlayRef = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    if (uiState.coveredHexes.isEmpty()) {
        EmptyMapState()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Interactive map (full bleed)
            OsmdroidMapView(
                hexes = uiState.displayHexes.ifEmpty { uiState.coveredHexes },
                freshnessHexes = uiState.freshnessHexes,
                onHexTapped = { hex -> viewModel.setTappedHex(hex) },
                onFreshnessHexTapped = { hex -> viewModel.setTappedFreshnessHex(hex) },
                onZoomChanged = { zoom -> viewModel.onZoomChanged(zoom) },
                onBoundsChanged = { s, w, n, e -> viewModel.fetchFreshness(s, w, n, e) },
                mapViewRef = mapViewRef,
                locationOverlayRef = locationOverlayRef,
                modifier = Modifier.fillMaxSize()
            )

            // Backfill progress banner (if running/failed)
            if (uiState.backfillState == BackfillState.RUNNING ||
                uiState.backfillState == BackfillState.FAILED
            ) {
                BackfillBanner(
                    state = uiState.backfillState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }

            // Map controls (right side): my-location + zoom
            MapControls(
                onMyLocation = {
                    locationOverlayRef.value?.let { overlay ->
                        overlay.enableFollowLocation()
                        val loc = overlay.myLocation
                        if (loc != null) {
                            mapViewRef.value?.controller?.animateTo(loc, 16.0, null)
                        }
                    }
                },
                onZoomIn = { mapViewRef.value?.controller?.zoomIn() },
                onZoomOut = { mapViewRef.value?.controller?.zoomOut() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )

            // Tapped hex info card
            val tapped = uiState.tappedHex
            if (tapped != null) {
                TappedHexCard(
                    hex = tapped,
                    onDismiss = { viewModel.setTappedHex(null) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 8.dp, end = 8.dp)
                        .fillMaxWidth()
                )
            }

            // Tapped freshness hex card
            val tappedFreshness = uiState.tappedFreshnessHex
            if (tappedFreshness != null) {
                TappedFreshnessHexCard(
                    hex = tappedFreshness,
                    onDismiss = { viewModel.setTappedFreshnessHex(null) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 8.dp, end = 8.dp)
                        .fillMaxWidth()
                )
            }

            // Freshness legend (bottom-start)
            FreshnessLegend(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 80.dp)
            )

            // Stats overlay card (bottom)
            AnimatedVisibility(
                visible = uiState.statsVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                CoverageStatsCard(
                    uiState = uiState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .fillMaxWidth()
                )
            }

            // Stats toggle FAB (always visible)
            SmallFloatingActionButton(
                onClick = { viewModel.toggleStats() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    if (uiState.statsVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (uiState.statsVisible) "Hide stats" else "Show stats"
                )
            }
        }
    }
}

// ─── Map controls ───────────────────────────────────────────────────

@Composable
private fun MapControls(
    onMyLocation: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // My location button
        Surface(
            shape = CircleShape,
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
        ) {
            IconButton(onClick = onMyLocation, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Zoom buttons
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
        ) {
            Column {
                IconButton(onClick = onZoomIn, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom in",
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .align(Alignment.CenterHorizontally)
                )
                IconButton(onClick = onZoomOut, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ─── OSMdroid map view ──────────────────────────────────────────────

@Composable
private fun OsmdroidMapView(
    hexes: List<HexCoverage>,
    freshnessHexes: List<HexFreshnessData> = emptyList(),
    onHexTapped: (HexCoverage) -> Unit = {},
    onFreshnessHexTapped: (HexFreshnessData) -> Unit = {},
    onZoomChanged: (Double) -> Unit = {},
    onBoundsChanged: (Double, Double, Double, Double) -> Unit = { _, _, _, _ -> },
    mapViewRef: androidx.compose.runtime.MutableState<MapView?>,
    locationOverlayRef: androidx.compose.runtime.MutableState<MyLocationNewOverlay?>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasInitializedCamera = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hexByPolygon = remember { mutableMapOf<Polygon, HexCoverage>() }

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            val density = ctx.resources.displayMetrics.density

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

                // Location overlay with custom cyan icons
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                    setDirectionArrow(
                        createLocationDot(density),
                        createDirectionArrow(density),
                    )
                    enableMyLocation()
                    enableFollowLocation()
                }
                overlays.add(locationOverlay)
                locationOverlayRef.value = locationOverlay

                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        val bb = boundingBox
                        onBoundsChanged(bb.latSouth, bb.lonWest, bb.latNorth, bb.lonEast)
                        return false
                    }
                    override fun onZoom(event: ZoomEvent?): Boolean {
                        event?.let { onZoomChanged(it.zoomLevel) }
                        val bb = boundingBox
                        onBoundsChanged(bb.latSouth, bb.lonWest, bb.latNorth, bb.lonEast)
                        return false
                    }
                })

                mapViewRef.value = this
            }
        },
        update = { mapView ->
            // Remove only hex polygons, keep location overlay
            mapView.overlays.removeAll { it is Polygon }
            hexByPolygon.clear()

            val h3Core = try {
                com.uber.h3core.H3Core.newInstance()
            } catch (_: Throwable) {
                null
            }

            val allPoints = mutableListOf<GeoPoint>()
            val otherPolygons = mutableListOf<Polygon>()
            val ownPolygons = mutableListOf<Polygon>()
            var renderedCount = 0
            val maxRendered = 500

            for (hex in hexes) {
                if (renderedCount >= maxRendered) break
                val tier = hex.getTier()
                if (tier == CoverageTier.UNMAPPED) continue

                val vertices: List<GeoPoint> = when {
                    // Prefer server-computed boundary (real H3 hexagons that interlock)
                    hex.boundary != null -> {
                        try {
                            parseBoundaryJson(hex.boundary)
                        } catch (_: Exception) {
                            continue
                        }
                    }
                    // H3Core fallback (if native lib works on this device)
                    !hex.h3Index.startsWith("grid") && h3Core != null -> {
                        try {
                            h3Core.cellToBoundary(hex.h3Index).map { latLng ->
                                GeoPoint(latLng.lat, latLng.lng)
                            }
                        } catch (_: Exception) {
                            continue
                        }
                    }
                    // Grid fallback — skip rendering (CoverageSyncWorker will normalize to real H3)
                    hex.h3Index.startsWith("grid8:") -> continue
                    else -> continue
                }

                allPoints.addAll(vertices)

                val obsCount = hex.observationCount
                val fillColor = when {
                    // OWN hexes get a 25% opacity floor so user always sees own coverage
                    tier == CoverageTier.OWN && obsCount < 100 ->
                        AndroidColor.argb(64, 245, 158, 11)
                    obsCount >= 5000 -> AndroidColor.argb(179, 6, 182, 212)
                    obsCount >= 1000 -> AndroidColor.argb(128, 6, 182, 212)
                    obsCount >= 100  -> AndroidColor.argb(102, 245, 158, 11)
                    else             -> AndroidColor.argb(38, 245, 158, 11)
                }

                val polygon = Polygon(mapView).apply {
                    points = vertices.toMutableList()
                    fillPaint.color = fillColor
                    outlinePaint.color = AndroidColor.argb(153, 245, 158, 11)
                    outlinePaint.strokeWidth = 1f
                    setOnClickListener { _, _, _ ->
                        onHexTapped(hex)
                        true
                    }
                }
                hexByPolygon[polygon] = hex
                renderedCount++

                if (tier == CoverageTier.OWN) {
                    ownPolygons.add(polygon)
                } else {
                    otherPolygons.add(polygon)
                }
            }

            // Z-order: OTHER below, OWN on top
            otherPolygons.forEach { mapView.overlays.add(it) }
            ownPolygons.forEach { mapView.overlays.add(it) }

            // Freshness hex overlay — renders on top when data available
            if (freshnessHexes.isNotEmpty()) {
                val fh3 = try { com.uber.h3core.H3Core.newInstance() } catch (_: Throwable) { null }
                for (fHex in freshnessHexes) {
                    if (fHex.freshnessTier == "virgin") continue
                    val fVerts: List<GeoPoint> = if (fHex.boundary.isNotEmpty()) {
                        fHex.boundary.map { coords ->
                            GeoPoint(coords.getOrElse(0) { 0.0 }, coords.getOrElse(1) { 0.0 })
                        }
                    } else if (fh3 != null) {
                        try {
                            fh3.cellToBoundary(fHex.h3Index).map { GeoPoint(it.lat, it.lng) }
                        } catch (_: Exception) { continue }
                    } else continue

                    val (fill, stroke) = freshnessColors(fHex.freshnessTier)
                    val fPoly = Polygon(mapView).apply {
                        points = fVerts.toMutableList()
                        fillPaint.color = fill
                        outlinePaint.color = stroke
                        outlinePaint.strokeWidth = 1f
                        setOnClickListener { _, _, _ ->
                            onFreshnessHexTapped(fHex)
                            true
                        }
                    }
                    mapView.overlays.add(fPoly)
                }
            }

            // Center on hex coverage only on first load
            if (allPoints.isNotEmpty() && !hasInitializedCamera.value) {
                hasInitializedCamera.value = true
                val centerLat = allPoints.map { it.latitude }.average()
                val centerLon = allPoints.map { it.longitude }.average()
                mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
                mapView.controller.setZoom(14.0)
                locationOverlayRef.value?.disableFollowLocation()
                mapView.postDelayed({ locationOverlayRef.value?.enableFollowLocation() }, 2000)
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
            locationOverlayRef.value?.disableMyLocation()
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
        }
    }
}

// ─── Supporting composables ─────────────────────────────────────────

@Composable
private fun CoverageStatsCard(
    uiState: MapUiState,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatBadge(
                    label = "Points",
                    value = uiState.pointsBalance.toString(),
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
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

            Spacer(modifier = Modifier.height(4.dp))

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

            Text(
                text = "Pending: ${uiState.pendingApprox}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
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
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackfillBanner(
    state: BackfillState,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                BackfillState.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Backfilling coverage data...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                BackfillState.FAILED -> {
                    Text(
                        text = "Coverage backfill incomplete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun TappedHexCard(
    hex: HexCoverage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onDismiss() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Coverage area",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${hex.cellCount + hex.gnssCount + hex.wifiCount} observations " +
                    "(${hex.cellCount} cell, ${hex.gnssCount} GNSS, ${hex.wifiCount} Wi-Fi)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Walk around with the app to light up new hexes!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FreshnessLegend(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Reward Multiplier",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LegendRow(color = Color(0xFFF59E0B), label = "High (>7d)")
            LegendRow(color = Color(0xFF06B6D4), label = "Standard")
            LegendRow(color = Color(0xFF64748B), label = "Low (<24h)")
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TappedFreshnessHexCard(
    hex: HexFreshnessData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.clickable { onDismiss() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Hex ${hex.h3Index.takeLast(8)}",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Last mapped: ${hex.lastObservation ?: "Never"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Reward: ${hex.freshnessMultiplier}x  |  ${hex.observationCount} observations",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
