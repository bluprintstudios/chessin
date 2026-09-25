package com.pro.chessin.engine

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NativeBridge {
    private const val TAG = "NativeBridge"

    init {
        try {
            System.loadLibrary("stockfish-jni")
            Log.d(TAG, "Stockfish JNI library loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Stockfish JNI library", e)
        }
    }

    // SharedFlow for broadcasting engine output to all collectors.
    // replay = 10 ensures new/late collectors receive recent emissions (e.g. bestmove line).
    // extraBufferCapacity = 500 handles high-throughput bursts from Stockfish info lines.
    private val _engineOutput = MutableSharedFlow<String>(replay = 10, extraBufferCapacity = 500)
    val engineOutput: SharedFlow<String> get() = _engineOutput.asSharedFlow()

    /**
     * Clears the replay cache for the engine output flow.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun resetOutputBuffer() {
        Log.d(TAG, "resetOutputBuffer() called - clearing replay cache")
        _engineOutput.resetReplayCache()
    }

    /**
     * Called from native code via JNI to report engine output.
     * Static method so JNI can find it via env->CallStaticVoidMethod
     */
    @JvmStatic
    fun onEngineOutput(line: String) {
        Log.d(TAG, "Engine output: $line")
        // Try to emit to the flow, but don't crash if there are no subscribers
        try {
            val emitted = _engineOutput.tryEmit(line)
            Log.d(TAG, "Emit result: $emitted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit engine output: $line", e)
        }
    }

    /**
     * Start the engine with the given NNUE network file path.
     * Must be called before sending any commands.
     */
    @JvmStatic
    external fun engineStart(nnuePath: String)

    /**
     * Send a UCI command to the engine (e.g., "position startpos", "go depth 20")
     */
    @JvmStatic
    external fun engineSendCommand(command: String)

    /**
     * Stop the engine and clean up resources.
     * After this, must call engineStart() before using the engine again.
     */
    @JvmStatic
    external fun engineStop()
}
