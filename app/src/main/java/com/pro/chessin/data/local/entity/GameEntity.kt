package com.pro.chessin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a chess game.
 * Stores game metadata and the raw PGN for the full game.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val whitePlayer: String,
    val blackPlayer: String,
    val date: Long, // Unix timestamp
    val result: String, // "1-0", "0-1", "1/2-1/2", "*"
    val event: String,
    val pgnRaw: String,
    val createdAt: Long // Unix timestamp when game was saved
)
