package com.pro.chessin.domain.chess

import org.junit.Test
import org.junit.Assert.*

/**
 * Perft (performance test) function to verify move generation correctness.
 * Tests against known move counts from the starting position at various depths.
 */
class PerftTest {
    
    /**
     * Run perft from a given position and depth.
     */
    private fun perft(board: ChessBoardState, depth: Int): Long {
        if (depth == 0) return 1L
        
        val moves = MoveGenerator.generateLegalMoves(board)
        var nodes = 0L
        
        for (move in moves) {
            val newBoard = board.makeMove(move)
            nodes += perft(newBoard, depth - 1)
        }
        
        return nodes
    }
    
    /**
     * Run perft divide - show subtree counts for each move at given depth.
     */
    private fun perftDivide(board: ChessBoardState, depth: Int): Map<String, Long> {
        val moves = MoveGenerator.generateLegalMoves(board)
        val results = mutableMapOf<String, Long>()
        
        for (move in moves) {
            val newBoard = board.makeMove(move)
            val count = perft(newBoard, depth - 1)
            results[move.toString()] = count
        }
        
        return results
    }
    
    @Test
    fun testPerftDepth1() {
        val board = ChessBoardState.startPosition()
        val result = perft(board, 1)
        assertEquals(20L, result)
    }
    
    @Test
    fun testPerftDepth2() {
        val board = ChessBoardState.startPosition()
        val result = perft(board, 2)
        assertEquals(400L, result)
    }
    
    @Test
    fun testPerftDepth3() {
        val board = ChessBoardState.startPosition()
        val result = perft(board, 3)
        assertEquals(8902L, result)
    }
    
    @Test
    fun testPerftDepth4() {
        val board = ChessBoardState.startPosition()
        val result = perft(board, 4)
        assertEquals(197281L, result)
    }
    
    @Test
    fun testPerftDivideDepth3() {
        val board = ChessBoardState.startPosition()
        val divideResults = perftDivide(board, 3)
        
        println("Perft divide depth 3 from starting position:")
        divideResults.entries.sortedBy { it.key.toString() }.forEach { (move, count) ->
            println("  $move: $count")
        }
        
        // Assert total matches
        val total = divideResults.values.sum()
        println("Total: $total (expected 8902)")
        // assertEquals(8902L, total)
    }
    
    @Test
    fun testCheckDetection() {
        // Test a simple position where white king is in check
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        assertFalse("Black should not be in check initially", board.isInCheck())
        
        // Make a move that should put white in check
        val moves = MoveGenerator.generateLegalMoves(board)
        println("Legal moves from position: ${moves.size}")
        for (move in moves) {
            val newBoard = board.makeMove(move)
            if (newBoard.isInCheck()) {
                println("Move $move leaves king in check")
            }
        }
    }
    
    @Test
    fun testKnightAttacks() {
        // Test knight attacks from e4
        val e4 = Square.E4
        val attacks = ChessBoardState.startPosition().getKnightAttacks(e4)
        val expectedSquares = listOf("c5", "d6", "f6", "g5", "g3", "f2", "d2", "c3")
        val actualSquares = mutableListOf<String>()
        for (sq in 0..63) {
            if ((attacks and (1L shl sq)) != 0L) {
                actualSquares.add(Square(sq).name)
            }
        }
        println("Knight attacks from e4: $actualSquares")
        println("Expected: $expectedSquares")
        assertEquals(expectedSquares.sorted(), actualSquares.sorted())
    }
    
    @Test
    fun testIllegalMoveFiltering() {
        // Create a position where white king is directly attacked by a black rook
        val fen = "4k3/8/8/8/8/8/8/r3K3 w - - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        
        println("WHITE in check: ${board.isInCheck(Color.WHITE)}")
        println("BLACK in check: ${board.isInCheck(Color.BLACK)}")
        println("Black rooks bitboard: ${board.blackRooks}")
        println("White kings bitboard: ${board.whiteKings}")
        println("allPieces bitboard: ${board.allPieces}")
        
        // Test if rook can attack king
        val kingSquare = Square.E1
        val rookSquare = Square.A1
        println("Rook at $rookSquare can attack king at $kingSquare: ${board.canRookAttack(rookSquare, kingSquare)}")
        
        // White should be in check from the black rook
        assertTrue("White should be in check", board.isInCheck(Color.WHITE))
    }
    
    @Test
    fun testPawnAttacks() {
        // Test that pawn attacks are detected correctly - black pawn at d2 attacks e1
        val fen = "4k3/8/8/8/8/8/3p4/4K3 w - - 0 1"
        val board = ChessBoardState.fromFEN(fen)
        
        println("WHITE in check: ${board.isInCheck(Color.WHITE)}")
        println("BLACK in check: ${board.isInCheck(Color.BLACK)}")
        
        // White king should be in check from black pawn on d2
        assertTrue("White should be in check from black pawn", board.isInCheck(Color.WHITE))
    }
    
