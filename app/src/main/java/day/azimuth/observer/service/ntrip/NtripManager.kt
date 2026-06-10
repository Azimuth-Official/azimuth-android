package day.azimuth.observer.service.ntrip

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import day.azimuth.observer.data.local.NtripConfig
import day.azimuth.observer.data.local.NtripConnectionState
import day.azimuth.observer.data.local.NtripStatus
import day.azimuth.observer.service.collectors.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NtripManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val locationProvider: LocationProvider,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var ntripClient: NtripClient? = null
    private val parser = Rtcm3Parser()
    private val corrector = GnssCorrector(context)
    private val encryptedPrefs = createEncryptedPrefs()

    private val _isRtkActive = MutableStateFlow(false)
    val isRtkActive: StateFlow<Boolean> = _isRtkActive.asStateFlow()

    private val _status = MutableStateFlow(NtripStatus())
    val status: StateFlow<NtripStatus> = _status.asStateFlow()

    private var gpsTaskJob: Job? = null

    fun start(config: NtripConfig) {
        scope.launch {
            try {
                val client = NtripClient()
                client.connect(config)
                ntripClient = client
                saveConfig(config)

                // Collect status updates
                var messagesProcessed = 0
                var lastGpsSend = System.currentTimeMillis()
                var rtcmReceived = false

                while (client.isConnected()) {
                    val rtcmData = client.readRtcmData()
                    if (rtcmData != null) {
                        // Parse and inject corrections
                        val messages = parser.parseMessage(rtcmData)
                        if (messages.isNotEmpty()) {
                            messagesProcessed += messages.size
                            _status.value = _status.value.copy(
                                messagesDecoded = messagesProcessed
                            )
                            corrector.injectCorrections(rtcmData)
                            if (!rtcmReceived) {
                                rtcmReceived = true
                                _isRtkActive.value = true
                                Log.i(TAG, "RTK active: first RTCM data received and injected")
                            }
                        }
                    }

                    // Send GGA every 10 seconds for VRS support
                    val now = System.currentTimeMillis()
                    if (now - lastGpsSend > 10000) {
                        try {
                            val location = locationProvider.getLastLocation()
                            if (location != null) {
                                client.sendGga(location.latitude, location.longitude, location.altitude)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to send GGA: ${e.message}")
                        }
                        lastGpsSend = now
                    }

                    delay(100) // Brief delay before next read
                }

                Log.i(TAG, "NTRIP connection lost")
                _isRtkActive.value = false
                _status.value = _status.value.copy(
                    state = NtripConnectionState.DISCONNECTED
                )
            } catch (e: Exception) {
                Log.e(TAG, "NTRIP manager error: ${e.message}")
                _isRtkActive.value = false
                _status.value = _status.value.copy(
                    state = NtripConnectionState.ERROR,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun stop() {
        gpsTaskJob?.cancel()
        ntripClient?.disconnect()
        ntripClient = null
        _isRtkActive.value = false
        _status.value = NtripStatus()
    }

    fun getConfig(): NtripConfig? {
        val json = encryptedPrefs.getString(KEY_NTRIP_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, NtripConfig::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse saved NTRIP config: ${e.message}")
            null
        }
    }

    fun saveConfig(config: NtripConfig) {
        val json = gson.toJson(config)
        encryptedPrefs.edit().putString(KEY_NTRIP_CONFIG, json).apply()
        Log.i(TAG, "Saved NTRIP config for ${config.providerName}")
    }

    private fun createEncryptedPrefs(): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "ntrip_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    companion object {
        private const val TAG = "NtripManager"
        private const val KEY_NTRIP_CONFIG = "ntrip_config"
    }
}
