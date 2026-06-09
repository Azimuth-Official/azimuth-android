package day.azimuth.observer.data.local

import androidx.room.Transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central write path for observations + coverage update.
 * Existing direct DAO uses (Dashboard, UploadWorker, etc.) remain unchanged to avoid regression.
 * Collectors will use this for new inserts.
 */
@Singleton
class ObservationRepository @Inject constructor(
    private val observationDao: ObservationDao,
    private val hexCoverageDao: HexCoverageDao,
    private val hexIndexer: HexIndexer
) {
    private val random = java.util.Random()

    @Transaction
    suspend fun recordObservation(observation: Observation) {
        // Always insert the raw observation (source of truth)
        observationDao.insert(observation)

        val h3 = hexIndexer.latLonToHex(observation.latitude, observation.longitude) ?: return

        val now = observation.timestamp
        val isPending = !observation.uploaded
        val signal = observation.signalType
        val acc = observation.accuracy

        val current = hexCoverageDao.getByH3(h3)
        val updated = if (current != null) {
            current.copy(
                lastSeen = maxOf(current.lastSeen, now),
                observationCount = current.observationCount + 1,
                pendingCount = current.pendingCount + if (isPending) 1 else 0,
                cellCount = current.cellCount + if (signal.startsWith("cell")) 1 else 0,
                gnssCount = current.gnssCount + if (signal == "gnss_raw") 1 else 0,
                wifiCount = current.wifiCount + if (signal.startsWith("wifi")) 1 else 0,
                latestAccuracy = acc
            )
        } else {
            HexCoverage(
                h3Index = h3,
                firstSeen = now,
                lastSeen = now,
                observationCount = 1,
                pendingCount = if (isPending) 1 else 0,
                cellCount = if (signal.startsWith("cell")) 1 else 0,
                gnssCount = if (signal == "gnss_raw") 1 else 0,
                wifiCount = if (signal.startsWith("wifi")) 1 else 0,
                latestAccuracy = acc,
                gridX = random.nextInt(10),
                gridY = random.nextInt(10),
                tier = "OWN"
            )
        }
        hexCoverageDao.upsert(updated)
    }
}
