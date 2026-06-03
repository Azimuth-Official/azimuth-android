package day.azimuth.observer.data.local;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class AzimuthPreferences_Factory implements Factory<AzimuthPreferences> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public AzimuthPreferences_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AzimuthPreferences get() {
    return newInstance(dataStoreProvider.get());
  }

  public static AzimuthPreferences_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AzimuthPreferences_Factory(dataStoreProvider);
  }

  public static AzimuthPreferences newInstance(DataStore<Preferences> dataStore) {
    return new AzimuthPreferences(dataStore);
  }
}
