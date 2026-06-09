package day.azimuth.observer.service.ntrip

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.util.Log

class GnssCorrector(private val context: Context) {
    /**
     * null = unknown (not yet tested), true = chipset supports injection, false = not supported
     */
    private var chipsetSupported: Boolean? = null

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

    fun isChipsetSupported(): Boolean? = chipsetSupported

    fun injectCorrections(rtcmData: ByteArray) {
        try {
            if (!isSupported()) {
                Log.d(TAG, "GNSS corrections not supported on this device")
                return
            }

            // If we already know chipset doesn't support injection, skip the reflection attempt
            if (chipsetSupported == false) {
                Log.d(TAG, "Chipset does not support GNSS correction injection, skipping")
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
                    if (chipsetSupported == null) {
                        chipsetSupported = true
                        Log.i(TAG, "Chipset supports GNSS correction injection")
                    }
                    Log.d(TAG, "Injected ${rtcmData.size} bytes of GNSS corrections")
                } catch (e: NoSuchMethodException) {
                    if (chipsetSupported == null) {
                        chipsetSupported = false
                        Log.w(TAG, "Chipset does not support GNSS correction injection: method not available")
                    }
                } catch (e: SecurityException) {
                    if (chipsetSupported == null) {
                        chipsetSupported = false
                        Log.w(TAG, "Chipset does not support GNSS correction injection: permission denied")
                    }
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
