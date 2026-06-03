package day.azimuth.observer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservationDao {

    @Insert
    suspend fun insert(observation: Observation)

    @Insert
    suspend fun insertAll(observations: List<Observation>)

    @Query("SELECT * FROM observations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<Observation>>

    @Query("SELECT COUNT(*) FROM observations")
    fun getTotalCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM observations WHERE uploaded = 0")
    fun getPendingUploadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM observations WHERE signal_type = :signalType")
    fun getCountByType(signalType: String): Flow<Long>

    @Query("SELECT * FROM observations WHERE uploaded = 0 ORDER BY timestamp ASC LIMIT :batchSize")
    suspend fun getUploadBatch(batchSize: Int = 50): List<Observation>

    @Query("UPDATE observations SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markUploaded(ids: List<Long>)

    @Query("DELETE FROM observations WHERE uploaded = 1 AND timestamp < :beforeTimestamp")
    suspend fun pruneUploaded(beforeTimestamp: Long)
}
