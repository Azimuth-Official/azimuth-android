package day.azimuth.observer.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.ObservationDao
import day.azimuth.observer.service.CollectionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isCollecting: Boolean = false,
    val cellTowerCount: Long = 0,
    val gnssSatelliteCount: Long = 0,
    val wifiApCount: Long = 0,
    val totalObservations: Long = 0,
    val pendingUploads: Int = 0,
    val lastUploadStatus: String = "",
    val permissionBlocked: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val collectionController: CollectionController,
    private val observationDao: ObservationDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cellLteCount: Long = 0
    private var cellNrCount: Long = 0
    private var wifiSurveyCount: Long = 0
    private var wifiRttCount: Long = 0

    init {
        viewModelScope.launch {
            observationDao.getCountByType("cell_lte").collect { count ->
                cellLteCount = count
                updateCounts()
            }
        }
        viewModelScope.launch {
            observationDao.getCountByType("cell_nr").collect { count ->
                cellNrCount = count
                updateCounts()
            }
        }
        viewModelScope.launch {
            observationDao.getCountByType("gnss_raw").collect { count ->
                _uiState.value = _uiState.value.copy(gnssSatelliteCount = count)
            }
        }
        viewModelScope.launch {
            observationDao.getCountByType("wifi_survey").collect { count ->
                wifiSurveyCount = count
                updateCounts()
            }
        }
        viewModelScope.launch {
            observationDao.getCountByType("wifi_rtt").collect { count ->
                wifiRttCount = count
                updateCounts()
            }
        }
        viewModelScope.launch {
            observationDao.getTotalCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalObservations = count)
            }
        }
        viewModelScope.launch {
            observationDao.getPendingUploadCount().collect { count ->
                _uiState.value = _uiState.value.copy(pendingUploads = count)
            }
        }
    }

    private fun updateCounts() {
        _uiState.value = _uiState.value.copy(
            cellTowerCount = cellLteCount + cellNrCount,
            wifiApCount = wifiSurveyCount + wifiRttCount,
        )
    }

    fun startCollecting() {
        if (!collectionController.hasRequiredPermissions()) {
            _uiState.value = _uiState.value.copy(permissionBlocked = true)
            Log.w(TAG, "Start requested but permissions missing: ${collectionController.missingPermissions()}")
            return
        }
        val started = collectionController.startCollection()
        if (started) {
            _uiState.value = _uiState.value.copy(isCollecting = true, permissionBlocked = false)
            Log.i(TAG, "Collection started")
        } else {
            _uiState.value = _uiState.value.copy(isCollecting = false)
            Log.e(TAG, "Collection start failed")
        }
    }

    fun stopCollecting() {
        collectionController.stopCollection()
        _uiState.value = _uiState.value.copy(isCollecting = false)
        Log.i(TAG, "Collection stopped")
    }

    fun refreshStatus() {
        _uiState.value = _uiState.value.copy(
            isCollecting = collectionController.isCollectionActive(),
        )
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}
