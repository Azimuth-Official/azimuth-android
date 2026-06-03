package day.azimuth.observer.service.collectors

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.data.local.Observation
import day.azimuth.observer.data.local.ObservationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiSurveyCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observationDao: ObservationDao,
    private val locationProvider: LocationProvider,
    private val gson: Gson,
) {
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        job = scope.launch {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            while (isActive) {
                val location = locationProvider.getLastLocation()
                if (location != null) {
                    @Suppress("DEPRECATION")
                    val results = wifiManager.scanResults
                    if (results.isNotEmpty()) {
                        val entries = results.map { sr ->
                            mapOf(
                                "bssid" to sr.BSSID,
                                "ssid" to sr.SSID,
                                "level" to sr.level,
                                "frequency" to sr.frequency,
                                "channelWidth" to sr.channelWidth,
                                "capabilities" to sr.capabilities,
                                "timestamp" to sr.timestamp,
                            )
                        }
                        observationDao.insert(
                            Observation(
                                signalType = "wifi_survey",
                                timestamp = System.currentTimeMillis(),
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                payload = gson.toJson(entries),
                            ),
                        )
                    }
                    wifiManager.startScan()
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        private const val SCAN_INTERVAL_MS = 30_000L // 30 seconds
    }
}
