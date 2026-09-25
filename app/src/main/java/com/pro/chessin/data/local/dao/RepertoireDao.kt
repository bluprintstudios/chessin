package com.pro.chessin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pro.chessin.data.local.entity.RepertoireNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for repertoire nodes.
 */
@Dao
interface RepertoireDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: RepertoireNodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<RepertoireNodeEntity>): List<Long>

    @Update
    suspend fun updateNode(node: RepertoireNodeEntity)

    @Query("DELETE FROM repertoire_nodes WHERE id = :id")
    suspend fun deleteNodeById(id: Long)

    @Query("SELECT * FROM repertoire_nodes WHERE id = :id")
    suspend fun getNodeById(id: Long): RepertoireNodeEntity?

    @Query("SELECT * FROM repertoire_nodes WHERE colorToPlay = :colorToPlay ORDER BY id ASC")
    fun getNodesByColor(colorToPlay: String): Flow<List<RepertoireNodeEntity>>

    @Query("SELECT * FROM repertoire_nodes ORDER BY id ASC")
    fun getAllNodes(): Flow<List<RepertoireNodeEntity>>

    @Query("SELECT * FROM repertoire_nodes WHERE parentId IS :parentId")
    suspend fun getChildrenOf(parentId: Long?): List<RepertoireNodeEntity>

    @Query("SELECT * FROM repertoire_nodes WHERE colorToPlay = :colorToPlay AND nextReviewAt <= :maxTimestamp ORDER BY nextReviewAt ASC, id ASC")
    suspend fun getDueNodes(colorToPlay: String, maxTimestamp: Long): List<RepertoireNodeEntity>

    @Query("SELECT * FROM repertoire_nodes WHERE nextReviewAt <= :maxTimestamp ORDER BY nextReviewAt ASC, id ASC")
    suspend fun getAllDueNodes(maxTimestamp: Long): List<RepertoireNodeEntity>

    @Query("UPDATE repertoire_nodes SET boxNumber = :boxNumber, nextReviewAt = :nextReviewAt WHERE id = :id")
    suspend fun updateSpacedRepetition(id: Long, boxNumber: Int, nextReviewAt: Long)
}
