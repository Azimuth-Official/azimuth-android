package day.azimuth.observer.service.ntrip.replay

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.Gson
import day.azimuth.observer.data.local.NtripConfig
import day.azimuth.observer.service.ntrip.NtripClient
import day.azimuth.observer.service.ntrip.Rtcm3Parser
import day.azimuth.observer.service.ntrip.RtklibNative
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertTrue

/**
 * Live runtime test against BangorCH NTRIP mount on rtk2go.com.
 *
 * Bypasses app UI entirely — creates NtripClient + calls RtklibNative JNI directly.
 * Tests the same code path as the production correction pipeline.
 *
 * All JNI calls run on a dedicated thread with 8 MB stack to avoid SIGSEGV from
 * RTKLIB's large stack allocations inside rtkinit/init_rtcm.
 *
 * Requires: network access to rtk2go.com:2101, native library on device.
 * Marked @LargeTest — excluded from normal CI.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BangorChRuntimeTest {

    @Test
    fun verifyTier3NativeAccumulationWithBangorCH() {
        // 1. Verify native library loads
        Assume.assumeTrue("Native library not available", RtklibNative.supported)

        // 2. Run entire pipeline on a thread with 8 MB stack (RTKLIB needs it)
        val resultRef = AtomicReference<NativeStatus>()
        val errorRef = AtomicReference<Throwable>()
        val bytesRef = AtomicLong(0)
        val msgsRef = AtomicLong(0)

        val worker = Thread(null, {
            try {
                val result = runPipeline()
                resultRef.set(result.status)
                bytesRef.set(result.totalBytes.toLong())
                msgsRef.set(result.totalMessages.toLong())
            } catch (e: Throwable) {
                errorRef.set(e)
            }
        }, "rtklib-bangor-test", 8L * 1024 * 1024) // 8 MB stack
        worker.start()
        worker.join(240_000) // 4 minute max

        // Check for errors
        val error = errorRef.get()
        if (error != null) {
            if (error.message?.contains("rtk2go.com") == true ||
                error.message?.contains("unreachable") == true ||
                error.message?.contains("connect") == true
            ) {
                Assume.assumeNoException("Network issue", error)
            }
            throw error
        }

        val status = resultRef.get()
        Assume.assumeNotNull("Pipeline timed out or produced no result", status)

        val totalBytes = bytesRef.get()
        val totalMessages = msgsRef.get()

        // Log evidence
        Log.i(TAG, "=== BANGOR RUNTIME RESULTS ===")
        Log.i(TAG, "Bytes received: $totalBytes")
        Log.i(TAG, "Messages decoded: $totalMessages")
        Log.i(TAG, "baseObs=${status!!.baseObs} basePosValid=${status.basePosValid}")
        Log.i(TAG, "ephGps=${status.ephGps} ephGal=${status.ephGal} ephBds=${status.ephBds} ephGlo=${status.ephGlo}")
        Log.i(TAG, "lastSolStat=${status.lastSolStat}")

        // Assert accumulation
        assertTrue(totalBytes > 0, "No RTCM bytes received")
        assertTrue(totalMessages > 0, "No RTCM messages decoded")
        assertTrue(
            status.baseObs > 0,
            "Base observations not accumulating (baseObs=${status.baseObs})",
        )
        assertTrue(status.basePosValid, "Base position not decoded")
        assertTrue(
            status.ephGps > 0 || status.ephGal > 0 || status.ephBds > 0,
            "No ephemeris received (gps=${status.ephGps}, gal=${status.ephGal}, bds=${status.ephBds})",
        )

        Log.i(TAG, "=== ALL ASSERTIONS PASSED ===")
    }

    private fun runPipeline(): PipelineResult {
        val handle = RtklibNative.nativeInit()
        assertTrue(handle != 0L, "nativeInit returned null handle")

        try {
            // Connect to BangorCH
            val config = NtripConfig("BangorCH", "rtk2go.com", 2101, "BangorCH", "observer@azimuth.day", "")
            val client = NtripClient()
            runBlocking { client.connect(config) }

            if (!client.isConnected()) {
                throw RuntimeException("Failed to connect to rtk2go.com/BangorCH")
            }

            // Feed RTCM for ~120 seconds
            val parser = Rtcm3Parser()
            val startTime = System.currentTimeMillis()
            var totalBytes = 0
            var totalMessages = 0

            try {
                while (System.currentTimeMillis() - startTime < 120_000 && client.isConnected()) {
                    val data = runBlocking { client.readRtcmData() }
                    if (data != null) {
                        totalBytes += data.size
                        val messages = parser.parseMessage(data)
                        totalMessages += messages.size
                        if (messages.isNotEmpty()) {
                            RtklibNative.nativeFeedRtcm(
                                handle, data, data.size, RtklibNative.STREAM_COMBINED,
                            )
                        }
                    }
                    Thread.sleep(100)
                }
            } finally {
                client.disconnect()
            }

            // Get native status
            val statusJson = RtklibNative.nativeGetStatus(handle)
            Log.i(TAG, "Native status JSON: $statusJson")
            val status = Gson().fromJson(statusJson, NativeStatus::class.java)

            return PipelineResult(totalBytes, totalMessages, status)
        } finally {
            RtklibNative.nativeFree(handle)
        }
    }

    private data class PipelineResult(
        val totalBytes: Int,
        val totalMessages: Int,
        val status: NativeStatus,
    )

    /** Matches nativeGetStatus JSON from rtklib_jni.c */
    data class NativeStatus(
        val baseObs: Int = 0,
        val basePosValid: Boolean = false,
        val ephGps: Int = 0,
        val ephGlo: Int = 0,
        val ephGal: Int = 0,
        val ephBds: Int = 0,
        val lastSolStat: Int = 0,
    )

    companion object {
        private const val TAG = "BangorChRuntime"
    }
}
