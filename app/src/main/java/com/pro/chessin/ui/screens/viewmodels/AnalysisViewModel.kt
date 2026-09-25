package com.pro.chessin.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Move
import com.pro.chessin.domain.chess.MoveGenerator
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.Square
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the analysis screen.
 *
 * Holds the chess board state and selected square as observable flows.
 * The board position and selection are owned here; the UI merely renders
 * and reports intentions back via the callbacks.
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor() : ViewModel() {

    private val _boardState = MutableStateFlow(ChessBoardState.startPosition())
    val boardState: StateFlow<ChessBoardState> = _boardState.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Square?>(null)
    val selectedSquare: StateFlow<Square?> = _selectedSquare.asStateFlow()

    /**
     * Select or deselect a square.
     */
    fun onSquareSelected(square: Square) {
        _selectedSquare.value = square
    }

    /**
     * Attempt to play [from] → [to] with an optional [promotion] piece type.
     * The move is validated against the current legal-move list; if it is
     * not a legal move the board state is left unchanged.
     */
    fun onMoveAttempted(from: Square, to: Square, promotion: PieceType?) {
        val board = _boardState.value
        val moves = MoveGenerator.generateLegalMoves(board)
        val move = moves.find {
            it.from == from && it.to == to && it.promotion == promotion
        }
        if (move != null) {
            _boardState.value = board.makeMove(move)
            _selectedSquare.value = null
        }
    }

    /**
     * Reset the board to the standard starting position.
     */
    fun reset() {
        _boardState.value = ChessBoardState.startPosition()
        _selectedSquare.value = null
    }
}
