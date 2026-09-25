package com.pro.chessin.ui.theme

import androidx.compose.ui.graphics.Color
import com.pro.chessin.domain.analysis.ClassificationTier

/**
 * Single source of truth for mapping [ClassificationTier] values to UI colors.
 *
 * Per AGENTS.md: "Define ClassificationColors as a single mapping in Phase 6/7 UI
 * code — don't scatter inline hex per composable."
 *
 * Move highlight convention: green = best/good move, red = blunder.
 */
object ClassificationColors {

    /**
     * Full-saturation color for each tier — used for badges and text.
     */
    val colors: Map<ClassificationTier, Color> = mapOf(
        ClassificationTier.BOOK      to Color(0xFF616161),  // Grey
        ClassificationTier.BEST      to Color(0xFF4CAF50),   // Green
        ClassificationTier.EXCELLENT to Color(0xFF8BC34A),   // Light green
        ClassificationTier.GOOD      to Color(0xFFCDDC39),   // Yellow-green
        ClassificationTier.INACCURACY to Color(0xFFFFEB3B),  // Yellow
        ClassificationTier.MISTAKE   to Color(0xFFFF9800), // Orange
        ClassificationTier.BLUNDER   to Color(0xFFF44336),   // Red
        ClassificationTier.MISS      to Color(0xFF9C27B0),  // Purple
    )

    /**
     * Background tint alpha for each tier — used as move-list row backgrounds.
     */
    val backgroundAlphas: Map<ClassificationTier, Float> = mapOf(
        ClassificationTier.BOOK      to 0.12f,
        ClassificationTier.BEST      to 0.20f,
        ClassificationTier.EXCELLENT to 0.18f,
        ClassificationTier.GOOD      to 0.18f,
        ClassificationTier.INACCURACY to 0.25f,
        ClassificationTier.MISTAKE   to 0.25f,
        ClassificationTier.BLUNDER   to 0.30f,
        ClassificationTier.MISS      to 0.20f,
    )

    /**
     * Returns the full-saturation color for the given tier.
     */
    fun forTier(tier: ClassificationTier): Color =
        colors[tier] ?: Color.Gray

    /**
     * Semi-transparent background tint for the given tier,
     * suitable as a row background in the move list.
     */
    fun backgroundForTier(tier: ClassificationTier): Color {
        val base = colors[tier] ?: Color.Gray
        val alpha = backgroundAlphas[tier] ?: 0.15f
        return base.copy(alpha = alpha)
    }

    /**
     * Text color for content displayed on top of [backgroundForTier].
     * White text on all backgrounds for readability on dark theme.
     */
    fun textForTier(tier: ClassificationTier): Color = Color.White

    /**
     * Color for the delta cp text in move-list rows, severity-based.
     * Red → orange → yellow → green as the loss gets smaller.
     */
    fun deltaColorForCp(cp: Int): Color = when {
        cp > 300 -> Color(0xFFF44336)   // Red        — blunder-scale
        cp > 100 -> Color(0xFFFF9800)   // Orange      — mistake-scale
        cp > 50  -> Color(0xFFFFEB3B)  // Yellow      — inaccuracy-scale
        cp > 0   -> Color(0xFFCDDC39)  // Yellow-green — small loss
        else     -> Color(0xFF8BC34A) // Green       — no loss / improvement
    }

    /**
     * Human-readable label for the given tier.
     */
    fun labelForTier(tier: ClassificationTier): String = when (tier) {
        ClassificationTier.BOOK -> "Book"
        ClassificationTier.BEST -> "Best"
        ClassificationTier.EXCELLENT -> "Excellent"
        ClassificationTier.GOOD -> "Good"
        ClassificationTier.INACCURACY -> "Inaccuracy"
        ClassificationTier.MISTAKE -> "Mistake"
        ClassificationTier.BLUNDER -> "Blunder"
        ClassificationTier.MISS -> "Miss"
    }
}
