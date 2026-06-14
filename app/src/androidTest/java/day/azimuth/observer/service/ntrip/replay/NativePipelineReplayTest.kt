package day.azimuth.observer.service.ntrip.replay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import day.azimuth.observer.service.ntrip.RtklibNative
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Instrumented test scaffold for full native RTKLIB pipeline replay.
 * Requires Android device/emulator with native library loaded.
 * Marked @LargeTest — excluded from normal CI runs.
 *
 * SCAFFOLD ONLY: Tests are NOT run during initial build, only written for future use.
 * Enable and run at integration phase when native library is present on test device.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NativePipelineReplayTest {

    private var nativeHandle: Long = 0L

    @Before
    fun setUp() {
        Assume.assumeTrue("Native library not available", RtklibNative.supported)
        nativeHandle = RtklibNative.nativeInit()
        Assume.assumeTrue("nativeInit returned null handle", nativeHandle != 0L)
    }

    @After
    fun tearDown() {
        if (nativeHandle != 0L) {
            RtklibNative.nativeFree(nativeHandle)
        }
    }

    @Test
    fun nativeInit_returnsValidHandle() {
        assertTrue(nativeHandle != 0L, "nativeHandle should be non-zero")
    }

    @Test
    fun nativeGetStatus_returnsNonEmptyJson() {
        val status = RtklibNative.nativeGetStatus(nativeHandle)
        assertTrue(status.isNotEmpty(), "Status JSON should be non-empty after init")
    }

    @Test
    fun feedCombinedRtcm_accumulatesStatus() {
        // Load fixture from androidTest assets
        val fixture = javaClass.classLoader!!.getResourceAsStream("rtcm/brandtfarms_120s.rtcm3")?.readBytes()
        Assume.assumeNotNull("RTCM fixture not found in androidTest assets", fixture)

        // Feed entire fixture as STREAM_COMBINED
        val result = RtklibNative.nativeFeedRtcm(
            nativeHandle, fixture!!, fixture.size, RtklibNative.STREAM_COMBINED
        )

        assertTrue(result >= 0, "nativeFeedRtcm should return non-negative bitmask")

        // Check status after feeding
        val statusJson = RtklibNative.nativeGetStatus(nativeHandle)
        assertTrue(statusJson.isNotEmpty(), "Status should be non-empty after feeding RTCM data")
    }

    @Test
    fun feedBaseObservations_propagatesCorrectly() {
        val fixture = javaClass.classLoader!!.getResourceAsStream("rtcm/brandtfarms_120s.rtcm3")?.readBytes()
        Assume.assumeNotNull("RTCM fixture not found in androidTest assets", fixture)

        val result = RtklibNative.nativeFeedRtcm(
            nativeHandle, fixture!!, fixture.size, RtklibNative.STREAM_BASE_OBS
        )

        assertTrue(result >= 0, "nativeFeedRtcm should succeed with STREAM_BASE_OBS")
    }

    @Test
    fun processRoverEpoch_returnsValidSolutionJson() {
        val epoch = SyntheticRoverEpoch.create(satCount = 8)

        val solutionJson = RtklibNative.nativeProcessEpoch(
            nativeHandle,
            epoch.timeNanos, epoch.fullBiasNanos, epoch.biasNanos,
            epoch.svids.size,
            epoch.svids, epoch.constellationTypes, epoch.states,
            epoch.receivedSvTimeNanos, epoch.timeOffsetNanos, epoch.cn0DbHz,
            epoch.carrierFreqHz, epoch.pseudorangeRateMps, epoch.adrMeters, epoch.adrStates,
        )

        assertTrue(solutionJson.isNotEmpty(), "nativeProcessEpoch should return non-empty JSON")
    }

    @Test
    fun multiEpochSequence_noExceptions() {
        val epochs = listOf(
            SyntheticRoverEpoch.create(satCount = 8),
            SyntheticRoverEpoch.create(satCount = 10),
            SyntheticRoverEpoch.create(satCount = 6),
        )

        epochs.forEach { epoch ->
            val solutionJson = RtklibNative.nativeProcessEpoch(
                nativeHandle,
                epoch.timeNanos, epoch.fullBiasNanos, epoch.biasNanos,
                epoch.svids.size,
                epoch.svids, epoch.constellationTypes, epoch.states,
                epoch.receivedSvTimeNanos, epoch.timeOffsetNanos, epoch.cn0DbHz,
                epoch.carrierFreqHz, epoch.pseudorangeRateMps, epoch.adrMeters, epoch.adrStates,
            )
            assertTrue(solutionJson.isNotEmpty(), "Solution should be non-empty for all epochs")
        }
    }

    @Test
    fun rtcmFeedThenEpoch_sequentialIntegration() {
        val fixture = javaClass.classLoader!!.getResourceAsStream("rtcm/brandtfarms_120s.rtcm3")?.readBytes()
        Assume.assumeNotNull("RTCM fixture not found in androidTest assets", fixture)

        // Feed RTCM first to populate base station state
        val rtcmResult = RtklibNative.nativeFeedRtcm(
            nativeHandle, fixture!!, fixture.size, RtklibNative.STREAM_COMBINED
        )
        assertTrue(rtcmResult >= 0, "RTCM feed should succeed")

        // Then process a rover epoch
        val epoch = SyntheticRoverEpoch.create()
        val solutionJson = RtklibNative.nativeProcessEpoch(
            nativeHandle,
            epoch.timeNanos, epoch.fullBiasNanos, epoch.biasNanos,
            epoch.svids.size,
            epoch.svids, epoch.constellationTypes, epoch.states,
            epoch.receivedSvTimeNanos, epoch.timeOffsetNanos, epoch.cn0DbHz,
            epoch.carrierFreqHz, epoch.pseudorangeRateMps, epoch.adrMeters, epoch.adrStates,
        )
        assertTrue(solutionJson.isNotEmpty(), "Solution should be non-empty after RTCM context setup")
    }
}
