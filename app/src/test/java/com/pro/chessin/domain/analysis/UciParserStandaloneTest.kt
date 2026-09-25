package com.pro.chessin.domain.analysis

import org.junit.Test
import org.junit.Assert.*

/**
 * Standalone test for UciParser to verify parsing logic without engine integration.
 * This test can run independently of the full MoveAnalyzer integration.
 */
class UciParserStandaloneTest {
    
    @Test
    fun testParseInfoLine_MultiPV1() {
        val line = "info depth 14 multipv 1 score cp 30 pv e2e4 e7e5"
        val result = UciParser.parseInfoLine(line)
        
        assertNotNull("Should parse info line", result)
        assertEquals("Depth should be 14", 14, result?.depth)
        assertEquals("MultiPV should be 1", 1, result?.multiPv)
        assertEquals("Score should be 30cp", EvalScore.fromCp(30), result?.score)
    }
    
    @Test
    fun testParseInfoLine_MultiPV2() {
        val line = "info depth 14 multipv 2 score cp 20 pv d2d4 d7d5"
        val result = UciParser.parseInfoLine(line)
        
        assertNotNull("Should parse info line", result)
        assertEquals("Depth should be 14", 14, result?.depth)
        assertEquals("MultiPV should be 2", 2, result?.multiPv)
        assertEquals("Score should be 20cp", EvalScore.fromCp(20), result?.score)
    }
    
    @Test
    fun testParseInfoLine_MateScore() {
        val line = "info depth 14 multipv 1 score mate 3 pv e2e4 e7e5"
        val result = UciParser.parseInfoLine(line)
        
        assertNotNull("Should parse info line", result)
        assertEquals("Depth should be 14", 14, result?.depth)
        assertEquals("MultiPV should be 1", 1, result?.multiPv)
        assertEquals("Score should be mate in 3", EvalScore.fromMate(3), result?.score)
    }
    
    @Test
    fun testParseInfoLine_NegativeCp() {
        val line = "info depth 14 multipv 1 score cp -50 pv e2e4 e7e5"
        val result = UciParser.parseInfoLine(line)
        
        assertNotNull("Should parse info line", result)
        assertEquals("Score should be -50cp", EvalScore.fromCp(-50), result?.score)
    }
    
    @Test
    fun testParseInfoLine_NonInfoLine() {
        val line = "bestmove e2e4 ponder e7e5"
        val result = UciParser.parseInfoLine(line)
        
        assertNull("Should return null for non-info line", result)
    }
    
    @Test
    fun testParseBestMove() {
        val line = "bestmove e2e4 ponder e7e5"
        val result = UciParser.parseBestMove(line)
        
        assertNotNull("Should parse bestmove line", result)
        assertEquals("Move should be e2e4", "e2e4", result?.move)
        assertEquals("Ponder should be e7e5", "e7e5", result?.ponder)
    }
    
    @Test
    fun testParseBestMove_NoPonder() {
        val line = "bestmove e2e4"
        val result = UciParser.parseBestMove(line)
        
        assertNotNull("Should parse bestmove line", result)
        assertEquals("Move should be e2e4", "e2e4", result?.move)
        assertNull("Ponder should be null", result?.ponder)
    }
    
    @Test
    fun testExtractTopScores() {
        val lines = sequenceOf(
            "info depth 14 multipv 1 score cp 30 pv e2e4",
            "info depth 14 multipv 2 score cp 20 pv d2d4",
            "info depth 14 multipv 3 score cp 10 pv g1f3",
            "bestmove e2e4"
        )
        
        val scores = UciParser.extractTopScores(lines, 3)
        
        assertEquals("Should extract 3 scores", 3, scores.size)
        assertEquals("MultiPV 1 should be 30cp", EvalScore.fromCp(30), scores[1])
        assertEquals("MultiPV 2 should be 20cp", EvalScore.fromCp(20), scores[2])
        assertEquals("MultiPV 3 should be 10cp", EvalScore.fromCp(10), scores[3])
    }
    
    @Test
    fun testFindBestMove() {
        val lines = sequenceOf(
            "info depth 14 multipv 1 score cp 30 pv e2e4",
            "bestmove e2e4"
        )
        
        val bestMove = UciParser.findBestMove(lines)
        
        assertNotNull("Should find bestmove", bestMove)
        assertEquals("Move should be e2e4", "e2e4", bestMove?.move)
    }
}
