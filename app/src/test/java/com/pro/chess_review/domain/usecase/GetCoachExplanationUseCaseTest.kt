package com.pro.chess_review.domain.usecase

import com.pro.chess_review.data.ai.AiCoachClient
import com.pro.chess_review.data.preferences.UserPreferences
import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.AppFailureException
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.domain.model.EngineEvidenceItem
import com.pro.chess_review.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCoachExplanationUseCaseTest {

    private class FakePreferencesRepository(
        private var consent: Boolean = false,
        private var usageCount: Int = 0
    ) : UserPreferencesRepository {
        override val preferences: Flow<UserPreferences> = flowOf(UserPreferences(hasAiConsent = consent, aiUsageCount = usageCount))
        override suspend fun getAiConsent(): Boolean = consent
        override suspend fun setAiConsent(granted: Boolean) { consent = granted }
        override suspend fun getAiUsageCount(): Int = usageCount
        override suspend fun incrementAiUsageCount() { usageCount++ }
        override suspend fun setChessPieceSetId(id: String) {}
        override suspend fun setHudThemeId(id: String) {}
    }

    private fun createOkHttpClient(jsonResponse: String, statusCode: Int = 200): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message("OK")
                    .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    private val sampleRequest = CoachRequest(
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        question = "Why e4?",
        playedMove = "e4",
        engineEvidence = listOf(
            EngineEvidenceItem("e2e4", "e4", 45, null, 18)
        )
    )

    @Test
    fun invoke_withoutConsent_failsConsentRequired() = runBlocking {
        val fakeRepo = FakePreferencesRepository(consent = false)
        val client = AiCoachClient("https://worker.local/", createOkHttpClient("{}"))
        val useCase = GetCoachExplanationUseCase(client, fakeRepo)

        val result = useCase(sampleRequest, "token")
        assertTrue(result.isFailure)

        val err = result.exceptionOrNull() as? AppFailureException
        assertTrue(err?.failure is AppFailure.Unexpected)
    }

    @Test
    fun invoke_whenLimitReached_failsRateLimited() = runBlocking {
        val fakeRepo = FakePreferencesRepository(consent = true, usageCount = 50)
        val client = AiCoachClient("https://worker.local/", createOkHttpClient("{}"))
        val useCase = GetCoachExplanationUseCase(client, fakeRepo)

        val result = useCase(sampleRequest, "token")
        assertTrue(result.isFailure)

        val failure = (result.exceptionOrNull() as? AppFailureException)?.failure
        assertEquals(AppFailure.RateLimited, failure)
    }

    @Test
    fun invoke_withConsentAndQuota_incrementsUsageCount() = runBlocking {
        val fakeRepo = FakePreferencesRepository(consent = true, usageCount = 5)
        val json = """
            {
                "version": "1",
                "explanation": "e4 controls center.",
                "groundedCandidates": ["e4"],
                "disclaimer": "Grounding disclaimer"
            }
        """.trimIndent()
        val client = AiCoachClient("https://worker.local/", createOkHttpClient(json))
        val useCase = GetCoachExplanationUseCase(client, fakeRepo)

        val result = useCase(sampleRequest, "token")
        assertTrue(result.isSuccess)
        assertEquals(6, fakeRepo.getAiUsageCount())
    }
}
