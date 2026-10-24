package com.pro.chess_review.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class, AnalysisEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ChessReviewDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun analysisDao(): AnalysisDao
}
