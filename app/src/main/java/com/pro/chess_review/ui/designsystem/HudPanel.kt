package com.pro.chess_review.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Compact technical frame used by the approved Chessin' HUD handoff. */
@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val frameColor = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier.drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                val inset = 6.dp.toPx()
                val width = size.width
                val height = size.height
                val frame = frameColor.copy(alpha = 0.88f)
                val active = accent.copy(alpha = 0.95f)

                fun segment(
                    start: Offset,
                    end: Offset,
                    color: androidx.compose.ui.graphics.Color = frame,
                ) {
                    drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Square)
                }

                drawRoundRect(
                    color = frame,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
                // The handoff uses one cyan tab at the panel's top-left and one
                // shorter tab at its lower-right; avoid decorative telemetry.
                segment(
                    Offset(inset + 6.dp.toPx(), inset + 2.dp.toPx()),
                    Offset(inset + 37.dp.toPx(), inset + 2.dp.toPx()),
                    active,
                )
                segment(
                    Offset(width - inset - 34.dp.toPx(), height - inset - 2.dp.toPx()),
                    Offset(width - inset - 6.dp.toPx(), height - inset - 2.dp.toPx()),
                    active,
                )
            },
        ) {
            content()
        }
    }
}
