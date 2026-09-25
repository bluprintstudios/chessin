package com.pro.chessin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a tactical chess puzzle.
 */
@Entity(
    tableName = "puzzles",
    indices = [
        Index(value = ["rating"]),
        Index(value = ["source"])
    ]
)
data class PuzzleEntity(
    @PrimaryKey
    val id: String,
    val fen: String,
    val solutionMoves: String, // Space-separated UCI/SAN moves
    val rating: Int,
    val themes: String = "",
    val source: String = "LICHESS", // "LICHESS" or "USER_MISTAKE"
    val createdAt: Long = System.currentTimeMillis()
)
