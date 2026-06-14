package day.azimuth.observer.data.local

data class EphemerisNtripConfig(
    val casterUrl: String,
    val casterPort: Int = 2101,
    val mountpoint: String,
    val username: String = "",
    val password: String = "",
)
