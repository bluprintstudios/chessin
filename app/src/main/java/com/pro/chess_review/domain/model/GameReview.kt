package com.pro.chess_review.domain.model

enum class ChessSide {
    WHITE,
    BLACK,
}

enum class MoveQuality {
    ONLY_LEGAL_MOVE,
    TOP_ENGINE_CHOICE,
    ACCURATE,
    SMALL_INACCURACY,
    ERROR,
    MAJOR_ERROR,
    MATE_ADVANTAGE_NO_LONGER_FOUND,
    MATE_ALLOWED,
    MATE_SCORE_PRESENT,
    NEEDS_ANALYSIS,
}

enum class MoveReviewReason {
    SINGLE_LEGAL_MOVE,
    MATCHED_TOP_LINE,
    EVALUATION_DROP,
    MATE_SCORE_CHANGED,
    NO_ENGINE_LINES,
    ANALYSIS_INCOMPLETE,
    MOVE_UNAVAILABLE,
}

enum class PerformanceEstimateStatus {
    /** Neither player has the rating/result evidence needed for a local estimate. */
    INSUFFICIENT_GAME_DATA,

    /**
     * A single-game, opponent-result performance signal. It is deliberately marked low
     * confidence and is not a rating or a prediction of future playing strength.
     */
    ROUGH_SINGLE_GAME,
}

enum class PerformanceEstimateConfidence {
    LOW,
}

/**
 * A deliberately conservative, local estimate of how one side performed in this game
 * relative to the opponent rating recorded in the PGN.
 */
data class RoughPerformanceEstimate(
    val side: ChessSide,
    val pointEstimate: Int,
    val lowerBound: Int,
    val upperBound: Int,
    val opponentRating: Int,
    val score: Double,
    val confidence: PerformanceEstimateConfidence,
    val formulaVersion: String,
)

data class ReviewCandidateLine(
    val movesUci: List<String>,
    val movesSan: List<String>,
    val whiteCentipawns: Int?,
    val whiteMateIn: Int?,
    val depth: Int,
)

data class PositionReview(
    val plyIndex: Int,
    val fen: String,
    val lines: List<ReviewCandidateLine>,
    val engineId: String?,
    val engineVersion: String?,
    val isAnalysisAvailable: Boolean,
    val isComplete: Boolean,
)

data class MoveAssessment(
    val plyIndex: Int,
    val mover: ChessSide,
    val moveSan: String?,
    val moveUci: String?,
    val bestMoveUci: String?,
    val quality: MoveQuality,
    val reason: MoveReviewReason,
    val thresholdVersion: String,
    val centipawnLoss: Int?,
    val beforeWhiteCentipawns: Int?,
    val afterWhiteCentipawns: Int?,
    val beforeMateIn: Int?,
    val afterMateIn: Int?,
    val analysisDepth: Int?,
    val hasEngineEvidence: Boolean,
    val analysisComplete: Boolean,
    val isKeyMoment: Boolean,
)

data class GameReviewSummary(
    val totalPlies: Int,
    val analyzedPlies: Int,
    val analysisCoveragePercent: Int,
    val averageCentipawnLoss: Int?,
    val qualityCounts: Map<MoveQuality, Int>,
    val keyMomentPlies: List<Int>,
    val performanceEstimateStatus: PerformanceEstimateStatus = PerformanceEstimateStatus.INSUFFICIENT_GAME_DATA,
    val performanceEstimates: List<RoughPerformanceEstimate> = emptyList(),
)

data class GameReview(
    val gameId: String,
    val positions: List<PositionReview>,
    val moves: List<MoveAssessment>,
    val summary: GameReviewSummary,
    val formulaVersion: String,
)

data class GameReviewProgress(
    val processedPositions: Int,
    val totalPositions: Int,
    val currentPly: Int?,
    val hasEngineLines: Boolean,
    val isAnalyzing: Boolean,
    val review: GameReview? = null,
)
