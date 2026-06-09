package day.azimuth.observer.service.collectors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        const val GPS_ACCURACY_THRESHOLD_M = 50.0f
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        return try {
            val location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token,
            ).await()

            if (location != null && location.accuracy > GPS_ACCURACY_THRESHOLD_M) {
                Log.d("LocationProvider", "Rejected: accuracy ${location.accuracy}m > ${GPS_ACCURACY_THRESHOLD_M}m")
                null
            } else {
                location
            }
        } catch (_: Exception) {
            null
        }
    }
}
