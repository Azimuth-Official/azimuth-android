package day.azimuth.observer.service.ntrip

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class NativeSolution(
    @SerializedName("hasFix") val hasFix: Boolean = false,
    @SerializedName("lat") val latitudeDegrees: Double = 0.0,
    @SerializedName("lon") val longitudeDegrees: Double = 0.0,
    @SerializedName("alt") val altitudeMeters: Double = 0.0,
    @SerializedName("accuracy") val accuracyMeters: Float? = null,
    @SerializedName("nSat") val satelliteCount: Int = 0,
    @SerializedName("ratio") val arRatio: Double = 0.0,
    @SerializedName("message") val message: String = "",
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String): NativeSolution {
            return try {
                gson.fromJson(json, NativeSolution::class.java)
            } catch (e: Exception) {
                NativeSolution(message = "JSON parse error: ${e.message}")
            }
        }
    }
}
