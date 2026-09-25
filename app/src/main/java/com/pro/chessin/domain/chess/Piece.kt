package com.pro.chessin.domain.chess

/**
 * Represents a chess piece with its color and type.
 */
enum class PieceType {
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

enum class Color {
    WHITE, BLACK;

    fun opposite(): Color = if (this == WHITE) BLACK else WHITE
}

data class Piece(
    val type: PieceType,
    val color: Color
) {
    companion object {
        fun fromChar(char: Char): Piece? {
            val color = if (char.isUpperCase()) Color.WHITE else Color.BLACK
            val type = when (char.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return Piece(type, color)
        }

        fun toChar(piece: Piece): Char {
            val base = when (piece.type) {
                PieceType.PAWN -> 'p'
                PieceType.KNIGHT -> 'n'
                PieceType.BISHOP -> 'b'
                PieceType.ROOK -> 'r'
                PieceType.QUEEN -> 'q'
                PieceType.KING -> 'k'
            }
            return if (piece.color == Color.WHITE) base.uppercaseChar() else base
        }
    }
}
