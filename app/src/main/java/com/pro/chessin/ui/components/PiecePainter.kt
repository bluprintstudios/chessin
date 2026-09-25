package com.pro.chessin.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.pro.chessin.R
import com.pro.chessin.domain.chess.PieceType

/**
 * Maps a [PieceType] to its corresponding vector drawable [Painter].
 * The drawable is tinted (white or black) when used in [Image].
 */
@Composable
fun piecePainter(pieceType: PieceType): Painter = painterResource(
    id = pieceResId(pieceType)
)

@DrawableRes
private fun pieceResId(pieceType: PieceType): Int = when (pieceType) {
    PieceType.KING -> R.drawable.ic_piece_king
    PieceType.QUEEN -> R.drawable.ic_piece_queen
    PieceType.ROOK -> R.drawable.ic_piece_rook
    PieceType.BISHOP -> R.drawable.ic_piece_bishop
    PieceType.KNIGHT -> R.drawable.ic_piece_knight
    PieceType.PAWN -> R.drawable.ic_piece_pawn
}
