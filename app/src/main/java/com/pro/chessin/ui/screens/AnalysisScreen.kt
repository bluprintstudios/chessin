package com.pro.chessin.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pro.chessin.ui.components.Chessboard
import com.pro.chessin.ui.screens.viewmodels.AnalysisViewModel

/**
 * Full-screen analysis view showing an interactive chess board with
 * tap-to-move and drag-to-move support.
 */
@Composable
fun AnalysisScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val board by viewModel.boardState.collectAsState()
    val selectedSquare by viewModel.selectedSquare.collectAsState()

    Chessboard(
        board = board,
        selectedSquare = selectedSquare,
        onSquareSelected = { square -> viewModel.onSquareSelected(square) },
        onMoveAttempted = { from, to, promotion ->
            viewModel.onMoveAttempted(from, to, promotion)
        },
        modifier = modifier.fillMaxSize()
    )
}
