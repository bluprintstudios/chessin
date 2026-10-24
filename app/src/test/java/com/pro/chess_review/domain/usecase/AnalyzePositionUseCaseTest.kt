package com.pro.chess_review.domain.usecase

import com.pro.chess_review.core.engine.AnalysisCache
import com.pro.chess_review.core.engine.ChessEngine
import com.pro.chess_review.core.engine.EngineResult
import com.pro.chess_review.core.engine.EngineSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyzePositionUseCaseTest {

    @Test
    fun cancellationStopsTheStartedEngine() = runBlocking {
        val engine = BlockingEngine()
        val useCase = AnalyzePositionUseCase(engine, EmptyCache())

        val collection = launch {
            useCase("test-fen", EngineSettings()).collect { }
        }
        engine.analysisStarted.await()

        collection.cancelAndJoin()

        assertEquals(1, engine.startCalls)
        assertEquals(1, engine.stopCalls)
    }

    private class EmptyCache : AnalysisCache {
        override suspend fun getCachedResult(
            fen: String,
            engineId: String,
            engineVersion: String,
            settings: EngineSettings,
        ): EngineResult? = null

        override suspend fun saveResult(result: EngineResult, settings: EngineSettings) = Unit
    }

    private class BlockingEngine : ChessEngine {
        override val engineId = "test"
        override val engineVersion = "1"
        val analysisStarted = CompletableDeferred<Unit>()
        var startCalls = 0
        var stopCalls = 0

        override suspend fun start() {
            startCalls += 1
        }

        override suspend fun stop() {
            withContext(Dispatchers.IO) {
                stopCalls += 1
            }
        }

        override fun analyze(fen: String, settings: EngineSettings): Flow<EngineResult> = flow {
            analysisStarted.complete(Unit)
            awaitCancellation()
        }
    }
}
