package com.pro.chessin.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a single move node in an opening repertoire tree.
 * 
 * Repertoire trees are stored as an adjacency list using a self-referencing
 * [parentId] foreign key with CASCADE deletion. A null [parentId] indicates
 * a root node of a variation tree.
 * 
 * Spaced repetition parameters (Leitner 5-box system) are stored per node.
 */
@Entity(
    tableName = "repertoire_nodes",
    foreignKeys = [
        ForeignKey(
            entity = RepertoireNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["colorToPlay"]),
        Index(value = ["nextReviewAt"])
    ]
)
data class RepertoireNodeEntity(
    @PrimaryKey(autoGenerate = true)
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
    val nextReviewAt: Long = 0L, // Epoch timestamp in ms; 0L = due immediately
    val createdAt: Long = System.currentTimeMillis()
)
