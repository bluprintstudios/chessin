package com.pro.chess_review.core.chess

import com.pro.chess_review.domain.model.GameSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnImporterTest {

    @Test
    fun rejectsTextThatChesslibRepresentsAsAnEmptyGame() {
        val games = PgnImporter.parsePgn(
            "This file is intentionally not a PGN game.\nIt has no valid tags or moves.",
            GameSource.PGN_FILE,
        )

        assertTrue(games.isEmpty())
    }

    @Test
    fun keepsAGameWithParsedMoves() {
        val games = PgnImporter.parsePgn(
            """
                [Event "Test"]
                [White "White"]
                [Black "Black"]
                [Result "*"]

                1. e4 e5 *
            """.trimIndent(),
            GameSource.PGN_FILE,
        )

        assertEquals(1, games.size)
    }
}
