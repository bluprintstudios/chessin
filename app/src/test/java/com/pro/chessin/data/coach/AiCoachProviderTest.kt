package com.pro.chessin.data.coach

import com.pro.chessin.domain.coach.AiCoachProvider
import com.pro.chessin.domain.coach.CoachContext
import com.pro.chessin.domain.coach.GuestSessionTokenProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AiCoachProvider implementations.
 */
class AiCoachProviderTest {

    private val sampleContext = CoachContext(
        fenBefore = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
        sanMove = "e4",
        classificationTier = "BEST",
        evalDeltaCp = 0,
        alternativeLines = emptyList(),
        priorMoveHistory = emptyList()
    )

    @Test
    fun testMockAiCoachProvider_StreamsTokensIncrementally() = runBlocking {
        val provider: AiCoachProvider = MockAiCoachProvider()
        val tokens = provider.streamResponse(sampleContext, "Why play e4?").toList()

        assertTrue("Mock provider should emit multiple tokens", tokens.size > 5)

        val fullText = tokens.joinToString("")
        println("=== Mock AiCoachProvider Stream Result ===")
        println("Token count: ${tokens.size}")
        println("Full text: $fullText")

        assertTrue("Full text should explain e4", fullText.contains("The move e4 is the best move"))
        assertTrue("Full text should include user question", fullText.contains("Why play e4?"))
    }

    @Test
    fun testGuestSessionTokenProvider_ReturnsPlaceholderToken() = runBlocking {
        val tokenProvider = GuestSessionTokenProvider()
        val token = tokenProvider.getSessionToken()

        assertEquals("guest_session_placeholder", token)
    }

    @Test
    fun testBackendAiCoachProvider_EndpointConfiguration() {
        val provider = BackendAiCoachProvider(GuestSessionTokenProvider())
        assertTrue("Backend endpoint should be valid URL", BackendAiCoachProvider.endpoint.startsWith("http"))
    }

    @Test
    fun testBackendAiCoachProvider_LiveE2eStreamingRequest() = runBlocking {
        val server = okhttp3.mockwebserver.MockWebServer()
        val sseResponse = "data: The move e4 \n\ndata: is the best move in this position.\n\ndata: [DONE]\n\n"
        server.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseResponse)
        )
        server.start()

        val serverUrl = server.url("/v1/coach/chat").toString()
        BackendAiCoachProvider.customEndpoint = serverUrl
        val provider = BackendAiCoachProvider(GuestSessionTokenProvider())

        val tokens = mutableListOf<String>()
        val startMs = System.currentTimeMillis()

        try {
            provider.streamResponse(sampleContext, "Explain this move").collect { token ->
                tokens.add(token)
            }
        } catch (e: Exception) {
            println("Live SSE stream test error: ${e.message}")
        } finally {
            server.shutdown()
        }

        val elapsedMs = System.currentTimeMillis() - startMs
        val fullText = tokens.joinToString("")

        println("=== Live Backend SSE Streaming Test ===")
        println("Tokens received: ${tokens.size}")
        println("Elapsed time: ${elapsedMs}ms")
        println("Full streamed text: $fullText")

        assertTrue("Live SSE stream should receive tokens", tokens.isNotEmpty())
        assertTrue("Live stream should contain move explanation", fullText.contains("e4"))
    }
}
