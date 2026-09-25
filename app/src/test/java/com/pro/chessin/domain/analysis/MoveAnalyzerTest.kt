package com.pro.chessin.domain.analysis

import com.pro.chessin.domain.engine.EngineRepository
import kotlinx.coroutines.flow.Flow
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for MoveAnalyzer classification logic.
 * Tests each classification tier with hand-picked FEN+eval fixtures.
 */
class MoveAnalyzerTest {
    
    // Mock EngineRepository for testing classification logic without actual engine
    private class MockEngineRepository : EngineRepository {
        override val engineOutput: Flow<String> = kotlinx.coroutines.flow.emptyFlow()
        
        override suspend fun startEngine(nnuePath: String): Result<Unit> = Result.success(Unit)
        override suspend fun sendCommand(command: String): Result<Unit> = Result.success(Unit)
        override suspend fun stopEngine(): Result<Unit> = Result.success(Unit)
        override fun clearOutputBuffer() {}
    }
    
    private val analyzer = MoveAnalyzer(MockEngineRepository())
    
    @Test
    fun testBookClassification() {
        val beforeEval = EvalScore.fromCp(0)
        val afterEval = EvalScore.fromCp(0)
        val bestEval = EvalScore.fromCp(0)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = true
        )
        
        assertEquals(ClassificationTier.BOOK, result)
    }
    
    @Test
    fun testBestClassification() {
        // Exact match with best line
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(50)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.BEST, result)
    }
    
    @Test
    fun testExcellentClassification() {
        // Small evaluation loss (5cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(45)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.EXCELLENT, result)
    }
    
    @Test
    fun testExcellentClassificationBoundary() {
        // At the boundary (10cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(40)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.EXCELLENT, result)
    }
    
    @Test
    fun testGoodClassification() {
        // Moderate evaluation loss (30cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(20)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.GOOD, result)
    }
    
    @Test
    fun testGoodClassificationBoundary() {
        // At the boundary (50cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(0)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.GOOD, result)
    }
    
    @Test
    fun testInaccuracyClassification() {
        // Significant evaluation loss (75cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(-25)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.INACCURACY, result)
    }
    
    @Test
    fun testInaccuracyClassificationBoundary() {
        // At the boundary (100cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(-50)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.INACCURACY, result)
    }
    
    @Test
    fun testMistakeClassification() {
        // Large evaluation loss (200cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(-150)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.MISTAKE, result)
    }
    
    @Test
    fun testMistakeClassificationBoundary() {
        // At the boundary (300cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(-250)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.MISTAKE, result)
    }
    
    @Test
    fun testBlunderClassification() {
        // Severe evaluation loss (400cp)
        val beforeEval = EvalScore.fromCp(50)
        val afterEval = EvalScore.fromCp(-350)
        val bestEval = EvalScore.fromCp(50)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false
        )
        
        assertEquals(ClassificationTier.BLUNDER, result)
    }
    
    @Test
    fun testMissClassification_Mate() {
        // Best line has mate-in-3, played move doesn't achieve it
        val beforeEval = EvalScore.fromCp(0)
        val afterEval = EvalScore.fromCp(100)
        val bestEval = EvalScore.fromMate(3)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false,
            bestLineContainsMate = true
        )
        
        assertEquals(ClassificationTier.MISS, result)
    }
    
    @Test
    fun testMissClassification_TacticalWin() {
        // Best line has large tactical win (600cp), played move doesn't achieve it
        val beforeEval = EvalScore.fromCp(0)
        val afterEval = EvalScore.fromCp(50)
        val bestEval = EvalScore.fromCp(600)
        
        val result = analyzer.classify(
            beforeEval = beforeEval,
            afterEval = afterEval,
            bestEval = bestEval,
            isBookMove = false,
            bestLineContainsMate = false
        )
        
        assertEquals(ClassificationTier.MISS, result)
    }
    
    @Test
    fun testMateScoreNormalization_WhitePerspective() {
        // Mate-in-5 from white's perspective
        val mateScore = EvalScore.fromMate(5)
        val normalized = mateScore.toNormalizedCp(perspective = true)
        
        // Expected: 10000 - (100 * 5) = 9500
        assertEquals(9500, normalized)
    }
    
    @Test
    fun testMateScoreNormalization_BlackPerspective() {
        // Mate-in-5 from black's perspective (being mated)
        val mateScore = EvalScore.fromMate(5)
        val normalized = mateScore.toNormalizedCp(perspective = false)
        
        // Expected: -(10000 - (100 * 5)) = -9500
        assertEquals(-9500, normalized)
    }
    
    @Test
    fun testCpScoreNormalization_WhitePerspective() {
        // 150cp from white's perspective
        val cpScore = EvalScore.fromCp(150)
        val normalized = cpScore.toNormalizedCp(perspective = true)
        
        assertEquals(150, normalized)
    }
    
    @Test
    fun testCpScoreNormalization_BlackPerspective() {
        // 150cp from black's perspective
        val cpScore = EvalScore.fromCp(150)
        val normalized = cpScore.toNormalizedCp(perspective = false)
        
        assertEquals(-150, normalized)
    }
    
    @Test
    fun testNegativeCpScoreNormalization_WhitePerspective() {
        // -100cp (black advantage) from white's perspective
        val cpScore = EvalScore.fromCp(-100)
        val normalized = cpScore.toNormalizedCp(perspective = true)
        
        assertEquals(-100, normalized)
    }
}
