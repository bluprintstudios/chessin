package com.pro.chessin.domain.chess

/**
 * Chess board state using bitboard representation.
 * Tracks piece placement, turn, castling rights, en passant, and move counters.
 */
data class ChessBoardState(
    // Piece bitboards (one for each piece type and color)
    internal val whitePawns: Long = 0L,
    internal val whiteKnights: Long = 0L,
    internal val whiteBishops: Long = 0L,
    internal val whiteRooks: Long = 0L,
    internal val whiteQueens: Long = 0L,
    internal val whiteKings: Long = 0L,
    internal val blackPawns: Long = 0L,
    internal val blackKnights: Long = 0L,
    internal val blackBishops: Long = 0L,
    internal val blackRooks: Long = 0L,
    internal val blackQueens: Long = 0L,
    internal val blackKings: Long = 0L,
    
    // Game state
    val sideToMove: Color = Color.WHITE,
    val castlingRights: CastlingRights = CastlingRights.ALL,
    val enPassantSquare: Square? = null,
    val halfMoveClock: Int = 0,  // For 50-move rule
    val fullMoveNumber: Int = 1,
    
    // Position history for repetition detection
    private val positionHistory: MutableList<Long> = mutableListOf(),
    private val currentHash: Long = 0L
) {
    // Combined bitboards for convenience
    val whitePieces: Long get() = whitePawns or whiteKnights or whiteBishops or whiteRooks or whiteQueens or whiteKings
    val blackPieces: Long get() = blackPawns or blackKnights or blackBishops or blackRooks or blackQueens or blackKings
    val allPieces: Long get() = whitePieces or blackPieces
    val emptySquares: Long get() = allPieces.inv()
    
    companion object {
        /**
         * Create the standard starting position.
         */
        fun startPosition(): ChessBoardState {
            return ChessBoardState(
                whitePawns = Bitboard.RANK_2,
                whiteKnights = Bitboard.squareBit(Square.B1) or Bitboard.squareBit(Square.G1),
                whiteBishops = Bitboard.squareBit(Square.C1) or Bitboard.squareBit(Square.F1),
                whiteRooks = Bitboard.squareBit(Square.A1) or Bitboard.squareBit(Square.H1),
                whiteQueens = Bitboard.squareBit(Square.D1),
                whiteKings = Bitboard.squareBit(Square.E1),
                blackPawns = Bitboard.RANK_7,
                blackKnights = Bitboard.squareBit(Square.B8) or Bitboard.squareBit(Square.G8),
                blackBishops = Bitboard.squareBit(Square.C8) or Bitboard.squareBit(Square.F8),
                blackRooks = Bitboard.squareBit(Square.A8) or Bitboard.squareBit(Square.H8),
                blackQueens = Bitboard.squareBit(Square.D8),
                blackKings = Bitboard.squareBit(Square.E8),
                sideToMove = Color.WHITE,
                castlingRights = CastlingRights.ALL,
                enPassantSquare = null,
                halfMoveClock = 0,
                fullMoveNumber = 1
            ).initializeHash()
        }
        
        /**
         * Create from FEN string.
         */
        fun fromFEN(fen: String): ChessBoardState {
            val parts = fen.split(" ")
            if (parts.size < 4) throw IllegalArgumentException("Invalid FEN: must have at least 4 fields")
            
            val positionPart = parts[0]
            val sideToMove = if (parts[1] == "w") Color.WHITE else Color.BLACK
            val castlingPart = parts[2]
            val enPassantPart = parts[3]
            val halfMoveClock = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            val fullMoveNumber = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1
            
            // Parse position
            val ranks = positionPart.split("/")
            if (ranks.size != 8) throw IllegalArgumentException("Invalid FEN: position must have 8 ranks")
            
            var whitePawns = 0L
            var whiteKnights = 0L
            var whiteBishops = 0L
            var whiteRooks = 0L
            var whiteQueens = 0L
            var whiteKings = 0L
            var blackPawns = 0L
            var blackKnights = 0L
            var blackBishops = 0L
            var blackRooks = 0L
            var blackQueens = 0L
            var blackKings = 0L
            
            for (rankIndex in 0..7) {
                val rank = ranks[rankIndex]
                var fileIndex = 0
                for (char in rank) {
                    when {
                        char.isDigit() -> fileIndex += char.digitToInt()
                        else -> {
                            val piece = Piece.fromChar(char) ?: throw IllegalArgumentException("Invalid piece character: $char")
                            val square = Square.fromCoordinates(fileIndex, 7 - rankIndex)!!
                            val bit = Bitboard.squareBit(square)
                            when (piece.color) {
                                Color.WHITE -> when (piece.type) {
                                    PieceType.PAWN -> whitePawns = whitePawns or bit
                                    PieceType.KNIGHT -> whiteKnights = whiteKnights or bit
                                    PieceType.BISHOP -> whiteBishops = whiteBishops or bit
                                    PieceType.ROOK -> whiteRooks = whiteRooks or bit
                                    PieceType.QUEEN -> whiteQueens = whiteQueens or bit
                                    PieceType.KING -> whiteKings = whiteKings or bit
                                }
                                Color.BLACK -> when (piece.type) {
                                    PieceType.PAWN -> blackPawns = blackPawns or bit
                                    PieceType.KNIGHT -> blackKnights = blackKnights or bit
                                    PieceType.BISHOP -> blackBishops = blackBishops or bit
                                    PieceType.ROOK -> blackRooks = blackRooks or bit
                                    PieceType.QUEEN -> blackQueens = blackQueens or bit
                                    PieceType.KING -> blackKings = blackKings or bit
                                }
                            }
                            fileIndex++
                        }
                    }
                }
            }
            
            // Parse castling rights
            val castlingRights = CastlingRights(
                whiteKingside = castlingPart.contains('K'),
                whiteQueenside = castlingPart.contains('Q'),
                blackKingside = castlingPart.contains('k'),
                blackQueenside = castlingPart.contains('q')
            )
            
            // Parse en passant square
            val enPassantSquare = if (enPassantPart == "-") null else Square.fromName(enPassantPart)
            
            return ChessBoardState(
                whitePawns = whitePawns,
                whiteKnights = whiteKnights,
                whiteBishops = whiteBishops,
                whiteRooks = whiteRooks,
                whiteQueens = whiteQueens,
                whiteKings = whiteKings,
                blackPawns = blackPawns,
                blackKnights = blackKnights,
                blackBishops = blackBishops,
                blackRooks = blackRooks,
                blackQueens = blackQueens,
                blackKings = blackKings,
                sideToMove = sideToMove,
                castlingRights = castlingRights,
                enPassantSquare = enPassantSquare,
                halfMoveClock = halfMoveClock,
                fullMoveNumber = fullMoveNumber
            ).initializeHash()
        }
    }
    
    /**
     * Get the piece at a given square.
     */
    fun getPiece(square: Square): Piece? {
        val bit = Bitboard.squareBit(square)
        return when {
            whitePawns and bit != 0L -> Piece(PieceType.PAWN, Color.WHITE)
            whiteKnights and bit != 0L -> Piece(PieceType.KNIGHT, Color.WHITE)
            whiteBishops and bit != 0L -> Piece(PieceType.BISHOP, Color.WHITE)
            whiteRooks and bit != 0L -> Piece(PieceType.ROOK, Color.WHITE)
            whiteQueens and bit != 0L -> Piece(PieceType.QUEEN, Color.WHITE)
            whiteKings and bit != 0L -> Piece(PieceType.KING, Color.WHITE)
            blackPawns and bit != 0L -> Piece(PieceType.PAWN, Color.BLACK)
            blackKnights and bit != 0L -> Piece(PieceType.KNIGHT, Color.BLACK)
            blackBishops and bit != 0L -> Piece(PieceType.BISHOP, Color.BLACK)
            blackRooks and bit != 0L -> Piece(PieceType.ROOK, Color.BLACK)
            blackQueens and bit != 0L -> Piece(PieceType.QUEEN, Color.BLACK)
            blackKings and bit != 0L -> Piece(PieceType.KING, Color.BLACK)
            else -> null
        }
    }
    
    /**
     * Get the bitboard for a specific piece type and color.
     */
    private fun getPieceBitboard(piece: Piece): Long {
        return when (piece.color) {
            Color.WHITE -> when (piece.type) {
                PieceType.PAWN -> whitePawns
                PieceType.KNIGHT -> whiteKnights
                PieceType.BISHOP -> whiteBishops
                PieceType.ROOK -> whiteRooks
                PieceType.QUEEN -> whiteQueens
                PieceType.KING -> whiteKings
            }
            Color.BLACK -> when (piece.type) {
                PieceType.PAWN -> blackPawns
                PieceType.KNIGHT -> blackKnights
                PieceType.BISHOP -> blackBishops
                PieceType.ROOK -> blackRooks
                PieceType.QUEEN -> blackQueens
                PieceType.KING -> blackKings
            }
        }
    }
    
    /**
     * Set a piece at a given square.
     */
    private fun setPiece(square: Square, piece: Piece?): ChessBoardState {
        val bit = Bitboard.squareBit(square)
        return when (piece?.color) {
            Color.WHITE -> when (piece.type) {
                PieceType.PAWN -> copy(whitePawns = whitePawns or bit)
                PieceType.KNIGHT -> copy(whiteKnights = whiteKnights or bit)
                PieceType.BISHOP -> copy(whiteBishops = whiteBishops or bit)
                PieceType.ROOK -> copy(whiteRooks = whiteRooks or bit)
                PieceType.QUEEN -> copy(whiteQueens = whiteQueens or bit)
                PieceType.KING -> copy(whiteKings = whiteKings or bit)
            }
            Color.BLACK -> when (piece.type) {
                PieceType.PAWN -> copy(blackPawns = blackPawns or bit)
                PieceType.KNIGHT -> copy(blackKnights = blackKnights or bit)
                PieceType.BISHOP -> copy(blackBishops = blackBishops or bit)
                PieceType.ROOK -> copy(blackRooks = blackRooks or bit)
                PieceType.QUEEN -> copy(blackQueens = blackQueens or bit)
                PieceType.KING -> copy(blackKings = blackKings or bit)
            }
            null -> clearSquare(square)
        }
    }
    
    /**
     * Clear a square (remove any piece).
     */
    private fun clearSquare(square: Square): ChessBoardState {
        val bit = Bitboard.squareBit(square)
        return copy(
            whitePawns = whitePawns and bit.inv(),
            whiteKnights = whiteKnights and bit.inv(),
            whiteBishops = whiteBishops and bit.inv(),
            whiteRooks = whiteRooks and bit.inv(),
            whiteQueens = whiteQueens and bit.inv(),
            whiteKings = whiteKings and bit.inv(),
            blackPawns = blackPawns and bit.inv(),
            blackKnights = blackKnights and bit.inv(),
            blackBishops = blackBishops and bit.inv(),
            blackRooks = blackRooks and bit.inv(),
            blackQueens = blackQueens and bit.inv(),
            blackKings = blackKings and bit.inv()
        )
    }
    
    /**
     * Move a piece from one square to another.
     */
    private fun movePiece(from: Square, to: Square, piece: Piece): ChessBoardState {
        return clearSquare(from).setPiece(to, piece)
    }
    
    /**
     * Initialize Zobrist hash for this position.
     */
    private fun initializeHash(): ChessBoardState {
        var hash = 0L
        
        // Hash all pieces
        for (square in 0..63) {
            val sq = Square(square)
            val piece = getPiece(sq)
            if (piece != null) {
                hash = ZobristHash.hashPiece(piece, sq, hash)
            }
        }
        
        // Hash castling rights
        hash = ZobristHash.hashCastling(castlingRights, hash)
        
        // Hash en passant square
        hash = ZobristHash.hashEnPassant(enPassantSquare, hash)
        
        // Hash side to move
        hash = ZobristHash.hashSideToMove(hash)
        
        return copy(currentHash = hash, positionHistory = mutableListOf(hash))
    }
    
    /**
     * Make a move on the board and return the new state.
     */
    fun makeMove(move: Move): ChessBoardState {
        var newBoard = this.clearSquare(move.from)
        // Handle capture - clear destination first, then place piece
        if (move.isCapture && !move.isEnPassant) {
            newBoard = newBoard.clearSquare(move.to)
        }
        newBoard = newBoard.setPiece(move.to, move.piece)
        
        // Handle en passant capture
        if (move.isEnPassant) {
            val capturedPawnSquare = move.to.offset(0, if (move.piece.color == Color.WHITE) -1 else 1)
            if (capturedPawnSquare != null) {
                newBoard = newBoard.clearSquare(capturedPawnSquare)
            }
        }
        
        // Handle castling (move the rook)
        if (move.isCastling) {
            val rookFrom = when (move.castlingSide) {
                Move.CastlingSide.KINGSIDE -> if (move.piece.color == Color.WHITE) Square.H1 else Square.H8
                Move.CastlingSide.QUEENSIDE -> if (move.piece.color == Color.WHITE) Square.A1 else Square.A8
                else -> Square.E1 // Should never happen
            }
            val rookTo = when (move.castlingSide) {
                Move.CastlingSide.KINGSIDE -> if (move.piece.color == Color.WHITE) Square.F1 else Square.F8
                Move.CastlingSide.QUEENSIDE -> if (move.piece.color == Color.WHITE) Square.D1 else Square.D8
                else -> Square.E1 // Should never happen
            }
            val rook = newBoard.getPiece(rookFrom)
            if (rook != null) {
                newBoard = newBoard.clearSquare(rookFrom).setPiece(rookTo, rook)
            }
        }
        
        // Handle promotion
        if (move.isPromotion) {
            newBoard = newBoard.clearSquare(move.to).setPiece(move.to, Piece(move.promotion!!, move.piece.color))
        }
        
        // Update castling rights
        var newCastlingRights = newBoard.castlingRights
        if (move.piece.type == PieceType.KING) {
            newCastlingRights = newCastlingRights.removeAll(move.piece.color)
        } else if (move.piece.type == PieceType.ROOK) {
            if (move.from == Square.A1 || move.from == Square.A8) {
                newCastlingRights = newCastlingRights.removeQueenside(move.piece.color)
            } else if (move.from == Square.H1 || move.from == Square.H8) {
                newCastlingRights = newCastlingRights.removeKingside(move.piece.color)
            }
        }
        newBoard = newBoard.copy(castlingRights = newCastlingRights)
        
        // Update en passant square
        newBoard = if (move.piece.type == PieceType.PAWN && 
                   kotlin.math.abs(move.to.rank - move.from.rank) == 2) {
            val epSquare = move.to.offset(0, if (move.piece.color == Color.WHITE) -1 else 1)
            newBoard.copy(enPassantSquare = epSquare)
        } else {
            newBoard.copy(enPassantSquare = null)
        }
        
        // Update side to move
        newBoard = newBoard.copy(sideToMove = newBoard.sideToMove.opposite())
        
        // Update half-move clock
        val resetClock = move.piece.type == PieceType.PAWN || move.isCapture
        newBoard = newBoard.copy(halfMoveClock = if (resetClock) 0 else newBoard.halfMoveClock + 1)
        
        // Update full-move number
        newBoard = if (newBoard.sideToMove == Color.WHITE) {
            newBoard.copy(fullMoveNumber = newBoard.fullMoveNumber + 1)
        } else {
            newBoard
        }
        
        // Update position history
        val newHistory = newBoard.positionHistory.toMutableList()
        val newHash = newBoard.recalculateHash()
        newHistory.add(newHash)
        
        return newBoard.copy(currentHash = newHash, positionHistory = newHistory)
    }
    
    /**
     * Recalculate Zobrist hash from scratch.
     */
    private fun recalculateHash(): Long {
        var hash = 0L
        
        // Hash all pieces
        for (square in 0..63) {
            val sq = Square(square)
            val piece = getPiece(sq)
            if (piece != null) {
                hash = ZobristHash.hashPiece(piece, sq, hash)
            }
        }
        
        // Hash castling rights
        hash = ZobristHash.hashCastling(castlingRights, hash)
        
        // Hash en passant square
        hash = ZobristHash.hashEnPassant(enPassantSquare, hash)
        
        // Hash side to move
        hash = ZobristHash.hashSideToMove(hash)
        
        return hash
    }
    
    /**
     * Export to FEN string.
     */
    fun toFEN(): String {
        val sb = StringBuilder()
        
        // Position
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val square = Square.fromCoordinates(file, rank)!!
                val piece = getPiece(square)
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(Piece.toChar(piece))
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (rank > 0) sb.append("/")
        }
        
        // Side to move
        sb.append(" ").append(if (sideToMove == Color.WHITE) "w" else "b")
        
        // Castling rights
        sb.append(" ")
        if (castlingRights == CastlingRights.NONE) {
            sb.append("-")
        } else {
            if (castlingRights.whiteKingside) sb.append("K")
            if (castlingRights.whiteQueenside) sb.append("Q")
            if (castlingRights.blackKingside) sb.append("k")
            if (castlingRights.blackQueenside) sb.append("q")
        }
        
        // En passant square
        sb.append(" ").append(enPassantSquare?.name ?: "-")
        
        // Half-move clock
        sb.append(" ").append(halfMoveClock)
        
        // Full-move number
        sb.append(" ").append(fullMoveNumber)
        
        return sb.toString()
    }
    
    /**
     * Check if the current position has occurred three times.
     */
    fun isThreefoldRepetition(): Boolean {
        return positionHistory.count { it == currentHash } >= 3
    }
    
    /**
     * Check if the 50-move rule applies (no pawn moves or captures in 50 half-moves).
     */
    fun isFiftyMoveRule(): Boolean {
        return halfMoveClock >= 100
    }
    
    /**
     * Check if the current side to move is in check.
     */
    fun isInCheck(): Boolean {
        return isSquareAttacked(findKing(sideToMove)!!, sideToMove.opposite())
    }
    
    /**
     * Check if a specific color is in check.
     */
    internal fun isInCheck(color: Color): Boolean {
        val kingSquare = findKing(color)
        if (kingSquare == null) {
            return false
        }
        return isSquareAttacked(kingSquare, color.opposite())
    }
    
    /**
     * Check if the current position is checkmate.
     */
    fun isCheckmate(): Boolean {
        return isInCheck() && MoveGenerator.generateLegalMoves(this).isEmpty()
    }
    
    /**
     * Check if the current position is stalemate.
     */
    fun isStalemate(): Boolean {
        return !isInCheck() && MoveGenerator.generateLegalMoves(this).isEmpty()
    }
    
    /**
     * Check if the game is over (checkmate, stalemate, 50-move rule, or threefold repetition).
     */
    fun isGameOver(): Boolean {
        return isCheckmate() || isStalemate() || isFiftyMoveRule() || isThreefoldRepetition()
    }
    
    /**
     * Find the king square for a given color.
     */
    internal fun findKing(color: Color): Square? {
        val kingBitboard = if (color == Color.WHITE) whiteKings else blackKings
        if (kingBitboard == 0L) return null
        return Square(Bitboard.lsb(kingBitboard))
    }
    
    /**
     * Check if a square is attacked by the given color.
     */
    internal fun isSquareAttacked(square: Square, byColor: Color): Boolean {
        val targetBit = Bitboard.squareBit(square)
        
        // Check pawn attacks
        val pawnAttacks = if (byColor == Color.WHITE) {
            // White pawns attack north-east and north-west (from their perspective)
            // So they must be on south-east or south-west of the target
            (Bitboard.shiftSouthEast(targetBit) or Bitboard.shiftSouthWest(targetBit))
        } else {
            // Black pawns attack south-east and south-west (from their perspective)
            // So they must be on north-east or north-west of the target
            (Bitboard.shiftNorthEast(targetBit) or Bitboard.shiftNorthWest(targetBit))
        }
        val pawns = if (byColor == Color.WHITE) whitePawns else blackPawns
        if (pawns and pawnAttacks != 0L) return true
        
        // Check knight attacks
        val knightMoves = getKnightAttacks(square)
        val knights = if (byColor == Color.WHITE) whiteKnights else blackKnights
        if (knights and knightMoves != 0L) return true
        
        // Check king attacks
        val kingMoves = getKingAttacks(square)
        val kings = if (byColor == Color.WHITE) whiteKings else blackKings
        if (kings and kingMoves != 0L) return true
        
        // Check rook/queen attacks (sliding) - iterate through each rook/queen
        val rooks = if (byColor == Color.WHITE) whiteRooks else blackRooks
        val queens = if (byColor == Color.WHITE) whiteQueens else blackQueens
        val rookAttackers = rooks or queens
        
        if (rookAttackers != 0L) {
            var attacker = rookAttackers
            while (attacker != 0L) {
                val attackerSquare = Square(Bitboard.lsb(attacker))
                attacker = attacker and (attacker - 1)
                val canAttack = canRookAttack(attackerSquare, square)
                if (canAttack) return true
            }
        }
        
        // Check bishop/queen attacks (sliding) - iterate through each bishop/queen
        val bishops = if (byColor == Color.WHITE) whiteBishops else blackBishops
        val bishopAttackers = bishops or queens
        
        if (bishopAttackers != 0L) {
            var attacker = bishopAttackers
            while (attacker != 0L) {
                val attackerSquare = Square(Bitboard.lsb(attacker))
                attacker = attacker and (attacker - 1)
                if (canBishopAttack(attackerSquare, square)) return true
            }
        }
        
        return false
    }
    
    internal fun canRookAttack(from: Square, to: Square): Boolean {
        // Check if on same rank or file
        if (from.file != to.file && from.rank != to.rank) return false
        
        val fromBit = Bitboard.squareBit(from)
        val toBit = Bitboard.squareBit(to)
        
        if (from.file == to.file) {
            // Same file - check squares between
            val direction = if (from.rank < to.rank) 1 else -1
            var current = from.rank + direction
            while (current != to.rank) {
                val square = Square.fromCoordinates(from.file, current)!!
                if (allPieces and Bitboard.squareBit(square) != 0L) return false
                current += direction
            }
        } else {
            // Same rank - check squares between
            val direction = if (from.file < to.file) 1 else -1
            var current = from.file + direction
            while (current != to.file) {
                val square = Square.fromCoordinates(current, from.rank)!!
                if (allPieces and Bitboard.squareBit(square) != 0L) return false
                current += direction
            }
        }
        return true
    }
    
    private fun canBishopAttack(from: Square, to: Square): Boolean {
        // Check if on same diagonal
        val fileDiff = kotlin.math.abs(from.file - to.file)
        val rankDiff = kotlin.math.abs(from.rank - to.rank)
        if (fileDiff != rankDiff) return false
        
        val fileDirection = if (from.file < to.file) 1 else -1
        val rankDirection = if (from.rank < to.rank) 1 else -1
        
        var currentFile = from.file + fileDirection
        var currentRank = from.rank + rankDirection
        
        while (currentFile != to.file || currentRank != to.rank) {
            val square = Square.fromCoordinates(currentFile, currentRank)!!
            if (allPieces and Bitboard.squareBit(square) != 0L) return false
            currentFile += fileDirection
            currentRank += rankDirection
        }
        
        return true
    }
    
    internal fun getKnightAttacks(square: Square): Long {
        val bit = Bitboard.squareBit(square)
        var attacks = 0L
        
        // Knight moves: offsets are (file, rank) pairs
        val offsets = listOf(1 to 2, 1 to -2, -1 to 2, -1 to -2, 2 to 1, 2 to -1, -2 to 1, -2 to -1)
        
        for ((fileOffset, rankOffset) in offsets) {
            val targetSquare = square.offset(fileOffset, rankOffset)
            if (targetSquare != null) {
                attacks = attacks or Bitboard.squareBit(targetSquare)
            }
        }
        
        return attacks
    }
    
    internal fun getKingAttacks(square: Square): Long {
        val bit = Bitboard.squareBit(square)
        return (Bitboard.shiftNorth(bit) or Bitboard.shiftSouth(bit) or
                Bitboard.shiftEast(bit) or Bitboard.shiftWest(bit) or
                Bitboard.shiftNorthEast(bit) or Bitboard.shiftNorthWest(bit) or
                Bitboard.shiftSouthEast(bit) or Bitboard.shiftSouthWest(bit))
    }
    
    internal fun getRookAttacks(square: Square): Long {
        val bit = Bitboard.squareBit(square)
        var attacks = 0L
        
        // North
        var temp = Bitboard.shiftNorth(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftNorth(temp)
        }
        
        // South
        temp = Bitboard.shiftSouth(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftSouth(temp)
        }
        
        // East
        temp = Bitboard.shiftEast(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftEast(temp)
        }
        
        // West
        temp = Bitboard.shiftWest(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftWest(temp)
        }
        
        return attacks
    }
    
    internal fun getBishopAttacks(square: Square): Long {
        val bit = Bitboard.squareBit(square)
        var attacks = 0L
        
        // North East
        var temp = Bitboard.shiftNorthEast(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftNorthEast(temp)
        }
        
        // North West
        temp = Bitboard.shiftNorthWest(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftNorthWest(temp)
        }
        
        // South East
        temp = Bitboard.shiftSouthEast(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftSouthEast(temp)
        }
        
        // South West
        temp = Bitboard.shiftSouthWest(bit)
        while (temp != 0L) {
            attacks = attacks or temp
            if (temp and allPieces != 0L) break
            temp = Bitboard.shiftSouthWest(temp)
        }
        
        return attacks
    }
}

