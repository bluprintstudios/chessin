package com.pro.chessin.domain.chess

/**
 * PGN parser that handles Standard Algebraic Notation (SAN) with disambiguation,
 * move comments, NAGs (Numeric Annotation Glyphs), and RAV (Recursive Annotation Variations).
 */
object PgnParser {
    
    data class PgnGame(
        val moves: List<ParsedMove>,
        val result: GameResult,
        val tags: Map<String, String> = emptyMap()
    )
    
    data class ParsedMove(
        val san: String,
        val move: Move,
        val comment: String = "",
        val nags: List<Int> = emptyList(),
        val variations: List<List<ParsedMove>> = emptyList()
    )
    
    enum class GameResult {
        WHITE_WIN, BLACK_WIN, DRAW, ONGOING
    }
    
    /**
     * Parse a PGN string into a list of games.
     */
    fun parse(pgn: String): List<PgnGame> {
        val games = mutableListOf<PgnGame>()
        val gameTexts = splitGames(pgn)
        
        for (gameText in gameTexts) {
            val game = parseSingleGame(gameText)
            if (game != null) {
                games.add(game)
            }
        }
        
        return games
    }
    
    /**
     * Split PGN text into individual games.
     */
    private fun splitGames(pgn: String): List<String> {
        val games = mutableListOf<String>()
        var currentGame = StringBuilder()
        var inGame = false
        var hasMoves = false
        
        for (line in pgn.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                // Empty lines are ignored
                continue
            } else if (trimmed.startsWith("[")) {
                // New game starts when we see a header after we've already seen moves
                if (inGame && hasMoves) {
                    games.add(currentGame.toString())
                    currentGame = StringBuilder()
                    hasMoves = false
                }
                currentGame.append(trimmed).append("\n")
                inGame = true
            } else {
                currentGame.append(trimmed).append(" ")
                inGame = true
                hasMoves = true
            }
        }
        
        if (currentGame.isNotEmpty()) {
            games.add(currentGame.toString())
        }
        
