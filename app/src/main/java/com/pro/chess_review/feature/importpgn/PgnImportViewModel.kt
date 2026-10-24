package com.pro.chess_review.feature.importpgn

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pro.chess_review.domain.usecase.ImportPgnUseCase
import com.pro.chess_review.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class PgnImportViewModel(
    private val importPgnUseCase: ImportPgnUseCase,
) : ViewModel() {

    private val _importState = MutableStateFlow<UiState<Int>>(UiState.Empty)
    val importState: StateFlow<UiState<Int>> = _importState.asStateFlow()

    fun importPgnFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importState.value = UiState.Loading
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _importState.value = UiState.Error(com.pro.chess_review.domain.model.AppFailure.Unexpected(Exception("Could not open file")))
                    return@launch
                }
                
                val content = InputStreamReader(inputStream).use { it.readText() }
                
                val result = importPgnUseCase(content)
                result.fold(
                    onSuccess = { games ->
                        _importState.value = UiState.Success(games.size)
                    },
                    onFailure = { error ->
                        _importState.value = UiState.Error(com.pro.chess_review.domain.model.AppFailure.Unexpected(error))
                    }
                )
            } catch (e: Exception) {
                _importState.value = UiState.Error(com.pro.chess_review.domain.model.AppFailure.Unexpected(e))
            }
        }
    }
}
