package com.pro.chessin.domain.chess

/**
 * Zobrist hashing for position hashing (used for threefold repetition detection).
 * Uses precomputed random numbers for each piece-square combination and game state.
 */
object ZobristHash {
    private val pieceSquare = Array(12) { Array(64) { 0L } }
    private val castlingRights = Array(16) { 0L }
    private val enPassantFile = Array(8) { 0L }
    private val sideToMove = 0L
    
    init {
        // Initialize with deterministic pseudo-random values
        var seed = 0x123456789ABCDEFL
        for (piece in 0..11) {
            for (square in 0..63) {
                seed = random64(seed)
                pieceSquare[piece][square] = seed
            }
        }
        for (i in 0..15) {
            seed = random64(seed)
            castlingRights[i] = seed
        }
        for (file in 0..7) {
            seed = random64(seed)
            enPassantFile[file] = seed
        }
    }
    
    private fun random64(seed: Long): Long {
        var s = seed
        s = s xor (s shr 15)
        s = s * 0x2C1B3C6D66BEBD5DL
        s = s xor (s shr 31)
        s = s * 0x49A8F39456F34A21L
        s = s xor (s shr 15)
        return s
    }
    
    fun pieceIndex(piece: Piece): Int {
        val typeIndex = piece.type.ordinal
        val colorOffset = if (piece.color == Color.WHITE) 0 else 6
        return typeIndex + colorOffset
    }
    
    fun hashPiece(piece: Piece, square: Square, hash: Long): Long {
        return hash xor pieceSquare[pieceIndex(piece)][square.index]
    }
    
    fun hashCastling(rights: CastlingRights, hash: Long): Long {
        val index = (if (rights.whiteKingside) 1 else 0) or
                   (if (rights.whiteQueenside) 2 else 0) or
                   (if (rights.blackKingside) 4 else 0) or
                   (if (rights.blackQueenside) 8 else 0)
        return hash xor castlingRights[index]
    }
    
    fun hashEnPassant(square: Square?, hash: Long): Long {
        return if (square != null) {
            hash xor enPassantFile[square.file]
        } else {
            hash
        }
    }
    
    fun hashSideToMove(hash: Long): Long {
        return hash xor sideToMove
    }
}
