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
import day.azimuth.observer.data.local.ObservationRepository

@HiltWorker
class CoverageBackfillWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observationRepository: ObservationRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val backfilled = observationRepository.backfillCoverage()
            if (backfilled) {
                Log.i(TAG, "Coverage backfill completed")
            } else {
                Log.i(TAG, "Coverage backfill not needed")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Coverage backfill failed", e)
            Result.retry()
        }
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
