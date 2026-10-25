package com.pro.chess_review.domain.usecase

import com.pro.chess_review.data.ai.AiCoachClient
import com.pro.chess_review.data.preferences.UserPreferencesStore
import com.pro.chess_review.domain.model.AiConsentState
import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.AppFailureException
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.domain.model.CoachResponse

class GetCoachExplanationUseCase(
    private val aiCoachClient: AiCoachClient,
    private val preferencesRepository: com.pro.chess_review.domain.repository.UserPreferencesRepository,
) {
    suspend operator fun invoke(
        request: CoachRequest,
        entitlementToken: String? = null
    ): Result<CoachResponse> {
        val hasConsented = preferencesRepository.getAiConsent()
        if (!hasConsented) {
            return Result.failure(AppFailureException(AppFailure.Unexpected(IllegalStateException("User consent for AI data usage is required before sending requests"))))
        }

        val currentUsage = preferencesRepository.getAiUsageCount()
        if (currentUsage >= 50) { // Fair use allowance limit
            return Result.failure(AppFailureException(AppFailure.RateLimited))
        }

        val result = aiCoachClient.requestExplanation(request, entitlementToken)
        if (result.isSuccess) {
            preferencesRepository.incrementAiUsageCount()
        }
        return result
    }

    suspend fun getConsentState(): AiConsentState {
        val consented = preferencesRepository.getAiConsent()
        val count = preferencesRepository.getAiUsageCount()
        return AiConsentState(hasConsented = consented, usageCount = count)
    }

    suspend fun setConsent(granted: Boolean) {
        preferencesRepository.setAiConsent(granted)
    }
}
