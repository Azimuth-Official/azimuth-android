package day.azimuth.observer.service.ntrip

import android.util.Log
import day.azimuth.observer.data.local.NtripConfig
import day.azimuth.observer.data.local.NtripConnectionState
import day.azimuth.observer.data.local.NtripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.Base64
import kotlin.math.min

class NtripClient {
    private var socket: Socket? = null
    private var inputReader: BufferedReader? = null
    private var outputWriter: BufferedWriter? = null

    private val _status = MutableStateFlow(NtripStatus())
    val status: StateFlow<NtripStatus> = _status.asStateFlow()

    suspend fun connect(config: NtripConfig) {
        try {
            _status.value = _status.value.copy(
                state = NtripConnectionState.CONNECTING,
                errorMessage = null
            )

            socket = Socket(config.casterUrl, config.casterPort).apply {
                soTimeout = 30000 // 30s read timeout
            }

            inputReader = BufferedReader(InputStreamReader(socket!!.inputStream))
            outputWriter = BufferedWriter(OutputStreamWriter(socket!!.outputStream))

            // Send NTRIP v2 request with Basic auth
            val auth = Base64.getEncoder().encodeToString(
                "${config.username}:${config.password}".toByteArray()
            )
            outputWriter!!.write(
                "GET /${config.mountpoint} HTTP/1.1\r\n" +
                "Host: ${config.casterUrl}\r\n" +
                "Authorization: Basic $auth\r\n" +
                "Ntrip-Version: Ntrip/2.0\r\n" +
                "User-Agent: NTRIP AzimuthClient/1.0\r\n" +
                "Connection: close\r\n" +
                "\r\n"
            )
            outputWriter!!.flush()

            // Read response
            var responseLine = inputReader!!.readLine()
            if (responseLine == null) {
                throw Exception("No response from caster")
            }

            val statusCode = Regex("\\b(\\d{3})\\b").find(responseLine)
                ?.groupValues?.get(1)?.toIntOrNull() ?: 0

            // Read response headers
            var header = inputReader!!.readLine()
            while (header != null && header.isNotEmpty()) {
                header = inputReader!!.readLine()
            }

            if (statusCode == 200) {
                _status.value = _status.value.copy(
                    state = NtripConnectionState.STREAMING,
                    bytesReceived = 0,
                    messagesDecoded = 0
                )
                Log.i(TAG, "Connected to NTRIP caster: ${config.providerName}")
            } else {
                val errorMsg = when (statusCode) {
                    401 -> "Unauthorized (401): check username/password"
                    403 -> "Forbidden (403): access denied — check credentials or caster policy"
                    404 -> "Mountpoint not found (404): '${config.mountpoint}' does not exist"
                    409 -> "Conflict (409): mountpoint '${config.mountpoint}' already in use"
                    503 -> "Service unavailable (503): caster overloaded or in maintenance"
                    else -> "NTRIP error: HTTP $statusCode"
                }
                _status.value = _status.value.copy(
                    state = NtripConnectionState.ERROR,
                    errorMessage = errorMsg
                )
                disconnect()
                throw Exception(errorMsg)
            }
        } catch (e: Exception) {
            _status.value = _status.value.copy(
                state = NtripConnectionState.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
            Log.e(TAG, "NTRIP connection failed: ${e.message}")
            disconnect()
            throw e
        }
    }

    fun disconnect() {
        try {
            inputReader?.close()
            outputWriter?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during disconnect: ${e.message}")
        }
        socket = null
        inputReader = null
        outputWriter = null
        _status.value = _status.value.copy(
            state = NtripConnectionState.DISCONNECTED
        )
    }

    fun sendGga(latitude: Double, longitude: Double, altitude: Double) {
        try {
            if (socket == null || !socket!!.isConnected) {
                Log.w(TAG, "Socket not connected, cannot send GGA")
                return
            }

            // Format NMEA GGA sentence
            val latDir = if (latitude >= 0) "N" else "S"
            val lonDir = if (longitude >= 0) "E" else "W"
            val latAbs = kotlin.math.abs(latitude)
            val lonAbs = kotlin.math.abs(longitude)

            val latDeg = latAbs.toInt()
            val latMin = (latAbs - latDeg) * 60
            val lonDeg = lonAbs.toInt()
            val lonMin = (lonAbs - lonDeg) * 60

            val ggaBody = String.format(
                "GPGGA,%02d%07.4f,%s,%03d%07.4f,%s,1,04,1.0,%.1f,M,0.0,M,,",
                latDeg, latMin, latDir,
                lonDeg, lonMin, lonDir,
                altitude
            )
            val ggaSentence = "\$${ggaBody}*${nmeaChecksum(ggaBody)}"

            outputWriter?.write("$ggaSentence\r\n")
            outputWriter?.flush()
            Log.d(TAG, "Sent GGA sentence")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send GGA: ${e.message}")
        }
    }

    suspend fun readRtcmData(bufferSize: Int = 4096): ByteArray? {
        return try {
            if (socket == null || !socket!!.isConnected) {
                return null
            }

            val buffer = ByteArray(bufferSize)
            val bytesRead = socket!!.inputStream.read(buffer)
            if (bytesRead <= 0) {
                return null
            }

            _status.value = _status.value.copy(
                bytesReceived = _status.value.bytesReceived + bytesRead,
                lastMessageTime = System.currentTimeMillis()
            )

            buffer.take(bytesRead).toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading RTCM data: ${e.message}")
            _status.value = _status.value.copy(
                state = NtripConnectionState.ERROR,
                errorMessage = "Read error: ${e.message}"
            )
            disconnect()
            null
        }
    }

    fun isConnected(): Boolean = socket != null && socket!!.isConnected

    private fun nmeaChecksum(body: String): String {
        var cs = 0
        for (ch in body) {
            cs = cs xor ch.code
        }
        return "%02X".format(cs)
    }

    companion object {
        private const val TAG = "NtripClient"
    }
}
