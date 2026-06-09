package day.azimuth.observer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import day.azimuth.observer.service.CoverageBackfillWorker
import day.azimuth.observer.service.CoverageSyncWorker
import javax.inject.Inject

@HiltAndroidApp
class AzimuthApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CoverageBackfillWorker.enqueue(this)
        CoverageSyncWorker.enqueue(this)

        // Add immediate one-shot coverage sync on launch
        WorkManager.getInstance(this).enqueueUniqueWork(
            "coverage_sync_immediate",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<CoverageSyncWorker>().build()
        )
    }
}
