package day.azimuth.observer.service.ntrip.decode

/**
 * Typed models for decoded RTCM3 messages.
 * All range values in meters (Double). Invalid/special encodings represented as null.
 */

/** Sealed class for all decoded RTCM message types. */
sealed class DecodedRtcm {
    abstract val messageType: Int

    data class Msm4(val message: Msm4Message) : DecodedRtcm() {
        override val messageType: Int get() = message.messageType
    }

    data class StationCoords(val coords: StationCoordinates1006) : DecodedRtcm() {
        override val messageType: Int get() = 1006
    }

    data class GloBiases(val biases: GlonassBiases1230) : DecodedRtcm() {
        override val messageType: Int get() = 1230
    }

    data class Unknown(override val messageType: Int, val payload: ByteArray) : DecodedRtcm() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Unknown) return false
            return messageType == other.messageType && payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int = 31 * messageType + payload.contentHashCode()
    }
}

/**
 * MSM4 decoded message (types 1074, 1084, 1094, 1124).
 * Contains per-satellite rough range and per-cell fine observations combined into meters.
 */
data class Msm4Message(
    val messageType: Int,
    val stationId: Int,
    /** Raw GNSS epoch time from header. Interpretation is constellation-dependent:
     *  GPS/Galileo/BDS: milliseconds from beginning of GPS week.
     *  GLONASS: day-of-week (bits 29-27) + milliseconds in day (bits 26-0). */
    val gnssEpochTimeRaw: Long,
    val multipleMessage: Boolean,
    val iods: Int,
    val clockSteering: Int,
    val externalClock: Int,
    val smoothingType: Boolean,
    val smoothingInterval: Int,
    val observations: List<Msm4Observation>,
)

/**
 * Single satellite-signal observation from MSM4.
 * Pseudorange and phaserange in meters (Double). Null if invalid encoding.
 */
data class Msm4Observation(
    val constellationType: Int,
    val satelliteId: Int,
    val signalId: Int,
    /** Carrier frequency in Hz. Null for GLONASS FDMA signals (channel unknown in MSM4). */
    val carrierFrequencyHz: Double?,
    /** Full pseudorange in meters, or null if rough range invalid (255) or fine PR invalid. */
    val pseudorangeM: Double?,
    /** Full phaserange in meters, or null if rough range invalid or fine phase invalid. */
    val phaserangeM: Double?,
    /** Lock time indicator (0-15). */
    val lockTimeIndicator: Int,
    /** Half-cycle ambiguity flag. */
    val halfCycleAmbiguity: Boolean,
    /** Carrier-to-noise ratio in dBHz, or null if zero (invalid). */
    val cnrDbHz: Double?,
    /** Rough range (integer + modulo) in meters, before fine correction. For consistency checks. */
    val roughRangeM: Double?,
)

/**
 * RTCM 1006: Station coordinates (ECEF) with antenna height.
 */
data class StationCoordinates1006(
    val stationId: Int,
    val itrf: Int,
    val ecefXMeters: Double,
    val ecefYMeters: Double,
    val ecefZMeters: Double,
    val antennaHeightMeters: Double,
)

/**
 * RTCM 1230: GLONASS code-phase biases.
 */
data class GlonassBiases1230(
    val stationId: Int,
    val indicator: Boolean,
    /** Signal mask indicating which bias values follow. */
    val signalMask: Int,
    /** L1 C/A bias in meters, or null if not present. */
    val l1CaBiasM: Double?,
    /** L1 P bias in meters, or null if not present. */
    val l1PBiasM: Double?,
    /** L2 C/A bias in meters, or null if not present. */
    val l2CaBiasM: Double?,
    /** L2 P bias in meters, or null if not present. */
    val l2PBiasM: Double?,
)
