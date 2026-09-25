package com.pro.chessin.ui.theme

import androidx.compose.ui.graphics.Color
import com.pro.chessin.domain.analysis.ClassificationTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [ClassificationColors] — the single source of truth that maps
 * [ClassificationTier] values to UI colors and severity-based delta colors.
 *
 * These tests verify that:
 * - Every tier has a color, background alpha, label, and delta color defined
 * - Delta colors change severity at the correct cp thresholds
 * - Background alphas are in valid range [0, 1]
 */
class ClassificationColorsTest {

    @Test
    fun allTiers_HaveColorsDefined() {
        ClassificationTier.entries.forEach { tier ->
            val color = ClassificationColors.forTier(tier)
            assertNotNull("Color should be defined for $tier", color)
            assert(color != Color.Unspecified) { "Color should not be Unspecified for $tier" }
        }
    }

    @Test
    fun allTiers_HaveBackgroundAlpha() {
        ClassificationTier.entries.forEach { tier ->
            val bg = ClassificationColors.backgroundForTier(tier)
            val alpha = bg.alpha
            assert(alpha >= 0f && alpha <= 1f) {
                "Background alpha for $tier must be in [0,1], got $alpha"
            }
        }
    }

    @Test
    fun allTiers_HaveLabels() {
        ClassificationTier.entries.forEach { tier ->
            val label = ClassificationColors.labelForTier(tier)
            assert(label.isNotBlank()) { "Label for $tier must not be blank" }
        }
    }

    @Test
    fun deltaColor_RedForBlunderScale() {
        // > 300 cp loss → Red
        assertEquals(Color(0xFFF44336), ClassificationColors.deltaColorForCp(301))
        assertEquals(Color(0xFFF44336), ClassificationColors.deltaColorForCp(1000))
    }

    @Test
    fun deltaColor_OrangeForMistakeScale() {
        // 101-300 cp → Orange
        assertEquals(Color(0xFFFF9800), ClassificationColors.deltaColorForCp(101))
        assertEquals(Color(0xFFFF9800), ClassificationColors.deltaColorForCp(300))
    }

    @Test
    fun deltaColor_YellowForInaccuracyScale() {
        // 51-100 cp → Yellow
        assertEquals(Color(0xFFFFEB3B), ClassificationColors.deltaColorForCp(51))
        assertEquals(Color(0xFFFFEB3B), ClassificationColors.deltaColorForCp(100))
    }

    @Test
    fun deltaColor_YellowGreenForSmallLoss() {
        // 1-50 cp → Yellow-green
        assertEquals(Color(0xFFCDDC39), ClassificationColors.deltaColorForCp(1))
        assertEquals(Color(0xFFCDDC39), ClassificationColors.deltaColorForCp(50))
    }

    @Test
    fun deltaColor_GreenForNoLoss() {
        // 0 or negative delta → Green
        assertEquals(Color(0xFF8BC34A), ClassificationColors.deltaColorForCp(0))
        assertEquals(Color(0xFF8BC34A), ClassificationColors.deltaColorForCp(-1))
        assertEquals(Color(0xFF8BC34A), ClassificationColors.deltaColorForCp(-500))
    }

    @Test
    fun deltaColor_BoundaryAt300() {
        // Exactly 300 is the MISTAKE/BLUNDER boundary
        assertEquals(Color(0xFFFF9800), ClassificationColors.deltaColorForCp(300))
        assertEquals(Color(0xFFF44336), ClassificationColors.deltaColorForCp(301))
    }

    @Test
    fun deltaColor_BoundaryAt100() {
        // Exactly 100 is the INACCURACY/MISTAKE boundary
        assertEquals(Color(0xFFFFEB3B), ClassificationColors.deltaColorForCp(100))
        assertEquals(Color(0xFFFF9800), ClassificationColors.deltaColorForCp(101))
    }

    @Test
    fun deltaColor_BoundaryAt50() {
        // At cp=50: 50 > 50 is false -> falls to cp > 0 -> Yellow-green (0xFFCDDC39)
        // At cp=51: 51 > 50 is true -> Yellow (0xFFFFEB3B)
        // The >50 threshold is strict (greater-than, not >=), matching deltaColor_YellowGreenForSmallLoss
        assertEquals(Color(0xFFCDDC39), ClassificationColors.deltaColorForCp(50))
        assertEquals(Color(0xFFFFEB3B), ClassificationColors.deltaColorForCp(51))
    }

    @Test
    fun deltaColor_BoundaryAtPositiveZero() {
        // Exactly 0 and negative values are green (no loss)
        assertEquals(Color(0xFF8BC34A), ClassificationColors.deltaColorForCp(0))
        assertEquals(Color(0xFF8BC34A), ClassificationColors.deltaColorForCp(-1))
    }

    @Test
    fun backgroundForTier_UsesCorrectAlpha() {
        // Book should have low alpha (subtle)
        val bookBg = ClassificationColors.backgroundForTier(ClassificationTier.BOOK)
        // Compose Color quantizes alpha to 8-bit (0.12f -> 31/255 ~ 0.1216)
        assertEquals(0.12f, bookBg.alpha, 0.01f)

        // Blunder should have high alpha (prominent warning)
        val blunderBg = ClassificationColors.backgroundForTier(ClassificationTier.BLUNDER)
        // 0.30f -> 77/255 ~ 0.3020
        assertEquals(0.30f, blunderBg.alpha, 0.01f)
    }

    @Test
    fun backgroundForTier_PreservesBaseColor() {
        // The background color should be a tint of the tier's base color
        ClassificationTier.entries.forEach { tier ->
            val base = ClassificationColors.forTier(tier)
            val bg = ClassificationColors.backgroundForTier(tier)
            assertEquals("Red channel should match for $tier", base.red, bg.red, 0.001f)
            assertEquals("Green channel should match for $tier", base.green, bg.green, 0.001f)
            assertEquals("Blue channel should match for $tier", base.blue, bg.blue, 0.001f)
        }
    }

    @Test
    fun moveHighlightConvention_FollowsDesignSystem() {
        // Per AGENTS.md: green = best/good move, red = blunder
        assertEquals(Color(0xFF4CAF50), ClassificationColors.forTier(ClassificationTier.BEST))
        assertEquals(Color(0xFFF44336), ClassificationColors.forTier(ClassificationTier.BLUNDER))
    }
}
