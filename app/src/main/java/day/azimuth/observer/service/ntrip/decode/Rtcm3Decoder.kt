package day.azimuth.observer.service.ntrip.decode

import day.azimuth.observer.service.ntrip.Rtcm3Message

/**
 * Facade decoder: routes framed RTCM3 messages to type-specific decoders.
 * Unknown message types pass through as DecodedRtcm.Unknown.
 *
 * NOT referenced in app runtime code — decode pipeline is unwired pending Phase 3.
 */
object Rtcm3Decoder {

    private val MSM4_TYPES = setOf(1074, 1084, 1094, 1124)

    /**
     * Decode a list of framed RTCM3 messages into typed models.
     */
    fun decode(messages: List<Rtcm3Message>): List<DecodedRtcm> {
        return messages.map { msg ->
            try {
                when (msg.messageType) {
                    in MSM4_TYPES -> DecodedRtcm.Msm4(
                        Msm4Decoder.decode(msg.messageType, msg.payload)
                    )
                    1006 -> DecodedRtcm.StationCoords(
                        Rtcm1006Decoder.decode(msg.payload)
                    )
                    1230 -> DecodedRtcm.GloBiases(
                        Rtcm1230Decoder.decode(msg.payload)
                    )
                    else -> DecodedRtcm.Unknown(msg.messageType, msg.payload)
                }
            } catch (e: Exception) {
                DecodedRtcm.Unknown(msg.messageType, msg.payload)
            }
        }
    }
}
