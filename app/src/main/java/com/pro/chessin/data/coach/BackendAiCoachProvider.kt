package com.pro.chessin.data.coach

import android.util.Log
import com.pro.chessin.domain.coach.AiCoachProvider
import com.pro.chessin.domain.coach.CoachContext
import com.pro.chessin.domain.coach.SessionTokenProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete AiCoachProvider implementation connecting to backend over SSE.
 * Streams incremental response tokens directly over HTTP SSE socket connections.
 */
@Singleton
class BackendAiCoachProvider @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider
) : AiCoachProvider {

    companion object {
        private const val TAG = "BackendAiCoachProvider"
        // Production Cloudflare Worker endpoint
        var customEndpoint: String? = null
        val endpoint: String get() = customEndpoint ?: "https://chessin.chessin.workers.dev/api/coach/stream"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun streamResponse(context: CoachContext, userQuestion: String): Flow<String> = callbackFlow {
        val sessionToken = sessionTokenProvider.getSessionToken()

        val escapedQuestion = userQuestion.replace("\\", "\\\\").replace("\"", "\\\"")
        val jsonBody = "{\"context\":${context.toJson()},\"userQuestion\":\"$escapedQuestion\"}"

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))

        if (!sessionToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $sessionToken")
        }

        val request = requestBuilder.build()
        val factory = EventSources.createFactory(client)

        val eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE Connection opened: ${response.code}")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    Log.d(TAG, "SSE Stream completed ([DONE])")
                    close()
                } else if (data.isNotEmpty()) {
                    trySend(data)
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = t?.message ?: response?.message ?: "SSE Network error"
                Log.e(TAG, "SSE Failure: $errorMsg", t)
                close(t ?: Exception(errorMsg))
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE Connection closed")
                close()
            }
        })

        awaitClose {
            Log.d(TAG, "Closing SSE EventSource")
            eventSource.cancel()
        }
    }
}
