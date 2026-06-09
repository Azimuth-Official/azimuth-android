package day.azimuth.observer.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class Observation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "signal_type") val signalType: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    @ColumnInfo(name = "frequency_hz") val frequencyHz: Long? = null,
    @ColumnInfo(name = "timestamp_ns") val timestampNs: Long? = null,
    val payload: String,
    val uploaded: Boolean = false,
    @ColumnInfo(name = "rtk_enabled") val rtkEnabled: Boolean = false,
)
