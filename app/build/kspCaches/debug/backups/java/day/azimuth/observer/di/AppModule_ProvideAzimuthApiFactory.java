package day.azimuth.observer.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.AzimuthPreferences;
import day.azimuth.observer.data.remote.AzimuthApi;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideAzimuthApiFactory implements Factory<AzimuthApi> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<AzimuthPreferences> prefsProvider;

  public AppModule_ProvideAzimuthApiFactory(Provider<OkHttpClient> clientProvider,
      Provider<AzimuthPreferences> prefsProvider) {
    this.clientProvider = clientProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public AzimuthApi get() {
    return provideAzimuthApi(clientProvider.get(), prefsProvider.get());
  }

  public static AppModule_ProvideAzimuthApiFactory create(Provider<OkHttpClient> clientProvider,
      Provider<AzimuthPreferences> prefsProvider) {
    return new AppModule_ProvideAzimuthApiFactory(clientProvider, prefsProvider);
  }

  public static AzimuthApi provideAzimuthApi(OkHttpClient client, AzimuthPreferences prefs) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAzimuthApi(client, prefs));
  }
}
