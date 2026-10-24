package com.pro.chess_review.domain.billing

sealed interface EntitlementState {
    data object Loading : EntitlementState
    data object NotPurchased : EntitlementState
    data class Purchased(val token: String) : EntitlementState
    data class Error(val message: String) : EntitlementState
}

data class ProductDetails(
    val formattedPrice: String,
    val description: String,
    val name: String,
)
