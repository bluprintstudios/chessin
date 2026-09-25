package com.pro.chessin.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AnalyzedMove] serialization/deserialization.
 *
 * Verifies that the compact tab-delimited format round-trips
 * correctly for typical chess game analysis results.
 */
class AnalyzedMoveTest {

    @Test
    fun testSerializeDeserializeRoundTrip() {
        val moves = listOf(
            AnalyzedMove(
                san = "e4",
                fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                tier = ClassificationTier.BOOK,
                evalAfterCp = 0,
                deltaCp = 0,
                bestEvalCp = 0,
                moveIndex = 0,
                isBookMove = true
            ),
            AnalyzedMove(
                san = "e5",
                fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                tier = ClassificationTier.BOOK,
                evalAfterCp = 0,
                deltaCp = 0,
                bestEvalCp = 0,
                moveIndex = 1,
                isBookMove = true
            ),
            AnalyzedMove(
                san = "Nf3",
                fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
                tier = ClassificationTier.EXCELLENT,
                evalAfterCp = 23,
                deltaCp = 5,
                bestEvalCp = 28,
                moveIndex = 2,
                isBookMove = false
            ),
            AnalyzedMove(
                san = "Nc6",
                fenAfter = "r1bqkbnr/pppp1ppp/2p2n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3",
                tier = ClassificationTier.GOOD,
                evalAfterCp = 18,
                deltaCp = 12,
                bestEvalCp = 30,
                moveIndex = 3,
                isBookMove = false
            ),
            AnalyzedMove(
                san = "Bb5",
                fenAfter = "r1bqkbnr/pppp1ppp/2p2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 0 3",
                tier = ClassificationTier.EXCELLENT,
                evalAfterCp = 45,
                deltaCp = 3,
                bestEvalCp = 48,
                moveIndex = 4,
                isBookMove = false
            ),
            AnalyzedMove(
                san = "a6",
                fenAfter = "r1bqkbnr/1ppp1ppp/p1P2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 0 3",
                tier = ClassificationTier.MISTAKE,
                evalAfterCp = 12,
                deltaCp = 65,
                bestEvalCp = 77,
                moveIndex = 5,
                isBookMove = false
            ),
        )

        val serialized = AnalyzedMove.serialize(moves)
        val deserialized = AnalyzedMove.deserialize(serialized)

        assertEquals(moves.size, deserialized.size)

        moves.zip(deserialized).forEach { (original, roundTripped) ->
            assertEquals(original.san, roundTripped.san)
            assertEquals(original.fenAfter, roundTripped.fenAfter)
            assertEquals(original.tier, roundTripped.tier)
            assertEquals(original.evalAfterCp, roundTripped.evalAfterCp)
            assertEquals(original.deltaCp, roundTripped.deltaCp)
            assertEquals(original.bestEvalCp, roundTripped.bestEvalCp)
            assertEquals(original.moveIndex, roundTripped.moveIndex)
            assertEquals(original.isBookMove, roundTripped.isBookMove)
        }
    }

    @Test
    fun testDeserializeEmptyOrNull() {
        assertTrue(AnalyzedMove.deserialize(null).isEmpty())
        assertTrue(AnalyzedMove.deserialize("").isEmpty())
    }

    @Test
    fun testDeserializeMalformedLineIsSkipped() {
        val data = buildString {
            append("e4\trnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1\tBOOK\t0\t0\t0\t0\ttrue\n")
            append("bad data with not enough fields\n")
            append("Nf3\trnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2\tBEST\t23\t5\t28\t2\tfalse\n")
        }

        val moves = AnalyzedMove.deserialize(data)
        assertEquals(2, moves.size)
        assertEquals("e4", moves[0].san)
        assertEquals("Nf3", moves[1].san)
    }

    @Test
    fun testResignationGameWithBlunder() {
        // Simulates a game where a player resigns after a blunder
        val moves = listOf(
            AnalyzedMove("e4", "fen1", ClassificationTier.BOOK, 0, 0, 0, 0, true),
            AnalyzedMove("e5", "fen2", ClassificationTier.BOOK, 0, 0, 0, 1, true),
            AnalyzedMove("Qh5", "fen3", ClassificationTier.MISS, 500, 0, 500, 2, false),
            AnalyzedMove("Nc6", "fen4", ClassificationTier.BLUNDER, -50, 120, 70, 3, false),
            AnalyzedMove("Qxf7#", "fen5", ClassificationTier.BEST, 0, -50, -50, 4, false),
        )

        val serialized = AnalyzedMove.serialize(moves)
        val deserialized = AnalyzedMove.deserialize(serialized)

        assertEquals(moves.size, deserialized.size)
        assertEquals(ClassificationTier.MISS, deserialized[2].tier)
        assertEquals(ClassificationTier.BLUNDER, deserialized[3].tier)
        assertEquals(120, deserialized[3].deltaCp)
    }
}
