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
                            fullBiasNanos = if (event.clock.hasFullBiasNanos()) event.clock.fullBiasNanos else null,
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

                        // Forward rover epoch to Tier 3 solver via CorrectionEngine.
                        // Only invoke when fullBiasNanos is available (required for
                        // pseudorange computation in native code).
                        if (event.clock.hasFullBiasNanos()) {
                            forwardRoverEpoch(event)
                        }
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

    /**
     * Forward one measurement epoch to the native Tier 3 solver via
     * NtripManager -> CorrectionEngine -> CorrectionTier.processRoverEpoch().
     *
     * Builds parallel primitive arrays from GnssMeasurementsEvent, filtering
     * to measurements that have a carrier frequency (required for code mapping).
     */
    private fun forwardRoverEpoch(event: GnssMeasurementsEvent) {
        val validMeasurements = event.measurements.filter { it.hasCarrierFrequencyHz() }
        if (validMeasurements.isEmpty()) return

        val n = validMeasurements.size
        val svids = IntArray(n)
        val constTypes = IntArray(n)
        val states = IntArray(n)
        val recvSvTime = LongArray(n)
        val timeOffset = DoubleArray(n)
        val cn0 = DoubleArray(n)
        val carrierFreq = DoubleArray(n)
        val prRate = DoubleArray(n)
        val adrM = DoubleArray(n)
        val adrSt = IntArray(n)

        for (i in validMeasurements.indices) {
            val m = validMeasurements[i]
            svids[i] = m.svid
            constTypes[i] = m.constellationType
            states[i] = m.state
            recvSvTime[i] = m.receivedSvTimeNanos
            timeOffset[i] = m.timeOffsetNanos
            cn0[i] = m.cn0DbHz.toDouble()
            carrierFreq[i] = m.carrierFrequencyHz.toDouble()
            prRate[i] = m.pseudorangeRateMetersPerSecond
            adrM[i] = m.accumulatedDeltaRangeMeters
            adrSt[i] = m.accumulatedDeltaRangeState
        }

        ntripManager.processRoverEpoch(
            timeNanos = event.clock.timeNanos,
            fullBiasNanos = event.clock.fullBiasNanos,
            biasNanos = event.clock.biasNanos,
            svids = svids,
            constellationTypes = constTypes,
            states = states,
            receivedSvTimeNanos = recvSvTime,
            timeOffsetNanos = timeOffset,
            cn0DbHz = cn0,
            carrierFreqHz = carrierFreq,
            pseudorangeRateMps = prRate,
            adrMeters = adrM,
            adrStates = adrSt,
        )
    }

    private fun extractMeasurement(m: GnssMeasurement): Map<String, Any?> = mapOf(
        "svid" to m.svid,
        "constellationType" to m.constellationType,
        "state" to m.state,
        "receivedSvTimeNanos" to m.receivedSvTimeNanos,
        "timeOffsetNanos" to m.timeOffsetNanos,
        "pseudorangeRateMetersPerSecond" to m.pseudorangeRateMetersPerSecond,
        "cn0DbHz" to m.cn0DbHz,
        "carrierFrequencyHz" to if (m.hasCarrierFrequencyHz()) m.carrierFrequencyHz else null,
        "accumulatedDeltaRangeMeters" to m.accumulatedDeltaRangeMeters,
        "accumulatedDeltaRangeState" to m.accumulatedDeltaRangeState,
        "multipathIndicator" to m.multipathIndicator,
    )
}
