package day.azimuth.observer.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.AzimuthPreferences;
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
public final class AppModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<AzimuthPreferences> prefsProvider;

  public AppModule_ProvideOkHttpClientFactory(Provider<AzimuthPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(prefsProvider.get());
  }

  public static AppModule_ProvideOkHttpClientFactory create(
      Provider<AzimuthPreferences> prefsProvider) {
    return new AppModule_ProvideOkHttpClientFactory(prefsProvider);
  }

  public static OkHttpClient provideOkHttpClient(AzimuthPreferences prefs) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideOkHttpClient(prefs));
  }
}
