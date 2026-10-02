package com.pro.chessin.domain.coach

import com.pro.chessin.data.local.entity.GameEntity
import com.pro.chessin.data.local.entity.MoveEntity
import com.pro.chessin.domain.chess.ChessBoardState
import org.json.JSONArray

/**
 * Builds structured CoachContext payloads for analyzed moves, enforcing a strict token budget (~800 tokens)
 * and guaranteed zero PII.
 */
object AICoachContextBuilder {

    private const val DEFAULT_TOKEN_BUDGET = 800
    private const val MAX_PRIOR_PLIES = 4
    private const val MAX_ALT_LINES = 3

    /**
     * Builds a CoachContext for a specific move, enforcing token budget truncation.
     *
     * @param move The target MoveEntity to build context for
     * @param game Owning GameEntity
     * @param priorPly List of MoveEntity instances preceding this move
     * @param maxTokenBudget Target token budget limit (default 800 BPE tokens)
     * @return Structured CoachContext payload under the token budget
     */
    @Suppress("UNUSED_PARAMETER")
    fun build(
        move: MoveEntity,
        game: GameEntity,
        priorPly: List<MoveEntity>,
        deltaCp: Int,
        maxTokenBudget: Int = DEFAULT_TOKEN_BUDGET
    ): CoachContext {
        // Determine fenBefore from prior move or starting position
        val lastPriorMove = priorPly.lastOrNull()
        val fenBefore = lastPriorMove?.fenAfter ?: ChessBoardState.startPosition().toFEN()

        // Extract prior move SAN history (up to 4 plies)
        var priorHistory = priorPly.takeLast(MAX_PRIOR_PLIES).map { it.san }

        // Extract alternative MultiPV lines from move entity if present
        var altLines = parseAlternativeLines(move.alternativeLinesJson).take(MAX_ALT_LINES)

        // Use the provided deltaCp
        val evalDeltaCp = deltaCp

        var currentContext = CoachContext(
            fenBefore = fenBefore,
            fenAfter = move.fenAfter,
            sanMove = move.san,
            classificationTier = move.classificationTier,
            evalDeltaCp = evalDeltaCp,
            alternativeLines = altLines,
            priorMoveHistory = priorHistory
        )

        // Enforce token budget with strict truncation hierarchy:
        // 1. Truncate prior move history (oldest plies first)
        // 2. Truncate alternative lines (remove 3rd, 2nd, or trim SAN sequence)
        // 3. Truncate position FEN as absolute last resort
        var tokenCount = CoachTokenCounter.countTokens(currentContext.toJson())

        // Truncation Loop 1: Prior move history
        while (tokenCount > maxTokenBudget && priorHistory.isNotEmpty()) {
            priorHistory = priorHistory.drop(1) // Drop oldest preceding move
            currentContext = currentContext.copy(priorMoveHistory = priorHistory)
            tokenCount = CoachTokenCounter.countTokens(currentContext.toJson())
        }

        // Truncation Loop 2: Alternative lines
        while (tokenCount > maxTokenBudget && altLines.isNotEmpty()) {
            val lastLine = altLines.last()
            if (lastLine.sanSequence.size > 1) {
                // Shorten the SAN sequence of the last alternative line
                val shortenedLine = lastLine.copy(sanSequence = lastLine.sanSequence.dropLast(1))
                altLines = altLines.dropLast(1) + shortenedLine
            } else if (altLines.size > 1) {
                // Remove the last alternative line completely
                altLines = altLines.dropLast(1)
            } else {
                // Remove alternative lines entirely
                altLines = emptyList()
            }
            currentContext = currentContext.copy(alternativeLines = altLines)
            tokenCount = CoachTokenCounter.countTokens(currentContext.toJson())
        }

        // Truncation Loop 3: Position FEN simplification (last resort)
        if (tokenCount > maxTokenBudget) {
            var simpleFenBefore = simplifyFen(currentContext.fenBefore)
            var simpleFenAfter = simplifyFen(currentContext.fenAfter)
            currentContext = currentContext.copy(
                fenBefore = simpleFenBefore,
                fenAfter = simpleFenAfter
            )
            tokenCount = CoachTokenCounter.countTokens(currentContext.toJson())

            if (tokenCount > maxTokenBudget) {
                // If still over budget, retain piece placement only
                simpleFenBefore = simpleFenBefore.split(" ")[0]
                simpleFenAfter = simpleFenAfter.split(" ")[0]
                currentContext = currentContext.copy(
                    fenBefore = simpleFenBefore,
                    fenAfter = simpleFenAfter
                )
            }
        }

        return currentContext
    }

    /**
     * Parses alternative lines JSON string from MoveEntity.
     */
    fun parseAlternativeLines(json: String?): List<AlternativeLine> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val objectRegex = Regex("""\{"sanSequence":\[(.*?)\],"evalCp":(-?\d+)\}""")
            val matches = objectRegex.findAll(json)
            val lines = mutableListOf<AlternativeLine>()

            for (match in matches) {
                val seqString = match.groupValues[1]
                val evalCp = match.groupValues[2].toIntOrNull() ?: 0
                val sanSeq = seqString.split(",")
                    .map { it.replace("\"", "").trim() }
                    .filter { it.isNotEmpty() }
                if (sanSeq.isNotEmpty()) {
                    lines.add(AlternativeLine(sanSeq, evalCp))
                }
            }
            lines
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Simplifies FEN string to piece placement and side to move as a last resort for token budget reduction.
     */
    private fun simplifyFen(fen: String): String {
        val parts = fen.split(" ")
        return if (parts.size >= 2) {
            "${parts[0]} ${parts[1]}"
        } else {
            fen
        }
    }
}
