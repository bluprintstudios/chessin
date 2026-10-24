package com.pro.chess_review.core.chess

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.game.Game
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.chesslib.pgn.GameLoader
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.ChessSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReplayState(
    val fen: String,
    val currentPly: Int,
    val totalPlies: Int,
    val isTerminal: Boolean,
    val nextMoveSan: String?,
    val nextMoveUci: String?,
    val sideToMove: ChessSide,
    val legalMoveCount: Int,
)

class GameReplay(val chessGame: ChessGame) {
    private var internalGame: Game? = null
    private val board = Board()
    private val moveList = mutableListOf<com.github.bhlangonijr.chesslib.move.Move>()
    
    private val _state = MutableStateFlow(
        ReplayState(
            fen = board.fen,
            currentPly = 0,
            totalPlies = 0,
            isTerminal = false,
            nextMoveSan = null,
            nextMoveUci = null,
            sideToMove = ChessSide.WHITE,
            legalMoveCount = board.legalMoves().size,
        )
    )
    val state: StateFlow<ReplayState> = _state.asStateFlow()

    init {
        loadInternalGame()
    }

    private fun loadInternalGame() {
        try {
            val iterator = chessGame.pgn.lineSequence().iterator()
            val game = GameLoader.loadNextGame(iterator)
            if (game != null) {
                internalGame = game
                moveList.clear()
                moveList.addAll(game.halfMoves)
                board.loadFromFen(Board().fen)
                board.history.clear()
                updateState(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun goToPly(plyIndex: Int) {
        val target = plyIndex.coerceIn(0, moveList.size)
        // A naive but safe implementation: always start from beginning
        // In a real optimized app, we'd step forward/backward from current state
        board.loadFromFen(Board().fen)
        board.history.clear()
        for (i in 0 until target) {
            board.doMove(moveList[i])
        }
        updateState(target)
    }

    fun nextMove() {
        val currentPly = getPlyCountFromBoard()
        if (currentPly < moveList.size) {
            goToPly(currentPly + 1)
        }
    }

    fun previousMove() {
        val currentPly = getPlyCountFromBoard()
        if (currentPly > 0) {
            goToPly(currentPly - 1)
        }
    }
    
    private fun getPlyCountFromBoard(): Int {
        return board.history.size
    }

    private fun updateState(ply: Int) {
        val isTerminal = board.isMated || board.isStaleMate || board.isDraw
        val nextSan = if (ply < moveList.size) moveList[ply].san else null
        val nextUci = if (ply < moveList.size) moveList[ply].toString() else null
        
        _state.value = ReplayState(
            fen = board.fen,
            currentPly = ply,
            totalPlies = moveList.size,
            isTerminal = isTerminal,
            nextMoveSan = nextSan,
            nextMoveUci = nextUci,
            sideToMove = if (board.sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE) {
                ChessSide.WHITE
            } else {
                ChessSide.BLACK
            },
            legalMoveCount = board.legalMoves().size,
        )
    }
}
