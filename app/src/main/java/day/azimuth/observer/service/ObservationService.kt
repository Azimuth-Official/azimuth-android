package day.azimuth.observer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import day.azimuth.observer.data.local.ObservationDao
import day.azimuth.observer.service.collectors.CellInfoCollector
import day.azimuth.observer.service.collectors.GnssMeasurementCollector
import day.azimuth.observer.service.collectors.WiFiRttCollector
import day.azimuth.observer.service.collectors.WiFiSurveyCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class ObservationService : Service() {

    @Inject lateinit var observationDao: ObservationDao
    @Inject lateinit var cellInfoCollector: CellInfoCollector
    @Inject lateinit var gnssMeasurementCollector: GnssMeasurementCollector
    @Inject lateinit var wifiSurveyCollector: WiFiSurveyCollector
    @Inject lateinit var wifiRttCollector: WiFiRttCollector

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCollecting()
            ACTION_STOP -> {
                stopCollecting()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCollecting()
        scope.cancel()
        super.onDestroy()
    }

    private fun startCollecting() {
        cellInfoCollector.start(scope)
        gnssMeasurementCollector.start(scope)
        wifiSurveyCollector.start(scope)
        wifiRttCollector.start(scope)
    }

    private fun stopCollecting() {
        cellInfoCollector.stop()
        gnssMeasurementCollector.stop()
        wifiSurveyCollector.stop()
        wifiRttCollector.stop()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        // Delete legacy channel whose IMPORTANCE_LOW is cached on existing installs
        nm.deleteNotificationChannel("observation_service")
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Observation Service",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Shows when Azimuth is collecting radio observations"
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Azimuth Observer")
            .setContentText("Collecting radio environment data...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    companion object {
        const val ACTION_START = "day.azimuth.observer.START"
        const val ACTION_STOP = "day.azimuth.observer.STOP"
        private const val CHANNEL_ID = "observation_service_v2"
        private const val NOTIFICATION_ID = 1
    }
}
