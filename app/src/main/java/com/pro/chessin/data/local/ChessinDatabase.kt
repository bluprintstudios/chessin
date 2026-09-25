package com.pro.chessin.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pro.chessin.data.local.dao.GameDao
import com.pro.chessin.data.local.dao.MoveDao
import com.pro.chessin.data.local.dao.PuzzleDao
import com.pro.chessin.data.local.dao.RepertoireDao
import com.pro.chessin.data.local.entity.GameEntity
import com.pro.chessin.data.local.entity.MoveEntity
import com.pro.chessin.data.local.entity.PuzzleAttemptEntity
import com.pro.chessin.data.local.entity.PuzzleEntity
import com.pro.chessin.data.local.entity.RepertoireNodeEntity
import com.pro.chessin.data.local.entity.UserPuzzleRatingEntity

/**
 * Room database for storing chess games, moves, opening repertoires, and tactical puzzles.
 * 
 * Schema version 3 adds tactical puzzle entities with AutoMigration.
 */
@Database(
    entities = [
        GameEntity::class,
        MoveEntity::class,
        RepertoireNodeEntity::class,
        PuzzleEntity::class,
        PuzzleAttemptEntity::class,
        UserPuzzleRatingEntity::class
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ],
    exportSchema = true
)
abstract class ChessinDatabase : RoomDatabase() {
    
    abstract fun gameDao(): GameDao
    abstract fun moveDao(): MoveDao
    abstract fun repertoireDao(): RepertoireDao
    abstract fun puzzleDao(): PuzzleDao
    
    companion object {
        private const val DATABASE_NAME = "chessin.db"
        
        @Volatile
        private var INSTANCE: ChessinDatabase? = null
        
        /**
         * Get the singleton database instance.
         * Uses double-checked locking for thread safety.
         */
        fun getInstance(context: Context): ChessinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        /**
         * Build the database instance.
         */
        private fun buildDatabase(context: Context): ChessinDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChessinDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
