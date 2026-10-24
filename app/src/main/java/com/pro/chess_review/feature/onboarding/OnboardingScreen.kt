package com.pro.chess_review.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pro.chess_review.ui.designsystem.HudPanel
import com.pro.chess_review.ui.designsystem.HudScreenHeader
import com.pro.chess_review.ui.theme.LocalHudPalette

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onImportPgn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HudScreenHeader(title = "ONBOARDING", routeLabel = "onboarding")

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = "WELCOME TO CHESSIN'",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Study the game\nyou just played.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            WelcomeBoard(modifier = Modifier.weight(0.82f))
            HudPanel(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .clickable(onClick = onImportPgn),
            ) {
                Column(
                modifier = Modifier.padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "LOCAL FIRST",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Import completed PGNs and replay them privately on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "IMPORT PGN  →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        HudPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onContinue),
        ) {
            Column(
                modifier = Modifier.padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "NEXT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Continue to your game library.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "OPEN LIBRARY  →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun WelcomeBoard(modifier: Modifier = Modifier) {
    val palette = LocalHudPalette.current
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 116.dp, height = 88.dp)) {
            val cellWidth = size.width / 4f
            val cellHeight = size.height / 4f
            repeat(4) { row ->
                repeat(4) { column ->
                    drawRoundRect(
                        color = if ((row + column) % 2 == 0) palette.frame.copy(alpha = 0.55f) else palette.surface,
                        topLeft = androidx.compose.ui.geometry.Offset(column * cellWidth, row * cellHeight),
                        size = androidx.compose.ui.geometry.Size(cellWidth - 3.dp.toPx(), cellHeight - 3.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                    )
                }
            }
            drawRoundRect(
                color = outline,
                style = Stroke(width = 1.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            )
        }
    }
}
