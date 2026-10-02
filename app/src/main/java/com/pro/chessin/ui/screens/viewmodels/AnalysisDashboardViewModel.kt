package com.pro.chessin.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pro.chessin.data.local.entity.MoveEntity
import com.pro.chessin.data.work.GameAnalyzerWorker
import com.pro.chessin.domain.analysis.AnalyzedMove
import com.pro.chessin.domain.analysis.ClassificationTier
import com.pro.chessin.domain.coach.AICoachContextBuilder
import com.pro.chessin.domain.coach.AiCoachProvider
import com.pro.chessin.domain.engine.EngineRepository
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.MoveGenerator
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.SanUtils
import com.pro.chessin.domain.chess.Square
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Analysis Dashboard screen.
 *
 * Manages a WorkManager [GameAnalyzerWorker] that performs batch per-move
 * analysis of a chess game. Observes work state via a unique-work-name flow
 * so the analysis correctly survives configuration changes and process death.
 *
 * Also integrates [AiCoachProvider] for incremental token streaming of AI Coach
 * explanations with loading states and retry affordances.
 */
@HiltViewModel
class AnalysisDashboardViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val engineRepository: EngineRepository,
    private val aiCoachProvider: AiCoachProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisDashboardUiState())
    val uiState: StateFlow<AnalysisDashboardUiState> = _uiState.asStateFlow()

    private val _coachChatState = MutableStateFlow<CoachChatState>(CoachChatState.Idle)
    val coachChatState: StateFlow<CoachChatState> = _coachChatState.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Square?>(null)
    val selectedSquare: StateFlow<Square?> = _selectedSquare.asStateFlow()

    private var currentWorkId: UUID? = null

    init {
        viewModelScope.launch {
            // Check if there's already an active (non-terminal) work request.
            // This handles process death recovery: if the app was killed mid-analysis,
            // we should observe the existing work, not start a new one.
            val existingInfos = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME)
                .first()
            val hasActiveWork = existingInfos.any {
                it.state == WorkInfo.State.ENQUEUED ||
                it.state == WorkInfo.State.RUNNING ||
                it.state == WorkInfo.State.BLOCKED
            }

            if (!hasActiveWork) {
                startAnalysis()
            }

            observeWork()
        }
    }

    private fun startAnalysis(depth: Int = 10) {
        val request = OneTimeWorkRequestBuilder<GameAnalyzerWorker>()
            .setInputData(workDataOf(
                GameAnalyzerWorker.KEY_PGN to DEFAULT_PGN,
                GameAnalyzerWorker.KEY_ANALYSIS_DEPTH to depth
            ))
            .addTag("game_analysis")
            .build()

        currentWorkId = request.id

        workManager.beginUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        ).enqueue()

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isCancelled = false,
                workId = request.id
            )
        }
    }

    private fun observeWork() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).collect { infos ->
                val info = infos.firstOrNull() ?: return@collect
                handleWorkInfo(info)
            }
        }
    }

    private fun handleWorkInfo(info: WorkInfo) {
        when (info.state) {
            WorkInfo.State.RUNNING -> {
                val current = info.progress.getInt(GameAnalyzerWorker.PROGRESS_CURRENT, 0)
                val total = info.progress.getInt(GameAnalyzerWorker.PROGRESS_TOTAL, 0)

                // During RUNNING, partial results are available in progress data
                val partialSerialized = info.progress.getString(GameAnalyzerWorker.KEY_RESULT)
                val moves = AnalyzedMove.deserialize(partialSerialized)

                _uiState.update { state ->
                    val combined = state.analyzedMoves.toMutableList()
                    for (i in moves.indices) {
                        if (i < combined.size) combined[i] = moves[i] else combined.add(moves[i])
                    }
                    state.copy(
                        isLoading = true,
                        isCancelled = false,
                        progressCurrent = current,
                        progressTotal = total,
                        analyzedMoves = combined,
                        selectedMoveIndex = if (state.selectedMoveIndex >= combined.size) combined.lastIndex.coerceAtLeast(0) else state.selectedMoveIndex,
                        error = null
                    )
                }
                currentWorkId = info.id
            }

            WorkInfo.State.SUCCEEDED -> {
                val serialized = info.outputData.getString(GameAnalyzerWorker.KEY_RESULT)
                val totalPly = info.outputData.getInt(GameAnalyzerWorker.KEY_TOTAL, 0)
                val moves = AnalyzedMove.deserialize(serialized)

                _uiState.update { state ->
                    val combined = state.analyzedMoves.toMutableList()
                    for (i in moves.indices) {
                        if (i < combined.size) combined[i] = moves[i] else combined.add(moves[i])
                    }
                    state.copy(
                        isLoading = false,
                        isCancelled = false,
                        progressCurrent = moves.size,
                        progressTotal = totalPly.coerceAtLeast(moves.size),
                        analyzedMoves = combined,
                        selectedMoveIndex = state.selectedMoveIndex.coerceIn(0, combined.lastIndex.coerceAtLeast(0)),
                        error = null
                    )
                }
                currentWorkId = info.id
            }

            WorkInfo.State.FAILED -> {
                val errorMsg = info.outputData.getString(GameAnalyzerWorker.KEY_ERROR)
                    ?: "Analysis failed. Check that the NNUE asset is available."
                val partialSerialized = info.outputData.getString(GameAnalyzerWorker.KEY_RESULT)
                val moves = AnalyzedMove.deserialize(partialSerialized)

                _uiState.update { state ->
                    val combined = state.analyzedMoves.toMutableList()
                    for (i in moves.indices) {
                        if (i < combined.size) combined[i] = moves[i] else combined.add(moves[i])
                    }
                    state.copy(
                        isLoading = false,
                        error = errorMsg,
                        analyzedMoves = combined,
                        selectedMoveIndex = state.selectedMoveIndex.coerceIn(0, combined.lastIndex.coerceAtLeast(0))
                    )
                }
                currentWorkId = info.id
            }

            WorkInfo.State.CANCELLED -> {
                val partialSerialized = info.outputData.getString(GameAnalyzerWorker.KEY_RESULT)
                val moves = AnalyzedMove.deserialize(partialSerialized)

                _uiState.update { state ->
                    val combined = state.analyzedMoves.toMutableList()
                    for (i in moves.indices) {
                        if (i < combined.size) combined[i] = moves[i] else combined.add(moves[i])
                    }
                    state.copy(
                        isLoading = false,
                        isCancelled = true,
                        error = null,
                        analyzedMoves = combined,
                        selectedMoveIndex = state.selectedMoveIndex.coerceIn(0, combined.lastIndex.coerceAtLeast(0))
                    )
                }
                currentWorkId = info.id
            }

            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                _uiState.update { state ->
                    state.copy(isLoading = true, isCancelled = false, error = null)
                }
            }
        }
    }

    /**
     * Cancel the current analysis.
     * This calls BOTH:
     * 1. WorkManager.cancelWorkById() — signals WorkManager to cancel the work
     * 2. EngineRepository.stopEngine() — immediately halts the Stockfish engine
     *    to stop CPU usage within seconds (not waiting for coroutine cancellation)
     */
    fun cancelAnalysis() {
        currentWorkId?.let { workManager.cancelWorkById(it) }
        // Also cancel by unique name as a fallback (handles process death recovery)
        workManager.cancelUniqueWork(WORK_NAME)

        // Immediately stop the engine to halt CPU usage
        viewModelScope.launch {
            try {
                engineRepository.stopEngine()
            } catch (_: Exception) {
                // Engine might already be stopped
            }
        }

        _uiState.update {
            it.copy(isLoading = false, isCancelled = true)
        }
    }

    /**
     * Select a move in the list to jump the board to that position.
     */
    fun selectMove(index: Int) {
        val moves = _uiState.value.analyzedMoves
        if (index in moves.indices) {
            if (_uiState.value.selectedMoveIndex != index) {
                _coachChatState.value = CoachChatState.Idle
            }
            _uiState.update { it.copy(selectedMoveIndex = index) }
        }
    }

    fun onSquareSelected(square: Square) {
        if (_selectedSquare.value == square) {
            _selectedSquare.value = null
        } else {
            _selectedSquare.value = square
        }
    }

    fun onMoveAttempted(
        from: Square,
        to: Square,
        promotion: PieceType?
    ) {
        val moves = _uiState.value.analyzedMoves
        val idx = _uiState.value.selectedMoveIndex
        val currentBoard = if (idx in moves.indices) {
            try {
                ChessBoardState.fromFEN(moves[idx].fenAfter) ?: ChessBoardState.startPosition()
            } catch (_: Exception) {
                ChessBoardState.startPosition()
            }
        } else {
            ChessBoardState.startPosition()
        }

        val legalMoves = MoveGenerator.generateLegalMoves(currentBoard)
        val move = legalMoves.find { it.from == from && it.to == to && (promotion == null || it.promotion == promotion) }
        if (move != null) {
            _coachChatState.value = CoachChatState.Idle
            val san = SanUtils.moveToSan(currentBoard, move)
            val newBoard = currentBoard.makeMove(move)
            val newFen = newBoard.toFEN()
            val newIndex = moves.size

            val newAnalyzedMove = AnalyzedMove(
                moveIndex = newIndex,
                san = san,
                fenAfter = newFen,
                evalAfterCp = 0,
                deltaCp = 0,
                bestEvalCp = 0,
                tier = ClassificationTier.GOOD
            )

            _uiState.update { state ->
                val updatedMoves = state.analyzedMoves + newAnalyzedMove
                state.copy(
                    analyzedMoves = updatedMoves,
                    selectedMoveIndex = updatedMoves.lastIndex
                )
            }
            triggerWorkerForCurrentMoves()
        }
        _selectedSquare.value = null
    }

    private fun triggerWorkerForCurrentMoves() {
        _uiState.update { it.copy(isLoading = true, error = null, isCancelled = false) }
        val pgnText = _uiState.value.analyzedMoves.joinToString(" ") { it.san }
        val request = OneTimeWorkRequestBuilder<GameAnalyzerWorker>()
            .setInputData(workDataOf(
                GameAnalyzerWorker.KEY_PGN to pgnText,
                GameAnalyzerWorker.KEY_ANALYSIS_DEPTH to 10
            ))
            .addTag("game_analysis")
            .build()
            
        currentWorkId = request.id
        workManager.beginUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        ).enqueue()
    }

    fun firstMove() {
        val moves = _uiState.value.analyzedMoves
        if (moves.isNotEmpty()) {
            if (_uiState.value.selectedMoveIndex != 0) {
                _coachChatState.value = CoachChatState.Idle
            }
            _uiState.update { it.copy(selectedMoveIndex = 0) }
        }
    }

    fun prevMove() {
        val moves = _uiState.value.analyzedMoves
        if (moves.isNotEmpty()) {
            val newIdx = (_uiState.value.selectedMoveIndex - 1).coerceAtLeast(0)
            if (_uiState.value.selectedMoveIndex != newIdx) {
                _coachChatState.value = CoachChatState.Idle
            }
            _uiState.update { state ->
                state.copy(selectedMoveIndex = newIdx)
            }
        }
    }

    fun nextMove() {
        val moves = _uiState.value.analyzedMoves
        if (moves.isNotEmpty()) {
            val newIdx = (_uiState.value.selectedMoveIndex + 1).coerceAtMost(moves.lastIndex)
            if (_uiState.value.selectedMoveIndex != newIdx) {
                _coachChatState.value = CoachChatState.Idle
            }
            _uiState.update { state ->
                state.copy(selectedMoveIndex = newIdx)
            }
        }
    }

    fun lastMove() {
        val moves = _uiState.value.analyzedMoves
        if (moves.isNotEmpty()) {
            if (_uiState.value.selectedMoveIndex != moves.lastIndex) {
                _coachChatState.value = CoachChatState.Idle
            }
            _uiState.update { state ->
                state.copy(selectedMoveIndex = moves.lastIndex)
            }
        }
    }

    fun importPgn(pgnText: String) {
        val games = com.pro.chessin.domain.chess.PgnParser.parse(pgnText)
        val game = games.firstOrNull()

        if (game != null && game.moves.isNotEmpty()) {
            _coachChatState.value = CoachChatState.Idle
            var currentBoard = ChessBoardState.startPosition()
            val parsedMoves = game.moves.mapIndexed { index, parsedMove ->
                val newBoard = currentBoard.makeMove(parsedMove.move)
                val fenAfter = newBoard.toFEN()
                currentBoard = newBoard
                AnalyzedMove(
                    san = parsedMove.san,
                    fenAfter = fenAfter,
                    tier = ClassificationTier.GOOD,
                    evalAfterCp = 0,
                    deltaCp = 0,
                    bestEvalCp = 0,
                    moveIndex = index
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    isCancelled = false,
                    analyzedMoves = parsedMoves,
                    selectedMoveIndex = 0
                )
            }

            val request = OneTimeWorkRequestBuilder<GameAnalyzerWorker>()
                .setInputData(workDataOf(
                    GameAnalyzerWorker.KEY_PGN to pgnText,
                    GameAnalyzerWorker.KEY_ANALYSIS_DEPTH to 10
                ))
                .addTag("game_analysis")
                .build()

            currentWorkId = request.id

            workManager.beginUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            ).enqueue()
        } else {
            _uiState.update {
                it.copy(
                    error = "Failed to parse PGN. Please check PGN format."
                )
            }
        }
    }

    /**
     * Restart analysis from scratch.
     */
    fun retryAnalysis() {
        _coachChatState.value = CoachChatState.Idle
        startAnalysis()
    }

    /**
     * Returns the FEN string of the position after the currently selected move,
     * or the starting position if no moves are available.
     */
    val currentPositionFen: String
        get() {
            val moves = _uiState.value.analyzedMoves
            val idx = _uiState.value.selectedMoveIndex
            return if (idx in moves.indices) {
                moves[idx].fenAfter
            } else {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            }
        }

    /**
     * Returns the evaluation (cp, white's perspective) of the currently
     * selected move, or 0 if no move is selected.
     */
    val currentEvalCp: Int
        get() {
            val moves = _uiState.value.analyzedMoves
            val idx = _uiState.value.selectedMoveIndex
            return if (idx in moves.indices) moves[idx].evalAfterCp else 0
        }

    /**
     * Request an AI Coach explanation stream for the currently selected move.
     */
    fun requestCoachExplanation(userQuestion: String = "Explain this move and position") {
        val moves = _uiState.value.analyzedMoves
        val idx = _uiState.value.selectedMoveIndex
        val move = moves.getOrNull(idx)

        val priorEntities = if (idx in moves.indices) {
            moves.take(idx).map { it.toMoveEntity() }
        } else {
            emptyList()
        }

        val targetEntity = move?.toMoveEntity() ?: MoveEntity(
            id = 0L,
            gameId = 1L,
            ply = 0,
            san = "Start",
            fenAfter = currentPositionFen,
            evalCp = currentEvalCp,
            classificationTier = ClassificationTier.BOOK.name
        )

        val dummyGame = com.pro.chessin.data.local.entity.GameEntity(
            id = 1L,
            whitePlayer = "White",
            blackPlayer = "Black",
            date = System.currentTimeMillis(),
            result = "*",
            event = "Local Analysis",
            pgnRaw = "",
            createdAt = System.currentTimeMillis()
        )

        val context = AICoachContextBuilder.build(
            move = targetEntity,
            game = dummyGame,
            priorPly = priorEntities,
            deltaCp = move?.deltaCp ?: 0
        )

        viewModelScope.launch {
            _coachChatState.value = CoachChatState.Loading
            var textAcc = ""

            try {
                aiCoachProvider.streamResponse(context, userQuestion)
                    .catch { e ->
                        _coachChatState.value = CoachChatState.Error(
                            message = e.message ?: "Failed to connect to AI Coach",
                            lastQuestion = userQuestion
                        )
                    }
                    .collect { token ->
                        textAcc += token
                        _coachChatState.value = CoachChatState.Streaming(textAcc)
                    }
            } catch (e: Exception) {
                _coachChatState.value = CoachChatState.Error(
                    message = e.message ?: "Streaming interrupted",
                    lastQuestion = userQuestion
                )
            }
        }
    }

    private fun AnalyzedMove.toMoveEntity(): MoveEntity {
        return MoveEntity(
            id = (moveIndex + 1).toLong(),
            gameId = 1L,
            ply = moveIndex,
            san = san,
            fenAfter = fenAfter,
            evalCp = evalAfterCp,
            classificationTier = tier.name,
            alternativeLinesJson = alternativeLinesJson
        )
    }

    companion object {
        const val WORK_NAME = "game_analysis"

        val DEFAULT_PGN = """
            [Event "Italian Game"]
            [Site "?"]
            [Date "2024.01.01"]
            [Round "?"]
            [White "White"]
            [Black "Black"]
            [Result "1-0"]

            1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. c3 Nf6 5. d4 exd4 6. cxd4 Bb4+ 1-0
        """.trimIndent()
    }
}

/**
 * State of the AI Coach Chat stream.
 */
sealed class CoachChatState {
    object Idle : CoachChatState()
    object Loading : CoachChatState()
    data class Streaming(val accumulatedText: String) : CoachChatState()
    data class Error(val message: String, val lastQuestion: String) : CoachChatState()
}

/**
 * Immutable UI state for the Analysis Dashboard screen.
 */
data class AnalysisDashboardUiState(
    val isLoading: Boolean = false,
    val analyzedMoves: List<AnalyzedMove> = emptyList(),
    val selectedMoveIndex: Int = 0,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val error: String? = null,
    val isCancelled: Boolean = false,
    val workId: UUID? = null
)
