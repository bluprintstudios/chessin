package com.pro.chess_review.feature.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessBoardTest {

    @Test
    fun pieceAssetPath_mapsAllFenPiecesForEveryAvailableSet() {
        val expectedSuffixes = mapOf(
            'K' to "wK.svg",
            'Q' to "wQ.svg",
            'R' to "wR.svg",
            'B' to "wB.svg",
            'N' to "wN.svg",
            'P' to "wP.svg",
            'k' to "bK.svg",
            'q' to "bQ.svg",
            'r' to "bR.svg",
            'b' to "bB.svg",
            'n' to "bN.svg",
            'p' to "bP.svg",
        )

        ChessPieceSetCatalog.all.forEach { set ->
            expectedSuffixes.forEach { (fenChar, fileName) ->
                assertEquals("${set.id}/$fenChar", "pieces/${set.id}/$fileName", pieceAssetPath(set.id, fenChar))
            }
        }
    }

    @Test
    fun pieceAssetPath_usesDefaultForUnknownSetAndRejectsEmptySquare() {
        assertEquals(
            "pieces/${ChessPieceSetCatalog.DEFAULT_ID}/wN.svg",
            pieceAssetPath("not-a-set", 'N'),
        )
        assertNull(pieceAssetPath(ChessPieceSetCatalog.DEFAULT_ID, ' '))
        assertNull(pieceAssetPath(ChessPieceSetCatalog.DEFAULT_ID, 'x'))
    }

    @Test
    fun catalogContainsTenPieceSets() {
        assertEquals(10, ChessPieceSetCatalog.all.size)
        assertEquals(10, ChessPieceSetCatalog.all.map { it.id }.distinct().size)
        assertTrue(ChessPieceSetCatalog.find(ChessPieceSetCatalog.DEFAULT_ID) != null)
    }

    @Test
    fun squareDescriptions_includeAlgebraicSquaresAndPieceNames() {
        assertEquals("a8", chessSquareName(rank = 0, file = 0))
        assertEquals("h1", chessSquareName(rank = 7, file = 7))
        assertEquals("e4, White knight", squareDescription("e4", 'N'))
        assertEquals("d5, Black pawn", squareDescription("d5", 'p'))
        assertEquals("a1, empty", squareDescription("a1", ' '))
    }
}
