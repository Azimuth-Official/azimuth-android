package day.azimuth.observer.service.ntrip

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.util.Log

class GnssCorrector(private val context: Context) {
    fun isSupported(): Boolean {
        // Requires Android 12+ (API 31)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.d(TAG, "GNSS corrections not supported on API ${Build.VERSION.SDK_INT}")
            return false
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            Log.w(TAG, "LocationManager not available")
            return false
        }

        return true
    }

    fun injectCorrections(rtcmData: ByteArray) {
        try {
            if (!isSupported()) {
                Log.d(TAG, "GNSS corrections not supported on this device")
                return
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                Log.w(TAG, "LocationManager not available")
                return
            }

            // Attempt to inject corrections
            // Note: This API is highly experimental and many chipsets don't support it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    // Call injectGnssMeasurementCorrections if available
                    val method = LocationManager::class.java.getMethod(
                        "injectGnssMeasurementCorrections",
                        ByteArray::class.java
                    )
                    method.invoke(locationManager, rtcmData)
                    Log.i(TAG, "Injected ${rtcmData.size} bytes of GNSS corrections")
                } catch (e: NoSuchMethodException) {
                    Log.d(TAG, "injectGnssMeasurementCorrections not available on this build")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to inject GNSS corrections: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in GNSS correction injection: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "GnssCorrector"
    }
}
