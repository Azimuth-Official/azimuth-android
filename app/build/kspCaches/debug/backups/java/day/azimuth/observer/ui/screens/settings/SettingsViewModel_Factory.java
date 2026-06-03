package day.azimuth.observer.ui.screens.settings;

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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AzimuthPreferences> prefsProvider;

  public SettingsViewModel_Factory(Provider<AzimuthPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(prefsProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<AzimuthPreferences> prefsProvider) {
    return new SettingsViewModel_Factory(prefsProvider);
  }

  public static SettingsViewModel newInstance(AzimuthPreferences prefs) {
    return new SettingsViewModel(prefs);
  }
}
