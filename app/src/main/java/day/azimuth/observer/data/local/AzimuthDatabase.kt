package day.azimuth.observer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Observation::class],
    version = 2,
    exportSchema = false,
)
abstract class AzimuthDatabase : RoomDatabase() {
    abstract fun observationDao(): ObservationDao
}
