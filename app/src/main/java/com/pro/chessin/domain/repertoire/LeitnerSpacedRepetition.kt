package com.pro.chessin.domain.repertoire

import java.util.concurrent.TimeUnit

/**
 * Result of a spaced repetition update.
 */
data class SpacedRepetitionResult(
    val boxNumber: Int,
    val nextReviewAt: Long
)

/**
 * Implementation of the 5-box Leitner Spaced Repetition System.
 * 
 * Rules:
 * - Box 1: Review every 1 day
 * - Box 2: Review every 3 days
 * - Box 3: Review every 7 days
 * - Box 4: Review every 14 days
 * - Box 5: Review every 30 days
 * 
 * Correct answer: Promotes node to box min(5, currentBox + 1) with corresponding interval.
 * Wrong answer: Demotes node back to Box 1 with 1-day interval.
 */
object LeitnerSpacedRepetition {

    const val MIN_BOX = 1
    const val MAX_BOX = 5

    private val BOX_INTERVALS_MS = mapOf(
        1 to TimeUnit.DAYS.toMillis(1),
        2 to TimeUnit.DAYS.toMillis(3),
        3 to TimeUnit.DAYS.toMillis(7),
        4 to TimeUnit.DAYS.toMillis(14),
        5 to TimeUnit.DAYS.toMillis(30)
    )

    /**
     * Calculate interval in milliseconds for a given box number.
     */
    fun getIntervalMs(boxNumber: Int): Long {
        val clampedBox = boxNumber.coerceIn(MIN_BOX, MAX_BOX)
        return BOX_INTERVALS_MS[clampedBox] ?: TimeUnit.DAYS.toMillis(1)
    }

    /**
     * Promote node to next box on correct review.
     */
    fun promote(currentBox: Int, currentTimeMs: Long = System.currentTimeMillis()): SpacedRepetitionResult {
        val newBox = (currentBox + 1).coerceAtMost(MAX_BOX)
        val intervalMs = getIntervalMs(newBox)
        return SpacedRepetitionResult(
            boxNumber = newBox,
            nextReviewAt = currentTimeMs + intervalMs
        )
    }

    /**
     * Demote node back to Box 1 on wrong review.
     */
    fun demote(currentBox: Int, currentTimeMs: Long = System.currentTimeMillis()): SpacedRepetitionResult {
        val newBox = MIN_BOX
        val intervalMs = getIntervalMs(newBox)
        return SpacedRepetitionResult(
            boxNumber = newBox,
            nextReviewAt = currentTimeMs + intervalMs
        )
    }

    /**
     * Check if a node is currently due for review.
     */
    fun isDue(nextReviewAt: Long, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return nextReviewAt <= currentTimeMs
    }
}
