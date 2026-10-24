package com.pro.chess_review.core.engine

import kotlinx.coroutines.flow.Flow

data class EngineSettings(
    val multiPv: Int = 5,
    val searchDepth: Int = 18,
    val timeLimitMs: Long = 2000L
)

data class EngineLine(
    val uciMoves: List<String>,
    val sanMoves: List<String>,
    val centipawns: Int?,
    val mateIn: Int?,
    val depth: Int
)

data class EngineResult(
    val fen: String,
    val lines: List<EngineLine>,
    val engineId: String,
    val engineVersion: String,
    val isComplete: Boolean,
)

interface ChessEngine {
    val engineId: String
    val engineVersion: String

    suspend fun start()
    suspend fun stop()
    
    /**
     * Analyzes a position. Returns a Flow of incremental results.
     */
    fun analyze(fen: String, settings: EngineSettings): Flow<EngineResult>
}
