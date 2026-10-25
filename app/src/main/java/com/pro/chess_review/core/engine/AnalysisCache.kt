package com.pro.chess_review.core.engine

import com.pro.chess_review.data.local.AnalysisDao
import com.pro.chess_review.data.local.AnalysisEntity
import org.json.JSONArray
import org.json.JSONObject

interface AnalysisCache {
    suspend fun getCachedResult(
        fen: String,
        engineId: String,
        engineVersion: String,
        settings: EngineSettings
    ): EngineResult?

    suspend fun saveResult(result: EngineResult, settings: EngineSettings)
}

/**
 * Wraps the AnalysisDao to handle JSON serialization of EngineLine lists
 * and maps the caching logic for Phase 4.
 */
class RoomAnalysisCache(private val analysisDao: AnalysisDao) : AnalysisCache {

    override suspend fun getCachedResult(
        fen: String,
        engineId: String,
        engineVersion: String,
        settings: EngineSettings
    ): EngineResult? {
        val entity = analysisDao.getCachedAnalysis(
            fen = fen,
            engineId = engineId,
            engineVersion = engineVersion,
            minDepth = settings.searchDepth,
            minMultiPv = settings.multiPv
        ) ?: return null

        val lines = mutableListOf<EngineLine>()
        try {
            val jsonArray = JSONArray(entity.linesJson)
            for (i in 0 until jsonArray.length()) {
                val lineObj = jsonArray.getJSONObject(i)
                
                val uciMoves = mutableListOf<String>()
                val uciArr = lineObj.getJSONArray("uciMoves")
                for (j in 0 until uciArr.length()) uciMoves.add(uciArr.getString(j))

                val sanMoves = mutableListOf<String>()
                val sanArr = lineObj.getJSONArray("sanMoves")
                for (j in 0 until sanArr.length()) sanMoves.add(sanArr.getString(j))

                val cp = if (lineObj.has("centipawns")) lineObj.getInt("centipawns") else null
                val mate = if (lineObj.has("mateIn")) lineObj.getInt("mateIn") else null
                val depth = lineObj.getInt("depth")
                
                lines.add(EngineLine(uciMoves, sanMoves, cp, mate, depth))
            }
        } catch (e: Exception) {
            return null // Invalid cache
        }
        if (lines.isEmpty()) return null

        return EngineResult(
            fen = fen,
            lines = lines,
            engineId = entity.engineId,
            engineVersion = entity.engineVersion,
            isComplete = true // Cached results in DB are always saved at their final depth
        )
    }

    override suspend fun saveResult(result: EngineResult, settings: EngineSettings) {
        if (!result.isComplete || result.lines.isEmpty()) return

        val jsonArray = JSONArray()
        for (line in result.lines) {
            val lineObj = JSONObject()
            lineObj.put("uciMoves", JSONArray(line.uciMoves))
            lineObj.put("sanMoves", JSONArray(line.sanMoves))
            line.centipawns?.let { lineObj.put("centipawns", it) }
            line.mateIn?.let { lineObj.put("mateIn", it) }
            lineObj.put("depth", line.depth)
            jsonArray.put(lineObj)
        }

        val entity = AnalysisEntity(
            fen = result.fen,
            engineId = result.engineId,
            engineVersion = result.engineVersion,
            searchDepth = settings.searchDepth,
            multiPv = settings.multiPv,
            linesJson = jsonArray.toString(),
            analyzedAtEpochMillis = System.currentTimeMillis()
        )
        
        analysisDao.upsert(entity)
    }
}
