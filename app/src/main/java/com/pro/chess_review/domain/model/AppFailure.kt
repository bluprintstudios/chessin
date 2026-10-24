package com.pro.chess_review.domain.model

sealed interface AppFailure {
    data object InvalidUsername : AppFailure
    data object InvalidGame : AppFailure
    data object GameNotFound : AppFailure
    data object NetworkUnavailable : AppFailure
    data object RateLimited : AppFailure
    data object PurchaseNotVerified : AppFailure
    data object AiUnavailable : AppFailure
    data class Unexpected(val cause: Throwable? = null) : AppFailure
}

class AppFailureException(val failure: AppFailure) : Exception(failure.toString())

