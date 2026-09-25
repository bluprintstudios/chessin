package com.pro.chessin.data

import com.pro.chessin.data.local.dao.RepertoireDao
import com.pro.chessin.data.local.entity.RepertoireNodeEntity
import com.pro.chessin.data.model.RepertoireNode
import com.pro.chessin.domain.repertoire.LeitnerSpacedRepetition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository managing opening repertoire operations.
 * Decouples Room entities from domain representation.
 */
@Singleton
class RepertoireRepository @Inject constructor(
    private val repertoireDao: RepertoireDao
) {

    /**
     * Get reactive flow of all nodes for a specific color repertoire ("WHITE" or "BLACK").
     */
    fun getNodesByColor(colorToPlay: String): Flow<List<RepertoireNode>> {
        return repertoireDao.getNodesByColor(colorToPlay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get reactive flow of all nodes across both colors.
     */
    fun getAllNodes(): Flow<List<RepertoireNode>> {
        return repertoireDao.getAllNodes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Get nodes due for review for a given color.
     */
    suspend fun getDueNodes(
        colorToPlay: String,
        maxTimestamp: Long = System.currentTimeMillis()
    ): List<RepertoireNode> {
        return repertoireDao.getDueNodes(colorToPlay, maxTimestamp).map { it.toDomain() }
    }

    /**
     * Get single node by ID.
     */
    suspend fun getNodeById(id: Long): RepertoireNode? {
        return repertoireDao.getNodeById(id)?.toDomain()
    }

    /**
     * Get direct children of a parent node.
     */
    suspend fun getChildrenOf(parentId: Long?): List<RepertoireNode> {
        return repertoireDao.getChildrenOf(parentId).map { it.toDomain() }
    }

    /**
     * Add a new move node to the repertoire tree.
     * Checks if identical move already exists under parentId to avoid duplicates.
     */
    suspend fun addMove(
        parentId: Long?,
        fenAfter: String,
        moveSan: String,
        fromSquare: String,
        toSquare: String,
        promotionPiece: String? = null,
        comment: String = "",
        colorToPlay: String
    ): Long {
        val children = repertoireDao.getChildrenOf(parentId)
        val existing = children.find {
            it.fromSquare == fromSquare && it.toSquare == toSquare && it.promotionPiece == promotionPiece
        }
        if (existing != null) {
            return existing.id
        }

        val entity = RepertoireNodeEntity(
            parentId = parentId,
            fen = fenAfter,
            moveSan = moveSan,
            fromSquare = fromSquare,
            toSquare = toSquare,
            promotionPiece = promotionPiece,
            comment = comment,
            colorToPlay = colorToPlay,
            boxNumber = 1,
            nextReviewAt = 0L, // Due immediately
            createdAt = System.currentTimeMillis()
        )
        return repertoireDao.insertNode(entity)
    }

    /**
     * Update comment on a node.
     */
    suspend fun updateComment(id: Long, comment: String) {
        val existing = repertoireDao.getNodeById(id) ?: return
        repertoireDao.updateNode(existing.copy(comment = comment))
    }

    /**
     * Delete node and all child variations (CASCADE).
     */
    suspend fun deleteNode(id: Long) {
        repertoireDao.deleteNodeById(id)
    }

    /**
     * Record practice/review result for a node using Leitner 5-box system.
     */
    suspend fun recordReviewResult(
        id: Long,
        correct: Boolean,
        currentTimeMs: Long = System.currentTimeMillis()
    ) {
        val node = repertoireDao.getNodeById(id) ?: return
        val result = if (correct) {
            LeitnerSpacedRepetition.promote(node.boxNumber, currentTimeMs)
        } else {
            LeitnerSpacedRepetition.demote(node.boxNumber, currentTimeMs)
        }
        repertoireDao.updateSpacedRepetition(
            id = id,
            boxNumber = result.boxNumber,
            nextReviewAt = result.nextReviewAt
        )
    }

    private fun RepertoireNodeEntity.toDomain(): RepertoireNode {
        return RepertoireNode(
            id = id,
            parentId = parentId,
            fen = fen,
            moveSan = moveSan,
            fromSquare = fromSquare,
            toSquare = toSquare,
            promotionPiece = promotionPiece,
            comment = comment,
            colorToPlay = colorToPlay,
            boxNumber = boxNumber,
            nextReviewAt = nextReviewAt,
            createdAt = createdAt
        )
    }
}
