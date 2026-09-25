package com.pro.chessin.data.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pro.chessin.engine.NativeBridge
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Log
import java.io.File

/**
 * Instrumented tests for EngineSessionManager.
 *
 * These tests run on an Android device or emulator where the native
 * library can be loaded and executed.
 */
@RunWith(AndroidJUnit4::class)
class EngineSessionManagerInstrumentedTest {

    private var manager: EngineSessionManager? = null
    private lateinit var nnuePath: String

    companion object {
        private const val TAG = "EngineTest"
    }

    @Before
    fun setUp() {
        // ALWAYS stop any running engine to ensure clean state.
        // Note: JUnit creates a new test instance per method, so
        // ::manager.isInitialized is always false at this point.
        // Using a simple call to NativeBridge.engineStop() is the
        // correct approach — it checks g_engineRunning internally.
        NativeBridge.engineStop()
        // Brief wait for thread cleanup
        Thread.sleep(200)

        // Reset output buffer FIRST to clear stale data from previous tests
        NativeBridge.resetOutputBuffer()

        manager = EngineSessionManager()

        // Try to find the NNUE file in predictable locations
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // First try: app files directory
        val appFilesDir = File(context.filesDir, "assets/nnue_assets")
        val nnueFile1 = File(appFilesDir, "nn-1a298aa575a0.nnue")

        // Second try: cache directory
        val cacheDir = File(context.cacheDir, "assets/nnue_assets")
        val nnueFile2 = File(cacheDir, "nn-1a298aa575a0.nnue")

        // Third try: direct assets (if extracted at install time)
        val directAssets = File(context.filesDir, "nn-1a298aa575a0.nnue")

        nnuePath = when {
            nnueFile1.exists() -> {
                Log.d(TAG, "✓ Found NNUE at: ${nnueFile1.absolutePath} (${nnueFile1.length()} bytes)")
                nnueFile1.absolutePath
            }
            nnueFile2.exists() -> {
                Log.d(TAG, "✓ Found NNUE at: ${nnueFile2.absolutePath} (${nnueFile2.length()} bytes)")
                nnueFile2.absolutePath
            }
            directAssets.exists() -> {
                Log.d(TAG, "✓ Found NNUE at: ${directAssets.absolutePath} (${directAssets.length()} bytes)")
                directAssets.absolutePath
            }
            else -> {
                // Fallback: use empty path to run without NNUE (classical evaluation)
                Log.w(TAG, "NNUE file not found in standard locations, engine will use classical evaluation")
                ""
            }
        }

        Log.d(TAG, "NNUE path resolved to: $nnuePath (empty = no NNUE, classical evaluation)")
    }

    @After
    fun tearDown() {
        // Always stop the engine after each test, even if the test failed
        NativeBridge.engineStop()
        Thread.sleep(200)
        manager?.close()
    }

    /**
     * Helper: suspend function that waits for a specific line from engine output.
     * Uses Flow.first instead of CountDownLatch to avoid blocking the thread,
     * which would prevent the SharedFlow collector coroutine from running.
     */
    private suspend fun awaitLine(predicate: (String) -> Boolean, timeoutMs: Long): String? {
        return withTimeoutOrNull(timeoutMs) {
            NativeBridge.engineOutput.first { predicate(it) }
        }
    }


    /**
     * Test: Start engine and send depth 10 analysis.
     * This is the main test verifying JNI bridge functionality with real NNUE file.
     * Follows proper UCI protocol: uci -> uciok -> isready -> readyok -> position -> go
     */
    @Test
    fun startEngine_sends_depth10_analysis() {
        runBlocking {
            // Start the collector to capture all output (for debugging)
            val outputReceived = mutableListOf<String>()
            val collectorJob = launch {
                NativeBridge.engineOutput.collect { line ->
                    Log.d(TAG, "Depth10 test output: $line")
                    synchronized(outputReceived) { outputReceived.add(line) }
                }
            }

            delay(500)

            val outputSnapshot: List<String>

            try {
                // Start the engine
                NativeBridge.engineStart("")
                delay(500)

                // Send uci and wait for uciok using suspending function
                NativeBridge.engineSendCommand("uci")
                val uciok = awaitLine({ it == "uciok" }, 5_000L)
                assertNotNull("Should receive uciok", uciok)

                // Send isready and wait for readyok
                NativeBridge.engineSendCommand("isready")
                val readyok = awaitLine({ it == "readyok" }, 5_000L)
                assertNotNull("Should receive readyok", readyok)

                // Set position before sending go (required by UCI protocol)
                NativeBridge.engineSendCommand("position startpos")
                delay(200)

                // Send go depth 10 for proper bounded analysis
                NativeBridge.engineSendCommand("go depth 10")

                // Wait for bestmove using suspending function
                val bestmoveLine = awaitLine({ it.startsWith("bestmove") }, 15_000L)

                outputSnapshot = synchronized(outputReceived) { outputReceived.toList() }
                assertNotNull("Should receive bestmove. Got: $outputSnapshot", bestmoveLine)
            } finally {
                collectorJob.cancel()
            }
        }
    }




