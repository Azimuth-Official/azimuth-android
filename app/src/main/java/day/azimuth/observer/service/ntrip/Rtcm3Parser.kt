package day.azimuth.observer.service.ntrip

import android.util.Log

data class Rtcm3Message(
    val messageType: Int,
    val payload: ByteArray,
    val length: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rtcm3Message) return false

        if (messageType != other.messageType) return false
        if (!payload.contentEquals(other.payload)) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageType
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + length
        return result
    }
}

class Rtcm3Parser {
    private val crc24Table = buildCrc24Table()

    fun parseMessage(data: ByteArray): List<Rtcm3Message> {
        val messages = mutableListOf<Rtcm3Message>()
        var index = 0

        while (index < data.size) {
            // Scan for preamble 0xD3
            while (index < data.size && data[index].toInt() and 0xFF != 0xD3) {
                index++
            }

            if (index >= data.size) break

            // Found preamble, read length
            if (index + 2 >= data.size) break

            val length = ((data[index + 1].toInt() and 0x03) shl 8) or (data[index + 2].toInt() and 0xFF)
            if (length < 1 || index + 6 + length > data.size) break

            // Extract payload and CRC
            val payload = data.copyOfRange(index + 3, index + 3 + length)
            val crcBytes = data.copyOfRange(index + 3 + length, index + 6 + length)

            val crc24 = ((crcBytes[0].toInt() and 0xFF) shl 16) or
                ((crcBytes[1].toInt() and 0xFF) shl 8) or
                (crcBytes[2].toInt() and 0xFF)

            // Verify CRC
            val computedCrc = computeCrc24(data.copyOfRange(index, index + 3 + length))
            if (computedCrc != crc24) {
                Log.w(TAG, "CRC mismatch at offset $index")
                index += 3
                continue
            }

            // Extract message type (12 bits from first 2 bytes)
            val messageType = ((payload[0].toInt() and 0xFF) shl 4) or
                ((payload[1].toInt() and 0xF0) shr 4)

            messages.add(
                Rtcm3Message(
                    messageType = messageType,
                    payload = payload,
                    length = length
                )
            )

            index += 6 + length
        }

        return messages
    }

    private fun computeCrc24(data: ByteArray): Int {
        var crc = 0
        for (byte in data) {
            val tmp = ((byte.toInt() and 0xFF) xor (crc shr 16)) and 0xFF
            crc = ((crc shl 8) xor crc24Table[tmp]) and 0xFFFFFF
        }
        return crc
    }

    private fun buildCrc24Table(): IntArray {
        val table = IntArray(256)
        val poly = 0x1864CFB

        for (i in 0..255) {
            var crc = i shl 16
            for (j in 0..7) {
                crc = crc shl 1
                if ((crc and 0x1000000) != 0) {
                    crc = (crc xor poly) and 0xFFFFFF
                }
            }
            table[i] = crc
        }

        return table
    }

    companion object {
        private const val TAG = "Rtcm3Parser"
    }
}
