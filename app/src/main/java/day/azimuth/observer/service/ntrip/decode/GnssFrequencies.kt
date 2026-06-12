package day.azimuth.observer.service.ntrip.decode

/**
 * GNSS signal ID to carrier frequency mapping per RTCM 10403 MSM signal tables.
 * GLONASS FDMA frequencies return null — channel number is only in MSM5/MSM7.
 */
object GnssFrequencies {

    // Constellation type constants matching GnssStatus.CONSTELLATION_*
    const val CONSTELLATION_GPS = 1
    const val CONSTELLATION_GLONASS = 3
    const val CONSTELLATION_GALILEO = 6
    const val CONSTELLATION_BDS = 5

    /** Speed of light in m/ms (for range conversion: ms of light travel → meters). */
    const val SPEED_OF_LIGHT_M_PER_MS = 299792.458

    /**
     * Get carrier frequency for a constellation + MSM signal ID.
     * Returns null for GLONASS FDMA signals (G1/G2) where channel is needed.
     *
     * MSM signal IDs are 1-based per the RTCM signal mask bit position.
     */
    fun carrierFrequencyHz(constellationType: Int, signalId: Int): Double? {
        return when (constellationType) {
            CONSTELLATION_GPS -> gpsFrequency(signalId)
            CONSTELLATION_GLONASS -> glonassFrequency(signalId)
            CONSTELLATION_GALILEO -> galileoFrequency(signalId)
            CONSTELLATION_BDS -> bdsFrequency(signalId)
            else -> null
        }
    }

    /**
     * Map MSM message type to constellation type.
     */
    fun constellationForMessageType(messageType: Int): Int = when {
        messageType in 1071..1077 -> CONSTELLATION_GPS
        messageType in 1081..1087 -> CONSTELLATION_GLONASS
        messageType in 1091..1097 -> CONSTELLATION_GALILEO
        messageType in 1121..1127 -> CONSTELLATION_BDS
        else -> 0
    }

    // --- GPS MSM signal table (DF395) ---
    // ID 2: L1 C/A, ID 15: L2CM, ID 16: L2CL, ID 22: L5I, ID 23: L5Q
    private fun gpsFrequency(signalId: Int): Double? = when (signalId) {
        2 -> 1575.42e6       // L1 C/A
        3 -> 1575.42e6       // L1 P
        4 -> 1575.42e6       // L1 Z-tracking
        8 -> 1227.60e6       // L2 CM
        9 -> 1227.60e6       // L2 CL
        10 -> 1227.60e6      // L2 CM+CL
        11 -> 1227.60e6      // L2 P
        12 -> 1227.60e6      // L2 Z-tracking
        15 -> 1227.60e6      // L2 CM (alt mapping)
        16 -> 1227.60e6      // L2 CL (alt mapping)
        17 -> 1227.60e6      // L2 CM+CL (alt mapping)
        22 -> 1176.45e6      // L5 I
        23 -> 1176.45e6      // L5 Q
        24 -> 1176.45e6      // L5 I+Q
        else -> null
    }

    // --- GLONASS MSM signal table (DF395) ---
    // FDMA: G1 C/A (ID 2), G2 C/A (ID 8) — frequency = base + k × step, k unknown in MSM4.
    // CDMA: L3 (ID 16) = fixed 1202.025 MHz.
    private fun glonassFrequency(signalId: Int): Double? = when (signalId) {
        2 -> null   // G1 C/A: 1602.0 + k*0.5625 MHz — channel k unknown in MSM4
        3 -> null   // G1 P: same FDMA dependency
        8 -> null   // G2 C/A: 1246.0 + k*0.4375 MHz — channel k unknown in MSM4
        9 -> null   // G2 P: same FDMA dependency
        16 -> 1202.025e6 // L3 CDMA (fixed frequency, no channel dependency)
        else -> null
    }

    // --- Galileo MSM signal table (DF395) ---
    private fun galileoFrequency(signalId: Int): Double? = when (signalId) {
        2 -> 1575.42e6       // E1 C
        3 -> 1575.42e6       // E1 A
        4 -> 1575.42e6       // E1 B
        5 -> 1575.42e6       // E1 B+C
        6 -> 1575.42e6       // E1 A+B+C
        8 -> 1176.45e6       // E5a I
        9 -> 1176.45e6       // E5a Q
        10 -> 1176.45e6      // E5a I+Q
        14 -> 1207.14e6      // E5b I
        15 -> 1207.14e6      // E5b Q
        16 -> 1207.14e6      // E5b I+Q
        18 -> 1191.795e6     // E5 (a+b) I
        19 -> 1191.795e6     // E5 (a+b) Q
        20 -> 1191.795e6     // E5 (a+b) I+Q
        22 -> 1278.75e6      // E6 B
        23 -> 1278.75e6      // E6 C
        24 -> 1278.75e6      // E6 B+C
        else -> null
    }

    // --- BDS MSM signal table (DF395) ---
    private fun bdsFrequency(signalId: Int): Double? = when (signalId) {
        2 -> 1561.098e6      // B1I (B1-2 I)
        3 -> 1561.098e6      // B1I Q
        4 -> 1561.098e6      // B1I I+Q
        8 -> 1207.14e6       // B2I (B3 I)
        9 -> 1207.14e6       // B2I Q
        10 -> 1207.14e6      // B2I I+Q
        14 -> 1268.52e6      // B3I
        15 -> 1268.52e6      // B3I Q
        16 -> 1268.52e6      // B3I I+Q
        22 -> 1575.42e6      // B1C (pilot)
        23 -> 1575.42e6      // B1C (data)
        24 -> 1575.42e6      // B1C (data+pilot)
        30 -> 1176.45e6      // B2a (pilot)
        31 -> 1176.45e6      // B2a (data)
        32 -> 1176.45e6      // B2a (data+pilot)
        else -> null
    }
}
