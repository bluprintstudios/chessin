package com.pro.chess_review.feature.review

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.caverock.androidsvg.SVG
import com.pro.chess_review.ui.theme.LocalHudPalette

private const val SVG_RENDER_SIZE = 256

@Composable
fun ChessBoard(
    fen: String,
    isFlipped: Boolean = false,
    pieceSetId: String = ChessPieceSetCatalog.DEFAULT_ID,
    modifier: Modifier = Modifier,
) {
    val placement = fen.split(" ").firstOrNull() ?: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
    val ranks = placement.split("/")
    val squares = Array(64) { ' ' }

    if (ranks.size == 8) {
        for (rank in 0..7) {
            var file = 0
            for (char in ranks[rank]) {
                if (char.isDigit()) {
                    file += char.digitToInt()
                } else if (file in 0..7) {
                    squares[rank * 8 + file] = char
                    file++
                }
            }
        }
    }

    val palette = LocalHudPalette.current
    val boardColors = listOf(palette.boardLight, palette.boardDark)
    val displayRanks = if (isFlipped) 7 downTo 0 else 0..7
    val displayFiles = if (isFlipped) 7 downTo 0 else 0..7

    Column(modifier = modifier.aspectRatio(1f)) {
        for (rank in displayRanks) {
            Row(modifier = Modifier.weight(1f)) {
                for (file in displayFiles) {
                    val squareColor = if ((rank + file) % 2 == 0) boardColors[0] else boardColors[1]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(squareColor)
                            .semantics {
                                contentDescription = squareDescription(
                                    square = chessSquareName(rank, file),
                                    fenChar = squares[rank * 8 + file],
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Piece(squares[rank * 8 + file], pieceSetId)
                    }
                }
            }
        }
    }
}

@Composable
fun Piece(fenChar: Char, pieceSetId: String = ChessPieceSetCatalog.DEFAULT_ID) {
    val assetPath = pieceAssetPath(pieceSetId, fenChar) ?: return
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = assetPath) {
        value = withContext(Dispatchers.IO) {
            PieceBitmapCache.load(context, assetPath)
                ?: if (pieceSetId != ChessPieceSetCatalog.DEFAULT_ID) {
                    pieceAssetPath(ChessPieceSetCatalog.DEFAULT_ID, fenChar)?.let {
                        PieceBitmapCache.load(context, it)
                    }
                } else {
                    null
                }
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.86f),
            contentScale = ContentScale.Fit,
        )
    }
}

internal fun pieceAssetPath(pieceSetId: String, fenChar: Char): String? {
    val piece = when (fenChar.lowercaseChar()) {
        'k' -> "K"
        'q' -> "Q"
        'r' -> "R"
        'b' -> "B"
        'n' -> "N"
        'p' -> "P"
        else -> return null
    }
    val setId = ChessPieceSetCatalog.find(pieceSetId)?.id ?: ChessPieceSetCatalog.DEFAULT_ID
    val color = if (fenChar.isUpperCase()) "w" else "b"
    return "pieces/$setId/$color$piece.svg"
}

private object PieceBitmapCache {
    private val bitmaps = object : LruCache<String, Bitmap>(4 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.byteCount / 1024)
    }

    fun load(context: Context, path: String): Bitmap? = synchronized(bitmaps) {
        bitmaps.get(path)?.let { return@synchronized it }
        runCatching {
            val picture = context.assets.open(path).use { stream ->
                SVG.getFromInputStream(stream).renderToPicture(SVG_RENDER_SIZE, SVG_RENDER_SIZE)
            }
            Bitmap.createBitmap(SVG_RENDER_SIZE, SVG_RENDER_SIZE, Bitmap.Config.ARGB_8888).also {
                AndroidCanvas(it).drawPicture(picture)
                bitmaps.put(path, it)
            }
        }.getOrNull()
    }
}

internal fun chessSquareName(rank: Int, file: Int): String {
    require(rank in 0..7) { "Rank must be between 0 and 7." }
    require(file in 0..7) { "File must be between 0 and 7." }
    return "${('a'.code + file).toChar()}${8 - rank}"
}

internal fun squareDescription(square: String, fenChar: Char): String {
    if (fenChar == ' ') return "$square, empty"
    return "$square, ${pieceDescription(fenChar)}"
}

private fun pieceDescription(fenChar: Char): String {
    val color = if (fenChar.isUpperCase()) "White" else "Black"
    val name = when (fenChar.lowercaseChar()) {
        'k' -> "king"
        'q' -> "queen"
        'r' -> "rook"
        'b' -> "bishop"
        'n' -> "knight"
        'p' -> "pawn"
        else -> "piece"
    }
    return "$color $name"
}
