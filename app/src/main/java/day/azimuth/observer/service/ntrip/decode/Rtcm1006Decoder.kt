package day.azimuth.observer.service.ntrip.decode

/**
 * Decodes RTCM 1006: Stationary RTK Reference Station ARP with Antenna Height.
 *
 * Layout (after 12-bit message type):
 *   stationId(12), itrf(6), gps(1), glonass(1), galileo(1), referenceStation(1),
 *   ecefX(38 signed), singleReceiver(1), reserved(1),
 *   ecefY(38 signed), reserved(2),
 *   ecefZ(38 signed),
 *   antennaHeight(16 unsigned)
 *
 * ECEF values scaled by 0.0001 m. Antenna height scaled by 0.0001 m.
 */
object Rtcm1006Decoder {

    private const val ECEF_SCALE = 0.0001 // meters per LSB
    private const val HEIGHT_SCALE = 0.0001 // meters per LSB

    fun decode(payload: ByteArray): StationCoordinates1006 {
        val reader = BitReader(payload)

        reader.skip(12) // message type
        val stationId = reader.readUnsigned(12).toInt()
        val itrf = reader.readUnsigned(6).toInt()
        reader.skip(4) // GPS/GLONASS/Galileo/reference-station indicators
        val ecefX = reader.readSigned(38).toDouble() * ECEF_SCALE
        reader.skip(2) // single-receiver + reserved
        val ecefY = reader.readSigned(38).toDouble() * ECEF_SCALE
        reader.skip(2) // reserved
        val ecefZ = reader.readSigned(38).toDouble() * ECEF_SCALE
        val antennaHeight = reader.readUnsigned(16).toDouble() * HEIGHT_SCALE

        return StationCoordinates1006(
            stationId = stationId,
            itrf = itrf,
            ecefXMeters = ecefX,
            ecefYMeters = ecefY,
            ecefZMeters = ecefZ,
            antennaHeightMeters = antennaHeight,
        )
    }
}
