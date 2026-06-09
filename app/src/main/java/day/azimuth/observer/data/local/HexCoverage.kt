package day.azimuth.observer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local coverage aggregation for hex map feature.
 * Coarse hex units (H3 res 8 or temporary grid fallback).
 * Never stores or exposes raw lat/lon or routes.
 */
@Entity(
    tableName = "hex_coverage",
    indices = [Index(value = ["lastSeen"], name = "index_hex_coverage_lastSeen")]
)
data class HexCoverage(
    @PrimaryKey val h3Index: String,
    val resolution: Int = 8,
    val firstSeen: Long,
    val lastSeen: Long,
    val observationCount: Int = 0,
    val pendingCount: Int = 0,
    val cellCount: Int = 0,
    val gnssCount: Int = 0,
    val wifiCount: Int = 0,
    val latestAccuracy: Float? = null,
    val gridX: Int? = null,
    val gridY: Int? = null,
    val tier: String = "OWN",
    val boundary: String? = null  // JSON [[lat,lng],...] from server h3-js cellToBoundary
) {
    fun getTier(): CoverageTier = try {
        CoverageTier.valueOf(tier)
    } catch (_: Exception) {
        CoverageTier.OWN
    }
}
