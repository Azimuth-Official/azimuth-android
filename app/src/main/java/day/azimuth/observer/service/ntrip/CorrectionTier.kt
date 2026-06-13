package day.azimuth.observer.service.ntrip

interface CorrectionTier {
    val id: String
    fun isSupported(): Boolean
    fun start()
    fun stop()
    fun onRtcmData(data: ByteArray)
    fun isApplyingCorrections(): Boolean
    fun reportedAccuracyMeters(): Float?

    /** Feed ephemeris-only RTCM bytes. Default no-op (Tier1 ignores). */
    fun onEphemerisData(data: ByteArray) {}

    /**
     * Process one rover measurement epoch from Android GnssMeasurement.
     * Default no-op (Tier1 ignores). Only call when hasFullBiasNanos is true.
     */
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
    ) {}
}
