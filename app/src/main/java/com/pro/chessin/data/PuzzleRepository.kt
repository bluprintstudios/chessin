package com.pro.chessin.data

import android.content.Context
import com.pro.chessin.data.local.dao.PuzzleDao
import com.pro.chessin.data.local.entity.PuzzleAttemptEntity
import com.pro.chessin.data.local.entity.PuzzleEntity
import com.pro.chessin.data.local.entity.UserPuzzleRatingEntity
import com.pro.chessin.data.model.Game
import com.pro.chessin.data.model.Move
import com.pro.chessin.data.model.Puzzle
import com.pro.chessin.data.model.PuzzleAttempt
import com.pro.chessin.domain.puzzles.Glicko2RatingSystem
import com.pro.chessin.domain.puzzles.GlickoRatingState
import com.pro.chessin.domain.puzzles.MistakePuzzleGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository managing tactical puzzles, user attempts, and Glicko-2 ratings.
 */
@Singleton
class PuzzleRepository @Inject constructor(
    private val puzzleDao: PuzzleDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Ensure curated Lichess puzzles asset is loaded into the database on first run.
     */
    suspend fun ensureCuratedPuzzlesImported() {
        if (puzzleDao.getPuzzleCount() > 0) return

        try {
            val jsonString = context.assets.open("puzzles_curated.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val puzzles = mutableListOf<PuzzleEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                puzzles.add(
                    PuzzleEntity(
                        id = obj.getString("id"),
                        fen = obj.getString("fen"),
                        solutionMoves = obj.getString("solutionMoves"),
                        rating = obj.getInt("rating"),
                        themes = obj.optString("themes", ""),
                        source = "LICHESS"
                    )
                )
            }
            puzzleDao.insertPuzzles(puzzles)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetch a random puzzle tailored near the user's rating.
     */
    suspend fun getRandomPuzzle(userRating: Int): Puzzle? {
        ensureCuratedPuzzlesImported()
        val minRating = (userRating - 200).coerceAtLeast(600)
        val maxRating = userRating + 200

        val entity = puzzleDao.getRandomPuzzleNearRating(minRating, maxRating)
            ?: puzzleDao.getRandomPuzzle()

        return entity?.toDomain()
    }

    /**
     * Get a puzzle by ID.
     */
    suspend fun getPuzzleById(id: String): Puzzle? {
        return puzzleDao.getPuzzleById(id)?.toDomain()
    }

    /**
     * Generate mistake puzzles from an analyzed game and save them.
     */
    suspend fun generateMistakePuzzlesForGame(game: Game, moves: List<Move>) {
        val userRating = getUserRatingState().rating.toInt()
        val puzzles = MistakePuzzleGenerator.generateFromGame(game, moves, userRating)
        if (puzzles.isNotEmpty()) {
            puzzleDao.insertPuzzles(puzzles)
        }
    }

    /**
     * Record a user attempt, calculate Glicko-2 rating update, and update DB.
     */
    suspend fun recordAttempt(
        puzzle: Puzzle,
        correct: Boolean,
        timeTakenMs: Long
    ): GlickoRatingState {
        val currentState = getUserRatingState()
        val score = if (correct) 1.0 else 0.0

        val newState = Glicko2RatingSystem.calculateNewRating(
            userState = currentState,
            puzzleRating = puzzle.rating.toDouble(),
            puzzleRD = 50.0,
            score = score
        )

        // Save new user rating
        puzzleDao.updateUserRating(
            UserPuzzleRatingEntity(
                id = 1,
                rating = newState.rating,
                ratingDeviation = newState.ratingDeviation,
                volatility = newState.volatility,
                updatedAt = System.currentTimeMillis()
            )
        )

        // Save attempt record
        puzzleDao.insertAttempt(
            PuzzleAttemptEntity(
                puzzleId = puzzle.id,
                correct = correct,
                timeTakenMs = timeTakenMs,
                ratingBefore = currentState.rating.toInt(),
                ratingAfter = newState.rating.toInt(),
                attemptedAt = System.currentTimeMillis()
            )
        )

        return newState
    }

    /**
     * Get user's current Glicko-2 rating state.
     */
    suspend fun getUserRatingState(): GlickoRatingState {
        val entity = puzzleDao.getUserRating()
        return if (entity != null) {
            GlickoRatingState(entity.rating, entity.ratingDeviation, entity.volatility)
        } else {
            GlickoRatingState(1500.0, 350.0, 0.06)
        }
    }

    /**
     * Observe user's rating state reactively.
     */
    fun observeUserRating(): Flow<GlickoRatingState> {
        return puzzleDao.observeUserRating().map { entity ->
            if (entity != null) {
                GlickoRatingState(entity.rating, entity.ratingDeviation, entity.volatility)
            } else {
                GlickoRatingState(1500.0, 350.0, 0.06)
            }
        }
    }

    /**
     * Observe all attempts.
     */
    fun getAllAttempts(): Flow<List<PuzzleAttempt>> {
        return puzzleDao.getAllAttempts().map { list ->
            list.map {
                PuzzleAttempt(
                    id = it.id,
                    puzzleId = it.puzzleId,
                    correct = it.correct,
                    timeTakenMs = it.timeTakenMs,
                    ratingBefore = it.ratingBefore,
                    ratingAfter = it.ratingAfter,
                    attemptedAt = it.attemptedAt
                )
            }
        }
    }

    private fun PuzzleEntity.toDomain(): Puzzle {
        return Puzzle(
            id = id,
            fen = fen,
            solutionMoves = solutionMoves.split(" ").filter { it.isNotBlank() },
            rating = rating,
            themes = themes.split(",").filter { it.isNotBlank() },
            source = source,
            createdAt = createdAt
        )
    }
}
