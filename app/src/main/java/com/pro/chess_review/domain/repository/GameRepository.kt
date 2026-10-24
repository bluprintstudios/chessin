package com.pro.chess_review.domain.repository

import com.pro.chess_review.domain.model.ChessGame
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGames(): Flow<List<ChessGame>>

    suspend fun getGame(id: String): ChessGame?

    suspend fun saveGame(game: ChessGame)

    suspend fun deleteGame(id: String)

    suspend fun deleteAllGames()
}
