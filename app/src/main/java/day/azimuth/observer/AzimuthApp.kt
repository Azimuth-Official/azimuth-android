package day.azimuth.observer

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.local.HexCoverageDao
import day.azimuth.observer.service.CollectionController
import day.azimuth.observer.service.CoverageBackfillWorker
import day.azimuth.observer.service.CoverageSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AzimuthApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MigrationEntryPoint {
        fun hexCoverageDao(): HexCoverageDao
        fun azimuthPreferences(): AzimuthPreferences
        fun collectionController(): CollectionController
    }

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

        runHexDataMigration()
        autoStartCollection()
    }

    private fun autoStartCollection() {
        appScope.launch {
            val ep = EntryPointAccessors.fromApplication(
                this@AzimuthApp, MigrationEntryPoint::class.java,
            )
            val prefs = ep.azimuthPreferences()
            val controller = ep.collectionController()

            val registered = prefs.isRegistered.first()
            val enabled = prefs.collectionEnabled.first()
            if (!registered || !enabled) return@launch

            val hasLocation = ContextCompat.checkSelfPermission(
                this@AzimuthApp, android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

            val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this@AzimuthApp, android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            if (!hasLocation || !hasNotifications) {
                Log.i("AzimuthApp", "Auto-start skipped: location=$hasLocation, notifications=$hasNotifications")
                return@launch
            }

            Log.i("AzimuthApp", "Auto-starting collection")
            controller.startCollection()
        }
    }

    private fun runHexDataMigration() {
        appScope.launch {
            val ep = EntryPointAccessors.fromApplication(
                this@AzimuthApp, MigrationEntryPoint::class.java
            )
            val prefs = ep.azimuthPreferences()
            val dao = ep.hexCoverageDao()

            if (prefs.getHexDataVersion() < 4) {
                // v4: wipe stale grid8 entries, re-derive + sync with server boundaries
                dao.deleteAll()
                // Re-derive hex coverage from local observations
                CoverageBackfillWorker.enqueue(this@AzimuthApp)
                // Fetch global coverage (with boundaries) from server
                WorkManager.getInstance(this@AzimuthApp).enqueueUniqueWork(
                    "coverage_migration_sync",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<CoverageSyncWorker>().build()
                )
                prefs.setHexDataVersion(4)
            }
        }
    }
}
