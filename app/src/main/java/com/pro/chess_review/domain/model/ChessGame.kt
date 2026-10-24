package com.pro.chess_review.domain.model

enum class GameSource {
    // Retained so older local database rows from a previous build can still be read.
    CHESS_COM,
    PGN_FILE,
}

enum class GameParseStatus {
    READY,
    NEEDS_REVIEW,
    UNSUPPORTED,
}

data class ChessGame(
    val id: String,
    val source: GameSource,
    val sourceGameId: String?,
    val username: String?,
    val importedAtEpochMillis: Long,
    val playedAtEpochMillis: Long?,
    val whitePlayer: String?,
    val blackPlayer: String?,
    val whiteRating: Int?,
    val blackRating: Int?,
    val timeControl: String?,
    val result: String?,
    val pgn: String,
    val variant: String,
    val parseStatus: GameParseStatus,
)
