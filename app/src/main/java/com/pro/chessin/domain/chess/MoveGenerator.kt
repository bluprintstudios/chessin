package com.pro.chessin.domain.chess

/**
 * Generates legal moves for a given board position.
 * Takes into account pins, checks, castling rights, and en passant.
 */
object MoveGenerator {
    
    /**
     * Generate all pseudo-legal moves (moves that follow piece movement rules but may leave king in check).
     */
    fun generatePseudoLegalMoves(board: ChessBoardState): List<Move> {
        val moves = mutableListOf<Move>()
        val side = board.sideToMove
        val opponent = side.opposite()
        
        // Generate pawn moves
        generatePawnMoves(board, side, moves)
        
        // Generate knight moves
        generateKnightMoves(board, side, moves)
        
        // Generate bishop moves
        generateBishopMoves(board, side, moves)
        
        // Generate rook moves
        generateRookMoves(board, side, moves)
        
        // Generate queen moves
        generateQueenMoves(board, side, moves)
        
        // Generate king moves
        generateKingMoves(board, side, moves)
        
        // Generate castling moves
        generateCastlingMoves(board, side, moves)
        
        return moves
    }
    
    /**
     * Generate all legal moves (moves that don't leave the king in check).
     */
    fun generateLegalMoves(board: ChessBoardState): List<Move> {
        val pseudoLegalMoves = generatePseudoLegalMoves(board)
        val side = board.sideToMove
        
        return pseudoLegalMoves.filter { move ->
            val newBoard = board.makeMove(move)
            // After making the move, sideToMove flips to opponent
            // We need to check if the side that just moved (original side) is in check
            // The new board has sideToMove = opponent, so we check if the opponent of the new side is in check
            val inCheck = newBoard.isInCheck(newBoard.sideToMove.opposite())
            !inCheck
        }
    }
    
    private fun generatePawnMoves(board: ChessBoardState, side: Color, moves: MutableList<Move>) {
        val pawns = if (side == Color.WHITE) board.whitePawns else board.blackPawns
        val opponentPieces = if (side == Color.WHITE) board.blackPieces else board.whitePieces
        val empty = board.emptySquares
        
        val direction = if (side == Color.WHITE) 1 else -1
        val startRank = if (side == Color.WHITE) 1 else 6
        val promotionRank = if (side == Color.WHITE) 7 else 0
        
        var pawnBitboard = pawns
        while (pawnBitboard != 0L) {
            val fromSquare = Square(Bitboard.lsb(pawnBitboard))
            
            // Single pawn push
            val toSquare = fromSquare.offset(0, direction)
            if (toSquare != null) {
                val toBit = Bitboard.squareBit(toSquare)
                if (toBit and empty != 0L) {
                    if (toSquare.rank == promotionRank) {
                        // Promotion
                        moves.add(Move.promotion(fromSquare, toSquare, Piece(PieceType.PAWN, side), PieceType.QUEEN))
                        moves.add(Move.promotion(fromSquare, toSquare, Piece(PieceType.PAWN, side), PieceType.ROOK))
                        moves.add(Move.promotion(fromSquare, toSquare, Piece(PieceType.PAWN, side), PieceType.BISHOP))
                        moves.add(Move.promotion(fromSquare, toSquare, Piece(PieceType.PAWN, side), PieceType.KNIGHT))
                    } else {
                        moves.add(Move.simple(fromSquare, toSquare, Piece(PieceType.PAWN, side)))
                        
                        // Double pawn push from starting rank
                        if (fromSquare.rank == startRank) {
                            val doubleToSquare = toSquare.offset(0, direction)
                            if (doubleToSquare != null) {
                                val doubleToBit = Bitboard.squareBit(doubleToSquare)
                                if (doubleToBit and empty != 0L) {
                                    moves.add(Move.simple(fromSquare, doubleToSquare, Piece(PieceType.PAWN, side)))
                                }
                            }
                        }
                    }
                }
            }
            
            // Pawn captures
            val captureOffsets = listOf(-1 to direction, 1 to direction)
            for ((fileOffset, rankOffset) in captureOffsets) {
                val captureSquare = fromSquare.offset(fileOffset, rankOffset)
                if (captureSquare != null) {
                    val captureBit = Bitboard.squareBit(captureSquare)
                    if (captureBit and opponentPieces != 0L) {
                        if (captureSquare.rank == promotionRank) {
                            moves.add(Move.promotionCapture(fromSquare, captureSquare, Piece(PieceType.PAWN, side), PieceType.QUEEN))
                            moves.add(Move.promotionCapture(fromSquare, captureSquare, Piece(PieceType.PAWN, side), PieceType.ROOK))
                            moves.add(Move.promotionCapture(fromSquare, captureSquare, Piece(PieceType.PAWN, side), PieceType.BISHOP))
                            moves.add(Move.promotionCapture(fromSquare, captureSquare, Piece(PieceType.PAWN, side), PieceType.KNIGHT))
                        } else {
                            moves.add(Move.capture(fromSquare, captureSquare, Piece(PieceType.PAWN, side)))
                        }
                    }
                }
            }
            
            // En passant capture
            if (board.enPassantSquare != null) {
                val epSquare = board.enPassantSquare!!
                // En passant can only be captured by pawns on the 5th rank (white) or 4th rank (black)
                val correctRank = if (side == Color.WHITE) 4 else 3
                if (fromSquare.rank == correctRank) {
                    for ((fileOffset, rankOffset) in captureOffsets) {
                        val epCaptureSquare = fromSquare.offset(fileOffset, rankOffset)
                        if (epCaptureSquare == epSquare) {
                            moves.add(Move.enPassant(fromSquare, epSquare, Piece(PieceType.PAWN, side)))
                        }
                    }
                }
            }
            
            pawnBitboard = Bitboard.popLsb(pawnBitboard)
        }
    }
    
