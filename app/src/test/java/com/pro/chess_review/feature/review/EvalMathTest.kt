package com.pro.chess_review.feature.review

import org.junit.Assert.assertEquals
import org.junit.Test

class EvalMathTest {

    @Test
    fun calculateFillPercent_handlesMateCorrectly() {
        // Mate in 1 for White -> full white bar
        assertEquals(1f, EvalMath.calculateFillPercent(null, 1))
        assertEquals(1f, EvalMath.calculateFillPercent(null, 10))
        
        // Mate in 1 for Black -> full black bar (0f)
        assertEquals(0f, EvalMath.calculateFillPercent(null, -1))
        assertEquals(0f, EvalMath.calculateFillPercent(null, -5))
    }

    @Test
    fun calculateFillPercent_handlesCentipawnsCorrectly() {
        // Equal position
        assertEquals(0.5f, EvalMath.calculateFillPercent(0, null))
        
        // +1.0 for White
        assertEquals(0.5625f, EvalMath.calculateFillPercent(100, null))
        
        // -1.0 for Black
        assertEquals(0.4375f, EvalMath.calculateFillPercent(-100, null))
        
        // Capped at +8.0
        assertEquals(1f, EvalMath.calculateFillPercent(800, null))
        assertEquals(1f, EvalMath.calculateFillPercent(2000, null)) // exceeds cap
        
        // Capped at -8.0
        assertEquals(0f, EvalMath.calculateFillPercent(-800, null))
        assertEquals(0f, EvalMath.calculateFillPercent(-2000, null)) // exceeds cap
    }

    @Test
    fun calculateFillPercent_handlesNullsCorrectly() {
        assertEquals(0.5f, EvalMath.calculateFillPercent(null, null))
    }

    @Test
    fun formatScoreText_handlesMateAndCentipawns() {
        assertEquals("M1", EvalMath.formatScoreText(null, 1))
        assertEquals("M5", EvalMath.formatScoreText(null, -5))
        
        assertEquals("0.0", EvalMath.formatScoreText(0, null))
        assertEquals("+1.5", EvalMath.formatScoreText(150, null))
        assertEquals("-2.3", EvalMath.formatScoreText(-230, null))
        
        assertEquals("0.0", EvalMath.formatScoreText(null, null))
    }

    @Test
    fun contentDescription_namesTheSideAndHandlesMissingAnalysis() {
        assertEquals("Evaluation: +1.5. White is ahead", EvalMath.contentDescription(150, null))
        assertEquals("Evaluation: -2.3. Black is ahead", EvalMath.contentDescription(-230, null))
        assertEquals("Evaluation: Black mates in 3", EvalMath.contentDescription(null, -3))
        assertEquals("Evaluation unavailable", EvalMath.contentDescription(null, null))
    }

    @Test
    fun analysisProgressFraction_handlesEmptyAndOutOfRangeProgress() {
        assertEquals(0f, analysisProgressFraction(processedPositions = 0, totalPositions = 0))
        assertEquals(0f, analysisProgressFraction(processedPositions = -1, totalPositions = 4))
        assertEquals(1f, analysisProgressFraction(processedPositions = 5, totalPositions = 4))
        assertEquals(0.5f, analysisProgressFraction(processedPositions = 2, totalPositions = 4))
    }
}
