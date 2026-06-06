package day.azimuth.observer.service.collectors

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import day.azimuth.observer.data.local.Observation
import day.azimuth.observer.data.local.ObservationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CellInfoCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observationRepository: ObservationRepository,
    private val locationProvider: LocationProvider,
    private val gson: Gson,
) {
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        job = scope.launch {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            while (isActive) {
                val location = locationProvider.getLastLocation()
                if (location != null) {
                    val cellInfoList = telephony.allCellInfo ?: emptyList()
                    val entries = cellInfoList.mapNotNull { cellInfo ->
                        extractCellEntry(cellInfo)
                    }
                    if (entries.isNotEmpty()) {
                        entries.map { (signalType, data) ->
                            Observation(
                                signalType = signalType,
                                timestamp = System.currentTimeMillis(),
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                payload = gson.toJson(data),
                            )
                        }.forEach { obs ->
                            observationRepository.recordObservation(obs)
                        }
                    }
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun extractCellEntry(cellInfo: CellInfo): Pair<String, Map<String, Any?>>? = when (cellInfo) {
        is CellInfoLte -> {
            val identity = cellInfo.cellIdentity
            val signal = cellInfo.cellSignalStrength
            "cell_lte" to mapOf(
                "ci" to identity.ci,
                "pci" to identity.pci,
                "tac" to identity.tac,
                "earfcn" to identity.earfcn,
                "mcc" to identity.mccString,
                "mnc" to identity.mncString,
                "rsrp" to signal.rsrp,
                "rsrq" to signal.rsrq,
                "rssi" to signal.rssi,
                "cqi" to signal.cqi,
                "timingAdvance" to signal.timingAdvance,
            )
        }
        is CellInfoNr -> {
            val identity = cellInfo.cellIdentity as android.telephony.CellIdentityNr
            val signal = cellInfo.cellSignalStrength as android.telephony.CellSignalStrengthNr
            "cell_nr" to mapOf(
                "nci" to identity.nci,
                "pci" to identity.pci,
                "tac" to identity.tac,
                "nrarfcn" to identity.nrarfcn,
                "mcc" to identity.mccString,
                "mnc" to identity.mncString,
                "ssRsrp" to signal.ssRsrp,
                "ssRsrq" to signal.ssRsrq,
                "ssSinr" to signal.ssSinr,
                "csiRsrp" to signal.csiRsrp,
                "csiRsrq" to signal.csiRsrq,
                "csiSinr" to signal.csiSinr,
            )
        }
        else -> null
    }

    companion object {
        private const val SCAN_INTERVAL_MS = 10_000L // 10 seconds
    }
}
