package com.pro.chessin.data.model

/**
 * Domain model representing a single move node in an opening repertoire tree.
 * Adjacency-list model using parentId for infinite tree depth.
 */
data class RepertoireNode(
    val id: Long = 0,
    val parentId: Long?,
    val fen: String,
    val moveSan: String,
    val fromSquare: String,
    val toSquare: String,
    val promotionPiece: String? = null,
    val comment: String = "",
    val colorToPlay: String, // "WHITE" or "BLACK"
    val boxNumber: Int = 1, // Leitner box 1..5
    val nextReviewAt: Long = 0L, // Epoch ms, 0L = due
    val createdAt: Long = System.currentTimeMillis()
)
