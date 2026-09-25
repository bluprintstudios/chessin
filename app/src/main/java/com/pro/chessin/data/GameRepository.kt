package com.pro.chessin.data

import com.pro.chessin.data.local.ChessinDatabase
import com.pro.chessin.data.local.dao.GameDao
import com.pro.chessin.data.local.dao.MoveDao
import com.pro.chessin.data.local.entity.GameEntity
import com.pro.chessin.data.local.entity.MoveEntity
import com.pro.chessin.data.model.Game
import com.pro.chessin.data.model.Move
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for game and move data.
 * Exposes domain models (Game, Move) instead of raw Room entities.
 * Uses Flow for reactive queries as per architecture conventions.
 */
@Singleton
class GameRepository @Inject constructor(
    private val database: ChessinDatabase
) {
    
    private val gameDao: GameDao = database.gameDao()
    private val moveDao: MoveDao = database.moveDao()
    
    /**
     * Observe all games as domain models.
     */
    fun observeAllGames(): Flow<List<Game>> {
        return gameDao.observeAllGames().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Observe a specific game by ID as a domain model.
     */
    fun observeGameById(gameId: Long): Flow<Game?> {
        return gameDao.observeGameById(gameId).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    /**
     * Observe all moves for a specific game as domain models.
     */
    fun observeMovesForGame(gameId: Long): Flow<List<Move>> {
        return moveDao.observeMovesForGame(gameId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Observe moves by classification tier across all games.
     */
    fun observeMovesByClassification(tier: String): Flow<List<Move>> {
        return moveDao.observeMovesByClassification(tier).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Observe the total count of games.
     */
    fun observeGameCount(): Flow<Int> = gameDao.observeGameCount()
    
    /**
     * Observe the total count of moves for a specific game.
     */
    fun observeMoveCountForGame(gameId: Long): Flow<Int> = moveDao.observeMoveCountForGame(gameId)
    
    /**
     * Insert a new game and return its generated ID.
     */
    suspend fun insertGame(game: Game): Long {
        val entity = game.toEntity()
        return gameDao.insertGame(entity)
    }
    
    /**
     * Insert a new move and return its generated ID.
     */
    suspend fun insertMove(move: Move): Long {
        val entity = move.toEntity()
        return moveDao.insertMove(entity)
    }
    
    /**
     * Insert multiple moves in a transaction.
     */
    suspend fun insertMoves(moves: List<Move>) {
        val entities = moves.map { it.toEntity() }
        moveDao.insertMoves(entities)
    }
    
    /**
     * Update an existing game.
     */
    suspend fun updateGame(game: Game) {
        val entity = game.toEntity()
        gameDao.updateGame(entity)
    }
    
    /**
     * Update an existing move.
     */
    suspend fun updateMove(move: Move) {
        val entity = move.toEntity()
        moveDao.updateMove(entity)
    }
    
    /**
     * Delete a game (moves are automatically deleted via CASCADE).
     */
    suspend fun deleteGame(game: Game) {
        val entity = game.toEntity()
        gameDao.deleteGame(entity)
    }
    
    /**
     * Delete a game by ID.
     */
    suspend fun deleteGameById(gameId: Long) {
        gameDao.deleteGameById(gameId.toInt())
    }
    
    /**
     * Delete all moves for a specific game.
     */
    suspend fun deleteMovesForGame(gameId: Long) {
        moveDao.deleteMovesForGame(gameId)
    }
}

// Extension functions to convert between entities and domain models

private fun GameEntity.toDomainModel(): Game {
    return Game(
        id = id,
        whitePlayer = whitePlayer,
        blackPlayer = blackPlayer,
        date = date,
        result = result,
        event = event,
        pgnRaw = pgnRaw,
        createdAt = createdAt
    )
}

private fun Game.toEntity(): GameEntity {
    return GameEntity(
        id = id,
        whitePlayer = whitePlayer,
        blackPlayer = blackPlayer,
        date = date,
        result = result,
        event = event,
        pgnRaw = pgnRaw,
        createdAt = createdAt
    )
}

private fun MoveEntity.toDomainModel(): Move {
    return Move(
        id = id,
        gameId = gameId,
        ply = ply,
        san = san,
        fenAfter = fenAfter,
        evalCp = evalCp,
        evalMate = evalMate,
        classificationTier = classificationTier,
        explanationText = explanationText,
        alternativeLinesJson = alternativeLinesJson
    )
}

private fun Move.toEntity(): MoveEntity {
    return MoveEntity(
        id = id,
        gameId = gameId,
        ply = ply,
        san = san,
        fenAfter = fenAfter,
        evalCp = evalCp,
        evalMate = evalMate,
        classificationTier = classificationTier,
        explanationText = explanationText,
        alternativeLinesJson = alternativeLinesJson
    )
}
