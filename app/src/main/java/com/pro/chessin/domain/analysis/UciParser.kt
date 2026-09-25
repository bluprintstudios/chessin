package com.pro.chessin.domain.analysis

import android.util.Log

/**
 * Parses UCI engine output to extract evaluation information.
 * Handles MultiPV output lines and bestmove lines.
 */
object UciParser {
    
    private const val TAG = "UciParser"
    
    /**
     * Represents a parsed UCI info line with evaluation data.
     */
    data class ParsedInfo(
        val depth: Int,
        val multiPv: Int,
        val score: EvalScore,
        val pv: List<String>
    )
    
    /**
     * Represents a parsed bestmove line.
     */
    data class BestMove(
        val move: String,
        val ponder: String?
    )
    
    /**
     * Parses a UCI info line and returns evaluation data if present.
     * 
     * Expected format:
     * info depth 14 multipv 1 score cp 50 pv e2e4 e7e5 ...
     * info depth 14 multipv 2 score mate 3 pv ...
     * 
     * @param line The UCI info line to parse
     * @return ParsedInfo if the line contains score data, null otherwise
     */
    fun parseInfoLine(line: String): ParsedInfo? {
        if (!line.startsWith("info")) return null
        
        val tokens = line.split(" ")
        var depth = 0
        var multiPv = 1
        var score: EvalScore? = null
        val pv = mutableListOf<String>()
        
        var i = 1 // Skip "info" token
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> {
                    if (i + 1 < tokens.size) {
                        depth = tokens[i + 1].toIntOrNull() ?: 0
                        i += 2
                    } else {
                        i++
                    }
                }
                "multipv" -> {
                    if (i + 1 < tokens.size) {
                        multiPv = tokens[i + 1].toIntOrNull() ?: 1
                        i += 2
                    } else {
                        i++
                    }
                }
                "score" -> {
                    if (i + 1 < tokens.size) {
                        score = when (tokens[i + 1]) {
                            "cp" -> {
                                if (i + 2 < tokens.size) {
                                    val cp = tokens[i + 2].toIntOrNull() ?: 0
                                    EvalScore.fromCp(cp)
                                } else {
                                    null
                                }
                            }
                            "mate" -> {
                                if (i + 2 < tokens.size) {
                                    val mate = tokens[i + 2].toIntOrNull() ?: 0
                                    EvalScore.fromMate(mate)
                                } else {
                                    null
                                }
                            }
                            else -> null
                        }
                        i += 3
                    } else {
                        i++
                    }
                }
                "pv" -> {
                    // Collect all remaining tokens as PV moves
                    i++
                    while (i < tokens.size) {
                        pv.add(tokens[i])
                        i++
                    }
                }
                else -> {
                    i++
                }
            }
        }
        
        // Only return if we have a score
        return if (score != null && depth > 0) {
            ParsedInfo(depth, multiPv, score, pv)
        } else {
            null
        }
    }
    
    /**
     * Parses a UCI bestmove line.
     * 
     * Expected format:
     * bestmove e2e4 ponder e7e5
     * bestmove e2e4
     * 
     * @param line The UCI bestmove line to parse
     * @return BestMove if the line is a bestmove line, null otherwise
     */
    fun parseBestMove(line: String): BestMove? {
        if (!line.startsWith("bestmove")) return null
        
        val tokens = line.split(" ")
        if (tokens.size < 2) return null
        
        val move = tokens[1]
        val ponder = if (tokens.size >= 4 && tokens[2] == "ponder") {
            tokens[3]
        } else {
            null
        }
        
        return BestMove(move, ponder)
    }
    
    /**
     * Extracts the top N evaluation scores from a sequence of UCI info lines.
     * 
     * @param lines Sequence of UCI output lines
     * @param count Number of top PV lines to extract
     * @return Map of multiPv index to EvalScore (1-indexed)
     */
    fun extractTopScores(lines: Sequence<String>, count: Int): Map<Int, EvalScore> {
        val scores = mutableMapOf<Int, EvalScore>()
        
        for (line in lines) {
            val parsed = parseInfoLine(line)
            if (parsed != null && parsed.multiPv <= count) {
                scores[parsed.multiPv] = parsed.score
            }
        }
        
        return scores
    }

    /**
     * Extracts the top N ParsedInfo objects from a sequence of UCI info lines.
     *
     * @param lines Sequence of UCI output lines
     * @param count Number of top PV lines to extract
     * @return Map of multiPv index to ParsedInfo (1-indexed)
     */
    fun extractTopInfo(lines: Sequence<String>, count: Int): Map<Int, ParsedInfo> {
        val infoMap = mutableMapOf<Int, ParsedInfo>()

        for (line in lines) {
            val parsed = parseInfoLine(line)
            if (parsed != null && parsed.multiPv <= count) {
                infoMap[parsed.multiPv] = parsed
            }
        }

        return infoMap
    }
    
    /**
     * Waits for a bestmove line in the output and returns it.
     * This is useful for synchronous analysis where we need to wait for completion.
     * 
     * @param lines Sequence of UCI output lines
     * @return BestMove if found, null otherwise
     */
    fun findBestMove(lines: Sequence<String>): BestMove? {
        for (line in lines) {
            val bestMove = parseBestMove(line)
            if (bestMove != null) {
                return bestMove
            }
        }
        return null
    }
}
