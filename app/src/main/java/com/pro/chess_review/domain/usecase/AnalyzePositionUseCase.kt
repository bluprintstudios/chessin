package com.pro.chess_review.domain.usecase

import com.pro.chess_review.core.engine.AnalysisCache
import com.pro.chess_review.core.engine.ChessEngine
import com.pro.chess_review.core.engine.EngineResult
import com.pro.chess_review.core.engine.EngineSettings
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class AnalyzePositionUseCase(
    private val engine: ChessEngine,
    private val cache: AnalysisCache
) {
    /**
     * Attempts to return a cached evaluation first. If none meets the constraints,
     * starts the engine, analyzes the position, caches the final result, and streams the flow.
     */
    operator fun invoke(fen: String, settings: EngineSettings): Flow<EngineResult> = flow {
        val cached = cache.getCachedResult(fen, engine.engineId, engine.engineVersion, settings)
        if (cached != null) {
            emit(cached)
            return@flow
        }

        // Cache miss -> use engine. Always release the local process, including when
        // the review screen is left before analysis completes.
        try {
            engine.start()
            engine.analyze(fen, settings).collect { result ->
                if (result.isComplete) {
                    cache.saveResult(result, settings)
                }
                emit(result)
            }
        } finally {
            withContext(NonCancellable) {
                engine.stop()
            }
        }
    }
}
