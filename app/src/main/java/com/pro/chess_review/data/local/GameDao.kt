package com.pro.chess_review.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY playedAtEpochMillis DESC, importedAtEpochMillis DESC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): GameEntity?

    @Upsert
    suspend fun upsert(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM games")
    suspend fun deleteAll()
}
