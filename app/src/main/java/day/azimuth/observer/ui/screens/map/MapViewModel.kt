package day.azimuth.observer.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.HexCoverage
import day.azimuth.observer.data.local.HexCoverageDao
import day.azimuth.observer.data.local.ObservationDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SchematicCell(
    val label: String,
    val gridX: Int,
    val gridY: Int,
    val pending: Boolean
)

data class MapUiState(
    val totalHexes: Int = 0,
    val hexesToday: Int = 0,
    val coveredHexes: List<HexCoverage> = emptyList(),
    val cellTotal: Int = 0,
    val gnssTotal: Int = 0,
    val wifiTotal: Int = 0,
    val pendingApprox: Int = 0,
    val schematicCells: List<SchematicCell> = emptyList(),
    val schematicRowCount: Int = 0
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val hexCoverageDao: HexCoverageDao,
    private val observationDao: ObservationDao
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
                val schematicList = sorted.filter { it.gridX != null && it.gridY != null }
                    .mapIndexed { index, hex ->
                        SchematicCell(
                            label = "Coverage area ${index + 1}",
                            gridX = hex.gridX!!,
                            gridY = hex.gridY!!,
                            pending = hex.pendingCount > 0
                        )
                    }
                _uiState.value = _uiState.value.copy(
                    totalHexes = list.size,
                    hexesToday = todayList.size,
                    coveredHexes = sorted,
                    cellTotal = list.sumOf { it.cellCount },
                    gnssTotal = list.sumOf { it.gnssCount },
                    wifiTotal = list.sumOf { it.wifiCount },
                    pendingApprox = list.sumOf { it.pendingCount },
                    schematicCells = schematicList,
                    schematicRowCount = schematicList.size
                )
            }
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
}
