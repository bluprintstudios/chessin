package com.pro.chess_review.feature.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.repository.GameRepository
import com.pro.chess_review.domain.usecase.ImportPgnUseCase
import com.pro.chess_review.ui.state.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStreamReader

class GameLibraryViewModel(
    private val gameRepository: GameRepository,
    private val importPgnUseCase: ImportPgnUseCase,
) : ViewModel() {

    private val _gamesState = MutableStateFlow<UiState<List<ChessGame>>>(UiState.Loading)
    val gamesState: StateFlow<UiState<List<ChessGame>>> = _gamesState.asStateFlow()

    private val _importState = MutableStateFlow<UiState<Int>>(UiState.Empty)
    val importState: StateFlow<UiState<Int>> = _importState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                gameRepository.observeGames().collect { games ->
                    _gamesState.value = if (games.isEmpty()) UiState.Empty else UiState.Success(games)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _gamesState.value = UiState.Error(AppFailure.Unexpected())
            }
        }
    }

    fun importPgnFile(context: Context, uri: Uri) {
        if (_importState.value is UiState.Loading) return

        viewModelScope.launch {
            _importState.value = UiState.Loading
            try {
                val pgn = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("The selected file could not be opened.")
                    InputStreamReader(input, Charsets.UTF_8).use { it.readText() }
                }

                val result = withContext(Dispatchers.Default) { importPgnUseCase(pgn) }
                _importState.value = result.fold(
                    onSuccess = { games -> UiState.Success(games.size) },
                    onFailure = { UiState.Error(AppFailure.InvalidGame) },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _importState.value = UiState.Error(AppFailure.InvalidGame)
            }
        }
    }

    fun clearImportMessage() {
        if (_importState.value !is UiState.Loading) _importState.value = UiState.Empty
    }
}

class GameLibraryViewModelFactory(
    private val gameRepository: GameRepository,
    private val importPgnUseCase: ImportPgnUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(GameLibraryViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return GameLibraryViewModel(gameRepository, importPgnUseCase) as T
    }
}
