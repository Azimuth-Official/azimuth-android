package day.azimuth.observer.service.collectors;

import android.content.Context;
import com.google.gson.Gson;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.ObservationDao;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class WiFiSurveyCollector_Factory implements Factory<WiFiSurveyCollector> {
  private final Provider<Context> contextProvider;

  private final Provider<ObservationDao> observationDaoProvider;

  private final Provider<LocationProvider> locationProvider;

  private final Provider<Gson> gsonProvider;

  public WiFiSurveyCollector_Factory(Provider<Context> contextProvider,
      Provider<ObservationDao> observationDaoProvider, Provider<LocationProvider> locationProvider,
      Provider<Gson> gsonProvider) {
    this.contextProvider = contextProvider;
    this.observationDaoProvider = observationDaoProvider;
    this.locationProvider = locationProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public WiFiSurveyCollector get() {
    return newInstance(contextProvider.get(), observationDaoProvider.get(), locationProvider.get(), gsonProvider.get());
  }

  public static WiFiSurveyCollector_Factory create(Provider<Context> contextProvider,
      Provider<ObservationDao> observationDaoProvider, Provider<LocationProvider> locationProvider,
      Provider<Gson> gsonProvider) {
    return new WiFiSurveyCollector_Factory(contextProvider, observationDaoProvider, locationProvider, gsonProvider);
  }

  public static WiFiSurveyCollector newInstance(Context context, ObservationDao observationDao,
      LocationProvider locationProvider, Gson gson) {
    return new WiFiSurveyCollector(context, observationDao, locationProvider, gson);
  }
}
