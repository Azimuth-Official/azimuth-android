package day.azimuth.observer.service.ntrip

import android.util.Log

class Tier1Injector(private val corrector: GnssCorrector) : CorrectionTier {

    override val id: String = "tier1_hidden_api"

    private var hasReceivedRtcm = false

    override fun isSupported(): Boolean = corrector.isSupported()

    override fun start() {
        hasReceivedRtcm = false
        Log.d(TAG, "Tier1Injector started")
    }

    override fun stop() {
        hasReceivedRtcm = false
        Log.d(TAG, "Tier1Injector stopped")
    }

    override fun onRtcmData(data: ByteArray) {
        corrector.injectCorrections(data)
        if (!hasReceivedRtcm) {
            hasReceivedRtcm = true
        }
    }

    override fun isApplyingCorrections(): Boolean {
        // MUST reduce to EXACTLY the ef0a376 gate:
        // chipset supported AND RTCM corrections flowing
        return hasReceivedRtcm && corrector.isChipsetSupported() == true
    }

    override fun reportedAccuracyMeters(): Float? = null

    // Explicit no-ops — prevent AbstractMethodError from Kotlin incremental
    // compilation when CorrectionTier interface gains default methods.
    override fun onEphemerisData(data: ByteArray) {}
    override fun processRoverEpoch(
        timeNanos: Long, fullBiasNanos: Long, biasNanos: Double,
        svids: IntArray, constellationTypes: IntArray, states: IntArray,
        receivedSvTimeNanos: LongArray, timeOffsetNanos: DoubleArray,
        cn0DbHz: DoubleArray, carrierFreqHz: DoubleArray,
        pseudorangeRateMps: DoubleArray, adrMeters: DoubleArray, adrStates: IntArray,
    ) {}

    companion object {
        private const val TAG = "Tier1Injector"
    }
}
