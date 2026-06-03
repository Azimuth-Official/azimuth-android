package day.azimuth.observer.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.local.ObservationDao
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.ObservationPayload
import day.azimuth.observer.data.remote.SubmitObservationsRequest
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observationDao: ObservationDao,
    private val api: AzimuthApi,
    private val prefs: AzimuthPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val nodeId = prefs.nodeId.first()
        if (nodeId.isEmpty()) return Result.failure()

        repeat(MAX_BATCHES) {
            val batch = observationDao.getUploadBatch(BATCH_SIZE)
            if (batch.isEmpty()) return Result.success()

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
                    )
                },
            )

            try {
                api.submitObservations(request)
                observationDao.markUploaded(batch.map { it.id })
            } catch (_: Exception) {
                return Result.retry()
            }
        }

        return Result.success()
    }

    companion object {
        private const val BATCH_SIZE = 50
        private const val MAX_BATCHES = 10
    }
}
