package com.pro.chess_review.data.ai

import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.AppFailureException
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.domain.model.CoachResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AiCoachClient(
    private val workerBaseUrl: String,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun requestExplanation(
        coachRequest: CoachRequest,
        entitlementToken: String? = null
    ): Result<CoachResponse> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("version", coachRequest.version)
                put("fen", coachRequest.fen)
                put("question", coachRequest.question)
                put("playedMove", coachRequest.playedMove)
                
                val evArray = JSONArray()
                coachRequest.engineEvidence.forEach { ev ->
                    val item = JSONObject().apply {
                        put("uci", ev.uci)
                        put("san", ev.san)
                        put("scoreCp", ev.scoreCp ?: JSONObject.NULL)
                        put("mateIn", ev.mateIn ?: JSONObject.NULL)
                        put("depth", ev.depth)
                        put("pvSan", JSONArray(ev.pvSan))
                        put("pvUci", JSONArray(ev.pvUci))
                    }
                    evArray.put(item)
                }
                put("engineEvidence", evArray)
            }

            val requestBuilder = Request.Builder()
                .url(workerBaseUrl)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

            if (!entitlementToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $entitlementToken")
            }

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext when (response.code) {
                        401 -> Result.failure(AppFailureException(AppFailure.PurchaseNotVerified))
                        429 -> Result.failure(AppFailureException(AppFailure.RateLimited))
                        503 -> Result.failure(AppFailureException(AppFailure.AiUnavailable))
                        else -> Result.failure(AppFailureException(AppFailure.Unexpected(IOException("HTTP ${response.code}"))))
                    }
                }

                val bodyStr = response.body?.string()
                    ?: return@withContext Result.failure(AppFailureException(AppFailure.Unexpected(IOException("Empty response"))))

                val jsonResponse = JSONObject(bodyStr)
                val version = jsonResponse.optString("version", "1")
                val explanation = jsonResponse.optString("explanation", "")
                val disclaimer = jsonResponse.optString("disclaimer", "")
                
                val groundedArr = jsonResponse.optJSONArray("groundedCandidates") ?: JSONArray()
                val grounded = mutableListOf<String>()
                for (i in 0 until groundedArr.length()) {
                    grounded.add(groundedArr.getString(i))
                }

                Result.success(
                    CoachResponse(
                        version = version,
                        explanation = explanation,
                        groundedCandidates = grounded,
                        disclaimer = disclaimer
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(AppFailureException(AppFailure.NetworkUnavailable))
        }
    }
}
