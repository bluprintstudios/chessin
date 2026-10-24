package com.pro.chess_review.domain.usecase

import com.pro.chess_review.domain.model.ChessSide
import com.pro.chess_review.domain.model.MoveAssessment
import com.pro.chess_review.domain.model.MoveQuality
import com.pro.chess_review.domain.model.MoveReviewReason
import com.pro.chess_review.domain.model.PositionReview

object MoveQualityClassifier {
    const val VERSION = "move-quality-v1"

    private const val ACCURATE_LIMIT_CP = 20
    private const val SMALL_INACCURACY_LIMIT_CP = 75
    private const val ERROR_LIMIT_CP = 150

    fun assess(
        plyIndex: Int,
        mover: ChessSide,
        moveSan: String?,
        moveUci: String?,
        legalMoveCount: Int,
        before: PositionReview,
        after: PositionReview,
    ): MoveAssessment {
        val beforeLine = before.lines.firstOrNull()
        val afterLine = after.lines.firstOrNull()
        val bestMoveUci = beforeLine?.movesUci?.firstOrNull()
        val depth = listOfNotNull(beforeLine?.depth, afterLine?.depth).minOrNull()
        val hasEvidence = beforeLine != null && afterLine != null
        val isComplete = hasEvidence && before.isComplete && after.isComplete

        fun result(
            quality: MoveQuality,
            reason: MoveReviewReason,
            centipawnLoss: Int? = null,
            keyMoment: Boolean = false,
        ) = MoveAssessment(
            plyIndex = plyIndex,
            mover = mover,
            moveSan = moveSan,
            moveUci = moveUci,
            bestMoveUci = bestMoveUci,
            quality = quality,
            reason = reason,
            thresholdVersion = VERSION,
            centipawnLoss = centipawnLoss,
            beforeWhiteCentipawns = beforeLine?.whiteCentipawns,
            afterWhiteCentipawns = afterLine?.whiteCentipawns,
            beforeMateIn = beforeLine?.whiteMateIn,
            afterMateIn = afterLine?.whiteMateIn,
            analysisDepth = depth,
            hasEngineEvidence = hasEvidence,
            analysisComplete = isComplete,
            isKeyMoment = keyMoment,
        )

        if (legalMoveCount == 1) {
            return result(MoveQuality.ONLY_LEGAL_MOVE, MoveReviewReason.SINGLE_LEGAL_MOVE)
        }
        if (moveUci.isNullOrBlank()) {
            return result(MoveQuality.NEEDS_ANALYSIS, MoveReviewReason.MOVE_UNAVAILABLE)
        }
        if (!before.isAnalysisAvailable || !after.isAnalysisAvailable || beforeLine == null || afterLine == null) {
            return result(MoveQuality.NEEDS_ANALYSIS, MoveReviewReason.NO_ENGINE_LINES)
        }
        if (!isComplete) {
            return result(MoveQuality.NEEDS_ANALYSIS, MoveReviewReason.ANALYSIS_INCOMPLETE)
        }
        if (bestMoveUci == moveUci) {
            return result(MoveQuality.TOP_ENGINE_CHOICE, MoveReviewReason.MATCHED_TOP_LINE, centipawnLoss = 0)
        }

        val sideSign = if (mover == ChessSide.WHITE) 1 else -1
        val beforeMate = beforeLine.whiteMateIn
        val afterMate = afterLine.whiteMateIn
        val beforeMateForMover = beforeMate?.let { it * sideSign > 0 } == true
        val beforeMateAgainstMover = beforeMate?.let { it * sideSign < 0 } == true
        val afterMateForMover = afterMate?.let { it * sideSign > 0 } == true
        val afterMateAgainstMover = afterMate?.let { it * sideSign < 0 } == true

        if (beforeMateForMover && !afterMateForMover) {
            return result(
                MoveQuality.MATE_ADVANTAGE_NO_LONGER_FOUND,
                MoveReviewReason.MATE_SCORE_CHANGED,
                keyMoment = true,
            )
        }
        if (afterMateAgainstMover && !beforeMateAgainstMover) {
            return result(
                MoveQuality.MATE_ALLOWED,
                MoveReviewReason.MATE_SCORE_CHANGED,
                keyMoment = true,
            )
        }
        if (beforeMate != null || afterMate != null) {
            return result(MoveQuality.MATE_SCORE_PRESENT, MoveReviewReason.MATE_SCORE_CHANGED)
        }

        val beforeCp = beforeLine.whiteCentipawns
        val afterCp = afterLine.whiteCentipawns
        if (beforeCp == null || afterCp == null) {
            return result(MoveQuality.NEEDS_ANALYSIS, MoveReviewReason.NO_ENGINE_LINES)
        }

        val loss = (sideSign * (beforeCp - afterCp)).coerceAtLeast(0)
        return when {
            loss <= ACCURATE_LIMIT_CP -> result(MoveQuality.ACCURATE, MoveReviewReason.EVALUATION_DROP, loss)
            loss <= SMALL_INACCURACY_LIMIT_CP -> result(MoveQuality.SMALL_INACCURACY, MoveReviewReason.EVALUATION_DROP, loss)
            loss <= ERROR_LIMIT_CP -> result(MoveQuality.ERROR, MoveReviewReason.EVALUATION_DROP, loss, keyMoment = true)
            else -> result(MoveQuality.MAJOR_ERROR, MoveReviewReason.EVALUATION_DROP, loss, keyMoment = true)
        }
    }
}
