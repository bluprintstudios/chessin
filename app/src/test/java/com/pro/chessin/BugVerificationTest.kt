package com.pro.chessin

import com.pro.chessin.domain.analysis.EvalScore
import com.pro.chessin.domain.analysis.MoveAnalyzer
import com.pro.chessin.domain.analysis.SacrificeDetector
import com.pro.chessin.domain.analysis.ClassificationTier
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Color
import com.pro.chessin.domain.chess.Move
import com.pro.chessin.domain.chess.Piece
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.PgnParser
import com.pro.chessin.domain.chess.Square
import com.pro.chessin.domain.engine.EngineRepository
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test
import org.junit.Assert.*

/**
 * Verification tests for bugs reported in external review.
 */
class BugVerificationTest {
    
    // Mock EngineRepository for testing classification logic without actual engine
    private class MockEngineRepository : EngineRepository {
        override val engineOutput = emptyFlow<String>()
        override suspend fun startEngine(nnuePath: String): Result<Unit> = Result.success(Unit)
        override suspend fun sendCommand(command: String): Result<Unit> = Result.success(Unit)
        override suspend fun stopEngine(): Result<Unit> = Result.success(Unit)
        override fun clearOutputBuffer() {}
    }
    
    private val analyzer = MoveAnalyzer(MockEngineRepository())

    // ============================================
    // BUG 1: MoveAnalyzer perspective bug
    // ============================================
    
    @Test
    fun testMoveAnalyzerPerspective_WhitePlus200_BestMovePlayed() {
        // Scenario: White to move, position is +200 for White
        // Best move is also +200 for White
        // Played move is also +200 for White (best move played)
        // Expected: delta = 0, classification = BEST
        
        val bestEval = EvalScore.fromCp(200)  // Best line gives White +200
        val afterEval = EvalScore.fromCp(200)  // Played move also gives White +200
        val beforeEval = EvalScore.fromCp(200)  // Position before move was +200 for White
        
        // Note: MoveAnalyzer.classify doesn't actually use beforeEval in delta calculation
        // It calculates: bestEval - afterEval (both from mover's perspective)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false,
            bestLineContainsMate = false,
            moverIsWhite = true
        )
        
        println("=== MoveAnalyzer Perspective Test ===")
        println("Scenario: White +200, best move played")
        println("bestEval.toNormalizedCp(perspective=true): ${bestEval.toNormalizedCp(perspective = true)}")
        println("afterEval.toNormalizedCp(perspective=true): ${afterEval.toNormalizedCp(perspective = true)}")
        println("Expected delta: 0")
        println("Actual classification: ${result}")
        
