package com.pro.chessin.domain.analysis

/**
 * Classification tier for a chess move based on evaluation delta.
 * These are named constants (not inline numbers) for consistency and testability.
 */
enum class ClassificationTier {
    /** Move from opening book (matched before engine analysis) */
    BOOK,
    
    /** Best move - matches top engine line exactly */
    BEST,
    
    /** Excellent move - very small evaluation loss or gain */
    EXCELLENT,
    
    /** Good move - acceptable position maintained */
    GOOD,
    
    /** Inaccuracy - small but noticeable evaluation loss */
    INACCURACY,
    
    /** Mistake - significant evaluation loss */
    MISTAKE,
    
    /** Blunder - severe evaluation loss, likely losing the game */
    BLUNDER,
    
    /** Miss - failed to find a forced mate or large tactical win */
    MISS
}
