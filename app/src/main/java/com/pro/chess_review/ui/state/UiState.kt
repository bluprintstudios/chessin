package com.pro.chess_review.ui.state

import com.pro.chess_review.domain.model.AppFailure

/**
 * Represents the core screen states for UI components.
 * This provides typed state handling for loading, empty, offline, and error conditions
 * without defining the visual representation.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val failure: AppFailure) : UiState<Nothing>
}
