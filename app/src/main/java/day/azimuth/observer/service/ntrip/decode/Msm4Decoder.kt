package day.azimuth.observer.service.ntrip.decode

/**
 * Decodes RTCM 10403 MSM4 messages (1074, 1084, 1094, 1124).
 *
 * MSM4 layout:
 *   Header: type(12), stationId(12), epoch(30), multiMsg(1), iods(3), reserved(7),
 *           clockSteering(2), extClock(2), smoothType(1), smoothInt(3),
 *           satMask(64), sigMask(32), cellMask(Nsat*Nsig)
 *   Satellite data (per Nsat): roughRangeInt(8), roughRangeMod(10)
 *   Signal data (per Ncell): finePR(15), finePhase(22), lockTime(4), halfCycle(1), cnr(6)
 */
object Msm4Decoder {

    private const val INVALID_ROUGH_RANGE = 255
    private const val INVALID_FINE_PR = -(1 shl 14)         // -16384
    private const val INVALID_FINE_PHASE = -(1 shl 21)      // -2097152
    private const val C = GnssFrequencies.SPEED_OF_LIGHT_M_PER_MS // 299792.458 m/ms

    /**
     * Decode an MSM4 message from raw RTCM payload bytes.
     * @param messageType one of 1074, 1084, 1094, 1124
     * @param payload raw payload bytes (after RTCM3 framing, starting from the message type bits)
     */
    fun decode(messageType: Int, payload: ByteArray): Msm4Message {
        val constellationType = GnssFrequencies.constellationForMessageType(messageType)
        val reader = BitReader(payload)

        // --- Header ---
        reader.skip(12) // message type already known
        val stationId = reader.readUnsigned(12).toInt()
        val epochTime = reader.readUnsigned(30)
        val multipleMessage = reader.readBool()
        val iods = reader.readUnsigned(3).toInt()
        reader.skip(7) // reserved
        val clockSteering = reader.readUnsigned(2).toInt()
        val externalClock = reader.readUnsigned(2).toInt()
        val smoothingType = reader.readBool()
        val smoothingInterval = reader.readUnsigned(3).toInt()

        // Satellite mask (64 bits) and signal mask (32 bits)
        val (satMask, nSat) = reader.readMask(64)
        val (sigMask, nSig) = reader.readMask(32)

        val satIds = reader.maskToIndices(satMask, 64)
        val sigIds = reader.maskToIndices(sigMask, 32)

        // Cell mask: Nsat × Nsig bits — which sat/signal combinations have data
        val cellMaskBits = nSat * nSig
        val cellFlags = mutableListOf<Boolean>()
        for (i in 0 until cellMaskBits) {
            cellFlags.add(reader.readBool())
        }

        // Build cell-to-satellite/signal mapping
        data class CellIndex(val satIdx: Int, val sigIdx: Int)
        val cells = mutableListOf<CellIndex>()
        var flagIdx = 0
        for (s in 0 until nSat) {
            for (g in 0 until nSig) {
                if (cellFlags[flagIdx]) {
                    cells.add(CellIndex(s, g))
                }
                flagIdx++
            }
        }
        val nCell = cells.size

        // --- Satellite data ---
        val roughRangeInt = IntArray(nSat) { reader.readUnsigned(8).toInt() }
        val roughRangeMod = IntArray(nSat) { reader.readUnsigned(10).toInt() }

        // --- Signal (cell) data ---
        val finePR = LongArray(nCell) { reader.readSigned(15) }
        val finePhase = LongArray(nCell) { reader.readSigned(22) }
        val lockTime = IntArray(nCell) { reader.readUnsigned(4).toInt() }
        val halfCycle = BooleanArray(nCell) { reader.readBool() }
        val cnr = IntArray(nCell) { reader.readUnsigned(6).toInt() }

        // --- Combine into observations ---
        val observations = mutableListOf<Msm4Observation>()
        for (c in 0 until nCell) {
            val cell = cells[c]
            val satId = satIds[cell.satIdx]
            val sigId = sigIds[cell.sigIdx]
            val roughInt = roughRangeInt[cell.satIdx]
            val roughMod = roughRangeMod[cell.satIdx]

            // Rough range in ms: integer + modulo×2^-10
            val roughRangeValid = roughInt != INVALID_ROUGH_RANGE
            val roughMs = if (roughRangeValid) {
                roughInt.toDouble() + roughMod.toDouble() * (1.0 / 1024.0)
            } else null

            val roughRangeM = roughMs?.let { it * C }

            // Fine pseudorange: 15-bit signed, scale 2^-24 ms
            val fineValid = finePR[c].toInt() != INVALID_FINE_PR
            val pseudorangeM = if (roughMs != null && fineValid) {
                (roughMs + finePR[c].toDouble() * (1.0 / (1 shl 24).toDouble())) * C
            } else null

            // Fine phaserange: 22-bit signed, scale 2^-29 ms
            val phaseValid = finePhase[c].toInt() != INVALID_FINE_PHASE
            val phaserangeM = if (roughMs != null && phaseValid) {
                (roughMs + finePhase[c].toDouble() * (1.0 / (1 shl 29).toDouble())) * C
            } else null

            val cnrDbHz = if (cnr[c] != 0) cnr[c].toDouble() else null

            observations.add(
                Msm4Observation(
                    constellationType = constellationType,
                    satelliteId = satId,
                    signalId = sigId,
                    carrierFrequencyHz = GnssFrequencies.carrierFrequencyHz(constellationType, sigId),
                    pseudorangeM = pseudorangeM,
                    phaserangeM = phaserangeM,
                    lockTimeIndicator = lockTime[c],
                    halfCycleAmbiguity = halfCycle[c],
                    cnrDbHz = cnrDbHz,
                    roughRangeM = roughRangeM,
                )
            )
        }

        return Msm4Message(
            messageType = messageType,
            stationId = stationId,
            gnssEpochTimeRaw = epochTime,
            multipleMessage = multipleMessage,
            iods = iods,
            clockSteering = clockSteering,
            externalClock = externalClock,
            smoothingType = smoothingType,
            smoothingInterval = smoothingInterval,
            observations = observations,
        )
    }
}
