package day.azimuth.observer.service.collectors

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurement
import android.location.GnssMeasurementsEvent
import android.location.LocationManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.data.local.Observation
import day.azimuth.observer.data.local.ObservationRepository
import day.azimuth.observer.service.ntrip.NtripManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnssMeasurementCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observationRepository: ObservationRepository,
    private val locationProvider: LocationProvider,
    private val gson: Gson,
    private val ntripManager: NtripManager,
) {
    private var callback: GnssMeasurementsEvent.Callback? = null
    private var scope: CoroutineScope? = null

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        this.scope = scope
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        callback = object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                scope.launch {
                    val location = locationProvider.getLastLocation() ?: return@launch
                    val measurements = event.measurements.map { m ->
                        extractMeasurement(m)
                    }
                    if (measurements.isNotEmpty()) {
                        val rtkActive = ntripManager.isRtkActive.value
                        val obs = Observation(
                            signalType = "gnss_raw",
                            timestamp = System.currentTimeMillis(),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestampNs = event.clock.timeNanos,
                            payload = gson.toJson(
                                mapOf(
                                    "clockBiasNanos" to event.clock.biasNanos,
                                    "clockDriftNanosPerSecond" to event.clock.driftNanosPerSecond,
                                    "measurements" to measurements,
                                ),
                            ),
                            rtkEnabled = rtkActive,
                        )
                        observationRepository.recordObservation(obs)
                    }
                }
            }
        }

        locationManager.registerGnssMeasurementsCallback(callback!!, null)
    }

    fun stop() {
        callback?.let { cb ->
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.unregisterGnssMeasurementsCallback(cb)
        }
        callback = null
        scope = null
    }

    private fun extractMeasurement(m: GnssMeasurement): Map<String, Any?> = mapOf(
        "svid" to m.svid,
        "constellationType" to m.constellationType,
        "state" to m.state,
        "receivedSvTimeNanos" to m.receivedSvTimeNanos,
        "pseudorangeRateMetersPerSecond" to m.pseudorangeRateMetersPerSecond,
        "cn0DbHz" to m.cn0DbHz,
        "carrierFrequencyHz" to if (m.hasCarrierFrequencyHz()) m.carrierFrequencyHz else null,
        "accumulatedDeltaRangeMeters" to m.accumulatedDeltaRangeMeters,
        "accumulatedDeltaRangeState" to m.accumulatedDeltaRangeState,
        "multipathIndicator" to m.multipathIndicator,
    )
}
