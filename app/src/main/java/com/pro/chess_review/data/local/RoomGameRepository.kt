package com.pro.chess_review.data.local

import com.pro.chess_review.domain.model.ChessGame
import com.pro.chess_review.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGameRepository(
    private val gameDao: GameDao,
) : GameRepository {
    override fun observeGames(): Flow<List<ChessGame>> =
        gameDao.observeAll().map { games -> games.map(GameEntity::toDomain) }

    override suspend fun getGame(id: String): ChessGame? =
        gameDao.findById(id)?.toDomain()

    override suspend fun saveGame(game: ChessGame) {
        gameDao.upsert(game.toEntity())
    }

    override suspend fun deleteGame(id: String) {
        gameDao.deleteById(id)
    }

    override suspend fun deleteAllGames() {
        gameDao.deleteAll()
    }
}
