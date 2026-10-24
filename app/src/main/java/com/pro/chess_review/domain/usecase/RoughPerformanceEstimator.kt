package com.pro.chess_review.domain.usecase

import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.ChessSide
import com.pro.chess_review.domain.model.PerformanceEstimateConfidence
import com.pro.chess_review.domain.model.RoughPerformanceEstimate
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Produces a low-confidence, single-game performance signal from PGN result and ratings.
 *
 * Engine evaluations are intentionally not used here. Converting centipawn loss into an
 * Elo rating needs a documented, time-control-aware calibration dataset, which this app
 * does not yet have. This formula is instead the standard Elo expected-score inversion,
 * regularized with one virtual draw so one win or loss cannot create an infinite result.
 */
object RoughPerformanceEstimator {
    const val VERSION = "opponent-result-v1"

    private const val VIRTUAL_DRAW_SCORE = 0.5
    private const val OBSERVED_GAMES = 1.0
    private const val VIRTUAL_GAMES = 1.0
    private const val SINGLE_GAME_RANGE_HALF_WIDTH = 350

    fun estimate(game: ChessGame): List<RoughPerformanceEstimate> {
        val whiteRating = game.whiteRating?.takeIf { it > 0 } ?: return emptyList()
        val blackRating = game.blackRating?.takeIf { it > 0 } ?: return emptyList()
        val whiteScore = game.result.toWhiteScore() ?: return emptyList()

        return listOf(
            estimateFor(
                side = ChessSide.WHITE,
                opponentRating = blackRating,
                score = whiteScore,
            ),
            estimateFor(
                side = ChessSide.BLACK,
                opponentRating = whiteRating,
                score = 1.0 - whiteScore,
            ),
        )
    }

    private fun estimateFor(
        side: ChessSide,
        opponentRating: Int,
        score: Double,
    ): RoughPerformanceEstimate {
        val regularizedScore = (
            score * OBSERVED_GAMES + VIRTUAL_DRAW_SCORE * VIRTUAL_GAMES
            ) / (OBSERVED_GAMES + VIRTUAL_GAMES)
        val ratingDifference = 400.0 * log10(regularizedScore / (1.0 - regularizedScore))
        val pointEstimate = (opponentRating + ratingDifference).roundToInt()

        return RoughPerformanceEstimate(
            side = side,
            pointEstimate = pointEstimate,
            lowerBound = pointEstimate - SINGLE_GAME_RANGE_HALF_WIDTH,
            upperBound = pointEstimate + SINGLE_GAME_RANGE_HALF_WIDTH,
            opponentRating = opponentRating,
            score = score,
            confidence = PerformanceEstimateConfidence.LOW,
            formulaVersion = VERSION,
        )
    }

    private fun String?.toWhiteScore(): Double? = when (this?.trim()) {
        "1-0" -> 1.0
        "0-1" -> 0.0
        "1/2-1/2", "½-½" -> 0.5
        else -> null
    }
}
