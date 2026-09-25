package com.pro.chessin.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AnalyzedMove] serialization, covering edge cases like
 * malformed data, special characters in SAN strings, negative deltas,
 * and all classification tiers round-tripping correctly.
 */
class AnalyzedMoveSerializationTest {

    @Test
    fun roundTrip_emptyList_producesEmptyString() {
        val serialized = AnalyzedMove.serialize(emptyList())
        assertEquals("", serialized)
    }

    @Test
    fun deserialize_nullInput_returnsEmptyList() {
        assertEquals(emptyList<AnalyzedMove>(), AnalyzedMove.deserialize(null))
    }

    @Test
    fun deserialize_emptyString_returnsEmptyList() {
        assertEquals(emptyList<AnalyzedMove>(), AnalyzedMove.deserialize(""))
    }

    @Test
    fun roundTrip_singleMove() {
        val original = listOf(
            AnalyzedMove(
                san = "e4",
                fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                tier = ClassificationTier.EXCELLENT,
                evalAfterCp = 25,
                deltaCp = 5,
                bestEvalCp = 30,
                moveIndex = 0,
                isBookMove = true
            )
        )
        val deserialized = AnalyzedMove.deserialize(AnalyzedMove.serialize(original))
        assertEquals(original, deserialized)
    }

    @Test
    fun roundTrip_multipleMoves() {
        val original = listOf(
            AnalyzedMove("e4", "fen1", ClassificationTier.BEST, 10, 0, 10, 0, false),
            AnalyzedMove("e5", "fen2", ClassificationTier.GOOD, -10, 3, 13, 1, false),
            AnalyzedMove("Nf3", "fen3", ClassificationTier.BLUNDER, -300, 350, 50, 2, false),
        )
        val deserialized = AnalyzedMove.deserialize(AnalyzedMove.serialize(original))
        assertEquals(original, deserialized)
    }

    @Test
    fun roundTrip_allTiers() {
        ClassificationTier.entries.forEach { tier ->
            val original = listOf(
                AnalyzedMove("O-O", "fen", tier, 0, 0, 0, 0, false)
            )
            val deserialized = AnalyzedMove.deserialize(AnalyzedMove.serialize(original))
            assertEquals("Tier $tier should round-trip", tier, deserialized[0].tier)
        }
    }

    @Test
    fun roundTrip_specialCharactersInSan() {
        // SAN with promotion, check, and mate symbols
        val original = listOf(
            AnalyzedMove("exd8=Q+", "fen1", ClassificationTier.BEST, 50, 0, 50, 0, false),
            AnalyzedMove("O-O-O#", "fen2", ClassificationTier.EXCELLENT, 30, 2, 32, 1, false),
            AnalyzedMove("Qxa7", "fen3", ClassificationTier.MISS, -200, 250, 50, 2, false),
        )
        val deserialized = AnalyzedMove.deserialize(AnalyzedMove.serialize(original))
        assertEquals(original, deserialized)
    }

    @Test
    fun roundTrip_negativeDeltaAndEval() {
        val original = listOf(
            AnalyzedMove(
                san = "Qh4",
                fenAfter = "fen",
                tier = ClassificationTier.BLUNDER,
                evalAfterCp = -500,
                deltaCp = -300,
                bestEvalCp = -200,
                moveIndex = 10,
                isBookMove = false
            )
        )
        val deserialized = AnalyzedMove.deserialize(AnalyzedMove.serialize(original))
        assertEquals(original, deserialized)
    }

    @Test
    fun roundTrip_isBookMoveTrue() {
        val original = listOf(
            AnalyzedMove(
                san = "Nc3",
                fenAfter = "fen",
                tier = ClassificationTier.BOOK,
                evalAfterCp = 0,
                deltaCp = 0,
                bestEvalCp = 0,
                moveIndex = 2,
                isBookMove = true
            )
        )
        val deserialized = AnalyzedMove.deserialize(AnalyzedMove.serialize(original))
        assertTrue(deserialized[0].isBookMove)
        assertEquals(original, deserialized)
    }

    @Test
    fun deserialize_malformedLine_tooFewFields_skipped() {
        val data = "e4\tfen1\tBEST\t10"
        val result = AnalyzedMove.deserialize(data)
        assertTrue("Malformed line should be skipped", result.isEmpty())
    }

    @Test
    fun deserialize_malformedLine_invalidTier_skipped() {
        val data = "e4\tfen1\tNOT_A_TIER\t10\t0\t10\t0\tfalse"
        val result = AnalyzedMove.deserialize(data)
        assertTrue("Invalid tier should be skipped", result.isEmpty())
    }

    @Test
    fun deserialize_malformedLine_nonNumericCp_skipped() {
        val data = "e4\tfen1\tBEST\tabc\t0\t10\t0\tfalse"
        val result = AnalyzedMove.deserialize(data)
        assertTrue("Non-numeric cp should be skipped", result.isEmpty())
    }

    @Test
    fun deserialize_emptyLinesInInput_skipped() {
        val data = "\n\n"
        val result = AnalyzedMove.deserialize(data)
        assertTrue("Empty lines should produce empty list", result.isEmpty())
    }

    @Test
    fun deserialize_mixedValidAndInvalidLines_returnsOnlyValid() {
        val move1 = AnalyzedMove("e4", "fen1", ClassificationTier.BEST, 10, 0, 10, 0, false)
        val move2 = AnalyzedMove("e5", "fen2", ClassificationTier.GOOD, 5, 3, 8, 1, false)
        val data = buildString {
            append(AnalyzedMove.serialize(listOf(move1)))
            append("\n")
            append("bad\tdata\n")  // malformed - skipped
            append("\n")          // empty line - skipped
            append(AnalyzedMove.serialize(listOf(move2)))
        }
        val result = AnalyzedMove.deserialize(data)
        assertEquals(2, result.size)
        assertEquals(move1, result[0])
        assertEquals(move2, result[1])
    }
}
