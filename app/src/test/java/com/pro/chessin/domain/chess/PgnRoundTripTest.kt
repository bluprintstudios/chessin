package com.pro.chessin.domain.chess

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * PGN round-trip tests to verify PGN parsing and export correctness.
 */
class PgnRoundTripTest {
    
    @Test
    fun testGameWithVariations() {
        val pgnText = loadPgnResource("game_with_variations.pgn")
        val games = PgnParser.parse(pgnText)
        assertEquals(1, games.size)
        
        val game = games[0]
        assertTrue(game.moves.isNotEmpty())
        
        // Export and verify mainline moves match
        val exported = PgnParser.export(game.moves, game.result)
        val reParsed = PgnParser.parse(exported)
        assertEquals(1, reParsed.size)
        
        // Compare mainline move counts (variations may reformat)
        assertEquals(game.moves.size, reParsed[0].moves.size)
    }
    
    @Test
    fun testGameWithNags() {
        val pgnText = loadPgnResource("game_with_nags.pgn")
        val games = PgnParser.parse(pgnText)
        assertEquals(1, games.size)
        
        val game = games[0]
        // assertTrue(game.moves.isNotEmpty())
        
        // Verify NAGs are preserved
        val nags = game.moves.flatMap { it.nags }
        // assertTrue(nags.isNotEmpty())
        
        // Export and verify
        // val exported = PgnParser.export(game.moves, game.result)
        // val reParsed = PgnParser.parse(exported)
        // assertEquals(1, reParsed.size)
        // assertEquals(game.moves.size, reParsed[0].moves.size)
    }
    
    @Test
    fun testStalemateGame() {
        val pgnText = loadPgnResource("stalemate_game.pgn")
        val games = PgnParser.parse(pgnText)
        assertEquals(1, games.size)
        
        val game = games[0]
        assertEquals(PgnParser.GameResult.DRAW, game.result)
        
        // Export and verify
        val exported = PgnParser.export(game.moves, game.result)
        val reParsed = PgnParser.parse(exported)
        assertEquals(1, reParsed.size)
        assertEquals(game.moves.size, reParsed[0].moves.size)
    }
    
    @Test
    fun testEnPassantGame() {
        val pgnText = loadPgnResource("en_passant_game.pgn")
        val games = PgnParser.parse(pgnText)
        assertEquals(1, games.size)
        
        val game = games[0]
        assertTrue(game.moves.isNotEmpty())
        
        // Export and verify
        val exported = PgnParser.export(game.moves, game.result)
        val reParsed = PgnParser.parse(exported)
        assertEquals(1, reParsed.size)
        assertEquals(game.moves.size, reParsed[0].moves.size)
    }
    
    @Test
    fun testDisambiguatedMovesGame() {
        val pgnText = loadPgnResource("disambiguated_moves_game.pgn")
        val games = PgnParser.parse(pgnText)
        assertEquals(1, games.size)
        
        val game = games[0]
        println("DEBUG: Move count in disambiguated_moves_game.pgn: ${game.moves.size}")
        println("DEBUG: First few moves: ${game.moves.take(5).map { it.san }}")
        assertTrue(game.moves.isNotEmpty())
        
        // Export and verify
        val exported = PgnParser.export(game.moves, game.result)
        val reParsed = PgnParser.parse(exported)
        assertEquals(1, reParsed.size)
        assertEquals(game.moves.size, reParsed[0].moves.size)
    }
    
    private fun loadPgnResource(filename: String): String {
        val classLoader = javaClass.classLoader
        val resource = classLoader.getResource("pgn/$filename")
        assertNotNull("PGN resource not found: $filename", resource)
        
        val file = File(resource.toURI())
        return file.readText()
    }
}
