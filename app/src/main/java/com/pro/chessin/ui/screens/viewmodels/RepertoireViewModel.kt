package com.pro.chessin.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pro.chessin.data.RepertoireRepository
import com.pro.chessin.data.model.RepertoireNode
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Color
import com.pro.chessin.domain.chess.MoveGenerator
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.SanUtils
import com.pro.chessin.domain.chess.Square
import com.pro.chessin.ui.components.BoardOrientation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RepertoireMode { BROWSE, PRACTICE }
enum class RepertoireColor { WHITE, BLACK }

enum class PracticeFeedbackType { CORRECT, INCORRECT }

data class PracticeFeedback(
    val type: PracticeFeedbackType,
    val message: String
)

data class RepertoireUiState(
    val selectedColor: RepertoireColor = RepertoireColor.WHITE,
    val mode: RepertoireMode = RepertoireMode.BROWSE,
    val boardState: ChessBoardState = ChessBoardState.startPosition(),
    val boardOrientation: BoardOrientation = BoardOrientation.WHITE_AT_BOTTOM,
    val selectedSquare: Square? = null,
    val currentPath: List<RepertoireNode> = emptyList(),
    val currentNode: RepertoireNode? = null,
    val availableVariations: List<RepertoireNode> = emptyList(),
    val commentText: String = "",
    val isEditingComment: Boolean = false,
    val allNodesForColor: List<RepertoireNode> = emptyList(),

    // Practice mode state
    val dueNodes: List<RepertoireNode> = emptyList(),
    val currentPracticeIndex: Int = 0,
    val practicePath: List<RepertoireNode> = emptyList(),
    val currentPracticeStepIndex: Int = 0, // Current index in practicePath displayed/expected
    val practiceFeedback: PracticeFeedback? = null,
    val isPracticeComplete: Boolean = false
)

