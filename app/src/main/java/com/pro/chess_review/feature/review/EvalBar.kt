package com.pro.chess_review.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import com.pro.chess_review.ui.theme.LocalHudPalette

object EvalMath {
    fun calculateFillPercent(whiteCentipawns: Int?, whiteMateIn: Int?): Float {
        return when {
            whiteMateIn != null -> if (whiteMateIn > 0) 1f else 0f
            whiteCentipawns != null -> {
                val clampedCp = whiteCentipawns.coerceIn(-800, 800)
                0.5f + (clampedCp / 1600f)
            }
            else -> 0.5f
        }
    }

    fun formatScoreText(whiteCentipawns: Int?, whiteMateIn: Int?): String {
        return when {
            whiteMateIn != null -> "M${whiteMateIn.absoluteValue}"
            whiteCentipawns != null -> {
                val score = whiteCentipawns / 100.0
                if (score > 0) "+%.1f".format(score) else "%.1f".format(score)
            }
            else -> "0.0"
        }
    }

    fun contentDescription(whiteCentipawns: Int?, whiteMateIn: Int?): String {
        return when {
            whiteMateIn != null && whiteMateIn > 0 -> "Evaluation: White mates in $whiteMateIn"
            whiteMateIn != null && whiteMateIn < 0 -> "Evaluation: Black mates in ${whiteMateIn.absoluteValue}"
            whiteMateIn == 0 -> "Evaluation: checkmate"
            whiteCentipawns != null && whiteCentipawns > 0 -> "Evaluation: ${formatScoreText(whiteCentipawns, null)}. White is ahead"
            whiteCentipawns != null && whiteCentipawns < 0 -> "Evaluation: ${formatScoreText(whiteCentipawns, null)}. Black is ahead"
            whiteCentipawns != null -> "Evaluation: equal position"
            else -> "Evaluation unavailable"
        }
    }
}

@Composable
fun EvalBar(
    whiteCentipawns: Int?,
    whiteMateIn: Int?,
    isFlipped: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Math to calculate fill percentage (0.0 = all black, 1.0 = all white)
    val fillPercent = EvalMath.calculateFillPercent(whiteCentipawns, whiteMateIn)
    val displayPercent = if (isFlipped) 1f - fillPercent else fillPercent

    val scoreText = EvalMath.formatScoreText(whiteCentipawns, whiteMateIn)
    val palette = LocalHudPalette.current

    Box(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .background(palette.boardDark)
            .semantics {
                contentDescription = EvalMath.contentDescription(whiteCentipawns, whiteMateIn)
            }
    ) {
        // White side fill
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxHeight(displayPercent)
                .background(palette.boardLight)
                .align(if (isFlipped) Alignment.TopCenter else Alignment.BottomCenter)
        )

        // Score text overlay
        Text(
            text = scoreText,
            fontSize = 10.sp,
            color = if (fillPercent > 0.5f) palette.background else palette.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(if (fillPercent > 0.5f) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(bottom = 4.dp, top = 4.dp)
        )
    }
}
