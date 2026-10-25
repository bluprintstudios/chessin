package com.pro.chess_review.core.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

class UciEngineProcess(
    private val engineExecutable: File
) : ChessEngine {

    override val engineId: String = "Stockfish_19"
    override val engineVersion: String = "19"

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null
    
    private val isRunning = AtomicBoolean(false)

    override suspend fun start() {
        withContext(Dispatchers.IO) {
            if (!engineExecutable.exists() || !engineExecutable.canExecute()) {
                // Unsupported ABIs can still replay games; report missing engine analysis as incomplete.
                return@withContext
            }

            process = ProcessBuilder(engineExecutable.absolutePath)
                .redirectErrorStream(true)
                .start()

            process?.let {
                reader = BufferedReader(InputStreamReader(it.inputStream))
                writer = OutputStreamWriter(it.outputStream)
                isRunning.set(true)
                
                // Initialize UCI
                sendCommand("uci")
                waitFor("uciok")
                sendCommand("isready")
                waitFor("readyok")
            }
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            sendCommand("quit")
            isRunning.set(false)
            process?.destroy()
            process = null
            reader = null
            writer = null
        }
    }

    private fun sendCommand(cmd: String) {
        try {
            writer?.apply {
                write("$cmd\n")
                flush()
            }
        } catch (e: Exception) {
            // Log or ignore if dead
        }
    }

    private fun waitFor(expected: String) {
        var line: String? = reader?.readLine()
        while (line != null && isRunning.get()) {
            if (line == expected) return
            line = reader?.readLine()
        }
    }

    override fun analyze(fen: String, settings: EngineSettings): Flow<EngineResult> = flow {
        if (!isRunning.get() || process == null) {
            emit(EngineResult(fen, emptyList(), engineId, engineVersion, isComplete = false))
            return@flow
        }
        
        sendCommand("setoption name MultiPV value ${settings.multiPv}")
        sendCommand("position fen $fen")
        sendCommand("go depth ${settings.searchDepth}")

        val linesMap = mutableMapOf<Int, EngineLine>()
        
        var line: String? = reader?.readLine()
        while (line != null && isRunning.get() && currentCoroutineContext().isActive) {
            // e.g. "info depth 18 seldepth 25 multipv 1 score cp 45 nodes 1234 nps 5678 time 100 pv e2e4 e7e5"
            if (line.startsWith("info") && line.contains("multipv")) {
                val parsedLine = parseInfoLine(fen, line)
                if (parsedLine != null) {
                    linesMap[parsedLine.multipv] = parsedLine.line
                    // Emit the current best known lines
                    val sortedLines = linesMap.entries.sortedBy { it.key }.map { it.value }
                    emit(EngineResult(fen, sortedLines, engineId, engineVersion, isComplete = false))
                }
            } else if (line.startsWith("bestmove")) {
                val sortedLines = linesMap.entries.sortedBy { it.key }.map { it.value }
                emit(EngineResult(fen, sortedLines, engineId, engineVersion, isComplete = true))
                break
            }
            line = reader?.readLine()
        }
        
        // Ensure we stop engine if coroutine cancelled
        sendCommand("stop")
    }.flowOn(Dispatchers.IO)

    private fun parseInfoLine(fen: String, info: String): ParsedUciLine? {
        val tokens = info.split(" ")
        
        var depth = 0
        var multipv = 1
        var cpString: String? = null
        var mateString: String? = null
        val uciMoves = mutableListOf<String>()
        
        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> depth = tokens[++i].toIntOrNull() ?: 0
                "multipv" -> multipv = tokens[++i].toIntOrNull() ?: 1
                "score" -> {
                    i++
                    if (tokens[i] == "cp") cpString = tokens[++i]
                    else if (tokens[i] == "mate") mateString = tokens[++i]
                }
                "pv" -> {
                    i++
                    while (i < tokens.size) {
                        uciMoves.add(tokens[i])
                        i++
                    }
                }
            }
            i++
        }
        
        if (uciMoves.isEmpty()) return null

        val engineLine = UciParser.parseUciLine(fen, uciMoves, cpString, mateString, depth)
        return ParsedUciLine(multipv, engineLine)
    }

    private data class ParsedUciLine(val multipv: Int, val line: EngineLine)
}
