package com.pro.chessin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pro.chessin.data.local.entity.PuzzleAttemptEntity
import com.pro.chessin.data.local.entity.PuzzleEntity
import com.pro.chessin.data.local.entity.UserPuzzleRatingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for puzzles, attempts, and user puzzle ratings.
 */
@Dao
interface PuzzleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzle(puzzle: PuzzleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzles(puzzles: List<PuzzleEntity>)

    @Query("SELECT * FROM puzzles WHERE id = :id")
    suspend fun getPuzzleById(id: String): PuzzleEntity?

    @Query("SELECT COUNT(*) FROM puzzles")
    suspend fun getPuzzleCount(): Int

    @Query("SELECT * FROM puzzles ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomPuzzle(): PuzzleEntity?

    @Query("SELECT * FROM puzzles WHERE rating BETWEEN :minRating AND :maxRating ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomPuzzleNearRating(minRating: Int, maxRating: Int): PuzzleEntity?

    @Query("SELECT * FROM puzzles WHERE source = :source ORDER BY createdAt DESC")
    fun getPuzzlesBySource(source: String): Flow<List<PuzzleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: PuzzleAttemptEntity): Long

    @Query("SELECT * FROM puzzle_attempts ORDER BY attemptedAt DESC")
    fun getAllAttempts(): Flow<List<PuzzleAttemptEntity>>

    @Query("SELECT * FROM user_puzzle_rating WHERE id = 1")
    suspend fun getUserRating(): UserPuzzleRatingEntity?

    @Query("SELECT * FROM user_puzzle_rating WHERE id = 1")
    fun observeUserRating(): Flow<UserPuzzleRatingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserRating(rating: UserPuzzleRatingEntity)
}
