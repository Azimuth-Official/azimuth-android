package day.azimuth.observer.ui.screens.leaderboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.LeaderboardEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val period: String = "alltime",
    val entries: List<LeaderboardEntry> = emptyList(),
    val totalParticipants: Int = 0,
    val isLoading: Boolean = false,
    val error: String = "",
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val api: AzimuthApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        fetchLeaderboard("alltime")
    }

    fun setPeriod(period: String) {
        if (period != _uiState.value.period) {
            _uiState.value = _uiState.value.copy(period = period, isLoading = true)
            fetchLeaderboard(period)
        }
    }

    private fun fetchLeaderboard(period: String) {
        viewModelScope.launch {
            try {
                val response = api.getLeaderboard(period = period, limit = 50)
                _uiState.value = _uiState.value.copy(
                    entries = response.entries,
                    totalParticipants = response.totalParticipants,
                    isLoading = false,
                    error = "",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch leaderboard: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load leaderboard: ${e.message}",
                )
            }
        }
    }

    companion object {
        private const val TAG = "LeaderboardViewModel"
    }
}
