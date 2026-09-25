package com.pro.chessin.domain.analysis

import android.util.Log
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Color
import com.pro.chessin.domain.engine.EngineRepository
import kotlinx.coroutines.flow.first

/**
 * Analyzes chess moves and classifies them based on evaluation deltas.
 * 
 * Classification thresholds (centipawn delta from mover's perspective):
 * - BEST: delta = 0 (exact match with top engine line)
 * - EXCELLENT: |delta| <= 10
 * - GOOD: 10 < |delta| <= 50
 * - INACCURACY: 50 < |delta| <= 100
 * - MISTAKE: 100 < |delta| <= 300
 * - BLUNDER: |delta| > 300
 * - MISS: Top line contains mate-in-N or large tactical win (>500cp) not taken
 * - BOOK: Position+move matches opening database (checked before engine analysis)
 * 
 * These thresholds are based on common chess engine classification standards:
 * - Lichess uses similar thresholds (10, 50, 100, 300)
 * - Chess.com uses 10, 30, 100, 300
 * - Our values are a middle ground, leaning slightly stricter on mistakes
 */
class MoveAnalyzer(
    private val engineRepository: EngineRepository,
    private val analysisDepth: Int = 14
) {
    
    private companion object {
        private const val TAG = "MoveAnalyzer"
        
        // Classification thresholds (centipawn values)
        private const val EXCELLENT_THRESHOLD = 10
        private const val GOOD_THRESHOLD = 50
        private const val INACCURACY_THRESHOLD = 100
        private const val MISTAKE_THRESHOLD = 300
        private const val BLUNDER_THRESHOLD = 300
        private const val MISS_TACTICAL_THRESHOLD = 500
        
        // Number of PV lines to analyze
        private const val MULTI_PV_COUNT = 3
    }
    
    /**
     * Classifies a move based on the evaluation delta.
     * 
     * @param beforeEval Evaluation before the move (from mover's perspective)
     * @param afterEval Evaluation after the move (from mover's perspective)
     * @param bestEval Evaluation of the best line before the move (from mover's perspective)
     * @param isBookMove Whether this move matches the opening book
     * @param bestLineContainsMate Whether the best line contains a forced mate
     * @param moverIsWhite Whether the side that made the move is White (determines evaluation perspective)
     * @return The classification tier for this move
     */
    fun classify(
        beforeEval: EvalScore,
        afterEval: EvalScore,
        bestEval: EvalScore,
        isBookMove: Boolean = false,
        bestLineContainsMate: Boolean = false,
        moverIsWhite: Boolean = true
    ): ClassificationTier {
        // Book moves are classified before any engine analysis
        if (isBookMove) {
            return ClassificationTier.BOOK
        }

        // Calculate delta from the MOVER'S perspective: how much worse the played
        // move is compared to best. When the mover is Black, Stockfish cp is
        // negated so that positive delta always means "worse for the mover".
        val delta = bestEval.toNormalizedCp(perspective = moverIsWhite) - afterEval.toNormalizedCp(perspective = moverIsWhite)

        // Check for miss: best line had mate or large tactical win that wasn't taken
        val bestHasMate = bestEval.mate != null
        val bestHasBigWin = bestEval.toNormalizedCp(perspective = moverIsWhite) > MISS_TACTICAL_THRESHOLD
        val playedAchievesMate = afterEval.mate != null && afterEval.mate == bestEval.mate
        val playedAchievesBigWin = afterEval.toNormalizedCp(perspective = moverIsWhite) >= MISS_TACTICAL_THRESHOLD

        if ((bestHasMate && !playedAchievesMate) || (bestHasBigWin && !playedAchievesBigWin)) {
            return ClassificationTier.MISS
        }
        
        // Classify based on delta magnitude
        return when {
            delta <= 0 -> ClassificationTier.BEST
            delta <= EXCELLENT_THRESHOLD -> ClassificationTier.EXCELLENT
            delta <= GOOD_THRESHOLD -> ClassificationTier.GOOD
            delta <= INACCURACY_THRESHOLD -> ClassificationTier.INACCURACY
            delta <= MISTAKE_THRESHOLD -> ClassificationTier.MISTAKE
            else -> ClassificationTier.BLUNDER
        }
    }
    
    /**
     * Analyzes a move by running engine analysis and classifying it.
     * This is the full analysis pipeline that integrates with EngineSessionManager.
     * 
     * @param beforeState Board state before the move
     * @param afterState Board state after the move
     * @param sideToMove The side that made the move
     * @param isBookMove Whether this move matches the opening book
     * @return Classification result with evaluation details
     */
    suspend fun analyze(
        beforeState: ChessBoardState,
        afterState: ChessBoardState,
        sideToMove: Color,
        isBookMove: Boolean = false,
        beforeEval: EvalScore = EvalScore.fromCp(0)
    ): ClassificationResult {
        // If it's a book move, classify immediately without engine analysis
        if (isBookMove) {
            return ClassificationResult(
                tier = ClassificationTier.BOOK,
                beforeEval = beforeEval,
                afterEval = EvalScore.fromCp(0),
                bestEval = EvalScore.fromCp(0),
                delta = 0
            )
        }
        
        // Integrate with EngineSessionManager for MultiPV analysis
        try {
            // 1. Set MultiPV option
            engineRepository.sendCommand("setoption name MultiPV value $MULTI_PV_COUNT")
                .getOrThrow()
            
            // 2. Send position command with beforeState FEN
            val fen = beforeState.toFEN()
            engineRepository.sendCommand("position fen $fen")
                .getOrThrow()
            
            // 3. Send go depth command
            engineRepository.sendCommand("go depth $analysisDepth")
                .getOrThrow()
            
            // 4. Collect engine output and parse scores from top PV lines
            val outputLines = mutableListOf<String>()
            engineRepository.engineOutput
                .first { line ->
                    outputLines.add(line)
                    // Stop when we see bestmove
                    line.startsWith("bestmove")
                }
            
            // 5. Extract top scores and alternative lines from MultiPV output
            val topInfoMap = UciParser.extractTopInfo(outputLines.asSequence(), MULTI_PV_COUNT)
            val rawBestEval = topInfoMap[1]?.score ?: EvalScore.fromCp(0)
            // Stockfish evaluates relative to sideToMove in beforeState.
            // If sideToMove is Black, invert rawBestEval to store on White's perspective scale.
            val bestEval = if (sideToMove == Color.WHITE) rawBestEval else rawBestEval.invert()

            // Extract alternative MultiPV lines (multiPv = 2, 3)
            val altLines = mutableListOf<com.pro.chessin.domain.coach.AlternativeLine>()
            for (pvIndex in 2..MULTI_PV_COUNT) {
                val info = topInfoMap[pvIndex]
                if (info != null && info.pv.isNotEmpty()) {
                    val evalScore = if (sideToMove == Color.WHITE) info.score else info.score.invert()
                    altLines.add(
                        com.pro.chessin.domain.coach.AlternativeLine(
                            sanSequence = info.pv.take(4),
                            evalCp = evalScore.toNormalizedCp(perspective = sideToMove == Color.WHITE)
                        )
                    )
                }
            }
            
            // 5b. Clear the output buffer before the second query cycle
            engineRepository.clearOutputBuffer()
            
            // 6. Analyze the after-state to get the played move's evaluation
            engineRepository.sendCommand("position fen ${afterState.toFEN()}")
                .getOrThrow()
            engineRepository.sendCommand("go depth $analysisDepth")
                .getOrThrow()
            
            val afterOutputLines = mutableListOf<String>()
            engineRepository.engineOutput
                .first { line ->
                    afterOutputLines.add(line)
                    line.startsWith("bestmove")
                }
            
            val afterScores = UciParser.extractTopScores(afterOutputLines.asSequence(), 1)
            val rawAfterEval = afterScores[1] ?: EvalScore.fromCp(0)
            // Stockfish evaluates afterState where opponent is side to move.
            // If mover was White, opponent is Black -> invert rawAfterEval to get White's perspective scale.
            // If mover was Black, opponent is White -> rawAfterEval is already White's perspective scale.
            val afterEval = if (sideToMove == Color.WHITE) rawAfterEval.invert() else rawAfterEval
            
            // 7. Calculate delta and classify
            val delta = bestEval.toNormalizedCp(perspective = sideToMove == Color.WHITE) - 
                        afterEval.toNormalizedCp(perspective = sideToMove == Color.WHITE)
            
            // Check if best line contains mate
            val bestLineContainsMate = bestEval.mate != null
            
            val tier = classify(
                beforeEval = beforeEval,
                afterEval = afterEval,
                bestEval = bestEval,
                isBookMove = false,
                bestLineContainsMate = bestLineContainsMate,
                moverIsWhite = sideToMove == Color.WHITE
            )
            
            return ClassificationResult(
                tier = tier,
                beforeEval = beforeEval,
                afterEval = afterEval,
                bestEval = bestEval,
                delta = delta,
                alternativeLines = altLines
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during move analysis", e)
            // Return a default result on error
            return ClassificationResult(
                tier = ClassificationTier.GOOD,
                beforeEval = EvalScore.fromCp(0),
                afterEval = EvalScore.fromCp(0),
                bestEval = EvalScore.fromCp(0),
                delta = 0
            )
        }
    }
}

/**
 * Result of move classification analysis.
 */
data class ClassificationResult(
    val tier: ClassificationTier,
    val beforeEval: EvalScore,
    val afterEval: EvalScore,
    val bestEval: EvalScore,
    val delta: Int,
    val alternativeLines: List<com.pro.chessin.domain.coach.AlternativeLine> = emptyList()
)
