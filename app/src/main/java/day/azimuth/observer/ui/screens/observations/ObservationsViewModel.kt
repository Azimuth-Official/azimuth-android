package day.azimuth.observer.ui.screens.observations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.Observation
import day.azimuth.observer.data.local.ObservationDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObservationsUiState(
    val observations: List<Observation> = emptyList(),
    val totalCount: Int = 0,
)

@HiltViewModel
class ObservationsViewModel @Inject constructor(
    private val observationDao: ObservationDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ObservationsUiState())
    val uiState: StateFlow<ObservationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observationDao.getRecent(200).collect { list ->
                _uiState.value = ObservationsUiState(
                    observations = list,
                    totalCount = list.size,
                )
            }
        }
    }
}
