package com.pro.chess_review

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pro.chess_review.feature.library.GameLibraryScreen
import com.pro.chess_review.feature.library.GameLibraryViewModel
import com.pro.chess_review.feature.library.GameLibraryViewModelFactory
import com.pro.chess_review.feature.coach.CoachScreen
import com.pro.chess_review.feature.coach.CoachViewModel
import com.pro.chess_review.feature.coach.CoachViewModelFactory
import com.pro.chess_review.feature.paywall.PaywallScreen
import com.pro.chess_review.feature.paywall.PaywallViewModel
import com.pro.chess_review.feature.paywall.PaywallViewModelFactory
import com.pro.chess_review.feature.review.ReviewScreen
import com.pro.chess_review.feature.review.ReviewViewModel
import com.pro.chess_review.feature.review.ReviewViewModelFactory
import com.pro.chess_review.navigation.AppDestination
import com.pro.chess_review.feature.settings.SettingsScreen
import com.pro.chess_review.feature.settings.OpenSourceLicensesScreen
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.ui.theme.Chess_reviewTheme
import com.pro.chess_review.feature.onboarding.OnboardingScreen
import com.pro.chess_review.feature.importpgn.ImportPgnScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as ChessReviewApplication).appContainer

        setContent {
            ChessReviewApp(appContainer)
        }
    }
}

@Composable
private fun ChessReviewApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val preferences by appContainer.userPreferences.preferences.collectAsState(
        initial = com.pro.chess_review.data.preferences.UserPreferences(),
    )
    val scope = rememberCoroutineScope()

    Chess_reviewTheme(themeId = preferences.hudThemeId) {
        LaunchedEffect(preferences.hasCompletedOnboarding) {
            if (preferences.hasCompletedOnboarding && navController.currentDestination?.route == AppDestination.Onboarding.route) {
                navController.navigate(AppDestination.Games.route) {
                    popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = if (preferences.hasCompletedOnboarding) AppDestination.Games.route else AppDestination.Onboarding.route,
        ) {
        composable(AppDestination.Onboarding.route) {
            OnboardingScreen(
                onContinue = {
                    scope.launch {
                        appContainer.userPreferences.setOnboardingComplete(true)
                    }
                },
                onImportPgn = {
                    scope.launch {
                        appContainer.userPreferences.setOnboardingComplete(true)
                    }
                    navController.navigate(AppDestination.Import.route)
                },
            )
        }

        composable(AppDestination.Games.route) {
            val factory = remember(appContainer) {
                GameLibraryViewModelFactory(
                    gameRepository = appContainer.gamesRepository,
                    importPgnUseCase = appContainer.importPgnUseCase,
                )
            }
            val viewModel: GameLibraryViewModel = viewModel(factory = factory)

            GameLibraryScreen(
                viewModel = viewModel,
                onOpenGame = { gameId ->
                    navController.navigate(AppDestination.Review.forGame(gameId))
                },
                onOpenImport = { navController.navigate(AppDestination.Import.route) },
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                onOpenPaywall = { navController.navigate(AppDestination.Paywall.route) },
            )
        }

        composable(AppDestination.Import.route) { backStackEntry ->
            val factory = remember(appContainer) {
                GameLibraryViewModelFactory(
                    gameRepository = appContainer.gamesRepository,
                    importPgnUseCase = appContainer.importPgnUseCase,
                )
            }
            val importViewModel: GameLibraryViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = factory,
            )
            ImportPgnScreen(
                viewModel = importViewModel,
                onBack = { navController.popBackStack() },
                onImported = {
                    navController.popBackStack(AppDestination.Games.route, inclusive = false)
                },
            )
        }

        composable(
            route = AppDestination.Review.route,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId").orEmpty()
            val factory = remember(appContainer, gameId) {
                ReviewViewModelFactory(
                    gameId = gameId,
                    gameRepository = appContainer.gamesRepository,
                    reviewGameUseCase = appContainer.reviewGameUseCase,
                    userPreferencesRepository = appContainer.userPreferences,
                )
            }
            val reviewViewModel: ReviewViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = factory,
            )
            ReviewScreen(
                viewModel = reviewViewModel,
                onBack = {
                    reviewViewModel.cancelReview()
                    navController.popBackStack()
                },
                onAskCoach = { request, ply ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("coach_request", request)
                    navController.navigate(AppDestination.Coach.forPosition(gameId, ply))
                },
            )
        }

        composable(
            route = AppDestination.Coach.route,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("ply") { type = NavType.IntType },
            ),
        ) { backStackEntry ->
            val request = remember(backStackEntry) {
                navController.previousBackStackEntry?.savedStateHandle?.get<CoachRequest>("coach_request")
            } ?: CoachRequest(fen = "", question = "", playedMove = "", engineEvidence = emptyList())
            val factory = remember(appContainer, request) {
                CoachViewModelFactory(
                    requestContext = request,
                    getCoachExplanation = appContainer.getCoachExplanationUseCase,
                    billingManager = appContainer.createBillingManager(),
                    serviceConfigured = appContainer.isAiCoachServiceConfigured,
                    isLocalDebugPreview = appContainer.isLocalAiCoachPreview,
                )
            }
            val coachViewModel: CoachViewModel = viewModel(viewModelStoreOwner = backStackEntry, factory = factory)
            CoachScreen(
                request = request,
                viewModel = coachViewModel,
                onBack = { navController.popBackStack() },
                onOpenPaywall = { navController.navigate(AppDestination.Paywall.route) },
            )
        }

        composable(AppDestination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPaywall = { navController.navigate(AppDestination.Paywall.route) },
                onOpenLicenses = { navController.navigate(AppDestination.Licenses.route) },
                selectedThemeId = preferences.hudThemeId,
                onSelectTheme = { themeId ->
                    scope.launch { appContainer.userPreferences.setHudThemeId(themeId) }
                },
                onDeleteData = { 
                    scope.launch { 
                        appContainer.gamesRepository.deleteAllGames()
                        appContainer.userPreferences.setOnboardingComplete(false)
                        navController.navigate(AppDestination.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(AppDestination.Licenses.route) {
            OpenSourceLicensesScreen(onBack = { navController.popBackStack() })
        }

        composable(AppDestination.Paywall.route) { backStackEntry ->
            val factory = remember(appContainer) {
                PaywallViewModelFactory(appContainer.createBillingManager())
            }
            val paywallViewModel: PaywallViewModel = viewModel(viewModelStoreOwner = backStackEntry, factory = factory)
            PaywallScreen(viewModel = paywallViewModel, onBack = { navController.popBackStack() })
        }
        }
    }
}
