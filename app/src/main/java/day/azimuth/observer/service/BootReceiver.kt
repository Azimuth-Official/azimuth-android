package day.azimuth.observer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import day.azimuth.observer.data.local.AzimuthPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var collectionController: CollectionController
    @Inject lateinit var prefs: AzimuthPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val canStart = runBlocking {
            val registered = prefs.isRegistered.first()
            val enabled = prefs.collectionEnabled.first()
            registered && enabled
        }

        if (!canStart) {
            Log.i(TAG, "Boot: skipping collection (not registered or disabled)")
            return
        }

        val hasLocation = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasLocation || !hasNotifications) {
            Log.i(TAG, "Boot: skipping collection (missing permissions: location=$hasLocation, notifications=$hasNotifications)")
            return
        }

        Log.i(TAG, "Boot: starting collection")
        collectionController.startCollection()
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
