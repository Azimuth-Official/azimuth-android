package day.azimuth.observer.service.ntrip

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests stream-type-aware routing in CorrectionEngine.
 * Verifies that ephemeris-only bytes are STRUCTURALLY BLOCKED from reaching Tier1 injection.
 */
class CorrectionEngineRoutingTest {

    // Manual mock — records all method calls
    private class RecordingTier(override val id: String) : CorrectionTier {
        data class RtcmCall(val dataSize: Int, val streamType: Int)

        val rtcmCalls = mutableListOf<RtcmCall>()
        val ephCalls = mutableListOf<Int>()  // data sizes

        override fun onRtcmData(data: ByteArray, streamType: Int) {
            rtcmCalls.add(RtcmCall(data.size, streamType))
        }

        override fun onEphemerisData(data: ByteArray) {
            ephCalls.add(data.size)
        }

        override fun isSupported() = true
        override fun start() {}
        override fun stop() {}
        override fun processRoverEpoch(
            timeNanos: Long, fullBiasNanos: Long, biasNanos: Double,
            svids: IntArray, constellationTypes: IntArray, states: IntArray,
            receivedSvTimeNanos: LongArray, timeOffsetNanos: DoubleArray,
            cn0DbHz: DoubleArray, carrierFreqHz: DoubleArray,
            pseudorangeRateMps: DoubleArray, adrMeters: DoubleArray, adrStates: IntArray,
        ) {}
        override fun isApplyingCorrections() = false
        override fun reportedAccuracyMeters(): Float? = null
    }

    @Test
    fun `STREAM_EPHEMERIS routes through onEphemerisData only`() {
        val tier = RecordingTier("test_tier")
        val engine = CorrectionEngine(listOf(tier))

        val testData = byteArrayOf(0x01, 0x02, 0x03)
        engine.onRtcmData(testData, RtklibNative.STREAM_EPHEMERIS)

        // CRITICAL: ephemeris bytes never reach onRtcmData — structurally blocked
        assertEquals(0, tier.rtcmCalls.size, "Ephemeris data should NOT reach onRtcmData")
        assertEquals(1, tier.ephCalls.size, "Ephemeris data MUST reach onEphemerisData")
        assertEquals(3, tier.ephCalls[0], "Data size preserved in ephemeris call")
    }

    @Test
    fun `STREAM_BASE_OBS routes to onRtcmData with correct stream type`() {
        val tier = RecordingTier("test_tier")
        val engine = CorrectionEngine(listOf(tier))

        val testData = byteArrayOf(0x01, 0x02, 0x03)
        engine.onRtcmData(testData, RtklibNative.STREAM_BASE_OBS)

        assertEquals(1, tier.rtcmCalls.size, "BASE_OBS must reach onRtcmData")
        assertEquals(3, tier.rtcmCalls[0].dataSize, "Data size preserved")
        assertEquals(RtklibNative.STREAM_BASE_OBS, tier.rtcmCalls[0].streamType, "Stream type preserved")
        assertEquals(0, tier.ephCalls.size, "BASE_OBS should NOT reach onEphemerisData")
    }

    @Test
    fun `STREAM_COMBINED routes to onRtcmData with correct stream type`() {
        val tier = RecordingTier("test_tier")
        val engine = CorrectionEngine(listOf(tier))

        val testData = byteArrayOf(0x01, 0x02, 0x03)
        engine.onRtcmData(testData, RtklibNative.STREAM_COMBINED)

        assertEquals(1, tier.rtcmCalls.size, "COMBINED must reach onRtcmData")
        assertEquals(3, tier.rtcmCalls[0].dataSize, "Data size preserved")
        assertEquals(RtklibNative.STREAM_COMBINED, tier.rtcmCalls[0].streamType, "Stream type preserved")
        assertEquals(0, tier.ephCalls.size, "COMBINED should NOT reach onEphemerisData")
    }

    @Test
    fun `default streamType parameter uses STREAM_COMBINED for backward compat`() {
        val tier = RecordingTier("test_tier")
        val engine = CorrectionEngine(listOf(tier))

        val testData = byteArrayOf(0x01, 0x02, 0x03)
        engine.onRtcmData(testData)  // No stream type specified

        assertEquals(1, tier.rtcmCalls.size, "Default must reach onRtcmData")
        assertEquals(RtklibNative.STREAM_COMBINED, tier.rtcmCalls[0].streamType, "Default is STREAM_COMBINED")
    }

    @Test
    fun `ephemeris bytes blocked from all tier onRtcmData calls with multiple tiers`() {
        val tier1 = RecordingTier("tier1")
        val tier3 = RecordingTier("tier3")
        val engine = CorrectionEngine(listOf(tier1, tier3))

        val testData = byteArrayOf(0x01, 0x02, 0x03)
        engine.onRtcmData(testData, RtklibNative.STREAM_EPHEMERIS)

        // STRUCTURAL SAFETY: ephemeris never reaches ANY tier's onRtcmData
        assertEquals(0, tier1.rtcmCalls.size, "Tier1 should never receive ephemeris via onRtcmData")
        assertEquals(0, tier3.rtcmCalls.size, "Tier3 should never receive ephemeris via onRtcmData")

        // Both tiers MUST receive via onEphemerisData
        assertEquals(1, tier1.ephCalls.size, "Tier1 must receive ephemeris via onEphemerisData")
        assertEquals(1, tier3.ephCalls.size, "Tier3 must receive ephemeris via onEphemerisData")
    }

    @Test
    fun `observation data reaches all tiers with matching stream type`() {
        val tier1 = RecordingTier("tier1")
        val tier3 = RecordingTier("tier3")
        val engine = CorrectionEngine(listOf(tier1, tier3))

        val testData = byteArrayOf(0x01, 0x02, 0x03)
        engine.onRtcmData(testData, RtklibNative.STREAM_BASE_OBS)

        // Both tiers receive observation data via onRtcmData
        assertEquals(1, tier1.rtcmCalls.size, "Tier1 must receive observation")
        assertEquals(1, tier3.rtcmCalls.size, "Tier3 must receive observation")
        assertEquals(RtklibNative.STREAM_BASE_OBS, tier1.rtcmCalls[0].streamType, "Stream type correct for tier1")
        assertEquals(RtklibNative.STREAM_BASE_OBS, tier3.rtcmCalls[0].streamType, "Stream type correct for tier3")

        // Neither tier receives via onEphemerisData
        assertEquals(0, tier1.ephCalls.size, "Tier1 should not receive observation as ephemeris")
        assertEquals(0, tier3.ephCalls.size, "Tier3 should not receive observation as ephemeris")
    }
}
