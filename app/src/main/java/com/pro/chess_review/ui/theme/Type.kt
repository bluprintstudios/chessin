package com.pro.chess_review.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Dense, neutral typography matching the supplied HUD overview. */
private val HudSans = FontFamily.SansSerif
private fun hudStyle(weight: FontWeight, size: Int, line: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = HudSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val Typography = Typography(
    displayLarge = hudStyle(FontWeight.Bold, 20, 23),
    displayMedium = hudStyle(FontWeight.Bold, 18, 21),
    displaySmall = hudStyle(FontWeight.Bold, 16, 19),
    headlineLarge = hudStyle(FontWeight.Bold, 16, 19),
    headlineMedium = hudStyle(FontWeight.Bold, 14, 17),
    headlineSmall = hudStyle(FontWeight.Bold, 12, 15),
    titleLarge = hudStyle(FontWeight.SemiBold, 12, 15),
    titleMedium = hudStyle(FontWeight.SemiBold, 10, 13),
    titleSmall = hudStyle(FontWeight.SemiBold, 9, 12),
    bodyLarge = hudStyle(FontWeight.Normal, 10, 13),
    bodyMedium = hudStyle(FontWeight.Normal, 9, 12),
    bodySmall = hudStyle(FontWeight.Normal, 8, 10),
    labelLarge = hudStyle(FontWeight.SemiBold, 8, 10, 0.6f),
    labelMedium = hudStyle(FontWeight.SemiBold, 7, 9, 0.6f),
    labelSmall = hudStyle(FontWeight.SemiBold, 6, 8, 0.6f),
)
