package day.azimuth.observer.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.RegisterNodeRequest
import day.azimuth.observer.data.remote.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val api: AzimuthApi,
    private val prefs: AzimuthPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun register() {
        val email = _uiState.value.email.trim()
        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            _uiState.value = _uiState.value.copy(error = "Invalid email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val authResponse = api.register(RegisterRequest(email = email))
                prefs.saveRegistration(
                    userId = authResponse.userId,
                    apiKey = authResponse.apiKey,
                    email = email,
                )

                val nodeResponse = api.registerNode(
                    RegisterNodeRequest(hardwareType = "tier0_mobile"),
                )
                prefs.saveNodeId(nodeResponse.nodeId)

                _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registration failed",
                )
            }
        }
    }
}
