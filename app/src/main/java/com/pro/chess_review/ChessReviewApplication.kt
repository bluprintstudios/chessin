package com.pro.chess_review

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.pro.chess_review.BuildConfig
import com.pro.chess_review.data.local.ChessReviewDatabase
import com.pro.chess_review.data.local.RoomGameRepository
import com.pro.chess_review.data.preferences.UserPreferencesStore
import com.pro.chess_review.domain.repository.GameRepository

class ChessReviewApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val database: ChessReviewDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            ChessReviewDatabase::class.java,
            "chess_review.db",
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    val gamesRepository: GameRepository by lazy {
        RoomGameRepository(database.gameDao())
    }

    val analysisCache: com.pro.chess_review.core.engine.AnalysisCache by lazy {
        com.pro.chess_review.core.engine.RoomAnalysisCache(database.analysisDao())
    }

    val userPreferences: UserPreferencesStore by lazy {
        UserPreferencesStore(context.applicationContext)
    }

    val importPgnUseCase: com.pro.chess_review.domain.usecase.ImportPgnUseCase by lazy {
        com.pro.chess_review.domain.usecase.ImportPgnUseCase(gamesRepository)
    }

    private val okHttpClient: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder().build()
    }

    val chessEngine: com.pro.chess_review.core.engine.ChessEngine by lazy {
        // Stockfish is packaged as an Android native executable and extracted by the installer.
        com.pro.chess_review.core.engine.UciEngineProcess(
            java.io.File(applicationContext.applicationInfo.nativeLibraryDir, "libstockfish.so"),
        )
    }

    val analyzePositionUseCase: com.pro.chess_review.domain.usecase.AnalyzePositionUseCase by lazy {
        com.pro.chess_review.domain.usecase.AnalyzePositionUseCase(chessEngine, analysisCache)
    }

    val reviewGameUseCase: com.pro.chess_review.domain.usecase.ReviewGameUseCase by lazy {
        com.pro.chess_review.domain.usecase.ReviewGameUseCase(analyzePositionUseCase)
    }

    val aiCoachClient: com.pro.chess_review.data.ai.AiCoachClient by lazy {
        com.pro.chess_review.data.ai.AiCoachClient(
            workerBaseUrl = BuildConfig.AI_COACH_BASE_URL,
            okHttpClient = okHttpClient
        )
    }

    val getCoachExplanationUseCase: com.pro.chess_review.domain.usecase.GetCoachExplanationUseCase by lazy {
        com.pro.chess_review.domain.usecase.GetCoachExplanationUseCase(aiCoachClient, userPreferences)
    }

    /** True only when the build receives a reviewed Worker URL through a local Gradle property. */
    val isAiCoachServiceConfigured: Boolean = BuildConfig.AI_COACH_SERVICE_CONFIGURED

    /**
     * Never true in a release build. This supports the private, machine-local free-model preview
     * documented in LOCAL_FREE_COACH_PREVIEW.md without weakening Play verification in release.
     */
    val isLocalAiCoachPreview: Boolean = BuildConfig.DEBUG &&
        BuildConfig.AI_COACH_BASE_URL.startsWith("http://")

    fun createBillingManager(): com.pro.chess_review.domain.billing.BillingManager =
        com.pro.chess_review.data.billing.PlayBillingManager(applicationContext)
}
