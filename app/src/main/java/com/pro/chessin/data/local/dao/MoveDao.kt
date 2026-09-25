package com.pro.chessin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.pro.chessin.data.local.entity.MoveEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for MoveEntity.
 * Uses Flow for reactive queries as per architecture conventions.
 */
@Dao
interface MoveDao {
    
    /**
     * Observe all moves for a specific game, ordered by ply.
     */
    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY ply ASC")
    fun observeMovesForGame(gameId: Long): Flow<List<MoveEntity>>
    
    /**
     * Get all moves for a specific game synchronously (for one-off queries).
     */
    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY ply ASC")
    suspend fun getMovesForGame(gameId: Long): List<MoveEntity>
    
    /**
     * Get a specific move by ID.
     */
    @Query("SELECT * FROM moves WHERE id = :moveId")
    suspend fun getMoveById(moveId: Long): MoveEntity?
    
    /**
     * Insert a single move.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMove(move: MoveEntity): Long
    
    /**
     * Insert multiple moves in a transaction.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMoves(moves: List<MoveEntity>)
    
    /**
     * Update an existing move.
     */
    @Update
    suspend fun updateMove(move: MoveEntity)
    
    /**
     * Delete a specific move.
     */
    @Delete
    suspend fun deleteMove(move: MoveEntity)
    
    /**
     * Delete all moves for a specific game.
     */
    @Query("DELETE FROM moves WHERE gameId = :gameId")
    suspend fun deleteMovesForGame(gameId: Long)
    
    /**
     * Query moves by classification tier across all games.
     * Enables "show all blunders across my games" queries.
     */
    @Query("SELECT * FROM moves WHERE classificationTier = :tier ORDER BY id DESC")
    fun observeMovesByClassification(tier: String): Flow<List<MoveEntity>>
    
    /**
     * Get the total count of moves for a specific game.
     */
    @Query("SELECT COUNT(*) FROM moves WHERE gameId = :gameId")
    fun observeMoveCountForGame(gameId: Long): Flow<Int>
}
