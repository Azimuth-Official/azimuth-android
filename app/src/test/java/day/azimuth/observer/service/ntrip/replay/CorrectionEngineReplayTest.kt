package day.azimuth.observer.service.ntrip.replay

import day.azimuth.observer.service.ntrip.CorrectionEngine
import day.azimuth.observer.service.ntrip.RtklibNative
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for CorrectionEngine with RTCM replay and synthetic rover epochs.
 *
 * Tests the NEW CorrectionEngine.onRtcmData(data, streamType) signature.
 * Uses MockCorrectionTier to verify fan-out behavior and stream type propagation.
 */
class CorrectionEngineReplayTest {

    private val source = RtcmReplaySource("/rtcm/brandtfarms_120s.rtcm3")
    private val tier1 = MockCorrectionTier("tier1")
    private val tier2 = MockCorrectionTier("tier2")
    private val engine = CorrectionEngine(listOf(tier1, tier2))

    @Before
    fun setUp() {
        tier1.reset()
        tier2.reset()
    }

    @Test
    fun `feedRtcmData fans out to all tiers with STREAM_COMBINED`() {
        val rtcmBytes = source.rawBytes()

        engine.onRtcmData(rtcmBytes, RtklibNative.STREAM_COMBINED)

        assertEquals(1, tier1.rtcmCalls.size)
        assertEquals(1, tier2.rtcmCalls.size)
        assertEquals(rtcmBytes.size, tier1.rtcmCalls[0].dataSize)
        assertEquals(rtcmBytes.size, tier2.rtcmCalls[0].dataSize)
        assertEquals(RtklibNative.STREAM_COMBINED, tier1.rtcmCalls[0].streamType)
        assertEquals(RtklibNative.STREAM_COMBINED, tier2.rtcmCalls[0].streamType)
    }

    @Test
    fun `feedRtcmData with STREAM_BASE_OBS propagates correct streamType`() {
        val rtcmBytes = source.rawBytes()

        engine.onRtcmData(rtcmBytes, RtklibNative.STREAM_BASE_OBS)

        assertEquals(RtklibNative.STREAM_BASE_OBS, tier1.rtcmCalls[0].streamType)
        assertEquals(RtklibNative.STREAM_BASE_OBS, tier2.rtcmCalls[0].streamType)
    }

    @Test
    fun `processRoverEpoch fans out to all tiers`() {
        val epoch = SyntheticRoverEpoch.create(satCount = 8)

        engine.processRoverEpoch(
            epoch.timeNanos, epoch.fullBiasNanos, epoch.biasNanos,
            epoch.svids, epoch.constellationTypes, epoch.states,
            epoch.receivedSvTimeNanos, epoch.timeOffsetNanos, epoch.cn0DbHz,
            epoch.carrierFreqHz, epoch.pseudorangeRateMps, epoch.adrMeters, epoch.adrStates,
        )

        assertEquals(1, tier1.roverCallCount)
        assertEquals(1, tier2.roverCallCount)
    }

    @Test
    fun `processRoverEpoch multiple times increments both tiers`() {
        val epoch = SyntheticRoverEpoch.create()

        repeat(5) {
            engine.processRoverEpoch(
                epoch.timeNanos, epoch.fullBiasNanos, epoch.biasNanos,
                epoch.svids, epoch.constellationTypes, epoch.states,
                epoch.receivedSvTimeNanos, epoch.timeOffsetNanos, epoch.cn0DbHz,
                epoch.carrierFreqHz, epoch.pseudorangeRateMps, epoch.adrMeters, epoch.adrStates,
            )
        }

        assertEquals(5, tier1.roverCallCount)
        assertEquals(5, tier2.roverCallCount)
    }

    @Test
    fun `onEphemerisData fans out to all tiers`() {
        val ephData = byteArrayOf(0xD3.toByte(), 0x00, 0x50) // Dummy RTCM frame header
        engine.onEphemerisData(ephData)

        assertEquals(1, tier1.ephCalls.size)
        assertEquals(1, tier2.ephCalls.size)
        assertEquals(ephData.size, tier1.ephCalls[0])
        assertEquals(ephData.size, tier2.ephCalls[0])
    }

    @Test
    fun `sequential RTCM and epoch processing`() {
        val rtcmBytes = source.rawBytes()
        val epoch = SyntheticRoverEpoch.create()

        engine.onRtcmData(rtcmBytes, RtklibNative.STREAM_COMBINED)
        engine.processRoverEpoch(
            epoch.timeNanos, epoch.fullBiasNanos, epoch.biasNanos,
            epoch.svids, epoch.constellationTypes, epoch.states,
            epoch.receivedSvTimeNanos, epoch.timeOffsetNanos, epoch.cn0DbHz,
            epoch.carrierFreqHz, epoch.pseudorangeRateMps, epoch.adrMeters, epoch.adrStates,
        )

        assertEquals(1, tier1.rtcmCalls.size)
        assertEquals(1, tier1.roverCallCount)
        assertEquals(1, tier2.rtcmCalls.size)
        assertEquals(1, tier2.roverCallCount)
    }

    @Test
    fun `multi-constellation epoch processed correctly`() {
        val epoch = SyntheticRoverEpoch.createMultiConstellation()

        engine.processRoverEpoch(
            epoch.timeNanos, epoch.fullBiasNanos, epoch.biasNanos,
            epoch.svids, epoch.constellationTypes, epoch.states,
            epoch.receivedSvTimeNanos, epoch.timeOffsetNanos, epoch.cn0DbHz,
            epoch.carrierFreqHz, epoch.pseudorangeRateMps, epoch.adrMeters, epoch.adrStates,
        )

        assertEquals(1, tier1.roverCallCount)
        assertEquals(1, tier2.roverCallCount)
    }
}
