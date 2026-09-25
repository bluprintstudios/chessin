package com.pro.chessin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a user attempt at a puzzle.
 */
@Entity(
    tableName = "puzzle_attempts",
    indices = [
        Index(value = ["puzzleId"]),
        Index(value = ["attemptedAt"])
    ]
)
data class PuzzleAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val puzzleId: String,
    val correct: Boolean,
    val timeTakenMs: Long,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val attemptedAt: Long = System.currentTimeMillis()
)
