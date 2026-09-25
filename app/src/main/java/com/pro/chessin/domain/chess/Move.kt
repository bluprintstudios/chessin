package com.pro.chessin.domain.chess

/**
 * Represents a chess move.
 */
data class Move(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val castlingSide: CastlingSide? = null
) {
    enum class CastlingSide {
        KINGSIDE, QUEENSIDE
    }
    
    val isPromotion: Boolean get() = promotion != null
    
    override fun toString(): String {
        val moveStr = "${from.name}${to.name}"
        return if (isPromotion) {
            "$moveStr=${promotion?.name?.get(0)?.lowercaseChar()}"
        } else {
            moveStr
        }
    }
    
    companion object {
        fun simple(from: Square, to: Square, piece: Piece): Move {
            return Move(from, to, piece)
        }
        
        fun capture(from: Square, to: Square, piece: Piece): Move {
            return Move(from, to, piece, isCapture = true)
        }
        
        fun enPassant(from: Square, to: Square, piece: Piece): Move {
            return Move(from, to, piece, isCapture = true, isEnPassant = true)
        }
        
        fun promotion(from: Square, to: Square, piece: Piece, promotionType: PieceType): Move {
            return Move(from, to, piece, promotion = promotionType, isCapture = false)
        }
        
        fun promotionCapture(from: Square, to: Square, piece: Piece, promotionType: PieceType): Move {
            return Move(from, to, piece, promotion = promotionType, isCapture = true)
        }
        
        fun castling(from: Square, to: Square, piece: Piece, side: CastlingSide): Move {
            return Move(from, to, piece, isCastling = true, castlingSide = side)
        }
    }
}
