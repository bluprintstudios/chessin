package com.pro.chessin.domain.chess

/**
 * Castling rights for both sides.
 */
data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun hasKingsideRights(color: Color): Boolean {
        return if (color == Color.WHITE) whiteKingside else blackKingside
    }
    
    fun hasQueensideRights(color: Color): Boolean {
        return if (color == Color.WHITE) whiteQueenside else blackQueenside
    }
    
    fun removeKingside(color: Color): CastlingRights {
        return if (color == Color.WHITE) copy(whiteKingside = false) else copy(blackKingside = false)
    }
    
    fun removeQueenside(color: Color): CastlingRights {
        return if (color == Color.WHITE) copy(whiteQueenside = false) else copy(blackQueenside = false)
    }
    
    fun removeAll(color: Color): CastlingRights {
        return if (color == Color.WHITE) copy(whiteKingside = false, whiteQueenside = false)
        else copy(blackKingside = false, blackQueenside = false)
    }
    
    companion object {
        val ALL = CastlingRights()
        val NONE = CastlingRights(false, false, false, false)
    }
}
