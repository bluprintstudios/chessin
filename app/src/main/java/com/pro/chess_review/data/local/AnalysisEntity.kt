package com.pro.chess_review.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pro.chess_review.core.engine.EngineLine
import com.pro.chess_review.core.engine.EngineResult

@Entity(tableName = "analysis_cache")
data class AnalysisEntity(
    @PrimaryKey val fen: String,
    val engineId: String,
    val engineVersion: String,
    val searchDepth: Int,
    val multiPv: Int,
    // JSON serialization of EngineLine objects
    val linesJson: String,
    val analyzedAtEpochMillis: Long
)
