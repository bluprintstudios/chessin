package com.pro.chess_review.core.chess

import com.github.bhlangonijr.chesslib.game.Game
import com.github.bhlangonijr.chesslib.pgn.GameLoader
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.GameParseStatus
import com.pro.chess_review.domain.model.GameSource
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object PgnImporter {
    /**
     * Parses a raw PGN string into a list of [ChessGame] objects.
     */
    fun parsePgn(pgnString: String, source: GameSource): List<ChessGame> {
        val iterator = pgnString.lineSequence().iterator()
        val parsedGames = mutableListOf<ChessGame>()
        
        while (iterator.hasNext()) {
            val game = GameLoader.loadNextGame(iterator) ?: break

            // chesslib can represent arbitrary text as an empty game. A reviewable
            // PGN must contain at least one parsed half-move.
            if (game.halfMoves.isEmpty()) continue

            val id = UUID.randomUUID().toString()
            val white = game.whitePlayer?.name
            val black = game.blackPlayer?.name
            val whiteRating = game.whitePlayer?.elo ?: 0
            val blackRating = game.blackPlayer?.elo ?: 0
            
            // Deduplication keys
            val sourceGameId = game.property?.get("Site") ?: game.property?.get("Event")
            
            // Try parse date
            var playedAt: Long? = null
            val dateString = game.date
            if (!dateString.isNullOrBlank() && dateString != "????.??.??") {
                try {
                    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.US)
                    playedAt = sdf.parse(dateString)?.time
                } catch (e: Exception) {
                    // Ignore malformed dates
                }
            }
            
            // Reconstruct single PGN
            val reconstructedPgn = try {
                game.toPgn(false, false)
            } catch (e: Exception) {
                pgnString // fallback to raw string if regeneration fails
            }
            
            // Check for unsupported variants. chesslib handles standard, but we'll check Variant header
            val variant = game.property?.get("Variant") ?: "Standard"
            val parseStatus = if (variant == "Standard") GameParseStatus.READY else GameParseStatus.UNSUPPORTED
            
            parsedGames.add(
                ChessGame(
                    id = id,
                    source = source,
                    sourceGameId = sourceGameId,
                    username = null,
                    importedAtEpochMillis = System.currentTimeMillis(),
                    playedAtEpochMillis = playedAt,
                    whitePlayer = white,
                    blackPlayer = black,
                    whiteRating = if (whiteRating > 0) whiteRating else null,
                    blackRating = if (blackRating > 0) blackRating else null,
                    timeControl = game.property?.get("TimeControl"),
                    result = game.result?.description,
                    pgn = reconstructedPgn,
                    variant = variant,
                    parseStatus = parseStatus,
                )
            )
        }
        
        return parsedGames
    }
}
