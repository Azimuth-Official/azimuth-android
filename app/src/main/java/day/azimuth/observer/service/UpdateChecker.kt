package day.azimuth.observer.service

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import day.azimuth.observer.data.remote.AzimuthApi
import day.azimuth.observer.data.remote.VersionInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateChecker @Inject constructor(
    private val api: AzimuthApi,
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun checkForUpdate(currentVersionCode: Int): VersionInfo? {
        return try {
        val lastCheckTime = dataStore.data.first()[KEY_LAST_UPDATE_CHECK] ?: 0L

        val now = System.currentTimeMillis()
        val cooldownMs = 24 * 60 * 60 * 1000L // 24 hours

        if (now - lastCheckTime < cooldownMs) {
            Log.d(TAG, "Update check still in cooldown period")
            return null
        }

        val response = api.getLatestVersion()

        // Save the timestamp
        dataStore.edit { prefs ->
            prefs[KEY_LAST_UPDATE_CHECK] = now
        }

        if (response.versionCode > currentVersionCode) {
            Log.i(TAG, "Update available: ${response.versionName} (code ${response.versionCode})")
            response
        } else {
            Log.d(TAG, "App is up to date")
            null
        }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for update: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "UpdateChecker"
        private val KEY_LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
    }
}
