package day.azimuth.observer.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import day.azimuth.observer.BuildConfig
import day.azimuth.observer.data.local.AzimuthDatabase
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.data.local.HexCoverageDao
import day.azimuth.observer.data.local.HexIndexer
import day.azimuth.observer.data.local.HexIndexerImpl
import day.azimuth.observer.data.local.ObservationDao
import day.azimuth.observer.data.local.ObservationRepository
import day.azimuth.observer.data.remote.AzimuthApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.google.gson.Gson
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "azimuth_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideAzimuthPreferences(dataStore: DataStore<Preferences>): AzimuthPreferences =
        AzimuthPreferences(dataStore)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AzimuthDatabase =
        Room.databaseBuilder(
            context,
            AzimuthDatabase::class.java,
            "azimuth_observations.db",
        )
            .addMigrations(AzimuthDatabase.MIGRATION_2_3, AzimuthDatabase.MIGRATION_3_4)
            // fallbackToDestructiveMigration removed - real additive migration in use
            .build()

    @Provides
    fun provideObservationDao(db: AzimuthDatabase): ObservationDao = db.observationDao()

    @Provides
    fun provideHexCoverageDao(db: AzimuthDatabase): HexCoverageDao = db.hexCoverageDao()

    @Provides
    @Singleton
    fun provideHexIndexer(): HexIndexer = HexIndexerImpl()

    @Provides
    @Singleton
    fun provideObservationRepository(
        observationDao: ObservationDao,
        hexCoverageDao: HexCoverageDao,
        hexIndexer: HexIndexer
    ): ObservationRepository = ObservationRepository(observationDao, hexCoverageDao, hexIndexer)

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(prefs: AzimuthPreferences): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val apiKey = runBlocking { prefs.apiKey.first() }
            val request = if (apiKey.isNotEmpty()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAzimuthApi(client: OkHttpClient, prefs: AzimuthPreferences): AzimuthApi {
        val baseUrl = runBlocking { prefs.apiEndpoint.first() }
        return Retrofit.Builder()
            .baseUrl(baseUrl.ifEmpty { "https://api.azimuth.day/" })
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AzimuthApi::class.java)
    }
}
