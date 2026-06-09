package day.azimuth.observer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.UpdateProfileRequest
import day.azimuth.observer.service.CollectionController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "",
    val nodeId: String = "",
    val isRegistered: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    val keepScreenOn: Boolean = false,
    val collectionEnabled: Boolean = true,
    val displayName: String? = null,
    val displayNameInput: String = "",
    val displayNameSaving: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AzimuthPreferences,
    private val collectionController: CollectionController,
    private val api: AzimuthApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                email = prefs.email.first(),
                nodeId = prefs.nodeId.first(),
                isRegistered = prefs.isRegistered.first(),
                keepScreenOn = prefs.keepScreenOn.first(),
                collectionEnabled = prefs.collectionEnabled.first(),
            )
            loadProfile()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = api.getMyProfile()
                _uiState.value = _uiState.value.copy(
                    displayName = profile.displayName,
                    displayNameInput = profile.displayName ?: "",
                )
            } catch (e: Exception) {
                // Silently fail — profile is optional
            }
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(keepScreenOn = enabled)
        viewModelScope.launch {
            prefs.setKeepScreenOn(enabled)
        }
    }

    fun setCollectionEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(collectionEnabled = enabled)
        viewModelScope.launch {
            prefs.setCollectionEnabled(enabled)
            if (enabled) {
                collectionController.startCollection()
            } else {
                collectionController.stopCollection()
            }
        }
    }

    fun requestLogout() {
        _uiState.value = _uiState.value.copy(showLogoutConfirm = true)
    }

    fun cancelLogout() {
        _uiState.value = _uiState.value.copy(showLogoutConfirm = false)
    }

    fun confirmLogout() {
        _uiState.value = _uiState.value.copy(showLogoutConfirm = false)
        viewModelScope.launch {
            collectionController.stopCollection()
            collectionController.cancelAllWork()
            prefs.clear()
            _events.emit(SettingsEvent.LoggedOut)
        }
    }

    fun onDisplayNameChange(input: String) {
        _uiState.value = _uiState.value.copy(displayNameInput = input)
    }

    fun saveDisplayName() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(displayNameSaving = true)
            try {
                val trimmed = _uiState.value.displayNameInput.trim()
                val response = api.updateProfile(UpdateProfileRequest(trimmed))
                _uiState.value = _uiState.value.copy(
                    displayName = response.displayName,
                    displayNameInput = response.displayName ?: "",
                    displayNameSaving = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(displayNameSaving = false)
                // Error handling could be added here
            }
        }
    }
}

sealed class SettingsEvent {
    data object LoggedOut : SettingsEvent()
}
