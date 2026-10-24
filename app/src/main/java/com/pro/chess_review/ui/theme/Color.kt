package com.pro.chess_review.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A display theme changes HUD treatment, never the meaning of review data. */
enum class HudThemeId(val storageId: String, val label: String, val shortLabel: String) {
    ARCTIC("arctic", "Arctic HUD", "ARCTIC"),
    SLATE("slate", "Slate HUD", "SLATE"),
    VIOLET("violet", "Ultraviolet", "VIOLET"),
    TERMINAL("terminal", "Terminal Green", "TERMINAL"),
    WHITE("white", "White Grid", "WHITE");

    companion object {
        fun fromStorageId(value: String?): HudThemeId =
            when (value) {
                // Preserve the existing saved choice while replacing the old orange palette.
                "amber" -> SLATE
                else -> entries.firstOrNull { it.storageId == value } ?: ARCTIC
            }
    }
}

data class HudPalette(
    val id: HudThemeId,
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val frame: Color,
    val text: Color,
    val mutedText: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val critical: Color,
    val boardLight: Color,
    val boardDark: Color,
)

private val Arctic = HudPalette(
    // Canonical values from the approved HUD handoff. Other selectable schemes
    // deliberately retain their own identity, but Arctic is the default target.
    HudThemeId.ARCTIC, Color(0xFF090A0C), Color(0xFF111417), Color(0xFF181B1F),
    Color(0xFF607280), Color(0xFFF5F7F8), Color(0xFFB8C0C6), Color(0xFF91D7EF),
    Color(0xFF363D43), Color(0xFF9ACB92), Color(0xFFE0C06B), Color(0xFFEA8C94),
    Color(0xFFE3E6E8), Color(0xFF59636B),
)

private val Slate = HudPalette(
    HudThemeId.SLATE, Color(0xFF12171C), Color(0xFF1A2229), Color(0xFF222D35),
    Color(0xFF607280), Color(0xFFF2F5F6), Color(0xFFB2BDC5), Color(0xFFAAB7C2),
    Color(0xFF34414B), Color(0xFF9ACB92), Color(0xFFDFC36B), Color(0xFFE68C91),
    Color(0xFFD9DEE2), Color(0xFF53616D),
)

private val Violet = HudPalette(
    HudThemeId.VIOLET, Color(0xFF100D1C), Color(0xFF1B162B), Color(0xFF272044),
    Color(0xFF716594), Color(0xFFF5F1FF), Color(0xFFC8BDE1), Color(0xFFB79BFF),
    Color(0xFF473B72), Color(0xFF9ED39B), Color(0xFFF0BE71), Color(0xFFFF8FBC),
    Color(0xFFE1D7F0), Color(0xFF5A4E7A),
)

private val Terminal = HudPalette(
    HudThemeId.TERMINAL, Color(0xFF090B0D), Color(0xFF111417), Color(0xFF1A1E22),
    Color(0xFF4E6259), Color(0xFFEFF9F1), Color(0xFFB1C3B8), Color(0xFF76E3A5),
    Color(0xFF20382B), Color(0xFF9CDB87), Color(0xFFE4C36F), Color(0xFFF18B82),
    Color(0xFFD8E1DA), Color(0xFF46554C),
)

private val White = HudPalette(
    HudThemeId.WHITE, Color(0xFF090A0C), Color(0xFF101215), Color(0xFF181B1F),
    Color(0xFF6E7881), Color(0xFFF5F7F8), Color(0xFFB8C0C6), Color(0xFFF5F7F8),
    Color(0xFF363D43), Color(0xFF9ACB92), Color(0xFFE0C06B), Color(0xFFEA8C94),
    Color(0xFFE3E6E8), Color(0xFF59636B),
)

fun hudPalette(themeId: String): HudPalette = when (HudThemeId.fromStorageId(themeId)) {
    HudThemeId.ARCTIC -> Arctic
    HudThemeId.SLATE -> Slate
    HudThemeId.VIOLET -> Violet
    HudThemeId.TERMINAL -> Terminal
    HudThemeId.WHITE -> White
}

val LocalHudPalette = staticCompositionLocalOf { Arctic }
