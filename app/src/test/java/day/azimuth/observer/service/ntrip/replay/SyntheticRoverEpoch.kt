package day.azimuth.observer.service.ntrip.replay

/**
 * Synthetic rover epoch for testing the correction pipeline.
 * Generates plausible GNSS measurement arrays without requiring Android framework.
 *
 * Fields match the signature of CorrectionTier.processRoverEpoch() and RtklibNative.nativeProcessEpoch().
 */
data class SyntheticRoverEpoch(
    val timeNanos: Long,
    val fullBiasNanos: Long,
    val biasNanos: Double,
    val svids: IntArray,
    val constellationTypes: IntArray,
    val states: IntArray,
    val receivedSvTimeNanos: LongArray,
    val timeOffsetNanos: DoubleArray,
    val cn0DbHz: DoubleArray,
    val carrierFreqHz: DoubleArray,
    val pseudorangeRateMps: DoubleArray,
    val adrMeters: DoubleArray,
    val adrStates: IntArray,
) {
    companion object {
        // Android GnssStatus constellation constants (mirrors android.location.GnssStatus)
        const val CONSTELLATION_GPS = 1
        const val CONSTELLATION_SBAS = 2
        const val CONSTELLATION_GLONASS = 3
        const val CONSTELLATION_QZSS = 4
        const val CONSTELLATION_BEIDOU = 5
        const val CONSTELLATION_GALILEO = 6
        const val CONSTELLATION_IRNSS = 7

        // Measurement state flags (from Android GnssMeasurement)
        const val STATE_CODE_LOCK = 1
        const val STATE_BIT_SYNC = 2
        const val STATE_SUBFRAME_SYNC = 4
        const val STATE_TOW_DECODED = 8
        const val STATE_MSEC_AMBIGUOUS = 16

        /**
         * Create a synthetic rover epoch with specified parameters.
         * All measurements share the same constellation and reasonable defaults.
         */
        fun create(
            satCount: Int = 8,
            constellation: Int = CONSTELLATION_GPS,
            timeNanos: Long = 1_000_000_000_000L,
            fullBiasNanos: Long = -1_262_304_000_000_000_000L, // approximate GPS epoch offset
        ): SyntheticRoverEpoch {
            require(satCount > 0) { "satCount must be > 0" }
            require(satCount <= 128) { "satCount must be <= 128" }

            val svids = IntArray(satCount) { it + 1 }
            val constTypes = IntArray(satCount) { constellation }
            val states = IntArray(satCount) { STATE_CODE_LOCK or STATE_TOW_DECODED }
            val recvSvTime = LongArray(satCount) { 80_000_000_000L + it * 1_000_000L }
            val timeOffset = DoubleArray(satCount) { 0.0 }
            val cn0 = DoubleArray(satCount) { 35.0 + it * 0.5 } // 35-39.5 dBHz range
            val carrierFreq = DoubleArray(satCount) { 1575.42e6 } // GPS L1, constant
            val prRate = DoubleArray(satCount) { -500.0 + it * 10.0 } // -500 to -420 m/s
            val adrM = DoubleArray(satCount) { 100_000.0 + it * 100.0 } // 100km to 100.7km
            val adrSt = IntArray(satCount) { 1 } // ADR valid

            return SyntheticRoverEpoch(
                timeNanos = timeNanos,
                fullBiasNanos = fullBiasNanos,
                biasNanos = 0.0,
                svids = svids,
                constellationTypes = constTypes,
                states = states,
                receivedSvTimeNanos = recvSvTime,
                timeOffsetNanos = timeOffset,
                cn0DbHz = cn0,
                carrierFreqHz = carrierFreq,
                pseudorangeRateMps = prRate,
                adrMeters = adrM,
                adrStates = adrSt,
            )
        }

        /**
         * Create a weak-signal epoch with low CN0 (useful for edge cases).
         */
        fun createWeakSignal(
            satCount: Int = 4,
            constellation: Int = CONSTELLATION_GPS,
        ): SyntheticRoverEpoch {
            val base = create(satCount, constellation)
            return base.copy(
                cn0DbHz = DoubleArray(satCount) { 20.0 + it * 2.0 } // 20-26 dBHz
            )
        }

        /**
         * Create an epoch with multi-constellation satellites.
         */
        fun createMultiConstellation(): SyntheticRoverEpoch {
            val gpsCount = 4
            val gloCount = 3
            val galCount = 3
            val totalCount = gpsCount + gloCount + galCount

            val svids = IntArray(totalCount) { i ->
                when {
                    i < gpsCount -> i + 1
                    i < gpsCount + gloCount -> i - gpsCount + 1
                    else -> i - gpsCount - gloCount + 1
                }
            }

            val constellations = IntArray(totalCount) { i ->
                when {
                    i < gpsCount -> CONSTELLATION_GPS
                    i < gpsCount + gloCount -> CONSTELLATION_GLONASS
                    else -> CONSTELLATION_GALILEO
                }
            }

            val states = IntArray(totalCount) { STATE_CODE_LOCK or STATE_TOW_DECODED }
            val recvSvTime = LongArray(totalCount) { 80_000_000_000L + it * 1_000_000L }
            val timeOffset = DoubleArray(totalCount) { 0.0 }
            val cn0 = DoubleArray(totalCount) { 35.0 + it * 0.5 }
            val carrierFreq = DoubleArray(totalCount) { 1575.42e6 }
            val prRate = DoubleArray(totalCount) { -500.0 + it * 10.0 }
            val adrM = DoubleArray(totalCount) { 100_000.0 + it * 100.0 }
            val adrSt = IntArray(totalCount) { 1 }

            return SyntheticRoverEpoch(
                timeNanos = 1_000_000_000_000L,
                fullBiasNanos = -1_262_304_000_000_000_000L,
                biasNanos = 0.0,
                svids = svids,
                constellationTypes = constellations,
                states = states,
                receivedSvTimeNanos = recvSvTime,
                timeOffsetNanos = timeOffset,
                cn0DbHz = cn0,
                carrierFreqHz = carrierFreq,
                pseudorangeRateMps = prRate,
                adrMeters = adrM,
                adrStates = adrSt,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyntheticRoverEpoch) return false
        return timeNanos == other.timeNanos && fullBiasNanos == other.fullBiasNanos
    }

    override fun hashCode(): Int = timeNanos.hashCode() xor fullBiasNanos.hashCode()
}
