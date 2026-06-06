package day.azimuth.observer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Observation::class, HexCoverage::class],
    version = 3,
    exportSchema = false,
)
abstract class AzimuthDatabase : RoomDatabase() {
    abstract fun observationDao(): ObservationDao
    abstract fun hexCoverageDao(): HexCoverageDao

    companion object {
        // Additive migration 2 -> 3: creates coverage table + index only.
        // No DROP, no rewrite of observations, no data loss.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS hex_coverage (h3Index TEXT PRIMARY KEY NOT NULL, resolution INTEGER NOT NULL DEFAULT 8, firstSeen INTEGER NOT NULL, lastSeen INTEGER NOT NULL, observationCount INTEGER NOT NULL DEFAULT 0, pendingCount INTEGER NOT NULL DEFAULT 0, cellCount INTEGER NOT NULL DEFAULT 0, gnssCount INTEGER NOT NULL DEFAULT 0, wifiCount INTEGER NOT NULL DEFAULT 0, latestAccuracy REAL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_hex_coverage_lastSeen ON hex_coverage(lastSeen)")
            }
        }
    }
}
