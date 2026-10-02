package com.pro.chessin.domain.coach

import com.pro.chessin.data.local.entity.GameEntity
import com.pro.chessin.data.local.entity.MoveEntity
import com.pro.chessin.domain.chess.ChessBoardState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AICoachContextBuilder.
 * Verifies token budget truncation, move 1 safety, and PII-free JSON output.
 */
class AICoachContextBuilderTest {

    private val dummyGame = GameEntity(
        id = 1L,
        whitePlayer = "Player 1",
        blackPlayer = "Player 2",
        date = 1727000000L,
        result = "1-0",
        event = "Casual Game",
        pgnRaw = "1. e4 e5",
        createdAt = 1727000000L
    )

    @Test
    fun testBuild_Move40DeepGame_TruncatesToStayUnderBudget() {
        // Create 40 prior moves
        val priorPly = mutableListOf<MoveEntity>()
        var board = ChessBoardState.startPosition()

        val sampleSans = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3", "Nf6", "d4", "exd4")
        for (i in 0 until 40) {
            val san = sampleSans[i % sampleSans.size]
            val fen = board.toFEN()
            priorPly.add(
                MoveEntity(
                    id = (i + 1).toLong(),
                    gameId = 1L,
                    ply = i,
                    san = san,
                    fenAfter = fen,
                    evalCp = 20,
                    classificationTier = "GOOD"
                )
            )
        }

        // Target move at ply 40
        val altLinesJson = """[
            {"sanSequence":["d4","d5","c4","e6"],"evalCp":35},
            {"sanSequence":["Nf3","Nf6","g3","d5"],"evalCp":20},
            {"sanSequence":["e4","c5","Nf3","d6"],"evalCp":10}
        ]""".trimIndent()

        val targetMove = MoveEntity(
            id = 41L,
            gameId = 1L,
            ply = 40,
            san = "d4",
            fenAfter = board.toFEN(),
            evalCp = 15,
            classificationTier = "BEST",
            alternativeLinesJson = altLinesJson
        )

        val context = AICoachContextBuilder.build(
            move = targetMove,
            game = dummyGame,
            priorPly = priorPly,
            deltaCp = 10,
            maxTokenBudget = 800
        )

        val json = context.toJson()
        val tokenCount = CoachTokenCounter.countTokens(json)

        System.err.println("=== Move 40 JTokkit CL100K Token Count ===")
        System.err.println("Exact BPE Token count: $tokenCount / 800")
        System.err.println("Prior history size: ${context.priorMoveHistory.size}")
        System.err.println("Alternative lines count: ${context.alternativeLines.size}")
        System.err.println("JSON Payload:\n$json")

        assertTrue("Token count ($tokenCount) should be <= 800 budget", tokenCount <= 800)
        assertTrue("Prior history should not exceed 4 moves", context.priorMoveHistory.size <= 4)
    }

    @Test
    fun testBuild_StrictLowTokenBudget_TruncatesHistoryAndAltLines() {
        val priorPly = (1..10).map { i ->
            MoveEntity(
                id = i.toLong(),
                gameId = 1L,
                ply = i - 1,
                san = "move$i",
                fenAfter = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - $i",
                classificationTier = "GOOD"
            )
        }

        val altLinesJson = """[
            {"sanSequence":["Nf3","Nc6","Bc4","c3","d4"],"evalCp":30},
            {"sanSequence":["c3","Nf6","d4","exd4","cxd4"],"evalCp":20},
            {"sanSequence":["d4","d5","c4","e6","Nc3"],"evalCp":10}
        ]""".trimIndent()

        val move = MoveEntity(
            id = 11L,
            gameId = 1L,
            ply = 10,
            san = "Nf3",
            fenAfter = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
            evalCp = 25,
            classificationTier = "EXCELLENT",
            alternativeLinesJson = altLinesJson
        )

        // Enforce a strict 150 token budget to force truncation of history and alternative lines
        val context = AICoachContextBuilder.build(
            move = move,
            game = dummyGame,
            priorPly = priorPly,
            deltaCp = 0,
            maxTokenBudget = 150
        )

        val tokenCount = CoachTokenCounter.countTokens(context.toJson())
        println("=== Strict 150 Token Budget Test ===")
        println("Token count after truncation: $tokenCount")

        assertTrue("Token count ($tokenCount) should be <= 150 budget", tokenCount <= 150)
        assertTrue("Prior history should be truncated to fit low budget", context.priorMoveHistory.size < priorPly.size)
    }

    @Test
    fun testBuild_Move1_FirstMoveNoHistory_Success() {
        val targetMove = MoveEntity(
            id = 1L,
            gameId = 1L,
            ply = 0,
            san = "e4",
            fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            evalCp = 20,
            classificationTier = "BOOK"
        )

        val context = AICoachContextBuilder.build(
            move = targetMove,
            game = dummyGame,
            priorPly = emptyList(),
            deltaCp = 0,
            maxTokenBudget = 800
        )

        val json = context.toJson()
        println("=== Move 1 Test ===")
        println("Move 1 JSON:\n$json")

        assertTrue("Prior move history should be empty for move 1", context.priorMoveHistory.isEmpty())
        assertEquals("e4", context.sanMove)
        assertEquals("BOOK", context.classificationTier)
        assertTrue("JSON should be valid", isValidJson(json))
    }

    @Test
    fun testBuild_PiiAudit_NoUserOrDeviceIdentifiersInSerializedOutput() {
        val move = MoveEntity(
            id = 10L,
            gameId = 1L,
            ply = 5,
            san = "Bc4",
            fenAfter = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
            evalCp = 15,
            classificationTier = "GOOD"
        )

        val context = AICoachContextBuilder.build(
            move = move,
            game = dummyGame,
            priorPly = emptyList(),
            deltaCp = 0
        )

        val json = context.toJson()

        // PII Audit Checklist: Verify allowed keys only
        val allowedKeys = setOf(
            "fenBefore",
            "fenAfter",
            "sanMove",
            "classificationTier",
            "evalDeltaCp",
            "alternativeLines",
            "priorMoveHistory"
        )

        // Extract key names from JSON string directly (JVM test friendly)
        val extractedKeys = Regex(""""(\w+)":""").findAll(json).map { it.groupValues[1] }.toSet()
        assertEquals("JSON should contain only the 7 standard context keys", allowedKeys, extractedKeys)

        // Confirm forbidden PII string matches do not appear anywhere in serialized output
        val lowerJson = json.lowercase()
        assertFalse("JSON must not contain user names", lowerJson.contains("player 1"))
        assertFalse("JSON must not contain user IDs", lowerJson.contains("userid"))
        assertFalse("JSON must not contain device IDs", lowerJson.contains("deviceid"))
        assertFalse("JSON must not contain email", lowerJson.contains("email"))
    }

    @Test
    fun testJtokkitExactTokenCounting_FenAndJson() {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
        val count = CoachTokenCounter.countTokens(fen)
        println("JTokkit exact cl100k_base count for FEN: $count")

        // Exact BPE token count from JTokkit for this FEN
        assertEquals(49, count)
    }

    private fun isValidJson(json: String): Boolean {
        return try {
            JSONObject(json)
            true
        } catch (e: Exception) {
            false
        }
    }
}
