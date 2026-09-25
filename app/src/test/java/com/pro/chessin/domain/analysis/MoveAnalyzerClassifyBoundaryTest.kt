package com.pro.chessin.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Additional JVM unit tests for MoveAnalyzer.classify() focusing on
 * boundary conditions and edge cases that complement the existing
 * MoveAnalyzerTest.kt.
 *
 * Uses a mock EngineRepository that never actually runs the engine —
 * we're testing the classification *logic*, not engine integration.
 */
class MoveAnalyzerClassifyBoundaryTest {

    private class DummyEngineRepository : com.pro.chessin.domain.engine.EngineRepository {
        override val engineOutput: kotlinx.coroutines.flow.Flow<String> =
            kotlinx.coroutines.flow.emptyFlow()

        override suspend fun startEngine(nnuePath: String): Result<Unit> = Result.success(Unit)
        override suspend fun sendCommand(command: String): Result<Unit> = Result.success(Unit)
        override suspend fun stopEngine(): Result<Unit> = Result.success(Unit)
        override fun clearOutputBuffer() {}
    }

    private val analyzer = MoveAnalyzer(DummyEngineRepository(), analysisDepth = 1)

    // ─── BEST tier: delta = 0 ────────────────────────────────────────────────

    @Test
    fun best_move_exactMatchAfterAndBest() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(50),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            bestLineContainsMate = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.BEST, result)
    }

    // ─── EXCELLENT tier: 0 < delta <= 10 ──────────────────────────────────────

    @Test
    fun excellent_deltaIsOne() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(49),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.EXCELLENT, result)
    }

    @Test
    fun excellent_deltaAtBoundary10() {
        // delta = 10 (bestEval - afterEval = 50 - 40 = 10)
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(40),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.EXCELLENT, result)
    }

    // ─── GOOD tier: 10 < delta <= 50 ───────────────────────────────────────────

    @Test
    fun good_deltaAt11() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(39),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.GOOD, result)
    }

    @Test
    fun good_deltaAtBoundary50() {
        // delta = 50 (bestEval - afterEval = 50 - 0 = 50)
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(0),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.GOOD, result)
    }

    // ─── INACCURACY tier: 50 < delta <= 100 ────────────────────────────────────

    @Test
    fun inaccuracy_deltaAt51() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(-1),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.INACCURACY, result)
    }

    @Test
    fun inaccuracy_deltaAtBoundary100() {
        // delta = 100 (bestEval - afterEval = 50 - (-50) = 100)
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(-50),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.INACCURACY, result)
    }

    // ─── MISTAKE tier: 100 < delta <= 300 ────────────────────────────────────

    @Test
    fun mistake_deltaAt101() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(-51),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.MISTAKE, result)
    }

    @Test
    fun mistake_deltaAtBoundary300() {
        // delta = 300 (bestEval - afterEval = 50 - (-250) = 300)
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(-250),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.MISTAKE, result)
    }

    // ─── BLUNDER tier: delta > 300 ────────────────────────────────────────────

    @Test
    fun blunder_deltaAt301() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(-251),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.BLUNDER, result)
    }

    @Test
    fun blunder_largeDelta() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(50),
            afterEval = EvalScore.fromCp(-500),
            bestEval = EvalScore.fromCp(50),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.BLUNDER, result)
    }

    // ─── BOOK tier ───────────────────────────────────────────────────────────

    @Test
    fun book_takesPriorityOverDelta() {
        // Even with a large delta, book moves are BOOK
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(-500),
            bestEval = EvalScore.fromCp(500),
            isBookMove = true,
            bestLineContainsMate = true,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.BOOK, result)
    }

    // ─── MISS tier ───────────────────────────────────────────────────────────

    @Test
    fun miss_mateAvailableNotTaken() {
        // Best eval is mate-in-3, after eval is only +100 (didn't take the mate)
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(100),
            bestEval = EvalScore.fromMate(3),
            isBookMove = false,
            bestLineContainsMate = true,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.MISS, result)
    }

    @Test
    fun miss_tacticalWinAvailableNotTaken() {
        // Best eval is 600cp tactical win, after eval is only +50
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(50),
            bestEval = EvalScore.fromCp(600),
            isBookMove = false,
            bestLineContainsMate = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.MISS, result)
    }

    @Test
    fun miss_tacticalWinExactlyAtThreshold_notMissed() {
        // Best eval is exactly 500cp, after is 50 — delta = 450, but threshold is > 500
        // The condition is bestEval > MISS_TACTICAL_THRESHOLD (500), so exactly 500 is NOT a miss
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(50),
            bestEval = EvalScore.fromCp(500),
            isBookMove = false,
            bestLineContainsMate = false,
            moverIsWhite = true
        )
        // delta = 500 - 50 = 450, which falls in BLUNDER range (> 300)
        assertEquals(ClassificationTier.BLUNDER, result)
    }

    @Test
    fun miss_justOverThreshold_isMiss() {
        // Best eval is 501cp (> 500 threshold), after is 0
        // Outer: 501 > 500 -> true
        // Inner: 0 < (501 - 500) = 1 -> true -> MISS
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(0),
            bestEval = EvalScore.fromCp(501),
            isBookMove = false,
            bestLineContainsMate = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.MISS, result)
    }

    // ─── Black perspective (moverIsWhite = false) ───────────────────────────

    @Test
    fun blackPerspective_mistake() {
        // When moverIsWhite=false, toNormalizedCp negates cp.
        // delta = bestEval.normalized - afterEval.normalized
        //       = (-bestEval.cp) - (-afterEval.cp)
        //       = afterEval.cp - bestEval.cp
        // For delta = 300 (MISTAKE boundary):
        // bestEval = -250 (black's best move: black better by 250)
        // afterEval = 50  (after bad move: white better by 50)
        // delta = 50 - (-250) = 300 -> MISTAKE
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(50),
            bestEval = EvalScore.fromCp(-250),
            isBookMove = false,
            moverIsWhite = false
        )
        assertEquals(ClassificationTier.MISTAKE, result)
    }

    @Test
    fun blackPerspective_blunder() {
        // delta = afterEval.cp - bestEval.cp = 100 - (-250) = 350 -> BLUNDER
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(100),
            bestEval = EvalScore.fromCp(-250),
            isBookMove = false,
            moverIsWhite = false
        )
        assertEquals(ClassificationTier.BLUNDER, result)
    }

    // ─── Edge cases ──────────────────────────────────────────────────────────

    @Test
    fun edgeCase_negativeBestEval() {
        // Position is losing, best move is least bad
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(-200),
            afterEval = EvalScore.fromCp(-200),
            bestEval = EvalScore.fromCp(-200),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.BEST, result)
    }

    @Test
    fun edgeCase_bothZero_cp() {
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(0),
            bestEval = EvalScore.fromCp(0),
            isBookMove = false,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.BEST, result)
    }

    @Test
    fun edgeCase_mateInOne_notMissed() {
        // Best line is mate-in-1, played move achieves mate-in-1 -> BEST
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromMate(1),
            bestEval = EvalScore.fromMate(1),
            isBookMove = false,
            bestLineContainsMate = true,
            moverIsWhite = true
        )
        assertEquals(ClassificationTier.BEST, result)
    }

    @Test
    fun edgeCase_mateInOne_missed() {
        // Best line is mate-in-1, played move doesn't achieve it
        val result = analyzer.classify(
            beforeEval = EvalScore.fromCp(0),
            afterEval = EvalScore.fromCp(0),
            bestEval = EvalScore.fromMate(1),
            isBookMove = false,
            bestLineContainsMate = true,
            moverIsWhite = true
        )
        // bestEval (9100 normalized) > MISS_TACTICAL_THRESHOLD (500), and
        // afterEval (0) < bestEval - 500 (8600) -> MISS
        assertEquals(ClassificationTier.MISS, result)
    }
}
