package com.pro.chessin.domain.puzzles

import com.pro.chessin.data.local.entity.PuzzleEntity
import com.pro.chessin.data.model.Game
import com.pro.chessin.data.model.Move
import com.pro.chessin.domain.chess.ChessBoardState

/**
 * Auto-generates "Learn from your mistakes" puzzles from analyzed user games.
 * 
 * Scans analyzed games for moves classified as BLUNDER or MISTAKE,
 * creating a [PuzzleEntity] from the position immediately preceding the mistake
 * with the engine's recommended line as the solution.
 */
object MistakePuzzleGenerator {

    /**
     * Generate mistake puzzles from an analyzed game and its move list.
     */
    fun generateFromGame(
        game: Game,
        moves: List<Move>,
        userRating: Int = 1500
    ): List<PuzzleEntity> {
        val puzzles = mutableListOf<PuzzleEntity>()

        for (i in moves.indices) {
            val move = moves[i]
            val isMistakeOrBlunder = move.classificationTier == "BLUNDER" || move.classificationTier == "MISTAKE"
            if (!isMistakeOrBlunder) continue

            // Position immediately BEFORE the mistake
            val fenBefore = if (i == 0) {
                ChessBoardState.startPosition().toFEN()
            } else {
                moves[i - 1].fenAfter
            }

            // Extract best move/solution from explanationText or fallback
            val solution = extractSolution(move) ?: continue

            val puzzleId = "mistake_${game.id}_${move.ply}"
            puzzles.add(
                PuzzleEntity(
                    id = puzzleId,
                    fen = fenBefore,
                    solutionMoves = solution,
                    rating = userRating,
                    themes = "user_mistake,${move.classificationTier.lowercase()}",
                    source = "USER_MISTAKE",
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        return puzzles
    }

    /**
     * Extract the recommended move sequence from explanationText.
     * Fallbacks to move.san if explanation is blank.
     */
    fun extractSolution(move: Move): String? {
        val text = move.explanationText
        if (!text.isNullOrEmpty()) {
            if (text.contains("Best move:")) {
                return text.substringAfter("Best move:").trim()
            }
            return text.trim()
        }
        return if (move.san.isNotEmpty()) move.san else null
    }
}
