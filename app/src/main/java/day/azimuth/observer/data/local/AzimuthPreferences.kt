package day.azimuth.observer.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzimuthPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val email: Flow<String> = dataStore.data.map { it[KEY_EMAIL] ?: "" }
    val apiKey: Flow<String> = dataStore.data.map { it[KEY_API_KEY] ?: "" }
    val nodeId: Flow<String> = dataStore.data.map { it[KEY_NODE_ID] ?: "" }
    val userId: Flow<String> = dataStore.data.map { it[KEY_USER_ID] ?: "" }
    val apiEndpoint: Flow<String> = dataStore.data.map { it[KEY_API_ENDPOINT] ?: "http://192.168.3.162:3000/" }

    val isRegistered: Flow<Boolean> = dataStore.data.map {
        !it[KEY_API_KEY].isNullOrEmpty() && !it[KEY_NODE_ID].isNullOrEmpty()
    }

    suspend fun saveRegistration(userId: String, apiKey: String, email: String) {
        dataStore.edit {
            it[KEY_USER_ID] = userId
            it[KEY_API_KEY] = apiKey
            it[KEY_EMAIL] = email
        }
    }

    suspend fun saveNodeId(nodeId: String) {
        dataStore.edit { it[KEY_NODE_ID] = nodeId }
    }

    suspend fun saveApiEndpoint(endpoint: String) {
        dataStore.edit { it[KEY_API_ENDPOINT] = endpoint }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_NODE_ID = stringPreferencesKey("node_id")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_API_ENDPOINT = stringPreferencesKey("api_endpoint")
    }
}
