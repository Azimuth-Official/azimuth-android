package day.azimuth.observer.data.local

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
                gridY = random.nextInt(10)
            )
        }
        hexCoverageDao.upsert(updated)
    }

    suspend fun backfillCoverage(): Boolean {
        val allCoverages = try {
            hexCoverageDao.getAllSync()
        } catch (e: Exception) {
            return false
        }

        if (allCoverages.isNotEmpty() && allCoverages.all { it.gridX != null }) {
            return false
        }

        val chunkSize = 100
        var offset = 0
        val allObservations = mutableListOf<Observation>()

        while (true) {
            val chunk = try {
                observationDao.getAllForBackfillChunk(chunkSize, offset)
            } catch (e: Exception) {
                break
            }
            if (chunk.isEmpty()) break
            allObservations.addAll(chunk)
            offset += chunkSize
        }

        val grouped = allObservations.groupBy { hexIndexer.latLonToHex(it.latitude, it.longitude) }

        for ((h3, obsList) in grouped) {
            if (h3 == null) continue

            val existing = try { hexCoverageDao.getByH3(h3) } catch (e: Exception) { null }
            if (existing != null && existing.gridX != null) continue

            val firstSeen = obsList.minOf { it.timestamp }
            val lastSeen = obsList.maxOf { it.timestamp }
            val observationCount = obsList.size
            val pendingCount = obsList.count { !it.uploaded }
            val cellCount = obsList.count { it.signalType.startsWith("cell") }
            val gnssCount = obsList.count { it.signalType == "gnss_raw" }
            val wifiCount = obsList.count { it.signalType.startsWith("wifi") }
            val latestAccuracy = obsList.maxByOrNull { it.timestamp }?.accuracy

            val coverage = if (existing != null) {
                existing.copy(
                    firstSeen = minOf(existing.firstSeen, firstSeen),
                    lastSeen = maxOf(existing.lastSeen, lastSeen),
                    observationCount = existing.observationCount + observationCount,
                    pendingCount = existing.pendingCount + pendingCount,
                    cellCount = existing.cellCount + cellCount,
                    gnssCount = existing.gnssCount + gnssCount,
                    wifiCount = existing.wifiCount + wifiCount,
                    latestAccuracy = latestAccuracy,
                    gridX = random.nextInt(10),
                    gridY = random.nextInt(10)
                )
            } else {
                HexCoverage(
                    h3Index = h3,
                    firstSeen = firstSeen,
                    lastSeen = lastSeen,
                    observationCount = observationCount,
                    pendingCount = pendingCount,
                    cellCount = cellCount,
                    gnssCount = gnssCount,
                    wifiCount = wifiCount,
                    latestAccuracy = latestAccuracy,
                    gridX = random.nextInt(10),
                    gridY = random.nextInt(10)
                )
            }

            hexCoverageDao.upsert(coverage)
        }

        return true
    }
}
