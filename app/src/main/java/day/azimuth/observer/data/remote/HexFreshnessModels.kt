package day.azimuth.observer.data.remote

import com.google.gson.annotations.SerializedName

data class HexFreshnessData(
    @SerializedName("h3_index") val h3Index: String,
    @SerializedName("last_observation") val lastObservation: String?,
    @SerializedName("freshness_tier") val freshnessTier: String,
    @SerializedName("freshness_multiplier") val freshnessMultiplier: Double,
    @SerializedName("observation_count") val observationCount: Int,
    val boundary: List<List<Double>>,
)

data class HexFreshnessResponse(
    val hexes: List<HexFreshnessData>,
)
