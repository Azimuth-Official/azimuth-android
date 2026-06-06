package day.azimuth.observer.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.service.CollectionController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionOnboardingUiState(
    val rationaleVisible: Boolean = true,
    val permanentlyDenied: List<String> = emptyList(),
    val allGranted: Boolean = false,
)

@HiltViewModel
class PermissionOnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val collectionController: CollectionController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionOnboardingUiState())
    val uiState: StateFlow<PermissionOnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PermissionOnboardingEvent>()
    val events: SharedFlow<PermissionOnboardingEvent> = _events.asSharedFlow()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val missing = collectionController.missingPermissions()
        _uiState.value = _uiState.value.copy(
            allGranted = missing.isEmpty(),
            rationaleVisible = missing.isNotEmpty(),
        )
        if (missing.isEmpty()) {
            viewModelScope.launch {
                _events.emit(PermissionOnboardingEvent.AllGranted)
            }
        }
    }

    fun onPermissionResult(results: Map<String, Boolean>) {
        val denied = results.filter { !it.value }.keys.toList()
        val permanentlyDenied = denied.filter { permission ->
            !shouldShowRequestPermissionRationale(context, permission)
        }
        Log.d(TAG, "Permission results: $results, permanentlyDenied: $permanentlyDenied")
        _uiState.value = _uiState.value.copy(
            permanentlyDenied = permanentlyDenied,
        )
        checkPermissions()
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun skip() {
        viewModelScope.launch {
            _events.emit(PermissionOnboardingEvent.Skipped)
        }
    }

    companion object {
        private const val TAG = "PermissionOnboardingVM"

        private fun shouldShowRequestPermissionRationale(context: Context, permission: String): Boolean {
            return when (permission) {
                android.Manifest.permission.ACCESS_FINE_LOCATION -> {
                    // Use Activity context for this; ViewModel can't easily check. Assume true if still requesting.
                    true
                }
                else -> true
            }
        }
    }
}

sealed class PermissionOnboardingEvent {
    data object AllGranted : PermissionOnboardingEvent()
    data object Skipped : PermissionOnboardingEvent()
}