    private fun generateKnightMoves(board: ChessBoardState, side: Color, moves: MutableList<Move>) {
        val knights = if (side == Color.WHITE) board.whiteKnights else board.blackKnights
        val opponentPieces = if (side == Color.WHITE) board.blackPieces else board.whitePieces
        val ownPieces = if (side == Color.WHITE) board.whitePieces else board.blackPieces
        
        var knightBitboard = knights
        while (knightBitboard != 0L) {
            val fromSquare = Square(Bitboard.lsb(knightBitboard))
            
            val attacks = board.getKnightAttacks(fromSquare)
            var destinations = attacks and ownPieces.inv()
            
            while (destinations != 0L) {
                val toSquare = Square(Bitboard.lsb(destinations))
                val toBit = Bitboard.squareBit(toSquare)
                val isCapture = toBit and opponentPieces != 0L
                
                if (isCapture) {
                    moves.add(Move.capture(fromSquare, toSquare, Piece(PieceType.KNIGHT, side)))
                } else {
                    moves.add(Move.simple(fromSquare, toSquare, Piece(PieceType.KNIGHT, side)))
                }
                
                destinations = Bitboard.popLsb(destinations)
            }
            
            knightBitboard = Bitboard.popLsb(knightBitboard)
        }
    }
    
    private fun generateBishopMoves(board: ChessBoardState, side: Color, moves: MutableList<Move>) {
        val bishops = if (side == Color.WHITE) board.whiteBishops else board.blackBishops
        val opponentPieces = if (side == Color.WHITE) board.blackPieces else board.whitePieces
        val ownPieces = if (side == Color.WHITE) board.whitePieces else board.blackPieces
        
        var bishopBitboard = bishops
        while (bishopBitboard != 0L) {
            val fromSquare = Square(Bitboard.lsb(bishopBitboard))
            
            val attacks = board.getBishopAttacks(fromSquare)
            var destinations = attacks and ownPieces.inv()
            
            while (destinations != 0L) {
                val toSquare = Square(Bitboard.lsb(destinations))
                val toBit = Bitboard.squareBit(toSquare)
                val isCapture = toBit and opponentPieces != 0L
                
                if (isCapture) {
                    moves.add(Move.capture(fromSquare, toSquare, Piece(PieceType.BISHOP, side)))
                } else {
                    moves.add(Move.simple(fromSquare, toSquare, Piece(PieceType.BISHOP, side)))
                }
                
                destinations = Bitboard.popLsb(destinations)
            }
            
            bishopBitboard = Bitboard.popLsb(bishopBitboard)
        }
    }
    
    private fun generateRookMoves(board: ChessBoardState, side: Color, moves: MutableList<Move>) {
        val rooks = if (side == Color.WHITE) board.whiteRooks else board.blackRooks
        val opponentPieces = if (side == Color.WHITE) board.blackPieces else board.whitePieces
        val ownPieces = if (side == Color.WHITE) board.whitePieces else board.blackPieces
        
        var rookBitboard = rooks
        while (rookBitboard != 0L) {
            val fromSquare = Square(Bitboard.lsb(rookBitboard))
            
            val attacks = board.getRookAttacks(fromSquare)
            var destinations = attacks and ownPieces.inv()
            
            while (destinations != 0L) {
                val toSquare = Square(Bitboard.lsb(destinations))
                val toBit = Bitboard.squareBit(toSquare)
                val isCapture = toBit and opponentPieces != 0L
                
                if (isCapture) {
                    moves.add(Move.capture(fromSquare, toSquare, Piece(PieceType.ROOK, side)))
                } else {
                    moves.add(Move.simple(fromSquare, toSquare, Piece(PieceType.ROOK, side)))
                }
                
                destinations = Bitboard.popLsb(destinations)
            }
            
            rookBitboard = Bitboard.popLsb(rookBitboard)
        }
    }
    
