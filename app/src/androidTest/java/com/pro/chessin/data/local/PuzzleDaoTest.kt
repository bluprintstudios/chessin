package com.pro.chessin.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pro.chessin.data.local.dao.PuzzleDao
import com.pro.chessin.data.local.entity.PuzzleAttemptEntity
import com.pro.chessin.data.local.entity.PuzzleEntity
import com.pro.chessin.data.local.entity.UserPuzzleRatingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PuzzleDaoTest {

    private lateinit var database: ChessinDatabase
    private lateinit var puzzleDao: PuzzleDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChessinDatabase::class.java
        ).build()

        puzzleDao = database.puzzleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryPuzzle_returnsSamePuzzle() = runTest {
        val puzzle = PuzzleEntity(
            id = "test_1",
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
            solutionMoves = "d2d4 e5d4",
            rating = 1200,
            themes = "opening,fork",
            source = "LICHESS"
        )

        puzzleDao.insertPuzzle(puzzle)

        val retrieved = puzzleDao.getPuzzleById("test_1")
        assertNotNull(retrieved)
        assertEquals(puzzle.fen, retrieved!!.fen)
        assertEquals(puzzle.solutionMoves, retrieved.solutionMoves)
        assertEquals(1200, retrieved.rating)
    }

    @Test
    fun updateUserRating_persistsUserRatingState() = runTest {
        val ratingState = UserPuzzleRatingEntity(
            id = 1,
            rating = 1650.5,
            ratingDeviation = 120.0,
            volatility = 0.058
        )

        puzzleDao.updateUserRating(ratingState)

        val retrieved = puzzleDao.getUserRating()
        assertNotNull(retrieved)
        assertEquals(1650.5, retrieved!!.rating, 0.01)
        assertEquals(120.0, retrieved.ratingDeviation, 0.01)
    }

    @Test
    fun insertAttempt_savesAttemptHistory() = runTest {
        val attempt = PuzzleAttemptEntity(
            puzzleId = "test_1",
            correct = true,
            timeTakenMs = 4500L,
            ratingBefore = 1500,
            ratingAfter = 1520
        )

        puzzleDao.insertAttempt(attempt)

        val attempts = puzzleDao.getAllAttempts().first()
        assertEquals(1, attempts.size)
        assertEquals("test_1", attempts[0].puzzleId)
        assertTrue(attempts[0].correct)
        assertEquals(1520, attempts[0].ratingAfter)
    }
}
