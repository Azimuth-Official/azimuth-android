package day.azimuth.observer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HexCoverageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(coverage: HexCoverage)

    @Query("SELECT * FROM hex_coverage WHERE h3Index = :h3 LIMIT 1")
    suspend fun getByH3(h3: String): HexCoverage?

    @Query("SELECT * FROM hex_coverage ORDER BY lastSeen DESC")
    fun getAll(): Flow<List<HexCoverage>>

    @Query("SELECT COUNT(*) FROM hex_coverage")
    fun getTotalCount(): Flow<Int>

    // For today, a simple query; VM can filter further if needed
    @Query("SELECT * FROM hex_coverage WHERE lastSeen >= :since ORDER BY lastSeen DESC")
    fun getSince(since: Long): Flow<List<HexCoverage>>

    @Query("UPDATE hex_coverage SET pendingCount = CASE WHEN pendingCount >= :count THEN pendingCount - :count ELSE 0 END WHERE h3Index = :h3Index")
    suspend fun decrementPendingCount(h3Index: String, count: Int)
}
