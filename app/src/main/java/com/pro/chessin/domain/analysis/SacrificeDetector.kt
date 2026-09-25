package com.pro.chessin.domain.analysis

import com.pro.chessin.domain.chess.Bitboard
import com.pro.chessin.domain.chess.ChessBoardState
import com.pro.chessin.domain.chess.Color
import com.pro.chessin.domain.chess.Move
import com.pro.chessin.domain.chess.PieceType
import com.pro.chessin.domain.chess.Square

/**
 * Detects "Brilliant" sacrifices - moves that give up material but lead to
 * a better or equal position, and were not forced.
 * 
 * A brilliant sacrifice requires:
 * 1. Material was given up on an opponent-attacked square or leaves another piece hanging
 * 2. Resulting evaluation is equal to or better than before the sacrifice from mover's perspective
 * 3. The move was not the only legal move available (involuntary sacrifices aren't brilliant)
 * 
 * This is isolated from MoveAnalyzer so it's independently testable and tunable.
 */
object SacrificeDetector {
    
    // Standard piece values for material comparison
    private val PIECE_VALUES = mapOf(
        PieceType.PAWN to 100,
        PieceType.KNIGHT to 320,
        PieceType.BISHOP to 330,
        PieceType.ROOK to 500,
        PieceType.QUEEN to 900,
        PieceType.KING to 0  // King is priceless, never counted in material
    )
    
    /**
     * Determines if a move is a brilliant sacrifice.
     * 
     * @param beforeState Board state before the move
     * @param move The move being evaluated
     * @param afterEval Evaluation after the move (from mover's perspective)
     * @param beforeEval Evaluation before the move (from mover's perspective)
     * @return true if the move is a brilliant sacrifice
     */
    fun isBrilliant(
        beforeState: ChessBoardState,
        move: Move,
        afterEval: EvalScore,
        beforeEval: EvalScore
    ): Boolean {
        // Check if material was given up
        val materialLost = calculateMaterialLoss(beforeState, move)
        if (materialLost <= 0) {
            return false  // No material given up
        }
        
        // Check if resulting evaluation is equal to or better from mover's perspective
        val moverIsWhite = beforeState.sideToMove == Color.WHITE
        val afterCp = afterEval.toNormalizedCp(perspective = moverIsWhite)
        val beforeCp = beforeEval.toNormalizedCp(perspective = moverIsWhite)
        if (afterCp < beforeCp) {
            return false  // Position got worse for mover
        }
        
        // Check if this was the only legal move (involuntary sacrifice)
        val legalMoves = com.pro.chessin.domain.chess.MoveGenerator.generateLegalMoves(beforeState)
        if (legalMoves.size <= 1) {
            return false  // Only one legal move, not a choice
        }
        
        return true
    }
    
    /**
     * Calculates material lost by a move.
     * A move only counts as a sacrifice if:
     * 1. The destination square is actually attacked by an opponent piece in the resulting position, AND
     *    the value given up exceeds the material captured (net loss on destination), OR
     * 2. The move leaves another friendly piece hanging (attacked by opponent in the resulting position
     *    when it was not attacked before or was defended by the moving piece).
     * 
     * @param beforeState Board state before the move
     * @param move The move being evaluated
     * @return Material lost (> 0 if material was sacrificed, 0 otherwise)
     */
    private fun calculateMaterialLoss(beforeState: ChessBoardState, move: Move): Int {
        val mover = beforeState.sideToMove
        val opponent = mover.opposite()
        val afterState = beforeState.makeMove(move)

        // 1. Destination square sacrifice evaluation
        val movingPiece = beforeState.getPiece(move.from)
        val movingValue = PIECE_VALUES[movingPiece?.type] ?: 0

        val capturedPiece = if (move.isEnPassant) {
            val epSquare = beforeState.enPassantSquare ?: return 0
            beforeState.getPiece(epSquare)
        } else {
            beforeState.getPiece(move.to)
        }

        val capturedValue = PIECE_VALUES[capturedPiece?.type] ?: 0
        val promotionValue = if (move.isPromotion) PIECE_VALUES[move.promotion] ?: 0 else 0

        val netDestinationLoss = (movingValue + promotionValue) - capturedValue

        // Destination loss ONLY counts if the destination square is actually attacked by opponent in afterState
        if (netDestinationLoss > 0 && afterState.isSquareAttacked(move.to, opponent)) {
            return netDestinationLoss
        }

        // 2. Hanging piece sacrifice evaluation (leaving another friendly piece attacked)
        val friendlyPieces = if (mover == Color.WHITE) afterState.whitePieces else afterState.blackPieces
        var bitboard = friendlyPieces
        while (bitboard != 0L) {
            val sqIndex = Bitboard.lsb(bitboard)
            val square = Square(sqIndex)
            bitboard = bitboard and (bitboard - 1)

            // Skip destination square (already evaluated above)
            if (square == move.to) continue

            val piece = afterState.getPiece(square) ?: continue
            val pieceVal = PIECE_VALUES[piece.type] ?: 0
            if (pieceVal == 0) continue // Skip King

            val attackedAfter = afterState.isSquareAttacked(square, opponent)
            if (attackedAfter) {
                val attackedBefore = beforeState.isSquareAttacked(square, opponent)
                if (!attackedBefore) {
                    // Piece became newly attacked after the move!
                    return pieceVal
                }
            }
        }

        return 0
    }
}
