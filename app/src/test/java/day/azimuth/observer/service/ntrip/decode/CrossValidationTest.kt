package day.azimuth.observer.service.ntrip.decode

import day.azimuth.observer.service.ntrip.Rtcm3Parser
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cross-validates Kotlin decoder against independently-computed reference values
 * from the Python xcheck.py (which uses its own bit-level parsing, NOT a library convenience).
 *
 * Reference values hardcoded from C:\tmp\phase2_xcheck\reference.json.
 * Tolerance: pseudorange/phaserange ±1e-3 m, CNR exact at 1 dBHz integer.
 */
class CrossValidationTest {

    private val fixture: ByteArray by lazy {
        javaClass.getResourceAsStream("/rtcm/brandtfarms_120s.rtcm3")!!.readBytes()
    }
    private val parser = Rtcm3Parser()
    private val allFramed by lazy { parser.parseMessage(fixture) }
    private val allDecoded by lazy { Rtcm3Decoder.decode(allFramed) }

    // Reference: 1006 ECEF from Python xcheck
    @Test
    fun `1006 ECEF matches reference within 1mm`() {
        val coord = allDecoded.filterIsInstance<DecodedRtcm.StationCoords>().first().coords
        assertWithin(coord.ecefXMeters, -323706.9128, 1e-3, "ECEF X")
        assertWithin(coord.ecefYMeters, -4643809.5573, 1e-3, "ECEF Y")
        assertWithin(coord.ecefZMeters, 4346041.0718, 1e-3, "ECEF Z")
        assertWithin(coord.antennaHeightMeters, 0.0, 1e-3, "Antenna height")
        assertEquals(6, coord.stationId, "Station ID")
    }

    // Reference: GPS MSM4 (1074) first epoch — 22 cells, PR range check
    @Test
    fun `GPS MSM4 epoch 0 pseudoranges match reference`() {
        val gps = allDecoded.filterIsInstance<DecodedRtcm.Msm4>()
            .filter { it.messageType == 1074 }
        val epoch0 = gps[0].message

        // Python reference: 22 cells, PR range [20263828.851, 25549790.011]
        val prValues = epoch0.observations.mapNotNull { it.pseudorangeM }
        assertEquals(22, epoch0.observations.size, "GPS epoch 0 cell count")
        assertWithin(prValues.min(), 20263828.851, 1e-3, "GPS epoch 0 PR min")
        assertWithin(prValues.max(), 25549790.011, 1e-3, "GPS epoch 0 PR max")
    }

    @Test
    fun `GPS MSM4 epoch 60 pseudoranges match reference`() {
        val gps = allDecoded.filterIsInstance<DecodedRtcm.Msm4>()
            .filter { it.messageType == 1074 }
        val epoch60 = gps[60].message
        val prValues = epoch60.observations.mapNotNull { it.pseudorangeM }
        assertEquals(22, epoch60.observations.size, "GPS epoch 60 cell count")
        assertWithin(prValues.min(), 20278165.973, 1e-3, "GPS epoch 60 PR min")
        assertWithin(prValues.max(), 25541559.486, 1e-3, "GPS epoch 60 PR max")
    }

    @Test
    fun `GPS MSM4 epoch 119 pseudoranges match reference`() {
        val gps = allDecoded.filterIsInstance<DecodedRtcm.Msm4>()
            .filter { it.messageType == 1074 }
        val epoch119 = gps[119].message
        val prValues = epoch119.observations.mapNotNull { it.pseudorangeM }
        assertEquals(22, epoch119.observations.size, "GPS epoch 119 cell count")
        assertWithin(prValues.min(), 20292549.518, 1e-3, "GPS epoch 119 PR min")
        assertWithin(prValues.max(), 25533851.093, 1e-3, "GPS epoch 119 PR max")
    }

    // GLONASS
    @Test
    fun `GLONASS MSM4 epoch 0 pseudoranges match reference`() {
        val glo = allDecoded.filterIsInstance<DecodedRtcm.Msm4>()
            .filter { it.messageType == 1084 }
        val epoch0 = glo[0].message
        val prValues = epoch0.observations.mapNotNull { it.pseudorangeM }
        assertEquals(17, epoch0.observations.size, "GLONASS epoch 0 cell count")
        assertWithin(prValues.min(), 19487987.342, 1e-3, "GLONASS epoch 0 PR min")
        assertWithin(prValues.max(), 24666906.853, 1e-3, "GLONASS epoch 0 PR max")
    }

    // Galileo
    @Test
    fun `Galileo MSM4 epoch 0 pseudoranges match reference`() {
        val gal = allDecoded.filterIsInstance<DecodedRtcm.Msm4>()
            .filter { it.messageType == 1094 }
        val epoch0 = gal[0].message
        val prValues = epoch0.observations.mapNotNull { it.pseudorangeM }
        assertEquals(32, epoch0.observations.size, "Galileo epoch 0 cell count")
        assertWithin(prValues.min(), 23810077.606, 1e-3, "Galileo epoch 0 PR min")
        assertWithin(prValues.max(), 28726904.010, 1e-3, "Galileo epoch 0 PR max")
    }

    // BDS
    @Test
    fun `BDS MSM4 epoch 0 pseudoranges match reference`() {
        val bds = allDecoded.filterIsInstance<DecodedRtcm.Msm4>()
            .filter { it.messageType == 1124 }
        val epoch0 = bds[0].message
        val prValues = epoch0.observations.mapNotNull { it.pseudorangeM }
        assertEquals(18, epoch0.observations.size, "BDS epoch 0 cell count")
        assertWithin(prValues.min(), 21652129.551, 1e-3, "BDS epoch 0 PR min")
        assertWithin(prValues.max(), 26592210.093, 1e-3, "BDS epoch 0 PR max")
    }

    @Test
    fun `station ID consistent with reference`() {
        allDecoded.filterIsInstance<DecodedRtcm.Msm4>().forEach { msm4 ->
            assertEquals(6, msm4.message.stationId, "Station ID for type ${msm4.messageType}")
        }
    }

    private fun assertWithin(actual: Double, expected: Double, tolerance: Double, label: String) {
        val delta = abs(actual - expected)
        assertTrue(
            delta <= tolerance,
            "$label: expected $expected ± $tolerance, got $actual (delta=$delta)"
        )
    }
}
