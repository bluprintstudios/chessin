package com.pro.chess_review.domain.usecase

import com.pro.chess_review.core.chess.PgnImporter
import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.model.GameSource
import com.pro.chess_review.domain.repository.GameRepository

class ImportPgnUseCase(private val gameRepository: GameRepository) {
    /**
     * Parses a PGN string and saves valid games to the repository.
     * Returns a list of parsed games or an AppFailure if the parsing failed completely.
     */
    suspend operator fun invoke(pgnString: String, source: GameSource = GameSource.PGN_FILE): Result<List<ChessGame>> {
        return try {
            val games = PgnImporter.parsePgn(pgnString, source)
            if (games.isEmpty()) {
                Result.failure(Exception("No games found in PGN"))
            } else {
                games.forEach { game ->
                    gameRepository.saveGame(game)
                }
                Result.success(games)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
