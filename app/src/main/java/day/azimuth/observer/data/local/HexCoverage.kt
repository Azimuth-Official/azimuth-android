package day.azimuth.observer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local coverage aggregation for hex map feature.
 * Coarse hex units (H3 res 8 or temporary grid fallback).
 * Never stores or exposes raw lat/lon or routes.
 */
@Entity(tableName = "hex_coverage")
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
    val latestAccuracy: Float? = null
)
