package day.azimuth.observer.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AzimuthDatabase_Impl : AzimuthDatabase() {
  private val _observationDao: Lazy<ObservationDao> = lazy {
    ObservationDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "554d43514f96f33e344114191c804a72", "3f1f9433e095514e1e5f932f7b985b2a") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `observations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `signal_type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `accuracy` REAL NOT NULL, `frequency_hz` INTEGER, `timestamp_ns` INTEGER, `payload` TEXT NOT NULL, `uploaded` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '554d43514f96f33e344114191c804a72')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `observations`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsObservations: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsObservations.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("signal_type", TableInfo.Column("signal_type", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("latitude", TableInfo.Column("latitude", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("longitude", TableInfo.Column("longitude", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("accuracy", TableInfo.Column("accuracy", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("frequency_hz", TableInfo.Column("frequency_hz", "INTEGER", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("timestamp_ns", TableInfo.Column("timestamp_ns", "INTEGER", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("payload", TableInfo.Column("payload", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsObservations.put("uploaded", TableInfo.Column("uploaded", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysObservations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesObservations: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoObservations: TableInfo = TableInfo("observations", _columnsObservations,
            _foreignKeysObservations, _indicesObservations)
        val _existingObservations: TableInfo = read(connection, "observations")
        if (!_infoObservations.equals(_existingObservations)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |observations(day.azimuth.observer.data.local.Observation).
              | Expected:
              |""".trimMargin() + _infoObservations + """
              |
              | Found:
              |""".trimMargin() + _existingObservations)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "observations")
  }

  public override fun clearAllTables() {
    super.performClear(false, "observations")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ObservationDao::class, ObservationDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun observationDao(): ObservationDao = _observationDao.value
}
