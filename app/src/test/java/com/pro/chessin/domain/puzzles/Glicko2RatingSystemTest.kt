package com.pro.chessin.domain.puzzles

import org.junit.Assert.assertTrue
import org.junit.Test

class Glicko2RatingSystemTest {

    @Test
    fun `solving puzzle increases user rating`() {
        val initial = GlickoRatingState(rating = 1500.0, ratingDeviation = 200.0, volatility = 0.06)
        val updated = Glicko2RatingSystem.calculateNewRating(
            userState = initial,
            puzzleRating = 1600.0,
            puzzleRD = 50.0,
            score = 1.0
        )

        assertTrue("Rating should increase on success", updated.rating > initial.rating)
        assertTrue("Rating deviation should decrease after attempt", updated.ratingDeviation < initial.ratingDeviation)
    }

    @Test
    fun `failing puzzle decreases user rating`() {
        val initial = GlickoRatingState(rating = 1500.0, ratingDeviation = 200.0, volatility = 0.06)
        val updated = Glicko2RatingSystem.calculateNewRating(
            userState = initial,
            puzzleRating = 1400.0,
            puzzleRD = 50.0,
            score = 0.0
        )

        assertTrue("Rating should decrease on failure", updated.rating < initial.rating)
    }

    @Test
    fun `high RD produces larger rating delta than low RD`() {
        val highRDState = GlickoRatingState(rating = 1500.0, ratingDeviation = 350.0, volatility = 0.06)
        val lowRDState = GlickoRatingState(rating = 1500.0, ratingDeviation = 50.0, volatility = 0.06)

        val updatedHigh = Glicko2RatingSystem.calculateNewRating(highRDState, 1600.0, 50.0, 1.0)
        val updatedLow = Glicko2RatingSystem.calculateNewRating(lowRDState, 1600.0, 50.0, 1.0)

        val deltaHigh = updatedHigh.rating - highRDState.rating
        val deltaLow = updatedLow.rating - lowRDState.rating

        assertTrue("High RD delta ($deltaHigh) should be larger than Low RD delta ($deltaLow)", deltaHigh > deltaLow)
    }

    @Test
    fun `solving 10 puzzles of increasing difficulty produces sensible rating curve`() {
        var current = GlickoRatingState(rating = 1500.0, ratingDeviation = 350.0, volatility = 0.06)
        val puzzleRatings = listOf(1400.0, 1450.0, 1500.0, 1550.0, 1600.0, 1650.0, 1700.0, 1750.0, 1800.0, 1850.0)

        for (puzzleRating in puzzleRatings) {
            val previousRating = current.rating
            current = Glicko2RatingSystem.calculateNewRating(current, puzzleRating, 50.0, 1.0)
            assertTrue("Rating should increase after solving puzzle", current.rating > previousRating)
        }

        assertTrue("Final rating should be significantly higher than 1500", current.rating > 1750.0)
    }
}
