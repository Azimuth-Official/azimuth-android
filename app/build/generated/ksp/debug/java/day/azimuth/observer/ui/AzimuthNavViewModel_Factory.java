package day.azimuth.observer.ui;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.AzimuthPreferences;
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
public final class AzimuthNavViewModel_Factory implements Factory<AzimuthNavViewModel> {
  private final Provider<AzimuthPreferences> prefsProvider;

  public AzimuthNavViewModel_Factory(Provider<AzimuthPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public AzimuthNavViewModel get() {
    return newInstance(prefsProvider.get());
  }

  public static AzimuthNavViewModel_Factory create(Provider<AzimuthPreferences> prefsProvider) {
    return new AzimuthNavViewModel_Factory(prefsProvider);
  }

  public static AzimuthNavViewModel newInstance(AzimuthPreferences prefs) {
    return new AzimuthNavViewModel(prefs);
  }
}
