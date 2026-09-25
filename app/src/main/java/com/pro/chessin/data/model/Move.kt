package com.pro.chessin.data.model

/**
 * Domain model representing a single move in a chess game.
 * This is the public API layer - Room entities are internal to the data layer.
 */
data class Move(
    val id: Long,
    val gameId: Long,
    val ply: Int,
    val san: String,
    val fenAfter: String,
    val evalCp: Int?,
    val evalMate: Int?,
    val classificationTier: String,
    val explanationText: String?,
    val alternativeLinesJson: String? = null
)