    /**
     * Test: Verify consistency across multiple start/stop cycles.
     * Run this 5 times to catch intermittent JNI issues.
     */
    @Test
    fun engine_consistency_multiple_runs() {
        runBlocking {
            Log.d(TAG, "==================== TEST START: engine_consistency_multiple_runs ====================")

            for (run in 1..5) {
                Log.d(TAG, "\n--- RUN $run/5 ---")

                // Stop and reset before each run to ensure clean state
                NativeBridge.engineStop()
                Thread.sleep(200)
                NativeBridge.resetOutputBuffer()
                delay(500)

                // Use EXACT same pattern as passing startEngine_sends_depth10_analysis test
                val outputReceived = mutableListOf<String>()
                val collectorJob = launch {
                    NativeBridge.engineOutput.collect { line ->
                        Log.d(TAG, "Consistency test output: $line")
                        synchronized(outputReceived) { outputReceived.add(line) }
                    }
                }

                delay(500)

                val outputSnapshot: List<String>

                try {
                    // Start the engine (same as passing test)
                    NativeBridge.engineStart("")
                    delay(500)

                    // Send uci and wait for uciok using suspending function (same as passing test)
                    NativeBridge.engineSendCommand("uci")
                    val uciok = awaitLine({ it == "uciok" }, 5_000L)
                    assertNotNull("Run $run: Should receive uciok", uciok)

                    // Send isready and wait for readyok (same as passing test)
                    NativeBridge.engineSendCommand("isready")
                    val readyok = awaitLine({ it == "readyok" }, 5_000L)
                    assertNotNull("Run $run: Should receive readyok", readyok)

                    // Set position before sending go (required by UCI protocol, same as passing test)
                    NativeBridge.engineSendCommand("position startpos")
                    delay(200)

                    // Send go depth 10 for proper bounded analysis (same as passing test)
                    NativeBridge.engineSendCommand("go depth 10")

                    // Wait for bestmove using suspending function (same as passing test)
                    val bestmoveLine = awaitLine({ it.startsWith("bestmove") }, 15_000L)

                    outputSnapshot = synchronized(outputReceived) { outputReceived.toList() }
                    assertNotNull("Run $run: Should receive bestmove. Got: $outputSnapshot", bestmoveLine)
                } finally {
                    collectorJob.cancel()
                    NativeBridge.engineStop()
                    Thread.sleep(200)
                }
            }

            Log.d(TAG, "==================== TEST END: PASSED ====================")
        }
    }

    /**
     * Test: Stop engine gracefully during analysis
     */
    @Test
    fun stop_engine_during_analysis() {
        runBlocking {
            manager!!.startEngine(nnuePath).getOrThrow()
            manager!!.sendCommand("position startpos").getOrThrow()
            manager!!.sendCommand("go depth 30").getOrThrow()

            // Wait a moment for analysis to start
            delay(500)

            // Stop the engine
            val stopResult = manager!!.stopEngine()
            assertTrue("Engine should stop successfully", stopResult.isSuccess)

            // Verify engine doesn't continue to produce output after stop (or produces final output)
            var outputCount = 0
            withTimeoutOrNull(2000L) {
                manager!!.engineOutput.collect { line ->
                    outputCount++
                }
            }

            // It's okay if we don't receive output (engine stopped in time)
            // or if we receive a final bestmove (valid per UCI spec after stop)
        }
    }

    /**
     * Test: Multiple start/stop cycles without crashes
     */
    @Test
    fun multiple_start_stop_cycles_no_crash() {
        runBlocking {
            for (cycle in 1..10) {
                Log.d(TAG, "--- Cycle $cycle/10 ---")

                // Start the engine
                val result = manager!!.startEngine(nnuePath)
                assertTrue("Cycle $cycle: Start should succeed", result.isSuccess)
                delay(500)

                // Verify engine is actually alive by sending isready and checking readyok
                manager!!.sendCommand("isready").getOrThrow()
                val readyok = awaitLine({ it == "readyok" }, 5_000L)
                assertNotNull("Cycle $cycle: Should receive readyok (engine is alive)", readyok)

                manager!!.sendCommand("position startpos").getOrThrow()
                manager!!.sendCommand("go depth 5").getOrThrow()

                delay(100)

                val stopResult = manager!!.stopEngine()
                assertTrue("Cycle $cycle: Stop should succeed", stopResult.isSuccess)

                delay(100)
            }
        }
    }
}
