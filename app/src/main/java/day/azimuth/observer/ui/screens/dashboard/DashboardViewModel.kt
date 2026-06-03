package day.azimuth.observer.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DashboardUiState(
    val isCollecting: Boolean = false,
    val cellTowerCount: Int = 0,
    val gnssSatelliteCount: Int = 0,
    val wifiApCount: Int = 0,
    val totalObservations: Long = 0,
    val pendingUploads: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun startCollecting() {
        _uiState.value = _uiState.value.copy(isCollecting = true)
        // TODO: Start ObservationService via foreground service
    }

    fun stopCollecting() {
        _uiState.value = _uiState.value.copy(isCollecting = false)
        // TODO: Stop ObservationService
    }
}
