package com.pro.chess_review.domain.usecase

import com.pro.chess_review.core.chess.GameReplay
import com.pro.chess_review.core.chess.ReplayState
import com.pro.chess_review.core.engine.EngineLine
import com.pro.chess_review.core.engine.EngineResult
import com.pro.chess_review.core.engine.EngineSettings
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.GameReview
import com.pro.chess_review.domain.model.GameReviewProgress
import com.pro.chess_review.domain.model.GameReviewSummary
import com.pro.chess_review.domain.model.PerformanceEstimateStatus
import com.pro.chess_review.domain.model.PositionReview
import com.pro.chess_review.domain.model.ReviewCandidateLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ReviewGameUseCase(
    private val analyzePosition: AnalyzePositionUseCase,
) {
    operator fun invoke(game: ChessGame, settings: EngineSettings): Flow<GameReviewProgress> = flow {
        val replay = GameReplay(game)
        val totalPlies = replay.state.value.totalPlies
        val replayStates = (0..totalPlies).map { ply ->
            replay.goToPly(ply)
            replay.state.value
        }
        val analyses = mutableListOf<EngineResult?>()

        replayStates.forEachIndexed { index, state ->
            var latestWithLines: EngineResult? = null
            analyzePosition(state.fen, settings).collect { result ->
                if (result.lines.isNotEmpty()) latestWithLines = result
                emit(
                    GameReviewProgress(
                        processedPositions = index,
                        totalPositions = replayStates.size,
                        currentPly = state.currentPly,
                        hasEngineLines = latestWithLines != null,
                        isAnalyzing = true,
                    ),
                )
            }
            analyses += latestWithLines
            emit(
                GameReviewProgress(
                    processedPositions = index + 1,
                    totalPositions = replayStates.size,
                    currentPly = state.currentPly,
                    hasEngineLines = latestWithLines != null,
                    isAnalyzing = false,
                ),
            )
        }

        val positionReviews = replayStates.mapIndexed { index, state ->
            state.toPositionReview(analyses[index])
        }
        val moveAssessments = (0 until totalPlies).map { ply ->
            val beforeState = replayStates[ply]
            MoveQualityClassifier.assess(
                plyIndex = ply,
                mover = beforeState.sideToMove,
                moveSan = beforeState.nextMoveSan,
                moveUci = beforeState.nextMoveUci,
                legalMoveCount = beforeState.legalMoveCount,
                before = positionReviews[ply],
                after = positionReviews[ply + 1],
            )
        }
        val analyzedPlies = moveAssessments.count { it.analysisComplete }
        val cpLosses = moveAssessments.mapNotNull { it.centipawnLoss }
        val performanceEstimates = RoughPerformanceEstimator.estimate(game)
        val summary = GameReviewSummary(
            totalPlies = totalPlies,
            analyzedPlies = analyzedPlies,
            analysisCoveragePercent = if (totalPlies == 0) 0 else analyzedPlies * 100 / totalPlies,
            averageCentipawnLoss = cpLosses.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            qualityCounts = moveAssessments.groupingBy { it.quality }.eachCount(),
            keyMomentPlies = moveAssessments.filter { it.isKeyMoment }.map { it.plyIndex },
            performanceEstimateStatus = if (performanceEstimates.isEmpty()) {
                PerformanceEstimateStatus.INSUFFICIENT_GAME_DATA
            } else {
                PerformanceEstimateStatus.ROUGH_SINGLE_GAME
            },
            performanceEstimates = performanceEstimates,
        )
        val review = GameReview(
            gameId = game.id,
            positions = positionReviews,
            moves = moveAssessments,
            summary = summary,
            formulaVersion = MoveQualityClassifier.VERSION,
        )
        emit(
            GameReviewProgress(
                processedPositions = replayStates.size,
                totalPositions = replayStates.size,
                currentPly = null,
                hasEngineLines = positionReviews.any { it.lines.isNotEmpty() },
                isAnalyzing = false,
                review = review,
            ),
        )
    }.flowOn(Dispatchers.Default)

    private fun ReplayState.toPositionReview(result: EngineResult?): PositionReview {
        val lines = result?.lines.orEmpty().take(5).map { line -> line.toReviewLine() }
        return PositionReview(
            plyIndex = currentPly,
            fen = fen,
            lines = lines,
            engineId = result?.engineId,
            engineVersion = result?.engineVersion,
            isAnalysisAvailable = lines.isNotEmpty(),
            isComplete = result?.isComplete == true && lines.isNotEmpty(),
        )
    }

    private fun EngineLine.toReviewLine() = ReviewCandidateLine(
        movesUci = uciMoves,
        movesSan = sanMoves,
        whiteCentipawns = centipawns,
        whiteMateIn = mateIn,
        depth = depth,
    )
}
