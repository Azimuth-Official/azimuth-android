package day.azimuth.observer.ui.screens.map

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.uber.h3core.H3Core
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.local.CoverageTier
import day.azimuth.observer.data.local.HexCoverage
import day.azimuth.observer.data.local.HexCoverageDao
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.HexFreshnessData
import day.azimuth.observer.service.CollectionController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class BackfillState {
    IDLE, RUNNING, SUCCEEDED, FAILED
}

data class MapUiState(
    val totalHexes: Int = 0,
    val hexesToday: Int = 0,
    val coveredHexes: List<HexCoverage> = emptyList(),
    val displayHexes: List<HexCoverage> = emptyList(),
    val displayResolution: Int = 8,
    val cellTotal: Int = 0,
    val gnssTotal: Int = 0,
    val wifiTotal: Int = 0,
    val pendingApprox: Int = 0,
    val tappedHex: HexCoverage? = null,
    val freshnessHexes: List<HexFreshnessData> = emptyList(),
    val tappedFreshnessHex: HexFreshnessData? = null,
    val backfillState: BackfillState = BackfillState.IDLE,
    val statsVisible: Boolean = true,
    val pointsBalance: Int = 0,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val hexCoverageDao: HexCoverageDao,
    @ApplicationContext private val context: Context,
    private val prefs: AzimuthPreferences,
    private val api: AzimuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val h3Core: H3Core? = try { H3Core.newInstance() } catch (_: Throwable) { null }
    private var currentZoom: Double = 14.0
    private var zoomDebounceJob: Job? = null
    private var freshnessFetchJob: Job? = null
    private var lastFreshnessBounds: String = ""

    init {
        viewModelScope.launch {
            hexCoverageDao.getAll().collect { list ->
                val todayStart = getTodayStartMillis()
                val todayList = list.filter { it.lastSeen >= todayStart }
                val sorted = list.sortedWith(
                    compareByDescending<HexCoverage> { it.pendingCount > 0 }
                        .thenByDescending { it.lastSeen }
                )
                _uiState.value = _uiState.value.copy(
                    totalHexes = list.size,
                    hexesToday = todayList.size,
                    coveredHexes = sorted,
                    cellTotal = list.sumOf { it.cellCount },
                    gnssTotal = list.sumOf { it.gnssCount },
                    wifiTotal = list.sumOf { it.wifiCount },
                    pendingApprox = list.sumOf { it.pendingCount }
                )
                recomputeDisplayHexes()
            }
        }

        viewModelScope.launch {
            prefs.statsVisible.collect { visible ->
                _uiState.value = _uiState.value.copy(statsVisible = visible)
            }
        }

        refreshPoints()

        observeBackfillWork()
        observeUploadWork()
    }

    fun onZoomChanged(zoom: Double) {
        currentZoom = zoom
        val newRes = zoomToH3Resolution(zoom)
        if (newRes != _uiState.value.displayResolution) {
            zoomDebounceJob?.cancel()
            zoomDebounceJob = viewModelScope.launch {
                delay(300)
                _uiState.value = _uiState.value.copy(displayResolution = newRes)
                recomputeDisplayHexes()
            }
        }
    }

    private fun recomputeDisplayHexes() {
        val raw = _uiState.value.coveredHexes
        val targetRes = _uiState.value.displayResolution

        if (targetRes >= 8 || h3Core == null) {
            _uiState.value = _uiState.value.copy(displayHexes = raw)
            return
        }

        // Aggregate real H3 hexes to parent resolution
        val parentMap = mutableMapOf<String, AggregatedHex>()
        val gridHexes = mutableListOf<HexCoverage>()

        for (hex in raw) {
            if (hex.getTier() == CoverageTier.UNMAPPED) continue

            if (hex.h3Index.startsWith("grid")) {
                gridHexes.add(hex)
                continue
            }

            try {
                val h3Long = h3Core.stringToH3(hex.h3Index)
                val parentLong = h3Core.cellToParent(h3Long, targetRes)
                val parent = h3Core.h3ToString(parentLong)
                val existing = parentMap[parent]
                if (existing != null) {
                    existing.observationCount += hex.observationCount
                    existing.cellCount += hex.cellCount
                    existing.gnssCount += hex.gnssCount
                    existing.wifiCount += hex.wifiCount
                    if (hex.getTier() == CoverageTier.OWN) existing.hasOwn = true
                } else {
                    parentMap[parent] = AggregatedHex(
                        observationCount = hex.observationCount,
                        cellCount = hex.cellCount,
                        gnssCount = hex.gnssCount,
                        wifiCount = hex.wifiCount,
                        hasOwn = hex.getTier() == CoverageTier.OWN,
                        lastSeen = hex.lastSeen,
                        firstSeen = hex.firstSeen
                    )
                }
            } catch (_: Exception) {
                gridHexes.add(hex)
            }
        }

        // Convert aggregated parent hexes to HexCoverage objects
        val aggregated = parentMap.map { (parentH3, agg) ->
            HexCoverage(
                h3Index = parentH3,
                resolution = targetRes,
                firstSeen = agg.firstSeen,
                lastSeen = agg.lastSeen,
                observationCount = agg.observationCount,
                cellCount = agg.cellCount,
                gnssCount = agg.gnssCount,
                wifiCount = agg.wifiCount,
                tier = if (agg.hasOwn) "OWN" else "OTHER"
            )
        }

        _uiState.value = _uiState.value.copy(
            displayHexes = aggregated + gridHexes
        )
    }

    private fun observeBackfillWork() {
        viewModelScope.launch {
            try {
                val workManager = WorkManager.getInstance(context)
                workManager.getWorkInfosForUniqueWorkFlow("azimuth_coverage_backfill")
                    .collect { workInfoList ->
                        val newState = when {
                            workInfoList.isEmpty() -> BackfillState.IDLE
                            workInfoList.any { it.state == androidx.work.WorkInfo.State.RUNNING } -> BackfillState.RUNNING
                            workInfoList.any { it.state == androidx.work.WorkInfo.State.SUCCEEDED } -> BackfillState.SUCCEEDED
                            workInfoList.any { it.state == androidx.work.WorkInfo.State.FAILED } -> BackfillState.FAILED
                            else -> BackfillState.IDLE
                        }
                        _uiState.value = _uiState.value.copy(backfillState = newState)
                    }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(backfillState = BackfillState.IDLE)
            }
        }
    }

    private fun refreshPoints() {
        viewModelScope.launch {
            try {
                val points = api.getMyPoints()
                _uiState.value = _uiState.value.copy(pointsBalance = points.balance)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch points: ${e.message}")
            }
        }
    }

    private fun observeUploadWork() {
        viewModelScope.launch {
            try {
                val workManager = WorkManager.getInstance(context)
                var wasRunning = false
                workManager.getWorkInfosForUniqueWorkFlow(CollectionController.UPLOAD_WORK_NAME)
                    .collect { workInfoList ->
                        val isRunning = workInfoList.any {
                            it.state == androidx.work.WorkInfo.State.RUNNING
                        }
                        if (wasRunning && !isRunning) {
                            refreshPoints()
                        }
                        wasRunning = isRunning
                    }
            } catch (_: Exception) { }
        }
    }

    fun setTappedHex(hex: HexCoverage?) {
        _uiState.value = _uiState.value.copy(tappedHex = hex, tappedFreshnessHex = null)
    }

    fun setTappedFreshnessHex(hex: HexFreshnessData?) {
        _uiState.value = _uiState.value.copy(tappedFreshnessHex = hex, tappedHex = null)
    }

    fun fetchFreshness(south: Double, west: Double, north: Double, east: Double) {
        val boundsKey = "${(south * 100).toLong()},${(west * 100).toLong()},${(north * 100).toLong()},${(east * 100).toLong()}"
        if (boundsKey == lastFreshnessBounds) return
        lastFreshnessBounds = boundsKey

        freshnessFetchJob?.cancel()
        freshnessFetchJob = viewModelScope.launch {
            delay(500)
            try {
                val bounds = "$south,$west,$north,$east"
                val response = api.getHexFreshness(bounds)
                _uiState.value = _uiState.value.copy(freshnessHexes = response.hexes)
            } catch (e: Exception) {
                Log.w(TAG, "Freshness fetch failed: ${e.message}")
            }
        }
    }

    fun toggleStats() {
        viewModelScope.launch {
            prefs.setStatsVisible(!_uiState.value.statsVisible)
        }
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "MapViewModel"

        fun zoomToH3Resolution(zoom: Double): Int = when {
            zoom >= 15 -> 8
            zoom >= 13 -> 7
            zoom >= 11 -> 6
            zoom >= 9  -> 5
            zoom >= 7  -> 4
            else       -> 3
        }
    }
}

private class AggregatedHex(
    var observationCount: Int,
    var cellCount: Int,
    var gnssCount: Int,
    var wifiCount: Int,
    var hasOwn: Boolean,
    val lastSeen: Long,
    val firstSeen: Long,
)
