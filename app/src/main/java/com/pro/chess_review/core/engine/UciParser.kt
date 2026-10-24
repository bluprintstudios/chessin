package com.pro.chess_review.core.engine

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move

object UciParser {

    /**
     * Translates a space-separated UCI move string (e.g. "e2e4 e7e5") to SAN.
     * Note: Stockfish evaluation is always relative to the side to move.
     * We should normalize it to White's perspective based on the FEN.
     */
    fun parseUciLine(
        fen: String,
        uciTokens: List<String>,
        cpString: String?,
        mateString: String?,
        depth: Int
    ): EngineLine {
        val board = Board()
        board.loadFromFen(fen)
        val isWhiteToMove = board.sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE

        val uciMoves = mutableListOf<String>()
        val moveList = com.github.bhlangonijr.chesslib.move.MoveList(fen)

        for (uci in uciTokens) {
            try {
                // Find matching legal move. chesslib move strings are usually just coordinates "e2e4", "e7e8q"
                val legalMoves = board.legalMoves()
                val actualMove = legalMoves.find { it.toString() == uci }
                
                if (actualMove != null) {
                    uciMoves.add(actualMove.toString())
                    moveList.add(actualMove)
                    board.doMove(actualMove)
                } else {
                    break
                }
            } catch (e: Exception) {
                break
            }
        }
        
        val sanList = try {
            moveList.toSanArray().toList()
        } catch (e: Exception) {
            emptyList()
        }

        // Normalize evaluations to White's perspective.
        // UCI sends cp/mate from the perspective of the side to move.
        val cp = cpString?.toIntOrNull()?.let { if (isWhiteToMove) it else -it }
        val mateIn = mateString?.toIntOrNull()?.let { if (isWhiteToMove) it else -it }

        return EngineLine(
            uciMoves = uciMoves,
            sanMoves = sanList,
            centipawns = cp,
            mateIn = mateIn,
            depth = depth
        )
    }
}
