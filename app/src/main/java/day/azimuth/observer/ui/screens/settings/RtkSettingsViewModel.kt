package day.azimuth.observer.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.EphemerisNtripConfig
import day.azimuth.observer.data.local.NtripConfig
import day.azimuth.observer.data.local.NtripStatus
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.RegisterRtkProviderRequest
import day.azimuth.observer.service.ntrip.NtripManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RtkSettingsUiState(
    val providerName: String = "",
    val casterUrl: String = "",
    val casterPort: Int = 2101,
    val mountpoint: String = "",
    val username: String = "",
    val password: String = "",
    val isEnabled: Boolean = false,
    // Ephemeris mount (dual NTRIP)
    val ephEnabled: Boolean = false,
    val ephCasterUrl: String = "",
    val ephCasterPort: Int = 2101,
    val ephMountpoint: String = "",
    val ephUsername: String = "",
    val ephPassword: String = "",
)

@HiltViewModel
class RtkSettingsViewModel @Inject constructor(
    private val ntripManager: NtripManager,
    private val api: AzimuthApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RtkSettingsUiState())
    val uiState: StateFlow<RtkSettingsUiState> = _uiState.asStateFlow()

    val status: StateFlow<NtripStatus> = ntripManager.status
    val isRtkActive: StateFlow<Boolean> = ntripManager.isRtkActive

    init {
        viewModelScope.launch {
            val config = ntripManager.getConfig()
            val ephConfig = ntripManager.getEphemerisConfig()
            if (config != null) {
                _uiState.value = RtkSettingsUiState(
                    providerName = config.providerName,
                    casterUrl = config.casterUrl,
                    casterPort = config.casterPort,
                    mountpoint = config.mountpoint,
                    username = config.username,
                    password = config.password,
                    isEnabled = ntripManager.isRtkActive.value,
                    ephEnabled = ephConfig != null,
                    ephCasterUrl = ephConfig?.casterUrl ?: "",
                    ephCasterPort = ephConfig?.casterPort ?: 2101,
                    ephMountpoint = ephConfig?.mountpoint ?: "",
                    ephUsername = ephConfig?.username ?: "",
                    ephPassword = ephConfig?.password ?: "",
                )
            }
        }
    }

    fun testConnection(
        providerName: String,
        casterUrl: String,
        casterPort: Int,
        mountpoint: String,
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
            val config = NtripConfig(
                providerName = providerName,
                casterUrl = casterUrl,
                casterPort = casterPort,
                mountpoint = mountpoint,
                username = username,
                password = password,
            )
            ntripManager.start(config)
        }
    }

    fun saveAndEnable(
        providerName: String,
        casterUrl: String,
        casterPort: Int,
        mountpoint: String,
        username: String,
        password: String,
        ephEnabled: Boolean = false,
        ephCasterUrl: String = "",
        ephCasterPort: Int = 2101,
        ephMountpoint: String = "",
        ephUsername: String = "",
        ephPassword: String = "",
    ) {
        viewModelScope.launch {
            val config = NtripConfig(
                providerName = providerName,
                casterUrl = casterUrl,
                casterPort = casterPort,
                mountpoint = mountpoint,
                username = username,
                password = password,
            )
            ntripManager.saveConfig(config)

            // Save ephemeris config if enabled
            if (ephEnabled && ephCasterUrl.isNotEmpty() && ephMountpoint.isNotEmpty()) {
                val ephConfig = EphemerisNtripConfig(
                    casterUrl = ephCasterUrl,
                    casterPort = ephCasterPort,
                    mountpoint = ephMountpoint,
                    username = ephUsername,
                    password = ephPassword,
                )
                ntripManager.saveEphemerisConfig(ephConfig)
            } else {
                ntripManager.clearEphemerisConfig()
            }

            // Register RTK provider with server for bonus points
            try {
                api.registerRtkProvider(RegisterRtkProviderRequest("other"))
            } catch (e: Exception) {
                Log.w("RtkSettingsVM", "Failed to register RTK provider: ${e.message}")
            }

            ntripManager.start(config)
            _uiState.value = _uiState.value.copy(isEnabled = true)
        }
    }

    fun disconnect() {
        ntripManager.stop()
        _uiState.value = _uiState.value.copy(isEnabled = false)
    }

    fun toggleEphemerisMount(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(ephEnabled = enabled)
    }
}
