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
public final class WiFiRttCollector_Factory implements Factory<WiFiRttCollector> {
  private final Provider<Context> contextProvider;

  private final Provider<ObservationDao> observationDaoProvider;

  private final Provider<LocationProvider> locationProvider;

  private final Provider<Gson> gsonProvider;

  public WiFiRttCollector_Factory(Provider<Context> contextProvider,
      Provider<ObservationDao> observationDaoProvider, Provider<LocationProvider> locationProvider,
      Provider<Gson> gsonProvider) {
    this.contextProvider = contextProvider;
    this.observationDaoProvider = observationDaoProvider;
    this.locationProvider = locationProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public WiFiRttCollector get() {
    return newInstance(contextProvider.get(), observationDaoProvider.get(), locationProvider.get(), gsonProvider.get());
  }

  public static WiFiRttCollector_Factory create(Provider<Context> contextProvider,
      Provider<ObservationDao> observationDaoProvider, Provider<LocationProvider> locationProvider,
      Provider<Gson> gsonProvider) {
    return new WiFiRttCollector_Factory(contextProvider, observationDaoProvider, locationProvider, gsonProvider);
  }

  public static WiFiRttCollector newInstance(Context context, ObservationDao observationDao,
      LocationProvider locationProvider, Gson gson) {
    return new WiFiRttCollector(context, observationDao, locationProvider, gson);
  }
}
