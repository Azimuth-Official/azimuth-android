package day.azimuth.observer.service.ntrip.decode

import day.azimuth.observer.service.ntrip.Rtcm3Parser
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests RTCM3 decoder against the BrandtFarms 120s capture fixture.
 * Ground truth from Phase 2 recon: 840 messages, 120 each of 7 types, 0 CRC failures.
 */
class Rtcm3DecoderTest {

    private val fixture: ByteArray by lazy {
        javaClass.getResourceAsStream("/rtcm/brandtfarms_120s.rtcm3")!!.readBytes()
    }

    private val parser = Rtcm3Parser()

    private val framedMessages by lazy { parser.parseMessage(fixture) }

    private val decoded by lazy { Rtcm3Decoder.decode(framedMessages) }

    // --- Frame count tests ---

    @Test
    fun `fixture parses to exactly 840 messages`() {
        assertEquals(840, framedMessages.size)
    }

    @Test
    fun `message type distribution matches recon inventory`() {
        val counts = framedMessages.groupBy { it.messageType }.mapValues { it.value.size }
        assertEquals(120, counts[1006], "1006 count")
        assertEquals(120, counts[1033], "1033 count")
        assertEquals(120, counts[1074], "1074 GPS MSM4 count")
        assertEquals(120, counts[1084], "1084 GLONASS MSM4 count")
        assertEquals(120, counts[1094], "1094 Galileo MSM4 count")
        assertEquals(120, counts[1124], "1124 BDS MSM4 count")
        assertEquals(120, counts[1230], "1230 count")
    }

    // --- MSM4 decode tests ---

    @Test
    fun `all MSM4 messages decode without exception`() {
        val msm4 = decoded.filterIsInstance<DecodedRtcm.Msm4>()
        assertEquals(480, msm4.size, "480 MSM4 messages (4 constellations × 120)")
    }

    @Test
    fun `MSM4 satellite count per epoch in range`() {
        decoded.filterIsInstance<DecodedRtcm.Msm4>().forEach { msm4 ->
            val obs = msm4.message.observations
            assertTrue(obs.isNotEmpty(), "MSM4 type ${msm4.messageType} has observations")
            val satCount = obs.map { it.satelliteId }.distinct().size
            assertTrue(satCount in 1..40, "Sat count $satCount out of range for type ${msm4.messageType}")
        }
    }

    @Test
    fun `MSM4 pseudoranges in valid orbital range`() {
        decoded.filterIsInstance<DecodedRtcm.Msm4>().forEach { msm4 ->
            msm4.message.observations.forEach { obs ->
                obs.pseudorangeM?.let { pr ->
                    assertTrue(
                        pr in 18.0e6..30.0e6,
                        "PR ${pr}m out of range for sat ${obs.satelliteId} sig ${obs.signalId} " +
                            "type ${msm4.messageType}"
                    )
                }
            }
        }
    }

    @Test
    fun `MSM4 CNR values in valid range`() {
        decoded.filterIsInstance<DecodedRtcm.Msm4>().forEach { msm4 ->
            msm4.message.observations.forEach { obs ->
                obs.cnrDbHz?.let { cnr ->
                    assertTrue(
                        cnr in 1.0..63.0,
                        "CNR ${cnr} dBHz out of range for sat ${obs.satelliteId}"
                    )
                }
            }
        }
    }

    @Test
    fun `MSM4 fine correction within 1ms of rough range`() {
        // |combined - rough| must be less than 1ms of light travel = 299792.458 m
        decoded.filterIsInstance<DecodedRtcm.Msm4>().forEach { msm4 ->
            msm4.message.observations.forEach { obs ->
                val pr = obs.pseudorangeM
                val rough = obs.roughRangeM
                if (pr != null && rough != null) {
                    val delta = abs(pr - rough)
                    assertTrue(
                        delta < GnssFrequencies.SPEED_OF_LIGHT_M_PER_MS,
                        "Fine correction delta ${delta}m exceeds 1ms for sat ${obs.satelliteId}"
                    )
                }
            }
        }
    }

    @Test
    fun `MSM4 constellation types match message types`() {
        decoded.filterIsInstance<DecodedRtcm.Msm4>().forEach { msm4 ->
            val expected = GnssFrequencies.constellationForMessageType(msm4.messageType)
            msm4.message.observations.forEach { obs ->
                assertEquals(expected, obs.constellationType,
                    "Constellation mismatch for type ${msm4.messageType}")
            }
        }
    }

    @Test
    fun `GLONASS MSM4 carrier frequency is null for FDMA signals`() {
        decoded.filterIsInstance<DecodedRtcm.Msm4>()
            .filter { it.messageType == 1084 }
            .forEach { msm4 ->
                msm4.message.observations.forEach { obs ->
                    if (obs.signalId in listOf(2, 3, 8, 9)) {
                        assertEquals(null, obs.carrierFrequencyHz,
                            "GLONASS FDMA signal ${obs.signalId} should have null carrier freq")
                    }
                }
            }
    }

    // --- 1006 station coordinates ---

    @Test
    fun `all 1006 messages decode`() {
        val coords = decoded.filterIsInstance<DecodedRtcm.StationCoords>()
        assertEquals(120, coords.size)
    }

    @Test
    fun `1006 ECEF within continental US`() {
        // Convert ECEF to lat/lon for sanity check
        val coord = decoded.filterIsInstance<DecodedRtcm.StationCoords>().first().coords
        val (lat, lon) = ecefToLatLon(coord.ecefXMeters, coord.ecefYMeters, coord.ecefZMeters)
        assertTrue(lat in 30.0..50.0, "Latitude $lat outside continental US range")
        assertTrue(lon in -130.0..-60.0, "Longitude $lon outside continental US range")
    }

    @Test
    fun `1006 antenna height non-negative and reasonable`() {
        decoded.filterIsInstance<DecodedRtcm.StationCoords>().forEach { sc ->
            assertTrue(sc.coords.antennaHeightMeters >= 0.0, "Negative antenna height")
            assertTrue(sc.coords.antennaHeightMeters < 100.0, "Antenna height ${sc.coords.antennaHeightMeters}m unreasonable")
        }
    }

    @Test
    fun `1006 station ID consistent across all epochs`() {
        val ids = decoded.filterIsInstance<DecodedRtcm.StationCoords>()
            .map { it.coords.stationId }.distinct()
        assertEquals(1, ids.size, "Expected single station ID, got $ids")
    }

    // --- 1230 GLONASS biases ---

    @Test
    fun `all 1230 messages decode`() {
        val biases = decoded.filterIsInstance<DecodedRtcm.GloBiases>()
        assertEquals(120, biases.size)
    }

    // --- 1033 passes through as Unknown ---

    @Test
    fun `1033 messages pass through as Unknown`() {
        val unknown1033 = decoded.filter { it is DecodedRtcm.Unknown && it.messageType == 1033 }
        assertEquals(120, unknown1033.size)
    }

    // --- Helpers ---

    private fun ecefToLatLon(x: Double, y: Double, z: Double): Pair<Double, Double> {
        val lon = Math.toDegrees(atan2(y, x))
        val p = sqrt(x * x + y * y)
        val lat = Math.toDegrees(atan2(z, p)) // simplified, good enough for sanity check
        return lat to lon
    }
}
