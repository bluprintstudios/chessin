package com.pro.chessin.domain.analysis

/**
 * Represents a chess engine evaluation score.
 * Can be either a centipawn value or a mate-in-N value.
 * 
 * @property cp Centipawn value (positive = white advantage, negative = black advantage), null if mate score
 * @property mate Mate distance (e.g., mate in 5), null if cp score
 */
data class EvalScore(
    val cp: Int? = null,
    val mate: Int? = null
) {
    companion object {
        /**
         * Maximum centipawn value for normalization.
         * Used to bound mate-in-N scores to a comparable scale.
         */
        const val MAX_CP = 10000
        
        /**
         * Centipawn penalty per move for mate-in-N normalization.
         * Formula: MAX_CP - (MATE_PENALTY * matingDistance)
         */
        const val MATE_PENALTY = 100
        
        /**
         * Creates an EvalScore from a centipawn value.
         */
        fun fromCp(cp: Int): EvalScore = EvalScore(cp = cp)
        
        /**
         * Creates an EvalScore from a mate-in-N value.
         */
        fun fromMate(mate: Int): EvalScore = EvalScore(mate = mate)
    }

    /**
     * Inverts the evaluation perspective (negates cp or mate score).
     */
    fun invert(): EvalScore = EvalScore(
        cp = cp?.let { -it },
        mate = mate?.let { -it }
    )
    
    init {
        require((cp != null) xor (mate != null)) { "EvalScore must have exactly one of cp or mate" }
    }
    
    /**
     * Normalizes this score to a centipawn-equivalent value on a consistent scale.
     * Mate-in-N scores are converted to large bounded centipawn values.
     * 
     * @param perspective The side to move perspective (true = white, false = black)
     * @return Normalized centipawn value from the mover's perspective
     */
    fun toNormalizedCp(perspective: Boolean): Int {
        return if (mate != null) {
            // Mate-in-N: convert to bounded centipawn
            // For the mating side: MAX_CP - (MATE_PENALTY * distance)
            // For the mated side: -(MAX_CP - (MATE_PENALTY * distance))
            val matingSideAdvantage = MAX_CP - (MATE_PENALTY * mate)
            if (perspective) matingSideAdvantage else -matingSideAdvantage
        } else {
            // Already in centipawns, just adjust for perspective
            val cpValue = cp!!
            if (perspective) cpValue else -cpValue
        }
    }
}
