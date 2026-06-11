package day.azimuth.observer.service.ntrip

interface CorrectionTier {
    val id: String
    fun isSupported(): Boolean
    fun start()
    fun stop()
    fun onRtcmData(data: ByteArray)
    fun isApplyingCorrections(): Boolean
    fun reportedAccuracyMeters(): Float?
}