    private fun generateQueenMoves(board: ChessBoardState, side: Color, moves: MutableList<Move>) {
        val queens = if (side == Color.WHITE) board.whiteQueens else board.blackQueens
        val opponentPieces = if (side == Color.WHITE) board.blackPieces else board.whitePieces
        val ownPieces = if (side == Color.WHITE) board.whitePieces else board.blackPieces
        
        var queenBitboard = queens
        while (queenBitboard != 0L) {
            val fromSquare = Square(Bitboard.lsb(queenBitboard))
            
            val attacks = board.getRookAttacks(fromSquare) or board.getBishopAttacks(fromSquare)
            var destinations = attacks and ownPieces.inv()
            
            while (destinations != 0L) {
                val toSquare = Square(Bitboard.lsb(destinations))
                val toBit = Bitboard.squareBit(toSquare)
                val isCapture = toBit and opponentPieces != 0L
                
                if (isCapture) {
                    moves.add(Move.capture(fromSquare, toSquare, Piece(PieceType.QUEEN, side)))
                } else {
                    moves.add(Move.simple(fromSquare, toSquare, Piece(PieceType.QUEEN, side)))
                }
                
                destinations = Bitboard.popLsb(destinations)
            }
            
            queenBitboard = Bitboard.popLsb(queenBitboard)
        }
    }
    
    private fun generateKingMoves(board: ChessBoardState, side: Color, moves: MutableList<Move>) {
        val king = if (side == Color.WHITE) board.whiteKings else board.blackKings
        val opponentPieces = if (side == Color.WHITE) board.blackPieces else board.whitePieces
        val ownPieces = if (side == Color.WHITE) board.whitePieces else board.blackPieces
        
        if (king == 0L) return
        
        val fromSquare = Square(Bitboard.lsb(king))
        val attacks = board.getKingAttacks(fromSquare)
        var destinations = attacks and ownPieces.inv()
        
        while (destinations != 0L) {
            val toSquare = Square(Bitboard.lsb(destinations))
            val toBit = Bitboard.squareBit(toSquare)
            val isCapture = toBit and opponentPieces != 0L
            
            if (isCapture) {
                moves.add(Move.capture(fromSquare, toSquare, Piece(PieceType.KING, side)))
            } else {
                moves.add(Move.simple(fromSquare, toSquare, Piece(PieceType.KING, side)))
            }
            
            destinations = Bitboard.popLsb(destinations)
        }
    }
    
    private fun generateCastlingMoves(board: ChessBoardState, side: Color, moves: MutableList<Move>) {
        if (board.isInCheck()) return // Can't castle while in check
        
        val opponent = side.opposite()
        val kingSquare = if (side == Color.WHITE) Square.E1 else Square.E8
        val king = board.getPiece(kingSquare)
        if (king == null || king.type != PieceType.KING || king.color != side) return
        
        // Kingside castling
        if (board.castlingRights.hasKingsideRights(side)) {
            val rookSquare = if (side == Color.WHITE) Square.H1 else Square.H8
            val rook = board.getPiece(rookSquare)
            if (rook != null && rook.type == PieceType.ROOK && rook.color == side) {
                // Check if squares between king and rook are empty
                val pathSquares = if (side == Color.WHITE) {
                    listOf(Square.F1, Square.G1)
                } else {
                    listOf(Square.F8, Square.G8)
                }
                val pathEmpty = pathSquares.all { board.getPiece(it) == null }
                
                // Check if king passes through check - check if pass-through square is attacked
                val kingPassSquare = if (side == Color.WHITE) Square.F1 else Square.F8
                val kingPassesThroughCheck = board.isSquareAttacked(kingPassSquare, opponent)
                
                if (pathEmpty && !kingPassesThroughCheck) {
                    moves.add(Move.castling(kingSquare, if (side == Color.WHITE) Square.G1 else Square.G8, king, Move.CastlingSide.KINGSIDE))
                }
            }
        }
        
        // Queenside castling
        if (board.castlingRights.hasQueensideRights(side)) {
            val rookSquare = if (side == Color.WHITE) Square.A1 else Square.A8
            val rook = board.getPiece(rookSquare)
            if (rook != null && rook.type == PieceType.ROOK && rook.color == side) {
                // Check if squares between king and rook are empty
                val pathSquares = if (side == Color.WHITE) {
                    listOf(Square.B1, Square.C1, Square.D1)
                } else {
                    listOf(Square.B8, Square.C8, Square.D8)
                }
                val pathEmpty = pathSquares.all { board.getPiece(it) == null }
                
                // Check if king passes through check - check if pass-through square is attacked
                val kingPassSquare = if (side == Color.WHITE) Square.D1 else Square.D8
                val kingPassesThroughCheck = board.isSquareAttacked(kingPassSquare, opponent)
                
                if (pathEmpty && !kingPassesThroughCheck) {
                    moves.add(Move.castling(kingSquare, if (side == Color.WHITE) Square.C1 else Square.C8, king, Move.CastlingSide.QUEENSIDE))
                }
            }
        }
    }
}
