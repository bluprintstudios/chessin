package com.pro.chessin.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pro.chessin.data.engine.NNUEAssetPackManager
import com.pro.chessin.domain.analysis.AnalyzedMove
import com.pro.chessin.domain.analysis.MoveAnalyzer
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Color
import com.pro.chessin.domain.chess.PgnParser
import com.pro.chessin.domain.engine.EngineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * WorkManager [CoroutineWorker] that performs batch per-move analysis of a
 * chess game using the on-device Stockfish engine.
 *
 * The worker:
 * 1. Receives the PGN and depth as [inputData]
 * 2. Starts the engine with the NNUE network (resolved via [NNUEAssetPackManager])
 * 3. Parses the PGN and iterates through each move
 * 4. For each non-book move, runs [MoveAnalyzer.analyze] after clearing the
 *    output buffer to prevent stale replay data
 * 5. Reports progress via [setProgress] (current / total ply)
 * 6. On cancellation, stops analyzing and returns partial results
 * 7. Returns serialized [AnalyzedMove] list as [outputData]
 *
 * Per AGENTS.md paywall boundary: on-device Stockfish analysis is never gated
 * behind a paywall — this worker runs entirely on-device.
 */
@HiltWorker
class GameAnalyzerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engineRepository: EngineRepository,
    private val nnueAssetPackManager: NNUEAssetPackManager,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PGN = "pgn"
        const val KEY_ANALYSIS_DEPTH = "analysis_depth"
        const val KEY_RESULT = "result"
        const val KEY_ERROR = "error"
        const val KEY_TOTAL = "total"

        const val PROGRESS_CURRENT = "progress_current"
        const val PROGRESS_TOTAL = "progress_total"

        private const val TAG = "GameAnalyzerWorker"
        const val BOOK_MOVE_COUNT = 0  // Disabled: Engine analyzes all moves from ply 0 so blunders like 1. Nh3 are caught
    }

    override suspend fun doWork(): ListenableWorker.Result {
        val pgn = inputData.getString(KEY_PGN)
        if (pgn.isNullOrEmpty()) {
            return ListenableWorker.Result.failure(workDataOf(KEY_ERROR to "Empty PGN"))
        }
        val depth = inputData.getInt(KEY_ANALYSIS_DEPTH, 14)

        return try {
            val data = performAnalysis(pgn, depth)
            ListenableWorker.Result.success(data)
        } catch (e: CancellationException) {
            // Coroutine was cancelled — re-throw so WorkManager marks work as CANCELLED.
            // Partial results in setProgress() already reflect progress to date.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during analysis", e)
            ListenableWorker.Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        } finally {
            try {
                engineRepository.stopEngine()
                Log.d(TAG, "Engine stopped in doWork finally block")
            } catch (_: Exception) {
                // Engine might already be stopped
            }
        }
    }

    private suspend fun performAnalysis(pgn: String, depth: Int): Data {
        // 1. Resolve NNUE path
        val nnuePath = try {
            nnueAssetPackManager.getAssetPackPath()
        } catch (e: Exception) {
            return workDataOf(KEY_ERROR to "Failed to get NNUE path: ${e.message}")
        }

        // 2. Start engine
        try {
            engineRepository.startEngine(nnuePath).getOrThrow()
        } catch (e: Exception) {
            return workDataOf(KEY_ERROR to "Failed to start engine: ${e.message}")
        }

        // 3. Parse PGN
        val games = try {
            PgnParser.parse(pgn)
        } catch (e: Exception) {
            return workDataOf(KEY_ERROR to "Failed to parse PGN: ${e.message}")
        }
        if (games.isEmpty()) return workDataOf(KEY_ERROR to "No games found in PGN")

        val moves = games[0].moves
        val totalPly = moves.size
        if (totalPly == 0) return workDataOf(KEY_ERROR to "No moves found in PGN")

        // 4. Analyze each move
        val analyzer = MoveAnalyzer(engineRepository, depth)
        var board = ChessBoardState.startPosition()
        var previousEval = com.pro.chessin.domain.analysis.EvalScore.fromCp(0)
        val analyzedMoves = mutableListOf<AnalyzedMove>()

        for ((index, parsedMove) in moves.withIndex()) {
            // Check for cancellation
            if (isStopped) {
                Log.d(TAG, "Analysis cancelled at ply $index of $totalPly")
                break
            }

            val beforeState = board
            val afterState = board.makeMove(parsedMove.move)
            val sideToMove = beforeState.sideToMove

            engineRepository.clearOutputBuffer()

            val isBook = index < BOOK_MOVE_COUNT
            val result = analyzer.analyze(
                beforeState = beforeState,
                afterState = afterState,
                sideToMove = sideToMove,
                isBookMove = isBook,
                beforeEval = previousEval
            )
            previousEval = result.afterEval

            val altLinesJson = if (result.alternativeLines.isNotEmpty()) {
                result.alternativeLines.joinToString(",", prefix = "[", postfix = "]") { it.toJson() }
            } else null

            val analyzedMove = AnalyzedMove(
                san = parsedMove.san,
                fenAfter = afterState.toFEN(),
                tier = result.tier,
                evalAfterCp = result.afterEval.toNormalizedCp(
                    perspective = true  // White's perspective (eval bar shows White's advantage)
                ),
                deltaCp = result.delta,
                bestEvalCp = result.bestEval.toNormalizedCp(
                    perspective = sideToMove == Color.WHITE
                ),
                moveIndex = index,
                isBookMove = isBook,
                alternativeLinesJson = altLinesJson
            )

            analyzedMoves.add(analyzedMove)
            board = afterState

            // Report lightweight progress (current and total ply)
            // Note: Avoid serializing full move history into setProgress Data
            // to comply with WorkManager's 10KB Data payload limit.
            val progressData = workDataOf(
                PROGRESS_CURRENT to (index + 1),
                PROGRESS_TOTAL to totalPly
            )
            setProgress(progressData)

            Log.d(TAG, "Analyzed ply ${index + 1}/$totalPly: ${parsedMove.san} -> ${result.tier}")
        }

        // 5. Serialize and return results
        val serialized = AnalyzedMove.serialize(analyzedMoves)
        return workDataOf(
            KEY_RESULT to serialized,
            KEY_TOTAL to totalPly
        )
    }
}
