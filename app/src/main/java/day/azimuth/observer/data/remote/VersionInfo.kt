package day.azimuth.observer.data.remote

import com.google.gson.annotations.SerializedName

data class VersionInfo(
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("release_notes") val releaseNotes: String?,
)
