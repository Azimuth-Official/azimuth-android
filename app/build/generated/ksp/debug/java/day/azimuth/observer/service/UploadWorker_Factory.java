package day.azimuth.observer.service;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.AzimuthPreferences;
import day.azimuth.observer.data.local.ObservationDao;
import day.azimuth.observer.data.remote.AzimuthApi;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class UploadWorker_Factory {
  private final Provider<ObservationDao> observationDaoProvider;

  private final Provider<AzimuthApi> apiProvider;

  private final Provider<AzimuthPreferences> prefsProvider;

  public UploadWorker_Factory(Provider<ObservationDao> observationDaoProvider,
      Provider<AzimuthApi> apiProvider, Provider<AzimuthPreferences> prefsProvider) {
    this.observationDaoProvider = observationDaoProvider;
    this.apiProvider = apiProvider;
    this.prefsProvider = prefsProvider;
  }

  public UploadWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, observationDaoProvider.get(), apiProvider.get(), prefsProvider.get());
  }

  public static UploadWorker_Factory create(Provider<ObservationDao> observationDaoProvider,
      Provider<AzimuthApi> apiProvider, Provider<AzimuthPreferences> prefsProvider) {
    return new UploadWorker_Factory(observationDaoProvider, apiProvider, prefsProvider);
  }

  public static UploadWorker newInstance(Context context, WorkerParameters params,
      ObservationDao observationDao, AzimuthApi api, AzimuthPreferences prefs) {
    return new UploadWorker(context, params, observationDao, api, prefs);
  }
}
