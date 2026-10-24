package com.pro.chess_review.data.ai

import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.AppFailureException
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.domain.model.EngineEvidenceItem
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
import org.json.JSONObject
import okio.Buffer

class AiCoachClientTest {

    @Test
    fun requestExplanation_serializesVersionTwoContinuations() = runBlocking {
        val evidence = EngineEvidenceItem("e7e5", "e5", -15, null, 18,
            pvSan = listOf("e5", "Nf3", "Nc6"),
            pvUci = listOf("e7e5", "g1f3", "b8c6"))
        val client = createClientWithInterceptor { chain ->
            val buffer = Buffer()
            chain.request().body!!.writeTo(buffer)
            val json = JSONObject(buffer.readUtf8())
            assertEquals("2", json.getString("version"))
            val line = json.getJSONArray("engineEvidence").getJSONObject(0)
            assertEquals(-15, line.getInt("scoreCp"))
            assertEquals(3, line.getJSONArray("pvSan").length())
            assertEquals("Nc6", line.getJSONArray("pvSan").getString(2))
            assertEquals("b8c6", line.getJSONArray("pvUci").getString(2))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body("""{"version":"1","explanation":"Consider e5.","groundedCandidates":["e5"],"disclaimer":""}""".toResponseBody("application/json".toMediaType()))
                .build()
        }
        assertTrue(client.requestExplanation(sampleRequest.copy(engineEvidence = listOf(evidence))).isSuccess)
    }

    private fun createClientWithInterceptor(handler: (Interceptor.Chain) -> Response): AiCoachClient {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(handler)
            .build()
        return AiCoachClient("https://worker.local/api/v1/explain", okHttpClient)
    }

    private val sampleRequest = CoachRequest(
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        question = "Why is e4 good?",
        playedMove = "e4",
        engineEvidence = listOf(
            EngineEvidenceItem(uci = "e2e4", san = "e4", scoreCp = 45, mateIn = null, depth = 18)
        )
    )

    @Test
    fun requestExplanation_success_returnsCoachResponse() = runBlocking {
        val client = createClientWithInterceptor { chain ->
            val req = chain.request()
            assertEquals("https://worker.local/api/v1/explain", req.url.toString())
            assertEquals("Bearer test-token", req.header("Authorization"))

            val json = """
                {
                    "version": "1",
                    "explanation": "e4 controls the center.",
                    "groundedCandidates": ["e4"],
                    "disclaimer": "Grounding disclaimer"
                }
            """.trimIndent()

            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = client.requestExplanation(sampleRequest, entitlementToken = "test-token")
        assertTrue(result.isSuccess)

        val response = result.getOrNull()!!
        assertEquals("1", response.version)
        assertEquals("e4 controls the center.", response.explanation)
        assertEquals(listOf("e4"), response.groundedCandidates)
    }

    @Test
    fun requestExplanation_unauthorized_returnsPurchaseNotVerifiedFailure() = runBlocking {
        val client = createClientWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = client.requestExplanation(sampleRequest, entitlementToken = null)
        assertTrue(result.isFailure)

        val failure = (result.exceptionOrNull() as? AppFailureException)?.failure
        assertEquals(AppFailure.PurchaseNotVerified, failure)
    }

    @Test
    fun requestExplanation_rateLimited_returnsRateLimitedFailure() = runBlocking {
        val client = createClientWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(429)
                .message("Too Many Requests")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = client.requestExplanation(sampleRequest, entitlementToken = "test-token")
        assertTrue(result.isFailure)

        val failure = (result.exceptionOrNull() as? AppFailureException)?.failure
        assertEquals(AppFailure.RateLimited, failure)
    }

    @Test
    fun requestExplanation_serviceUnavailable_returnsAiUnavailableFailure() = runBlocking {
        val client = createClientWithInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val result = client.requestExplanation(sampleRequest, entitlementToken = "test-token")
        assertTrue(result.isFailure)

        val failure = (result.exceptionOrNull() as? AppFailureException)?.failure
        assertEquals(AppFailure.AiUnavailable, failure)
    }
}
