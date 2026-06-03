package day.azimuth.observer.service;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import day.azimuth.observer.data.local.ObservationDao;
import day.azimuth.observer.service.collectors.CellInfoCollector;
import day.azimuth.observer.service.collectors.GnssMeasurementCollector;
import day.azimuth.observer.service.collectors.WiFiRttCollector;
import day.azimuth.observer.service.collectors.WiFiSurveyCollector;
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
public final class ObservationService_MembersInjector implements MembersInjector<ObservationService> {
  private final Provider<ObservationDao> observationDaoProvider;

  private final Provider<CellInfoCollector> cellInfoCollectorProvider;

  private final Provider<GnssMeasurementCollector> gnssMeasurementCollectorProvider;

  private final Provider<WiFiSurveyCollector> wifiSurveyCollectorProvider;

  private final Provider<WiFiRttCollector> wifiRttCollectorProvider;

  public ObservationService_MembersInjector(Provider<ObservationDao> observationDaoProvider,
      Provider<CellInfoCollector> cellInfoCollectorProvider,
      Provider<GnssMeasurementCollector> gnssMeasurementCollectorProvider,
      Provider<WiFiSurveyCollector> wifiSurveyCollectorProvider,
      Provider<WiFiRttCollector> wifiRttCollectorProvider) {
    this.observationDaoProvider = observationDaoProvider;
    this.cellInfoCollectorProvider = cellInfoCollectorProvider;
    this.gnssMeasurementCollectorProvider = gnssMeasurementCollectorProvider;
    this.wifiSurveyCollectorProvider = wifiSurveyCollectorProvider;
    this.wifiRttCollectorProvider = wifiRttCollectorProvider;
  }

  public static MembersInjector<ObservationService> create(
      Provider<ObservationDao> observationDaoProvider,
      Provider<CellInfoCollector> cellInfoCollectorProvider,
      Provider<GnssMeasurementCollector> gnssMeasurementCollectorProvider,
      Provider<WiFiSurveyCollector> wifiSurveyCollectorProvider,
      Provider<WiFiRttCollector> wifiRttCollectorProvider) {
    return new ObservationService_MembersInjector(observationDaoProvider, cellInfoCollectorProvider, gnssMeasurementCollectorProvider, wifiSurveyCollectorProvider, wifiRttCollectorProvider);
  }

  @Override
  public void injectMembers(ObservationService instance) {
    injectObservationDao(instance, observationDaoProvider.get());
    injectCellInfoCollector(instance, cellInfoCollectorProvider.get());
    injectGnssMeasurementCollector(instance, gnssMeasurementCollectorProvider.get());
    injectWifiSurveyCollector(instance, wifiSurveyCollectorProvider.get());
    injectWifiRttCollector(instance, wifiRttCollectorProvider.get());
  }

  @InjectedFieldSignature("day.azimuth.observer.service.ObservationService.observationDao")
  public static void injectObservationDao(ObservationService instance,
      ObservationDao observationDao) {
    instance.observationDao = observationDao;
  }

  @InjectedFieldSignature("day.azimuth.observer.service.ObservationService.cellInfoCollector")
  public static void injectCellInfoCollector(ObservationService instance,
      CellInfoCollector cellInfoCollector) {
    instance.cellInfoCollector = cellInfoCollector;
  }

  @InjectedFieldSignature("day.azimuth.observer.service.ObservationService.gnssMeasurementCollector")
  public static void injectGnssMeasurementCollector(ObservationService instance,
      GnssMeasurementCollector gnssMeasurementCollector) {
    instance.gnssMeasurementCollector = gnssMeasurementCollector;
  }

  @InjectedFieldSignature("day.azimuth.observer.service.ObservationService.wifiSurveyCollector")
  public static void injectWifiSurveyCollector(ObservationService instance,
      WiFiSurveyCollector wifiSurveyCollector) {
    instance.wifiSurveyCollector = wifiSurveyCollector;
  }

  @InjectedFieldSignature("day.azimuth.observer.service.ObservationService.wifiRttCollector")
  public static void injectWifiRttCollector(ObservationService instance,
      WiFiRttCollector wifiRttCollector) {
    instance.wifiRttCollector = wifiRttCollector;
  }
}
