package day.azimuth.observer;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class AzimuthApp_MembersInjector implements MembersInjector<AzimuthApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public AzimuthApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<AzimuthApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new AzimuthApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(AzimuthApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("day.azimuth.observer.AzimuthApp.workerFactory")
  public static void injectWorkerFactory(AzimuthApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
