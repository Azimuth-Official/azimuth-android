package day.azimuth.observer.service.ntrip.replay

import day.azimuth.observer.service.ntrip.CorrectionTier
import day.azimuth.observer.service.ntrip.RtklibNative

/**
 * Test double that records all method invocations for assertion.
 *
 * Implements the CorrectionTier interface with streamType parameter on onRtcmData().
 * Captures RTCM data, ephemeris data, and rover epoch calls for verification in tests.
 */
class MockCorrectionTier(override val id: String = "mock") : CorrectionTier {

    data class RtcmCall(val dataSize: Int, val streamType: Int)

    val rtcmCalls = mutableListOf<RtcmCall>()
    val ephCalls = mutableListOf<Int>() // data sizes
    var roverCallCount = 0
        private set

    override fun onRtcmData(data: ByteArray, streamType: Int) {
        rtcmCalls.add(RtcmCall(data.size, streamType))
    }

    override fun onEphemerisData(data: ByteArray) {
        ephCalls.add(data.size)
    }

    override fun processRoverEpoch(
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
        roverCallCount++
    }

    override fun isSupported() = true
    override fun start() {}
    override fun stop() {}
    override fun isApplyingCorrections() = false
    override fun reportedAccuracyMeters(): Float? = null

    /**
     * Reset all recorded calls for a fresh test.
     */
    fun reset() {
        rtcmCalls.clear()
        ephCalls.clear()
        roverCallCount = 0
    }
}
