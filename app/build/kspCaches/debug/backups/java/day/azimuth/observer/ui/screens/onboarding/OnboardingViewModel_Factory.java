package day.azimuth.observer.ui.screens.onboarding;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.AzimuthPreferences;
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
public final class OnboardingViewModel_Factory implements Factory<OnboardingViewModel> {
  private final Provider<AzimuthApi> apiProvider;

  private final Provider<AzimuthPreferences> prefsProvider;

  public OnboardingViewModel_Factory(Provider<AzimuthApi> apiProvider,
      Provider<AzimuthPreferences> prefsProvider) {
    this.apiProvider = apiProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public OnboardingViewModel get() {
    return newInstance(apiProvider.get(), prefsProvider.get());
  }

  public static OnboardingViewModel_Factory create(Provider<AzimuthApi> apiProvider,
      Provider<AzimuthPreferences> prefsProvider) {
    return new OnboardingViewModel_Factory(apiProvider, prefsProvider);
  }

  public static OnboardingViewModel newInstance(AzimuthApi api, AzimuthPreferences prefs) {
    return new OnboardingViewModel(api, prefs);
  }
}
