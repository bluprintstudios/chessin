package com.pro.chess_review.feature.coach

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.SdkSuppress
import com.pro.chess_review.data.ai.AiCoachClient
import com.pro.chess_review.data.preferences.UserPreferences
import com.pro.chess_review.domain.billing.*
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.domain.model.EngineEvidenceItem
import com.pro.chess_review.domain.repository.UserPreferencesRepository
import com.pro.chess_review.domain.usecase.GetCoachExplanationUseCase
import com.pro.chess_review.ui.theme.Chess_reviewTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test

/**
 * kotlinx-coroutines-test cannot discover its ExceptionCollector ServiceLoader provider from
 * the instrumentation APK on API 36, so Compose's test rule crashes before each test body.
 * Keep these tests enabled on every other API level; their Android 17 emulator run is the
 * supported verification path until the upstream coroutines issue is resolved.
 *
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/4782.
 */
@SdkSuppress(excludedSdks = [36])
class CoachScreenTest {
    @get:Rule val compose = createComposeRule()

    private val request = CoachRequest(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        question = "", playedMove = "e4",
        engineEvidence = listOf(EngineEvidenceItem("e7e5", "e5", -15, null, 18,
            listOf("e5", "Nf3", "Nc6"), listOf("e7e5", "g1f3", "b8c6"))),
    )

    private fun model(context: CoachRequest, configured: Boolean) = CoachViewModel(
        context,
        GetCoachExplanationUseCase(
            AiCoachClient("https://unused.invalid/", OkHttpClient.Builder().addInterceptor {
                error("UI verification must never send a network request")
            }.build()),
            object : UserPreferencesRepository {
                override val preferences = flowOf(UserPreferences())
                override suspend fun getAiConsent() = false
                override suspend fun setAiConsent(granted: Boolean) = Unit
                override suspend fun getAiUsageCount() = 0
                override suspend fun incrementAiUsageCount() = Unit
                override suspend fun setChessPieceSetId(id: String) = Unit
                override suspend fun setHudThemeId(id: String) = Unit
            },
        ),
        object : BillingManager {
            override val entitlementState = MutableStateFlow<EntitlementState>(EntitlementState.NotPurchased)
            override val productDetails = MutableStateFlow<ProductDetails?>(null)
            override fun startConnection() = Unit
            override fun launchBillingFlow(activity: Activity?) = Unit
            override fun restorePurchases() = Unit
            override fun destroy() = Unit
        },
        configured,
        false,
    )

    @Test fun draftSurvivesSavedStateRestorationInDarkTheme() {
        val restoration = StateRestorationTester(compose)
        val vm = model(request, true)
        restoration.setContent {
            Chess_reviewTheme(darkTheme = true) { CoachScreen(request, vm, {}, {}) }
        }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("Why this continuation?")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextContains("Why this continuation?")
        saveScreenshot("coach-dark-draft.png")
        compose.onNodeWithText("Ask Chessin'").performScrollTo().assertIsNotEnabled()
    }

    @Test fun previewShowsWhitePerspectiveAndContinuationWithoutAllowingRequests() {
        val vm = model(request, false)
        compose.setContent {
            Chess_reviewTheme(darkTheme = true) { CoachScreen(request, vm, {}, {}) }
        }
        compose.onNodeWithText("Scores favor White when positive and Black when negative.").assertExists()
        compose.onNodeWithText("1. e5  ·  -0.15  ·  depth 18").assertExists()
        compose.onNodeWithText("e5 → Nf3 → Nc6").assertExists()
        compose.onNodeWithText("e.g. Why was this move important?").assertIsNotEnabled()
        compose.onNodeWithText("AI Coach unavailable in preview").performScrollTo().assertIsNotEnabled()
        saveScreenshot("coach-dark-preview.png")
    }

    @Test fun negativeMateNamesBlackAsWinningSide() {
        val mateRequest = request.copy(engineEvidence = listOf(request.engineEvidence.first().copy(scoreCp = null, mateIn = -3)))
        val vm = model(mateRequest, false)
        compose.setContent { Chess_reviewTheme { CoachScreen(mateRequest, vm, {}, {}) } }
        compose.onNodeWithText("1. e5  ·  Black mates in 3  ·  depth 18").assertExists()
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
