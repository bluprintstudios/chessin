package com.pro.chess_review.feature.importpgn

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/** Opens the system document picker for PGN files and text-based PGN exports. */
@Composable
fun rememberPgnFilePicker(onFilePicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onFilePicked(it) }
    }

    return {
        // PGN providers disagree about the MIME type, so allow the common types and a fallback.
        launcher.launch(arrayOf("application/x-chess-pgn", "application/*", "text/*", "*/*"))
    }
}
