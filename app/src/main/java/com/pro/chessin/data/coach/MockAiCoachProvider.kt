package com.pro.chessin.data.coach

import com.pro.chessin.domain.coach.AiCoachProvider
import com.pro.chessin.domain.coach.CoachContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline mock AiCoachProvider implementation.
 * Emits realistic, position-aware canned explanations word-by-word with typing delays (~30ms/token).
 */
@Singleton
class MockAiCoachProvider @Inject constructor() : AiCoachProvider {

    override fun streamResponse(context: CoachContext, userQuestion: String): Flow<String> = flow {
        val move = context.sanMove
        val tier = context.classificationTier
        val delta = context.evalDeltaCp

        val explanation = when (tier) {
            "BEST", "BOOK" -> "The move $move is the best move in this position! It maintains optimal control and development."
            "EXCELLENT", "GOOD" -> "The move $move is a solid move! It keeps a good position with a tiny delta of ${delta}cp."
            "INACCURACY" -> "The move $move is an inaccuracy (eval loss: ${delta}cp). A better line was available."
            "MISTAKE" -> "The move $move is a mistake (eval loss: ${delta}cp). It loses momentum in the position."
            "BLUNDER" -> "The move $move is a blunder (eval loss: ${delta}cp). It gives away a significant tactical or material advantage."
            else -> "The move $move was analyzed with an eval delta of ${delta}cp."
        }

        val fullText = "$explanation Regarding your question: \"$userQuestion\" — focus on piece activity and king safety."
        val tokens = fullText.split(" ")

        for ((index, token) in tokens.withIndex()) {
            val space = if (index < tokens.size - 1) " " else ""
            emit(token + space)
            delay(30) // 30ms typing delay per token
        }
    }
}
