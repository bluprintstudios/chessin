package com.pro.chessin.data.model

/**
 * Domain model representing a tactical puzzle.
 */
data class Puzzle(
    val id: String,
    val fen: String,
    val solutionMoves: List<String>,
    val rating: Int,
    val themes: List<String>,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)
