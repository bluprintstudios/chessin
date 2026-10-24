package com.pro.chess_review.core.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class UciParserTest {

    @Test
    fun parseUciLine_whiteToMove_returnsWhitePerspective() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val line = UciParser.parseUciLine(fen, listOf("e2e4"), cpString = "45", mateString = null, depth = 18)
        
        assertEquals(45, line.centipawns)
        assertEquals(null, line.mateIn)
        assertEquals(listOf("e2e4"), line.uciMoves)
        assertEquals(listOf("e4"), line.sanMoves)
    }

    @Test
    fun parseUciLine_legalContinuation_returnsSanForEachMove() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val line = UciParser.parseUciLine(
            fen,
            listOf("e2e4", "e7e5", "g1f3"),
            cpString = "12",
            mateString = null,
            depth = 18
        )

        assertEquals(listOf("e2e4", "e7e5", "g1f3"), line.uciMoves)
        assertEquals(listOf("e4", "e5", "Nf3"), line.sanMoves)
    }

    @Test
    fun parseUciLine_blackToMove_normalizesToWhitePerspective() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        // Black is to move. If Stockfish reports cp=50 (meaning +0.50 for Black),
        // we expect the parser to invert it to -50 so it's always White's perspective.
        val line = UciParser.parseUciLine(fen, listOf("e7e5"), cpString = "50", mateString = null, depth = 18)
        
        assertEquals(-50, line.centipawns)
    }
    
    @Test
    fun parseUciLine_mateForBlackToMove_normalizesMate() {
        val fen = "4k3/8/8/8/8/8/8/4K2R b K - 0 1" // Black to move
        // Black has mate in 3 (so mate=3 from Black's view)
        // White perspective should be mate in -3
        val line = UciParser.parseUciLine(fen, listOf("e8d8"), cpString = null, mateString = "3", depth = 18)
        
        assertEquals(-3, line.mateIn)
    }
    
    @Test
    fun parseUciLine_invalidMoveStopsParsing() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        // e2e5 is invalid for a pawn
        val line = UciParser.parseUciLine(fen, listOf("e2e4", "e2e5", "g1f3"), cpString = "10", mateString = null, depth = 18)
        
        assertEquals(listOf("e2e4"), line.uciMoves)
        assertEquals(listOf("e4"), line.sanMoves)
    }
}
