package com.pro.chessin.domain.analysis

/**
 * Represents the result of analyzing a single chess move.
 * Used by the AnalysisDashboard to display move classifications
 * and drive the evaluation bar.
 *
 * @property san Standard Algebraic Notation for the move (e.g. "Nf3", "exd4")
 * @property fenAfter FEN string after the move was played
 * @property tier Classification result for this move
 * @property evalAfterCp Normalized centipawn evaluation from white's perspective
 *   (positive = white advantage, negative = black advantage)
 * @property deltaCp How many centipawns this move lost relative to the best
 *   move, from the mover's perspective (positive = worse than best)
 * @property bestEvalCp Best move evaluation in centipawns, from the mover's perspective
 * @property moveIndex 0-based ply index
 * @property isBookMove Whether this move was classified as a book move
 */
data class AnalyzedMove(
    val san: String,
    val fenAfter: String,
    val tier: ClassificationTier,
    val evalAfterCp: Int,
    val deltaCp: Int,
    val bestEvalCp: Int,
    val moveIndex: Int,
    val isBookMove: Boolean = false,
    val alternativeLinesJson: String? = null
) {
    companion object {
        /**
         * Serialize a list of [AnalyzedMove] to a compact tab-delimited
         * newline-record format suitable for WorkManager Data (under 10KB
         * for most games).
         */
        fun serialize(moves: List<AnalyzedMove>): String {
            return moves.joinToString("\n") { move ->
                listOf(
                    move.san,
                    move.fenAfter,
                    move.tier.name,
                    move.evalAfterCp.toString(),
                    move.deltaCp.toString(),
                    move.bestEvalCp.toString(),
                    move.moveIndex.toString(),
                    move.isBookMove.toString(),
                    move.alternativeLinesJson ?: ""
                ).joinToString("\t")
            }
        }

        /**
         * Deserialize a list of [AnalyzedMove] from the compact format
         * produced by [serialize]. Returns an empty list for null or
         * malformed input.
         */
        fun deserialize(data: String?): List<AnalyzedMove> {
            if (data.isNullOrEmpty()) return emptyList()
            return data.lines().mapNotNull { line ->
                if (line.isEmpty()) return@mapNotNull null
                val fields = line.split("\t")
                if (fields.size < 8) return@mapNotNull null
                try {
                    AnalyzedMove(
                        san = fields[0],
                        fenAfter = fields[1],
                        tier = ClassificationTier.valueOf(fields[2]),
                        evalAfterCp = fields[3].toInt(),
                        deltaCp = fields[4].toInt(),
                        bestEvalCp = fields[5].toInt(),
                        moveIndex = fields[6].toInt(),
                        isBookMove = fields[7].toBoolean(),
                        alternativeLinesJson = if (fields.size >= 9 && fields[8].isNotEmpty()) fields[8] else null
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
