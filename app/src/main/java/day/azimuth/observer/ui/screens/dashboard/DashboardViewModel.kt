package day.azimuth.observer.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.ObservationDao
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.LeaderboardEntry
import day.azimuth.observer.data.remote.ListRewardsResponse
import day.azimuth.observer.data.remote.NetworkStats
import day.azimuth.observer.data.remote.NodeInfo
import day.azimuth.observer.data.remote.PointsResponse
import day.azimuth.observer.data.remote.ReferralResponse
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
    val pointsBalance: Int = 0,
    val networkStats: NetworkStats? = null,
    val myNodes: List<NodeInfo> = emptyList(),
    val recentRewards: ListRewardsResponse? = null,
    val referral: ReferralResponse? = null,
    val loadingError: String = "",
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val collectionController: CollectionController,
    private val observationDao: ObservationDao,
    private val api: AzimuthApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var cellLteCount: Long = 0
    private var cellNrCount: Long = 0
    private var wifiSurveyCount: Long = 0
    private var wifiRttCount: Long = 0

    init {
        // Fetch API data
        fetchNetworkStats()
        fetchMyNodes()
        fetchMyRewards()
        fetchMyPoints()
        fetchMyReferral()
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

    private fun fetchNetworkStats() {
        viewModelScope.launch {
            try {
                val stats = api.getStats()
                _uiState.value = _uiState.value.copy(networkStats = stats, loadingError = "")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch network stats: ${e.message}")
                // Don't block UI on stats failure
            }
        }
    }

    private fun fetchMyNodes() {
        viewModelScope.launch {
            try {
                val response = api.getMyNodes()
                _uiState.value = _uiState.value.copy(myNodes = response.nodes, loadingError = "")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch my nodes: ${e.message}")
                // Don't block UI on nodes failure
            }
        }
    }

    private fun fetchMyRewards() {
        viewModelScope.launch {
            try {
                val rewards = api.getMyRewards()
                _uiState.value = _uiState.value.copy(recentRewards = rewards, loadingError = "")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch my rewards: ${e.message}")
                // Don't block UI on rewards failure
            }
        }
    }

    private fun fetchMyPoints() {
        viewModelScope.launch {
            try {
                val points = api.getMyPoints(limit = 50)
                _uiState.value = _uiState.value.copy(pointsBalance = points.balance, loadingError = "")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch my points: ${e.message}")
                // Don't block UI on points failure
            }
        }
    }

    private fun fetchMyReferral() {
        viewModelScope.launch {
            try {
                val referral = api.getMyReferral()
                _uiState.value = _uiState.value.copy(referral = referral, loadingError = "")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch my referral: ${e.message}")
                // Don't block UI on referral failure
            }
        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}
