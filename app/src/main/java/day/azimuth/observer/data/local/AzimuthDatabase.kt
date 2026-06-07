package day.azimuth.observer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Observation::class, HexCoverage::class],
    version = 5,
    exportSchema = false,
)
abstract class AzimuthDatabase : RoomDatabase() {
    abstract fun observationDao(): ObservationDao
    abstract fun hexCoverageDao(): HexCoverageDao

    companion object {
        private fun recreateHexCoverageTable(db: SupportSQLiteDatabase) {
            // hex_coverage is derived from observations and can be safely rebuilt.
            // Never drop or alter observations here.
            db.execSQL("DROP TABLE IF EXISTS `hex_coverage`")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `hex_coverage` (" +
                    "`h3Index` TEXT NOT NULL, " +
                    "`resolution` INTEGER NOT NULL, " +
                    "`firstSeen` INTEGER NOT NULL, " +
                    "`lastSeen` INTEGER NOT NULL, " +
                    "`observationCount` INTEGER NOT NULL, " +
                    "`pendingCount` INTEGER NOT NULL, " +
                    "`cellCount` INTEGER NOT NULL, " +
                    "`gnssCount` INTEGER NOT NULL, " +
                    "`wifiCount` INTEGER NOT NULL, " +
                    "`latestAccuracy` REAL, " +
                    "PRIMARY KEY(`h3Index`)" +
                ")"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hex_coverage_lastSeen` " +
                    "ON `hex_coverage` (`lastSeen`)"
            )
        }

        // MIGRATION_2_3 creates/repairs the derived hex_coverage table.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateHexCoverageTable(db)
            }
        }

        // MIGRATION_3_4 repairs bad v3 schemas on devices that already have version 3.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateHexCoverageTable(db)
            }
        }

        // MIGRATION_4_5 adds schematic grid layout columns without dropping data.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hex_coverage ADD COLUMN gridX INTEGER")
                db.execSQL("ALTER TABLE hex_coverage ADD COLUMN gridY INTEGER")
            }
        }
    }
}
