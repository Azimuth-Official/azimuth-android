package day.azimuth.observer.ui.screens.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.data.local.HexCoverage
import day.azimuth.observer.data.local.HexCoverageDao
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
    val cellTotal: Int = 0,
    val gnssTotal: Int = 0,
    val wifiTotal: Int = 0,
    val pendingApprox: Int = 0,
    val tappedHex: HexCoverage? = null,
    val backfillState: BackfillState = BackfillState.IDLE
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val hexCoverageDao: HexCoverageDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

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
            }
        }

        observeBackfillWork()
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

    fun setTappedHex(hex: HexCoverage?) {
        _uiState.value = _uiState.value.copy(tappedHex = hex)
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
