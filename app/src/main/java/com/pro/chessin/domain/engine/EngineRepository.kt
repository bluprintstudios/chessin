package com.pro.chessin.domain.engine

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for all Stockfish engine interactions.
 * All native/engine calls must go through this interface - never call
 * native methods directly from ViewModels or Composables.
 */
interface EngineRepository {
    /**
     * Flow of UCI output lines from the engine
     */
    val engineOutput: Flow<String>

    /**
     * Start the engine with the given NNUE network file path
     */
    suspend fun startEngine(nnuePath: String): Result<Unit>

    /**
     * Send a UCI command to the engine
     */
    suspend fun sendCommand(command: String): Result<Unit>

    /**
     * Stop the engine and clean up resources
     */
    suspend fun stopEngine(): Result<Unit>

    /**
     * Clear the engine output buffer to prevent stale output from previous
     * analysis commands from being replayed into subsequent analyses.
     *
     * This calls NativeBridge.resetOutputBuffer() which creates a fresh
     * MutableSharedFlow instance. Call this between sequential MoveAnalyzer.analyze()
     * calls to avoid the SharedFlow replay=10 buffer containing stale data
     * from previous analysis sessions.
     */
    fun clearOutputBuffer()
}