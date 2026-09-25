package com.pro.chessin.domain.chess

import org.junit.Assert.assertEquals
import org.junit.Test

class SanUtilsTest {

    @Test
    fun `moveToSan converts pawn opening move e2e4`() {
        val board = ChessBoardState.startPosition()
        val move = Move.simple(Square.E2, Square.E4, Piece(PieceType.PAWN, Color.WHITE))
        val san = SanUtils.moveToSan(board, move)
        assertEquals("e4", san)
    }

    @Test
    fun `moveToSan converts knight move g1f3`() {
        val board = ChessBoardState.startPosition()
        val move = Move.simple(Square.G1, Square.F3, Piece(PieceType.KNIGHT, Color.WHITE))
        val san = SanUtils.moveToSan(board, move)
        assertEquals("Nf3", san)
    }

    @Test
    fun `moveToSan converts kingside castling`() {
        // Position where kingside castling is legal: e1 to g1
        val fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
        val board = ChessBoardState.fromFEN(fen)
        val move = Move.castling(Square.E1, Square.G1, Piece(PieceType.KING, Color.WHITE), Move.CastlingSide.KINGSIDE)
        val san = SanUtils.moveToSan(board, move)
        assertEquals("O-O", san)
    }
}