        return games
    }
    
    /**
     * Parse a single game from PGN text.
     */
    private fun parseSingleGame(gameText: String): PgnGame? {
        val lines = gameText.lines()
        val tags = mutableMapOf<String, String>()
        val moveText = StringBuilder()
        
        for (line in lines) {
            if (line.startsWith("[")) {
                val tagMatch = Regex("""\[(\w+)\s+"([^"]*)"\]""").find(line)
                if (tagMatch != null) {
                    tags[tagMatch.groupValues[1]] = tagMatch.groupValues[2]
                }
            } else {
                moveText.append(line).append(" ")
            }
        }
        
        val moves = parseMoves(moveText.toString(), tags)
        val result = parseResult(tags["Result"] ?: moveText.toString())
        
        return PgnGame(moves, result, tags)
    }
    
    /**
     * Parse the move text section.
     */
    private fun parseMoves(moveText: String, tags: Map<String, String>): List<ParsedMove> {
        val moves = mutableListOf<ParsedMove>()
        val board = ChessBoardState.startPosition()
        
        // Remove move numbers, result, and comments for initial parsing
        val cleaned = moveText
            .replace(Regex("""\d+\.""")) { "" }
            .replace(Regex("""1-0|0-1|1/2-1/2|\*"""), "")
            .trim()
        
        // Parse moves with variations, comments, and NAGs
        val tokens = tokenize(cleaned)
        var currentBoard = board
        var index = 0
        
        while (index < tokens.size) {
            val token = tokens[index]
            
            when {
                token == "(" -> {
                    // Start of variation
                    val (variationMoves, newIndex) = parseVariation(tokens, index + 1, currentBoard)
                    if (moves.isNotEmpty()) {
                        val lastMove = moves.last()
                        moves[moves.size - 1] = lastMove.copy(variations = lastMove.variations + listOf(variationMoves))
                    }
                    index = newIndex
                }
                token == "{" -> {
                    // Comment
                    val (comment, newIndex) = parseComment(tokens, index + 1)
                    if (moves.isNotEmpty()) {
                        val lastMove = moves.last()
                        moves[moves.size - 1] = lastMove.copy(comment = lastMove.comment + comment)
                    }
                    index = newIndex
                }
                token.startsWith("$") -> {
                    // NAG
                    val nag = token.substring(1).toIntOrNull()
                    if (moves.isNotEmpty() && nag != null) {
                        val lastMove = moves.last()
                        moves[moves.size - 1] = lastMove.copy(nags = lastMove.nags + nag)
                    }
                    index++
                }
                else -> {
                    // Regular move
                    val move = parseSan(token, currentBoard)
                    if (move != null) {
                        moves.add(ParsedMove(token, move))
                        currentBoard = currentBoard.makeMove(move)
                    }
                    index++
                }
            }
        }
        
        return moves
    }
    
    /**
     * Tokenize the move text.
     */
    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        var inComment = false
        var inVariation = false
        var depth = 0
        
        for (char in text) {
            when {
                char == '{' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString().trim())
                        current = StringBuilder()
                    }
                    inComment = true
                    current.append(char)
                }
                char == '}' && inComment -> {
                    current.append(char)
                    tokens.add(current.toString())
                    current = StringBuilder()
                    inComment = false
                }
                char == '(' && !inComment -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString().trim())
                        current = StringBuilder()
                    }
                    inVariation = true
                    depth++
                    current.append(char)
                }
                char == ')' && inVariation -> {
                    current.append(char)
                    depth--
                    if (depth == 0) {
                        tokens.add(current.toString())
                        current = StringBuilder()
                        inVariation = false
                    }
                }
                char.isWhitespace() && !inComment && !inVariation -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString().trim())
                        current = StringBuilder()
                    }
                }
                else -> {
                    current.append(char)
                }
            }
        }
        
        if (current.isNotEmpty()) {
            tokens.add(current.toString().trim())
        }
        
        return tokens.filter { it.isNotEmpty() }
    }
    
    /**
     * Parse a variation (recursive).
     */
    private fun parseVariation(tokens: List<String>, startIndex: Int, board: ChessBoardState): Pair<List<ParsedMove>, Int> {
        val moves = mutableListOf<ParsedMove>()
        var currentBoard = board
        var index = startIndex
        var depth = 1
        
        while (index < tokens.size && depth > 0) {
            val token = tokens[index]
            
            when {
                token == "(" -> {
                    depth++
                    index++
                }
                token == ")" -> {
                    depth--
                    if (depth == 0) {
                        return Pair(moves, index + 1)
                    }
                    index++
                }
                token.startsWith("{") -> {
                    val (comment, newIndex) = parseComment(tokens, index + 1)
                    if (moves.isNotEmpty()) {
                        val lastMove = moves.last()
                        moves[moves.size - 1] = lastMove.copy(comment = lastMove.comment + comment)
                    }
                    index = newIndex
                }
                token.startsWith("$") -> {
                    val nag = token.substring(1).toIntOrNull()
                    if (moves.isNotEmpty() && nag != null) {
                        val lastMove = moves.last()
                        moves[moves.size - 1] = lastMove.copy(nags = lastMove.nags + nag)
                    }
                    index++
                }
                else -> {
                    val move = parseSan(token, currentBoard)
                    if (move != null) {
                        moves.add(ParsedMove(token, move))
                        currentBoard = currentBoard.makeMove(move)
                    }
                    index++
                }
            }
        }
        
        return Pair(moves, index)
    }
    
    /**
     * Parse a comment.
     */
    private fun parseComment(tokens: List<String>, startIndex: Int): Pair<String, Int> {
        val comment = StringBuilder()
        var index = startIndex
        
        while (index < tokens.size) {
            val token = tokens[index]
            comment.append(token).append(" ")
            if (token.endsWith("}")) {
                return Pair(comment.toString().trim().drop(1).dropLast(1), index + 1)
            }
            index++
        }
        
        return Pair(comment.toString().trim(), index)
    }
    
    /**
     * Parse a SAN move string.
     */
    private fun parseSan(san: String, board: ChessBoardState): Move? {
        val side = board.sideToMove
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        
        // Parse the SAN string
        val isCastlingKingside = san == "O-O" || san == "0-0"
        val isCastlingQueenside = san == "O-O-O" || san == "0-0-0"
        
        if (isCastlingKingside) {
            val kingSquare = if (side == Color.WHITE) Square.E1 else Square.E8
            val king = board.getPiece(kingSquare)
            if (king != null) {
                val toSquare = if (side == Color.WHITE) Square.G1 else Square.G8
                return Move.castling(kingSquare, toSquare, king, Move.CastlingSide.KINGSIDE)
            }
        }
        
        if (isCastlingQueenside) {
            val kingSquare = if (side == Color.WHITE) Square.E1 else Square.E8
            val king = board.getPiece(kingSquare)
            if (king != null) {
                val toSquare = if (side == Color.WHITE) Square.C1 else Square.C8
                return Move.castling(kingSquare, toSquare, king, Move.CastlingSide.QUEENSIDE)
            }
        }
        
        // Regular move - clean check/mate/eval symbols (+, #, !, ?)
        val cleanSan = san.replace(Regex("""[+#!?]*"""), "")
        
        // Promotion parsing (=Q, =R, =B, =N)
        val promotionMatch = Regex("""(.*)=([QRBN])""").find(cleanSan)
        val promotion = promotionMatch?.groupValues?.get(2)?.let { 
            when (it) {
                "Q" -> PieceType.QUEEN
                "R" -> PieceType.ROOK
                "B" -> PieceType.BISHOP
                "N" -> PieceType.KNIGHT
                else -> null
            }
        }
        val movePart = promotionMatch?.groupValues?.get(1) ?: cleanSan
        val isCapture = movePart.contains('x')
        
        // Identify piece type
        val firstChar = movePart.firstOrNull() ?: return null
        val pieceType = when {
            firstChar.isUpperCase() -> when (firstChar) {
                'K' -> PieceType.KING
                'Q' -> PieceType.QUEEN
                'R' -> PieceType.ROOK
                'B' -> PieceType.BISHOP
                'N' -> PieceType.KNIGHT
                else -> return null
            }
            else -> PieceType.PAWN
        }
        
        // Strip piece char if uppercase
        val spec = if (firstChar.isUpperCase()) movePart.substring(1) else movePart
        
        // Find destination square (last 2 chars of spec matching a-h1-8)
        val destMatch = Regex("""([a-h])([1-8])""").find(spec) ?: return null
        val destFile = destMatch.groupValues[1][0] - 'a'
        val destRank = destMatch.groupValues[2].toInt() - 1
        val destSquare = Square.fromCoordinates(destFile, destRank) ?: return null
        
        // Disambiguation is everything in `spec` BEFORE the destination match, excluding 'x'
        val disambigString = spec.substring(0, destMatch.range.first).replace("x", "")
        val disambigFile = disambigString.firstOrNull { it in 'a'..'h' }?.let { it - 'a' }
        val disambigRank = disambigString.firstOrNull { it in '1'..'8' }?.let { it.digitToInt() - 1 }
        
        // Filter matching legal moves
        val matchingMoves = legalMoves.filter { move ->
            move.piece.type == pieceType &&
            move.piece.color == side &&
            move.to == destSquare &&
            move.isCapture == isCapture &&
            (promotion == null || move.promotion == promotion) &&
            (disambigFile == null || move.from.file == disambigFile) &&
            (disambigRank == null || move.from.rank == disambigRank)
        }
        
        return matchingMoves.firstOrNull()
    }
    
    /**
     * Parse game result from tags or move text.
     */
    private fun parseResult(resultText: String): GameResult {
        return when {
            resultText.contains("1-0") -> GameResult.WHITE_WIN
            resultText.contains("0-1") -> GameResult.BLACK_WIN
            resultText.contains("1/2-1/2") -> GameResult.DRAW
            else -> GameResult.ONGOING
        }
    }
    
    /**
     * Export a list of moves to PGN format.
     */
    fun export(moves: List<ParsedMove>, result: GameResult = GameResult.ONGOING): String {
        val pgn = StringBuilder()
        var moveNumber = 1
        var isWhiteTurn = true
        
        for (move in moves) {
            if (isWhiteTurn) {
                pgn.append("$moveNumber. ")
            }
            
            pgn.append(move.san)
            
            if (move.comment.isNotEmpty()) {
                pgn.append(" {${move.comment}}")
            }
            
            for (nag in move.nags) {
                pgn.append(" $$nag")
            }
            
            for (variation in move.variations) {
                pgn.append(" (")
                pgn.append(export(variation, GameResult.ONGOING).trim())
                pgn.append(")")
            }
            
            pgn.append(" ")
            
            isWhiteTurn = !isWhiteTurn
            if (isWhiteTurn) moveNumber++
        }
        
        val resultStr = when (result) {
            GameResult.WHITE_WIN -> "1-0"
            GameResult.BLACK_WIN -> "0-1"
            GameResult.DRAW -> "1/2-1/2"
            GameResult.ONGOING -> "*"
        }
        
        pgn.append(resultStr)
        
        return pgn.toString().trim()
    }
}
