package day.azimuth.observer.ui.screens.onboarding

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import day.azimuth.observer.R
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.GoogleSignInRequest
import day.azimuth.observer.data.remote.LoginRequest
import day.azimuth.observer.data.remote.RegisterNodeRequest
import day.azimuth.observer.data.remote.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val referralCode: String = "",
    val isLoginMode: Boolean = false,
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

    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun setConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword, error = null)
    }

    fun setReferralCode(referralCode: String) {
        _uiState.value = _uiState.value.copy(referralCode = referralCode, error = null)
    }

    fun toggleLoginMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            password = "",
            confirmPassword = "",
            error = null,
        )
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            _uiState.value = _uiState.value.copy(error = "Invalid email address")
            return
        }

        if (password.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Password cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val loginResponse = api.login(LoginRequest(email = email, password = password))
                completeAuthAndRegisterNode(
                    userId = loginResponse.userId,
                    apiKey = loginResponse.apiKey,
                    email = email,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed",
                )
            }
        }
    }

    fun register() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword
        val referralCode = _uiState.value.referralCode.trim().ifEmpty { null }

        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            _uiState.value = _uiState.value.copy(error = "Invalid email address")
            return
        }

        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 8 characters")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val authResponse = api.register(RegisterRequest(email = email, password = password, referralCode = referralCode))
                completeAuthAndRegisterNode(
                    userId = authResponse.userId,
                    apiKey = authResponse.apiKey,
                    email = email,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registration failed",
                )
            }
        }
    }

    fun googleSignIn(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(context.getString(R.string.google_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = googleCredential.idToken

                val response = api.googleSignIn(GoogleSignInRequest(idToken = idToken))
                completeAuthAndRegisterNode(
                    userId = response.userId,
                    apiKey = response.apiKey,
                    email = googleCredential.id,
                )
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: NoCredentialException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google Sign-In is not available. Please register with email.",
                )
            } catch (e: GetCredentialException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google sign-in failed. Please try again.",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign-in failed",
                )
            }
        }
    }

    private suspend fun completeAuthAndRegisterNode(userId: String, apiKey: String, email: String) {
        prefs.saveRegistration(userId = userId, apiKey = apiKey, email = email)
        try {
            val nodesResponse = api.getMyNodes()
            if (nodesResponse.nodes.isNotEmpty()) {
                prefs.saveNodeId(nodesResponse.nodes.first().id)
            } else {
                val nodeResponse = api.registerNode(
                    RegisterNodeRequest(hardwareType = "tier0_mobile"),
                )
                prefs.saveNodeId(nodeResponse.nodeId)
            }
        } catch (e: Exception) {
            val nodeResponse = api.registerNode(
                RegisterNodeRequest(hardwareType = "tier0_mobile"),
            )
            prefs.saveNodeId(nodeResponse.nodeId)
        }
        _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true)
    }
}
