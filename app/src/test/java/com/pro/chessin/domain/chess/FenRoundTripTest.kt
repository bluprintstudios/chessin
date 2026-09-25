package com.pro.chessin.domain.chess

import org.junit.Test
import org.junit.Assert.*

/**
 * FEN round-trip tests to verify FEN import/export correctness.
 */
class FenRoundTripTest {
    
    @Test
    fun testStartingPosition() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testAfterE4() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testMidGamePosition() {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testCastlingRightsLostKingside() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1KNR b Qkq - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testCastlingRightsLostQueenside() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQK2R w Kkq - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testEnPassantSquare() {
        val fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testBlackToMove() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testHalfMoveClock() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 50 25"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testNoCastlingRights() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
    
    @Test
    fun testComplexPosition() {
        val fen = "r2q1rk1/ppp2ppp/2n1pn2/3p4/3P4/2N1PN2/PPP2PPP/R2Q1RK1 w - - 0 10"
        val board = ChessBoardState.fromFEN(fen)
        val exported = board.toFEN()
        assertEquals(fen, exported)
    }
}
