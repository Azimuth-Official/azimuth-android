package day.azimuth.observer.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import day.azimuth.observer.data.local.AzimuthDatabase;
import day.azimuth.observer.data.local.ObservationDao;
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
public final class AppModule_ProvideObservationDaoFactory implements Factory<ObservationDao> {
  private final Provider<AzimuthDatabase> dbProvider;

  public AppModule_ProvideObservationDaoFactory(Provider<AzimuthDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ObservationDao get() {
    return provideObservationDao(dbProvider.get());
  }

  public static AppModule_ProvideObservationDaoFactory create(
      Provider<AzimuthDatabase> dbProvider) {
    return new AppModule_ProvideObservationDaoFactory(dbProvider);
  }

  public static ObservationDao provideObservationDao(AzimuthDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideObservationDao(db));
  }
}
