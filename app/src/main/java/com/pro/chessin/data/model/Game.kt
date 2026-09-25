package com.pro.chessin.data.model

/**
 * Domain model representing a chess game.
 * This is the public API layer - Room entities are internal to the data layer.
 */
data class Game(
    val id: Long,
    val whitePlayer: String,
    val blackPlayer: String,
    val date: Long,
    val result: String,
    val event: String,
    val pgnRaw: String,
    val createdAt: Long
)
