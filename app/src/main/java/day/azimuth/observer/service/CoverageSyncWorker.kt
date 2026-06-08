package day.azimuth.observer.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import day.azimuth.observer.data.local.HexCoverageDao
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.CoverageHexUpload
import day.azimuth.observer.data.remote.PostCoverageRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class CoverageSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                CoverageSyncEntryPoint::class.java
            )
            val hexCoverageDao = entryPoint.hexCoverageDao()
            val api = entryPoint.azimuthApi()

            uploadOwnCoverage(hexCoverageDao, api)
            fetchGlobalCoverage(hexCoverageDao, api)

            Log.i(TAG, "Coverage sync completed")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Coverage sync failed", e)
            Result.retry()
        }
    }

    private suspend fun uploadOwnCoverage(
        dao: HexCoverageDao,
        api: AzimuthApi
    ) {
        val ownHexes = dao.getAllSync().filter { it.tier == "OWN" }
        if (ownHexes.isEmpty()) {
            Log.i(TAG, "No OWN coverage hexes to upload")
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val uploads = ownHexes.map { hex ->
            val types = mutableListOf<String>()
            if (hex.cellCount > 0) types.add("cell")
            if (hex.gnssCount > 0) types.add("gnss")
            if (hex.wifiCount > 0) types.add("wifi")

            CoverageHexUpload(
                h3Index = hex.h3Index,
                observationCount = hex.observationCount,
                signalTypes = types,
                firstSeen = dateFormat.format(Date(hex.firstSeen)),
                lastSeen = dateFormat.format(Date(hex.lastSeen))
            )
        }

        try {
            api.postCoverage(PostCoverageRequest(hexes = uploads))
            Log.i(TAG, "Uploaded ${uploads.size} coverage hexes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload coverage hexes", e)
            throw e
        }
    }

    private suspend fun fetchGlobalCoverage(
        dao: HexCoverageDao,
        api: AzimuthApi
    ) {
        val response = try {
            api.getGlobalCoverage()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch global coverage", e)
            throw e
        }

        for (globalHex in response.hexes) {
            val existing = try {
                dao.getByH3(globalHex.h3Index)
            } catch (e: Exception) {
                null
            }

            // Skip if this hex is already our OWN coverage
            if (existing != null && existing.tier == "OWN") continue

            // Upsert as OTHER tier
            val otherHex = existing?.copy(
                observationCount = globalHex.totalObservations.toInt(),
                tier = "OTHER"
            ) ?: day.azimuth.observer.data.local.HexCoverage(
                h3Index = globalHex.h3Index,
                resolution = 8,
                firstSeen = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                observationCount = globalHex.totalObservations.toInt(),
                tier = "OTHER"
            )

            try {
                dao.upsert(otherHex)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to upsert OTHER coverage hex ${globalHex.h3Index}", e)
                // Continue with other hexes on error
            }
        }
        Log.i(TAG, "Fetched and synced ${response.hexes.size} global coverage hexes")
    }

    companion object {
        private const val TAG = "CoverageSyncWorker"
        private const val WORK_NAME = "azimuth_coverage_sync"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<CoverageSyncWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoverageSyncEntryPoint {
    fun hexCoverageDao(): HexCoverageDao
    fun azimuthApi(): AzimuthApi
}
