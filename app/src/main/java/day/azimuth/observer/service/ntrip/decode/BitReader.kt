package day.azimuth.observer.service.ntrip.decode

/**
 * MSB-first bitfield reader over a ByteArray.
 * Supports unsigned and signed (two's complement) extraction of arbitrary bit widths
 * across byte boundaries.
 */
class BitReader(private val data: ByteArray) {

    /** Current bit position (0-based from MSB of first byte). */
    var position: Int = 0
        private set

    /** Total bits available. */
    val totalBits: Int get() = data.size * 8

    /** Remaining bits from current position. */
    val remainingBits: Int get() = totalBits - position

    /**
     * Read [bits] bits as an unsigned Long value (MSB-first).
     * @param bits number of bits to read (1..64)
     * @return unsigned value
     * @throws IllegalArgumentException if bits < 1 or > 64
     * @throws IllegalStateException if not enough bits remain
     */
    fun readUnsigned(bits: Int): Long {
        require(bits in 1..64) { "bits must be 1..64, got $bits" }
        check(remainingBits >= bits) { "Not enough bits: need $bits, have $remainingBits" }

        var value = 0L
        var remaining = bits

        while (remaining > 0) {
            val byteIndex = position / 8
            val bitOffset = position % 8
            val bitsAvailableInByte = 8 - bitOffset
            val bitsToRead = minOf(remaining, bitsAvailableInByte)

            val mask = ((1 shl bitsToRead) - 1)
            val shift = bitsAvailableInByte - bitsToRead
            val extracted = (data[byteIndex].toInt() ushr shift) and mask

            value = (value shl bitsToRead) or extracted.toLong()
            position += bitsToRead
            remaining -= bitsToRead
        }

        return value
    }

    /**
     * Read [bits] bits as a signed Long value (two's complement, MSB-first).
     * @param bits number of bits to read (1..64)
     * @return signed value
     */
    fun readSigned(bits: Int): Long {
        val raw = readUnsigned(bits)
        val signBit = 1L shl (bits - 1)
        return if (raw and signBit != 0L) {
            raw - (1L shl bits)
        } else {
            raw
        }
    }

    /**
     * Read a single bit as a Boolean (1 = true, 0 = false).
     */
    fun readBool(): Boolean = readUnsigned(1) == 1L

    /**
     * Skip [bits] bits without reading.
     */
    fun skip(bits: Int) {
        check(remainingBits >= bits) { "Not enough bits to skip: need $bits, have $remainingBits" }
        position += bits
    }

    /**
     * Count set bits in a mask read of [bits] width.
     * Position advances past the mask. Returns the mask value and the count.
     */
    fun readMask(bits: Int): Pair<Long, Int> {
        val mask = readUnsigned(bits)
        var count = 0
        var v = mask
        while (v != 0L) {
            count++
            v = v and (v - 1)
        }
        return mask to count
    }

    /**
     * Return indices of set bits in a mask (MSB = index 1, LSB = index [bits]).
     * For RTCM satellite masks where bit N corresponds to satellite N.
     */
    fun maskToIndices(mask: Long, bits: Int): List<Int> {
        val indices = mutableListOf<Int>()
        for (i in 0 until bits) {
            if (mask and (1L shl (bits - 1 - i)) != 0L) {
                indices.add(i + 1) // 1-based satellite/signal IDs
            }
        }
        return indices
    }
}
