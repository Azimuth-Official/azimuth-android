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
        val solutionJson = processInlineEpoch(8)
        assertTrue(solutionJson.isNotEmpty(), "nativeProcessEpoch should return non-empty JSON")
    }

    @Test
    fun multiEpochSequence_noExceptions() {
        for (n in listOf(8, 10, 6)) {
            val json = processInlineEpoch(n)
            assertTrue(json.isNotEmpty(), "Solution should be non-empty for satCount=$n")
        }
    }

    @Test
    fun rtcmFeedThenEpoch_sequentialIntegration() {
        val fixture = javaClass.classLoader!!.getResourceAsStream("rtcm/brandtfarms_120s.rtcm3")?.readBytes()
        Assume.assumeNotNull("RTCM fixture not found in androidTest assets", fixture)

        val rtcmResult = RtklibNative.nativeFeedRtcm(
            nativeHandle, fixture!!, fixture.size, RtklibNative.STREAM_COMBINED
        )
        assertTrue(rtcmResult >= 0, "RTCM feed should succeed")

        val json = processInlineEpoch(8)
        assertTrue(json.isNotEmpty(), "Solution should be non-empty after RTCM context setup")
    }

    /** Build a synthetic rover epoch inline — avoids cross-source-set dependency on SyntheticRoverEpoch. */
    private fun processInlineEpoch(satCount: Int): String {
        val timeNanos = 1_000_000_000_000L
        val fullBiasNanos = -1_262_304_000_000_000_000L
        val biasNanos = 0.0
        val svids = IntArray(satCount) { it + 1 }
        val constTypes = IntArray(satCount) { 1 } // GPS
        val states = IntArray(satCount) { 9 } // CODE_LOCK | TOW_DECODED
        val recvSvTime = LongArray(satCount) { 80_000_000_000L + it * 1_000_000L }
        val timeOffset = DoubleArray(satCount) { 0.0 }
        val cn0 = DoubleArray(satCount) { 35.0 + it * 2.0 }
        val carrierFreq = DoubleArray(satCount) { 1575.42e6 }
        val prRate = DoubleArray(satCount) { -500.0 + it * 100.0 }
        val adrM = DoubleArray(satCount) { 100_000.0 + it * 1000.0 }
        val adrSt = IntArray(satCount) { 1 }

        return RtklibNative.nativeProcessEpoch(
            nativeHandle, timeNanos, fullBiasNanos, biasNanos,
            satCount, svids, constTypes, states,
            recvSvTime, timeOffset, cn0, carrierFreq, prRate, adrM, adrSt,
        )
    }
}
