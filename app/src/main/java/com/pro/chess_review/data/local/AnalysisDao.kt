package com.pro.chess_review.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analysis_cache WHERE fen = :fen AND engineId = :engineId AND engineVersion = :engineVersion AND searchDepth >= :minDepth AND multiPv >= :minMultiPv LIMIT 1")
    suspend fun getCachedAnalysis(
        fen: String, 
        engineId: String, 
        engineVersion: String, 
        minDepth: Int, 
        minMultiPv: Int
    ): AnalysisEntity?

    @Upsert
    suspend fun upsert(analysis: AnalysisEntity)

    @Query("DELETE FROM analysis_cache")
    suspend fun deleteAll()
}
