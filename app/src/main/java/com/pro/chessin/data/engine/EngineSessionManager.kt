package com.pro.chessin.data.engine

import android.util.Log
import com.pro.chessin.domain.engine.EngineRepository
import com.pro.chessin.engine.NativeBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages a single active Stockfish engine session.
 * Implements the EngineRepository interface, ensuring all engine interactions
 * go through this controlled interface rather than calling NativeBridge directly.
 *
 * Rules:
 * - Only one active analysis at a time (mutex-guarded; concurrent commands wait/synchronize)
 * - When the owning coroutine scope is cancelled, sends UCI "stop" command
 * - Handles repeated start/stop cycles safely without leaking threads
 */
@Singleton
class EngineSessionManager @Inject constructor() : EngineRepository {

    private companion object {
        private const val TAG = "EngineSessionManager"
    }

    private val sessionMutex = Mutex()
    private var activeAnalysisJob: Job? = null
    private var engineStarted = false

    override val engineOutput: Flow<String>
        get() = NativeBridge.engineOutput
            .onEach { line ->
                Log.d(TAG, "Engine output received: $line")
            }
            .catch { e ->
                Log.e(TAG, "Error in engine output flow", e)
            }

    /**
     * Start the engine with the given NNUE network file path.
     */
    override suspend fun startEngine(nnuePath: String): Result<Unit> = sessionMutex.withLock {
        runCatching {
            Log.d(TAG, "startEngine() called with nnuePath: $nnuePath")
            
            if (engineStarted) {
                Log.w(TAG, "Engine already started, skipping redundant start call")
                return@runCatching Unit
            }

            try {
                NativeBridge.engineStart(nnuePath)
                engineStarted = true
                Log.d(TAG, "Engine started successfully")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start engine", e)
                throw e
            }
        }
    }

    /**
     * Send a UCI command to the engine (e.g., "position startpos", "go depth 20").
     */
    override suspend fun sendCommand(command: String): Result<Unit> = sessionMutex.withLock {
        runCatching {
            Log.d(TAG, "sendCommand(): '$command' (length: ${command.length})")
            
            if (!engineStarted) {
                throw IllegalStateException("Engine not started - call startEngine() first")
            }

            try {
                NativeBridge.engineSendCommand(command)
                Log.d(TAG, "Command sent successfully: '$command'")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send command: '$command'", e)
                throw e
            }
        }
    }

    /**
     * Stop the engine and clean up resources.
     * After this, must call startEngine() before using the engine again.
     */
    override suspend fun stopEngine(): Result<Unit> = sessionMutex.withLock {
        runCatching {
            Log.d(TAG, "stopEngine() called")

            // Stop any active analysis
            if (activeAnalysisJob != null) {
                Log.d(TAG, "Cancelling active analysis")
                activeAnalysisJob?.cancel()
                activeAnalysisJob = null
                
                // Send stop command to engine
                try {
                    NativeBridge.engineSendCommand("stop")
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending stop command", e)
                }
            }
            
            try {
                NativeBridge.engineStop()
                engineStarted = false
                Log.d(TAG, "Engine stopped successfully")
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop engine", e)
                throw e
            }
        }
    }

    /**
     * Clear the engine output buffer to prevent stale output from previous
     * analysis commands being replayed into subsequent analyses.
     * This creates a new MutableSharedFlow instance with empty replay buffer.
     * Must be called before each sequential MoveAnalyzer.analyze() call.
     */
    override fun clearOutputBuffer() {
        Log.d(TAG, "clearOutputBuffer() - resetting NativeBridge output buffer")
        NativeBridge.resetOutputBuffer()
    }

    /**
     * Clean up resources. Should be called from ViewModel.onCleared() or similar.
     */
    fun close() {
        Log.d(TAG, "close() called")
        
        // Stop any active analysis
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null

        // Stop the engine
        if (engineStarted) {
            try {
                NativeBridge.engineStop()
                engineStarted = false
                Log.d(TAG, "Engine cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}