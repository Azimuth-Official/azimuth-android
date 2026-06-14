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
import day.azimuth.observer.data.local.EphemerisNtripConfig
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
    private var observationClient: NtripClient? = null
    private var ephemerisClient: NtripClient? = null
    private var ephemerisConfig: EphemerisNtripConfig? = null
    private val parser = Rtcm3Parser()
    private val engine = CorrectionEngine(listOf(
        Tier1Injector(GnssCorrector(context)),
        Tier3Solver(),
    ))
    private val encryptedPrefs = createEncryptedPrefs()

    private val _isRtkActive = MutableStateFlow(false)
    val isRtkActive: StateFlow<Boolean> = _isRtkActive.asStateFlow()

    private val _status = MutableStateFlow(NtripStatus())
    val status: StateFlow<NtripStatus> = _status.asStateFlow()

    private var gpsTaskJob: Job? = null
    private var observationReadJob: Job? = null
    private var ephemerisReadJob: Job? = null

    fun start(config: NtripConfig) {
        scope.launch {
            try {
                engine.start()
                ephemerisConfig = getEphemerisConfig()
                val isDualMode = ephemerisConfig != null

                // Connect observation client
                val obsClient = NtripClient()
                obsClient.connect(config)
                observationClient = obsClient
                saveConfig(config)
                _status.value = _status.value.copy(state = NtripConnectionState.CONNECTED)

                // Start observation read loop
                observationReadJob = scope.launch {
                    startObservationReadLoop(obsClient, isDualMode)
                }

                // Start ephemeris read loop if dual mode
                if (isDualMode && ephemerisConfig != null) {
                    val ephClient = NtripClient()
                    try {
                        // Convert EphemerisNtripConfig to NtripConfig for connection
                        val ephConnConfig = NtripConfig(
                            providerName = "ephemeris_mount",
                            casterUrl = ephemerisConfig!!.casterUrl,
                            casterPort = ephemerisConfig!!.casterPort,
                            mountpoint = ephemerisConfig!!.mountpoint,
                            username = ephemerisConfig!!.username,
                            password = ephemerisConfig!!.password,
                        )
                        ephClient.connect(ephConnConfig)
                        ephemerisClient = ephClient
                        Log.i(TAG, "Ephemeris mount connected")

                        ephemerisReadJob = scope.launch {
                            startEphemerisReadLoop(ephClient)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to connect ephemeris mount: ${e.message}")
                        ephemerisClient = null
                    }
                }

                // Wait for observation client to disconnect
                while (obsClient.isConnected()) {
                    delay(1000)
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

    private suspend fun startObservationReadLoop(client: NtripClient, isDualMode: Boolean) {
        var messagesProcessed = 0
        var lastGpsSend = System.currentTimeMillis()
        var rtcmReceived = false
        val streamType = if (isDualMode) RtklibNative.STREAM_BASE_OBS else RtklibNative.STREAM_COMBINED

        while (client.isConnected()) {
            val rtcmData = client.readRtcmData()
            if (rtcmData != null) {
                val messages = parser.parseMessage(rtcmData)
                if (messages.isNotEmpty()) {
                    messagesProcessed += messages.size
                    _status.value = _status.value.copy(
                        messagesDecoded = messagesProcessed
                    )
                    engine.onRtcmData(rtcmData, streamType)
                    if (!rtcmReceived) {
                        rtcmReceived = true
                        if (engine.isAnyTierApplyingCorrections()) {
                            _isRtkActive.value = true
                            Log.i(TAG, "RTK active: RTCM data received, corrections applied")
                        } else {
                            Log.i(TAG, "RTCM data flowing but chipset does not support correction injection")
                        }
                    }
                }
            }

            // Send GGA every 10 seconds for VRS support (observation mount only)
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

            delay(100)
        }
    }

    private suspend fun startEphemerisReadLoop(client: NtripClient) {
        while (client.isConnected()) {
            val rtcmData = client.readRtcmData()
            if (rtcmData != null) {
                val messages = parser.parseMessage(rtcmData)
                if (messages.isNotEmpty()) {
                    engine.onRtcmData(rtcmData, RtklibNative.STREAM_EPHEMERIS)
                }
            }
            delay(100)
        }
        Log.i(TAG, "Ephemeris mount connection lost")
    }

    fun stop() {
        gpsTaskJob?.cancel()
        observationReadJob?.cancel()
        ephemerisReadJob?.cancel()
        observationClient?.disconnect()
        ephemerisClient?.disconnect()
        observationClient = null
        ephemerisClient = null
        engine.stop()
        _isRtkActive.value = false
        _status.value = NtripStatus()
    }

    /**
     * Feed ephemeris-only RTCM bytes (from a dedicated ephemeris mount).
     * Routes to CorrectionEngine.onEphemerisData — Tier1 ignores (default no-op),
     * Tier3 feeds into native ephemeris decoder.
     *
     * Does NOT pass through Tier1 injection path.
     */
    fun onEphemerisData(data: ByteArray) {
        engine.onEphemerisData(data)
    }

    /**
     * Forward one rover measurement epoch to the correction engine.
     * Called from GnssMeasurementCollector when hasFullBiasNanos is true.
     * Routes through CorrectionEngine → CorrectionTier.processRoverEpoch().
     * Tier1 ignores (default no-op), Tier3 packs into native obsd_t + rtkpos().
     */
    fun processRoverEpoch(
        timeNanos: Long,
        fullBiasNanos: Long,
        biasNanos: Double,
        svids: IntArray,
        constellationTypes: IntArray,
        states: IntArray,
        receivedSvTimeNanos: LongArray,
        timeOffsetNanos: DoubleArray,
        cn0DbHz: DoubleArray,
        carrierFreqHz: DoubleArray,
        pseudorangeRateMps: DoubleArray,
        adrMeters: DoubleArray,
        adrStates: IntArray,
    ) {
        engine.processRoverEpoch(
            timeNanos, fullBiasNanos, biasNanos,
            svids, constellationTypes, states,
            receivedSvTimeNanos, timeOffsetNanos, cn0DbHz,
            carrierFreqHz, pseudorangeRateMps, adrMeters, adrStates,
        )
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

    fun getEphemerisConfig(): EphemerisNtripConfig? {
        val json = encryptedPrefs.getString(KEY_EPH_NTRIP_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, EphemerisNtripConfig::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse saved ephemeris NTRIP config: ${e.message}")
            null
        }
    }

    fun saveEphemerisConfig(config: EphemerisNtripConfig) {
        val json = gson.toJson(config)
        encryptedPrefs.edit().putString(KEY_EPH_NTRIP_CONFIG, json).apply()
        Log.i(TAG, "Saved ephemeris NTRIP config")
    }

    fun clearEphemerisConfig() {
        encryptedPrefs.edit().remove(KEY_EPH_NTRIP_CONFIG).apply()
        Log.i(TAG, "Cleared ephemeris NTRIP config")
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
        private const val KEY_EPH_NTRIP_CONFIG = "eph_ntrip_config"
    }
}
