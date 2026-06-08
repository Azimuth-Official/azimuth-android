package day.azimuth.observer.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import day.azimuth.observer.data.local.HexCoverage
import day.azimuth.observer.data.local.HexCoverageDao
import day.azimuth.observer.data.local.HexIndexer
import day.azimuth.observer.data.local.ObservationDao

@HiltWorker
class CoverageBackfillWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observationDao: ObservationDao,
    private val hexCoverageDao: HexCoverageDao,
    private val hexIndexer: HexIndexer,
) : CoroutineWorker(context, params) {

    private val random = java.util.Random()

    override suspend fun doWork(): Result {
        return try {
            backfillCoverage()
            Log.i(TAG, "Coverage backfill completed")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Coverage backfill failed", e)
            Result.retry()
        }
    }

    private suspend fun backfillCoverage() {
        val chunkSize = 100
        var offset = 0
        val aggregates = mutableMapOf<String, Aggregate>()

        while (true) {
            val chunk = try {
                observationDao.getAllForBackfillChunk(chunkSize, offset)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read observations at offset $offset", e)
                break
            }
            if (chunk.isEmpty()) break

            for (obs in chunk) {
                val h3 = hexIndexer.latLonToHex(obs.latitude, obs.longitude) ?: continue
                val agg = aggregates.getOrPut(h3) { Aggregate() }
                agg.firstSeen = minOf(agg.firstSeen, obs.timestamp)
                agg.lastSeen = maxOf(agg.lastSeen, obs.timestamp)
                agg.observationCount++
                if (!obs.uploaded) agg.pendingCount++
                if (obs.signalType.startsWith("cell")) agg.cellCount++
                if (obs.signalType == "gnss_raw") agg.gnssCount++
                if (obs.signalType.startsWith("wifi")) agg.wifiCount++
                if (obs.timestamp >= agg.lastTimestampForAccuracy) {
                    agg.lastTimestampForAccuracy = obs.timestamp
                    agg.latestAccuracy = obs.accuracy
                }
            }
            offset += chunkSize
        }

        for ((h3, agg) in aggregates) {
            val existing = try { hexCoverageDao.getByH3(h3) } catch (e: Exception) { null }

            val gridX = existing?.gridX ?: random.nextInt(10)
            val gridY = existing?.gridY ?: random.nextInt(10)

            val coverage = HexCoverage(
                h3Index = h3,
                firstSeen = agg.firstSeen,
                lastSeen = agg.lastSeen,
                observationCount = agg.observationCount,
                pendingCount = agg.pendingCount,
                cellCount = agg.cellCount,
                gnssCount = agg.gnssCount,
                wifiCount = agg.wifiCount,
                latestAccuracy = agg.latestAccuracy,
                gridX = gridX,
                gridY = gridY
            )

            hexCoverageDao.upsert(coverage)
        }
    }

    private class Aggregate {
        var firstSeen: Long = Long.MAX_VALUE
        var lastSeen: Long = Long.MIN_VALUE
        var observationCount: Int = 0
        var pendingCount: Int = 0
        var cellCount: Int = 0
        var gnssCount: Int = 0
        var wifiCount: Int = 0
        var lastTimestampForAccuracy: Long = Long.MIN_VALUE
        var latestAccuracy: Float? = null
    }

    companion object {
        private const val TAG = "CoverageBackfill"
        private const val WORK_NAME = "azimuth_coverage_backfill"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<CoverageBackfillWorker>()
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
