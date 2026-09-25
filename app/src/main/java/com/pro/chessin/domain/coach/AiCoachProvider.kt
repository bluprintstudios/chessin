package com.pro.chessin.domain.coach

import kotlinx.coroutines.flow.Flow

/**
 * Interface for AI Coach explanation streaming.
 * Decouples the UI from specific vendor implementations (backend SSE vs mock).
 */
interface AiCoachProvider {
    /**
     * Streams incremental AI Coach explanation response tokens for a move context.
     *
     * @param context Structured move context payload
     * @param userQuestion User's question or default prompt
     * @return Flow emitting incremental response text tokens
     */
    fun streamResponse(context: CoachContext, userQuestion: String): Flow<String>
}
