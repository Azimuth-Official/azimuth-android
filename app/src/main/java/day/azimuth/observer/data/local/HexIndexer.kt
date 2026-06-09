package day.azimuth.observer.data.local

import com.uber.h3core.H3Core

/**
 * Abstraction for hex indexing to support local coverage map.
 * Prefers real H3 (com.uber:h3) if available and runtime succeeds.
 * Falls back to labeled temporary approximate grid indexer if H3 native/runtime fails.
 * Output is always coarse (res ~8 equivalent); never raw coordinates.
 * TEMPORARY FALLBACK: if used, UI and verdict must label as approximate grid, not true H3.
 */
interface HexIndexer {
    fun latLonToHex(lat: Double, lon: Double, resolution: Int = DEFAULT_RES): String?

    companion object {
        const val DEFAULT_RES = 8
    }
}

class HexIndexerImpl : HexIndexer {
    private val h3: H3Core? = try {
        H3Core.newInstance()
    } catch (e: Throwable) {
        null // will use fallback
    }

    override fun latLonToHex(lat: Double, lon: Double, resolution: Int): String? {
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
        if (java.lang.Double.isNaN(lat) || java.lang.Double.isNaN(lon)) return null

        if (h3 != null) {
            try {
                val cell = h3.latLngToCell(lat, lon, resolution)
                return h3.h3ToString(cell)
            } catch (e: Exception) {
                // fall through to grid fallback
            }
        }

        // TEMPORARY FALLBACK GRID INDEXER - approximate coverage only, not true H3
        // Grid size chosen to approximate res 8 (~0.46 km cells)
        val scale = 0.00417
        val latBucket = (lat / scale).toInt()
        // Apply latitude-dependent cosine correction to longitude bucket to match MapScreen rendering
        val cosLat = Math.cos(Math.toRadians(lat))
        val lonBucket = (lon / (scale / cosLat)).toInt()
        return "grid8:$latBucket:$lonBucket"
    }
}
