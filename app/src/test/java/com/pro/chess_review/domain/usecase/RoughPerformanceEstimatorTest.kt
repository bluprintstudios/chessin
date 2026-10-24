package com.pro.chess_review.domain.usecase

import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.ChessSide
import com.pro.chess_review.domain.model.GameParseStatus
import com.pro.chess_review.domain.model.GameSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoughPerformanceEstimatorTest {

    @Test
    fun estimate_regularizesAWinAndReturnsBothSides() {
        val estimates = RoughPerformanceEstimator.estimate(game(whiteRating = 1500, blackRating = 1600, result = "1-0"))

        assertEquals(2, estimates.size)
        val white = estimates.single { it.side == ChessSide.WHITE }
        val black = estimates.single { it.side == ChessSide.BLACK }
        assertEquals(1791, white.pointEstimate)
        assertEquals(1441, white.lowerBound)
        assertEquals(2141, white.upperBound)
        assertEquals(0, white.score.compareTo(1.0))
        assertEquals(1309, black.pointEstimate)
        assertEquals(0, black.score.compareTo(0.0))
        assertEquals(RoughPerformanceEstimator.VERSION, white.formulaVersion)
    }

    @Test
    fun estimate_drawUsesOpponentRatingAsPointEstimate() {
        val estimates = RoughPerformanceEstimator.estimate(game(whiteRating = 1500, blackRating = 1600, result = "1/2-1/2"))

        assertEquals(1600, estimates.single { it.side == ChessSide.WHITE }.pointEstimate)
        assertEquals(1500, estimates.single { it.side == ChessSide.BLACK }.pointEstimate)
    }

    @Test
    fun estimate_requiresBothRatingsAndACompletedResult() {
        assertTrue(RoughPerformanceEstimator.estimate(game(whiteRating = 1500, blackRating = null, result = "1-0")).isEmpty())
        assertTrue(RoughPerformanceEstimator.estimate(game(whiteRating = 1500, blackRating = 1600, result = "*")).isEmpty())
    }

    private fun game(whiteRating: Int?, blackRating: Int?, result: String) = ChessGame(
        id = "test",
        source = GameSource.PGN_FILE,
        sourceGameId = null,
        username = null,
        importedAtEpochMillis = 0,
        playedAtEpochMillis = null,
        whitePlayer = "White",
        blackPlayer = "Black",
        whiteRating = whiteRating,
        blackRating = blackRating,
        timeControl = null,
        result = result,
        pgn = "1. e4",
        variant = "Standard",
        parseStatus = GameParseStatus.READY,
    )
}
