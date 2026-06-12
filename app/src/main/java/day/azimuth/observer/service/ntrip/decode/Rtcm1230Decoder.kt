package day.azimuth.observer.service.ntrip.decode

/**
 * Decodes RTCM 1230: GLONASS L1 and L2 Code-Phase Biases.
 *
 * Layout (after 12-bit message type):
 *   stationId(12), indicator(1), reserved(3), signalMask(4),
 *   then for each set bit in signalMask: codePhaseBias(16 signed, scale 0.02 m)
 *
 * Signal mask bits (MSB first): L1 C/A, L1 P, L2 C/A, L2 P
 */
object Rtcm1230Decoder {

    private const val BIAS_SCALE = 0.02 // meters per LSB

    fun decode(payload: ByteArray): GlonassBiases1230 {
        val reader = BitReader(payload)

        reader.skip(12) // message type
        val stationId = reader.readUnsigned(12).toInt()
        val indicator = reader.readBool()
        reader.skip(3) // reserved
        val signalMask = reader.readUnsigned(4).toInt()

        // Read biases for each set bit in mask (MSB = L1 C/A, LSB = L2 P)
        val l1CaBias = if (signalMask and 0x08 != 0) reader.readSigned(16).toDouble() * BIAS_SCALE else null
        val l1PBias = if (signalMask and 0x04 != 0) reader.readSigned(16).toDouble() * BIAS_SCALE else null
        val l2CaBias = if (signalMask and 0x02 != 0) reader.readSigned(16).toDouble() * BIAS_SCALE else null
        val l2PBias = if (signalMask and 0x01 != 0) reader.readSigned(16).toDouble() * BIAS_SCALE else null

        return GlonassBiases1230(
            stationId = stationId,
            indicator = indicator,
            signalMask = signalMask,
            l1CaBiasM = l1CaBias,
            l1PBiasM = l1PBias,
            l2CaBiasM = l2CaBias,
            l2PBiasM = l2PBias,
        )
    }
}
