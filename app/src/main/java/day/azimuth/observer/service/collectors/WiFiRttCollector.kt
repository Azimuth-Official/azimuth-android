package day.azimuth.observer.service.collectors

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.data.local.Observation
import day.azimuth.observer.data.local.ObservationDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiRttCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observationDao: ObservationDao,
    private val locationProvider: LocationProvider,
    private val gson: Gson,
) {
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        // WiFi RTT requires Android 9+ (API 28)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val rttManager = context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as? WifiRttManager ?: return
        if (!context.packageManager.hasSystemFeature("android.hardware.wifi.rtt")) return

        job = scope.launch {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val executor = Executors.newSingleThreadExecutor()

            while (isActive) {
                val location = locationProvider.getLastLocation()
                if (location != null) {
                    @Suppress("DEPRECATION")
                    val rttCapable = wifiManager.scanResults.filter { it.is80211mcResponder }

                    if (rttCapable.isNotEmpty()) {
                        val request = RangingRequest.Builder()
                            .addAccessPoints(rttCapable.take(RangingRequest.getMaxPeers()))
                            .build()

                        rttManager.startRanging(request, executor, object : RangingResultCallback() {
                            override fun onRangingResults(results: List<RangingResult>) {
                                scope.launch {
                                    val loc = locationProvider.getLastLocation() ?: return@launch
                                    val successful = results
                                        .filter { it.status == RangingResult.STATUS_SUCCESS }
                                        .map { r ->
                                            mapOf(
                                                "macAddress" to r.macAddress.toString(),
                                                "distanceMm" to r.distanceMm,
                                                "distanceStdDevMm" to r.distanceStdDevMm,
                                                "rssi" to r.rssi,
                                                "numAttemptedMeasurements" to r.numAttemptedMeasurements,
                                                "numSuccessfulMeasurements" to r.numSuccessfulMeasurements,
                                            )
                                        }
                                    if (successful.isNotEmpty()) {
                                        observationDao.insert(
                                            Observation(
                                                signalType = "wifi_rtt",
                                                timestamp = System.currentTimeMillis(),
                                                latitude = loc.latitude,
                                                longitude = loc.longitude,
                                                accuracy = loc.accuracy,
                                                payload = gson.toJson(successful),
                                            ),
                                        )
                                    }
                                }
                            }

                            override fun onRangingFailure(code: Int) {
                                // WiFi RTT not available — non-critical, skip silently
                            }
                        })
                    }
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
        private const val SCAN_INTERVAL_MS = 30_000L
    }
}
