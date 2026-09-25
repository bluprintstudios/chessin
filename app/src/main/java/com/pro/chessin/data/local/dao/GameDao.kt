package com.pro.chessin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.pro.chessin.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GameEntity.
 * Uses Flow for reactive queries as per architecture conventions.
 */
@Dao
interface GameDao {
    
    /**
     * Observe all games, ordered by creation date (newest first).
     */
    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun observeAllGames(): Flow<List<GameEntity>>
    
    /**
     * Get a specific game by ID as a Flow.
     */
    @Query("SELECT * FROM games WHERE id = :gameId")
    fun observeGameById(gameId: Long): Flow<GameEntity?>
    
    /**
     * Get a specific game by ID synchronously (for one-off queries).
     */
    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: Long): GameEntity?
    
    /**
     * Insert a new game. Returns the generated row ID.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGame(game: GameEntity): Long
    
    /**
     * Update an existing game.
     */
    @Update
    suspend fun updateGame(game: GameEntity)
    
    /**
     * Delete a game. Moves are automatically deleted via CASCADE.
     */
    @Delete
    suspend fun deleteGame(game: GameEntity)
    
    /**
     * Delete a game by ID.
     */
    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: Int)
    
    /**
     * Get the total count of games.
     */
    @Query("SELECT COUNT(*) FROM games")
    fun observeGameCount(): Flow<Int>
}
