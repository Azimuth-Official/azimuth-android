package day.azimuth.observer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import day.azimuth.observer.service.CoverageBackfillWorker
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
    }
}