        assertEquals(ClassificationTier.BEST, result)
    }
    
    @Test
    fun testMoveAnalyzerPerspective_BlackMinus200_BestMovePlayed() {
        // Scenario: Black to move, position is -200 for White (Black is +200)
        // Best move keeps Black at +200
        // Played move also keeps Black at +200 (best move played)
        // Expected: delta = 0, classification = BEST
        
        val bestEval = EvalScore.fromCp(-200)  // Best line gives White -200 (Black +200)
        val afterEval = EvalScore.fromCp(-200)  // Played move also gives White -200
        val beforeEval = EvalScore.fromCp(-200)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false,
            bestLineContainsMate = false,
            moverIsWhite = false
        )
        
        println("\n=== MoveAnalyzer Perspective Test (Black) ===")
        println("Scenario: Black +200 (White -200), best move played")
        println("bestEval.toNormalizedCp(perspective=false): ${bestEval.toNormalizedCp(perspective = false)}")
        println("afterEval.toNormalizedCp(perspective=false): ${afterEval.toNormalizedCp(perspective = false)}")
        println("Expected delta: 0")
        println("Actual classification: ${result}")
        
        assertEquals(ClassificationTier.BEST, result)
    }
    
    @Test
    fun testMoveAnalyzerPerspective_WhitePlus200_BlunderPlayed() {
        // Scenario: White to move, position is +200 for White
        // Best move is +200 for White
        // Played move is -110 for White (blunder)
        // Expected: delta = 310, classification = BLUNDER
        
        val bestEval = EvalScore.fromCp(200)
        val afterEval = EvalScore.fromCp(-110)
        val beforeEval = EvalScore.fromCp(200)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false,
            bestLineContainsMate = false,
            moverIsWhite = true
        )
        
        println("\n=== MoveAnalyzer Perspective Test (Blunder) ===")
        println("Scenario: White +200, blunder to -110")
        println("bestEval.toNormalizedCp(perspective=true): ${bestEval.toNormalizedCp(perspective = true)}")
        println("afterEval.toNormalizedCp(perspective=true): ${afterEval.toNormalizedCp(perspective = true)}")
        println("Expected delta: 310")
        println("Actual classification: ${result}")
        
        assertEquals("310cp delta should classify as BLUNDER", ClassificationTier.BLUNDER, result)
    }

    // ============================================
    // BUG 2: SAN parsing bug for pawn moves
    // ============================================
    
    @Test
    fun testSanParsing_PawnMoves() {
        val pgn = "[Event \"Test\"]\n\n1. e4 e5 2. Nf3"
        
        println("\n=== SAN Parsing Test ===")
        println("Parsing PGN: '$pgn'")
        
        try {
            val games = PgnParser.parse(pgn)
            println("Parsed successfully!")
            println("Number of games: ${games.size}")
            if (games.isNotEmpty()) {
                println("Moves in game 1: ${games[0].moves.map { it.san }}")
                println("Move count: ${games[0].moves.size}")
                if (games[0].moves.isEmpty()) {
                    println("ERROR: No moves parsed!")
                }
            }
        } catch (e: Exception) {
            println("FAILED with exception: ${e.message}")
            e.printStackTrace()
        }
    }
    
    @Test
    fun testSanParsing_PlainPawnMoves() {
        // Test without move numbers
        val pgn = "[Event \"Test\"]\n\ne4 e5 Nf3"
        
        println("\n=== SAN Parsing Test (No move numbers) ===")
        println("Parsing PGN: '$pgn'")
        
        try {
            val games = PgnParser.parse(pgn)
            println("Parsed successfully!")
            println("Number of games: ${games.size}")
            if (games.isNotEmpty()) {
                println("Moves in game 1: ${games[0].moves.map { it.san }}")
                println("Move count: ${games[0].moves.size}")
                if (games[0].moves.isEmpty()) {
                    println("ERROR: No moves parsed!")
                }
            }
        } catch (e: Exception) {
            println("FAILED with exception: ${e.message}")
            e.printStackTrace()
        }
    }
    
    @Test
    fun testSanParsing_SinglePawnMove() {
        // Test just one pawn move
        val pgn = "[Event \"Test\"]\n\ne4"
        
        println("\n=== SAN Parsing Test (Single pawn move) ===")
        println("Parsing PGN: '$pgn'")
        
        // Manually test the tokenizer
        val moveText = "e4"
        println("Testing tokenizer on: '$moveText'")
        
        try {
            // Use reflection to access private tokenize method for debugging
            val tokenizeMethod = PgnParser.javaClass.getDeclaredMethod("tokenize", String::class.java)
            tokenizeMethod.isAccessible = true
            val tokens = tokenizeMethod.invoke(PgnParser, moveText) as List<String>
            println("Tokens: $tokens")
        } catch (e: Exception) {
            println("Could not access tokenize method: ${e.message}")
        }
        
        // Test parseSan directly
        val board = ChessBoardState.startPosition()
        try {
            val parseSanMethod = PgnParser.javaClass.getDeclaredMethod("parseSan", String::class.java, ChessBoardState::class.java)
            parseSanMethod.isAccessible = true
            val move = parseSanMethod.invoke(PgnParser, "e4", board) as com.pro.chessin.domain.chess.Move?
            println("parseSan('e4') result: $move")
        } catch (e: Exception) {
            println("Could not access parseSan method: ${e.message}")
            e.printStackTrace()
        }
        
        try {
            val games = PgnParser.parse(pgn)
            println("Parsed successfully!")
            println("Number of games: ${games.size}")
            if (games.isNotEmpty()) {
                println("Moves in game 1: ${games[0].moves.map { it.san }}")
                println("Move count: ${games[0].moves.size}")
                if (games[0].moves.isEmpty()) {
                    println("ERROR: No moves parsed!")
                }
            }
        } catch (e: Exception) {
            println("FAILED with exception: ${e.message}")
            e.printStackTrace()
        }
    }

    // ============================================
    // BUG 3: SacrificeDetector false positive
    // ============================================
    
    @Test
    fun testSacrificeDetector_RookCapturesBishop() {
        // Scenario: Rook captures undefended Bishop (ordinary capture)
        // This should NOT be flagged as a brilliant sacrifice
        // Material: Rook (500) captures Bishop (330) = material GAINED, not lost
        
        val board = ChessBoardState.startPosition()
        
        // Create a position where a rook can capture a bishop
        // Let's manually construct a simple position
        val customFen = "4k3/8/8/8/8/8/5B2/4R2K w - - 0 1"
        val customBoard = ChessBoardState.fromFEN(customFen)
        
        // Rook on f1 captures Bishop on f2
        val rook = Piece(PieceType.ROOK, Color.WHITE)
        val move = Move.capture(
            from = Square.F1,
            to = Square.F2,
            piece = rook
        )
        
        val beforeEval = EvalScore.fromCp(100)  // Slight advantage before
        val afterEval = EvalScore.fromCp(150)   // Slightly better after capture
        
        println("\n=== SacrificeDetector Test ===")
        println("Scenario: Rook (500) captures undefended Bishop (330)")
        println("Material calculation: (500 + 0) - 330 = 170 (material GAINED)")
        println("Before eval: ${beforeEval.cp}, After eval: ${afterEval.cp}")
        
        val isBrilliant = SacrificeDetector.isBrilliant(
            beforeState = customBoard,
            move = move,
            afterEval = afterEval,
            beforeEval = beforeEval
        )
        
        println("isBrilliant result: $isBrilliant")
        println("Expected: false (material was gained, not lost)")
        
        assertFalse(isBrilliant)
    }

    @Test
    fun testSacrificeDetector_RookCapturesDefendedBishop() {
        // Scenario: Rook captures Bishop defended by Pawn (real exchange sacrifice)
        // Position: Black Pawn on g3 defending Bishop on f2
        val customFen = "4k3/8/8/8/8/6p1/5B2/4R2K w - - 0 1"
        val customBoard = ChessBoardState.fromFEN(customFen)
        
        // Rook on e1 captures Bishop on f2 (f2 square is attacked by g3 pawn)
        val rook = Piece(PieceType.ROOK, Color.WHITE)
        val move = Move.capture(
            from = Square.E1,
            to = Square.F2,
            piece = rook
        )
        
        val beforeEval = EvalScore.fromCp(100)
        val afterEval = EvalScore.fromCp(300)   // Strong attack compensation
        
        val isBrilliant = SacrificeDetector.isBrilliant(
            beforeState = customBoard,
            move = move,
            afterEval = afterEval,
            beforeEval = beforeEval
        )
        
        assertTrue("Rook capturing defended bishop for winning attack should be brilliant", isBrilliant)
    }
    
    @Test
    fun testSacrificeDetector_RealSacrifice() {
        // Scenario: Queen sacrifices itself for a mating attack
        // This SHOULD be flagged as brilliant
        
        val board = ChessBoardState.startPosition()
        
        // Queen captures a pawn but leads to mate
        val queen = Piece(PieceType.QUEEN, Color.WHITE)
        val move = Move.capture(
            from = Square.D1,
            to = Square.D7,
            piece = queen
        )
        
        val beforeEval = EvalScore.fromCp(100)
        val afterEval = EvalScore.fromCp(500)  // Much better after sacrifice
        
        println("\n=== SacrificeDetector Test (Real Sacrifice) ===")
        println("Scenario: Queen (900) captures pawn (100) but position improves")
        println("Material calculation: (900 + 0) - 100 = 800 (material LOST)")
        println("Before eval: ${beforeEval.cp}, After eval: ${afterEval.cp}")
        
        val isBrilliant = SacrificeDetector.isBrilliant(
            beforeState = board,
            move = move,
            afterEval = afterEval,
            beforeEval = beforeEval
        )
        
        println("isBrilliant result: $isBrilliant")
        println("Expected: true (material lost but position improved)")
    }
}
