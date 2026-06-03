package day.azimuth.observer.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ObservationDao_Impl(
  __db: RoomDatabase,
) : ObservationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfObservation: EntityInsertAdapter<Observation>
  init {
    this.__db = __db
    this.__insertAdapterOfObservation = object : EntityInsertAdapter<Observation>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `observations` (`id`,`signal_type`,`timestamp`,`latitude`,`longitude`,`accuracy`,`frequency_hz`,`timestamp_ns`,`payload`,`uploaded`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Observation) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.signalType)
        statement.bindLong(3, entity.timestamp)
        statement.bindDouble(4, entity.latitude)
        statement.bindDouble(5, entity.longitude)
        statement.bindDouble(6, entity.accuracy.toDouble())
        val _tmpFrequencyHz: Long? = entity.frequencyHz
        if (_tmpFrequencyHz == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpFrequencyHz)
        }
        val _tmpTimestampNs: Long? = entity.timestampNs
        if (_tmpTimestampNs == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpTimestampNs)
        }
        statement.bindText(9, entity.payload)
        val _tmp: Int = if (entity.uploaded) 1 else 0
        statement.bindLong(10, _tmp.toLong())
      }
    }
  }

  public override suspend fun insert(observation: Observation): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfObservation.insert(_connection, observation)
  }

  public override suspend fun insertAll(observations: List<Observation>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfObservation.insert(_connection, observations)
  }

  public override fun getRecent(limit: Int): Flow<List<Observation>> {
    val _sql: String = "SELECT * FROM observations ORDER BY timestamp DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("observations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signal_type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfAccuracy: Int = getColumnIndexOrThrow(_stmt, "accuracy")
        val _columnIndexOfFrequencyHz: Int = getColumnIndexOrThrow(_stmt, "frequency_hz")
        val _columnIndexOfTimestampNs: Int = getColumnIndexOrThrow(_stmt, "timestamp_ns")
        val _columnIndexOfPayload: Int = getColumnIndexOrThrow(_stmt, "payload")
        val _columnIndexOfUploaded: Int = getColumnIndexOrThrow(_stmt, "uploaded")
        val _result: MutableList<Observation> = mutableListOf()
        while (_stmt.step()) {
          val _item: Observation
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          val _tmpAccuracy: Float
          _tmpAccuracy = _stmt.getDouble(_columnIndexOfAccuracy).toFloat()
          val _tmpFrequencyHz: Long?
          if (_stmt.isNull(_columnIndexOfFrequencyHz)) {
            _tmpFrequencyHz = null
          } else {
            _tmpFrequencyHz = _stmt.getLong(_columnIndexOfFrequencyHz)
          }
          val _tmpTimestampNs: Long?
          if (_stmt.isNull(_columnIndexOfTimestampNs)) {
            _tmpTimestampNs = null
          } else {
            _tmpTimestampNs = _stmt.getLong(_columnIndexOfTimestampNs)
          }
          val _tmpPayload: String
          _tmpPayload = _stmt.getText(_columnIndexOfPayload)
          val _tmpUploaded: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfUploaded).toInt()
          _tmpUploaded = _tmp != 0
          _item =
              Observation(_tmpId,_tmpSignalType,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpFrequencyHz,_tmpTimestampNs,_tmpPayload,_tmpUploaded)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalCount(): Flow<Long> {
    val _sql: String = "SELECT COUNT(*) FROM observations"
    return createFlow(__db, false, arrayOf("observations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPendingUploadCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM observations WHERE uploaded = 0"
    return createFlow(__db, false, arrayOf("observations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCountByType(signalType: String): Flow<Long> {
    val _sql: String = "SELECT COUNT(*) FROM observations WHERE signal_type = ?"
    return createFlow(__db, false, arrayOf("observations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, signalType)
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUploadBatch(batchSize: Int): List<Observation> {
    val _sql: String =
        "SELECT * FROM observations WHERE uploaded = 0 ORDER BY timestamp ASC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, batchSize.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signal_type")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfAccuracy: Int = getColumnIndexOrThrow(_stmt, "accuracy")
        val _columnIndexOfFrequencyHz: Int = getColumnIndexOrThrow(_stmt, "frequency_hz")
        val _columnIndexOfTimestampNs: Int = getColumnIndexOrThrow(_stmt, "timestamp_ns")
        val _columnIndexOfPayload: Int = getColumnIndexOrThrow(_stmt, "payload")
        val _columnIndexOfUploaded: Int = getColumnIndexOrThrow(_stmt, "uploaded")
        val _result: MutableList<Observation> = mutableListOf()
        while (_stmt.step()) {
          val _item: Observation
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          val _tmpAccuracy: Float
          _tmpAccuracy = _stmt.getDouble(_columnIndexOfAccuracy).toFloat()
          val _tmpFrequencyHz: Long?
          if (_stmt.isNull(_columnIndexOfFrequencyHz)) {
            _tmpFrequencyHz = null
          } else {
            _tmpFrequencyHz = _stmt.getLong(_columnIndexOfFrequencyHz)
          }
          val _tmpTimestampNs: Long?
          if (_stmt.isNull(_columnIndexOfTimestampNs)) {
            _tmpTimestampNs = null
          } else {
            _tmpTimestampNs = _stmt.getLong(_columnIndexOfTimestampNs)
          }
          val _tmpPayload: String
          _tmpPayload = _stmt.getText(_columnIndexOfPayload)
          val _tmpUploaded: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfUploaded).toInt()
          _tmpUploaded = _tmp != 0
          _item =
              Observation(_tmpId,_tmpSignalType,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpFrequencyHz,_tmpTimestampNs,_tmpPayload,_tmpUploaded)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markUploaded(ids: List<Long>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("UPDATE observations SET uploaded = 1 WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in ids) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pruneUploaded(beforeTimestamp: Long) {
    val _sql: String = "DELETE FROM observations WHERE uploaded = 1 AND timestamp < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, beforeTimestamp)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
