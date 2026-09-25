package com.pro.chessin.domain.puzzles

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Rating state produced by Glicko-2 calculations.
 */
data class GlickoRatingState(
    val rating: Double,
    val ratingDeviation: Double,
    val volatility: Double
)

/**
 * Implementation of the Glicko-2 rating system for chess puzzles.
 * 
 * Converts standard Glicko scale (1500 rating, 350 RD) to Glicko-2 scale,
 * calculates expected outcome and rating variance, and converts back.
 */
object Glicko2RatingSystem {

    private const val SCALE_FACTOR = 173.7178
    private const val TAU = 0.5 // System constant
    private const val EPSILON = 0.000001

    /**
     * Calculate updated rating state after a single puzzle attempt.
     * 
     * @param userState User's current rating state.
     * @param puzzleRating Difficulty rating of the puzzle.
     * @param puzzleRD Rating deviation of the puzzle (default 50.0).
     * @param score 1.0 for success/correct, 0.0 for failure/incorrect.
     */
    fun calculateNewRating(
        userState: GlickoRatingState,
        puzzleRating: Double,
        puzzleRD: Double = 50.0,
        score: Double
    ): GlickoRatingState {
        // Step 1: Convert to Glicko-2 scale
        val mu = (userState.rating - 1500.0) / SCALE_FACTOR
        val phi = userState.ratingDeviation / SCALE_FACTOR
        val sigma = userState.volatility

        val muJ = (puzzleRating - 1500.0) / SCALE_FACTOR
        val phiJ = puzzleRD / SCALE_FACTOR

        // Step 2: Calculate g(phiJ) and E(mu, muJ, phiJ)
        val gPhiJ = 1.0 / sqrt(1.0 + (3.0 * phiJ * phiJ) / (PI * PI))
        val expected = 1.0 / (1.0 + exp(-gPhiJ * (mu - muJ)))

        // Step 3: Estimated variance v
        val v = 1.0 / (gPhiJ * gPhiJ * expected * (1.0 - expected))

        // Step 4: Estimated improvement delta
        val delta = v * gPhiJ * (score - expected)

        // Step 5: Determine new volatility sigma'
        val a = ln(sigma * sigma)
        fun f(x: Double): Double {
            val ex = exp(x)
            val d2 = delta * delta
            val phi2 = phi * phi
            val num = ex * (d2 - phi2 - v - ex)
            val den = 2.0 * (phi2 + v + ex) * (phi2 + v + ex)
            return (num / den) - ((x - a) / (TAU * TAU))
        }

        var A = a
        var B = if (delta * delta > phi * phi + v) {
            ln(delta * delta - phi * phi - v)
        } else {
            var k = 1.0
            while (f(a - k * TAU) < 0) {
                k += 1.0
            }
            a - k * TAU
        }

        var fA = f(A)
        var fB = f(B)

        while (abs(B - A) > EPSILON) {
            val C = A + (A - B) * fA / (fB - fA)
            val fC = f(C)
            if (fC * fB < 0) {
                A = B
                fA = fB
            } else {
                fA /= 2.0
            }
            B = C
            fB = fC
        }

        val newSigma = exp(A / 2.0)

        // Step 6: Update rating deviation phi'
        val phiStar = sqrt(phi * phi + newSigma * newSigma)
        val newPhi = 1.0 / sqrt(1.0 / (phiStar * phiStar) + 1.0 / v)

        // Step 7: Update rating mu'
        val newMu = mu + newPhi * newPhi * gPhiJ * (score - expected)

        // Step 8: Convert back to standard scale
        val finalRating = newMu * SCALE_FACTOR + 1500.0
        val finalRD = newPhi * SCALE_FACTOR

        return GlickoRatingState(
            rating = finalRating,
            ratingDeviation = finalRD,
            volatility = newSigma
        )
    }

    /**
     * Helper to get standard rounded integer rating from double.
     */
    fun toDisplayRating(rating: Double): Int {
        return rating.roundToInt()
    }
}
