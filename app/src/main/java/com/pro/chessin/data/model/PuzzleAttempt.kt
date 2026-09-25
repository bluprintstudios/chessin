package com.pro.chessin.data.model

/**
 * Domain model representing a puzzle attempt.
 */
data class PuzzleAttempt(
    val id: Long = 0,
    val puzzleId: String,
    val correct: Boolean,
    val timeTakenMs: Long,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val attemptedAt: Long = System.currentTimeMillis()
)
