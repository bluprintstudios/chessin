package com.pro.chessin.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pro.chessin.data.PuzzleRepository
import com.pro.chessin.data.model.Puzzle
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Color as ChessColor
import com.pro.chessin.domain.chess.MoveGenerator
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.SanUtils
import com.pro.chessin.domain.chess.Square
import com.pro.chessin.ui.components.BoardOrientation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PuzzleStatus {
    LOADING,
    IN_PROGRESS,
    CORRECT,
    FAILED
}

data class PuzzlesUiState(
    val status: PuzzleStatus = PuzzleStatus.LOADING,
    val currentPuzzle: Puzzle? = null,
    val boardState: ChessBoardState = ChessBoardState.startPosition(),
    val boardOrientation: BoardOrientation = BoardOrientation.WHITE_AT_BOTTOM,
    val selectedSquare: Square? = null,
    val userRating: Int = 1500,
    val currentMoveIndex: Int = 0,
    val lastRatingDelta: Int? = null,
    val feedbackMessage: String? = null,
    val isAutoResponding: Boolean = false,
    val hintText: String? = null,
    val startTimeMs: Long = System.currentTimeMillis()
)

@HiltViewModel
class PuzzlesViewModel @Inject constructor(
    private val puzzleRepository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzlesUiState())
    val uiState: StateFlow<PuzzlesUiState> = _uiState.asStateFlow()

    init {
        loadUserRatingAndPuzzle()
    }

    fun loadUserRatingAndPuzzle() {
        viewModelScope.launch {
            puzzleRepository.ensureCuratedPuzzlesImported()
            val ratingState = puzzleRepository.getUserRatingState()
            val ratingInt = ratingState.rating.toInt()
            _uiState.update { it.copy(userRating = ratingInt) }
            loadNextPuzzle()
        }
    }

    fun loadNextPuzzle() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = PuzzleStatus.LOADING, feedbackMessage = null, hintText = null) }
            val currentRating = _uiState.value.userRating
            val puzzle = puzzleRepository.getRandomPuzzle(currentRating)

            if (puzzle != null) {
                val board = ChessBoardState.fromFEN(puzzle.fen)
                val orientation = if (board.sideToMove == ChessColor.WHITE) {
                    BoardOrientation.WHITE_AT_BOTTOM
                } else {
                    BoardOrientation.BLACK_AT_BOTTOM
                }

                _uiState.update {
                    it.copy(
                        status = PuzzleStatus.IN_PROGRESS,
                        currentPuzzle = puzzle,
                        boardState = board,
                        boardOrientation = orientation,
                        selectedSquare = null,
                        currentMoveIndex = 0,
                        lastRatingDelta = null,
                        feedbackMessage = "Your turn - find the best move!",
                        isAutoResponding = false,
                        startTimeMs = System.currentTimeMillis()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        status = PuzzleStatus.FAILED,
                        feedbackMessage = "No puzzles found."
                    )
                }
            }
        }
    }

    fun onSquareSelected(square: Square?) {
        _uiState.update { it.copy(selectedSquare = square) }
    }

    fun onMoveAttempted(from: Square, to: Square, promotion: PieceType?) {
        val state = _uiState.value
        if (state.status != PuzzleStatus.IN_PROGRESS || state.isAutoResponding) return
        val puzzle = state.currentPuzzle ?: return

        val board = state.boardState
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        val move = legalMoves.find {
            it.from == from && it.to == to && (promotion == null || it.promotion == promotion)
        } ?: return

        val moveUci = "${from.name.lowercase()}${to.name.lowercase()}${promotion?.name?.get(0)?.lowercaseChar() ?: ""}"
        val moveSan = SanUtils.moveToSan(board, move)
        val expectedMove = puzzle.solutionMoves[state.currentMoveIndex]

        val isMatch = matchesMove(expectedMove, moveUci, moveSan)

        if (isMatch) {
            val nextBoard = board.makeMove(move)
            val nextIndex = state.currentMoveIndex + 1

            if (nextIndex >= puzzle.solutionMoves.size) {
                // Puzzle solved!
                onPuzzleSolved(puzzle, nextBoard)
            } else {
                // User move correct, auto-play opponent reply!
                _uiState.update {
                    it.copy(
                        boardState = nextBoard,
                        currentMoveIndex = nextIndex,
                        selectedSquare = null,
                        feedbackMessage = "Correct! Opponent playing...",
                        isAutoResponding = true
                    )
                }
                viewModelScope.launch {
                    delay(300) // Brief delay for opponent reply animation
                    playOpponentReply(puzzle, nextBoard, nextIndex)
                }
            }
        } else {
            // Puzzle failed!
            onPuzzleFailed(puzzle)
        }
    }

    private suspend fun playOpponentReply(puzzle: Puzzle, board: ChessBoardState, opponentIndex: Int) {
        val expectedOpponentMove = puzzle.solutionMoves[opponentIndex]
        val legalMoves = MoveGenerator.generateLegalMoves(board)

        val opponentMove = legalMoves.find { move ->
            val uci = "${move.from.name.lowercase()}${move.to.name.lowercase()}${move.promotion?.name?.get(0)?.lowercaseChar() ?: ""}"
            val san = SanUtils.moveToSan(board, move)
            matchesMove(expectedOpponentMove, uci, san)
        }

        if (opponentMove != null) {
            val nextBoard = board.makeMove(opponentMove)
            val userNextIndex = opponentIndex + 1

            if (userNextIndex >= puzzle.solutionMoves.size) {
                onPuzzleSolved(puzzle, nextBoard)
            } else {
                _uiState.update {
                    it.copy(
                        boardState = nextBoard,
                        currentMoveIndex = userNextIndex,
                        selectedSquare = null,
                        feedbackMessage = "Your turn - find the next move!",
                        isAutoResponding = false
                    )
                }
            }
        } else {
            // Fallback if opponent move signature couldn't be parsed
            onPuzzleSolved(puzzle, board)
        }
    }

    private fun onPuzzleSolved(puzzle: Puzzle, finalBoard: ChessBoardState) {
        val timeTaken = System.currentTimeMillis() - _uiState.value.startTimeMs
        viewModelScope.launch {
            val oldRating = _uiState.value.userRating
            val newState = puzzleRepository.recordAttempt(puzzle, correct = true, timeTaken)
            val newRating = newState.rating.toInt()
            val delta = newRating - oldRating

            _uiState.update {
                it.copy(
                    boardState = finalBoard,
                    status = PuzzleStatus.CORRECT,
                    userRating = newRating,
                    lastRatingDelta = delta,
                    feedbackMessage = "Puzzle Solved! +$delta Rating",
                    isAutoResponding = false
                )
            }
        }
    }

    private fun onPuzzleFailed(puzzle: Puzzle) {
        val timeTaken = System.currentTimeMillis() - _uiState.value.startTimeMs
        viewModelScope.launch {
            val oldRating = _uiState.value.userRating
            val newState = puzzleRepository.recordAttempt(puzzle, correct = false, timeTaken)
            val newRating = newState.rating.toInt()
            val delta = newRating - oldRating

            _uiState.update {
                it.copy(
                    status = PuzzleStatus.FAILED,
                    userRating = newRating,
                    lastRatingDelta = delta,
                    feedbackMessage = "Incorrect move. Try again or view solution ($delta Rating)",
                    isAutoResponding = false
                )
            }
        }
    }

    fun showHint() {
        val state = _uiState.value
        val puzzle = state.currentPuzzle ?: return
        if (state.currentMoveIndex < puzzle.solutionMoves.size) {
            val expected = puzzle.solutionMoves[state.currentMoveIndex]
            _uiState.update {
                it.copy(hintText = "Hint: Look for $expected")
            }
        }
    }

    private fun matchesMove(expected: String, playedUci: String, playedSan: String): Boolean {
        val cleanExp = expected.replace(Regex("""[+#!?]*"""), "").lowercase()
        val cleanUci = playedUci.replace(Regex("""[+#!?]*"""), "").lowercase()
        val cleanSan = playedSan.replace(Regex("""[+#!?]*"""), "").lowercase()

        return cleanExp == cleanUci || cleanExp == cleanSan
    }
}
