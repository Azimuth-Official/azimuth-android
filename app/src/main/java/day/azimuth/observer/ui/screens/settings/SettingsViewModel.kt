package day.azimuth.observer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.AzimuthPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "",
    val apiEndpoint: String = "http://192.168.3.162:3000/",
    val nodeId: String = "",
    val isRegistered: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AzimuthPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                email = prefs.email.first(),
                apiEndpoint = prefs.apiEndpoint.first(),
                nodeId = prefs.nodeId.first(),
                isRegistered = prefs.isRegistered.first(),
            )
        }
    }

    fun setApiEndpoint(endpoint: String) {
        _uiState.value = _uiState.value.copy(apiEndpoint = endpoint)
        viewModelScope.launch {
            prefs.saveApiEndpoint(endpoint)
        }
    }
}
