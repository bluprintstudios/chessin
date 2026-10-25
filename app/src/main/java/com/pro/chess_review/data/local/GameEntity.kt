package com.pro.chess_review.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.GameParseStatus
import com.pro.chess_review.domain.model.GameSource

@Entity(
    tableName = "games",
    indices = [Index(value = ["source", "sourceGameId"], unique = true)],
)
data class GameEntity(
    @PrimaryKey val id: String,
    val source: String,
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
    val parseStatus: String,
)

fun GameEntity.toDomain(): ChessGame = ChessGame(
    id = id,
    source = GameSource.valueOf(source),
    sourceGameId = sourceGameId,
    username = username,
    importedAtEpochMillis = importedAtEpochMillis,
    playedAtEpochMillis = playedAtEpochMillis,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    whiteRating = whiteRating,
    blackRating = blackRating,
    timeControl = timeControl,
    result = result,
    pgn = pgn,
    variant = variant,
    parseStatus = GameParseStatus.valueOf(parseStatus),
)

fun ChessGame.toEntity(): GameEntity = GameEntity(
    id = id,
    source = source.name,
    sourceGameId = sourceGameId,
    username = username,
    importedAtEpochMillis = importedAtEpochMillis,
    playedAtEpochMillis = playedAtEpochMillis,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    whiteRating = whiteRating,
    blackRating = blackRating,
    timeControl = timeControl,
    result = result,
    pgn = pgn,
    variant = variant,
    parseStatus = parseStatus.name,
)
