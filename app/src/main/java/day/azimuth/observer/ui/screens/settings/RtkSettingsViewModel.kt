package day.azimuth.observer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.NtripConfig
import day.azimuth.observer.data.local.NtripStatus
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
)

@HiltViewModel
class RtkSettingsViewModel @Inject constructor(
    private val ntripManager: NtripManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RtkSettingsUiState())
    val uiState: StateFlow<RtkSettingsUiState> = _uiState.asStateFlow()

    val status: StateFlow<NtripStatus> = ntripManager.status
    val isRtkActive: StateFlow<Boolean> = ntripManager.isRtkActive

    init {
        viewModelScope.launch {
            val config = ntripManager.getConfig()
            if (config != null) {
                _uiState.value = RtkSettingsUiState(
                    providerName = config.providerName,
                    casterUrl = config.casterUrl,
                    casterPort = config.casterPort,
                    mountpoint = config.mountpoint,
                    username = config.username,
                    password = config.password,
                    isEnabled = ntripManager.isRtkActive.value,
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
            ntripManager.start(config)
            _uiState.value = _uiState.value.copy(isEnabled = true)
        }
    }

    fun disconnect() {
        ntripManager.stop()
        _uiState.value = _uiState.value.copy(isEnabled = false)
    }
}
