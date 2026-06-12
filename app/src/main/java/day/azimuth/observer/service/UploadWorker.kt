package day.azimuth.observer.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.local.HexCoverageDao
import day.azimuth.observer.data.local.HexIndexer
import day.azimuth.observer.data.local.ObservationDao
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.ObservationPayload
import day.azimuth.observer.data.remote.SubmitObservationsRequest
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import android.os.Build

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observationDao: ObservationDao,
    private val api: AzimuthApi,
    private val prefs: AzimuthPreferences,
    private val hexCoverageDao: HexCoverageDao,
    private val hexIndexer: HexIndexer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val nodeId = prefs.nodeId.first()
        if (nodeId.isEmpty()) {
            Log.w(TAG, "No node_id configured; skipping upload")
            return Result.failure(workDataOf(KEY_ERROR to "No node_id"))
        }

        var totalAccepted = 0
        var hadFailure = false

        val packageInfo = runCatching {
            @Suppress("DEPRECATION")
            applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
        }.getOrNull()
        val appVersion = packageInfo?.versionName
        @Suppress("DEPRECATION")
        val buildNumber = packageInfo?.versionCode?.toString()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val androidApiLevel = Build.VERSION.SDK_INT

        repeat(MAX_BATCHES) {
            val batch = observationDao.getUploadBatch(BATCH_SIZE)
            if (batch.isEmpty()) {
                Log.i(TAG, "Upload complete: $totalAccepted accepted, queue empty")
                // Queue drained — terminate periodic chain so WorkManager stops scheduling
                return Result.success()
            }

            val request = SubmitObservationsRequest(
                nodeId = nodeId,
                observations = batch.map { obs ->
                    ObservationPayload(
                        signalType = obs.signalType,
                        observedAt = Instant.ofEpochMilli(obs.timestamp)
                            .atOffset(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT),
                        latitude = obs.latitude,
                        longitude = obs.longitude,
                        accuracy = obs.accuracy,
                        frequencyHz = obs.frequencyHz,
                        timestampNs = obs.timestampNs,
                        fullBiasNanos = obs.fullBiasNanos,
                        clientDedupeKey = obs.id.toString(),
                        appVersion = appVersion,
                        buildNumber = buildNumber,
                        deviceModel = deviceModel,
                        androidApiLevel = androidApiLevel,
                        validationStatus = "raw",
                        rawData = mapOf(
                            "payload" to obs.payload,
                        ),
                        rtkEnabled = obs.rtkEnabled,
                    )
                },
            )

            try {
                val response = api.submitObservations(request)

                // Validate partial accept: if fewer than batch size were accepted, log a warning
                val batchSize = batch.size
                if (response.accepted < batchSize) {
                    Log.w(TAG, "Partial accept: ${response.accepted}/$batchSize observations accepted")
                }

                // Mark all observations as uploaded in BOTH cases
                // (whether server accepted or filtered them)
                observationDao.markUploaded(batch.map { it.id })

                if (response.accepted == 0) {
                    // Server filtered all observations (accuracy >50m or deduped).
                    // Mark as uploaded — retrying won't change the outcome.
                    Log.w(TAG, "Server filtered ${batch.size} observations (accuracy/dedupe); marking as uploaded")
                } else {
                    totalAccepted += response.accepted
                    Log.i(TAG, "Uploaded batch: ${response.accepted} accepted")
                }

                // ALWAYS reconcile coverage: decrement pending counts for all uploaded observations
                // This runs whether accepted == 0 or not, ensuring pending count is reset
                try {
                    val countsByHex = mutableMapOf<String, Int>()
                    for (obs in batch) {
                        val h3 = hexIndexer.latLonToHex(obs.latitude, obs.longitude) ?: continue
                        countsByHex[h3] = countsByHex.getOrDefault(h3, 0) + 1
                    }
                    for ((h3, count) in countsByHex) {
                        hexCoverageDao.decrementPendingCount(h3, count)
                    }
                } catch (reconcileEx: Exception) {
                    Log.w(TAG, "Coverage reconciliation failed: ${reconcileEx.message}")
                }
            } catch (e: HttpException) {
                when (e.code()) {
                    401, 403 -> {
                        Log.e(TAG, "Auth failed (${e.code()}); will not retry automatically")
                        return Result.failure(workDataOf(KEY_ERROR to "Auth ${e.code()}"))
                    }
                    400 -> {
                        Log.e(TAG, "Payload rejected (${e.code()}): ${e.response()?.errorBody()?.string()}")
                        hadFailure = true
                    }
                    in 500..599 -> {
                        Log.w(TAG, "Server error (${e.code()}); will retry")
                        hadFailure = true
                    }
                    else -> {
                        Log.w(TAG, "Unexpected HTTP ${e.code()}; will retry")
                        hadFailure = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error; will retry: ${e.message}")
                hadFailure = true
            }
        }

        return if (hadFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "UploadWorker"
        private const val BATCH_SIZE = 50
        private const val MAX_BATCHES = 10
        const val UPLOAD_WORK_TAG = "azimuth_upload"
        const val KEY_ERROR = "upload_error"

        fun enqueueOneOff(context: Context) {
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .addTag(UPLOAD_WORK_TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
