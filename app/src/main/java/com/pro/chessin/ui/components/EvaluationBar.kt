package com.pro.chessin.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vertical evaluation bar showing the current position's evaluation from
 * White's perspective. Designed to sit next to the chess board.
 *
 * - Positive cp = White advantage (blue fill grows upward from center)
 * - Negative cp = Black advantage (red fill grows downward from center)
 * - Center line = even position (0 cp)
 * - The fill animates via [animateFloatAsState] with a [spring] spec,
 *   so dragging through the move list produces smooth interpolation.
 *
 * Handles all evaluation types:
 * - Normal cp scores (positive = White better)
 * - Mate scores (normalized to cp via Phase 5's MAX_CP / MATE_PENALTY)
 * - Resignation games (the eval bar shows the static position eval;
 *   the move list displays the full classification deltas)
 */
@Composable
fun EvaluationBar(
    evalCp: Int,
    modifier: Modifier = Modifier,
    maxCp: Float = 1000f,
) {
    // Clamp the displayed eval to [-maxCp, +maxCp] so mate scores
    // don't compress the entire bar into a single pixel.
    val clampedEval = evalCp.toFloat().coerceIn(-maxCp, maxCp)

    // Animate the interpolation factor using a spring spec.
    val animatedFraction by animateFloatAsState(
        targetValue = (0.5f + clampedEval / (2f * maxCp)),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val fraction = animatedFraction.coerceIn(0f, 1f)

    val whiteFillColor = Color(0xFF4A90D9)   // Blue for White advantage
    val blackFillColor = Color(0xFFE74C3C)   // Red for Black advantage
    val centerLineColor = Color.White.copy(alpha = 0.3f)
    val backgroundColor = Color(0xFF191E2B)
    val labelColor = if (clampedEval >= 0) whiteFillColor else blackFillColor

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val height = size.height
            val width = size.width
            val centerY = height / 2f

            // Background
            drawRect(backgroundColor)

            // White advantage fill (from center downward)
            if (fraction > 0.5f) {
                val whiteHeight = (fraction - 0.5f) * 2f * centerY
                drawRect(
                    color = whiteFillColor,
                    topLeft = Offset(0f, centerY),
                    size = Size(width, whiteHeight)
                )
            }

            // Black advantage fill (from center upward)
            if (fraction < 0.5f) {
                val blackHeight = (0.5f - fraction) * 2f * centerY
                drawRect(
                    color = blackFillColor,
                    topLeft = Offset.Zero,
                    size = Size(width, blackHeight)
                )
            }

            // Center line separating White (bottom) from Black (top)
            drawLine(
                color = centerLineColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2f
            )
        }

        // Eval text centered on the bar
        Text(
            text = if (clampedEval == 0f) {
                "∞"
            } else {
                val sign = if (clampedEval > 0) "+" else ""
                "$sign${clampedEval.toInt()}"
            },
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
