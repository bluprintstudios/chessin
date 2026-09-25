package com.pro.chessin.domain.coach

/**
 * Structured data class representing chess move context for the AI Coach.
 * Serializes to JSON only when sending to the LLM provider.
 *
 * @property fenBefore Position FEN before the played move
 * @property fenAfter Position FEN after the played move
 * @property sanMove Played move in Standard Algebraic Notation (e.g. "Nf3", "exd5")
 * @property classificationTier Classification tier (e.g. "BEST", "BLUNDER", "INACCURACY")
 * @property evalDeltaCp Centipawn evaluation loss relative to the best move (from mover's perspective)
 * @property alternativeLines Top alternative PV engine lines (short SAN sequence + score)
 * @property priorMoveHistory Recent preceding move history in SAN (up to 4 plies)
 */
data class CoachContext(
    val fenBefore: String,
    val fenAfter: String,
    val sanMove: String,
    val classificationTier: String,
    val evalDeltaCp: Int,
    val alternativeLines: List<AlternativeLine> = emptyList(),
    val priorMoveHistory: List<String> = emptyList()
) {
    /**
     * Serializes this CoachContext into a valid JSON string.
     * Guaranteed to contain zero PII or user/device identifiers.
     */
    fun toJson(): String {
        val altLinesJson = alternativeLines.joinToString(",") { it.toJson() }
        val priorHistoryJson = priorMoveHistory.joinToString(",") { "\"${escapeJson(it)}\"" }

        return """{
  "fenBefore": "${escapeJson(fenBefore)}",
  "fenAfter": "${escapeJson(fenAfter)}",
  "sanMove": "${escapeJson(sanMove)}",
  "classificationTier": "${escapeJson(classificationTier)}",
  "evalDeltaCp": $evalDeltaCp,
  "alternativeLines": [$altLinesJson],
  "priorMoveHistory": [$priorHistoryJson]
}""".trimIndent()
    }

    private fun escapeJson(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

/**
 * Represents an alternative engine PV line.
 *
 * @property sanSequence Short sequence of move SANs (e.g. ["e4", "e5", "Nf3"])
 * @property evalCp Evaluation score in centipawns for this line
 */
data class AlternativeLine(
    val sanSequence: List<String>,
    val evalCp: Int
) {
    /**
     * Serializes this AlternativeLine to JSON.
     */
    fun toJson(): String {
        val seqJson = sanSequence.joinToString(",") { "\"${escapeJson(it)}\"" }
        return "{\"sanSequence\":[$seqJson],\"evalCp\":$evalCp}"
    }

    private fun escapeJson(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }
}
