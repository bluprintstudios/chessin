package com.pro.chessin.domain.puzzles

import com.pro.chessin.data.model.Game
import com.pro.chessin.data.model.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MistakePuzzleGeneratorTest {

    @Test
    fun `generateFromGame creates puzzle for BLUNDER move with FEN before move`() {
        val game = Game(
            id = 101L,
            whitePlayer = "Player1",
            blackPlayer = "Player2",
            date = 1704067200000L,
            result = "1-0",
            event = "Casual Game",
            pgnRaw = "1. e4 e5 2. Nf3 Nc6",
            createdAt = System.currentTimeMillis()
        )

        val fenAfterE4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val fenAfterE5 = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"

        val moves = listOf(
            Move(
                id = 1L,
                gameId = 101L,
                ply = 0,
                san = "e4",
                fenAfter = fenAfterE4,
                evalCp = 20,
                evalMate = null,
                classificationTier = "BEST",
                explanationText = null
            ),
            Move(
                id = 2L,
                gameId = 101L,
                ply = 1,
                san = "f6",
                fenAfter = fenAfterE5,
                evalCp = -250,
                evalMate = null,
                classificationTier = "BLUNDER",
                explanationText = "Best move: e5"
            )
        )

        val puzzles = MistakePuzzleGenerator.generateFromGame(game, moves, userRating = 1500)

        assertEquals("Should generate 1 mistake puzzle for the blunder", 1, puzzles.size)

        val puzzle = puzzles[0]
        assertEquals("mistake_101_1", puzzle.id)
        assertEquals("USER_MISTAKE", puzzle.source)
        assertEquals("FEN should be position before move 1 (i.e. fenAfterE4)", fenAfterE4, puzzle.fen)
        assertEquals("Solution should match extracted best move", "e5", puzzle.solutionMoves)
        assertEquals(1500, puzzle.rating)
        assertTrue(puzzle.themes.contains("blunder"))
    }

    @Test
    fun `generateFromGame generates no puzzles when all moves are BEST or GOOD`() {
        val game = Game(
            id = 102L,
            whitePlayer = "Player1",
            blackPlayer = "Player2",
            date = 1704067200000L,
            result = "1-0",
            event = "Casual Game",
            pgnRaw = "1. e4 e5",
            createdAt = System.currentTimeMillis()
        )

        val moves = listOf(
            Move(
                id = 1L,
                gameId = 102L,
                ply = 0,
                san = "e4",
                fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                evalCp = 20,
                evalMate = null,
                classificationTier = "BEST",
                explanationText = null
            ),
            Move(
                id = 2L,
                gameId = 102L,
                ply = 1,
                san = "e5",
                fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                evalCp = 15,
                evalMate = null,
                classificationTier = "GOOD",
                explanationText = null
            )
        )

        val puzzles = MistakePuzzleGenerator.generateFromGame(game, moves)
        assertTrue("No puzzles should be generated for non-mistakes", puzzles.isEmpty())
    }
}
