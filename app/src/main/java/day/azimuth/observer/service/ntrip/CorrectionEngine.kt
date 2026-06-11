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

    fun onRtcmData(data: ByteArray) {
        // Unconditional fan-out — each tier handles its own support checks internally.
        // Gating here would prevent chipsetSupported from being set via reflection.
        tiers.forEach { it.onRtcmData(data) }
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