@HiltViewModel
class RepertoireViewModel @Inject constructor(
    private val repository: RepertoireRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepertoireUiState())
    val uiState: StateFlow<RepertoireUiState> = _uiState.asStateFlow()

    init {
        observeRepertoireNodes()
    }

    private fun observeRepertoireNodes() {
        repository.getNodesByColor(_uiState.value.selectedColor.name)
            .onEach { nodes ->
                _uiState.update { state ->
                    val currentNode = state.currentNode?.let { curr ->
                        nodes.find { it.id == curr.id }
                    }
                    val currentPath = state.currentPath.mapNotNull { pathNode ->
                        nodes.find { it.id == pathNode.id }
                    }
                    val availableVariations = nodes.filter { it.parentId == state.currentNode?.id }

                    state.copy(
                        allNodesForColor = nodes,
                        currentNode = currentNode,
                        currentPath = currentPath,
                        availableVariations = availableVariations,
                        commentText = currentNode?.comment ?: ""
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectColor(color: RepertoireColor) {
        val orientation = if (color == RepertoireColor.WHITE) {
            BoardOrientation.WHITE_AT_BOTTOM
        } else {
            BoardOrientation.BLACK_AT_BOTTOM
        }
        _uiState.update {
            RepertoireUiState(
                selectedColor = color,
                boardOrientation = orientation,
                mode = RepertoireMode.BROWSE
            )
        }
        observeRepertoireNodes()
    }

    fun setMode(mode: RepertoireMode) {
        if (mode == RepertoireMode.PRACTICE) {
            startPracticeMode()
        } else {
            _uiState.update { it.copy(mode = RepertoireMode.BROWSE, practiceFeedback = null) }
        }
    }

    fun onSquareSelected(square: Square?) {
        _uiState.update { it.copy(selectedSquare = square) }
    }

    fun onMoveAttempted(from: Square, to: Square, promotion: PieceType?) {
        if (_uiState.value.mode == RepertoireMode.PRACTICE) {
            onPracticeMoveAttempted(from, to, promotion)
            return
        }

        val board = _uiState.value.boardState
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        val move = legalMoves.find {
            it.from == from && it.to == to && (promotion == null || it.promotion == promotion)
        } ?: return

        val san = SanUtils.moveToSan(board, move)
        val nextBoard = board.makeMove(move)
        val fromStr = from.name.lowercase()
        val toStr = to.name.lowercase()
        val promoStr = promotion?.name?.lowercase()
        val currentColor = _uiState.value.selectedColor.name

        viewModelScope.launch {
            val parentId = _uiState.value.currentNode?.id
            val nodeId = repository.addMove(
                parentId = parentId,
                fenAfter = nextBoard.toFEN(),
                moveSan = san,
                fromSquare = fromStr,
                toSquare = toStr,
                promotionPiece = promoStr,
                colorToPlay = currentColor
            )

            val createdNode = repository.getNodeById(nodeId)
            _uiState.update { state ->
                val newPath = if (createdNode != null) state.currentPath + createdNode else state.currentPath
                val nextVariations = if (createdNode != null) {
                    state.allNodesForColor.filter { it.parentId == createdNode.id }
                } else emptyList()

                state.copy(
                    boardState = nextBoard,
                    selectedSquare = null,
                    currentNode = createdNode,
                    currentPath = newPath,
                    availableVariations = nextVariations,
                    commentText = createdNode?.comment ?: ""
                )
            }
        }
    }

    fun selectVariation(node: RepertoireNode) {
        val nextBoard = ChessBoardState.fromFEN(node.fen)
        _uiState.update { state ->
            val newPath = state.currentPath + node
            val nextVariations = state.allNodesForColor.filter { it.parentId == node.id }
            state.copy(
                boardState = nextBoard,
                selectedSquare = null,
                currentNode = node,
                currentPath = newPath,
                availableVariations = nextVariations,
                commentText = node.comment
            )
        }
    }

    fun selectBreadcrumb(index: Int) {
        val path = _uiState.value.currentPath
        if (index < 0) {
            resetToStart()
            return
        }
        if (index >= path.size) return

        val targetNode = path[index]
        val newPath = path.subList(0, index + 1)
        val nextBoard = ChessBoardState.fromFEN(targetNode.fen)
        _uiState.update { state ->
            val nextVariations = state.allNodesForColor.filter { it.parentId == targetNode.id }
            state.copy(
                boardState = nextBoard,
                selectedSquare = null,
                currentNode = targetNode,
                currentPath = newPath,
                availableVariations = nextVariations,
                commentText = targetNode.comment
            )
        }
    }

    fun resetToStart() {
        _uiState.update { state ->
            val rootVariations = state.allNodesForColor.filter { it.parentId == null }
            state.copy(
                boardState = ChessBoardState.startPosition(),
                selectedSquare = null,
                currentNode = null,
                currentPath = emptyList(),
                availableVariations = rootVariations,
                commentText = ""
            )
        }
    }

    fun stepBack() {
        val path = _uiState.value.currentPath
        if (path.isEmpty()) return
        if (path.size == 1) {
            resetToStart()
        } else {
            selectBreadcrumb(path.size - 2)
        }
    }

    fun updateComment(text: String) {
        val node = _uiState.value.currentNode ?: return
        _uiState.update { it.copy(commentText = text) }
        viewModelScope.launch {
            repository.updateComment(node.id, text)
        }
    }

    fun deleteCurrentNode() {
        val node = _uiState.value.currentNode ?: return
        viewModelScope.launch {
            repository.deleteNode(node.id)
            stepBack()
        }
    }

    // --- Practice Mode ---

    fun startPracticeMode() {
        viewModelScope.launch {
            val due = repository.getDueNodes(_uiState.value.selectedColor.name)
            if (due.isEmpty()) {
                _uiState.update {
                    it.copy(
                        mode = RepertoireMode.PRACTICE,
                        dueNodes = emptyList(),
                        isPracticeComplete = true,
                        practiceFeedback = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        mode = RepertoireMode.PRACTICE,
                        dueNodes = due,
                        currentPracticeIndex = 0,
                        isPracticeComplete = false,
                        practiceFeedback = null
                    )
                }
                loadPracticeLine(due[0])
            }
        }
    }

    private fun loadPracticeLine(targetNode: RepertoireNode) {
        val allNodesMap = _uiState.value.allNodesForColor.associateBy { it.id }
        val path = mutableListOf<RepertoireNode>()
        var curr: RepertoireNode? = targetNode
        while (curr != null) {
            path.add(0, curr)
            curr = curr.parentId?.let { allNodesMap[it] }
        }

        // Determine initial step
        // Find first step in path where player needs to play move
        _uiState.update { state ->
            state.copy(
                practicePath = path,
                currentPracticeStepIndex = 0,
                boardState = ChessBoardState.startPosition(),
                practiceFeedback = null
            )
        }
        advancePracticeLineToNextUserTurn()
    }

    private fun advancePracticeLineToNextUserTurn() {
        val state = _uiState.value
        val path = state.practicePath
        var stepIndex = state.currentPracticeStepIndex
        var board = if (stepIndex == 0) ChessBoardState.startPosition() else state.boardState

        val userIsWhite = state.selectedColor == RepertoireColor.WHITE

        while (stepIndex < path.size) {
            val node = path[stepIndex]
            val isUserMove = (board.sideToMove == Color.WHITE && userIsWhite) ||
                             (board.sideToMove == Color.BLACK && !userIsWhite)

            if (isUserMove) {
                // Wait for user move input
                _uiState.update {
                    it.copy(
                        boardState = board,
                        currentPracticeStepIndex = stepIndex
                    )
                }
                return
            } else {
                // Auto-advance opponent move
                board = ChessBoardState.fromFEN(node.fen)
                stepIndex++
            }
        }

        _uiState.update {
            it.copy(
                boardState = board,
                currentPracticeStepIndex = stepIndex
            )
        }
    }

    private fun onPracticeMoveAttempted(from: Square, to: Square, promotion: PieceType?) {
        val state = _uiState.value
        val path = state.practicePath
        val stepIndex = state.currentPracticeStepIndex

        if (stepIndex >= path.size) return

        val expectedNode = path[stepIndex]
        val fromStr = from.name.lowercase()
        val toStr = to.name.lowercase()
        val promoStr = promotion?.name?.lowercase()

        val isMatch = expectedNode.fromSquare == fromStr &&
                      expectedNode.toSquare == toStr &&
                      expectedNode.promotionPiece == promoStr

        if (isMatch) {
            val nextBoard = ChessBoardState.fromFEN(expectedNode.fen)
            viewModelScope.launch {
                repository.recordReviewResult(expectedNode.id, correct = true)

                val nextStep = stepIndex + 1
                if (nextStep >= path.size) {
                    // Completed this due line
                    advanceToNextDueLine()
                } else {
                    _uiState.update {
                        it.copy(
                            boardState = nextBoard,
                            currentPracticeStepIndex = nextStep,
                            practiceFeedback = PracticeFeedback(PracticeFeedbackType.CORRECT, "Correct!")
                        )
                    }
                    advancePracticeLineToNextUserTurn()
                }
            }
        } else {
            viewModelScope.launch {
                repository.recordReviewResult(expectedNode.id, correct = false)
                _uiState.update {
                    it.copy(
                        practiceFeedback = PracticeFeedback(
                            PracticeFeedbackType.INCORRECT,
                            "Incorrect. Expected move: ${expectedNode.moveSan}"
                        )
                    )
                }
            }
        }
    }

    private fun advanceToNextDueLine() {
        val state = _uiState.value
        val nextIndex = state.currentPracticeIndex + 1
        if (nextIndex >= state.dueNodes.size) {
            _uiState.update {
                it.copy(
                    isPracticeComplete = true,
                    practiceFeedback = PracticeFeedback(PracticeFeedbackType.CORRECT, "Practice complete for today!")
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentPracticeIndex = nextIndex,
                    practiceFeedback = PracticeFeedback(PracticeFeedbackType.CORRECT, "Line completed! Next move...")
                )
            }
            loadPracticeLine(state.dueNodes[nextIndex])
        }
    }
}
