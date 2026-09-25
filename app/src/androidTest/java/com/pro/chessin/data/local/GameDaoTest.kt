package com.pro.chessin.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pro.chessin.data.local.dao.GameDao
import com.pro.chessin.data.local.dao.MoveDao
import com.pro.chessin.data.local.entity.GameEntity
import com.pro.chessin.data.local.entity.MoveEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Instrumented tests for GameDao and MoveDao.
 * Uses an in-memory database for fast, isolated testing.
 */
@RunWith(AndroidJUnit4::class)
class GameDaoTest {
    
    private lateinit var database: ChessinDatabase
    private lateinit var gameDao: GameDao
    private lateinit var moveDao: MoveDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChessinDatabase::class.java
        ).build()
        
        gameDao = database.gameDao()
        moveDao = database.moveDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertGame_and_queryById_returnsSameGame() = runTest {
        val game = GameEntity(
            whitePlayer = "Magnus Carlsen",
            blackPlayer = "Fabiano Caruana",
            date = 1704067200000L, // 2024-01-01
            result = "1-0",
            event = "World Championship",
            pgnRaw = "1. e4 e5 2. Nf3 Nc6",
            createdAt = System.currentTimeMillis()
        )
        
        val gameId = gameDao.insertGame(game)
        assertTrue("Game ID should be positive", gameId > 0)
        
        val retrievedGame = gameDao.getGameById(gameId)
        assertNotNull("Game should be retrieved", retrievedGame)
        assertEquals("White player should match", game.whitePlayer, retrievedGame!!.whitePlayer)
        assertEquals("Black player should match", game.blackPlayer, retrievedGame.blackPlayer)
        assertEquals("Result should match", game.result, retrievedGame.result)
        assertEquals("PGN should match", game.pgnRaw, retrievedGame.pgnRaw)
    }
    
    @Test
    fun insertGame_and_observeAllGames_returnsGame() = runTest {
        val game = GameEntity(
            whitePlayer = "Magnus Carlsen",
            blackPlayer = "Fabiano Caruana",
            date = 1704067200000L,
            result = "1-0",
            event = "World Championship",
            pgnRaw = "1. e4 e5 2. Nf3 Nc6",
            createdAt = System.currentTimeMillis()
        )
        
        val gameId = gameDao.insertGame(game)
        
        val games = gameDao.observeAllGames().first()
        assertEquals("Should have 1 game", 1, games.size)
        assertEquals("Game should match", game.whitePlayer, games[0].whitePlayer)
    }
    
    @Test
    fun insertMoves_and_queryForGame_returnsMoves() = runTest {
        val game = GameEntity(
            whitePlayer = "Magnus Carlsen",
            blackPlayer = "Fabiano Caruana",
            date = 1704067200000L,
            result = "1-0",
            event = "World Championship",
            pgnRaw = "1. e4 e5 2. Nf3 Nc6",
            createdAt = System.currentTimeMillis()
        )
        
        val gameId = gameDao.insertGame(game)
        
        val moves = listOf(
            MoveEntity(
                gameId = gameId,
                ply = 0,
                san = "e4",
                fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                evalCp = 20,
                evalMate = null,
                classificationTier = "BEST",
                explanationText = null
            ),
            MoveEntity(
                gameId = gameId,
                ply = 1,
                san = "e5",
                fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                evalCp = 15,
                evalMate = null,
                classificationTier = "GOOD",
                explanationText = null
            ),
            MoveEntity(
                gameId = gameId,
                ply = 2,
                san = "Nf3",
                fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
                evalCp = 25,
                evalMate = null,
                classificationTier = "BEST",
                explanationText = null
            )
        )
        
        moveDao.insertMoves(moves)
        
        val retrievedMoves = moveDao.getMovesForGame(gameId)
        assertEquals("Should have 3 moves", 3, retrievedMoves.size)
        assertEquals("First move SAN should match", "e4", retrievedMoves[0].san)
        assertEquals("Second move SAN should match", "e5", retrievedMoves[1].san)
        assertEquals("Third move SAN should match", "Nf3", retrievedMoves[2].san)
        assertEquals("Moves should be ordered by ply", 0, retrievedMoves[0].ply)
        assertEquals("Moves should be ordered by ply", 1, retrievedMoves[1].ply)
        assertEquals("Moves should be ordered by ply", 2, retrievedMoves[2].ply)
    }
    
    @Test
    fun deleteGame_cascadesToMoves() = runTest {
        val game = GameEntity(
            whitePlayer = "Magnus Carlsen",
            blackPlayer = "Fabiano Caruana",
            date = 1704067200000L,
            result = "1-0",
            event = "World Championship",
            pgnRaw = "1. e4 e5 2. Nf3 Nc6",
            createdAt = System.currentTimeMillis()
        )
        
        val gameId = gameDao.insertGame(game)
        
        val moves = listOf(
            MoveEntity(
                gameId = gameId,
                ply = 0,
                san = "e4",
                fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                evalCp = 20,
                evalMate = null,
                classificationTier = "BEST",
                explanationText = null
            )
        )
        
        moveDao.insertMoves(moves)
        
        // Verify moves exist
        var retrievedMoves = moveDao.getMovesForGame(gameId)
        assertEquals("Should have 1 move before deletion", 1, retrievedMoves.size)
        
        // Delete game
        gameDao.deleteGameById(gameId.toInt())
        
        // Verify moves are deleted via CASCADE
        retrievedMoves = moveDao.getMovesForGame(gameId)
        assertEquals("Moves should be deleted via CASCADE", 0, retrievedMoves.size)
        
        // Verify game is deleted
        val retrievedGame = gameDao.getGameById(gameId)
        assertNull("Game should be deleted", retrievedGame)
    }
    
    @Test
    fun observeMovesByClassification_filtersCorrectly() = runTest {
        val game = GameEntity(
            whitePlayer = "Magnus Carlsen",
            blackPlayer = "Fabiano Caruana",
            date = 1704067200000L,
            result = "1-0",
            event = "World Championship",
            pgnRaw = "1. e4 e5 2. Nf3 Nc6",
            createdAt = System.currentTimeMillis()
        )
        
        val gameId = gameDao.insertGame(game)
        
        val moves = listOf(
            MoveEntity(
                gameId = gameId,
                ply = 0,
                san = "e4",
                fenAfter = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                evalCp = 20,
                evalMate = null,
                classificationTier = "BEST",
                explanationText = null
            ),
            MoveEntity(
                gameId = gameId,
                ply = 1,
                san = "e5",
                fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                evalCp = -50,
                evalMate = null,
                classificationTier = "BLUNDER",
                explanationText = null
            ),
            MoveEntity(
                gameId = gameId,
                ply = 2,
                san = "Nf3",
                fenAfter = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
                evalCp = 25,
                evalMate = null,
                classificationTier = "BEST",
                explanationText = null
            )
        )
        
        moveDao.insertMoves(moves)
        
        val blunders = moveDao.observeMovesByClassification("BLUNDER").first()
        assertEquals("Should have 1 blunder", 1, blunders.size)
        assertEquals("Blunder SAN should match", "e5", blunders[0].san)
        
        val bestMoves = moveDao.observeMovesByClassification("BEST").first()
        assertEquals("Should have 2 best moves", 2, bestMoves.size)
    }
}
