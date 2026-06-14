package day.azimuth.observer.service.ntrip

import android.util.Log

class CorrectionEngine(private val tiers: List<CorrectionTier>) {

    fun start() {
        Log.i(TAG, "CorrectionEngine starting with ${tiers.size} tier(s): ${tiers.map { it.id }}")
        tiers.forEach { it.start() }
    }

    fun stop() {
        tiers.forEach { it.stop() }
        Log.i(TAG, "CorrectionEngine stopped")
    }

    fun onRtcmData(data: ByteArray, streamType: Int = RtklibNative.STREAM_COMBINED) {
        when (streamType) {
            RtklibNative.STREAM_EPHEMERIS -> {
                // Ephemeris ONLY → route through onEphemerisData (Tier1 = no-op, Tier3 feeds eph decoder)
                // SAFETY: ephemeris bytes never reach tier.onRtcmData() — structurally blocked
                tiers.forEach { it.onEphemerisData(data) }
            }
            else -> {
                // STREAM_BASE_OBS or STREAM_COMBINED → fan-out to all tiers via onRtcmData
                // Unconditional fan-out — each tier handles its own support checks internally.
                // Gating here would prevent chipsetSupported from being set via reflection.
                tiers.forEach { it.onRtcmData(data, streamType) }
            }
        }
    }

    fun onEphemerisData(data: ByteArray) {
        // Fan-out ephemeris bytes — Tier1 ignores via default no-op.
        tiers.forEach { it.onEphemerisData(data) }
    }

    fun processRoverEpoch(
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
        tiers.forEach {
            it.processRoverEpoch(
                timeNanos, fullBiasNanos, biasNanos,
                svids, constellationTypes, states,
                receivedSvTimeNanos, timeOffsetNanos, cn0DbHz,
                carrierFreqHz, pseudorangeRateMps, adrMeters, adrStates,
            )
        }
    }

    fun activeTier(): CorrectionTier? = tiers.firstOrNull { it.isApplyingCorrections() }

    fun isAnyTierApplyingCorrections(): Boolean = tiers.any { it.isApplyingCorrections() }

    // TODO: Phase 2 — register Tier2MockLocation
    // TODO: Phase 3 — register Tier3ExternalRadio
    // TODO: Phase 4-5 — AccuracyComparator for tier switching

    companion object {
        private const val TAG = "CorrectionEngine"
    }
}
