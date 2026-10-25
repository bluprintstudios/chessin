package com.pro.chess_review.navigation

/** Stable route identifiers for provisional Compose screens and the eventual design handoff. */
sealed class AppDestination(val route: String) {
    data object Onboarding : AppDestination("onboarding")
    data object Games : AppDestination("games")
    data object Import : AppDestination("import")
    data object Review : AppDestination("review/{gameId}") {
        fun forGame(gameId: String): String = "review/$gameId"
    }

    data object Coach : AppDestination("coach/{gameId}/{ply}") {
        fun forPosition(gameId: String, ply: Int): String = "coach/$gameId/$ply"
    }

    data object Settings : AppDestination("settings")
    data object Licenses : AppDestination("licenses")
    data object Paywall : AppDestination("paywall")
}
