package day.azimuth.observer.service.ntrip.replay

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for RtcmReplaySource fixture loading and filtering.
 *
 * SAFE TO COMPILE AND RUN during solo phase — no Team B dependencies.
 * Tests the fixture from Phase 2 recon: 840 messages, 120 each of 7 types.
 */
class RtcmReplaySourceTest {

    private val source = RtcmReplaySource("/rtcm/brandtfarms_120s.rtcm3")

    @Test
    fun `fixture parses to exactly 840 messages`() {
        assertEquals(840, source.frameCount)
        assertEquals(840, source.frames.size)
    }

    @Test
    fun `message type distribution matches expected`() {
        val dist = source.messageTypeDistribution()
        assertEquals(120, dist[1006], "Station coordinate (1006) count")
        assertEquals(120, dist[1033], "1033 count")
        assertEquals(120, dist[1074], "GPS MSM4 (1074) count")
        assertEquals(120, dist[1084], "GLONASS MSM4 (1084) count")
        assertEquals(120, dist[1094], "Galileo MSM4 (1094) count")
        assertEquals(120, dist[1124], "BeiDou MSM4 (1124) count")
        assertEquals(120, dist[1230], "GLONASS biases (1230) count")
    }

    @Test
    fun `observationFrames returns all MSM4 messages`() {
        val obs = source.observationFrames()
        assertEquals(480, obs.size, "480 MSM4 messages (4 constellations × 120)")
        assertTrue(obs.all { it.messageType in setOf(1074, 1084, 1094, 1124) })
    }

    @Test
    fun `ephemerisFrames returns empty for this fixture`() {
        val eph = source.ephemerisFrames()
        assertEquals(0, eph.size, "BrandtFarms fixture has no ephemeris messages")
    }

    @Test
    fun `stationFrames returns all 1006 messages`() {
        val station = source.stationFrames()
        assertEquals(120, station.size, "120 station coordinate messages (1006 only)")
        assertTrue(station.all { it.messageType == 1006 })
    }

    @Test
    fun `frameCount matches frames list size`() {
        assertEquals(source.frames.size, source.frameCount)
    }

    @Test
    fun `rawBytes is a copy of the fixture`() {
        val raw1 = source.rawBytes()
        val raw2 = source.rawBytes()
        assertEquals(raw1.size, raw2.size)
        assertTrue(raw1.contentEquals(raw2), "Multiple rawBytes() calls return identical content")
    }

    @Test
    fun `framesByType filters correctly`() {
        val gpsMessages = source.framesByType(1074)
        assertEquals(120, gpsMessages.size)
        assertTrue(gpsMessages.all { it.messageType == 1074 })

        val emptyFilter = source.framesByType(9999)
        assertEquals(0, emptyFilter.size)
    }
}