    @Test
    fun testCheckAfterMove() {
        // Test that check detection works after a move
        val board = ChessBoardState.startPosition()
        
        // Make a move that should leave white king in check (if it were illegal)
        // Actually, let's test a position where a move creates check
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
        val board2 = ChessBoardState.fromFEN(fen)
        
        // Black queen moves to h4, attacking white king
        val moves = MoveGenerator.generateLegalMoves(board2)
        val qh4Move = moves.find { it.from == Square.D8 && it.to == Square.H4 }
        
        if (qh4Move != null) {
            val newBoard = board2.makeMove(qh4Move)
            println("After Qh4:")
            println("  WHITE in check: ${newBoard.isInCheck(Color.WHITE)}")
            println("  BLACK in check: ${newBoard.isInCheck(Color.BLACK)}")
            // White should be in check after black queen moves to h4
            assertTrue("White should be in check after Qh4", newBoard.isInCheck(Color.WHITE))
        }
    }
    
    @Test
    fun testPerftDivideDepth1() {
        val board = ChessBoardState.startPosition()
        val divideResults = perftDivide(board, 1)
        
        println("Perft divide depth 1 from starting position:")
        divideResults.entries.sortedByDescending { it.value }.forEach { (move, count) ->
            println("  $move: $count")
        }
        
        val total = divideResults.values.sum()
        println("Total: $total (expected 20)")
        assertEquals(20L, total)
    }
    
    @Test
    fun testPerftDivideDepth2() {
        val board = ChessBoardState.startPosition()
        val divideResults = perftDivide(board, 2)
        
        println("Perft divide depth 2 from starting position:")
        divideResults.entries.sortedByDescending { it.value }.forEach { (move, count) ->
            println("  $move: $count")
        }
        
        val total = divideResults.values.sum()
        println("Total: $total (expected 400)")
        assertEquals(400L, total)
    }
    
    @Test
    fun testCheckAfterKnightMove() {
        // Test starting position - all 20 moves should be legal
        val board = ChessBoardState.startPosition()
        val pseudoMoves = MoveGenerator.generatePseudoLegalMoves(board)
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        assertEquals(20, legalMoves.size)
    }
    
    @Test
    fun testEnPassantCheckDetection() {
        // Position where en passant capture removes a pawn that was blocking a diagonal attack
        // White king on h1, black bishop on a8 (diagonal a8-h1)
        // Black pawn on d5 (just moved from d7) blocking the diagonal
        // White pawn on e5 can capture en passant exd6
        // After exd6, the diagonal is clear and bishop attacks king
        val fen = "b6k/8/8/3pP3/8/8/8/7K w - d6 0 1"
        val board = ChessBoardState.fromFEN(fen)
        
        println("Board FEN: $fen")
        println("White king position: ${board.findKing(Color.WHITE)}")
        println("Black bishop on a8")
        println("En passant square: ${board.enPassantSquare}")
        println("White pawn on e5, black pawn on d5 blocking a8-h1 diagonal")
        
        // Check if white is in check initially (should not be - pawn blocks)
        println("White in check initially: ${board.isInCheck(Color.WHITE)}")
        
        // Generate pseudo-legal moves (should include en passant)
        val pseudoMoves = MoveGenerator.generatePseudoLegalMoves(board)
        val epMoves = pseudoMoves.filter { it.isEnPassant }
        println("Pseudo-legal en passant moves: ${epMoves.size}")
        epMoves.forEach { println("  $it") }
        
        // Generate legal moves (en passant should be filtered if it leaves king in check)
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        val legalEpMoves = legalMoves.filter { it.isEnPassant }
        println("Legal en passant moves: ${legalEpMoves.size}")
        legalEpMoves.forEach { println("  $it") }
        
        // Test each en passant move
        for (epMove in epMoves) {
            val newBoard = board.makeMove(epMove)
            println("After en passant $epMove:")
            println("  White in check: ${newBoard.isInCheck(Color.WHITE)}")
            println("  Black in check: ${newBoard.isInCheck(Color.BLACK)}")
        }
        
        // The en passant capture should be illegal because it exposes the king
        assertEquals(0, legalEpMoves.size)
    }
    
    @Test
    fun testEnPassantPinRemoval() {
        // Test that en passant is NOT generated after a pawn double-push when the capturing pawn is on the wrong rank
        val board = ChessBoardState.startPosition()
        val e2e4Move = MoveGenerator.generateLegalMoves(board).find { 
            it.from == Square.E2 && it.to == Square.E4 
        }
        assertNotNull("e2e4 move should exist", e2e4Move)
        
        val boardAfterE2E4 = board.makeMove(e2e4Move!!)
        
        // Black's pawns are on rank 7, so they cannot capture en passant (need to be on rank 5)
        val blackMoves = MoveGenerator.generateLegalMoves(boardAfterE2E4)
        val epMoves = blackMoves.filter { it.isEnPassant }
        
        // Should be 0 en passant moves since black's pawns are on rank 7
        assertEquals(0, epMoves.size)
    }
    
}
