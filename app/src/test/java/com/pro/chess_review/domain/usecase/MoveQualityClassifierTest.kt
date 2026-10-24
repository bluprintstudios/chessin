package com.pro.chess_review.domain.usecase

import com.pro.chess_review.domain.model.ChessSide
import com.pro.chess_review.domain.model.MoveQuality
import com.pro.chess_review.domain.model.MoveReviewReason
import com.pro.chess_review.domain.model.PositionReview
import com.pro.chess_review.domain.model.ReviewCandidateLine
import org.junit.Assert.assertEquals
import org.junit.Test

class MoveQualityClassifierTest {

    private fun createReview(
        isAvailable: Boolean = true,
        isComplete: Boolean = true,
        cp: Int? = null,
        mate: Int? = null,
        bestMoveUci: String = "e2e4"
    ): PositionReview {
        val line = if (isAvailable) {
            ReviewCandidateLine(
                movesUci = listOf(bestMoveUci),
                movesSan = listOf("e4"),
                whiteCentipawns = cp,
                whiteMateIn = mate,
                depth = 18
            )
        } else null

        return PositionReview(
            plyIndex = 0,
            fen = "",
            lines = listOfNotNull(line),
            engineId = "Stockfish",
            engineVersion = "19",
            isAnalysisAvailable = isAvailable,
            isComplete = isComplete
        )
    }

    @Test
    fun assess_accurate_whenLossIsSmall() {
        val before = createReview(cp = 50, bestMoveUci = "e2e4")
        val after = createReview(cp = 40) // White lost 10 cp

        val assessment = MoveQualityClassifier.assess(
            plyIndex = 0,
            mover = ChessSide.WHITE,
            moveSan = "d4",
            moveUci = "d2d4",
            legalMoveCount = 20,
            before = before,
            after = after
        )

        assertEquals(MoveQuality.ACCURATE, assessment.quality)
        assertEquals(MoveReviewReason.EVALUATION_DROP, assessment.reason)
        assertEquals(10, assessment.centipawnLoss)
    }

    @Test
    fun assess_thresholdBoundaries_useInclusiveCutoffs() {
        val cases = listOf(
            20 to MoveQuality.ACCURATE,
            21 to MoveQuality.SMALL_INACCURACY,
            75 to MoveQuality.SMALL_INACCURACY,
            76 to MoveQuality.ERROR,
            150 to MoveQuality.ERROR,
            151 to MoveQuality.MAJOR_ERROR,
        )

        cases.forEach { (loss, expectedQuality) ->
            val assessment = MoveQualityClassifier.assess(
                plyIndex = 0,
                mover = ChessSide.WHITE,
                moveSan = "d4",
                moveUci = "d2d4",
                legalMoveCount = 20,
                before = createReview(cp = 0, bestMoveUci = "e2e4"),
                after = createReview(cp = -loss),
            )

            assertEquals("loss=$loss", expectedQuality, assessment.quality)
            assertEquals("loss=$loss", loss, assessment.centipawnLoss)
        }
    }

    @Test
    fun assess_majorError_whenLossIsHuge() {
        val before = createReview(cp = 0, bestMoveUci = "e2e4")
        val after = createReview(cp = -200) // White lost 200 cp

        val assessment = MoveQualityClassifier.assess(
            plyIndex = 0,
            mover = ChessSide.WHITE,
            moveSan = "g4",
            moveUci = "g2g4",
            legalMoveCount = 20,
            before = before,
            after = after
        )

        assertEquals(MoveQuality.MAJOR_ERROR, assessment.quality)
        assertEquals(200, assessment.centipawnLoss)
    }

    @Test
    fun assess_topEngineChoice_whenUciMatches() {
        val before = createReview(cp = 50, bestMoveUci = "e2e4")
        val after = createReview(cp = 50) 

        val assessment = MoveQualityClassifier.assess(
            plyIndex = 0,
            mover = ChessSide.WHITE,
            moveSan = "e4",
            moveUci = "e2e4",
            legalMoveCount = 20,
            before = before,
            after = after
        )

        assertEquals(MoveQuality.TOP_ENGINE_CHOICE, assessment.quality)
        assertEquals(MoveReviewReason.MATCHED_TOP_LINE, assessment.reason)
        assertEquals(0, assessment.centipawnLoss)
    }

    @Test
    fun assess_mateAllowed_whenMoverAllowsMate() {
        val before = createReview(cp = 0) // No mate
        val after = createReview(mate = -1) // White just allowed mate in 1 (score is -1 for white)

        val assessment = MoveQualityClassifier.assess(
            plyIndex = 0,
            mover = ChessSide.WHITE,
            moveSan = "a3",
            moveUci = "a2a3",
            legalMoveCount = 20,
            before = before,
            after = after
        )

        assertEquals(MoveQuality.MATE_ALLOWED, assessment.quality)
        assertEquals(MoveReviewReason.MATE_SCORE_CHANGED, assessment.reason)
        assertEquals(true, assessment.isKeyMoment)
    }

    @Test
    fun assess_mateAdvantageNoLongerFound_whenMoverMissesMate() {
        val before = createReview(mate = 1) // White has mate in 1
        val after = createReview(cp = 500) // White played a move, now it's just +5.00

        val assessment = MoveQualityClassifier.assess(
            plyIndex = 0,
            mover = ChessSide.WHITE,
            moveSan = "a3",
            moveUci = "a2a3",
            legalMoveCount = 20,
            before = before,
            after = after
        )

        assertEquals(MoveQuality.MATE_ADVANTAGE_NO_LONGER_FOUND, assessment.quality)
    }

    @Test
    fun assess_needsAnalysis_whenAnalysisIncomplete() {
        val before = createReview(isComplete = false, cp = 0)
        val after = createReview(isComplete = true, cp = 0)

        val assessment = MoveQualityClassifier.assess(
            plyIndex = 0,
            mover = ChessSide.WHITE,
            moveSan = "e4",
            moveUci = "e2e4",
            legalMoveCount = 20,
            before = before,
            after = after
        )

        assertEquals(MoveQuality.NEEDS_ANALYSIS, assessment.quality)
        assertEquals(MoveReviewReason.ANALYSIS_INCOMPLETE, assessment.reason)
    }

    @Test
    fun assess_onlyLegalMove_doesNotLabelItAnErrorWithoutAnalysis() {
        val assessment = MoveQualityClassifier.assess(
            plyIndex = 0,
            mover = ChessSide.WHITE,
            moveSan = "Kf2",
            moveUci = "e1f2",
            legalMoveCount = 1,
            before = createReview(isAvailable = false, isComplete = false),
            after = createReview(isAvailable = false, isComplete = false),
        )

        assertEquals(MoveQuality.ONLY_LEGAL_MOVE, assessment.quality)
        assertEquals(MoveReviewReason.SINGLE_LEGAL_MOVE, assessment.reason)
        assertEquals(false, assessment.hasEngineEvidence)
    }
}
