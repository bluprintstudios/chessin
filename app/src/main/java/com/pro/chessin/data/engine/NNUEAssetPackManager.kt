package com.pro.chessin.data.engine

import android.content.Context
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Resolves the path to the NNUE network file from the install-time
 * Play Asset Delivery module `:nnue_assets`.
 *
 * For production installs delivered via Google Play, queries [AssetPackManager].
 * For local debug APK builds, falls back to local storage locations.
 */
@Singleton
class NNUEAssetPackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NNUEAssetPackManager"
        const val NNUE_ASSET_PACK = "nnue_assets"
        const val NNUE_FILE_NAME = "nn-1a298aa575a0.nnue"
    }

    private val assetPackManager: AssetPackManager by lazy {
        AssetPackManagerFactory.getInstance(context)
    }

    suspend fun getAssetPackPath(assetPackName: String = NNUE_ASSET_PACK): String =
        suspendCancellableCoroutine { continuation ->
            try {
                // 1. Try resolving via Play Asset Delivery AssetPackManager
                val packLocation = assetPackManager.getPackLocation(assetPackName)
                if (packLocation != null) {
                    val assetsPath = packLocation.assetsPath()
                    if (!assetsPath.isNullOrEmpty()) {
                        val nnueFile = File(assetsPath, NNUE_FILE_NAME)
                        if (nnueFile.exists()) {
                            logAssetPackInfo(assetPackName, nnueFile.absolutePath)
                            continuation.resume(nnueFile.absolutePath)
                            return@suspendCancellableCoroutine
                        }
                    }
                }

                Log.d(TAG, "Play Asset Delivery pack location not resolved directly, trying fallback paths...")
                tryAlternativePaths(assetPackName, continuation)
            } catch (e: Exception) {
                Log.e(TAG, "Error querying Play Asset Manager, trying fallback paths...", e)
                tryAlternativePaths(assetPackName, continuation)
            }
        }

    private fun tryAlternativePaths(
        assetPackName: String,
        continuation: kotlinx.coroutines.CancellableContinuation<String>
    ) {
        val candidatePaths = listOfNotNull(
            context.getExternalFilesDir("local_testing")?.let { "${it.absolutePath}/$NNUE_FILE_NAME" },
            context.getExternalFilesDir(null)?.let { "${it.absolutePath}/$NNUE_FILE_NAME" },
            "${context.filesDir.absolutePath}/$NNUE_FILE_NAME",
            "${context.cacheDir.absolutePath}/$NNUE_FILE_NAME",
            "${context.cacheDir.absolutePath}/assets/$assetPackName/$NNUE_FILE_NAME",
            "${context.filesDir.absolutePath}/assets/$assetPackName/$NNUE_FILE_NAME"
        )

        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                logAssetPackInfo(assetPackName, path)
                continuation.resume(path)
                return
            }
        }

        Log.e(TAG, "NNUE asset pack '$assetPackName' not found in any known location")
        continuation.resumeWithException(
            IllegalStateException("NNUE Asset pack not found: $assetPackName. Ensure install-time asset pack is installed.")
        )
    }

    private fun logAssetPackInfo(assetPackName: String, path: String) {
        try {
            val file = File(path)
            Log.d(TAG, "Asset pack '$assetPackName' resolved to: $path (${file.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging asset pack info", e)
        }
    }
}
