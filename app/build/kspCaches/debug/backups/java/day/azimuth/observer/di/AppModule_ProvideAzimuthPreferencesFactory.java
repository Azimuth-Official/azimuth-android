package day.azimuth.observer.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.AzimuthPreferences;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideAzimuthPreferencesFactory implements Factory<AzimuthPreferences> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public AppModule_ProvideAzimuthPreferencesFactory(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AzimuthPreferences get() {
    return provideAzimuthPreferences(dataStoreProvider.get());
  }

  public static AppModule_ProvideAzimuthPreferencesFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AppModule_ProvideAzimuthPreferencesFactory(dataStoreProvider);
  }

  public static AzimuthPreferences provideAzimuthPreferences(DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAzimuthPreferences(dataStore));
  }
}
