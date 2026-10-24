package com.pro.chess_review.domain.usecase

import com.pro.chess_review.core.engine.AnalysisCache
import com.pro.chess_review.core.engine.ChessEngine
import com.pro.chess_review.core.engine.EngineLine
import com.pro.chess_review.core.engine.EngineResult
import com.pro.chess_review.core.engine.EngineSettings
import com.pro.chess_review.data.local.AnalysisDao
import com.pro.chess_review.data.local.AnalysisEntity
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.GameParseStatus
import com.pro.chess_review.domain.model.GameSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewGameUseCaseTest {

    private class FakeAnalysisCache : AnalysisCache {
        override suspend fun getCachedResult(
            fen: String,
            engineId: String,
            engineVersion: String,
            settings: EngineSettings
        ): EngineResult? = null

        override suspend fun saveResult(result: EngineResult, settings: EngineSettings) {}
    }

    private class FakeEngine(private val mockResults: Map<String, EngineResult>) : ChessEngine {
        override val engineId = "FakeEngine"
        override val engineVersion = "1.0"
        
        override suspend fun start() {}
        override suspend fun stop() {}

        override fun analyze(fen: String, settings: EngineSettings): Flow<EngineResult> {
            val res = mockResults.entries.firstOrNull { fen.startsWith(it.key) }?.value 
                ?: EngineResult(fen, emptyList(), engineId, engineVersion, isComplete = true)
            return flowOf(res)
        }
    }

    @Test
    fun reviewGame_calculatesSummaryCorrectly() = runBlocking {
        // A tiny PGN: 1. e4
        val pgn = """
            [Event "Test"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "Player1"]
            [Black "Player2"]
            [Result "1-0"]

            1. e4 1-0
        """.trimIndent()
        val game = ChessGame(
            id = "test", source = GameSource.PGN_FILE, sourceGameId = null, username = null,
            importedAtEpochMillis = 0, playedAtEpochMillis = null, whitePlayer = null, blackPlayer = null,
            whiteRating = null, blackRating = null, timeControl = null, result = null,
            pgn = pgn, variant = "Standard", parseStatus = GameParseStatus.READY
        )

        // Initial fen is startpos
        val fen1 = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        // After e4
        val fen2 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3"
        
        val line1 = EngineLine(listOf("e2e4"), listOf("e4"), 50, null, 18)
        val line2 = EngineLine(listOf("e7e5"), listOf("e5"), 50, null, 18)

        val mockResults = mapOf(
            fen1 to EngineResult(fen1, listOf(line1), "Fake", "1", true),
            fen2 to EngineResult(fen2, listOf(line2), "Fake", "1", true)
        )

        val engine = FakeEngine(mockResults)
        val cache = FakeAnalysisCache()
        val analyzeUc = AnalyzePositionUseCase(engine, cache)
        val reviewUc = ReviewGameUseCase(analyzeUc)

        val progressEmissions = reviewUc.invoke(game, EngineSettings()).toList()
        
        val finalReview = progressEmissions.last().review
        assert(finalReview != null)

        val summary = finalReview!!.summary
        assertEquals(1, summary.totalPlies) // e4 is 1 ply
        assertEquals(1, summary.analyzedPlies)
        assertEquals(100, summary.analysisCoveragePercent)
        assertEquals(0, summary.averageCentipawnLoss) // cp went from 50 to 50
    }

    @Test
    fun reviewGame_excludesIncompleteAnalysisFromSummary() = runBlocking {
        val pgn = """
            [Event "Test"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "Player1"]
            [Black "Player2"]
            [Result "1-0"]

            1. e4 1-0
        """.trimIndent()
        val game = ChessGame(
            id = "test", source = GameSource.PGN_FILE, sourceGameId = null, username = null,
            importedAtEpochMillis = 0, playedAtEpochMillis = null, whitePlayer = null, blackPlayer = null,
            whiteRating = null, blackRating = null, timeControl = null, result = null,
            pgn = pgn, variant = "Standard", parseStatus = GameParseStatus.READY
        )

        val fen1 = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        val fen2 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3"
        
        val line1 = EngineLine(listOf("e2e4"), listOf("e4"), 50, null, 18)
        
        // fen2 returns incomplete analysis
        val mockResults = mapOf(
            fen1 to EngineResult(fen1, listOf(line1), "Fake", "1", true),
            fen2 to EngineResult(fen2, emptyList(), "Fake", "1", false) 
        )

        val engine = FakeEngine(mockResults)
        val analyzeUc = AnalyzePositionUseCase(engine, FakeAnalysisCache())
        val reviewUc = ReviewGameUseCase(analyzeUc)

        val progressEmissions = reviewUc.invoke(game, EngineSettings()).toList()
        val summary = progressEmissions.last().review!!.summary
        
        assertEquals(1, summary.totalPlies)
        assertEquals(0, summary.analyzedPlies)
        assertEquals(0, summary.analysisCoveragePercent)
        assertEquals(null, summary.averageCentipawnLoss)
    }
}
