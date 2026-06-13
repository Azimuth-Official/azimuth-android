package day.azimuth.observer.service.ntrip

import android.util.Log

/**
 * JNI bridge to RTKLIB positioning engine.
 *
 * Phase 3c: real RTCM ingestion (nativeFeedRtcm), rover observation packing
 * (nativeProcessEpoch), and diagnostic status (nativeGetStatus).
 */
object RtklibNative {

    private const val TAG = "RtklibNative"

    /** Stream kind constants — match C-side STREAM_* in azimuth_ctx.h */
    const val STREAM_BASE_OBS = 1   // observation mount (nearby base station)
    const val STREAM_EPHEMERIS = 2  // ephemeris-only mount
    const val STREAM_COMBINED = 3   // single mount providing both obs + eph

    val supported: Boolean

    init {
        supported = try {
            System.loadLibrary("azimuth_rtklib_jni")
            Log.i(TAG, "libazimuth_rtklib_jni loaded successfully")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "libazimuth_rtklib_jni not available: ${e.message}")
            false
        }
    }

    /** Initialize RTK solver context. Returns opaque handle, or 0 on failure. */
    external fun nativeInit(): Long

    /** Free RTK solver context. */
    external fun nativeFree(handle: Long)

    /**
     * Feed raw RTCM bytes into the native decoder.
     *
     * @param streamKind STREAM_BASE_OBS, STREAM_EPHEMERIS, or STREAM_COMBINED
     * @return bitmask: bit 0 = obs complete, bit 1 = eph updated, bit 2 = station info
     */
    external fun nativeFeedRtcm(handle: Long, data: ByteArray, length: Int, streamKind: Int): Int

    /**
     * Process one rover measurement epoch.
     *
     * Contract: caller MUST only invoke when GnssClock.hasFullBiasNanos() is true.
     * fullBiasNanos is passed as Long (int64) for zero precision loss.
     *
     * @return NativeSolution JSON string
     */
    external fun nativeProcessEpoch(
        handle: Long,
        timeNanos: Long,
        fullBiasNanos: Long,
        biasNanos: Double,
        nMeas: Int,
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
    ): String

    /** Diagnostic status JSON (safe to call anytime after init). */
    external fun nativeGetStatus(handle: Long): String

    /* Deprecated Phase 3b stubs — kept for JNI table compatibility */
    external fun nativeProcessPoint(handle: Long, obsJson: String): String
    external fun nativeProcessRtk(handle: Long, roverJson: String, baseJson: String): String
}
