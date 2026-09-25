package com.pro.chessin.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single move in a chess game.
 * Normalized per-move table enables efficient queries like "show all blunders".
 */
@Entity(
    tableName = "moves",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["classificationTier"])
    ]
)
data class MoveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val gameId: Long, // Foreign key to GameEntity
    
    val ply: Int, // Move number (0-indexed: 0 = white's first move, 1 = black's first move, etc.)
    val san: String, // Standard Algebraic Notation (e.g., "e4", "Nf3", "O-O")
    val fenAfter: String, // FEN string after this move
    
    // Engine analysis results (nullable if not analyzed)
    val evalCp: Int? = null, // Centipawn evaluation (positive = white advantage)
    val evalMate: Int? = null, // Mate in N (positive = white mates in N, negative = black mates in |N|)
    
    // Move classification from MoveAnalyzer
    val classificationTier: String, // "BOOK", "BEST", "EXCELLENT", "GOOD", "INACCURACY", "MISTAKE", "BLUNDER", "MISS"
    
    // AI Coach explanation (nullable if not generated)
    val explanationText: String? = null,

    // MultiPV alternative engine lines serialized as JSON (nullable if not captured/analyzed)
    val alternativeLinesJson: String? = null
)
