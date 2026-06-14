package day.azimuth.observer.service.ntrip.replay

import day.azimuth.observer.service.ntrip.Rtcm3Parser
import day.azimuth.observer.service.ntrip.Rtcm3Message

/**
 * Load an RTCM3 binary fixture from classpath, parse into individual frames,
 * and provide methods to filter by message type and analyze distributions.
 */
class RtcmReplaySource(resourcePath: String) {
    private val rawBytes: ByteArray = javaClass.getResourceAsStream(resourcePath)?.readBytes()
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

    private val parser = Rtcm3Parser()

    val frames: List<Rtcm3Message> by lazy { parser.parseMessage(rawBytes) }

    val frameCount: Int get() = frames.size

    /**
     * Returns a map of message type -> count.
     */
    fun messageTypeDistribution(): Map<Int, Int> =
        frames.groupBy { it.messageType }.mapValues { it.value.size }

    /**
     * Observation MSM messages (1074 GPS, 1084 GLONASS, 1094 Galileo, 1124 BeiDou)
     */
    fun observationFrames(): List<Rtcm3Message> =
        frames.filter { it.messageType in setOf(1074, 1084, 1094, 1124) }

    /**
     * Ephemeris messages (1019 GPS, 1020 GLONASS, 1042 BeiDou, 1045/1046 Galileo)
     */
    fun ephemerisFrames(): List<Rtcm3Message> =
        frames.filter { it.messageType in setOf(1019, 1020, 1042, 1045, 1046) }

    /**
     * Station coordinate messages (1005, 1006)
     */
    fun stationFrames(): List<Rtcm3Message> =
        frames.filter { it.messageType in setOf(1005, 1006) }

    /**
     * Get raw bytes for the entire fixture.
     * Safe to feed directly to CorrectionEngine.onRtcmData() — the native decoder handles frame sync.
     */
    fun rawBytes(): ByteArray = rawBytes.copyOf()

    /**
     * Get frames filtered by message type.
     */
    fun framesByType(messageType: Int): List<Rtcm3Message> =
        frames.filter { it.messageType == messageType }
}
