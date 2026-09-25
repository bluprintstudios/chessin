package com.pro.chessin.domain.chess

/**
 * Utility for converting [Move] and [ChessBoardState] to Standard Algebraic Notation (SAN).
 */
object SanUtils {

    /**
     * Convert a [Move] played on [board] into a SAN string (e.g., "e4", "Nf3", "Bxc6+", "O-O").
     */
    fun moveToSan(board: ChessBoardState, move: Move): String {
        if (move.isCastling) {
            return if (move.castlingSide == Move.CastlingSide.KINGSIDE) "O-O" else "O-O-O"
        }

        val legalMoves = MoveGenerator.generateLegalMoves(board)
        val nextBoard = board.makeMove(move)
        val isCheck = nextBoard.isInCheck()
        val nextLegalMoves = MoveGenerator.generateLegalMoves(nextBoard)
        val isCheckmate = isCheck && nextLegalMoves.isEmpty()

        val suffix = when {
            isCheckmate -> "#"
            isCheck -> "+"
            else -> ""
        }

        val pieceType = move.piece.type
        val isCapture = move.isCapture

        if (pieceType == PieceType.PAWN) {
            val sb = StringBuilder()
            if (isCapture) {
                sb.append(move.from.name[0].lowercaseChar())
                sb.append('x')
            }
            sb.append(move.to.name.lowercase())
            if (move.isPromotion && move.promotion != null) {
                val promoChar = when (move.promotion) {
                    PieceType.QUEEN -> 'Q'
                    PieceType.ROOK -> 'R'
                    PieceType.BISHOP -> 'B'
                    PieceType.KNIGHT -> 'N'
                    else -> 'Q'
                }
                sb.append('=').append(promoChar)
            }
            sb.append(suffix)
            return sb.toString()
        }

        val pieceChar = when (pieceType) {
            PieceType.KNIGHT -> 'N'
            PieceType.BISHOP -> 'B'
            PieceType.ROOK -> 'R'
            PieceType.QUEEN -> 'Q'
            PieceType.KING -> 'K'
            else -> ' '
        }

        // Disambiguation: check other legal moves for same piece type to same 'to' square
        val ambiguousMoves = legalMoves.filter {
            it.piece.type == pieceType && it.to == move.to && it.from != move.from
        }

        val sb = StringBuilder()
        sb.append(pieceChar)

        if (ambiguousMoves.isNotEmpty()) {
            val sameFile = ambiguousMoves.any { it.from.file == move.from.file }
            val sameRank = ambiguousMoves.any { it.from.rank == move.from.rank }

            if (!sameFile) {
                sb.append(move.from.name[0].lowercaseChar())
            } else if (!sameRank) {
                sb.append(move.from.name[1])
            } else {
                sb.append(move.from.name.lowercase())
            }
        }

        if (isCapture) {
            sb.append('x')
        }

        sb.append(move.to.name.lowercase())
        sb.append(suffix)

        return sb.toString()
    }
}
