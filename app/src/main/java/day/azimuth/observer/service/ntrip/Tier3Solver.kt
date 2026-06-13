package day.azimuth.observer.service.ntrip

import android.util.Log

/**
 * Tier 3: RTKLIB-backed software RTK solver (Phase 3c).
 *
 * Feeds raw RTCM bytes to the native decoder for base observations and
 * ephemeris, packs Android GnssMeasurement epochs as rover observations,
 * and calls rtkpos() for positioning.
 *
 * isApplyingCorrections() returns true ONLY for differential solutions
 * (SOLQ_FIX or SOLQ_FLOAT) — never for single-point.
 */
class Tier3Solver : CorrectionTier {

    private companion object {
        const val TAG = "Tier3Solver"
    }

    override val id: String = "tier3_rtklib"

    private var nativeHandle: Long = 0L
    private var lastSolution: NativeSolution? = null

    override fun isSupported(): Boolean = RtklibNative.supported

    override fun start() {
        if (!RtklibNative.supported) {
            Log.w(TAG, "start: native library not available, skipping")
            return
        }
        if (nativeHandle != 0L) {
            Log.w(TAG, "start: already initialized, skipping")
            return
        }
        nativeHandle = RtklibNative.nativeInit()
        if (nativeHandle == 0L) {
            Log.e(TAG, "start: nativeInit returned null handle")
        } else {
            Log.i(TAG, "start: RTK solver context initialized (Phase 3c)")
        }
    }

    override fun stop() {
        if (nativeHandle != 0L) {
            RtklibNative.nativeFree(nativeHandle)
            nativeHandle = 0L
            lastSolution = null
            Log.i(TAG, "stop: RTK solver context released")
        }
    }

    override fun onRtcmData(data: ByteArray) {
        if (nativeHandle == 0L) return
        RtklibNative.nativeFeedRtcm(
            nativeHandle, data, data.size, RtklibNative.STREAM_COMBINED,
        )
    }

    override fun onEphemerisData(data: ByteArray) {
        if (nativeHandle == 0L) return
        RtklibNative.nativeFeedRtcm(
            nativeHandle, data, data.size, RtklibNative.STREAM_EPHEMERIS,
        )
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
        if (nativeHandle == 0L) return
        val json = RtklibNative.nativeProcessEpoch(
            nativeHandle,
            timeNanos, fullBiasNanos, biasNanos,
            svids.size,
            svids, constellationTypes, states,
            receivedSvTimeNanos, timeOffsetNanos, cn0DbHz,
            carrierFreqHz, pseudorangeRateMps, adrMeters, adrStates,
        )
        lastSolution = NativeSolution.fromJson(json)
    }

    override fun isApplyingCorrections(): Boolean {
        // ONLY true for differential RTK solutions (SOLQ_FIX or SOLQ_FLOAT).
        // SOLQ_SINGLE is NOT a correction — it means no base data was used.
        val sol = lastSolution ?: return false
        return sol.hasFix && sol.satelliteCount >= 4
    }

    override fun reportedAccuracyMeters(): Float? {
        // Only report accuracy when actually applying differential corrections.
        if (!isApplyingCorrections()) return null
        return lastSolution?.accuracyMeters
    }

    /** Diagnostic status from native solver. Safe to call anytime after start(). */
    fun getStatus(): String? {
        if (nativeHandle == 0L) return null
        return RtklibNative.nativeGetStatus(nativeHandle)
    }
}
