package com.pro.chess_review.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun Chess_reviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeId: String = HudThemeId.ARCTIC.storageId,
    content: @Composable () -> Unit,
) {
    // Keep this parameter so previews and existing callers remain source-compatible.
    @Suppress("UNUSED_VARIABLE") val ignoredSystemDarkPreference = darkTheme
    val palette = hudPalette(themeId)
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.background,
        primaryContainer = palette.accentSoft,
        onPrimaryContainer = palette.text,
        secondary = palette.mutedText,
        onSecondary = palette.background,
        secondaryContainer = palette.elevatedSurface,
        onSecondaryContainer = palette.text,
        tertiary = palette.warning,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.elevatedSurface,
        onSurfaceVariant = palette.mutedText,
        outline = palette.frame,
        error = palette.critical,
    )

    CompositionLocalProvider(LocalHudPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
