package com.pro.chessin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.Color as ChessColor

/**
 * A popup overlay that lets the user choose a promotion piece type.
 *
 * @param pieceColor The color of the promoting pawn (determines tint of the piece icons).
 * @param onPieceSelected Called with the chosen [PieceType] when the user selects an option.
 * @param onDismiss Called when the user dismisses the picker without choosing.
 * @param modifier Optional modifier for the picker container.
 */
@Composable
fun PromotionPicker(
    pieceColor: ChessColor,
    onPieceSelected: (PieceType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val pieceTint = if (pieceColor == ChessColor.WHITE) {
        Color(0xFFE8E8E8)
    } else {
        Color(0xFF1A1A1A)
    }

    Box(
        modifier = modifier
            .background(
                color = Color(0xFF191E2B),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PromotionOption(
                pieceType = PieceType.QUEEN,
                pieceTint = pieceTint,
                contentDescription = stringResource(id = com.pro.chessin.R.string.promo_queen),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPieceSelected(PieceType.QUEEN)
                }
            )
            PromotionOption(
                pieceType = PieceType.ROOK,
                pieceTint = pieceTint,
                contentDescription = stringResource(id = com.pro.chessin.R.string.promo_rook),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPieceSelected(PieceType.ROOK)
                }
            )
            PromotionOption(
                pieceType = PieceType.BISHOP,
                pieceTint = pieceTint,
                contentDescription = stringResource(id = com.pro.chessin.R.string.promo_bishop),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPieceSelected(PieceType.BISHOP)
                }
            )
            PromotionOption(
                pieceType = PieceType.KNIGHT,
                pieceTint = pieceTint,
                contentDescription = stringResource(id = com.pro.chessin.R.string.promo_knight),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPieceSelected(PieceType.KNIGHT)
                }
            )
        }
    }
}

@Composable
private fun PromotionOption(
    pieceType: PieceType,
    pieceTint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .semantics { testTag = "promotion_option_${pieceType.name}" }
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2E3A))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = piecePainter(pieceType),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(pieceTint),
            modifier = Modifier.size(32.dp)
        )
    }
}
