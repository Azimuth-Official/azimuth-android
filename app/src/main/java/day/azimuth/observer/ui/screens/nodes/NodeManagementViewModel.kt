package day.azimuth.observer.ui.screens.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.NodeInfo
import day.azimuth.observer.data.remote.RegisterNodeRequest
import day.azimuth.observer.data.remote.UpdateNodeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NodesUiState(
    val nodes: List<NodeInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String = "",
    val showAddDialog: Boolean = false,
    val addingNode: Boolean = false,
)

@HiltViewModel
class NodeManagementViewModel @Inject constructor(
    private val api: AzimuthApi,
    private val prefs: AzimuthPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodesUiState())
    val uiState: StateFlow<NodesUiState> = _uiState.asStateFlow()

    init {
        loadNodes()
    }

    fun loadNodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = "")
            try {
                val response = api.getMyNodes()
                _uiState.value = _uiState.value.copy(
                    nodes = response.nodes,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load nodes",
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun dismissAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addNode(hardwareType: String, label: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(addingNode = true)
            try {
                val request = RegisterNodeRequest(
                    hardwareType = hardwareType,
                    label = label?.takeIf { it.isNotBlank() },
                )
                api.registerNode(request)
                _uiState.value = _uiState.value.copy(
                    showAddDialog = false,
                    addingNode = false,
                )
                loadNodes()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    addingNode = false,
                    error = e.message ?: "Failed to add node",
                )
            }
        }
    }

    fun updateLabel(nodeId: String, newLabel: String) {
        viewModelScope.launch {
            try {
                val request = UpdateNodeRequest(label = newLabel.takeIf { it.isNotBlank() })
                api.updateNode(nodeId, request)
                loadNodes()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update node",
                )
            }
        }
    }
}
