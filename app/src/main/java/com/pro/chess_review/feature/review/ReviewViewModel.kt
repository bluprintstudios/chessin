package com.pro.chess_review.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pro.chess_review.core.engine.EngineSettings
import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.GameReview
import com.pro.chess_review.domain.model.GameReviewProgress
import com.pro.chess_review.domain.repository.GameRepository
import com.pro.chess_review.domain.repository.UserPreferencesRepository
import com.pro.chess_review.domain.usecase.ReviewGameUseCase
import com.pro.chess_review.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ReviewScreenState(
    val gameId: String,
    val whitePlayer: String? = null,
    val blackPlayer: String? = null,
    val result: String? = null,
    val reviewProgress: GameReviewProgress? = null,
    val currentPly: Int = 0,
    val review: GameReview? = null,
    val isFlipped: Boolean = false,
    val pieceSetId: String = ChessPieceSetCatalog.DEFAULT_ID,
)

class ReviewViewModel(
    private val gameId: String,
    private val gameRepository: GameRepository,
    private val reviewGameUseCase: ReviewGameUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ReviewScreenState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ReviewScreenState>> = _uiState.asStateFlow()
    private var selectedPieceSetId = ChessPieceSetCatalog.DEFAULT_ID
    private var reviewJob: Job? = null

    init {
        observePieceSetPreference()
        loadGameAndReview()
    }

    private fun observePieceSetPreference() {
        viewModelScope.launch {
            userPreferencesRepository.preferences
                .map { it.chessPieceSetId }
                .distinctUntilChanged()
                .collect { id ->
                    selectedPieceSetId = ChessPieceSetCatalog.find(id)?.id ?: ChessPieceSetCatalog.DEFAULT_ID
                    val currentState = _uiState.value as? UiState.Success
                    if (currentState != null) {
                        _uiState.value = UiState.Success(currentState.data.copy(pieceSetId = selectedPieceSetId))
                    }
                }
        }
    }

    fun selectPieceSet(id: String) {
        val pieceSetId = ChessPieceSetCatalog.find(id)?.id ?: return
        selectedPieceSetId = pieceSetId
        val currentState = _uiState.value as? UiState.Success
        if (currentState != null) {
            _uiState.value = UiState.Success(currentState.data.copy(pieceSetId = pieceSetId))
        }
        viewModelScope.launch { runCatching { userPreferencesRepository.setChessPieceSetId(pieceSetId) } }
    }

    private fun loadGameAndReview() {
        reviewJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            val game = gameRepository.getGame(gameId)
            if (game == null) {
                _uiState.value = UiState.Error(AppFailure.GameNotFound)
                return@launch
            }

            _uiState.value = UiState.Success(
                ReviewScreenState(
                    gameId = gameId,
                    whitePlayer = game.whitePlayer,
                    blackPlayer = game.blackPlayer,
                    result = game.result,
                    pieceSetId = selectedPieceSetId,
                ),
            )

            try {
                reviewGameUseCase(game, EngineSettings()).collect { progress ->
                    val currentState = _uiState.value as? UiState.Success ?: return@collect
                    _uiState.value = UiState.Success(
                        currentState.data.copy(
                            reviewProgress = progress,
                            review = progress.review ?: currentState.data.review
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(AppFailure.Unexpected(e))
            }
        }
    }

    fun cancelReview() {
        reviewJob?.cancel()
    }

    override fun onCleared() {
        cancelReview()
    }

    fun goToPly(ply: Int) {
        val currentState = _uiState.value as? UiState.Success ?: return
        val review = currentState.data.review ?: return
        val maxPly = review.positions.lastOrNull()?.plyIndex ?: 0
        val target = ply.coerceIn(0, maxPly)
        _uiState.value = UiState.Success(currentState.data.copy(currentPly = target))
    }

    fun nextMove() {
        val currentState = _uiState.value as? UiState.Success ?: return
        goToPly(currentState.data.currentPly + 1)
    }

    fun previousMove() {
        val currentState = _uiState.value as? UiState.Success ?: return
        goToPly(currentState.data.currentPly - 1)
    }

    fun toggleFlip() {
        val currentState = _uiState.value as? UiState.Success ?: return
        _uiState.value = UiState.Success(currentState.data.copy(isFlipped = !currentState.data.isFlipped))
    }
}

class ReviewViewModelFactory(
    private val gameId: String,
    private val gameRepository: GameRepository,
    private val reviewGameUseCase: ReviewGameUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return ReviewViewModel(
            gameId = gameId,
            gameRepository = gameRepository,
            reviewGameUseCase = reviewGameUseCase,
            userPreferencesRepository = userPreferencesRepository,
        ) as T
    }
}
