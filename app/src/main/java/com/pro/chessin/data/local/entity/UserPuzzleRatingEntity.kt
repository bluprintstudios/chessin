package com.pro.chessin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton entity storing user's current puzzle rating parameters (Glicko-2).
 */
@Entity(tableName = "user_puzzle_rating")
data class UserPuzzleRatingEntity(
    @PrimaryKey
    val id: Int = 1,
    val rating: Double = 1500.0,
    val ratingDeviation: Double = 350.0,
    val volatility: Double = 0.06,
    val updatedAt: Long = System.currentTimeMillis()
)
