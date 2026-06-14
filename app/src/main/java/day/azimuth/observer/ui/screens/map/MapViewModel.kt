package day.azimuth.observer.ui.screens.map

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.HexFreshnessData
import day.azimuth.observer.service.CollectionController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BackfillState {
    IDLE, RUNNING, SUCCEEDED, FAILED
}

data class MapUiState(
    val totalHexes: Int = 0,
    val hexesToday: Int = 0,
    val cellTotal: Int = 0,
    val gnssTotal: Int = 0,
    val wifiTotal: Int = 0,
    val pendingApprox: Int = 0,
    val freshnessHexes: List<HexFreshnessData> = emptyList(),
    val tappedFreshnessHex: HexFreshnessData? = null,
    val backfillState: BackfillState = BackfillState.IDLE,
    val statsVisible: Boolean = true,
    val pointsBalance: Int = 0,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AzimuthPreferences,
    private val api: AzimuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var freshnessFetchJob: Job? = null
    private var lastFreshnessBounds: String = ""

    init {
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
        // Zoom tracking removed with freshness-only rendering
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

    fun setTappedFreshnessHex(hex: HexFreshnessData?) {
        _uiState.value = _uiState.value.copy(tappedFreshnessHex = hex)
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

    companion object {
        private const val TAG = "MapViewModel"
    }
}
